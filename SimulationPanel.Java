// To run the code: javac Circle.java WorkerTask.java SimulationPanel.java in terminal
// Then run: java -cp . SimulationPanel.java in terminal

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class SimulationPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int NUMBER_OF_CIRCLES = 50;
    private static final int MAX_ITERATIONS = 5000;
    private static final int THREAD_COUNT = 2;

    private Circle[] circles;
    private Timer timer;
    private int iterations = 0;

    // Thread pool
    private final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_COUNT);

    // FPS tracking
    private int frames = 0;
    private long lastFPSCheck = System.currentTimeMillis();
    private int fps = 0;

    // Average FPS tracking
    private long totalFrames = 0;
    private long simulationStart = System.currentTimeMillis();
    private double avgFps = 0;

    public SimulationPanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);

        Random random = new Random();
        circles = new Circle[NUMBER_OF_CIRCLES];

        for (int i = 0; i < NUMBER_OF_CIRCLES; i++) {
            int radius = 10;
            double x = random.nextInt(WIDTH - 2 * radius) + radius;
            double y = random.nextInt(HEIGHT - 2 * radius) + radius;

            double dx, dy;
            do {
                dx = random.nextDouble() * 4 - 2;
                dy = random.nextDouble() * 4 - 2;
            } while (dx == 0 && dy == 0);

            Color color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            circles[i] = new Circle(x, y, radius, color, dx, dy);
        }

        timer = new Timer(0, e -> {
            if (iterations >= MAX_ITERATIONS) {
                timer.stop();
                threadPool.shutdown();
                System.out.println("Simulation complete. Final average FPS: " + String.format("%.1f", avgFps));
                return;
            }

            try {
                // Step 1 - split circles into batches and serialise
                int batchSize = circles.length / THREAD_COUNT;
                List<Future<List<String>>> futures = new ArrayList<>();

                for (int i = 0; i < THREAD_COUNT; i++) {
                    int start = i * batchSize;
                    // Last thread takes any remainder
                    int end = (i == THREAD_COUNT - 1) ? circles.length : start + batchSize;

                    List<String> batch = new ArrayList<>();
                    for (int j = start; j < end; j++) {
                        batch.add(circles[j].serialise());
                    }

                    // Step 2 - submit batch to thread pool
                    futures.add(threadPool.submit(new WorkerTask(batch, getWidth(), getHeight())));
                }

                // Step 3 - collect results and deserialise back into circles array
                int index = 0;
                for (Future<List<String>> future : futures) {
                    for (String s : future.get()) {
                        circles[index++] = Circle.deserialise(s);
                    }
                }

            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }

            // Step 4 - handle collisions centrally
            handleCollisions();

            iterations++;
            repaint();

            // FPS tracking
            frames++;
            totalFrames++;
            long now = System.currentTimeMillis();
            if (now - lastFPSCheck >= 1000) {
                fps = frames;
                frames = 0;
                lastFPSCheck = now;

                double elapsedSeconds = (now - simulationStart) / 1000.0;
                avgFps = totalFrames / elapsedSeconds;
                System.out.println("Current FPS: " + fps + " | Average FPS: " + String.format("%.1f", avgFps));
            }
        });

        timer.start();
    }

    private void handleCollisions() {
        for (int i = 0; i < circles.length; i++) {
            for (int j = i + 1; j < circles.length; j++) {
                Circle a = circles[i];
                Circle b = circles[j];

                double dx = b.getX() - a.getX();
                double dy = b.getY() - a.getY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                double minDist = a.getRadius() + b.getRadius();

                if (dist < minDist) {
                    double nx = dx / dist;
                    double ny = dy / dist;

                    double p = 2 * (a.getDx() * nx + a.getDy() * ny - b.getDx() * nx - b.getDy() * ny) / 2;

                    a.setDx(a.getDx() - p * nx);
                    a.setDy(a.getDy() - p * ny);
                    b.setDx(b.getDx() + p * nx);
                    b.setDy(b.getDy() + p * ny);

                    double overlap = minDist - dist;
                    a.setX(a.getX() - nx * overlap / 2);
                    a.setY(a.getY() - ny * overlap / 2);
                    b.setX(b.getX() + nx * overlap / 2);
                    b.setY(b.getY() + ny * overlap / 2);
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (Circle c : circles) {
            c.draw(g);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("FPS: " + fps, 10, 20);
        g.drawString("Avg FPS: " + String.format("%.1f", avgFps), 10, 40);
        g.drawString("Threads: " + THREAD_COUNT, 10, 60);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Colliding Bouncing Circles - Multithreaded");
        SimulationPanel panel = new SimulationPanel();

        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}