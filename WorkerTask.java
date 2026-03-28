import java.util.List;
import java.util.ArrayList;

public class WorkerTask implements Runnable {

    private final WorkQueue inputQueue;
    private final WorkQueue resultQueue;
    private volatile boolean running = true;
    private final boolean[] runningFlag = new boolean[]{true};
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
        runningFlag[0] = false;
    }

    @Override
    public void run() {
        while (running) {
            String batch = (String) inputQueue.dequeue(runningFlag);
            if (batch == null) break;

            String[] serialisedCircles = batch.split(";");
            List<String> results = new ArrayList<>();

            for (String s : serialisedCircles) {
                if (!s.isEmpty()) {
                    Circle c = Circle.deserialise(s);
                    c.move(panelWidth, panelHeight);
                    results.add(c.serialise());
                }
            }

            resultQueue.enqueue(String.join(";", results));
        }
    }
}