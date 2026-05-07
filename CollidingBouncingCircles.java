import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class CollidingBouncingCircles extends JPanel {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int NUMBER_OF_CIRCLES = 100;
    private static final int RUN_DURATION_SECS = 60;

    // FPS / timing
    private int frames = 0;
    private int fps = 0;
    private long simStart = System.currentTimeMillis();
    private double avgFps = 0;
    private Timer swingTimer;

    static class Circle {
        double x, y; // use double for smoother collision physics
        double dx, dy;
        int radius;
        Color color;

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

            // Bounce on walls
            if (x - radius <= 0 || x + radius >= panelWidth) dx = -dx;
            if (y - radius <= 0 || y + radius >= panelHeight) dy = -dy;
        }

        public void draw(Graphics g) {
            g.setColor(color);
            g.fillOval((int)(x - radius), (int)(y - radius), radius*2, radius*2);
        }
    }

    private Circle[] circles;
    private Timer timer;

    // FPS calculation
    private long lastFPSCheck = System.currentTimeMillis();

    public CollidingBouncingCircles() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);

        Random random = new Random();
        circles = new Circle[NUMBER_OF_CIRCLES];

        for (int i = 0; i < NUMBER_OF_CIRCLES; i++) {
            int radius = 10;//random.nextInt(20) + 10;
            double x = random.nextInt(WIDTH - 2*radius) + radius;
            double y = random.nextInt(HEIGHT - 2*radius) + radius;

            double dx, dy;
            do {
                dx = random.nextDouble() * 4 - 2; // -2 to 2
                dy = random.nextDouble() * 4 - 2;
            } while (dx == 0 && dy == 0);

            Color color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));

            circles[i] = new Circle(x, y, radius, color, dx, dy);
        }

        timer = new Timer(33, e -> {
            long now = System.currentTimeMillis();
            // Stop after the configured duration
            if ((now - simStart) / 1000 >= RUN_DURATION_SECS) {
            swingTimer.stop();
            System.out.println("Simulation complete after " + RUN_DURATION_SECS + " seconds.");
            System.out.printf("Final average FPS: %.1f%n", avgFps);
            return;
        }

            // Move circles
            for (Circle c : circles) {
                c.move(getWidth(), getHeight());
            }

            // Handle collisions
            handleCollisions();

            repaint();

            // FPS counter
            frames++;
            if (now - lastFPSCheck >= 1000) {
                fps = frames;
                frames = 0;
                lastFPSCheck = now;
            }
        });

        timer.start();
    }

    private void handleCollisions() {
        for (int i = 0; i < circles.length; i++) {
            for (int j = i + 1; j < circles.length; j++) {
                Circle a = circles[i];
                Circle b = circles[j];

                double dx = b.x - a.x;
                double dy = b.y - a.y;
                double dist = Math.sqrt(dx*dx + dy*dy);
                double minDist = a.radius + b.radius;

                if (dist < minDist) {
                    // Simple elastic collision approximation: swap velocities along the collision vector
                    double nx = dx / dist;
                    double ny = dy / dist;

                    double p = 2 * (a.dx * nx + a.dy * ny - b.dx * nx - b.dy * ny) / 2;

                    a.dx = a.dx - p * nx;
                    a.dy = a.dy - p * ny;
                    b.dx = b.dx + p * nx;
                    b.dy = b.dy + p * ny;

                    // Move circles so they are no longer overlapping
                    double overlap = minDist - dist;
                    a.x -= nx * overlap / 2;
                    a.y -= ny * overlap / 2;
                    b.x += nx * overlap / 2;
                    b.y += ny * overlap / 2;
                }
            }
        }
    }

    // Rendering user interface elements
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // For each circle in the circles array, draw them
        for (Circle c : circles) c.draw(g);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("FPS: " + fps, 10, 20);
        g.drawString("Avg FPS: " + String.format("%.1f", avgFps), 10, 40);
        g.drawString("Circles: " + NUMBER_OF_CIRCLES, 10, 80);
        long elapsed = (System.currentTimeMillis() - simStart) / 1000;
        long remaining = RUN_DURATION_SECS - elapsed;
        g.drawString("Time left: " + remaining + "s", 10, 100);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Colliding Bouncing Circles");
        CollidingBouncingCircles panel = new CollidingBouncingCircles();

        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}