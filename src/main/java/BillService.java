import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BillService {

    private final Map<String, Pairing<Bill, Client>> bills;
    private final Bill bankBill;
    public static final double INTEREST_RATE = 0.01;

    public BillService(Bill bankBill) {
        this.bills = new HashMap<>();
        this.bankBill = bankBill;
    }

    public Bill getBankBill() {
        return this.bankBill;
    }

    public boolean add(Client client, Bill bill) {
        client.addBill(bill);
        return this.bills.put(bill.getIban(), new Pairing<>(bill, client)) == null;
    }

    public Pairing<Bill, Client> get(String iban) {
        return this.bills.get(iban);
    }

    public Collection<Pairing<Bill, Client>> values() {
        return this.bills.values();
    }
}
