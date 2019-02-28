import java.io.*;
import java.util.ArrayList;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import edu.wpi.first.networktables.NetworkTableEntry;

public class VisionMethods {

    public static int xMaxForShape = 10;
    public static int yMaxForShape = 60;
    
    /*Enable if you want to better equate the shapes that are in the sight of the robot
    * (i.e. if there are four shapes, figure out exactly which ones are hatches and which ones to focus on)
    * We shouldn't need to use advanced filters because the robot can probably drive up to the hatch acurately enough to
    * avoid scanning all other shapes and wasting 
    */
    public static boolean useAdvancedFilters = false;
    
    public static Hatch GetHatch(GripPipeline pipeline){
        
        ArrayList<Shape> shapes = new ArrayList<Shape>();
        shapes.add(new Shape());
        Point lastPoint = null;

        System.out.println("GetHatch() start");

        BufferedWriter writer = null;
        try{
            writer = new BufferedWriter(new FileWriter("saved.txt"));
        } catch(Exception e){
            e.printStackTrace();
        }

        boolean firstPoint = true;
        for(MatOfPoint m : pipeline.filterContoursOutput()){
            System.out.println();
            for(int i = 0; i < m.toList().size(); i++){
                Point p = m.toList().get(i);
                if(firstPoint){
                   //Delete me at some point
                    try{
                        writer.write(p.x + ", " + p.y);
                        writer.write("First point in mat of point number: " + pipeline.filterContoursOutput().indexOf(m) + System.lineSeparator());
                    } 
                    catch(Exception e){
                    }
                }
                
                //Delte me at some point
                try{
                    writer.write(p.x + ", " + p.y + System.lineSeparator());
                } 
                catch(Exception e){
                }
                
                //If this is the first point, then don't bother trying to compare it and just continue to the next point
                if(firstPoint){
                    shapes.get(0).add(p);
                    lastPoint = p;
                    firstPoint = false;
                    continue;
                }

                //if the difference in x values is bigger than 5
                //but the difference in y values is less than 5, then create a new shape (list) and add this point
                if(Math.abs(p.x - lastPoint.x) > xMaxForShape && Math.abs(p.y - lastPoint.y) < 5){
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

        try{
            writer.close();
        }
        catch(Exception e){
        }
        

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
            shapes.remove(shape);
        }

        ArrayList<Hatch> hatches = new ArrayList<>();
        if(shapes.size() > 2){
            
            for(int i = 0; i < shapes.size(); i++){
                Shape shape = shapes.get(i);
                if(i != shapes.size() - 1 && shape.leansRight() && !shapes.get(i + 1).leansRight()){
                    hatches.add(new Hatch(shape, shapes.get(i + 1)));
                    i++;
                }
            }
            if(hatches.size() == 1){
                System.out.println("There is one hatch");
                return hatches.get(0);
            }
            else if (hatches.size() > 1){
                System.out.println("There is more than one hatch");
                Hatch closestHatch = hatches.get(0);
                for(int i = 1; i < hatches.size(); i++){
                    Hatch hatch = hatches.get(i);
                    if(Math.abs(hatch.middle() - 80) < Math.abs(closestHatch.middle() - 80)){
                        closestHatch = hatch;
                    }
                }
                return closestHatch;
            }
            
        }
        else if(shapes.size() == 1){
            System.out.println("There is only one shape");
            return null;
        }
        else{
            System.out.println("There are no shapes");
            return null;
        }
        System.out.println("There are no shapes");
        return null;
    }

}