import java.math.BigDecimal;
import java.util.Date;

public interface ClientServices extends RequestQueueAccess<Client> {
    System.TransferOperation createTransferOperation(
            String title,
            String senderIban,
            String recipientIban,
            BigDecimal amount
    );
    System.TransferOperation createTransferOperation(
            String title,
            String senderIban,
            String recipientIban,
            BigDecimal amount,
            Date executionDate
    );
    System.StartInvestmentOperation createInvestmentOperation(
            String investorIban,
            BigDecimal amount,
            System.InvestmentPeriod investmentPeriod
    );
    System.BillCreationOperation createBillCreationOperation(
            Client client,
            Currency currency
    );
    System.OrderNewCardOperation createOrderNewCardOperation(
            String iban
    );
}
