import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/*
 * Client to Bank
 * Takes hostName, hostPort and housePort as constructor parameters
 */
public class AuctionHouseClient implements Runnable{
    private Socket bankSocket;
    private int accID;
    private final int housePort;
    private PrintWriter out;
    private BufferedReader in;

    public AuctionHouseClient(String hostName, int hostPort, int housePort) {
        this.housePort = housePort;
        try {
            bankSocket = new Socket(hostName, hostPort);

            out = new PrintWriter(bankSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(bankSocket.getInputStream()));

            System.out.println("Connected to Bank");

        }  catch (IOException e) {
            System.err.println("Couldn't connect Bank " + e);
            System.exit(1);
        }

    }

    // sends start auction house request to bank
    @Override
    public void run() {
        try {
            JSONObject json = new JSONObject();
            json.put(Keys.SENDER.name(),Commands.AUCTION.name());
            json.put(Keys.REQUESTTYPE.name(),Commands.OPENAUCTION.name());
            json.put(Keys.ADDRESSINFO.name(),InetAddress.getLocalHost().getHostName() + " " + housePort);
            out.println(json);

            String str = in.readLine();
            System.out.println(str);

            String[] bankResponse = str.split(" ");

            // Assign account ID given by bank to the current auction house
            accID = Integer.parseInt(bankResponse[4]);

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    // returns bank socket
    public Socket getBankSocket() {
        return bankSocket;
    }

    // returns account id of house
    public int getAccID() {
        return accID;
    }
}
