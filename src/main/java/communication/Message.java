package communication;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

public class Message implements Serializable {

    public enum Type {
        LOGIN(-1),
        LIST_BILLS(0),
        BILL_SCHEDULED(1),
        BILL_HISTORY(2),
        CARD_CHANGE_PIN(3),
        OPERATION_INTERNAL_TRANSFER(4),
        OPERATION_TRANSFER(5),
        OPERATION_INVESTMENT(6),
        OPERATION_BILL_CREATION(7),
        OPERATION_CARD_ORDER(8),
        APPLICATION_CLIENT_CREATION(0),
        ADMIN_EMPLOYEE_CREATION(0);

        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private final Type type;
    private final Object[] content;

    public Message(Type type, Collection<Object> content) {
        this.type = type;
        this.content = content.toArray();
    }

    public Message(Type type, Object[] content) {
        this.type = type;
        this.content = Arrays.copyOf(content, content.length);
    }

    public Message(Type type) {
        this(type, new Object[] {});
    }

    public Type getType() {
        return type;
    }

    public Object[] getContent() {
        return content;
    }
}
