import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

public class YeOldVisionAlg {

    //The string that will have all output printed to it to be viewed in a text file
    public static String output = "";
    
    //Changed from 10
    public static int xMaxForShape = 5;
    //Changed from 60
    public static int yMaxForShape = 60;
    
    public static Hatch GetHatch(GripPipeline pipeline){
        
        ArrayList<Shape> shapes = new ArrayList<Shape>();
        shapes.add(new Shape());
        Point lastPoint = null;

        println("GetHatch() start");

        //When this boolean is false, the first contour has been processed
        boolean firstPoint = true;
        //The list that will be assigned to the current matOfPoint in the loop
        List<Point> points;

        println("Number of matofpoint's: " + pipeline.filterContoursOutput().size());

        for(MatOfPoint m : pipeline.filterContoursOutput()){
            points = m.toList();
            
            println("Contours in matofpoint: " + pipeline.filterContoursOutput().indexOf(m) + " = " +  points.size());

            for(int i = 0; i < points.size(); i++){
                Point p = points.get(i);
            
                //If this is the first point, then don't bother trying to compare it and just continue to the next point
                if(firstPoint){
                    println(p.x + ", " + p.y);
                    println("First point in mat of point number: " + pipeline.filterContoursOutput().indexOf(m));
                    shapes.get(0).add(p);
                    lastPoint = p;
                    firstPoint = false;
                }
                else{
                    println(p.x + ", " + p.y);
                    //if the difference in x values is bigger than the max x value for a shape
                    //but the difference in y values is less than 5, then create a new shape and add this point
                    //add this: && Math.abs(p.y - lastPoint.y) < 5, if you want to account for difference in y
                    if(Math.abs(p.x - lastPoint.x) > xMaxForShape){
                        shapes.add(new Shape());
                        shapes.get(shapes.size() - 1).add(p);
                        lastPoint = p;
                    }
                    //else if the difference in y values is less than 5, add it to the current shape
                    else if(Math.abs(p.y - lastPoint.y) < yMaxForShape){
                        shapes.get(shapes.size() - 1).add(p);
                        lastPoint = p;
                    }
                    //if the code reaches this point, the contour is a stray and should be ignored
                }
            }
        }

        println("Number of shapes before deleting: " + shapes.size());
        

        /* After creating all the shapes, loop through them to check if any are made of only a few pixels, if so
        *  they shouldn't be counted as a shape and should be deleted. Also, if the useAdvancedFilters boolean is true, 
        *  it will delete shapes that aren't the two we want to focus on (i.e. another hatch that is nearby but not the 
        *  closest hatch that we want to go to)
        */
        ArrayList<Shape> shapesToDelete = new ArrayList<>();

        //Add shapes that need to be deleted
        for(int i = 0; i < shapes.size(); i++){
            
            Shape shape = shapes.get(i);
            
            //If the number of contours in the shape is too small, its probably not a shape we want
            if(shape.size() < 10){
                shapesToDelete.add(shape);
                continue;
            }
        }

        //Remove shapes that were added to shapesToDelete
        for(Shape shape : shapesToDelete){
            println("Deleted a shape");
            shapes.remove(shape);
        }

        ArrayList<Hatch> hatches = new ArrayList<>();
        if(shapes.size() > 2){
            
            for(int i = 0; i < shapes.size(); i++){
                Shape shape = shapes.get(i);
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
                Hatch closestHatch = hatches.get(0);
                for(int i = 1; i < hatches.size(); i++){
                    Hatch hatch = hatches.get(i);
                    if(Math.abs(hatch.middleX() - 80) < Math.abs(closestHatch.middleX() - 80)){
                        closestHatch = hatch;
                    }
                }
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