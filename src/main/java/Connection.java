import communication.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Connection implements Runnable {

    private final AtomicBoolean killSwitch = new AtomicBoolean(false);
    private Signable signable;
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public boolean isRunning() {
        return killSwitch.get();
    }

    private void tryLogin() throws IOException, ClassNotFoundException {
        while (!killSwitch.get() && signable == null) {
            Message message = (Message) in.readObject();
            String returnMessage;
            boolean status;
            if (message.getType() == Message.Type.LOGIN) {
                String login = (String) message.getContent()[0];
                String password = (String) message.getContent()[1];
                signable = System.getInstance().signIn(login, password);
                if (signable == null) {
                    returnMessage = "Wystąpił błąd podczas logowania.";
                    status = false;
                } else {
                    returnMessage = "Zalogowano pomyślnie.";
                    status = true;
                }
            } else {
                returnMessage = "Błąd komunikacji.";
                status = false;
            }
            out.writeObject(new Message(message.getType(), List.of(status, returnMessage)));
            java.lang.System.out.println("Login handled.");
        }
    }

    public void stop() {
        this.killSwitch.set(true);
        if (signable != null) {
            signable.logOut();
        }
        try { this.socket.close(); } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void run() {
        try { tryLogin(); } catch (IOException | ClassNotFoundException e) { e.printStackTrace(); stop(); }
        while (!killSwitch.get()) {
            try {
                out.writeObject(signable.handleMessageAndRespond((Message) in.readObject()));
                java.lang.System.out.println("Request handled.");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                stop();
            }
        }
    }
}
