import java.net.Socket;
import java.util.ArrayList;

/*
    Gets initialized to each client that connects to Bank
    Keep track of all agents sockets, house sockets and house ip address
 */
public class ClientsInfo {

     final ArrayList<String> houseAddress;
     final ArrayList<Socket> houseSocketsList;
     final ArrayList<Socket> agentsSocketsList;

    public ClientsInfo(){
        houseAddress = new ArrayList<>();
        houseSocketsList = new ArrayList<>();
        agentsSocketsList = new ArrayList<>();
    }
}
