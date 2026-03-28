public class WorkQueue {

    private final Object[] items;
    private volatile int head = 0;
    private volatile int tail = 0;
    private volatile int count = 0;
    private final int capacity;

    public WorkQueue(int capacity) {
        this.capacity = capacity;
        items = new Object[capacity];
    }

    // Add item to queue - busy waits if full
    public void enqueue(Object item) {
        while (count == capacity) {
            // Busy wait until space is available
            Thread.yield();
        }
        items[tail] = item;
        tail = (tail + 1) % capacity;
        count++;
    }

    // Remove item from queue - busy waits if empty
    public Object dequeue() {
        while (count == 0) {
            // Busy wait until item is available
            Thread.yield();
        }
        Object item = items[head];
        items[head] = null;
        head = (head + 1) % capacity;
        count--;
        return item;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public int size() {
        return count;
    }
}