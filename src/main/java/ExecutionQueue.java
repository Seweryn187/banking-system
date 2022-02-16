import java.util.concurrent.LinkedBlockingQueue;

public class ExecutionQueue {

    private static ExecutionQueue instance = null;

    public static ExecutionQueue getInstance(System.ExecutionVisitor executionVisitor) {
        if (executionVisitor != null && ExecutionQueue.instance == null) {
            return ExecutionQueue.instance = new ExecutionQueue(executionVisitor);
        }
        return instance;
    }

    public static ExecutionQueue getInstance() {
        return ExecutionQueue.instance;
    }

    private final System.ExecutionVisitor executionVisitor;
    private final LinkedBlockingQueue<Operation> queue;

    private ExecutionQueue(System.ExecutionVisitor executionVisitor) {
        this.executionVisitor = executionVisitor;
        this.queue = new LinkedBlockingQueue<>();
    }

    public void executeNextOperation() throws InterruptedException {
        this.queue.take().execute(this.executionVisitor);
    }

    public boolean enqueue(Operation operation) {
        return this.queue.offer(operation);
    }

    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

    public Operation get() {
        return this.queue.peek();
    }

    public boolean remove(Operation operation) {
        return this.queue.remove(operation);
    }
}
