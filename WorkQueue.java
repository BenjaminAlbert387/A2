public class WorkQueue {

    private final Object[] items;
    private int head = 0;
    private int tail = 0;
    private int count = 0;
    private final int capacity;
    private volatile boolean lock = false;

    public WorkQueue(int capacity) {
        this.capacity = capacity;
        items = new Object[capacity];
    }

    private void acquireLock() {
        while (true) {
            if (!lock) {
                lock = true;
                return;
            }
            Thread.yield();
        }
    }

    private void releaseLock() {
        lock = false;
    }

    public void enqueue(Object item) {
        while (true) {
            acquireLock();
            if (count < capacity) {
                items[tail] = item;
                tail = (tail + 1) % capacity;
                count++;
                releaseLock();
                return;
            }
            releaseLock();
            Thread.yield();
        }
    }

    public Object dequeue() {
        while (true) {
            acquireLock();
            if (count > 0) {
                Object item = items[head];
                items[head] = null;
                head = (head + 1) % capacity;
                count--;
                releaseLock();
                return item;
            }
            releaseLock();
            Thread.yield();
        }
    }

    public Object dequeue(boolean[] runningFlag) {
        while (true) {
            if (!runningFlag[0]) return null;
            acquireLock();
            if (count > 0) {
                Object item = items[head];
                items[head] = null;
                head = (head + 1) % capacity;
                count--;
                releaseLock();
                return item;
            }
            releaseLock();
            Thread.yield();
        }
    }

    public boolean isEmpty() {
        acquireLock();
        boolean empty = count == 0;
        releaseLock();
        return empty;
    }

    public int size() {
        acquireLock();
        int size = count;
        releaseLock();
        return size;
    }
}