/**
    Project 5 : Distributed Auction
    CS 351L 004

    Brian Kimbrell
    Laxman Adhikari
    Shaswat Shukla
 */



import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/*
 * Bank Server accepts client and start new thread for each client
 * BankServer is continuously listening to Client's Request
 * Port Number for Bank Server is provided in the first argument args[0]
 */
public class BankServer {
    static int accountID = 0;
    static final Map<Integer,BankAccount> bankAccounts = new HashMap<>();
    static ClientsInfo clientsInfo = new ClientsInfo();
    static ServerSocket serverSocket = null;

    public static void main(String[] args) {
        int bankPort = Integer.parseInt(args[0]);
        System.out.println("Listening on port: " + bankPort);

        try {
            serverSocket = new ServerSocket(bankPort);
        }
        catch (IOException e) {
            System.out.println("Error Listening: Port " + bankPort);
            System.exit(1);
        }

        // continuously running to accept clients
        while (true) {
            try {
                Socket client = serverSocket.accept();
                System.out.println("New Client Connection !!");

                BankHandler bankHandler = new BankHandler(client, accountID++,bankAccounts,clientsInfo);
                Thread t = new Thread(bankHandler);
                t.start();
            }
            catch (IOException e) {
                System.out.println("Couldn't Accept Client "+e);
            }
        }
    }
}
