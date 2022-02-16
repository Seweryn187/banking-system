package client;

import communication.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class ClientDesktopApp implements Runnable {

    private final Scanner scanner;
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    private boolean status = false;

    public ClientDesktopApp(Socket socket) throws IOException {
        this.scanner = new Scanner(System.in);
        this.socket = socket;
        this.out = new ObjectOutputStream(this.socket.getOutputStream());
        this.in = new ObjectInputStream(this.socket.getInputStream());
    }

    private void signIn() throws IOException, ClassNotFoundException {
        while (!status) {
            System.out.println("Podaj login i hasło:");
            out.writeObject(new Message(
                    Message.Type.LOGIN, List.of(scanner.nextLine(), scanner.nextLine())));
            Message response = (Message) in.readObject();
            status = (boolean) response.getContent()[0];
            System.out.println(response.getContent()[1] + "\n");
        }
        System.out.println("Twoje rachunki:");
        listBills();
    }

    private void listBills() throws IOException, ClassNotFoundException {
        out.writeObject(new Message(Message.Type.LIST_BILLS));
        Message response = (Message) in.readObject();
        for (Object o : response.getContent()) {
            Object[] oArr = (Object[]) o;
            int index = (int) oArr[0];
            String iban = (String) oArr[1];
            String amount = (String) oArr[2];
            String[][] cards = (String[][]) oArr[3];
            System.out.println(index + " - IBAN: " + iban);
            System.out.println("Current balance: " + amount);
            for (int i = 0; i < cards.length; ++i) {
                System.out.println(new StringBuilder("Card ").append(i).append(" {")
                        .append("\n\tNumber: ").append(cards[i][0])
                        .append("\n\tValid to: ").append(cards[i][1])
                        .append("\n\tActive: ").append(cards[i][2])
                        .append("\n}")
                        .toString());
            }
            System.out.println();
        }
    }

    private void transfer() throws IOException, ClassNotFoundException {
        String[] strings = new String[4];
        System.out.println("Podaj indeks Twojego rachunku:");
        strings[0] = scanner.nextLine();
        System.out.println("Podaj iban rachunku docelowego:");
        strings[1] = scanner.nextLine();
        System.out.println("Podaj kwotę przelewu:");
        strings[2] = scanner.nextLine();
        System.out.println("Podaj tytuł przelewu:");
        strings[3] = scanner.nextLine();
        out.writeObject(new Message(Message.Type.OPERATION_TRANSFER, List.of(
                new Date(new Date().getTime() + 15000),
                strings[3],
                Integer.parseInt(strings[0]),
                strings[1],
                new BigDecimal(strings[2])
        )));
        Message response = (Message) in.readObject();
        System.out.println(response.getContent()[0]);
    }

    private void mainLoop() throws IOException, ClassNotFoundException {
        System.out.println("Wybierz opcję:");
        System.out.println("1: Wyświetl rachunki");
        System.out.println("2: Wykonaj przelew");
        int choice = Integer.parseInt(scanner.nextLine());
        switch (choice) {
            case 1: listBills(); break;
            case 2: transfer(); break;
            default: System.out.println("Zły numer."); break;
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (status) {
                    mainLoop();
                } else {
                    signIn();
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ClientDesktopApp clientDesktopApp = new ClientDesktopApp(new Socket("127.0.0.1", 2999));
        clientDesktopApp.run();
    }
}
