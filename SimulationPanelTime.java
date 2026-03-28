// Run the following line of code first: javac Circle.java WorkQueue.java WorkerTask.java SimulationPanelTime.java

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class SimulationPanelTime extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int NUMBER_OF_CIRCLES = 50;
    private static final int RUN_DURATION_SECONDS = 60;
    private static final int THREAD_COUNT = 2;
    private static final int TARGET_FPS = 30;
    private static final long FRAME_TIME_MS = 1000 / TARGET_FPS;

    private Circle[] circles;
    private volatile boolean running = true;

    private final WorkQueue inputQueue = new WorkQueue(THREAD_COUNT * 2);
    private final WorkQueue resultQueue = new WorkQueue(THREAD_COUNT * 2);

    private final WorkerTask[] workerTasks = new WorkerTask[THREAD_COUNT];
    private final Thread[] workerThreads = new Thread[THREAD_COUNT];

    // FPS tracking
    private volatile int fps = 0;
    private volatile double avgFps = 0;
    private int frames = 0;
    private long lastFPSCheck = System.currentTimeMillis();
    private long totalFrames = 0;
    private long simulationStart = System.currentTimeMillis();

    public SimulationPanelTime() {
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

        // Start worker threads
        for (int i = 0; i < THREAD_COUNT; i++) {
            workerTasks[i] = new WorkerTask(inputQueue, resultQueue, WIDTH, HEIGHT);
            workerThreads[i] = new Thread(workerTasks[i]);
            workerThreads[i].setDaemon(true);
            workerThreads[i].start();
        }

        // Game loop thread - replaces javax.swing.Timer
        Thread gameLoop = new Thread(() -> {
            simulationStart = System.currentTimeMillis();
            lastFPSCheck = simulationStart;

            while (running) {
                long frameStart = System.currentTimeMillis();
                long elapsed = (frameStart - simulationStart) / 1000;

                if (elapsed >= RUN_DURATION_SECONDS) {
                    running = false;
                    for (WorkerTask task : workerTasks) task.stop();
                    System.out.println("Simulation complete after " + RUN_DURATION_SECONDS + " seconds.");
                    System.out.println("Final average FPS: " + String.format("%.1f", avgFps));
                    break;
                }

                // Step 1 - split circles into batches and enqueue
                int batchSize = circles.length / THREAD_COUNT;
                for (int i = 0; i < THREAD_COUNT; i++) {
                    int start = i * batchSize;
                    int end = (i == THREAD_COUNT - 1) ? circles.length : start + batchSize;

                    StringBuilder batch = new StringBuilder();
                    for (int j = start; j < end; j++) {
                        if (j > start) batch.append(";");
                        batch.append(circles[j].serialise());
                    }
                    inputQueue.enqueue(batch.toString());
                }

                // Step 2 - collect results
                int index = 0;
                for (int i = 0; i < THREAD_COUNT; i++) {
                    String result = (String) resultQueue.dequeue();
                    String[] parts = result.split(";");
                    for (String s : parts) {
                        if (!s.isEmpty()) {
                            circles[index++] = Circle.deserialise(s);
                        }
                    }
                }

                // Step 3 - handle collisions on main loop thread
                handleCollisions();

                // Step 4 - repaint on EDT
                SwingUtilities.invokeLater(this::repaint);

                // FPS tracking
                frames++;
                totalFrames++;
                long now = System.currentTimeMillis();
                if (now - lastFPSCheck >= 1000) {
                    fps = frames;
                    frames = 0;
                    lastFPSCheck = now;

                    double elapsedSecs = (now - simulationStart) / 1000.0;
                    avgFps = totalFrames / elapsedSecs;
                    System.out.println("Current FPS: " + fps + " | Average FPS: " + String.format("%.1f", avgFps));
                }

                // Sleep for remainder of frame budget
                long frameElapsed = System.currentTimeMillis() - frameStart;
                long sleepTime = FRAME_TIME_MS - frameElapsed;
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });

        gameLoop.setDaemon(true);
        gameLoop.start();
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

                if (dist < minDist && dist > 0) {
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

        Circle[] snapshot = circles;
        for (Circle c : snapshot) {
            c.draw(g);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("FPS: " + fps, 10, 20);
        g.drawString("Avg FPS: " + String.format("%.1f", avgFps), 10, 40);
        g.drawString("Threads: " + THREAD_COUNT, 10, 60);

        long elapsed = (System.currentTimeMillis() - simulationStart) / 1000;
        long remaining = RUN_DURATION_SECONDS - elapsed;
        g.drawString("Time remaining: " + remaining + "s", 10, 80);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Colliding Bouncing Circles - Multithreaded");
            SimulationPanelTime panel = new SimulationPanelTime();

            frame.add(panel);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}