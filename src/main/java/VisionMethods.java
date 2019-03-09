import java.util.ArrayList;
import java.util.List;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

public class VisionMethods {

    //The string that will have all output printed to it to be viewed in a text file
    public static String output = "";
    
    //xMax and yMax forShape should not be needed because open_cv's algorithm already organizes shapes
    //If these values are ever used, they need to be tested/tweaked
    public static int xMaxForShape = 5;
    public static int yMaxForShape = 60;
    
    public static Hatch GetHatch(GripPipeline pipeline){
        
        //println("GetHatch() start");
        
        //Shapes that will be used to determine hatches
        ArrayList<Shape> shapes = new ArrayList<Shape>();
        
        //The list that will be assigned to the current matOfPoint in the loop
        List<Point> points;

        //println("Number of matofpoint's: " + pipeline.filterContoursOutput().size());

        //Loop through every matOfPoint in the output of the filterContours algorithm
        for(int m = 0; m < pipeline.filterContoursOutput().size(); m++){
            //Assign the matOfPoint, points, and shape
            MatOfPoint matOfPoint = pipeline.filterContoursOutput().get(m);
            points = matOfPoint.toList();
            Shape shape = new Shape();
            shapes.add(shape);
            
            //println("Contours in matofpoint: " + pipeline.filterContoursOutput().indexOf(matOfPoint) + " = " +  points.size());

            //If there are more than 10 contours, process the points
            if(points.size() > 10){
                
                shape.highestPoint = points.get(0);
                shape.lowestPoint = points.get(0);
                shape.leftMostPoint  = points.get(0);
                shape.rightMostPoint = points.get(0);

                //println(points.get(0).x + ", " + points.get(0).y);

                //Loop through every point
                for(int p = 1; p < points.size(); p++){
                    Point point = points.get(p);
                    //println(point.x + ", " + point.y);

                    //If the point is higher, it is the new highest point
                    if(shape.highestPoint.y > point.y){
                        shape.highestPoint = point;
                    }
                    //if the point is lower, it is the new lowest
                    else if(shape.lowestPoint.y < point.y){
                        shape.lowestPoint = point;
                    }
                    //if the point's x is greater, its the right most
                    if(shape.rightMostPoint.x < point.x){
                        shape.rightMostPoint = point;
                    }
                    //if thhe point's x is lesser, its the left most
                    else if(shape.leftMostPoint.x > point.x){
                        shape.leftMostPoint = point;
                    }
                    
                }
                //if the rightMostPoint's y is lesser, its higher and the shape must be leaning right
                if(shape.rightMostPoint.y < shape.leftMostPoint.y){
                    shape.leansRight = true;
                }
                //else, its leaning left so leansRight should be false
                else{
                    shape.leansRight = false;
                }
            }
            //else, if the shape had 10 or less contours, it wasn't a piece of vision tape
            else{
                println("removed a shape");
                shapes.remove(shape);
            }
        }

        //if there are only two shapes, assume there is a hatch and return from the function
        if(shapes.size() == 2){
            println("There is one hatch");
            return new Hatch(shapes.get(0), shapes.get(1));
        }
        //else if there is more than two shapes, determine what hatches are which and choose one
        else if(shapes.size() > 2){
            ArrayList<Hatch> hatches = new ArrayList<>();

            //Loop through every piece of vision tape
            for(int i = 0; i < shapes.size(); i++){
                Shape shape = shapes.get(i);
                //if this isn't the first shape and it leans right and the following shape doesn't lean right, it must be a hatch
                if(i != shapes.size() - 1 && shape.leansRight && !shapes.get(i + 1).leansRight){
                    hatches.add(new Hatch(shape, shapes.get(i + 1)));
                    //skip forward an extra time because we know the next shape is part of a hatch already
                    i++;
                }
            }
            //if there is only one hatch, return it
            if(hatches.size() == 1){
                println("There is one hatch");
                return hatches.get(0);
            }
            //else if there is more than one hatch, determine which hatch is closest, 
            //and go to it (this is tested and should hopefully not be needed)
            else if (hatches.size() > 1){
                println("There is more than one hatch");
                //set the closest hatch to the first hatch to start
                Hatch closestHatch = hatches.get(0);
                //loop through hatches, starting at the second hatch because the closest is already hatch 1 (thats why i = 1 not 0)
                for(int i = 1; i < hatches.size(); i++){
                    Hatch hatch = hatches.get(i);
                    //if the difference between the middle of the hatch and the center of the screen if less than the difference for the closest hatch
                    //the closest hatch is set to this hatch
                    if(Math.abs(hatch.middleX() - 80) < Math.abs(closestHatch.middleX() - 80)){
                        closestHatch = hatch;
                    }
                }
                println("The closest hatch was hatch number: " + hatches.indexOf(closestHatch));
                return closestHatch;
            }
        }
        //else if there is only one shape, report it and return null because there is not a full hatch visible
        //NOTE: We may want to code it such that it will interpolate when the hatch is and go to it anyway
        else if(shapes.size() == 1){
            println("There is only one shape");
            return null;
        }
        //else, there was somehow nothing and should return null (this will never happen, but the compiler requires this)
        println("There are no shapes");
        return null;
    }

    /**
     * For printing to the console and saving the print to a string that will be put in a text file on the RPI for debug after a match
     * @param string
     */
    public static void println(String string){
        System.out.println(string);
        output += string + System.lineSeparator();
    }

}