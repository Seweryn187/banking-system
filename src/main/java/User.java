import communication.Message;
import org.bson.types.ObjectId;

import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.regex.Pattern;

public abstract class User extends Signable {

    public static final User NONEXISTENT_USER = new User(null,null,null,null,null, null, null, null) {
        @Override
        public boolean isActive() {
            throw new IllegalStateException();
        }

        @Override
        public boolean isLogged() {
            throw new IllegalStateException();
        }

        @Override
        public ObjectId getIdentifier() {
            throw new IllegalStateException();
        }

        @Override
        public void setLogged() {
            throw new IllegalStateException();
        }

        @Override
        public String getLogin() {
            throw new IllegalStateException();
        }

        @Override
        public void logOut() {
            throw new IllegalStateException();
        }

        @Override
        public void changePassword(String oldPassword, String newPassword) throws Exception {
            throw new IllegalStateException();
        }

        @Override
        protected Message handleMessageAndRespond(Message message) {
            throw new IllegalStateException();
        }

        @Override
        public String getFirstName() {
            throw new IllegalStateException();
        }

        @Override
        public void setFirstName(String firstName) {
            throw new IllegalStateException();
        }

        @Override
        public String getLastName() {
            throw new IllegalStateException();
        }

        @Override
        public void setLastName(String lastName) {
            throw new IllegalStateException();
        }

        @Override
        public Date getBirthDate() {
            throw new IllegalStateException();
        }

        @Override
        public String getPhoneNumber() {
            throw new IllegalStateException();
        }

        @Override
        public void setPhoneNumber(String phoneNumber) {
            throw new IllegalStateException();
        }

        @Override
        public String getEmail() {
            throw new IllegalStateException();
        }

        @Override
        public void setEmail(String email) {
            throw new IllegalStateException();
        }

        @Override
        public int getAge() {
            throw new IllegalStateException();
        }
    };
    public static final Pattern phonePattern = Pattern.compile("\\d{3}-?\\d{3}-?\\d{3}");
    public static final Pattern emailPattern = Pattern.compile("^(.+)@(.+)$");

    private String firstName;
    private String lastName;
    private final Date birthDate;
    private String phoneNumber;
    private String email;

    protected User(
            ObjectId identifier,
            String login,
            String password,
            String firstName,
            String lastName,
            Date birthDate,
            String phoneNumber,
            String email)
    {
        super(identifier, login, password);
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    public static Pattern getPhonePattern() {
        return phonePattern;
    }

    public static Pattern getEmailPattern() {
        return emailPattern;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getAge() {
        return Period.between(
                birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        ).getYears();
    }
}
