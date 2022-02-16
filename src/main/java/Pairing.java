public class Pairing<T, U> {

    private final T item1;
    private final U item2;

    public Pairing(T item1, U item2) {
        this.item1 = item1;
        this.item2 = item2;
    }

    public T getItem1() {
        return item1;
    }

    public U getItem2() {
        return item2;
    }
}