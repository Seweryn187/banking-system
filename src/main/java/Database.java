import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Database {

    private static Database instance = null;

    private final Logger logger;
    private final MongoDatabase database;

    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    private Database() {
        logger = Logger.getLogger("org.mongodb.driver");
        logger.setLevel(Level.SEVERE);

        database = MongoClients.create().getDatabase("bank");
    }

    public Logger getLogger() {
        return logger;
    }

    public MongoDatabase getDatabase() {
        return database;
    }
}
