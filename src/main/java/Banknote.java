public class Banknote {

    private static final Currency currency = Currency.PLN;

    private final Integer value;

    private Banknote(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static final Banknote PAPER_10 = new Banknote(10);
    public static final Banknote PAPER_20 = new Banknote(20);
    public static final Banknote PAPER_50 = new Banknote(50);
    public static final Banknote PAPER_100 = new Banknote(100);
    public static final Banknote PAPER_200 = new Banknote(200);
    public static final Banknote PAPER_500 = new Banknote(500);
}
