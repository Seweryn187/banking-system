import java.math.BigDecimal;
import java.util.*;

public class Bill {
    private final String countryCode;
    private final String number;

    private final Currency billCurrency;
    private BigDecimal currentBalance;
    private BigDecimal balanceOnHold;

    private final SortedSet<Operation> scheduledOperations;
    private final SortedSet<Operation> history;

    private final Map<String, ElectronicCard> cards;

    public Bill(
            String countryCode,
            String number,
            Currency billCurrency,
            BigDecimal currentBalance,
            BigDecimal balanceOnHold
    ) {
        this.countryCode = countryCode;
        this.number = number;
        this.billCurrency = billCurrency;
        this.currentBalance = currentBalance;
        this.balanceOnHold = balanceOnHold;

        this.scheduledOperations = new TreeSet<>(Comparator.comparing(Operation::getCreationDate));
        this.history = new TreeSet<>(Comparator.comparing(Operation::getCreationDate));
        this.cards = new HashMap<>();
    }

    public Bill(String countryCode, String number, Currency billCurrency) {
        this(countryCode, number, billCurrency, new BigDecimal(0), new BigDecimal(0));
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getNumber() {
        return number;
    }

    public Currency getBillCurrency() {
        return billCurrency;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getBalanceOnHold() {
        return balanceOnHold;
    }

    public SortedSet<Operation> getScheduledOperations() {
        return scheduledOperations;
    }

    public SortedSet<Operation> getHistory() {
        return history;
    }

    public boolean addScheduledOperation(Operation operation) {
        return this.scheduledOperations.add(operation);
    }

    public boolean moveToHistory(Operation operation) {
        if (!this.scheduledOperations.remove(operation)) {
            return false;
        }
        this.history.add(operation);
        return true;
    }

    public boolean addToHistory(Operation operation) {
        return this.history.add(operation);
    }

    public boolean hold(BigDecimal amount) {
        if (currentBalance.compareTo(amount) >= 0) {
            currentBalance = currentBalance.subtract(amount);
            balanceOnHold = balanceOnHold.add(amount);
            return true;
        }
        return false;
    }

    public boolean release(BigDecimal amount) {
        if (balanceOnHold.compareTo(amount) >= 0) {
            balanceOnHold = balanceOnHold.subtract(amount);
            currentBalance = currentBalance.add(amount);
            return true;
        }
        return false;
    }

    public void deposit(BigDecimal amount) {
        this.currentBalance = this.currentBalance.add(amount);
    }

    public boolean withdraw(BigDecimal amount) {
        BigDecimal balanceAfterDecreasing = this.currentBalance.subtract(amount);
        if (balanceAfterDecreasing.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        this.currentBalance = balanceAfterDecreasing;
        return true;
    }

    public String getIban() {
        return this.countryCode + this.number;
    }

    public void addCard(ElectronicCard card) {
        this.cards.put(card.getNumber(), card);
    }

    public ElectronicCard getCard(String number) {
        return this.cards.get(number);
    }

    public Collection<ElectronicCard> getCards() {
        return this.cards.values();
    }

    public String[][] getCardsInfo() {
        String[][] cardsInfo = new String[cards.size()][];
        int i = 0;
        for (var card : cards.values()) {
            cardsInfo[i] = new String[] {
                    card.getNumber(),
                    card.getValidityEndDate().toString(),
                    "" + card.isActive()
            };
            ++i;
        }
        return cardsInfo;
    }
}
