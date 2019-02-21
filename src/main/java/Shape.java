import org.opencv.core.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class Shape {

    private ArrayList<Point> points = new ArrayList<>();
    
    //These point variables are caches for their respective functions which is why they are private
    private Point highestPoint;
    private Point lowestPoint;
    private Point rightMostPoint;
    private Point leftMostPoint;
    private Point middlePoint;

    //Boolean for the rotation of the shape, if true, its leaning right, if false, leaning left
    private Boolean leansRight;

    public Shape(){

    }
    public Shape(ArrayList<Point> points){
        this.points.addAll(points);
    }
    public Shape(Point[] points){
        this.points.addAll(Arrays.asList(points));
    }

    public Point highestPoint(){
        if(highestPoint == null){
            highestPoint = points.get(0);
            for(Point p : points){
                if(p.y < highestPoint.y){
                    highestPoint = p;
                }
            }
        }
        
        return highestPoint;
    }

    public Point lowestPoint(){
        if(lowestPoint == null){
            lowestPoint = points.get(0);
            for(Point p : points){
                if(p.y > lowestPoint.y){
                    lowestPoint = p;
                }
            }
        }
        return lowestPoint;
    }

    public Point rightMostPoint(){
        if(rightMostPoint == null){
            rightMostPoint = points.get(points.size() - 1);
        }
        return rightMostPoint;
    }

    public Point leftMostPoint(){
        if(leftMostPoint == null){
            leftMostPoint = points.get(0);
        }
        return leftMostPoint;
    }

    public Point middlePoint(){
        if(middlePoint == null){
            int xTotal = 0;
            int yTotal = 0;
            for(Point p : points){
                xTotal += p.x;
                yTotal += p.y;
            }
            middlePoint = new Point(xTotal / points.size(), yTotal / points.size());
        }
        return middlePoint;
    }

    public boolean leansRight(){
        if(leansRight == null && rightMostPoint().y < leftMostPoint().y){
            leansRight = true;
            return leansRight;
        }
        else if(leansRight == null){
            leansRight = false;
            return leansRight;
        }
        return leansRight;
    }

    public Point get(int index){
        return points.get(index);
    }
    public void add(Point p){
        points.add(p);
    }
    public void addAll(Collection<? extends Point> points){
        this.points.addAll(points);
    }
    public void remove(Point p){
        if(points.contains(p)){
            points.remove(p);
        }
    }
    public int size(){
        return points.size();
    }
    
}