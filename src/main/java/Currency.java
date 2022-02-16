public class Currency {

    private final String name;
    private final String abbreviation;

    private Currency(String name, String abbreviation) {
        this.name = name;
        this.abbreviation = abbreviation;
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public static final Currency PLN = new Currency("Złoty", "PLN");
    public static final Currency USD = new Currency("Dollar", "USD");
    public static final Currency EURO = new Currency("Euro", "EUR");

    public static Currency getCurrency(String abbreviation) {
        switch (abbreviation) {
            case "PLN": return Currency.PLN;
            case "USD": return Currency.USD;
            case "EURO": return Currency.EURO;
        }
        return null;
    }
}
