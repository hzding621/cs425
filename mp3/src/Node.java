/**
 * Created by hzding621 on 4/27/15.
 */
import java.util.*;
import java.net.*;
import java.io.*;
import java.sql.*;
import java.util.concurrent.locks.*;

class NodeServer extends Thread {
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
                ) {
            while (true) {
                try (
                        Socket s = serverSocket.accept();
                        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                ) {
                    String mm = in.readLine();
                    String[] m = mm.split(",");
                    if (m[0].equals("INIT")) {
                        out.println("READY");
                    } else {
                        int from = Integer.parseInt(m[1]);
                        switch (m[0]) {
                            case "REQUEST": {
                                if (_parent.granted != -1) {
                                    _parent.queue.add(from);
                                    _parent.sendMessage(from, "FAIL");
                                } else {
                                    _parent.granted = from;
                                    _parent.sendMessage(from, "GRANT");
                                }
                                break;
                            }
                            case "GRANT": {
                                synchronized (_parent.responseSet) {
                                    _parent.responseSet[Main.getIndex(get_Id(), from)] = 1;
                                }
                                break;
                            }
                            case "FAIL": {
                                synchronized (_parent.responseSet) {
                                    _parent.responseSet[Main.getIndex(get_Id(), from)] = 2;
                                }
                                break;
                            }
                            case "RELEASE": {
                                _parent.granted = -1;
                                if (_parent.queue.isEmpty() == false) {
                                    int i = _parent.queue.remove();
                                    _parent.granted = i;
                                    _parent.sendMessage(i, "GRANT");
                                }
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    // Nothing
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}

class NodeClient extends Thread{
    Node _parent;
    public int get_Id() {
        return _parent.getId();
    }
    public NodeClient(Node n) {
        _parent = n;
    }
    public void init() {
        for (int p: Main.getVotingPort(get_Id())) {
            while (true) {
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
        System.err.println(get_Id()+" is ready.");
    }
    public int entry() {
        for (int i: Main.getVotingSet(get_Id())) {
            _parent.sendMessage(i, "REQUEST");
        }
        while (true) {

            boolean ok;
            synchronized (_parent.responseSet) {
                ok = _parent.responseSet[0] == 1 &&
                        _parent.responseSet[1] == 1 &&
                        _parent.responseSet[2] == 1 &&
                        _parent.responseSet[3] == 1;
            }
            if ( ok ) {
//                Main.print(get_Id());
                return 0;
            }
        }
    }
    public void exitEntry() {
        synchronized (_parent.responseSet) {
            for (int i=0; i<4; i++) {
                if (_parent.responseSet[i] == 1) {
                    _parent.sendMessage(Main.getVotingSet(get_Id())[i], "RELEASE");
                    _parent.responseSet[i] = 0;
                } else {
                    _parent.responseSet[i] = 0;
                }
            }
        }
    }
    public void run() {
        init();
        while (true) {

            entry();
            Time t = new Time(System.currentTimeMillis());
            System.out.println(t.toString() + " " + get_Id() + " Enter CS");
            try {
                Thread.sleep(Main.cs_int);

            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
            t = new Time(System.currentTimeMillis());
            System.out.println(t.toString()+" "+get_Id() + " Exit CS");
            exitEntry();
            try {
                Thread.sleep(Main.next_req);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}

public class Node {

    int _id;
    NodeServer _server;
    NodeClient _client;
    Lock lock = new ReentrantLock();
    int[] responseSet = {0,0,0,0};
    int granted = -1;
    Queue<Integer> queue = new ArrayDeque<Integer>();
    public void sendMessage(int id, String message) {
        try (
                Socket s = new Socket("127.0.0.1", Main.getPort(id));
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                ) {
            out.println(message+","+_id);
        }catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

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
