import org.opencv.core.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class Shape {

    private ArrayList<Point> points;
    
    //These point variables are caches for their respective functions which is why they are private
    public Point highestPoint;
    public Point lowestPoint;
    public Point rightMostPoint;
    public Point leftMostPoint;
    public Point middlePoint;

    //Boolean for the rotation of the shape, if true, its leaning right, if false, leaning left
    public boolean leansRight;

    public Shape(){

    }
    public Shape(Collection<? extends Point> points){
        this.points = new ArrayList<>();
        this.points.addAll(points);
    }
    public Shape(Point[] points){
        this.points = new ArrayList<>();
        this.points.addAll(Arrays.asList(points));
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

    public Point get(int index){
        return points.get(index);
    }
    public void add(Point p){
        points.add(p);
    }
    public void addAll(Collection<? extends Point> points){
        if(this.points == null){
            this.points = new ArrayList<>();
        }
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