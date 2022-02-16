import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Date;

public class ApplicationTest {

    private static System system;
    private System.Admin employee;

    @BeforeAll
    static void createSystem() {
        system = System.getInstance(new BigDecimal(1_000_000_000));
        system.start();
    }

    @BeforeEach
    void createEmployee() {
        employee = (System.Admin) system.signIn("ADMIN0000000", "admin");
    }

    @Test
    public void testPositive() throws InterruptedException {
        Application application = Client.sendClientCreationApplication(
                "test",
                "test",
                "TEST1",
                "TEST2",
                new Date(),
                "000-000-000",
                "email@email.com",
                Currency.PLN
        );

        assertEquals(Application.State.PROCESSING, application.getStatus());

        application.reviewPositively(employee);

        Thread.sleep(1000);

        assertEquals(Application.State.POSITIVE, application.getStatus());

        employee.removeApplication(application);

        Client client = (Client) system.signIn("test", "test");

        assertNotNull(client);
    }

    @Test
    public void testNegative() throws InterruptedException {
        Application application = Client.sendClientCreationApplication(
                "test2",
                "test",
                "TEST1",
                "TEST2",
                new Date(),
                "000-000-000",
                "email@email.com",
                Currency.PLN
        );

        assertEquals(Application.State.PROCESSING, application.getStatus());

        application.reviewNegatively(employee);

        Thread.sleep(1000);

        assertEquals(Application.State.NEGATIVE, application.getStatus());

        employee.removeApplication(application);

        Client client = (Client) system.signIn("test2", "test");

        assertNull(client);
    }
}
