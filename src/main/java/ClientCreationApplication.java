import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Date;

public class ClientCreationApplication extends Application {

    private final String login;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final Date birthDate;
    private final String phoneNumber;
    private final String email;
    private final String billCountryCode;
    private final Currency billCurrency;

    public ClientCreationApplication(
        String login,
        String password,
        String firstName,
        String lastName,
        Date birthDate,
        String phoneNumber,
        String email,
        Currency billCurrency
    ) {
        super(new Date());
        this.login = login;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.billCountryCode = "PL";
        this.billCurrency = billCurrency;
    }

    public ClientCreationApplication(
            ObjectId identifier,
            String login,
            String password,
            String firstName,
            String lastName,
            Date birthDate,
            String phoneNumber,
            String email,
            String billCountryCode,
            Currency billCurrency,
            Date submissionDate,
            State state
    ) {
        super(identifier, submissionDate, state);
        this.login = login;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.billCountryCode = billCountryCode;
        this.billCurrency = billCurrency;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getBillCountryCode() {
        return billCountryCode;
    }

    public Currency getBillCurrency() {
        return billCurrency;
    }

    @Override
    protected void reviewPositivelyAction(Employee employee) {
        employee.getEmployeeServices().addRequest(employee, employee.getEmployeeServices().createClientCreationOperation(this));
        employee.sendEmail(
                email,
                "Wniosek o założenie konta",
                "Wniosek został rozpatrzony pozytywnie. Wkrótce konto zostanie otwarte."
        );
    }

    @Override
    protected void reviewNegativelyAction(Employee employee) {
        employee.sendEmail(
                email,
                "Wniosek o założenie konta",
                "Wniosek został rozpatrzony negatywnie. Konto nie zostanie utworzone."
        );
    }

    @Override
    protected Document convertToDocument() {
        Document document = super.convertToDocument();
        document.put("type", "clientCreation");
        document.put("login", getLogin());
        document.put("password", getPassword());
        document.put("firstName", getFirstName());
        document.put("lastName", getLastName());
        document.put("birthDate", getBirthDate());
        document.put("phoneNumber", getPhoneNumber());
        document.put("email", getEmail());
        document.put("billCountryCode", getBillCountryCode());
        document.put("billCurrency", getBillCurrency().getAbbreviation());
        return document;
    }
}
