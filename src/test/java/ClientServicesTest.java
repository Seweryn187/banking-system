import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ClientServicesTest {

    private static System system;
    private final Client[] clients = new Client[2];

    private void createClientsAndTheirBills() {
        clients[0] = system.createClientAndHisBill(
                "login123",
                "password123",
                "FirstName",
                "LastName",
                new GregorianCalendar(
                        1900,
                        Calendar.JANUARY,
                        1
                ).getTime(),
                "123-456-789",
                "email@email.com",
                "PL",
                Currency.PLN
        );
        assertNotNull(clients[0]);
        system.createAndAddBill(clients[0], "PL", Currency.PLN);
        clients[1] = system.createClientAndHisBill(
                "login1234",
                "password1234",
                "FirstName2",
                "LastName2",
                new GregorianCalendar(
                        1900,
                        Calendar.JANUARY,
                        2
                ).getTime(),
                "987-654-321",
                "email2@email.com",
                "PL",
                Currency.PLN
        );
        assertNotNull(clients[1]);
        system.createAndAddBill(clients[1], "PL", Currency.PLN);
    }

    @BeforeAll
    static void createSystem() {
        system = System.getInstance(new BigDecimal(1_000_000_000));
        system.start();
    }

    @BeforeEach
    void addMoney() {
        createClientsAndTheirBills();
        clients[0].getBill(0).deposit(new BigDecimal(1000));
        clients[0].getBill(1).deposit(new BigDecimal(1000));
        clients[1].getBill(0).deposit(new BigDecimal(1000));
        clients[1].getBill(1).deposit(new BigDecimal(1000));
    }

    @Test
    public void scheduleTransfer() throws InterruptedException {
        assertNotNull(clients[0].getBill(0));
        assertNotNull(clients[1].getBill(0));
        clients[0].scheduleTransfer(
                "Test",
                clients[0].getBill(0),
                clients[1].getBill(0).getIban(),
                new BigDecimal(20),
                new Date(new Date().getTime() + 5000)
        );
        assertEquals(new BigDecimal(980), clients[0].getBill(0).getCurrentBalance());
        assertEquals(new BigDecimal(20), clients[0].getBill(0).getBalanceOnHold());
        assertEquals(new BigDecimal(1000), clients[1].getBill(0).getCurrentBalance());
        Thread.sleep(6000);
        assertEquals(new BigDecimal(980), clients[0].getBill(0).getCurrentBalance());
        assertEquals(new BigDecimal(0), clients[0].getBill(0).getBalanceOnHold());
        assertEquals(new BigDecimal(1020), clients[1].getBill(0).getCurrentBalance());
    }

    @Test
    public void scheduleInternalTransfer() throws InterruptedException {
        assertNotNull(clients[0].getBill(0));
        assertNotNull(clients[0].getBill(1));
        clients[0].scheduleInternalTransfer(
                "Test",
                clients[0].getBill(0),
                clients[0].getBill(1),
                new BigDecimal(20),
                new Date(new Date().getTime() + 5000)
        );
        assertEquals(new BigDecimal(980), clients[0].getBill(0).getCurrentBalance());
        assertEquals(new BigDecimal(20), clients[0].getBill(0).getBalanceOnHold());
        assertEquals(new BigDecimal(1000), clients[0].getBill(1).getCurrentBalance());
        Thread.sleep(6000);
        assertEquals(new BigDecimal(980), clients[0].getBill(0).getCurrentBalance());
        assertEquals(new BigDecimal(0), clients[0].getBill(0).getBalanceOnHold());
        assertEquals(new BigDecimal(1020), clients[0].getBill(1).getCurrentBalance());
    }

    @Test
    public void performTransfer() throws InterruptedException {
        assertNotNull(clients[0].getBill(0));
        assertNotNull(clients[1].getBill(0));
        clients[0].performTransfer(
                "Test",
                clients[0].getBill(0),
                clients[1].getBill(0).getIban(),
                new BigDecimal(20)
        );
        Thread.sleep(1000);
        assertEquals(new BigDecimal(980), clients[0].getBill(0).getCurrentBalance());
        assertEquals(new BigDecimal(1020), clients[1].getBill(0).getCurrentBalance());
    }

    @Test
    public void performInternalTransfer() throws InterruptedException {
        assertNotNull(clients[0].getBill(0));
        assertNotNull(clients[0].getBill(1));
        clients[0].performInternalTransfer(
                "Test",
                clients[0].getBill(0),
                clients[0].getBill(1),
                new BigDecimal(20)
        );
        Thread.sleep(1000);
        assertEquals(new BigDecimal(980), clients[0].getBill(0).getCurrentBalance());
        assertEquals(new BigDecimal(1020), clients[0].getBill(1).getCurrentBalance());
    }

    @Test
    @Disabled("Ten test trwa 30 dni")
    public void performInvestmentCreation() throws InterruptedException {
        assertNotNull(clients[0].getBill(0));
        clients[0].performInvestmentCreation(
                clients[0].getBill(0),
                new BigDecimal(1000),
                System.InvestmentPeriod.ONE_MONTH
        );
        Thread.sleep(1000);
        assertEquals(new BigDecimal(0), clients[0].getBill(0).getCurrentBalance());
        Thread.sleep(TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS));
        assertTrue(clients[0].getBill(0).getCurrentBalance().compareTo(new BigDecimal(0)) > 0);
        java.lang.System.out.println(clients[0].getBill(0).getCurrentBalance());
    }
}
