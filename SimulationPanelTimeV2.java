// Compile: javac CircleV2.java WorkerTaskV2.java SimulationPanelTimeV2.java
// Run: javac CircleV2.java WorkerTaskV2.java SimulationPanelTimeV2.java && java SimulationPanelTimeV2

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public class SimulationPanelTimeV2 extends JPanel {

    private static final long serialVersionUID = 1L;

    // Simulation parameters

    // Width and height is the size of the window when the simulation is started
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int NUMBER_OF_CIRCLES = 2500;

    // Radius is how large the collision range is
    private static final int RADIUS = 10;
    private static final int THREAD_COUNT = 8;
    private static final int RUN_DURATION_SECS = 60;

    // Spatial grid
    // Cell size is the diameter so two circles in the same cell can always collide
    private static final int CELL_SIZE = RADIUS * 2;
    private static final int GRID_COLS = WIDTH  / CELL_SIZE + 1;
    private static final int GRID_ROWS = HEIGHT / CELL_SIZE + 1;

    // Pre-allocated grid: each cell holds a list of circle indices
    @SuppressWarnings("unchecked")
    private final ArrayList<Integer>[][] grid = new ArrayList[GRID_COLS][GRID_ROWS];

    // Shared array of all circles
    // Workers access parts of this shared array
    private final CircleV2[] circles = new CircleV2[NUMBER_OF_CIRCLES];

    // Worker threads
    // One worker per thread, each having a part of the shared circles array
    private final WorkerTaskV2[] workers = new WorkerTaskV2[THREAD_COUNT];

    // Raw threads backing each worker thread
    // They are kept as daemon threads so they die with the main process
    private final Thread[] workerThreads = new Thread[THREAD_COUNT];

    // FPS / timing
    private int frames = 0;
    private long lastFPSTime = System.currentTimeMillis();
    private int fps = 0;
    private long totalFrames = 0;
    private long simStart = System.currentTimeMillis();
    private double avgFps = 0;
    private Timer swingTimer;

    // Class function
    public SimulationPanelTimeV2() {
        // Sets the size of the window based on given width and height
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        // Sets the colour of the background
        setBackground(Color.BLACK);

        // Initialise grid cells
        for (int c = 0; c < GRID_COLS; c++)
            for (int r = 0; r < GRID_ROWS; r++)
                // Pre-allocate a list for every grid cell
                // This avoids allocation during the simulation loop
                grid[c][r] = new ArrayList<>();

        // Spawns a circle at a random position and velocity
        // Circle must spawn within the screen window and have a velocity that is not zero
        Random rng = new Random();
        for (int i = 0; i < NUMBER_OF_CIRCLES; i++) {
            double x = rng.nextInt(WIDTH  - 2 * RADIUS) + RADIUS;
            double y = rng.nextInt(HEIGHT - 2 * RADIUS) + RADIUS;
            double dx, dy;

            // Retry if velocity is zero
            do {
                dx = rng.nextDouble() * 4 - 2;
                dy = rng.nextDouble() * 4 - 2;
            } while (dx == 0 && dy == 0);

            // Circle generated is assigned a random colour
            Color color = new Color(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));

            // Store the new circle in the shared array at index i
            circles[i] = new CircleV2(x, y, RADIUS, color, dx, dy);
        }

        // Create workers based on the number of circles
        int base  = NUMBER_OF_CIRCLES / THREAD_COUNT;
        int extra = NUMBER_OF_CIRCLES % THREAD_COUNT;
        int start = 0;

        // Start each worker thread with string parsing
        for (int i = 0; i < THREAD_COUNT; i++) {
            int end = start + base + (i < extra ? 1 : 0);
            String params = start + "," + end + "," + WIDTH + "," + HEIGHT;
            workers[i] = new WorkerTaskV2(circles, params);
            workerThreads[i] = new Thread(workers[i]);
            workerThreads[i].setDaemon(true);
            workerThreads[i].start();
            start = end;
        }

        // FPS limiter
        // A delay of 33ms will cap the code to 30FPS
        swingTimer = new Timer(33, e -> gameLoop());
        swingTimer.start();
    }

    // Game loop
    private void gameLoop() {
        long now = System.currentTimeMillis();

        // Stop after the configured duration
        if ((now - simStart) / 1000 >= RUN_DURATION_SECS) {
            swingTimer.stop();
            for (WorkerTaskV2 w : workers) w.stop();
            System.out.println("Simulation complete after " + RUN_DURATION_SECS + " seconds.");
            System.out.printf("Final average FPS: %.1f%n", avgFps);
            return;
        }

        // Step 1:
        // Signal all workers to move and bounce their slice
        for (WorkerTaskV2 w : workers) w.signal();

        // Wait for all workers to finish before touching the array
        for (WorkerTaskV2 w : workers) w.await();

        // Step 2:
        // Clear all cells (reuse the lists to avoid allocation overhead)
        for (int c = 0; c < GRID_COLS; c++)
            for (int r = 0; r < GRID_ROWS; r++)
                grid[c][r].clear();

        // Map each circle to its grid cell based on its current position
        for (int i = 0; i < circles.length; i++) {
            int col = (int)(circles[i].getX() / CELL_SIZE);
            int row = (int)(circles[i].getY() / CELL_SIZE);

            // Clamp to grid bounds 
            // Used in case a circle strays slightly outside the screen
            col = Math.max(0, Math.min(col, GRID_COLS - 1));
            row = Math.max(0, Math.min(row, GRID_ROWS - 1));
            grid[col][row].add(i);
        }

        // Step 3:
        // For each circle, only examine circles in the same cell and the 8 surrounding cells
        // O(n) on average instead of O(n²).
        handleCollisions();

        // Step 4:
        // Renders the window again after all collsions have occured
        repaint();

        // FPS tracking
        frames++;
        totalFrames++;
        if (now - lastFPSTime >= 1000) {
            fps = frames;
            frames = 0;
            lastFPSTime = now;
            double elapsed = (now - simStart) / 1000.0;
            avgFps = totalFrames / elapsed;
        }
    }

    // Collision detection
    private void handleCollisions() {
        for (int col = 0; col < GRID_COLS; col++) {
            for (int row = 0; row < GRID_ROWS; row++) {
                ArrayList<Integer> cell = grid[col][row];
                if (cell.isEmpty()) continue;

                // Check every circle in this cell against neighbours in the 3×3 block
                for (int ci = 0; ci < cell.size(); ci++) {
                    int i = cell.get(ci);

                    // Check all neighbouring columns, skipping any that fall outside the grid
                    for (int dc = -1; dc <= 1; dc++) {
                        int nc = col + dc;
                        if (nc < 0 || nc >= GRID_COLS) continue;

                        // Check all neighbouring rows, skipping any that fall outside the grid
                        for (int dr = -1; dr <= 1; dr++) {
                            int nr = row + dr;
                            if (nr < 0 || nr >= GRID_ROWS) continue;

                            // Check each circle in the neighbouring cell for a potential collision
                            ArrayList<Integer> neighbour = grid[nc][nr];
                            for (int cj = 0; cj < neighbour.size(); cj++) {
                                int j = neighbour.get(cj);

                                // Only process each pair once (lower index first)
                                if (j <= i) continue;

                                // Resolve the collision between the two overlapping circles
                                resolveCollision(circles[i], circles[j]);
                            }
                        }
                    }
                }
            }
        }
    }

    // Calculate the distance between centres
    private void resolveCollision(CircleV2 a, CircleV2 b) {
        double dx   = b.getX() - a.getX();
        double dy   = b.getY() - a.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);

        // Calculate the minimum distance before overlap
        double minD = a.getRadius() + b.getRadius();

        // No collision occurs if the distance is zero or less than the minimum distance
        if (dist == 0 || dist >= minD) return;

        // Normalise the displacement vector to get the collision axis
        double nx = dx / dist;
        double ny = dy / dist;

        // Apply elastic collision impulse along the collision axis
        double p = (a.getDx() - b.getDx()) * nx + (a.getDy() - b.getDy()) * ny;
        a.setDx(a.getDx() - p * nx);
        a.setDy(a.getDy() - p * ny);
        b.setDx(b.getDx() + p * nx);
        b.setDy(b.getDy() + p * ny);

        // Separate overlapping circles
        double overlap = (minD - dist) / 2;
        a.setX(a.getX() - nx * overlap);
        a.setY(a.getY() - ny * overlap);
        b.setX(b.getX() + nx * overlap);
        b.setY(b.getY() + ny * overlap);
    }

    // Rendering user interface elements
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // For each circle in the circles array, draw them
        for (CircleV2 c : circles) c.draw(g);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("FPS: " + fps, 10, 20);
        g.drawString("Avg FPS: " + String.format("%.1f", avgFps), 10, 40);
        g.drawString("Threads: " + THREAD_COUNT, 10, 60);
        g.drawString("Circles: " + NUMBER_OF_CIRCLES, 10, 80);
        long elapsed = (System.currentTimeMillis() - simStart) / 1000;
        long remaining = RUN_DURATION_SECS - elapsed;
        g.drawString("Time left: " + remaining + "s", 10, 100);
    }

    // Main class
    public static void main(String[] args) {
        JFrame frame = new JFrame("Colliding Bouncing Circles - Multithreaded");
        SimulationPanelTimeV2 panel = new SimulationPanelTimeV2();
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
