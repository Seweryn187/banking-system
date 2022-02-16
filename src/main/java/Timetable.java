import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

public class Timetable {

    private static Timetable instance = null;

    private final PriorityBlockingQueue<Operation> timetable;

    public static Timetable getInstance() {
        if (Timetable.instance == null) {
            Timetable.instance = new Timetable();
        }
        return Timetable.instance;
    }

    private Timetable() {
        this.timetable = new PriorityBlockingQueue<>(11, Comparator.comparing(Operation::getExecutionDate));
    }

    public boolean add(Operation operation) {
        return this.timetable.add(operation);
    }

    public Operation get() {
        return this.timetable.peek();
    }

    public boolean remove(Operation operation) {
        return this.timetable.remove(operation);
    }

    public boolean isEmpty() { return this.timetable.isEmpty(); }
}
