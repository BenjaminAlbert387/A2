package Version2;

import java.awt.Color;
import java.awt.Graphics;

public class Circle {

    private double x, y;
    private double dx, dy;
    private int radius;
    private Color color;

    public Circle(double x, double y, int radius, Color color, double dx, double dy) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.dx = dx;
        this.dy = dy;
    }

    public void move(int panelWidth, int panelHeight) {
        x += dx;
        y += dy;

        if (x - radius <= 0 || x + radius >= panelWidth) dx = -dx;
        if (y - radius <= 0 || y + radius >= panelHeight) dy = -dy;
    }

    public void draw(Graphics g) {
        g.setColor(color);
        g.fillOval((int)(x - radius), (int)(y - radius), radius * 2, radius * 2);
    }

    // Getters
    public double getX()    { return x; }
    public double getY()    { return y; }
    public double getDx()   { return dx; }
    public double getDy()   { return dy; }
    public int getRadius()  { return radius; }
    public Color getColor() { return color; }

    // Setters
    public void setX(double x)   { this.x = x; }
    public void setY(double y)   { this.y = y; }
    public void setDx(double dx) { this.dx = dx; }
    public void setDy(double dy) { this.dy = dy; }
}
