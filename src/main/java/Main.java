/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.vision.VisionThread;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.utils.Converters;

/*
   JSON format:
   {
       "team": <team number>,
       "ntmode": <"client" or "server", "client" if unspecified>
       "cameras": [
           {
               "name": <camera name>
               "path": <path, e.g. "/dev/video0">
               "pixel format": <"MJPEG", "YUYV", etc>   // optional
               "width": <video mode width>              // optional
               "height": <video mode height>            // optional
               "fps": <video mode fps>                  // optional
               "brightness": <percentage brightness>    // optional
               "white balance": <"auto", "hold", value> // optional
               "exposure": <"auto", "hold", value>      // optional
               "properties": [                          // optional
                   {
                       "name": <property name>
                       "value": <property value>
                   }
               ],
               "stream": {                              // optional
                   "properties": [
                       {
                           "name": <stream property name>
                           "value": <stream property value>
                       }
                   ]
               }
           }
       ]
   }
 */

/* NOTE!
*
* The majority of this class is camera setup stuff (generating json files for camera configurations) 
* that shouldn't cause any problems with vision processing (unless there is a camera error). 
* The setup for the network table starts on line 231, and the vision pipeline code starts on line 260
*
*/

public final class Main {
  private static String configFile = "/boot/frc.json";

  @SuppressWarnings("MemberName")
  public static class CameraConfig {
    public String name;
    public String path;
    public JsonObject config;
    public JsonElement streamConfig;
  }

  public static int team;
  public static boolean server;
  public static List<CameraConfig> cameraConfigs = new ArrayList<>();

  private Main() {
  }

  /**
   * Report parse error.
   */
  public static void parseError(String str) {
    System.err.println("config error in '" + configFile + "': " + str);
  }

  /**
   * Read single camera configuration.
   */
  public static boolean readCameraConfig(JsonObject config) {
    CameraConfig cam = new CameraConfig();

    // name
    JsonElement nameElement = config.get("name");
    if (nameElement == null) {
      parseError("could not read camera name");
      return false;
    }
    cam.name = nameElement.getAsString();

    // path
    JsonElement pathElement = config.get("path");
    if (pathElement == null) {
      parseError("camera '" + cam.name + "': could not read path");
      return false;
    }
    cam.path = pathElement.getAsString();

    // stream properties
    cam.streamConfig = config.get("stream");

    cam.config = config;

    cameraConfigs.add(cam);
    return true;
  }

  /**
   * Read configuration file.
   */
  @SuppressWarnings("PMD.CyclomaticComplexity")
  public static boolean readConfig() {
    // parse file
    JsonElement top;
    try {
      top = new JsonParser().parse(Files.newBufferedReader(Paths.get(configFile)));
    } catch (IOException ex) {
      System.err.println("could not open '" + configFile + "': " + ex);
      return false;
    }

    // top level must be an object
    if (!top.isJsonObject()) {
      parseError("must be JSON object");
      return false;
    }
    JsonObject obj = top.getAsJsonObject();

    // team number
    JsonElement teamElement = obj.get("team");
    if (teamElement == null) {
      parseError("could not read team number");
      return false;
    }
    team = teamElement.getAsInt();

    // ntmode (optional)
    if (obj.has("ntmode")) {
      String str = obj.get("ntmode").getAsString();
      if ("client".equalsIgnoreCase(str)) {
        server = false;
      } else if ("server".equalsIgnoreCase(str)) {
        server = true;
      } else {
        parseError("could not understand ntmode value '" + str + "'");
      }
    }

    // cameras
    JsonElement camerasElement = obj.get("cameras");
    if (camerasElement == null) {
      parseError("could not read cameras");
      return false;
    }
    JsonArray cameras = camerasElement.getAsJsonArray();
    for (JsonElement camera : cameras) {
      if (!readCameraConfig(camera.getAsJsonObject())) {
        return false;
      }
    }

    return true;
  }

  /**
   * Start running the camera.
   */
  public static VideoSource startCamera(CameraConfig config) {
    System.out.println("Starting camera '" + config.name + "' on " + config.path);
    CameraServer inst = CameraServer.getInstance();
    UsbCamera camera = new UsbCamera(config.name, config.path);
    MjpegServer server = inst.startAutomaticCapture(camera);

    Gson gson = new GsonBuilder().create();

    camera.setConfigJson(gson.toJson(config.config));
    camera.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen);

    if (config.streamConfig != null) {
      server.setConfigJson(gson.toJson(config.streamConfig));
    }

    return camera;
  }

  /**
   * Main.
   */
  public static void main(String[] args) {
    if (args.length > 0) {
      configFile = args[0];
    }

    // read configuration
    if (!readConfig()) {
      return;
    }

    // Get the instance of the network table storing all table data
    NetworkTableInstance ntinst = NetworkTableInstance.getDefault();

    //Declarations for all the network table variables
    NetworkTable ntTable;
    NetworkTableEntry xEntry;
    NetworkTableEntry yEntry;

    //Start the the server for the network table or start a client
    if (server) {
      System.out.println("Setting up NetworkTables server");
      ntinst.startServer();
    } else {
      System.out.println("Setting up NetworkTables client for team " + team);
      ntinst.startClientTeam(team);
    }

    //Get the table specifically holding vision data (this table is created if it doesn't already exist)
    ntTable = ntinst.getTable("visiontable");

    //Get or create an entry in the table for the y and x data
    xEntry = ntTable.getEntry("X");
    yEntry = ntTable.getEntry("Y");

    // start cameras
    List<VideoSource> cameras = new ArrayList<>();
    for (CameraConfig cameraConfig : cameraConfigs) {
      cameras.add(startCamera(cameraConfig));
    }

    // if there is at least one camera detected, start processing
    if (cameras.size() >= 1) {

      //Use Imgcodecs.imread() to make a mat with an image stored locally on the system (pi), the path given is an example path that follows windows notation (not linux)
      //Mat inputMat = Imgcodecs.imread(".\\src\\main\\java\\VisionTestImage.jpg");

      //Make the mat that will store the image data
      Mat mat = new Mat();

      //Make a video sink for the camera feed, this sink will generate the camera feed into a mat
      CvSink cvSink = CameraServer.getInstance().getVideo();

      /* NOTE
      *
      * The camera is defaultly configured to by 160 x 120 at 30 fps
      *
      */
      VisionThread visionThread = new VisionThread(cameras.get(0), new GripPipeline(), pipeline -> {

        //Get the camera frame and use it for the mat. grabFrame() will return a number value, if it is zero, there is nothing there and we shouldn't process it
        if(cvSink.grabFrame(mat) != 0){
          
          //call the process function in the GripPipeline class to process the mat
          pipeline.process(mat);

          //Are there contours to get a hatch from? If not then print that there are none
          if(pipeline.filterContoursOutput().size() > 0){
            Hatch hatch = VisionMethods.GetHatch(pipeline);
            //If the hatch returned from the GetHatch() method was not null, then give the network table the middle x value
            if(hatch != null){
              xEntry.setDouble(hatch.middleX());
              VisionMethods.println("Hatch middle x value is: " + hatch.middleX());
            }
          }
          else{
            VisionMethods.println("No contours");
          }
        }
        else{
          VisionMethods.println("Skipping frame");
        }
        /*try {
          BufferedWriter writer = new BufferedWriter(new FileWriter("consoleOutput.txt"));
          //writer.write(VisionMethods.output);
          writer.close();
        } catch (IOException ex) {
          ex.printStackTrace();
        }*/
      });
      //Began the thread (keep in mind that a "vision thread" is basically treated the same as any java thread for our purposes)
      visionThread.start();
    }

    // loop forever while telling the thread to wait before executing again (Thread.sleep)
    for (;;) {
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) {
        //Print the stack then exit the loop (end the process)
        e.printStackTrace();
        return;
      }
    }
  }
}
