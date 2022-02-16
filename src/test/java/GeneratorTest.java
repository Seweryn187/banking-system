import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;

public class GeneratorTest {

    private Generator generator;

    @BeforeEach
    void createGenerator() {
        generator = new Generator();
    }

    @Test
    public void uniqueIdTest() {
        HashSet<String> uniqueId = new HashSet<>();
        HashSet<String> cardNumbers = new HashSet<>();
        for (int i = 0; i < 25000; ++i) {
            assertTrue(uniqueId.add(generator.generateUniqueId(Generator.UNIQUE_BILL_NUMBER_LENGTH)));
            assertTrue(cardNumbers.add(generator.generateCardNumber(Generator.UNIQUE_CARD_NUMBER_LENGTH)));
        }
    }
}
