package Version2;

// Simple container passed through the WorkQueue instead of a serialised string.
// Workers receive this, move circles[start..end], then post it back as a done signal.
public class CircleBatch {
    public final Circle[] circles;
    public final int start;
    public final int end;

    public CircleBatch(Circle[] circles, int start, int end) {
        this.circles = circles;
        this.start   = start;
        this.end     = end;
    }
}
