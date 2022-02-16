import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Generator {

    public static final int UNIQUE_BILL_NUMBER_LENGTH = 26;
    public static final int UNIQUE_CARD_NUMBER_LENGTH = 16;

    private final List<String> identifiers;
    private final List<String> cardNumbers;
    private final Random rand;
    private static final char[] characters
            = "QWERTYUIOPASDFGHJKLZXCVBNM0123456789qwertyuiopasdfghjklzxcvbnm".toCharArray();

    public Generator() {
        this(new LinkedList<>(), new LinkedList<>());
    }

    public Generator(List<String> identifiers, List<String> cardNumbers) {
        this.identifiers = identifiers;
        this.cardNumbers = cardNumbers;
        this.rand = new Random();
    }

    public List<String> getIdentifiers() {
        return identifiers;
    }

    public List<String> getCardNumbers() {
        return cardNumbers;
    }

    private String generateId(int length) {
        char[] str = new char[length];
        for (int i = 0; i < str.length; ++i) {
            str[i] = characters[rand.nextInt(characters.length)];
        }
        return new String(str);
    }

    private String generateNumber(int length) {
        char[] str = new char[length];
        for (int i = 0; i < str.length; ++i) {
            str[i] = (char) ('0' + rand.nextInt(10));
        }
        return new String(str);
    }

    public String generateUniqueId(int length) {
        String id = generateId(length);
        while (identifiers.contains(id)) {
            id = generateId(length);
        }
        identifiers.add(id);
        return id;
    }

    public String generateCardNumber(int length) {
        String id = generateNumber(length);
        while (cardNumbers.contains(id)) {
            id = generateId(length);
        }
        cardNumbers.add(id);
        return id;
    }

    public byte[] generatePin() {
        byte[] bytes = new byte[4];
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte) (rand.nextInt(10));
        }
        return bytes;
    }

    public byte[] generateCvvCode() {
        byte[] bytes = new byte[3];
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte) (rand.nextInt(10));
        }
        return bytes;
    }
}
