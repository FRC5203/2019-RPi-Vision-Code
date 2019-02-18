import java.util.ArrayList;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

public class VisionMethods {

    public static ArrayList<ArrayList<Point>> GetShapes(GripPipeline pipeline){
        
        ArrayList<ArrayList<Point>> shapes = new ArrayList<ArrayList<Point>>();
        shapes.add(new ArrayList<Point>());
        Point lastPoint = null;

        for(MatOfPoint m : pipeline.filterContoursOutput()){
            for(int i = 0; i < m.toList().size(); i++){
                Point p = m.toList().get(i);
                
                //If this is the first point, then don't bother trying to compare it and just continue to the next point
                if(i == 0){
                    lastPoint = p;
                    continue;
                }

                //if the difference in x values is bigger than 5
                //but the difference in y values is less than 5, then create a new shape (list) and add this point
                if(p.x - lastPoint.x > 5 && p.y - lastPoint.y < 5){
                    shapes.add(new ArrayList<Point>());
                    shapes.get(shapes.size() - 1).add(p);
                }
                //else if the difference in y values is less than 5, add it to the current shape
                else if(p.y - lastPoint.y < 5){
                    shapes.get(shapes.size() - 1).add(p);
                }
                //else, disregard this point because it is most likely an unrelated contour to the actual shapes
                else {
                    shapes.get(0).add(p);
                }
                lastPoint = p;
            }
        }

        /* After creating all the shapes, loop through them to check if any are made of only a few pixels, if so
        *  they shouldn't be counted as a shape and should be deleted. Also delete shapes that aren't the two we want
        *  to focus on (i.e. another hatch that is nearby but not the closest hatch that we want to go to)
        *
        *  NOTE: that we have to make another list to store the shapes to delete because removing things from 
        *  a list while iterating through that list will cause an error due to commodification
        */
        ArrayList<ArrayList<Point>> shapesToDelete = new ArrayList<ArrayList<Point>>();

        //Add shapes that need to be deleted
        for(int i = 0; i < shapes.size(); i++){
            
            ArrayList<Point> shape = shapes.get(i);
            
            if(shape.size() < 10){
                shapesToDelete.add(shape);
                continue;
            }
            int[] distances = DistancesBetweenShapes(shape, shapes.get(i + 1));
            if(i == 0 && distances[0] > distances[1]){
                shapesToDelete.add(shape);
            }

        }
        //Remove shapes that were added to shapesToDelete
        for(ArrayList<Point> shape : shapesToDelete){
            shapes.remove(shape);
        }
        return shapes;
    }

    public static int[] DistancesBetweenShapes(ArrayList<Point> s1, ArrayList<Point> s2){
        int[] distances = new int[2];

        //All of the points we will need to find the distances
        Point s1_highestPoint = s1.get(0);
        Point s1_lowestPoint = s1.get(0);
        Point s2_highestPoint = s2.get(0);
        Point s2_lowestPoint = s2.get(0);

        //Find the highest and lowest points in shape 1
        for(Point p : s1){
            if(p.y < s1_highestPoint.y){
                s1_highestPoint = p;
            }
            else if(p.y > s1_lowestPoint.y){
                s1_lowestPoint = p;
            }
        }
        //Find the highest and lowest points in shape 2
        for(Point p : s2){
            if(p.y < s2_highestPoint.y){
                s1_highestPoint = p;
            }
            else if(p.y > s1_lowestPoint.y){
                s1_lowestPoint = p;
            }
        }
        //Assign the distance between the highest points
        distances[0] = (int)s1_highestPoint.x - (int)s2_highestPoint.x;
        //Assign the distance between the lowest points
        distances[1] = (int)s1_lowestPoint.x - (int)s2_lowestPoint.x;

        return distances;
    }

    public static void DetermineX(ArrayList<ArrayList<Point>> shapes){
        
    }

}