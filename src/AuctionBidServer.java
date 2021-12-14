import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/*
 * Server for Bidding, keep listening to Agent
 */
public class AuctionBidServer implements Runnable{
    private final ArrayList<Socket> agentsSocketList;
    private int bidderID;
    private final AuctionHouseClient auctionHouseClient;
    private final ServerSocket serverSocket;
    private final AuctionItems auctionItems;

    public AuctionBidServer(int portNumber, AuctionHouseClient auctionHouseClient,AuctionItems auctionItems) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(portNumber);
            System.out.println("Listening on : " + InetAddress.getLocalHost().getHostName() + ":" + portNumber);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        this.agentsSocketList = new ArrayList<>();
        this.bidderID = 0;
        this.auctionHouseClient = auctionHouseClient;
        this.serverSocket = serverSocket;
        this.auctionItems = auctionItems;
    }

    // Accepts agent and start new thread to handle their communication
    @Override
    public void run() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Agent got Connected");

                AuctionHandler auctionHandler =
                        new AuctionHandler(clientSocket, bidderID++, agentsSocketList, auctionHouseClient,auctionItems);
                agentsSocketList.add(clientSocket);
                Thread t = new Thread(auctionHandler);
                t.start();
            } catch (IOException e) {
                System.out.println("Couldn't connect Agent"+e);
            }
        }
    }
}
