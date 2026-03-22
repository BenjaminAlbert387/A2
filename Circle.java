// Importing 
import java.awt.Color;
import java.awt.Graphics;

// Create a public class
public class Circle {

    // Variables for each circle
    private double x, y;
    private double dx, dy;
    private int radius;
    private Color color;

    // Initialises a circle with position (x, y), size, colour, and velocity (dx, dy)
    public Circle(double x, double y, int radius, Color color, double dx, double dy) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.dx = dx;
        this.dy = dy;
    }

    // Updates position by velocity and reverses direction if circle hits a wall or another circle
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
    public double getX() { return x; }
    public double getY() { return y; }
    public double getDx() { return dx; }
    public double getDy() { return dy; }
    public int getRadius() { return radius; }
    public Color getColor() { return color; }

    // Setters
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setDx(double dx) { this.dx = dx; }
    public void setDy(double dy) { this.dy = dy; }

    // Serialise method to string for inter-thread communication
    public String serialise() {
        return x + "," + y + "," + dx + "," + dy + "," + radius + ","
             + color.getRed() + "," + color.getGreen() + "," + color.getBlue();
    }

    // Deserialise method from string
    public static Circle deserialise(String s) {
        String[] parts = s.split(",");
        double x      = Double.parseDouble(parts[0]);
        double y      = Double.parseDouble(parts[1]);
        double dx     = Double.parseDouble(parts[2]);
        double dy     = Double.parseDouble(parts[3]);
        int radius    = Integer.parseInt(parts[4]);
        int r         = Integer.parseInt(parts[5]);
        int g         = Integer.parseInt(parts[6]);
        int b         = Integer.parseInt(parts[7]);
        return new Circle(x, y, radius, new Color(r, g, b), dx, dy);
    }
}