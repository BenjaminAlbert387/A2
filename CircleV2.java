import java.awt.*;

public class CircleV2 {
    private double x, y;
    private double dx, dy;
    private final int radius;
    private final Color color;

    public CircleV2(double x, double y, int radius, Color color, double dx, double dy) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.dx = dx;
        this.dy = dy;
    }

    public double getX()     { return x; }
    public double getY()     { return y; }
    public double getDx()    { return dx; }
    public double getDy()    { return dy; }
    public int    getRadius(){ return radius; }
    public Color  getColor() { return color; }

    public void setX(double x)   { this.x = x; }
    public void setY(double y)   { this.y = y; }
    public void setDx(double dx) { this.dx = dx; }
    public void setDy(double dy) { this.dy = dy; }

    public void draw(Graphics g) {
        g.setColor(color);
        g.fillOval((int)(x - radius), (int)(y - radius), radius * 2, radius * 2);
    }
}
