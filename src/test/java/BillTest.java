import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Date;

public class BillTest {

    private static System system;
    Client client = null;
    Bill bill1 = null;
    Bill bill2 = null;

    @BeforeAll
    static void createSystem() {
        system = System.getInstance(new BigDecimal(1_000_000_000));
        system.start();
    }

    @BeforeEach
    void createClientAndHisBill() {
        client = system.createClientAndHisBill(
                "test2",
                "test",
                "TEST1",
                "TEST2",
                new Date(),
                "000-000-000",
                "email@email.com",
                "PL",
                Currency.PLN
        );
        system.createAndAddBill(client, "PL", Currency.PLN);
        bill1 = client.getPrimaryBill();
        bill1.deposit(new BigDecimal(200));
        bill2 = client.getBill(1);
    }

    @Test
    public void historyTest() throws InterruptedException {
        System.TransferOperation transferOperation = system.createTransferOperation(
                "Test",
                bill1.getIban(),
                bill2.getIban(),
                new BigDecimal(100)
        );
        Thread.sleep(1000);
        System.TransferOperation transferOperation2 = system.createTransferOperation(
                "Test2",
                bill1.getIban(),
                bill2.getIban(),
                new BigDecimal(100)
        );

        Thread.sleep(1000);

        bill1.addToHistory(transferOperation2);
        bill1.addToHistory(transferOperation);

        assertEquals(transferOperation, bill1.getHistory().first());
        bill1.getHistory().remove(transferOperation);
        assertEquals(transferOperation2, bill1.getHistory().first());
    }

    @Test
    public void depositTest() {
        BigDecimal amount = bill1.getCurrentBalance();
        bill1.deposit(new BigDecimal(100));

        assertEquals(bill1.getCurrentBalance(), amount.add(new BigDecimal(100)));
    }

    @Test
    public void withdrawTest() {
        BigDecimal amount = bill1.getCurrentBalance();
        bill1.withdraw(new BigDecimal(100));

        assertEquals(bill1.getCurrentBalance(), amount.subtract(new BigDecimal(100)));
    }

    @Test
    public void holdTest() {
        BigDecimal amount = bill1.getBalanceOnHold();
        bill1.hold(new BigDecimal(100));

        assertEquals(bill1.getBalanceOnHold(), amount.add(new BigDecimal(100)));
    }
}
