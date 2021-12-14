import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.TimerTask;

/*
 * Handles all the bidding action
 */
public class AuctionHandler implements Runnable{
    private final ArrayList<Socket> agentSocketsList;
    private final int bidderID;
    private final Socket agentClientSocket;
    private final AuctionItems auctionItems;
    private final AuctionHouseClient auctionHouseClient;
    private final PrintWriter out;
    private final BufferedReader in;

    // Constructor
    public AuctionHandler(Socket agentClientSocket, int bidderID,
                          ArrayList<Socket> agentSocketsList, AuctionHouseClient auctionHouseClient, AuctionItems auctionItems) throws IOException {
        this.agentClientSocket = agentClientSocket;
        out = new PrintWriter(agentClientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(agentClientSocket.getInputStream()));
        this.auctionItems = auctionItems;
        this.agentSocketsList = agentSocketsList;
        this.bidderID = bidderID;
        this.auctionHouseClient = auctionHouseClient;
    }

    /*
     * Deals with Agent's Request
     */
    @Override
    public void run() {
        String inputLine = null;
        String outputLine;

        do {
            try {
                inputLine = in.readLine();

                if (inputLine!=null) {
                    JSONObject json = new JSONObject(inputLine);
                    outputLine = processJSON(json);

                    out.println(outputLine);
                }
                else {
                    out.println("Agent Left ");
                }
            } catch (IOException e) {
                inputLine = null;
                System.out.println("Agent Left");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } while (inputLine != null);
    }

    /*
     * check the request type and sends relevant output back to Agent
     * Also sends output to Bank to check the agent's balance
     */
    private String processJSON(JSONObject json) throws JSONException {
        String command;
        String output;

        command = json.getString(Keys.REQUESTTYPE.name());

        if (command.equalsIgnoreCase(Commands.BID.name())) {

            System.out.print("Bidding Info: ");

            int itmObjID = json.getInt(Keys.ITEMID.name());
            JSONObject itemObject = auctionItems.getJSONItem(itmObjID);

            /*
             * Checks if itemObject is valid and available to bid, and bid amount is greater than
             * initial price or current highest bid
             */
            if (itemObject == null) {
                System.out.println("BID Rejected - For invalid itemObject ID");
                output = "Item "+itmObjID+" is not in the auction";
            }
            else {
                // check if the item is active or not. Also check the availability
                if (!(itemObject.getString(Keys.STATE.name()).matches("AVAILABLE|ONBID")) ||
                itemObject.getString(Keys.ACTIVEITEM.name()).equals("0")) {
                    System.out.println("BID Rejected - Item not Available !!");
                    output = "Item " + itmObjID + " is not available.";
                }
                else {
                    double newBid = json.getDouble(Keys.AMOUNT.name());
                    double currBid = itemObject.getDouble(Keys.CURRENTBID.name());

                    if (currBid < newBid ) {
                        JSONObject jsonObj = new JSONObject();
                        jsonObj.put(Keys.SENDER.name(),Commands.AUCTION.name());
                        jsonObj.put(Keys.REQUESTTYPE.name(),Commands.BLOCK.name());
                        jsonObj.put(Keys.ACCOUNTID.name(),json.getInt(Keys.ACCOUNTID.name()));
                        jsonObj.put(Keys.AMOUNT.name(),newBid);
                        try {
                            // requests bank for the balance of agent
                            PrintWriter out = new PrintWriter(
                                    auctionHouseClient.getBankSocket().getOutputStream(), true);
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(auctionHouseClient
                                            .getBankSocket().getInputStream()));
                            out.println(jsonObj.toString());

                            String bankResponse = in.readLine();

                            String itemInfo = "Item ID: " + itmObjID +" {"+
                                    itemObject.getString(Keys.NAME.name()) + "} : " +
                                    itemObject.getDouble(Keys.CURRENTBID.name()) +
                                    " Starting Price: " + itemObject.getDouble(Keys.INIITIALPRICE.name());

                            System.out.println(bankResponse);

                            // swap highest bidder
                            if (bankResponse.equals(Keys.SUCCESS.name())) {
                                int previousHighBidder = itemObject.getInt(Keys.HIGHESTBIDDER.name());
                                int previousHBAccountID = itemObject.getInt(Keys.ACCOUNTID.name());
                                double prevBidAmount = itemObject.getDouble(Keys.CURRENTBID.name());

                                itemObject.remove(Keys.HIGHESTBIDDER.name());
                                itemObject.remove(Keys.ACCOUNTID.name());
                                itemObject.remove(Keys.CURRENTBID.name());
                                itemObject.put(Keys.HIGHESTBIDDER.name(), bidderID);
                                itemObject.put(Keys.ACCOUNTID.name(), json.getInt(Keys.ACCOUNTID.name()));
                                itemObject.put(Keys.CURRENTBID.name(), newBid);

                                itemInfo = "Item ID: " + itmObjID +" {"+
                                        itemObject.getString(Keys.NAME.name()) + "} : " +
                                        itemObject.getDouble(Keys.CURRENTBID.name()) +
                                        " Starting Price: " + itemObject.getDouble(Keys.INIITIALPRICE.name());

                                System.out.println("Bid Accepted!");
                                output = Commands.BIDACCEPTED.name()+"," + itemInfo;

                                String finalInfo = itemInfo;

                                // Set the task of timer to 25 seconds
                                TimerTask task = bidInterval(itemObject,finalInfo);
                                auctionItems.bidInterval(itmObjID, task);

                                if (previousHighBidder != -1) {
                                    // notify outbid to agent
                                    bidLostInfo(previousHighBidder,itemInfo);
                                    JSONObject jsonbObject = new JSONObject();
                                    jsonbObject.put(Keys.SENDER.name(),Commands.AUCTION.name());
                                    jsonbObject.put(Keys.REQUESTTYPE.name(),Commands.UNBLOCK.name());
                                    jsonbObject.put(Keys.ACCOUNTID.name(),previousHBAccountID);
                                    jsonbObject.put(Keys.AMOUNT.name(),prevBidAmount);
                                    out.println(jsonbObject);

                                    bankResponse = in.readLine();

                                    if (bankResponse.equals(Keys.SUCCESS.name())) {
                                        fundUnblock(previousHighBidder, prevBidAmount);
                                    }
                                    else {
                                        System.out.println(bankResponse);
                                    }
                                }
                            }
                            else {
                                System.out.println("Bid Rejected - You don't have enough Fund");
                                output = Commands.BIDREJECTED.name()+",You don't have enough Fund,"
                                        + itemInfo;
                            }
                        } catch (IOException e) {
                            output = "No response from Bank";
                        }
                    }
                    else {
                        System.out.println("Bid Rejected - Your Bid is lower than previous Bid");
                        output = Commands.BIDREJECTED.name()+", item " + itmObjID +
                                " is currently bidding for " + currBid;
                    }
                }
            }
        }
        else if (command.equalsIgnoreCase(Commands.REQUESTITEMS.name())) {
            output = auctionItems.showItems();
        }
        else {
            output = "Command couldn't be processed from Auction House";
        }
        return output;
    }


    // sends outbid message to agent
    private void bidLostInfo(int prevHighestBidder, String item_info) throws IOException {
        Socket socket = agentSocketsList.get(prevHighestBidder);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(Commands.OUTBID.name()+"," + item_info);
    }

    // Unblock amount of agent which he had bid on Item
    private void fundUnblock(int prevHighestBidder, double bidAmount) throws IOException {
        Socket socket = agentSocketsList.get(prevHighestBidder);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println("Amount "+bidAmount+" for the item is unblocked");
    }

    /*
     * Sets Bid Interval for an Item in auction
     */
    private TimerTask bidInterval(JSONObject jsonItem, String itemInfo) {
        return new TimerTask() {
            @Override
            public void run() {
                PrintWriter outTOAgent;
                try {
                    outTOAgent = new PrintWriter(agentClientSocket.getOutputStream(), true);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                try {
                    outTOAgent.println(Commands.WONBID.name()+"," + itemInfo + "," +
                            auctionHouseClient.getAccID() + "," + jsonItem.getDouble(Keys.CURRENTBID.name()));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                jsonItem.remove(Keys.STATE.name());
                try {
                    jsonItem.put(Keys.STATE.name(),Commands.ITEMSOLD.name());
                    auctionItems.updateToShowArray();

                    if (auctionItems.isItemListEmpty()) {
                        System.out.println("No Items Available");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
    }

}
