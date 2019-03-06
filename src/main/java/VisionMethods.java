import java.util.ArrayList;
import java.util.List;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

public class VisionMethods {

    //The string that will have all output printed to it to be viewed in a text file
    public static String output = "";
    
    //Changed from 10
    public static int xMaxForShape = 5;
    //Changed from 60
    public static int yMaxForShape = 60;
    
    public static Hatch GetHatch(GripPipeline pipeline){
        
        println("GetHatch() start");
        
        ArrayList<Shape> shapes = new ArrayList<Shape>();
        
        //The list that will be assigned to the current matOfPoint in the loop
        List<Point> points;

        println("Number of matofpoint's: " + pipeline.filterContoursOutput().size());

        for(int m = 0; m < pipeline.filterContoursOutput().size(); m++){
            MatOfPoint matOfPoint = pipeline.filterContoursOutput().get(m);
            points = matOfPoint.toList();
            shapes.add(new Shape(points));
            Shape shape = shapes.get(m);
            
            println("Contours in matofpoint: " + pipeline.filterContoursOutput().indexOf(matOfPoint) + " = " +  points.size());

            //If there are more than 10 contours, process the shape
            if(points.size() > 10){
                for(int p = 0; p < points.size(); p++){
                    Point point = points.get(p);
            
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
                    //if the rightMostPoint's y is lesser, its higher and the shape must be leaning right
                    if(shape.rightMostPoint.y < shape.leftMostPoint.y){
                        shape.leansRight = true;
                    }
                    //else, its leaning left so leansRight should be false
                    else{
                        shape.leansRight = false;
                    }
                }
            }
            //else, if the shape had 10 or less contours, it wasn't a piece of vision tape
            else{
                shapes.remove(shape);
            }
        }

        
        if(shapes.size() == 2){
            println("There is one hatch");
            return new Hatch(shapes.get(0), shapes.get(1));
        }
        else if(shapes.size() > 2){
            //Make a list of hatches and loop through shapes to find hatches
            ArrayList<Hatch> hatches = new ArrayList<>();

            for(int i = 0; i < shapes.size(); i++){
                Shape shape = shapes.get(i);
                //if this isn't the first shape and it leans right and the following shape doesn't lean right, it must be a hatch
                if(i != shapes.size() - 1 && shape.leansRight && !shapes.get(i + 1).leansRight){
                    hatches.add(new Hatch(shape, shapes.get(i + 1)));
                    i++;
                }
            }
            if(hatches.size() == 1){
                println("There is one hatch");
                return hatches.get(0);
            }
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
        else if(shapes.size() == 1){
            println("There is only one shape");
            return null;
        }
        println("There are no shapes");
        return null;
    }

    public static void println(String string){
        System.out.println(string);
        output += string + System.lineSeparator();
    }

}