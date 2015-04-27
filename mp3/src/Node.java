/**
 * Created by hzding621 on 4/27/15.
 */
import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.sql.*;

class NodeServer {
    Node _parent;
    int _port;
    public int get_Id() {
        return _parent.getId();
    }
    public NodeServer(Node n, int port) {
        _parent = n;
        _port = port;
    }
    public void run() {
        try (
                ServerSocket serverSocket = new ServerSocket(_port);
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                ) {
            Socket s = serverSocket.accept();
            String mm = in.readLine();
            String[] m = mm.split(",");
            if (m[0].equals("INIT")) {
                out.println("READY");
            } else if 

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}

class NodeClient {
    Node _parent;
    public int get_Id() {
        return _parent.getId();
    }
    public NodeClient(Node n) {
        _parent = n;
    }
    public void init() {
        for (int p: Main.getVotingPort(get_Id())) {
            try (
                    Socket s = new Socket("127.0.0.1", p);
                    PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    ){
                out.println("INIT");
                String m = in.readLine();
                if (m.equals("READY"))
                    break;

            } catch (IOException e) {
                // Retry
            }
        }
    }
}

public class Node {

    int _id;
    NodeServer _server;
    NodeClient _client;
    public int getId() {
        return _id;
    }
    public Node(int id) {
        _id = id;
        _server = new NodeServer(this, Main.getPort(id));
        _client = new NodeClient(this);
    }
    public void startServer() {
        _server.start();
    }

    public void startClient() {
        _client.start();
    }
}
