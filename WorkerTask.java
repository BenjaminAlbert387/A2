import java.util.List;
import java.util.ArrayList;

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
                // Dequeue a batch of serialised circle strings
                String batch = (String) inputQueue.dequeue();

                // Process each circle in the batch
                String[] serialisedCircles = batch.split(";");
                List<String> results = new ArrayList<>();

                for (String s : serialisedCircles) {
                    if (!s.isEmpty()) {
                        Circle c = Circle.deserialise(s);
                        c.move(panelWidth, panelHeight);
                        results.add(c.serialise());
                    }
                }

                // Put results back as a single string
                resultQueue.enqueue(String.join(";", results));
            } else {
                // No work available, yield to avoid burning CPU
                Thread.yield();
            }
        }
    }
}