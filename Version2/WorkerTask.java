package Version2;

public class WorkerTask implements Runnable {

    private final WorkQueue inputQueue;
    private final WorkQueue resultQueue;
    private volatile boolean running = true;
    private final int panelWidth;
    private final int panelHeight;

    public WorkerTask(WorkQueue inputQueue, WorkQueue resultQueue, int panelWidth, int panelHeight) {
        this.inputQueue = inputQueue;
        this.resultQueue = resultQueue;
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            if (!inputQueue.isEmpty()) {
                // Dequeue the int[] work order: [sharedArrayRef via wrapper, start, end]
                CircleBatch batch = (CircleBatch) inputQueue.dequeue();

                // Move each circle in the assigned slice directly - no copy, no serialise
                for (int i = batch.start; i < batch.end; i++) {
                    batch.circles[i].move(panelWidth, panelHeight);
                }

                // Signal done by posting the same batch back
                resultQueue.enqueue(batch);
            } else {
                Thread.yield();
            }
        }
    }
}
