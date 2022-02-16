public interface RequestQueueAccess<T extends Signable> {
    boolean addRequest(T requester, Operation operation);
}
