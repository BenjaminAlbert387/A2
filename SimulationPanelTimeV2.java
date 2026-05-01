// Compile: javac CircleV2V2.java WorkerTaskV2V2.java SimulationPanelTimeV2.java
// Run:     java SimulationPanelTimeV2

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public class SimulationPanelTimeV2 extends JPanel {

    private static final long serialVersionUID = 1L;

    // ── Simulation parameters ────────────────────────────────────────────────
    private static final int WIDTH              = 1280;
    private static final int HEIGHT             = 720;
    private static final int NUMBER_OF_CIRCLES  = 1000;
    private static final int RADIUS             = 10;
    private static final int THREAD_COUNT       = 4;
    private static final int RUN_DURATION_SECS  = 60;

    // ── Spatial grid ─────────────────────────────────────────────────────────
    // Cell size = diameter so two circles in the same cell can always collide,
    // and we only need to check the 3x3 neighbourhood around each cell.
    private static final int CELL_SIZE = RADIUS * 2;
    private static final int GRID_COLS = WIDTH  / CELL_SIZE + 1;
    private static final int GRID_ROWS = HEIGHT / CELL_SIZE + 1;

    // Pre-allocated grid: each cell holds a list of circle indices
    @SuppressWarnings("unchecked")
    private final ArrayList<Integer>[][] grid = new ArrayList[GRID_COLS][GRID_ROWS];

    // ── CircleV2s ──────────────────────────────────────────────────────────────
    private final CircleV2[] circles = new CircleV2[NUMBER_OF_CIRCLES];

    // ── Workers ──────────────────────────────────────────────────────────────
    private final WorkerTaskV2V2[] workers       = new WorkerTaskV2V2[THREAD_COUNT];
    private final Thread[]     workerThreads = new Thread[THREAD_COUNT];

    // ── FPS / timing ─────────────────────────────────────────────────────────
    private int    frames      = 0;
    private long   lastFPSTime = System.currentTimeMillis();
    private int    fps         = 0;
    private long   totalFrames = 0;
    private long   simStart    = System.currentTimeMillis();
    private double avgFps      = 0;

    private Timer swingTimer;

    // ────────────────────────────────────────────────────────────────────────
    public SimulationPanelTimeV2() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);

        // Initialise grid cells
        for (int c = 0; c < GRID_COLS; c++)
            for (int r = 0; r < GRID_ROWS; r++)
                grid[c][r] = new ArrayList<>();

        // Spawn circles at random positions with random velocities
        Random rng = new Random();
        for (int i = 0; i < NUMBER_OF_CIRCLES; i++) {
            double x = rng.nextInt(WIDTH  - 2 * RADIUS) + RADIUS;
            double y = rng.nextInt(HEIGHT - 2 * RADIUS) + RADIUS;
            double dx, dy;
            do {
                dx = rng.nextDouble() * 4 - 2;
                dy = rng.nextDouble() * 4 - 2;
            } while (dx == 0 && dy == 0);
            Color color = new Color(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
            circles[i] = new CircleV2(x, y, RADIUS, color, dx, dy);
        }

        // Create workers, each responsible for a contiguous slice of the array
        int base  = NUMBER_OF_CIRCLES / THREAD_COUNT;
        int extra = NUMBER_OF_CIRCLES % THREAD_COUNT;
        int start = 0;
        for (int i = 0; i < THREAD_COUNT; i++) {
            int end = start + base + (i < extra ? 1 : 0);
            workers[i]       = new WorkerTaskV2V2(circles, start, end, WIDTH, HEIGHT);
            workerThreads[i] = new Thread(workers[i]);
            workerThreads[i].setDaemon(true);
            workerThreads[i].start();
            start = end;
        }

        // Main game loop via Swing Timer (runs on the EDT)
        swingTimer = new Timer(0, e -> gameLoop());
        swingTimer.start();
    }

    // ── Game loop ────────────────────────────────────────────────────────────

    private void gameLoop() {
        long now = System.currentTimeMillis();

        // Stop after the configured duration
        if ((now - simStart) / 1000 >= RUN_DURATION_SECS) {
            swingTimer.stop();
            for (WorkerTaskV2V2 w : workers) w.stop();
            System.out.println("Simulation complete after " + RUN_DURATION_SECS + " seconds.");
            System.out.printf("Final average FPS: %.1f%n", avgFps);
            return;
        }

        // ── Step 1: parallel movement ────────────────────────────────────────
        // Signal all workers to move+bounce their slice
        for (WorkerTaskV2V2 w : workers) w.signal();
        // Wait for all workers to finish before touching the array
        for (WorkerTaskV2V2 w : workers) w.await();

        // ── Step 2: rebuild spatial grid ────────────────────────────────────
        // Clear all cells (reuse the lists to avoid allocation)
        for (int c = 0; c < GRID_COLS; c++)
            for (int r = 0; r < GRID_ROWS; r++)
                grid[c][r].clear();

        for (int i = 0; i < circles.length; i++) {
            int col = (int)(circles[i].getX() / CELL_SIZE);
            int row = (int)(circles[i].getY() / CELL_SIZE);
            // Clamp to grid bounds (shouldn't be needed if wall bounce is correct)
            col = Math.max(0, Math.min(col, GRID_COLS - 1));
            row = Math.max(0, Math.min(row, GRID_ROWS - 1));
            grid[col][row].add(i);
        }

        // ── Step 3: collision detection via grid ─────────────────────────────
        // For each circle we only examine circles in the same cell and the 8
        // surrounding cells — O(n) on average instead of O(n²).
        handleCollisions();

        // ── Step 4: repaint ───────────────────────────────────────────────────
        repaint();

        // ── FPS tracking ─────────────────────────────────────────────────────
        frames++;
        totalFrames++;
        if (now - lastFPSTime >= 1000) {
            fps         = frames;
            frames      = 0;
            lastFPSTime = now;
            double elapsed = (now - simStart) / 1000.0;
            avgFps = totalFrames / elapsed;
        }
    }

    // ── Collision detection ──────────────────────────────────────────────────

    private void handleCollisions() {
        for (int col = 0; col < GRID_COLS; col++) {
            for (int row = 0; row < GRID_ROWS; row++) {
                ArrayList<Integer> cell = grid[col][row];
                if (cell.isEmpty()) continue;

                // Check every circle in this cell against neighbours in the 3×3 block
                for (int ci = 0; ci < cell.size(); ci++) {
                    int i = cell.get(ci);

                    for (int dc = -1; dc <= 1; dc++) {
                        int nc = col + dc;
                        if (nc < 0 || nc >= GRID_COLS) continue;

                        for (int dr = -1; dr <= 1; dr++) {
                            int nr = row + dr;
                            if (nr < 0 || nr >= GRID_ROWS) continue;

                            ArrayList<Integer> neighbour = grid[nc][nr];
                            for (int cj = 0; cj < neighbour.size(); cj++) {
                                int j = neighbour.get(cj);

                                // Only process each pair once (lower index first)
                                if (j <= i) continue;

                                resolveCollision(circles[i], circles[j]);
                            }
                        }
                    }
                }
            }
        }
    }

    private void resolveCollision(CircleV2 a, CircleV2 b) {
        double dx   = b.getX() - a.getX();
        double dy   = b.getY() - a.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);
        double minD = a.getRadius() + b.getRadius();

        if (dist == 0 || dist >= minD) return;  // no collision (or degenerate)

        double nx = dx / dist;
        double ny = dy / dist;

        // Elastic collision impulse (equal mass)
        double p = (a.getDx() - b.getDx()) * nx + (a.getDy() - b.getDy()) * ny;
        a.setDx(a.getDx() - p * nx);
        a.setDy(a.getDy() - p * ny);
        b.setDx(b.getDx() + p * nx);
        b.setDy(b.getDy() + p * ny);

        // Separate overlapping circles
        double overlap = (minD - dist) / 2.0;
        a.setX(a.getX() - nx * overlap);
        a.setY(a.getY() - ny * overlap);
        b.setX(b.getX() + nx * overlap);
        b.setY(b.getY() + ny * overlap);
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (CircleV2 c : circles) c.draw(g);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("FPS: "         + fps,                                      10, 20);
        g.drawString("Avg FPS: "     + String.format("%.1f", avgFps),            10, 40);
        g.drawString("Threads: "     + THREAD_COUNT,                             10, 60);
        g.drawString("CircleV2s: "     + NUMBER_OF_CIRCLES,                        10, 80);
        long elapsed   = (System.currentTimeMillis() - simStart) / 1000;
        long remaining = RUN_DURATION_SECS - elapsed;
        g.drawString("Time left: "   + remaining + "s",                          10, 100);
    }

    // ── Entry point ──────────────────────────────────────────────────────────

    public static void main(String[] args) {
        JFrame frame = new JFrame("Colliding Bouncing CircleV2s – Multithreaded");
        SimulationPanelTime panel = new SimulationPanelTime();
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
