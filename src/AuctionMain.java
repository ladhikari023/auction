/**
    Project 5 : Distributed Auction
    CS 351L 004

    Brian Kimbrell
    Laxman Adhikari
    Shaswat Shukla
 */

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/*
 * Creates new thread to connect to bank, start server for bidding and a listener to user input
 */
public class AuctionMain {

    public static void main(String[] args) throws JSONException, InterruptedException {
        int auctionPort = Integer.parseInt(args[0]);
        String bankIP = args[1];
        int bankPort = Integer.parseInt(args[2]);


        // Initialize auctionItems for each Auction House
        AuctionItems auctionItems = new AuctionItems();
        auctionItems.createItems();
        auctionItems.createJSONObjects();

        // Client to Bank
        AuctionHouseClient auctionClient = new AuctionHouseClient(bankIP, bankPort, auctionPort);
        Thread t1 = new Thread(auctionClient);
        t1.start();

        // Bid Server
        AuctionBidServer bidServer = new AuctionBidServer(auctionPort,auctionClient,auctionItems);
        Thread t2 = new Thread(bidServer);
        t2.start();

        // Listener for terminating request from user
        System.out.println(" Press Q to close Auction House. Can't Close if BID is ACTIVE !!\n");
        ExitListener exitListener = new ExitListener(auctionItems);
        Thread t3 = new Thread(exitListener);
        t3.start();
    }

    // An inbuilt class to keep listening to user input in auction house
     static class ExitListener implements Runnable {
        private final AuctionItems collection;


        public ExitListener(AuctionItems collection) throws JSONException {
            this.collection = collection;
        }

        @Override
        public void run() {
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                try {
                    String userInput = stdIn.readLine();
                    if (userInput.equalsIgnoreCase("Q")) {
                        if (!(collection.isBidActive())) {
                            System.exit(1);
                        }
                        else {
                            System.out.println("Can't Exit while Bid is Active");
                        }
                    }
                    else {
                        System.out.println("(Enter Q to Close)");
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
