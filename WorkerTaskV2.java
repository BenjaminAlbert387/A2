// Each worker gets a parsed string
// Communication with the main thread uses two volatile boolean flags per worker:
// workReady: main sets true to tell the worker to start a frame
// workDone: worker sets true when the frame is finished

public class WorkerTaskV2 implements Runnable {

    private final CircleV2[] circles;
    private final int start;
    private final int end;
    private final int width;
    private final int height;

    // Flags shared with the main thread (written by one side, read by the other)
    // Used instead of synchronised or other Java key words
    private volatile boolean workReady = false;
    private volatile boolean workDone  = false;
    private volatile boolean running   = true;

    // Receive the parsed string
    public WorkerTaskV2(CircleV2[] circles, String params) {
        String[] parts = params.split(",");
        this.start  = Integer.parseInt(parts[0]);
        this.end    = Integer.parseInt(parts[1]);
        this.width  = Integer.parseInt(parts[2]);
        this.height = Integer.parseInt(parts[3]);
        this.circles = circles;
    }

    // Called by the main thread

    // Tell this worker to process a single frame
    private volatile String command = "";

    public void signal() {
        workDone  = false;
        command = "run";
        // Causes the worker to start up
        workReady = true;
    }

    // Block the calling thread until this worker has finished
    public void await() {
        while (!workDone) {
            Thread.yield();
        }
    }

    // Shut down the worker loop
    public void stop() {
        command = "stop";
        running = false;
        // Unblocks the spin loop so the thread can exit
        workReady = true;
    }

    // Worker loop

    @Override
    public void run() {
        if (command.equals("run")) {
    }
        while (running) {
            // Wait for the main thread to signal work
            while (!workReady) {
                Thread.yield();
            }
            workReady = false;

            if (command.equals("stop")) break;

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

            // Tells the main thread that the work in the worker thread is done
            workDone = true;
        }
    }
}
