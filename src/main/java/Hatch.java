public class Hatch {
    
    Shape s1;
    Shape s2;

    public Hatch(Shape s1, Shape s2){
        this.s1 = s1;
        this.s2 = s2;
    }
    public double middleX(){
        return (s1.rightMostPoint.x + s2.leftMostPoint.x) / 2;
    }
}