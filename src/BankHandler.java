import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

/*
 * Handles all requests from agent and auction house
 */
public class BankHandler implements Runnable{
    private final Socket socket;
    private String currClient ="";
    private final int accountID;
    private final ClientsInfo clientsInfo;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Map<Integer,BankAccount> bankAccounts;

    // constructor
    public BankHandler(Socket socket, int accountID, Map<Integer,BankAccount> bankAccounts,
                       ClientsInfo clientsInfo) throws IOException {
        this.socket = socket;
        this.accountID = accountID;
        this.bankAccounts = bankAccounts;
        this.clientsInfo = clientsInfo;
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /*
     * Listens to request from agent and auction house
     * Sends them relevant response
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
                    socket.close();
                    if (currClient.equals(Commands.AGENT.name())) {
                        System.out.println("AGENT LEFT !!");
                        clientsInfo.agentsSocketsList.remove(socket);
                    }
                    else if (currClient.equals(Commands.AUCTION.name())) {
                        System.out.println("AUCTION HOUSE LEFT !!");

                        // updates house list when auction house is closed
                        updateHouseList(clientsInfo.houseSocketsList.indexOf(socket));
                    }
                }

            } catch (IOException e) { 
                inputLine = null;
            } catch (JSONException e) {
                out.println("Request couldn't be Processed !!");
            }
        } while (inputLine != null);
    }

    /*
     * check the request type and sends relevant response to Clients
     * returns output line to send to client
     */
    private String processJSON(JSONObject input) throws JSONException, IOException {
        String command;
        String output = "";

        try {
            command = input.getString(Keys.REQUESTTYPE.name());
        } catch (JSONException e) {
            return "REQUESTTYPE not found !!";
        }

        if (command.equalsIgnoreCase(Commands.OPENACCOUNT.name())) {
            String name = input.getString(Keys.NAME.name());
            double amount = input.getDouble(Keys.AMOUNT.name());
            output = initAccount(accountID, name, amount);

            currClient = Commands.AGENT.name();
            clientsInfo.agentsSocketsList.add(socket);

            output += ",";

            for (int i = 0; i < clientsInfo.houseAddress.size(); i++) {
                output += clientsInfo.houseAddress.get(i) + ",";
            }
        }
        else if (command.equalsIgnoreCase(Commands.CHECKBALANCE.name())) {
            output = getBalanceInfo(input.getInt(Keys.ACCOUNTID.name()));
        }
        else if (command.equalsIgnoreCase(Commands.OPENAUCTION.name())) {
            output = initAccount(accountID, "house",0);

            currClient = Commands.AUCTION.name();
            String info = input.getString(Keys.ADDRESSINFO.name());
            clientsInfo.houseAddress.add(info);
            clientsInfo.houseSocketsList.add(socket);
            updateHouseList(-1);

        }
        else if (command.equalsIgnoreCase(Commands.HOUSELISTS.name())) {
            output = "Auction house list,";
            if (clientsInfo.houseAddress.size() == 0) {
                output += "Auction houses not found";
            }
            else {
                for (String str : clientsInfo.houseAddress) {
                    output += str + ",";
                }
            }
        }
        else if (command.equalsIgnoreCase(Commands.BLOCK.name())) {
            int accountID = input.getInt(Keys.ACCOUNTID.name());
            double amount = input.getDouble(Keys.AMOUNT.name());
            output = blockAmount(accountID, amount);
        }
        else if (command.equalsIgnoreCase(Commands.UNBLOCK.name())) {
            int accountID = input.getInt(Keys.ACCOUNTID.name());
            double amount = input.getDouble(Keys.AMOUNT.name());
            output = unblockAmount(accountID, amount);
        }
        else if (command.equalsIgnoreCase(Commands.TRANSFERAMOUNT.name())) {
            int toAccountID = input.getInt(Keys.ACCOUNTID.name());
            double amount = input.getDouble(Keys.AMOUNT.name());
            output = transferAmount(accountID,toAccountID,amount) + "," + toAccountID;
        }
        else {
            output = "Invalid Request type";
        }
        return output;
    }

    // initialize account of agent and auction house
    public String initAccount(int id, String name, double balance) {
        bankAccounts.put(id, new BankAccount(Math.max(balance,0)));
        if (!name.equalsIgnoreCase("house")) {
            return "Hey agent " + name + ". Your account number is " + id;
        }else{
            return "Your account number is "+id;
        }
    }

    // returns balance info of client
    public String getBalanceInfo(int id) {
        BankAccount account = bankAccounts.get(id);
        String s = "Total balance: " + account.getTotalBalance() + ",";
        s += "Available funds: " + account.getAvailableBalance() + ",";
        s += "Holding funds: " + account.getBalanceOnHold();
        return s;
    }

    // block given amount into the account number accesses with id
    public String blockAmount(int id, double amount) {
        if (amount <= 0) return Keys.UNSUCCESS.name();
        BankAccount account = bankAccounts.get(id);
        return account.blockAmount(amount) ? Keys.SUCCESS.name() : Keys.UNSUCCESS.name();
    }

    // unblock given amount into the account number accesses with id
    public String unblockAmount(int id, double amount) {
        if (amount <= 0) return Keys.UNSUCCESS.name();
        BankAccount account = bankAccounts.get(id);
        return account.unBlockAmount(amount) ? Keys.SUCCESS.name() : Keys.UNSUCCESS.name();
    }

    // transfers amount from a bank account to another
    // Bank Account accessed with given ids a and b
    public String transferAmount(int a, int b, double amount) {
        if (amount <= 0) return Keys.UNSUCCESS.name();
        BankAccount fromAccount = bankAccounts.get(a);
        BankAccount toAccount = bankAccounts.get(b);
        return fromAccount.transferAmountTo(toAccount, amount) ? Keys.SUCCESS.name() : Keys.UNSUCCESS.name();
    }


    // updates house list
    private void updateHouseList(int index) throws IOException {
        if (index!=-1) {
            clientsInfo.houseSocketsList.remove(socket);
            clientsInfo.houseAddress.remove(index);
        }
        if (clientsInfo.agentsSocketsList.size() == 0) return;

        for (Socket s : clientsInfo.agentsSocketsList) {
            PrintWriter out = new PrintWriter(s.getOutputStream(),true);
            String output = "Auction house list,";

            if (clientsInfo.houseAddress.size() != 0) {
                for (String str : clientsInfo.houseAddress) {
                    output += str + ",";
                }
            }
            else {
                output += "Auction houses not found";
            }
            out.println(output);
        }
    }
}
