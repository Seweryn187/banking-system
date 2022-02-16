import com.mongodb.BasicDBObject;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Date;

public abstract class Operation {

    public static final Operation EMPTY_OPERATION = new Operation(null,null,null) {
        @Override
        public Date getCreationDate() {
            throw new IllegalStateException();
        }

        @Override
        public Date getExecutionDate() {
            throw new IllegalStateException();
        }

        @Override
        public boolean isActive() {
            throw new IllegalStateException();
        }

        @Override
        public void activate() {
            throw new IllegalStateException();
        }

        @Override
        public void deactivate() {
            throw new IllegalStateException();
        }

        @Override
        public boolean isExecuted() {
            throw new IllegalStateException();
        }

        @Override
        public boolean execute(System.ExecutionVisitor executionVisitor) {
            throw new IllegalStateException();
        }

        @Override
        public void executeAction(System.ExecutionVisitor executionVisitor) {
            throw new IllegalStateException();
        }

        @Override
        protected Document convertToDocument() {
            throw new IllegalStateException();
        }

        @Override
        protected BasicDBObject updatedObject() {
            throw new IllegalStateException();
        }
    };

    public enum State {
        ACTIVE,
        BLOCKED,
        EXECUTED
    }
    private final ObjectId identifier;
    private final Date creationDate;
    private final Date executionDate;
    private State state;

    protected Operation(ObjectId identifier, Date creationDate, Date executionDate, State state) {
        this.identifier = identifier;
        this.creationDate = creationDate;
        this.executionDate = executionDate;
        this.state = state;
    }

    protected Operation(ObjectId identifier, Date creationDate, Date executionDate) {
        this(identifier, creationDate, executionDate, State.ACTIVE);
    }

    protected Operation(ObjectId identifier) {
        this(identifier, new Date(), new Date());
    }

    protected Operation(ObjectId identifier, Date executionDate) {
        this(identifier, new Date(), executionDate);
    }

    public ObjectId getIdentifier() {
        return identifier;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getExecutionDate() {
        return executionDate;
    }

    public State getState() {
        return state;
    }

    public boolean isActive() {
        return this.state == State.ACTIVE;
    }

    public boolean isBlocked() {
        return this.state == State.BLOCKED;
    }

    public boolean isExecuted() {
        return this.state == State.EXECUTED;
    }

    public void activate() {
        this.state = State.ACTIVE;
    }

    public void deactivate() {
        this.state = State.BLOCKED;
    }

    public boolean execute(System.ExecutionVisitor executionVisitor) {
        if (this.state == State.BLOCKED) {
            return false;
        }
        executeAction(executionVisitor);
        this.state = State.EXECUTED;
        return true;
    }

    protected abstract void executeAction(System.ExecutionVisitor executionVisitor);

    protected Document convertToDocument() {
        Document document = new Document();
        document.put("_id", this.getIdentifier());
        document.put("type", "transfer");
        document.put("creationDate", this.getCreationDate());
        document.put("executionDate", this.getExecutionDate());
        document.put("state", this.getState());
        return document;
    }

    protected BasicDBObject updatedObject() {
        return new BasicDBObject("state", this.getState());
    }
}
