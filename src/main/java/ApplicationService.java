import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ApplicationService {

    private final ConcurrentMap<Application, User> applications;

    public ApplicationService() {
        this.applications = new ConcurrentHashMap<>();
    }

    public List<Application> getApplications() {
        return new ArrayList<>(this.applications.keySet());
    }

    public boolean add(User user, Application application) {
        return this.applications.put(application, Objects.requireNonNullElse(user, User.NONEXISTENT_USER)) == null;
    }

    public Application getApplication(ObjectId identifier) {
        for (Application application : applications.keySet()) {
            if (application.getIdentifier().equals(identifier)) {
                return application;
            }
        }
        return null;
    }
}
