import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import communication.Message;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class System {

    private static System instance = null;
    private static final long MAX_TIME_FOR_IMMEDIATE_EXECUTION = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);

    private static final Pairing<Operation, Signable> EMPTY_REQUEST = new Pairing<>(null, null) {
        @Override
        public Operation getItem1() {
            throw new IllegalStateException();
        }

        @Override
        public Signable getItem2() {
            throw new IllegalStateException();
        }
    };
    private final BlockingQueue<Pairing<Operation, Signable>> requestedOperationsQueue;

    private final Database database;
    private final Generator generator;

    private final ConcurrentMap<String, Client> clients;
    private final ConcurrentMap<String, Employee> employees;
    private final ConcurrentMap<String, ATM> atms;

    private final BillService billService;
    private final ApplicationService applicationService;
    private final ExecutionQueue executionQueue;
    private final Timetable timetable;

    private final AtomicBoolean killSwitch = new AtomicBoolean(false);
    private final ExecutorService requestProcessingExecutor;
    private final ExecutorService operationSchedulingExecutor;
    private final ExecutorService operationExecutingExecutor;
    private final ExecutorService connectionsHandlers;

    public static System getInstance() {
        return System.instance;
    }

    public static System getInstance(BigDecimal startingMoney) {
        if (startingMoney != null && System.instance == null) {
            return System.instance = new System(startingMoney);
        }
        return System.instance;
    }

    private System(BigDecimal startingMoney) {
        this.requestedOperationsQueue = new LinkedBlockingQueue<>();

        this.database = Database.getInstance();
        this.generator = new Generator(getIdentifiers(), getCardNumbers());

        this.clients = new ConcurrentHashMap<>();
        this.employees = new ConcurrentHashMap<>();
        this.atms = new ConcurrentHashMap<>();

        if (!loadSignablesAndCheckForAdminPresence()) {
            createDefaultAdmin();
        }

        Bill bankBill = loadBankBill();
        if (bankBill == null) {
            this.billService = new BillService(
                    new Bill("PL", "00000000000000000000000000", Currency.PLN)
            );
            this.billService.getBankBill().deposit(startingMoney);
        } else {
            this.billService = new BillService(loadBankBill());
        }
        loadBills();
        loadCards();
        loadOperations();

        this.applicationService = new ApplicationService();
        loadApplications();

        this.executionQueue = ExecutionQueue.getInstance(new ExecutionVisitor());
        this.timetable = Timetable.getInstance();

        this.requestProcessingExecutor = Executors.newSingleThreadExecutor();
        this.operationSchedulingExecutor = Executors.newSingleThreadExecutor();
        this.operationExecutingExecutor = Executors.newSingleThreadExecutor();
        this.connectionsHandlers = Executors.newFixedThreadPool(50);
    }

    private static boolean isCloseToExecutionDate(Date date, Date executionDate) {
        if (executionDate == null) {
            return true;
        }
        return executionDate.getTime() - date.getTime() <= MAX_TIME_FOR_IMMEDIATE_EXECUTION;
    }

    private static boolean isCloseToExecutionDate(Date executionDate) {
        return isCloseToExecutionDate(new Date(), executionDate);
    }

    Signable signIn(String login, String password) {
        Signable signable = this.clients.get(login);
        if (signable != null && password.equals(signable.getPassword())) {
            signable.setLogged();
            return signable;
        }
        signable = this.employees.get(login);
        if (signable != null && password.equals(signable.getPassword())) {
            signable.setLogged();
            return signable;
        }
        return null;
    }

    private ClientServices getClientServices() {
        return new ClientServices() {
            @Override
            public TransferOperation createTransferOperation(String title, String senderIban, String recipientIban, BigDecimal amount) {
                return System.this.createTransferOperation(title, senderIban, recipientIban, amount);
            }

            @Override
            public TransferOperation createTransferOperation(String title, String senderIban, String recipientIban, BigDecimal amount, Date executionDate) {
                return System.this.createTransferOperation(title, senderIban, recipientIban, amount, executionDate);
            }

            @Override
            public StartInvestmentOperation createInvestmentOperation(String investorIban, BigDecimal amount, InvestmentPeriod investmentPeriod) {
                return System.this.createInvestmentOperation(investorIban, amount, investmentPeriod);
            }

            @Override
            public BillCreationOperation createBillCreationOperation(Client client, Currency currency) {
                return System.this.createBillCreationOperation(client, currency);
            }

            @Override
            public OrderNewCardOperation createOrderNewCardOperation(String iban) {
                return System.this.createOrderNewCardOperation(iban);
            }

            @Override
            public boolean addRequest(Client requester, Operation operation) {
                return System.this.addRequest(requester, operation);
            }
        };
    }

    private EmployeeServices getEmployeeServices() {
        return new EmployeeServices() {
            @Override
            public List<Application> getApplications() {
                return System.this.applicationService.getApplications();
            }

            @Override
            public ClientCreationOperation createClientCreationOperation(ClientCreationApplication application) {
                return System.this.createClientCreationOperation(application);
            }

            @Override
            public boolean addRequest(Employee requester, Operation operation) {
                return System.this.addRequest(requester, operation);
            }
        };
    }

    private List<String> getIdentifiers() {
        FindIterable<Document> billsIterator = database.getDatabase().getCollection("bills").find();
        List<String> identifiers = new LinkedList<>();
        for (Document billIterator : billsIterator) {
            String number = billIterator.getString("number");
            identifiers.add(number);
        }
        return identifiers;
    }

    private List<String> getCardNumbers() {
        FindIterable<Document> cardsIterator = database.getDatabase().getCollection("cards").find();
        List<String> cardNumbers = new LinkedList<>();
        for (Document cardIterator : cardsIterator) {
            String number = cardIterator.getString("number");
            cardNumbers.add(number);
        }
        return cardNumbers;
    }

    private void createDefaultAdmin() {
        Admin admin = new Admin(
                new ObjectId(),
                "ADMIN0000000",
                "admin",
                "Admin",
                "Admin",
                new Date(),
                "000-000-000",
                "admin@bank.pl",
                getEmployeeServices()
        );
        this.employees.put("ADMIN0000000", admin);
    }

    private boolean loadSignablesAndCheckForAdminPresence() {
        boolean isAdminPresent = false;
        FindIterable<Document> usersIterator = database.getDatabase().getCollection("users").find();
        for (Document userIterator : usersIterator) {
            ObjectId identifier = userIterator.getObjectId("_id");
            String login = userIterator.getString("login");
            String password = userIterator.getString("password");
            String firstName = userIterator.getString("firstName");
            String lastName = userIterator.getString("lastName");
            Date birthDate = userIterator.getDate("birthDate");
            String phoneNumber = userIterator.getString("phoneNumber");
            String email = userIterator.getString("email");
            switch (userIterator.getString("type")) {
                case "Admin": {
                    this.employees.put(login, new Admin(
                            identifier,
                            login,
                            password,
                            firstName,
                            lastName,
                            birthDate,
                            phoneNumber,
                            email,
                            getEmployeeServices()
                    ));
                    isAdminPresent = true;
                } break;
                case "Employee": {
                    this.employees.put(login, new Employee(
                            identifier,
                            login,
                            password,
                            firstName,
                            lastName,
                            birthDate,
                            phoneNumber,
                            email,
                            getEmployeeServices()
                    ));
                } break;
                case "Client": {
                    this.clients.put(login, new Client(
                            identifier,
                            login,
                            password,
                            firstName,
                            lastName,
                            birthDate,
                            phoneNumber,
                            email,
                            getClientServices()
                    ));
                } break;
            }
        }
        return isAdminPresent;
    }

    private void updateSignables() {
        MongoCollection<Document> users = database.getDatabase().getCollection("users");
        Consumer<User> consumer = (user) -> {
            BasicDBObject filter = new BasicDBObject("login", user.getLogin());
            if (users.find(filter).first() != null) {
                BasicDBObject document = new BasicDBObject();
                document.put("password", user.getPassword());
                document.put("firstName", user.getFirstName());
                document.put("lastName", user.getLastName());
                document.put("phoneNumber", user.getPhoneNumber());
                document.put("email", user.getEmail());
                users.updateOne(filter, new BasicDBObject("$set", document));
            } else {
                Document document = new Document();
                document.put("_id", user.getIdentifier());
                document.put("type", user.getClass().getSimpleName());
                document.put("login", user.getLogin());
                document.put("password", user.getPassword());
                document.put("firstName", user.getFirstName());
                document.put("lastName", user.getLastName());
                document.put("birthDate", user.getBirthDate());
                document.put("phoneNumber", user.getPhoneNumber());
                document.put("email", user.getEmail());
                users.insertOne(document);
            }
        };
        this.clients.values().forEach(consumer);
        this.employees.values().forEach(consumer);
    }

    private Bill loadBankBill() {
        BasicDBObject query = new BasicDBObject("clientLogin", new BasicDBObject("$exists", false));
        Document bankBill = database.getDatabase().getCollection("bills").find(query).first();
        if (bankBill == null) {
            return null;
        }
        String countryCode = bankBill.getString("countryCode");
        String number = bankBill.getString("number");
        Currency currency = Currency.getCurrency(bankBill.getString("currency"));
        BigDecimal currentBalance = new BigDecimal(bankBill.getString("balance"));
        BigDecimal balanceOnHold = new BigDecimal(bankBill.getString("balanceOnHold"));
        return new Bill(countryCode, number, currency, currentBalance, balanceOnHold);
    }

    private void loadBills() {
        BasicDBObject query = new BasicDBObject("clientLogin", new BasicDBObject("$exists", true));
        FindIterable<Document> billsIterator = database.getDatabase().getCollection("bills").find(query);
        for (Document billIterator : billsIterator) {
            String clientLogin = billIterator.getString("clientLogin");
            String countryCode = billIterator.getString("countryCode");
            String number = billIterator.getString("number");
            Currency currency = Currency.getCurrency(billIterator.getString("currency"));
            BigDecimal currentBalance = new BigDecimal(billIterator.getString("balance"));
            BigDecimal balanceOnHold = new BigDecimal(billIterator.getString("balanceOnHold"));

            this.billService.add(
                    this.clients.get(clientLogin), new Bill(countryCode, number, currency, currentBalance, balanceOnHold)
            );
        }
    }

    private void updateBankBill() {
        MongoCollection<Document> bills = database.getDatabase().getCollection("bills");
        BasicDBObject filter = new BasicDBObject("clientLogin", new BasicDBObject("$exists", false));
        Bill bankBill = this.billService.getBankBill();
        if (bills.find(filter).first() != null) {
            BasicDBObject document = new BasicDBObject();
            document.put("balance", bankBill.getCurrentBalance().toString());
            document.put("balanceOnHold", bankBill.getBalanceOnHold().toString());
            bills.updateOne(filter, new BasicDBObject("$set", document));
        } else {
            Document document = new Document();
            document.put("_id", new ObjectId());
            document.put("countryCode", bankBill.getCountryCode());
            document.put("number", bankBill.getNumber());
            document.put("currency", bankBill.getBillCurrency().getAbbreviation());
            document.put("balance", bankBill.getCurrentBalance().toString());
            document.put("balanceOnHold", bankBill.getBalanceOnHold().toString());
            bills.insertOne(document);
        }
    }

    private void updateBills() {
        MongoCollection<Document> bills = database.getDatabase().getCollection("bills");
        Consumer<Pairing<Bill, Client>> consumer = billClientPairing -> {
            BasicDBObject filter = new BasicDBObject();
            filter.put("countryCode", billClientPairing.getItem1().getCountryCode());
            filter.put("number", billClientPairing.getItem1().getNumber());
            if (bills.find(filter).first() != null) {
                BasicDBObject document = new BasicDBObject();
                document.put("balance", billClientPairing.getItem1().getCurrentBalance().toString());
                document.put("balanceOnHold", billClientPairing.getItem1().getBalanceOnHold().toString());
                bills.updateOne(filter, new BasicDBObject("$set", document));
            } else {
                Document document = new Document();
                document.put("_id", new ObjectId());
                document.put("countryCode", billClientPairing.getItem1().getCountryCode());
                document.put("number", billClientPairing.getItem1().getNumber());
                document.put("currency", billClientPairing.getItem1().getBillCurrency().getAbbreviation());
                document.put("balance", billClientPairing.getItem1().getCurrentBalance().toString());
                document.put("balanceOnHold", billClientPairing.getItem1().getBalanceOnHold().toString());
                document.put("clientLogin", billClientPairing.getItem2().getLogin());
                bills.insertOne(document);
            }
        };
        this.billService.values().forEach(consumer);
    }

    private void loadCards() {
        FindIterable<Document> cardsIterator = database.getDatabase().getCollection("cards").find();
        for (Document cardIterator : cardsIterator) {
            String number = cardIterator.getString("number");
            String iban = cardIterator.getString("iban");
            byte[] pin = cardIterator.getString("pin").getBytes();
            byte[] cvv = cardIterator.getString("cvv").getBytes();
            Date validityEndDate = cardIterator.getDate("validityEndDate");
            boolean active = cardIterator.getBoolean("active");

            this.billService.get(iban).getItem1().addCard(new ElectronicCard(
                    number,
                    iban,
                    pin,
                    cvv,
                    validityEndDate,
                    active
            ));
        }
    }

    private void updateCards() {
        MongoCollection<Document> cards = database.getDatabase().getCollection("cards");
        Consumer<Pairing<Bill, Client>> consumer = billClientPairing -> {
            billClientPairing.getItem1().getCards().forEach((card) -> {
                BasicDBObject filter = new BasicDBObject();
                filter.put("number", card.getNumber());
                if (cards.find(filter).first() != null) {
                    BasicDBObject document = new BasicDBObject();
                    document.put("pin", new String(card.getPin()));
                    document.put("active", card.isActive());
                    cards.updateOne(filter, new BasicDBObject("$set", document));
                } else {
                    Document document = new Document();
                    document.put("_id", new ObjectId());
                    document.put("number", card.getNumber());
                    document.put("iban", card.getBillIban());
                    document.put("pin", new String(card.getPin()));
                    document.put("cvv", new String(card.getCvvCode()));
                    document.put("validityEndDate", card.getValidityEndDate());
                    document.put("active", card.isActive());
                    cards.insertOne(document);
                }
            });
        };
        this.billService.values().forEach(consumer);
    }

    private void loadOperations() {
        FindIterable<Document> operationsIterator = database.getDatabase().getCollection("operations").find();
        for (Document operationIterator : operationsIterator) {
            ObjectId identifier = operationIterator.getObjectId("_id");
            Date creationDate = operationIterator.getDate("creationDate");
            Date executionDate = operationIterator.getDate("executionDate");
            Operation.State state = Operation.State.valueOf(operationIterator.getString("state"));
            Bill ownerBill = null;
            Operation operation = null;
            switch (operationIterator.getString("type")) {
                case "transfer": {
                    String title = operationIterator.getString("title");
                    String senderIban = operationIterator.getString("senderIban");
                    Bill sender = this.billService.get(senderIban).getItem1();
                    String recipientIban = operationIterator.getString("recipientIban");
                    BigDecimal amount = new BigDecimal(operationIterator.getString("amount"));
                    TransferOperationType type = TransferOperationType.valueOf(operationIterator.getString("transferType"));
                    operation = new TransferOperation(
                            title,
                            sender,
                            this.billService.get(recipientIban).getItem1(),
                            amount,
                            type,
                            identifier,
                            creationDate,
                            executionDate,
                            state
                    );
                    ownerBill = sender;
                } break;
                case "startInvestment": {
                    String investorIban = operationIterator.getString("investorIban");
                    Bill investor = this.billService.get(investorIban).getItem1();
                    BigDecimal amount = new BigDecimal(operationIterator.getString("amount"));
                    Double interestRate = operationIterator.getDouble("interestRate");
                    InvestmentPeriod investmentPeriod = InvestmentPeriod.get(operationIterator.getString("investmentPeriod"));
                    operation = new StartInvestmentOperation(
                            investor,
                            this.billService.getBankBill(),
                            amount,
                            interestRate,
                            investmentPeriod,
                            identifier,
                            creationDate,
                            executionDate,
                            state
                    );
                    ownerBill = investor;
                } break;
                case "endInvestment": {
                    String investorIban = operationIterator.getString("investorIban");
                    Bill investor = this.billService.get(investorIban).getItem1();
                    Bill bankBill = this.billService.getBankBill();
                    BigDecimal amount = new BigDecimal(operationIterator.getString("amount"));
                    operation = new EndInvestmentOperation(
                            investor,
                            this.billService.getBankBill(),
                            amount,
                            identifier,
                            creationDate,
                            executionDate,
                            state
                    );
                    if (state == Operation.State.EXECUTED) {
                        ownerBill = bankBill;
                    } else {
                        ownerBill = investor;
                    }
                } break;
            }
            if (operation == null || ownerBill == null) {
                continue;
            }
            if (state == Operation.State.EXECUTED) {
                ownerBill.addToHistory(operation);
            } else {
                this.addRequest(this.billService.get(ownerBill.getIban()).getItem2(), operation);
            }
        }
    }

    private void updateOperation(MongoCollection<Document> operations, Operation operation) {
        BasicDBObject filter = new BasicDBObject("_id", operation.getIdentifier());
        if (operations.find(filter).first() != null) {
            operations.updateOne(filter, new BasicDBObject("$set", operation.updatedObject()));
        } else {
            Document document = operation.convertToDocument();
            if (document != null) {
                operations.insertOne(operation.convertToDocument());
            }
        }
    }

    private void updateOperations() {
        MongoCollection<Document> operations = database.getDatabase().getCollection("operations");
        Consumer<Operation> operationConsumer = operation -> {
            updateOperation(operations, operation);
        };
        Consumer<Pairing<Bill, Client>> pairingConsumer = billClientPairing -> {
            billClientPairing.getItem1().getScheduledOperations().forEach(operationConsumer);
            billClientPairing.getItem1().getHistory().forEach(operationConsumer);
        };
        this.billService.values().forEach(pairingConsumer);
        while (!this.timetable.isEmpty()) {
            Operation operation = this.timetable.get();
            updateOperation(operations, operation);
            this.timetable.remove(operation);
        }
        while (!this.executionQueue.isEmpty()) {
            Operation operation = this.executionQueue.get();
            updateOperation(operations, operation);
            this.executionQueue.remove(operation);
        }
    }

    private void loadApplications() {
        FindIterable<Document> applicationsIterator = database.getDatabase().getCollection("applications").find();
        for (Document applicationIterator : applicationsIterator) {
            ObjectId identifier = applicationIterator.getObjectId("_id");
            Application.State status = Application.State.valueOf(applicationIterator.getString("status"));
            Date submissionDate = applicationIterator.getDate("submissionDate");

            Application application = null;
            switch (applicationIterator.getString("type")) {
                case "clientCreation": {
                    String login = applicationIterator.getString("login");
                    String password = applicationIterator.getString("password");
                    String firstName = applicationIterator.getString("firstName");
                    String lastName = applicationIterator.getString("lastName");
                    Date birthDate = applicationIterator.getDate("birthDate");
                    String phoneNumber = applicationIterator.getString("phoneNumber");
                    String email = applicationIterator.getString("email");
                    String billCountryCode = applicationIterator.getString("billCountryCode");
                    Currency currency = Currency.getCurrency(applicationIterator.getString("billCurrency"));
                    application = new ClientCreationApplication(
                            identifier,
                            login,
                            password,
                            firstName,
                            lastName,
                            birthDate,
                            phoneNumber,
                            email,
                            billCountryCode,
                            currency,
                            submissionDate,
                            status
                    );
                } break;
            }
            String clientLogin = applicationIterator.getString("clientLogin");
            if (clientLogin != null) {
                this.applicationService.add(this.clients.get(clientLogin), application);
            } else {
                this.applicationService.add(User.NONEXISTENT_USER, application);
            }
        }
    }

    private void updateApplications() {
        MongoCollection<Document> applications = database.getDatabase().getCollection("applications");
        this.applicationService.getApplications().forEach(application -> {
            BasicDBObject filter = new BasicDBObject();
            filter.put("_id", application.getIdentifier());
            if (applications.find(filter).first() != null) {
                BasicDBObject document = new BasicDBObject();
                document.put("status", application.getStatus());
                applications.updateOne(filter, new BasicDBObject("$set", document));
            } else {
                Document document = application.convertToDocument();
                applications.insertOne(document);
            }
        });
    }

    public void createAndAddBill(Client client, String billCountryCode, Currency billCurrency) {
        Bill bill = new Bill(billCountryCode, generator.generateUniqueId(Generator.UNIQUE_BILL_NUMBER_LENGTH), billCurrency);
        this.billService.add(client, bill);
    }

    public Client createClientAndHisBill(
            String login,
            String password,
            String firstName,
            String lastName,
            Date birthDate,
            String phoneNumber,
            String email,
            String billCountryCode,
            Currency billCurrency
    ) {
        Client client = new Client(
                new ObjectId(),
                login,
                password,
                firstName,
                lastName,
                birthDate,
                phoneNumber,
                email,
                this.getClientServices()
        );
        createAndAddBill(client, billCountryCode, billCurrency);
        this.clients.put(login, client);
        return client;
    }

    public TransferOperation createTransferOperation(
            String title,
            String senderIban,
            String recipientIban,
            BigDecimal amount
    ) {
        return createTransferOperation(title, senderIban, recipientIban, amount, new Date());
    }

    public TransferOperation createTransferOperation(
            String title,
            String senderIban,
            String recipientIban,
            BigDecimal amount,
            Date executionDate
    ) {
        if (senderIban.equals(recipientIban)) {
            throw new IllegalArgumentException("senderIban recipientIban");
        }

        Pairing<Bill, Client> senderPairing = this.billService.get(senderIban);
        if (senderPairing == null) {
            throw new IllegalArgumentException("senderIban");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0
                || !senderPairing.getItem1().hold(amount)) {
            throw new IllegalArgumentException("amount");
        }

        Pairing<Bill, Client> recipientPairing = this.billService.get(recipientIban);
        if (recipientPairing == null) {
            throw new IllegalArgumentException("recipientIban");
        }

        TransferOperationType type;
        if (senderPairing.getItem2() == recipientPairing.getItem2()) {
            type = TransferOperationType.PRIVATE;
        } else {
            type = TransferOperationType.FOREIGN;
        }

        return new TransferOperation(
                title,
                senderPairing.getItem1(),
                recipientPairing.getItem1(),
                amount,
                type,
                executionDate
        );
    }

    public StartInvestmentOperation createInvestmentOperation(
            String investorIban,
            BigDecimal amount,
            InvestmentPeriod investmentPeriod
    ) {
        Pairing<Bill, Client> investorPairing = this.billService.get(investorIban);
        if (investorPairing == null) {
            throw new IllegalArgumentException("senderIban");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0
                || !investorPairing.getItem1().hold(amount)) {
            throw new IllegalArgumentException("amount");
        }

        return new StartInvestmentOperation(
                investorPairing.getItem1(),
                amount,
                BillService.INTEREST_RATE,
                investmentPeriod
        );
    }

    public ClientCreationApplication createClientCreationApplication(
            String login,
            String password,
            String firstName,
            String lastName,
            Date birthDate,
            String phoneNumber,
            String email,
            Currency billCurrency
    ) {
        if (this.clients.get(login) != null) {
            throw new IllegalArgumentException("login");
        }

        return new ClientCreationApplication(
                login,
                password,
                firstName,
                lastName,
                birthDate,
                phoneNumber,
                email,
                billCurrency
        );
    }

    public ClientCreationOperation createClientCreationOperation(ClientCreationApplication application) {
        if (application.getStatus() != Application.State.POSITIVE) {
            throw new IllegalArgumentException("application");
        }

        return new ClientCreationOperation(application);
    }

    public BillCreationOperation createBillCreationOperation(Client client, Currency currency) {
        return new BillCreationOperation(
                client,
                currency
        );
    }

    public OrderNewCardOperation createOrderNewCardOperation(String iban) {
        Pairing<Bill, Client> billClientPairing = this.billService.get(iban);
        if (billClientPairing == null) {
            throw new IllegalArgumentException("iban");
        }

        return new OrderNewCardOperation(billClientPairing.getItem1());
    }

    private boolean addRequest(Signable requester, Operation operation) {
        return this.requestedOperationsQueue.add(new Pairing<>(operation,requester));
    }

    public boolean addApplication(User applicant, Application application) {
        return this.applicationService.add(applicant, application);
    }

    private static void println(Object o) {
        java.lang.System.out.println(o.toString());
    }

    private void processingRequests() {
        while (!killSwitch.get()) {
            Pairing<Operation, Signable> request = EMPTY_REQUEST;
            try {
                request = this.requestedOperationsQueue.take();
            } catch (InterruptedException e) {
                continue;
            }

            if (isCloseToExecutionDate(request.getItem1().getExecutionDate())) {
                this.executionQueue.enqueue(request.getItem1());
            } else {
                this.timetable.add(request.getItem1());
            }
        }
    }

    private void schedulingOperations() {
        while (!killSwitch.get()) {
            while (true) {
                Operation operation = this.timetable.get();
                if (operation == null || !isCloseToExecutionDate(operation.getExecutionDate())) {
                    break;
                }
                this.timetable.remove(operation);
                this.executionQueue.enqueue(operation);
            }
            Thread.yield();
        }
    }

    private void executingOperations() {
        while (!killSwitch.get()) {
            try {
                this.executionQueue.executeNextOperation();
            } catch (InterruptedException e) {
                continue;
            }
        }
    }

    public void start() {
        this.operationExecutingExecutor.submit(this::executingOperations);
        this.operationSchedulingExecutor.submit(this::schedulingOperations);
        this.requestProcessingExecutor.submit(this::processingRequests);
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.requestProcessingExecutor.awaitTermination(timeout, unit) &&
                this.operationSchedulingExecutor.awaitTermination(timeout, unit) &&
                this.operationExecutingExecutor.awaitTermination(timeout, unit);
    }

    public void shutdownNow() {
        this.killSwitch.set(true);

        this.requestProcessingExecutor.shutdownNow();
        this.operationSchedulingExecutor.shutdownNow();
        this.operationExecutingExecutor.shutdownNow();

        updateSignables();
        updateBankBill();
        updateBills();
        updateCards();
        updateOperations();
        updateApplications();
    }

    public final class ExecutionVisitor {

        private BigDecimal calculateInvestmentFinalAmount(int count, double interestRate, BigDecimal amount) {
            double tmp = interestRate / count + 1.0;
            tmp = Math.pow(tmp, count);
            return amount.multiply(new BigDecimal(tmp)).setScale(2, RoundingMode.HALF_UP);
        }

        public void executeTransfer(TransferOperation transferOperation) {
            BigDecimal amount = transferOperation.getAmount();
            transferOperation.getSender().release(amount);
            transferOperation.getSender().withdraw(amount);
            transferOperation.getRecipient().deposit(amount);

            if (transferOperation.getSender().moveToHistory(transferOperation)) {
                transferOperation.getSender().addToHistory(transferOperation);
            }
            transferOperation.getRecipient().addToHistory(transferOperation);
        }

        public void executeStartInvestment(StartInvestmentOperation investmentOperation) {
            BigDecimal amount = investmentOperation.getAmount();
            investmentOperation.getInvestorBill().release(amount);
            investmentOperation.getInvestorBill().withdraw(amount);
            investmentOperation.getBankBill().deposit(amount);

            BigDecimal finalAmount = calculateInvestmentFinalAmount(
                    investmentOperation.getInvestmentPeriod().getCapitalizationCount(),
                    investmentOperation.getInterestRate(),
                    amount
            );
            addRequest(
                    billService.get(investmentOperation.getInvestorBill().getIban()).getItem2(),
                    new EndInvestmentOperation(
                            investmentOperation.getInvestorBill(),
                            finalAmount,
                            new Date(
                                    investmentOperation.getExecutionDate().getTime()
                                            + investmentOperation.getInvestmentPeriod().getMilliseconds()
                            )
                    )
            );

            investmentOperation.getBankBill().addToHistory(investmentOperation);
        }

        public void executeEndInvestment(EndInvestmentOperation investmentOperation) {
            BigDecimal amount = investmentOperation.getAmount();
            investmentOperation.getBankBill().withdraw(amount);
            investmentOperation.getInvestorBill().deposit(amount);

            investmentOperation.getInvestorBill().addToHistory(investmentOperation);
        }

        public void executeATMOperation(ATMOperation atmOperation) {

        }

        public void executeClientCreation(ClientCreationOperation clientCreationOperation) {
            ClientCreationApplication application = clientCreationOperation.getApplication();
            createClientAndHisBill(
                    application.getLogin(),
                    application.getPassword(),
                    application.getFirstName(),
                    application.getLastName(),
                    application.getBirthDate(),
                    application.getPhoneNumber(),
                    application.getEmail(),
                    application.getBillCountryCode(),
                    application.getBillCurrency()
            );
        }

        public void executeBillCreation(BillCreationOperation billCreationOperation) {
            createAndAddBill(
                    billCreationOperation.getClient(),
                    "PL",
                    billCreationOperation.getBillCurrency()
            );
        }

        public void executeNewCardOrder(OrderNewCardOperation orderNewCardOperation) {
            orderNewCardOperation.getBill().addCard(new ElectronicCard(
                    generator.generateCardNumber(Generator.UNIQUE_CARD_NUMBER_LENGTH),
                    orderNewCardOperation.getBill().getIban(),
                    generator.generatePin(),
                    generator.generateCvvCode(),
                    new Date(new Date().getTime() + ElectronicCard.VALIDITY_TIME)
            ));
        }
    }

    public enum TransferOperationType {
        PRIVATE,
        FOREIGN
    }

    public class TransferOperation extends Operation {

        private final String title;
        private final Bill sender;
        private final Bill recipient;
        private final BigDecimal amount;
        private final TransferOperationType type;

        private TransferOperation(
                String title,
                Bill sender,
                Bill recipient,
                BigDecimal amount,
                TransferOperationType type,
                ObjectId identifier,
                Date creationDate,
                Date executionDate,
                Operation.State state
        ) {
            super(identifier, creationDate, executionDate, state);
            this.title = title;
            this.sender = sender;
            this.recipient = recipient;
            this.amount = amount;
            this.type = type;
        }

        private TransferOperation(
                String title,
                Bill sender,
                Bill recipient,
                BigDecimal amount,
                TransferOperationType type,
                Date executionDate
        ) {
            super(new ObjectId(), executionDate);
            this.title = title;
            this.sender = sender;
            this.recipient = recipient;
            this.amount = amount;
            this.type = type;
        }

        private TransferOperation(
                String title,
                Bill sender,
                Bill recipient,
                BigDecimal amount,
                TransferOperationType type
        ) {
            this(title, sender, recipient, amount, type, new Date());
        }

        public String getTitle() {
            return title;
        }

        public Bill getSender() {
            return sender;
        }

        public Bill getRecipient() {
            return recipient;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public TransferOperationType getType() {
            return type;
        }

        @Override
        public void executeAction(System.ExecutionVisitor executionVisitor) {
            executionVisitor.executeTransfer(this);
        }

        @Override
        protected Document convertToDocument() {
            Document document = super.convertToDocument();
            document.put("title", this.getTitle());
            document.put("senderIban", this.getSender().getIban());
            document.put("recipientIban", this.getRecipient().getIban());
            document.put("amount", String.valueOf(this.getAmount()));
            document.put("transferType", this.getType());
            return document;
        }

        @Override
        public String toString() {
            return "Transfer {" +
                    "title=" + title +
                    ", sender=" + sender.getIban() +
                    ", recipient=" + recipient.getIban() +
                    ", amount=" + amount +
                    ", type=" + type +
                    ", creationDate=" + getCreationDate() +
                    ", status=" + getState() +
                    "}";
        }
    }

    public static class InvestmentPeriod {
        public static final InvestmentPeriod ONE_MONTH = new InvestmentPeriod(
                TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS), 1, "ONE_MONTH");
        public static final InvestmentPeriod THREE_MONTHS = new InvestmentPeriod(
                TimeUnit.MILLISECONDS.convert(91, TimeUnit.DAYS), 3, "THREE_MONTHS");
        public static final InvestmentPeriod SIX_MONTHS = new InvestmentPeriod(
                TimeUnit.MILLISECONDS.convert(182, TimeUnit.DAYS), 6, "SIX_MONTHS");
        public static final InvestmentPeriod ONE_YEAR = new InvestmentPeriod(
                TimeUnit.MILLISECONDS.convert(365, TimeUnit.DAYS), 12, "ONE_YEAR");
        public static final InvestmentPeriod TWO_YEARS = new InvestmentPeriod(
                TimeUnit.MILLISECONDS.convert(730, TimeUnit.DAYS), 24, "TWO_YEARS");
        public static final InvestmentPeriod TEN_YEARS = new InvestmentPeriod(
                TimeUnit.MILLISECONDS.convert(3648, TimeUnit.DAYS), 120, "TEN_YEARS");

        public static InvestmentPeriod get(String name) {
            switch (name) {
                case "ONE_MONTH": return ONE_MONTH;
                case "THREE_MONTHS": return THREE_MONTHS;
                case "SIX_MONTHS": return SIX_MONTHS;
                case "ONE_YEAR": return ONE_YEAR;
                case "TWO_YEARS": return TWO_YEARS;
                case "TEN_YEARS": return TEN_YEARS;
            }
            return null;
        }

        final long milliseconds;
        final int capitalizationCount;
        final String name;

        private InvestmentPeriod(long milliseconds, int capitalizationCount, String name) {
            this.milliseconds = milliseconds;
            this.capitalizationCount = capitalizationCount;
            this.name = name;
        }

        public long getMilliseconds() {
            return milliseconds;
        }

        public int getCapitalizationCount() {
            return capitalizationCount;
        }

        public String getName() {
            return name;
        }
    }

    public class StartInvestmentOperation extends Operation {

        private final Bill investorBill;
        private final Bill bankBill;
        private final BigDecimal amount;
        private final double interestRate;
        private final InvestmentPeriod investmentPeriod;

        private StartInvestmentOperation(
                Bill investorBill,
                Bill bankBill,
                BigDecimal amount,
                double interestRate,
                InvestmentPeriod investmentPeriod,
                ObjectId identifier,
                Date creationDate,
                Date executionDate,
                Operation.State state
        ) {
            super(identifier, creationDate, executionDate, state);
            this.investorBill = investorBill;
            this.bankBill = bankBill;
            this.amount = amount;
            this.interestRate = interestRate;
            this.investmentPeriod = investmentPeriod;
        }

        private StartInvestmentOperation(
                Bill investorBill,
                BigDecimal amount,
                double interestRate,
                InvestmentPeriod investmentPeriod
        ) {
            super(new ObjectId());
            this.investorBill = investorBill;
            this.bankBill = billService.getBankBill();
            this.amount = amount;
            this.interestRate = interestRate;
            this.investmentPeriod = investmentPeriod;
        }

        public Bill getInvestorBill() {
            return investorBill;
        }

        public Bill getBankBill() {
            return bankBill;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public double getInterestRate() {
            return interestRate;
        }

        public InvestmentPeriod getInvestmentPeriod() {
            return investmentPeriod;
        }

        @Override
        protected void executeAction(ExecutionVisitor executionVisitor) {
            executionVisitor.executeStartInvestment(this);
        }

        @Override
        protected Document convertToDocument() {
            Document document = super.convertToDocument();
            document.put("investorIban", this.getInvestorBill().getIban());
            document.put("bankIban", this.getBankBill().getIban());
            document.put("amount", String.valueOf(this.getAmount()));
            document.put("interestRate", this.getInterestRate());
            document.put("investmentPeriod", this.getInvestmentPeriod().getName());
            return document;
        }

        @Override
        public String toString() {
            return "Investment {" +
                    "investor=" + investorBill.toString() +
                    ", amount=" + amount +
                    ", interestRate=" + interestRate +
                    ", investmentPeriod=" + investmentPeriod.getName() +
                    ", creationDate=" + getCreationDate() +
                    ", status=" + getState() +
                    "}";
        }
    }

    public class EndInvestmentOperation extends Operation {

        private final Bill bankBill;
        private final Bill investorBill;
        private final BigDecimal amount;

        private EndInvestmentOperation(
                Bill investorBill,
                Bill bankBill,
                BigDecimal amount,
                ObjectId identifier,
                Date creationDate,
                Date executionDate,
                Operation.State state
        ) {
            super(identifier, creationDate, executionDate, state);
            this.investorBill = investorBill;
            this.bankBill = bankBill;
            this.amount = amount;
        }

        private EndInvestmentOperation(
                Bill investorBill,
                BigDecimal amount,
                Date executionDate
        ) {
            super(new ObjectId(), executionDate);
            this.investorBill = investorBill;
            this.bankBill = System.getInstance().billService.getBankBill();
            this.amount = amount;
        }

        public Bill getBankBill() {
            return bankBill;
        }

        public Bill getInvestorBill() {
            return investorBill;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        @Override
        protected void executeAction(ExecutionVisitor executionVisitor) {
            executionVisitor.executeEndInvestment(this);
        }

        @Override
        protected Document convertToDocument() {
            Document document = super.convertToDocument();
            document.put("investorIban", this.getInvestorBill().getIban());
            document.put("bankIban", this.getBankBill().getIban());
            document.put("amount", String.valueOf(this.getAmount()));
            return document;
        }

        @Override
        public String toString() {
            return "Investment {" +
                    ", investor=" + investorBill.getIban() +
                    ", amount=" + amount +
                    ", creationDate=" + getCreationDate() +
                    ", status=" + getState() +
                    '}';
        }
    }

    public class ClientCreationOperation extends Operation {

        private final ClientCreationApplication application;

        private ClientCreationOperation(ClientCreationApplication application) {
            super(new ObjectId());
            this.application = application;
        }

        public ClientCreationApplication getApplication() {
            return application;
        }

        @Override
        protected void executeAction(System.ExecutionVisitor executionVisitor) {
            executionVisitor.executeClientCreation(this);
        }

        @Override
        protected Document convertToDocument() {
            return null;
        }
    }

    public class BillCreationOperation extends Operation {

        private final Client client;
        private final Currency billCurrency;

        private BillCreationOperation(Client client, Currency billCurrency) {
            super(new ObjectId());
            this.client = client;
            this.billCurrency = billCurrency;
        }

        public Client getClient() {
            return client;
        }

        public Currency getBillCurrency() {
            return billCurrency;
        }

        @Override
        protected void executeAction(ExecutionVisitor executionVisitor) {
            executionVisitor.executeBillCreation(this);
        }

        @Override
        protected Document convertToDocument() {
            return null;
        }
    }

    public class OrderNewCardOperation extends Operation {

        private final Bill bill;

        private OrderNewCardOperation(Bill bill) {
            super(new ObjectId());
            this.bill = bill;
        }

        public Bill getBill() {
            return bill;
        }

        @Override
        protected void executeAction(ExecutionVisitor executionVisitor) {
            executionVisitor.executeNewCardOrder(this);
        }

        @Override
        protected Document convertToDocument() {
            return null;
        }
    }

    class Admin extends Employee {

        public Admin(
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
            super(identifier, login, password, firstName, lastName, birthDate, phoneNumber, email, employeeServices);
        }

        public Employee createNewEmployee(
                String login,
                String password,
                String firstName,
                String lastName,
                Date birthDate,
                String phoneNumber,
                String email
        ) {
            if (System.this.employees.get(login) != null) {
                throw new IllegalArgumentException("login");
            }

            Employee employee = new Employee(
                    new ObjectId(),
                    login,
                    password,
                    firstName,
                    lastName,
                    birthDate,
                    phoneNumber,
                    email,
                    getEmployeeServices()
            );
            System.this.employees.put(login, employee);
            return employee;
        }

        public ATM createNewATM(
                String login,
                String password
        ) {
            if (System.this.atms.get(login) != null) {
                throw new IllegalArgumentException("login");
            }

            ATM atm = new ATM(
                    new ObjectId(),
                    login,
                    password
            );
            System.this.atms.put(login, atm);
            return atm;
        }



        @Override
        public Message handleMessageAndRespond(Message message) {
            return super.handleMessageAndRespond(message);
        }
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(2999)) {
            println("Listening on port " + serverSocket.getLocalPort() + "...");

            System system = System.getInstance(new BigDecimal(1_000_000_000));
            system.start();

            while (true) {
                system.connectionsHandlers.submit(new Connection(serverSocket.accept()));
            }

        } catch (IOException e) {
            println("Server exception: " + e.getMessage());
        }
    }
}
