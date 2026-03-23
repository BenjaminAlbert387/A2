import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class WorkerTask implements Callable<List<String>> {

    private final List<String> serialisedCircles;
    private final int panelWidth;
    private final int panelHeight;

    public WorkerTask(List<String> serialisedCircles, int panelWidth, int panelHeight) {
        this.serialisedCircles = serialisedCircles;
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
    }

    @Override
    public List<String> call() {
        List<String> results = new ArrayList<>();

        for (String s : serialisedCircles) {
            // Deserialise string into a Circle object
            Circle c = Circle.deserialise(s);

            // Compute movement
            c.move(panelWidth, panelHeight);

            // Serialise back to string and add to results
            results.add(c.serialise());
        }

        return results;
    }
}
