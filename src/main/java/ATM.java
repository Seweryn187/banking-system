import communication.Message;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ATM extends Signable {

    public static final Pattern loginPattern = Pattern.compile("[a-zA-Z0-9]{15}");

    private final Map<Banknote, Integer> banknotes;

    public ATM(ObjectId identifier, String login, String password) {
        super(identifier, login, password);
        this.banknotes = new HashMap<>(Map.of(
                Banknote.PAPER_10, 0,
                Banknote.PAPER_20, 0,
                Banknote.PAPER_50, 0,
                Banknote.PAPER_100, 0,
                Banknote.PAPER_200, 0,
                Banknote.PAPER_500, 0
        ));
    }

    @Override
    public Message handleMessageAndRespond(Message message) {
        return null;
    }

    public void addBanknote(Banknote banknote) {
        int currentAmount = this.banknotes.get(banknote);
        this.banknotes.replace(banknote, currentAmount + 1);
    }

    public void addBanknotes(Banknote banknote, Integer amount) {
        int currentAmount = this.banknotes.get(banknote);
        this.banknotes.replace(banknote, currentAmount + amount);
    }

    public void deposit(List<Banknote> cash, ElectronicCard card) {

    }

    public List<Banknote> withdraw(int amount, ElectronicCard card) {
        return null;
    }
}
