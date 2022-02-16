import communication.Message;
import org.bson.types.ObjectId;

public abstract class Signable {

    private boolean active;
    private boolean logged;

    private final ObjectId identifier;
    private final String login;
    private String password;

    public Signable(ObjectId identifier, String login, String password) {
        this.identifier = identifier;
        this.login = login;
        this.password = password;
        this.logged = false;
        this.active = true;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isLogged() {
        return logged;
    }

    public void setLogged() { this.logged = true; }

    public ObjectId getIdentifier() {
        return identifier;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public void logOut() {
        if (!logged) {
            return;
        }
        logged = false;
    }

    public void changePassword(String oldPassword, String newPassword) throws Exception {
        if (!oldPassword.equals(this.password)) {
            throw new InvalidPasswordException(oldPassword);
        }
        this.password = newPassword;
    }

    protected abstract Message handleMessageAndRespond(Message message);

    static class InvalidPasswordException extends Exception {
        public InvalidPasswordException(String message) {
            super(message);
        }

        public InvalidPasswordException() {
            super();
        }
    }
}
