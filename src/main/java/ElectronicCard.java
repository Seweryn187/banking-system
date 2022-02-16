import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ElectronicCard {

    public static final long VALIDITY_TIME = TimeUnit.MILLISECONDS.convert(1825, TimeUnit.DAYS);

    private final String number;
    private final String billIban;
    private final byte[] pin;
    private final byte[] cvvCode;
    private final Date validityEndDate;
    private boolean active;

    public ElectronicCard(
            String number,
            String billIban,
            byte[] pin,
            byte[] cvvCode,
            Date validityEndDate,
            boolean active
    ) {
        this.pin = pin;
        this.cvvCode = cvvCode;
        this.number = number;
        this.billIban = billIban;
        this.validityEndDate = validityEndDate;
        this.active = active;
    }

    public ElectronicCard(String number, String billIban, byte[] pin, byte[] cvvCode, Date validityEndDate) {
        this(number, billIban, pin, cvvCode, validityEndDate, true);
    }

    public String getNumber() {
        return number;
    }

    public String getBillIban() {
        return billIban;
    }

    public byte[] getPin() {
        return pin;
    }

    public void changePin(byte[] pin) {
        java.lang.System.arraycopy(pin, 0, this.pin, 0, this.pin.length);
    }

    public byte[] getCvvCode() {
        return cvvCode;
    }

    public Date getValidityEndDate() {
        return validityEndDate;
    }

    public boolean isActive() {
        return active;
    }

    public boolean validate() {
        return new Date().getTime() <= getValidityEndDate().getTime() && active;
    }

    public boolean authorize(byte[] typedPin) {
        if (typedPin.length != pin.length) {
            return false;
        }
        for (int i = 0; i < typedPin.length; ++i) {
            if (typedPin[i] != pin[i]) {
                return false;
            }
        }
        return true;
    }

    public void lock() throws ElectronicCardLockException {
        if (!this.active) {
            throw new ElectronicCardLockException("Cannot lock card which is already locked.");
        }
        this.active = false;
    }

    public void unlock() throws ElectronicCardLockException {
        if (this.active) {
            throw new ElectronicCardLockException("Cannot unlock card which is already unlocked.");
        }
        this.active = true;
    }

    static class ElectronicCardLockException extends Exception {
        public ElectronicCardLockException(String message) {
            super(message);
        }

        public ElectronicCardLockException() {
            super();
        }
    }
}
