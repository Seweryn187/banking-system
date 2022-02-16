import communication.Message;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class Client extends User {

    public static final Pattern loginPattern = Pattern.compile("[a-zA-Z0-9]{10}");

    private final ClientServices clientServices;

    private final ArrayList<Bill> billList;

    public static ClientCreationApplication sendClientCreationApplication(
            String login,
            String password,
            String firstName,
            String lastName,
            Date birthDate,
            String phoneNumber,
            String email,
            Currency billCurrency
    ) {
        ClientCreationApplication clientCreationApplication = System.getInstance().createClientCreationApplication(
                login, password, firstName, lastName, birthDate, phoneNumber, email, billCurrency
        );
        System.getInstance().addApplication(null, clientCreationApplication);
        return clientCreationApplication;
    }

    protected Client(
            ObjectId identifier,
            String login,
            String password,
            String firstName,
            String lastName,
            Date birthDate,
            String phoneNumber,
            String email,
            ClientServices clientServices
    ) {
        super(identifier, login, password, firstName, lastName, birthDate, phoneNumber, email);
        this.billList = new ArrayList<>();
        this.clientServices = clientServices;
    }

    public boolean addBill(Bill newBill) {
        return this.billList.add(newBill);
    }

    public Bill getBill(int index) {
        return this.billList.get(index);
    }

    public Bill getPrimaryBill() {
        return this.getBill(0);
    }

    public int getBillsAmount() { return this.billList.size(); }

    public boolean scheduleTransfer(String title, Bill sender, String recipientIban, BigDecimal amount, Date date) {
        System.TransferOperation transferOperation = this.clientServices.createTransferOperation(
                title,
                sender.getIban(),
                recipientIban,
                amount,
                date
        );
        if (this.clientServices.addRequest(this, transferOperation)) {
            sender.addScheduledOperation(transferOperation);
            return true;
        }
        return false;
    }

    public boolean scheduleInternalTransfer(String title, Bill sender, Bill recipient, BigDecimal amount, Date date) {
        System.TransferOperation transferOperation = this.clientServices.createTransferOperation(
                title,
                sender.getIban(),
                recipient.getIban(),
                amount,
                date
        );
        if (this.clientServices.addRequest(this, transferOperation)) {
            sender.addScheduledOperation(transferOperation);
            return true;
        }
        return false;
    }

    public boolean performTransfer(String title, Bill sender, String recipientIban, BigDecimal amount) {
        System.TransferOperation transferOperation = this.clientServices.createTransferOperation(
                title,
                sender.getIban(),
                recipientIban,
                amount
        );
        if (this.clientServices.addRequest(this, transferOperation)) {
            sender.addScheduledOperation(transferOperation);
            return true;
        }
        return false;
    }

    public boolean performInternalTransfer(String title, Bill sender, Bill recipient, BigDecimal amount) {
        System.TransferOperation transferOperation = this.clientServices.createTransferOperation(
                title,
                sender.getIban(),
                recipient.getIban(),
                amount
        );
        if (this.clientServices.addRequest(this, transferOperation)) {
            sender.addScheduledOperation(transferOperation);
            return true;
        }
        return false;
    }

    public boolean performInvestmentCreation(Bill investor, BigDecimal amount, System.InvestmentPeriod investmentPeriod) {
        System.StartInvestmentOperation startInvestmentOperation = this.clientServices.createInvestmentOperation(
                investor.getIban(),
                amount,
                investmentPeriod
        );
        if (this.clientServices.addRequest(this, startInvestmentOperation)) {
            investor.addScheduledOperation(startInvestmentOperation);
            return true;
        }
        return false;
    }

    public boolean createNewBill(Currency currency) {
        return this.clientServices.addRequest(this, this.clientServices.createBillCreationOperation(this, currency));
    }

    public boolean orderNewCard(Bill bill) {
        return this.clientServices.addRequest(this, this.clientServices.createOrderNewCardOperation(bill.getIban()));
    }

    private Message listBills(Message message) {
        List<Object> content = new LinkedList<>();
        int size = this.getBillsAmount();
        for (int i = 0; i < size; ++i) {
            content.add(
                    new Object[] {
                            i,
                            getBill(i).getIban(),
                            getBill(i).getCurrentBalance().toString(),
                            getBill(i).getCardsInfo()
                    }
            );
        }
        return new Message(message.getType(), content);
    }

    private Message getBillScheduledOperations(Message message) {
        int index = (int) message.getContent()[0];
        List<Object> content = new LinkedList<>();
        getBill(index).getScheduledOperations().forEach((operation -> {
            content.add(operation.toString());
        }));
        return new Message(message.getType(), content);
    }

    private Message getBillHistory(Message message) {
        int index = (int) message.getContent()[0];
        List<Object> content = new LinkedList<>();
        getBill(index).getHistory().forEach((operation -> content.add(operation.toString())));
        return new Message(message.getType(), content);
    }

    private Message changePin(Message message) {
        int billIndex = (int) message.getContent()[0];
        String cardNumber = (String) message.getContent()[1];
        byte[] newPin = (byte[]) message.getContent()[2];
        getBill(billIndex).getCard(cardNumber).changePin(newPin);
        return new Message(message.getType(), List.of("Zmieniono kod PIN."));
    }

    private Message internalTransfer(Message message) {
        Date executionDate = (Date) message.getContent()[0];
        String title = (String) message.getContent()[1];
        int senderIndex = (int) message.getContent()[2];
        int recipientIndex = (int) message.getContent()[3];
        BigDecimal amount = (BigDecimal) message.getContent()[4];
        boolean status;
        if (executionDate.getTime() <= new Date().getTime()) {
            try {
                status = performInternalTransfer(title, getBill(senderIndex), getBill(recipientIndex), amount);
            } catch (Exception e) {
                status = false;
            }
        } else {
            try {
                status = scheduleInternalTransfer(title, getBill(senderIndex), getBill(recipientIndex), amount, executionDate);
            } catch (Exception e) {
                status = false;
            }
        }
        if (status) {
            return new Message(message.getType(), List.of("Operacja została przyjęta do wykonania."));
        }
        return new Message(message.getType(), List.of("Wystąpił błąd podczas przetwarzania operacji."));
    }

    private Message transfer(Message message) {
        Date executionDate = (Date) message.getContent()[0];
        String title = (String) message.getContent()[1];
        int senderIndex = (int) message.getContent()[2];
        String recipientIban = (String) message.getContent()[3];
        BigDecimal amount = (BigDecimal) message.getContent()[4];
        boolean status;
        if (executionDate.getTime() <= new Date().getTime()) {
            try {
                status = performTransfer(title, getBill(senderIndex), recipientIban, amount);
            } catch (Exception e) {
                status = false;
            }
        } else {
            try {
                status = scheduleTransfer(title, getBill(senderIndex), recipientIban, amount, executionDate);
            } catch (Exception e) {
                status = false;
            }
        }
        if (status) {
            return new Message(message.getType(), List.of("Operacja została przyjęta do wykonania."));
        }
        return new Message(message.getType(), List.of("Wystąpił błąd podczas przetwarzania operacji."));
    }

    private Message investment(Message message) {
        int investorIndex = (int) message.getContent()[0];
        BigDecimal amount = (BigDecimal) message.getContent()[1];
        System.InvestmentPeriod period = System.InvestmentPeriod.get((String) message.getContent()[2]);
        boolean status;
        try {
            status = performInvestmentCreation(getBill(investorIndex), amount, period);
        } catch(Exception e) {
            status = false;
        }
        if (status) {
            return new Message(message.getType(), List.of("Lokata została przyjęta do założenia."));
        }
        return new Message(message.getType(), List.of("Wystąpił błąd podczas przetwarzania operacji."));
    }

    private Message createBill(Message message) {
        Currency currency = Currency.getCurrency((String) message.getContent()[0]);
        boolean status;
        try {
            status = createNewBill(currency);
        } catch (Exception e) {
            status = false;
        }
        if (status) {
            return new Message(message.getType(), List.of("Utworzenie rachunku zostało przyjęte do wykonania."));
        }
        return new Message(message.getType(), List.of("Wystąpił błąd podczas przetwarzania operacji."));
    }

    private Message orderCard(Message message) {
        int billIndex = (int) message.getContent()[0];
        boolean status;
        try {
            status = orderNewCard(getBill(billIndex));
        } catch (Exception e) {
            status = false;
        }
        if (status) {
            return new Message(message.getType(), List.of("Zamówienie karty zostało złożone."));
        }
        return new Message(message.getType(), List.of("Wystąpił błąd podczas przetwarzania operacji."));
    }

    @Override
    public Message handleMessageAndRespond(Message message) {
        return new MessageHandler[] {
                this::listBills,
                this::getBillScheduledOperations,
                this::getBillHistory,
                this::changePin,
                this::internalTransfer,
                this::transfer,
                this::investment,
                this::createBill,
                this::orderCard
        } [message.getType().getValue()].invoke(message);
    }
}
