import communication.Message;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class Employee extends User {

    public static final Pattern loginPattern = Pattern.compile("[a-zA-Z0-9]{12}");

    private final EmployeeServices employeeServices;

    protected Employee(
            ObjectId identifier,
            String login,
            String password,
            String firstName,
            String lastName,
            Date birthDate,
            String phoneNumber,
            String email,
            EmployeeServices employeeServices
    ) {
        super(identifier, login, password, firstName, lastName, birthDate, phoneNumber, email);
        this.employeeServices = employeeServices;
    }

    public EmployeeServices getEmployeeServices() {
        return employeeServices;
    }

    public List<Application> getApplications() {
        return employeeServices.getApplications();
    }

    public Application getApplication(int index) {
        return employeeServices.getApplications().get(index);
    }

    public boolean removeApplication(Application application) {
        return employeeServices.getApplications().remove(application);
    }

    public void sendEmail(String email, String title, String content) {
        // send email
    }

    @Override
    public Message handleMessageAndRespond(Message message) {
        return null;
    }
}
