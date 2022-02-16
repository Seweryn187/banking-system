import communication.Message;

public interface MessageHandler {
    Message invoke(Message message);
}
