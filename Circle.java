import java.awt.Color;
import java.awt.Graphics;

public class Circle {
	
	private int x;
    private int y;
    private int radius;
    private Color color;
    private int dx, dy;

    public Circle(int x, int y, int radius, Color color, int dx, int dy) {
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

        // Bounce on left/right walls
        if (x - radius <= 0 || x + radius >= panelWidth) {
            dx = -dx;
        }

        // Bounce on top/bottom walls
        if (y - radius <= 0 || y + radius >= panelHeight) {
            dy = -dy;
        }
    }
    
    
    public void draw(Graphics g) {
        g.setColor(color);
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getRadius() { return radius; }
}