import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Bank
{
    public void main(String[] args)
    {
        int port = Integer.parseInt(args[0]);

        ServerSocket bankSever = new ServerSocket(port);
    }

}
