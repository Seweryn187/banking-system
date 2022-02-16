import java.util.List;

public interface EmployeeServices extends RequestQueueAccess<Employee> {
    List<Application> getApplications();
    System.ClientCreationOperation createClientCreationOperation(
            ClientCreationApplication application
    );
}
