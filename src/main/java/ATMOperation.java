import org.bson.types.ObjectId;

public class ATMOperation extends Operation {

    public ATMOperation(String identifier) {
        super(new ObjectId());
    }

    @Override
    public void executeAction(System.ExecutionVisitor executionVisitor) {
        executionVisitor.executeATMOperation(this);
    }
}
