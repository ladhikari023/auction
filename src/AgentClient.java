/**
    Project 5 : Distributed Auction
    CS 351L 004

    Brian Kimbrell
    Laxman Adhikari
    Shaswat Shukla
 */


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class AgentClient {

    private static final BufferedReader stdIn = new BufferedReader(
            new InputStreamReader(System.in));
    private static final HashMap<String, Socket> agentSocketList = new HashMap<>();
    private static JSONObject jsonObj = new JSONObject();
    private static AgentHouseListener agentHouseConnection = null;
    private static AgentBankListener agentBankConnection = null;
    private static PrintWriter outToAuctionHouse = null;
    private static PrintWriter outToBank = null;
    private static int agentAccountID = -99;
    private static BufferedReader in;
    private static int currentBidding = 0;
    private static final HashMap<Integer, Double> buyRecord = new HashMap<>();

    /*
     * Main function for AgentClient class
     * Deals with getting user inputs and sending appropriate request to bank and auction house
     */
    public static void main(String[] args) throws IOException,
            InterruptedException, JSONException {

        // stores host and port information of bank
        String bank_hostName = args[0];
        int bank_portNumber = Integer.parseInt(args[1]);

        // jsonObject is created with initial information
        // Sent to bank eventually
        jsonObj.put(Keys.SENDER.name(),Commands.AGENT.name());
        jsonObj.put(Keys.REQUESTTYPE.name(),Commands.OPENACCOUNT.name());


        // Asks for agent's name and amount to initialize for Agent
        System.out.println("Give a name to Agent");
        String name = stdIn.readLine();

        jsonObj.put(Keys.NAME.name(),name);
        System.out.println("Hi user, I am agent "+name);
        System.out.println("Initialize amount of agent "+name);

        Scanner sc = new Scanner(System.in);
        double amount;
        while (true) {
            try {
                amount = Double.parseDouble(sc.next());
                break;
            } catch (NumberFormatException ignore) {
                System.out.println("Invalid input, Amount should be a double value !!");
            }
        }
        jsonObj.put(Keys.AMOUNT.name(),amount);

        try {
            Socket socket = new Socket(bank_hostName, bank_portNumber);
            // sends request to bank
            outToBank = new PrintWriter(socket.getOutputStream(), true);
            // reads input from bank
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            outToBank.println(jsonObj.toString());


            String input = in.readLine();
            String[] inputLine = input.split(",");
            System.out.println(inputLine[0]);

            System.out.println("Available Auction Houses:");
            String[] line = inputLine[0].split(" ");

            // Assign accountID of agent given from Bank
            agentAccountID = Integer.parseInt(line[7]);

            if (inputLine.length < 2) {
                System.out.println("No Auction houses to show");
            }
            for (int i = 1; i < inputLine.length; i++) {
                    System.out.println("House : " + i);
            }

            // New thread to keep listening to Bank's input
            agentBankConnection = new AgentBankListener(socket);
            Thread t = new Thread(agentBankConnection);
            t.start();

        } catch (IOException e) {
            System.err.println("Couldn't connect Bank");
        }

        // Deals with User Input
        do {
            String userInput;
            do {
                TimeUnit.MILLISECONDS.sleep(500);
                showOptions();

                userInput = stdIn.readLine();

                if (!(userInput.equals("A") || userInput.equals("B") || userInput.equals("Q"))) {
                    System.out.println( userInput + " is not a valid command.");
                } else {
                    break;
                }
            } while (true);

            switch (Objects.requireNonNull(userInput)) {
                case "A": {
                    contactAuctionHouse();
                    break;
                }
                case "B": {
                    // Sends check balance request to bank
                    jsonObj = new JSONObject();
                    jsonObj.put(Keys.SENDER.name(),Commands.AGENT.name());
                    jsonObj.put(Keys.REQUESTTYPE.name(),Commands.CHECKBALANCE.name());
                    jsonObj.put(Keys.ACCOUNTID.name(), agentAccountID);
                    outToBank.println(jsonObj.toString());

                    // Asks to pay for previously held balance
                    if (agentHouseConnection != null && !buyRecord.isEmpty()) {
                        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        System.out.println("Pay For Previously Won Auction Items? (Y/N)");
                        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        String userRequest = stdIn.readLine();
                        if (userRequest.equals("Y")){
                            for (Map.Entry record : buyRecord.entrySet()) {
                                jsonObj = new JSONObject();
                                jsonObj.put(Keys.SENDER.name(),Commands.AGENT.name());
                                jsonObj.put(Keys.REQUESTTYPE.name(),Commands.TRANSFERAMOUNT.name());
                                jsonObj.put(Keys.ACCOUNTID.name(),record.getKey());
                                jsonObj.put(Keys.AMOUNT.name(),record.getValue());
                                outToBank.println((jsonObj.toString()));
                            }
                        }
                    }
                    break;
                }
                //quit
                case "Q": {
                    if (currentBidding != 0 || (agentHouseConnection!=null && agentHouseConnection.getBidStatus())) {
                        System.out.println("Cannot exit while bid is active!");
                        break;
                    }
                    else {
                        System.exit(1);
                        break;
                    }
                }
                default: {
                    System.exit(1);
                }
            }
        } while (true);
    }

    // show options to interact with bank
    private static void showOptions() {
        System.out.println("\nAgents Option:");
        System.out.println("A: Connect Auction house");
        System.out.println("B: Show Bank Balance");
        System.out.println("Q: Quit\n");
    }

    // Sends Auction Items request to bank
    // In return Bank provides with all the Auctions Houses list
    public static void contactAuctionHouse() throws IOException,
            JSONException, InterruptedException {
        jsonObj = new JSONObject();
        jsonObj.put(Keys.SENDER.name(),Commands.AGENT.name());
        jsonObj.put(Keys.REQUESTTYPE.name(),Commands.HOUSELISTS.name());
        outToBank.println(jsonObj.toString());
        TimeUnit.MILLISECONDS.sleep(500);


        if (agentSocketList.isEmpty()) {
            System.out.println("No Auction Houses Available");
        }
        else {
            do {
                //show options to interact with auction houses
                auctionOptions();
                String userInput = stdIn.readLine();
                switch (userInput) {
                    case "R": {
                        showAvailableAuctionItems(agentBankConnection.getBankMessage());
                        return;
                    }
                    case "B": {
                        bidOnItem(agentBankConnection.getBankMessage());
                        return;
                    }
                    case "X": {
                        return;
                    }
                    default: {
                        System.out.println(userInput+" is not a valid input!");
                        break;
                    }
                }
            } while (true);
        }
    }

    //show options to interact with auction houses
    private static void auctionOptions() {
        System.out.println("\nAuction options:");
        System.out.println("R: Show the items on Auction");
        System.out.println("B: Bid on Auction Items");
        System.out.println("X: Main Menu\n");
    }

    /*
     * Handles Bid Functionality
     * Ask house number and item number
     * Ask amount to bid
     * Sends Bid Request to Auction House
     */
    public static void bidOnItem(String[] auctionAddressArray)
            throws IOException, JSONException, InterruptedException {
        do {
            System.out.println("House # ? (X to cancel)");
            String str = stdIn.readLine();
            if (str.matches("\\d+")) {
                int houseNumber = Integer.parseInt(str);
                if (houseNumber > 0 && houseNumber <= agentSocketList.size()) {
                    listenAuctionHouse(str, auctionAddressArray);

                    do {
                        System.out.println("Item # ? " +
                                "(Select Item by Id , X to cancel)");
                        String itemID = stdIn.readLine();

                        if (itemID.matches("\\d+")) {
                            do {
                                System.out.println("Enter Bid Amount " +
                                        "(X : Cancel)");
                                String bidAmount = stdIn.readLine();

                                if (bidAmount.matches("\\d+") &&
                                        houseNumber <= agentSocketList.size()) {
                                    jsonObj = new JSONObject();
                                    jsonObj.put(Keys.SENDER.name(),Commands.AGENT.name());
                                    jsonObj.put(Keys.REQUESTTYPE.name(),Commands.BID.name());
                                    jsonObj.put(Keys.ITEMID.name(),
                                            Double.parseDouble(itemID));
                                    jsonObj.put(Keys.AMOUNT.name(),
                                            Double.parseDouble(bidAmount));
                                    jsonObj.put(Keys.ACCOUNTID.name(), agentAccountID);
                                    outToAuctionHouse.println(jsonObj.toString());
                                    return;
                                }
                                else if (bidAmount.equals("X")) {
                                    break;
                                }
                                else {
                                    System.out.println("Input is not valid");
                                }
                            } while (true);
                        }
                        else if (itemID.equals("X")) {
                            break;
                        }
                        else {
                            System.out.println(itemID + " is not a valid item id");
                        }
                    } while (true);
                }
            }
            else if (str.equals("X")) {
                return;
            }
            else {
                System.out.println(str+" is not valid House Number");
            }
        }while (true);
    }

    /*
     * Asks for House number of which you want to request Items of
     * Sends request to that auction house
     */
    public static void showAvailableAuctionItems(String[] auctionAddressArray)
            throws IOException, JSONException {

        do {
            System.out.println("Enter house number from which you want to see Auction Listings");
            System.out.print("House #");
            //get house number from user
            String userInput = stdIn.readLine();

            if (!userInput.matches("\\d+")) {
                System.out.println("Not a valid house ID");
            }
            else if (Integer.parseInt(userInput) > 0 &&
                    Integer.parseInt(userInput) <= agentSocketList.size()) {

                //listen to house
                listenAuctionHouse(userInput, auctionAddressArray);

                //send request
                jsonObj = new JSONObject();
                jsonObj.put(Keys.SENDER.name(), Commands.AGENT.name());
                jsonObj.put(Keys.REQUESTTYPE.name(),Commands.REQUESTITEMS.name());
                outToAuctionHouse.println(jsonObj.toString());

                return;
            }
            else {
                System.out.println("House don't exist");
            }
        }while (true);
    }

    // Start new thread to listen input from auction house with given id entered by user as parameter
    public static void listenAuctionHouse(String id, String[] auctionAddressArray)
            throws IOException {

        int houseID = Integer.parseInt(id);

        if (agentSocketList.get(auctionAddressArray[houseID]) == null) {
            String[] inputLine = auctionAddressArray[houseID].split(" ");
            try {
                Socket socket = new Socket(inputLine[0],
                        Integer.parseInt(inputLine[1]));
                agentSocketList.put(auctionAddressArray[houseID], socket);

                currentBidding = 0;
                agentHouseConnection = new AgentHouseListener(socket,currentBidding,auctionAddressArray[houseID]);
                Thread t = new Thread(agentHouseConnection);
                t.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (int i = 1; i < auctionAddressArray.length; i++) {
            if (houseID == i) {
                outToAuctionHouse = new PrintWriter(agentSocketList.get(auctionAddressArray[i])
                        .getOutputStream(), true);
            }
        }
    }

    /*
        An inbuilt class to help connect to Bank with each Agent so that all agents get real time
        update from Bank
     */
     static class AgentBankListener implements Runnable{
        private final BufferedReader in;
        private String[] bankMessage;

        public AgentBankListener(Socket socket)
                throws IOException {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        // returns bank message
        public String[] getBankMessage() {
            return bankMessage;
        }

        // reads bank message and checks for the event of message
        @Override
        public void run() {
            try {
                do {
                    String input = in.readLine();

                    if (!(input == null)) {
                        bankMessage = input.split(",");
                        System.out.println();
                        if (bankMessage[0].matches("Auction house list")) {
                            if (!bankMessage[1].matches("Auction houses not found")) {
                                System.out.println("Available Auction House:");
                                for (int i = 1; i < bankMessage.length; i++) {
                                    if (!agentSocketList.containsKey(bankMessage[i])) {
                                        agentSocketList.put(bankMessage[i], null);
                                    }
                                    System.out.println("House : " + i);
                                }
                            }
                        }
                        else if (bankMessage[0].matches(Keys.SUCCESS.name())) {
                            System.out.println("Thank you agent!");
                            buyRecord.remove(Integer.parseInt(bankMessage[1]));
                        }
                        else {
                            for (String str : bankMessage) {
                                System.out.println(str);
                            }
                        }
                    }
                } while (true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /*
        An inbuilt class to help connect to Auction House with each Agent so that all agents
        get real time update from Auction House
     */
     static class AgentHouseListener implements Runnable{
        private final BufferedReader in;
        private int bidStatus;
        private final String auctionAddress;

        public AgentHouseListener(Socket socket,int currentBidding, String auctionAddress)
                throws IOException {
            this.auctionAddress = auctionAddress;
            this.bidStatus = currentBidding;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        // process input from Auction House
        @Override
        public void run() {
            try {
                do {
                    String input = in.readLine();
                    System.out.println();
                    if (input == null) {
                        agentSocketList.remove(auctionAddress);
                        System.out.println("Auction house " + (agentSocketList.size() + 1) +
                                " is disconnected");
                        break;
                    }
                    else {
                        String[] auctionMessage = input.split(",");
                        if (auctionMessage[0].matches(Commands.BIDACCEPTED.name())) {
                            for (String str : auctionMessage) {
                                System.out.println(str);
                            }
                            bidStatus++;
                        }
                        else if (auctionMessage[0].matches(Commands.OUTBID.name())) {
                            for (String str : auctionMessage) {
                                System.out.println(str);
                            }
                            bidStatus--;
                        }
                        else if (auctionMessage[0].matches(Commands.WONBID.name())) {
                            System.out.println("Congrats Agent!!");
                            System.out.print("Won Item: ");
                            System.out.println(auctionMessage[1]);

                            int auctionId = Integer.parseInt(auctionMessage[2]);
                            double bidAmount = Double.parseDouble((auctionMessage[3]));

                            if (buyRecord.containsKey(auctionId)) {
                                double prevAmount = buyRecord.get(auctionId);
                                buyRecord.replace(auctionId, prevAmount + bidAmount);
                            }
                            else {
                                buyRecord.put(auctionId, bidAmount);
                            }

                            bidStatus--;
                        }

                        else {
                            for (String s : auctionMessage) {
                                System.out.println(s);
                            }
                        }
                    }
                } while (true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Returns true if Agent is bidding
        public boolean getBidStatus() {
            return (bidStatus > 0);
        }
    }

}
