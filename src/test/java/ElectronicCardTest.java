import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

public class ElectronicCardTest {

    private ElectronicCard card;

    @BeforeEach
    void createCard() {
        card = new ElectronicCard(
                "010101",
                "000000",
                "1234".getBytes(),
                "444".getBytes(),
                new Date(new Date().getTime() + 3000)
        );
    }

    @Test
    public void validityTest() {
        assertTrue(card.validate());
    }

    @Test
    public void invalidityTest() throws InterruptedException {
        Thread.sleep(3000);

        assertFalse(card.validate());
    }

    @Test
    public void successfulAuthorizeTest() {
        assertTrue(card.authorize("1234".getBytes()));
    }

    @Test
    public void unsuccessfulAuthorizeTest() {
        assertFalse(card.authorize("1235".getBytes()));
    }

    @Test
    public void changePinAndAuthorizeTest() {
        card.changePin("1111".getBytes());

        assertTrue(card.authorize("1111".getBytes()));
    }

    @Test
    public void lockCardTest() throws ElectronicCard.ElectronicCardLockException {
        card.lock();

        assertFalse(card.isActive());
    }

    @Test
    public void unlockCardTest() throws ElectronicCard.ElectronicCardLockException {
        card.lock();

        assertFalse(card.isActive());

        card.unlock();

        assertTrue(card.isActive());
    }

    @Test
    public void trySecondLockTest() {
        boolean exceptionThrown = false;
        try {
            card.lock();
            card.lock();
        } catch (ElectronicCard.ElectronicCardLockException e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown);
    }

    @Test
    public void lockAndValidate() throws ElectronicCard.ElectronicCardLockException {
        assertTrue(card.isActive());
        card.lock();

        assertFalse(card.isActive());
        assertFalse(card.validate());
    }
}
