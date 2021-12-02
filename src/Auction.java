import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// The Auction Class will contain the runnable part of operating the auction
// generating an Auction will automatically generate an AuctionHouse and connect
// it to the Bank.
public class Auction implements Runnable
{
    private final Socket bank;
    private final Socket auctionHouse;
    private final PrintWriter out;
    private final BufferedReader in;

    public Auction(Socket bank)
    {
         this.bank = bank;
    }

    // Print the list of items up for auction
    public void printItemList()
    {

    }

    @Override
    public void run()
    {

    }

}