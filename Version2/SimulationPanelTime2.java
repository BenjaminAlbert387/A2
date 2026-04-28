package Version2;

// Compile: javac Circle.java CircleBatch.java WorkQueue.java WorkerTask.java SimulationPanelTime.java
// Run:     java SimulationPanelTime

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class SimulationPanelTime2 extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int WIDTH               = 1280;
    private static final int HEIGHT              = 720;
    private static final int NUMBER_OF_CIRCLES   = 3000;
    private static final int RUN_DURATION_SECONDS = 60;
    private static final int THREAD_COUNT        = 4;

    private Circle[] circles;
    private Timer timer;

    // One input and result queue per worker
    private final WorkQueue[] inputQueues  = new WorkQueue[THREAD_COUNT];
    private final WorkQueue[] resultQueues = new WorkQueue[THREAD_COUNT];

    private final WorkerTask[] workerTasks   = new WorkerTask[THREAD_COUNT];
    private final Thread[]     workerThreads = new Thread[THREAD_COUNT];

    // FPS tracking
    private int  frames       = 0;
    private long lastFPSCheck = System.currentTimeMillis();
    private int  fps          = 0;

    // Average FPS tracking
    private long   totalFrames      = 0;
    private long   simulationStart  = System.currentTimeMillis();
    private double avgFps           = 0;

    public SimulationPanelTime2() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);

        Random random = new Random();
        circles = new Circle[NUMBER_OF_CIRCLES];

        for (int i = 0; i < NUMBER_OF_CIRCLES; i++) {
            int radius = 10;
            double x = random.nextInt(WIDTH  - 2 * radius) + radius;
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
            inputQueues[i]  = new WorkQueue(4);
            resultQueues[i] = new WorkQueue(4);
            workerTasks[i]  = new WorkerTask(inputQueues[i], resultQueues[i], WIDTH, HEIGHT);
            workerThreads[i] = new Thread(workerTasks[i]);
            workerThreads[i].setDaemon(true);
            workerThreads[i].start();
        }

        timer = new Timer(0, e -> {  // 0ms delay = as fast as possible
            long now = System.currentTimeMillis();
            long elapsedSeconds = (now - simulationStart) / 1000;

            if (elapsedSeconds >= RUN_DURATION_SECONDS) {
                timer.stop();
                for (WorkerTask task : workerTasks) task.stop();
                System.out.println("Simulation complete after " + RUN_DURATION_SECONDS + " seconds.");
                System.out.println("Final average FPS: " + String.format("%.1f", avgFps));
                return;
            }

            // Step 1 - send each worker a CircleBatch pointing at its slice of the shared array
            int batchSize = circles.length / THREAD_COUNT;
            for (int i = 0; i < THREAD_COUNT; i++) {
                int start = i * batchSize;
                int end   = (i == THREAD_COUNT - 1) ? circles.length : start + batchSize;
                inputQueues[i].enqueue(new CircleBatch(circles, start, end));
            }

            // Step 2 - wait for all workers to finish (result queue acts as a done signal)
            for (int i = 0; i < THREAD_COUNT; i++) {
                while (resultQueues[i].isEmpty()) {
                    Thread.yield();
                }
                resultQueues[i].dequeue(); // discard - circles array already updated in place
            }

            // Step 3 - handle collisions centrally on main thread
            handleCollisions();

            repaint();

            // FPS tracking
            frames++;
            totalFrames++;
            if (now - lastFPSCheck >= 1000) {
                fps = frames;
                frames = 0;
                lastFPSCheck = now;

                double elapsedSecs = (now - simulationStart) / 1000.0;
                avgFps = totalFrames / elapsedSecs;
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

                double dx   = b.getX() - a.getX();
                double dy   = b.getY() - a.getY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                double minDist = a.getRadius() + b.getRadius();

                if (dist < minDist && dist > 0) {
                    double nx = dx / dist;
                    double ny = dy / dist;

                    double p = 2 * (a.getDx() * nx + a.getDy() * ny
                                  - b.getDx() * nx - b.getDy() * ny) / 2;

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
        g.drawString("Circles: " + NUMBER_OF_CIRCLES, 10, 80);

        long elapsed   = (System.currentTimeMillis() - simulationStart) / 1000;
        long remaining = RUN_DURATION_SECONDS - elapsed;
        g.drawString("Time remaining: " + remaining + "s", 10, 100);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Colliding Bouncing Circles - Multithreaded");
        SimulationPanelTime2 panel = new SimulationPanelTime2();

        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
