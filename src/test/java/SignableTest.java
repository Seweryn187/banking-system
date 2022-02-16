import communication.Message;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SignableTest {

    private Signable signable;

    @BeforeEach
    public void createSignable() {
        signable = new Signable(new ObjectId(), "test", "test") {
            @Override
            protected Message handleMessageAndRespond(Message message) {
                return null;
            }
        };
    }

    @Test
    public void changePasswordTest() throws Exception {
        assertEquals("test", signable.getPassword());

        signable.changePassword("test", "test2");

        assertEquals("test2", signable.getPassword());
    }

    @Test
    public void logInAndLogOutTest() {
        signable.setLogged();
        assertTrue(signable.isLogged());
        signable.logOut();
        assertFalse(signable.isLogged());
    }
}
