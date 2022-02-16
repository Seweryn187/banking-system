import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Date;

public abstract class Application {

    public enum State {
        PROCESSING,
        POSITIVE,
        NEGATIVE
    }
    private final ObjectId identifier;
    private final Date submissionDate;
    private State status;

    protected Application(Date submissionDate) {
        this(new ObjectId(), submissionDate, State.PROCESSING);
    }

    protected Application(ObjectId identifier, Date submissionDate, State status) {
        this.identifier = identifier;
        this.submissionDate = submissionDate;
        this.status = status;
    }

    public ObjectId getIdentifier() {
        return identifier;
    }

    public Date getSubmissionDate() {
        return submissionDate;
    }

    public State getStatus() {
        return status;
    }

    protected abstract void reviewPositivelyAction(Employee employee);

    protected abstract void reviewNegativelyAction(Employee employee);

    public void reviewPositively(Employee employee) {
        if (this.status == State.PROCESSING) {
            this.status = State.POSITIVE;
            reviewPositivelyAction(employee);
        }
    }

    public void reviewNegatively(Employee employee) {
        if (this.status == State.PROCESSING) {
            this.status = State.NEGATIVE;
            reviewNegativelyAction(employee);
        }
    }

    protected Document convertToDocument() {
        Document document = new Document();
        document.put("_id", this.getIdentifier());
        document.put("submissionDate", this.getSubmissionDate());
        document.put("status", this.getStatus());
        return document;
    }
}