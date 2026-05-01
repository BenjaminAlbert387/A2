/**
 * WorkerTask
 *
 * Each worker owns a contiguous slice [start, end) of the shared circles array.
 * Communication with the main thread uses two volatile boolean flags per worker:
 *   workReady  – main sets true to tell the worker to start a frame
 *   workDone   – worker sets true when the frame is finished
 *
 * No serialisation, no queues, no synchronized blocks.
 */
public class WorkerTaskV2 implements Runnable {

    private final CircleV2[] circles;
    private final int start;
    private final int end;
    private final int width;
    private final int height;

    // Flags shared with the main thread (written by one side, read by the other)
    private volatile boolean workReady = false;
    private volatile boolean workDone  = false;
    private volatile boolean running   = true;

    public WorkerTaskV2(CircleV2[] circles, int start, int end, int width, int height) {
        this.circles = circles;
        this.start   = start;
        this.end     = end;
        this.width   = width;
        this.height  = height;
    }

    // ── Called by the main thread ────────────────────────────────────────────

    /** Tell this worker to process one frame. */
    public void signal() {
        workDone  = false;
        workReady = true;          // worker will see this and wake up
    }

    /** Block the calling thread until this worker has finished. */
    public void await() {
        while (!workDone) {
            Thread.yield();        // give up timeslice while spinning
        }
    }

    /** Shut down the worker loop cleanly. */
    public void stop() {
        running = false;
        workReady = true;          // unblock the spin loop so the thread can exit
    }

    // ── Worker loop ──────────────────────────────────────────────────────────

    @Override
    public void run() {
        while (running) {
            // Wait for the main thread to signal work
            while (!workReady) {
                Thread.yield();
            }
            workReady = false;

            if (!running) break;

            // Move each circle in our slice and bounce off walls
            for (int i = start; i < end; i++) {
                CircleV2 c = circles[i];
                c.setX(c.getX() + c.getDx());
                c.setY(c.getY() + c.getDy());

                // Bounce off left/right walls
                if (c.getX() - c.getRadius() < 0) {
                    c.setX(c.getRadius());
                    c.setDx(-c.getDx());
                } else if (c.getX() + c.getRadius() > width) {
                    c.setX(width - c.getRadius());
                    c.setDx(-c.getDx());
                }

                // Bounce off top/bottom walls
                if (c.getY() - c.getRadius() < 0) {
                    c.setY(c.getRadius());
                    c.setDy(-c.getDy());
                } else if (c.getY() + c.getRadius() > height) {
                    c.setY(height - c.getRadius());
                    c.setDy(-c.getDy());
                }
            }

            workDone = true;       // tell main thread we're done
        }
    }
}
