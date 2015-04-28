/**
 * Created by hzding621 on 4/27/15.
 */
import java.util.*;
import java.net.*;
import java.io.*;
import java.sql.*;
import java.util.concurrent.locks.*;

class NodeChecker extends Thread {
    Node _parent;
    public NodeChecker(Node n) {
        _parent = n;
    }
    public void run() {
        while (true) {
            try {
                // Sleep for retry_factor x cs_int
                Thread.sleep(Main.retry_factor*Main.cs_int);
            } catch (InterruptedException e) {
                continue;
            }
            List<Integer> l = new ArrayList<Integer>();
            boolean should = false;
            for (int i=0; i<4; i++) {
                if (_parent.responseSet[i] == 0) {
                    should = true;
                }
                else {
                    int j = Main.getVotingSet(_parent.getId())[i];
                    l.add(j); // Should yield to all grants
                }
            }
            if (should) {
                for (int j: l) {
                    try (
                            Socket s = new Socket("127.0.0.1", _parent._server._port);
                            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                    ) {
                        // Perform a fake Inquire as if it was sent from the granter
                        out.println("INQUIRE,"+j);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            }
        }
    }
}

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

                    // Print to stdout if option = 1
                    if (!m[0].equals("INIT") && Main.option == 1) {
                        String str = Main.getTimeFormatted() + " "+get_Id()+" "+m[m.length-1]+" "+m[0];
                        if (m[0].equals("REQUEST"))
                            str += " "+m[1];
                        System.out.println(str);
                    }

                    if (m[0].equals("INIT")) {
                        out.println("READY");
                    } else {
                        int from = Integer.parseInt(m[m.length-1]);
                        switch (m[0]) {
                            case "REQUEST": {

                                long timestamp = Long.parseLong(m[1]);
                                Request n = new Request(from, timestamp);
                                synchronized (_parent.responseSet) {
                                    if (_parent.granted != null) {
                                        // always enqueue request if already granted
                                        // then check if granted has lower priority
                                        // if so, send Inquire to granted, otherwise do nothing
                                        _parent._pq.add(n);
                                        if (_parent.granted.compareTo(n)<0 && _parent.has_inquired == false) {
                                            _parent.sendMessage(_parent.granted._from, "INQUIRE");
                                            _parent.has_inquired = true;
                                        }
                                    } else {
                                        // if not granted, then grant this request
                                        _parent.granted = n;
                                        _parent.sendMessage(from, "GRANT");
                                    }
                                }
                                break;
                            }
                            case "GRANT": {
                                synchronized (_parent.responseSet) {
                                    _parent.responseSet[Main.getIndex(get_Id(), from)] = 1;
                                }
                                break;
                            }
                            case "RELINQUISH": {
                                // A node has yielded its grant, add back granted to the queue and dequeue the one with highest priority
                                // Send grant to it.
                                synchronized (_parent.responseSet) {
                                    Request o = _parent.granted;
                                    _parent._pq.add(o);
                                    Request p = _parent._pq.poll();

                                    _parent.granted = p;
                                    _parent.sendMessage(p._from, "GRANT");

                                    _parent.has_inquired = false;
                                }
                                break;
                            }
                            case "INQUIRE": {
                                // Upon receipt of Inquire, if I am not able to proceed, then yield my grant

                                synchronized (_parent.responseSet) {
                                    boolean should = false;
                                    for (int i=0; i<4; i++) {
                                        if (_parent.responseSet[i] != 1) {
                                            should = true;
                                            break;
                                        }
                                    }
                                    if (should) {
                                        _parent.responseSet[Main.getIndex(get_Id(), from)] = 0;
                                        _parent.sendMessage(from, "RELINQUISH");
                                    }

                                }
                                break;
                            }
                            case "RELEASE": {
                                // Upon receipt of Release, dequeue from pq and send grant to it

                                _parent.granted = null;
                                if (_parent._pq.isEmpty() == false) {
                                    Request i = _parent._pq.poll();
                                    _parent.granted = i;
                                    _parent.sendMessage(i._from, "GRANT");
                                }
                                _parent.has_inquired = false;
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

    /*
     * Method to establish connections with all voting set servers
     */
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
                    // Retry until server is connected
                }
            }
        }
        _parent._checker.start(); // start the checker thread
    }
    public int entry() {
        long t = System.currentTimeMillis();
        int[] vs = Main.getVotingSet(get_Id());
        for (int i =0;i<4;i++ ) {
            int j = vs[i];
            _parent.sendMessage(j, "REQUEST,"+t); // send request to all voting set
        }
        while (true) {
            // Busy-waiting for all voting set to reply
            // could improve here by using Conditional vars

            boolean ok;
            synchronized (_parent.responseSet) {
                ok = _parent.responseSet[0] == 1 &&
                        _parent.responseSet[1] == 1 &&
                        _parent.responseSet[2] == 1 &&
                        _parent.responseSet[3] == 1;
            }
            if ( ok ) {
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

            entry(); // call to acquire lock
            System.out.println(Main.getTimeFormatted()+" "+get_Id()+" "+_parent.getVTString());
            try {
                Thread.sleep(Main.cs_int);

            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }

            exitEntry(); // call to release lock
            try {
                Thread.sleep(Main.next_req);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}

class Request implements Comparable<Request>{
    Integer _from;
    Long _timestamp;
    public Request(int f, long t) {
        _from = f;
        _timestamp = t;
    }

    /*
     * Method to tiebreak two request
     */
    public int compareTo(Request o) {
        int i = o._timestamp.compareTo(_timestamp);
        if (i != 0)
            return i;
        else
            return o._from.compareTo(_from);
    }
}

public class Node {

    int _id;
    NodeServer _server; // server thread for receiving
    NodeClient _client; // client thread for performing entry/exit
    NodeChecker _checker; // checker thread for deadlock handling

    int[] responseSet = {0,0,0,0}; // grants from all voting set
    Request granted = null; // which node I am currently granting
    PriorityQueue<Request> _pq = new PriorityQueue<Request>();
    boolean has_inquired = false;

    /*
     * Handles all the message passing logic
     * Always concatenate sender id
     */
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

    /*
     * String representation of voting set
     */

    public String getVTString() {
        int[] vt = Main.getVotingSet(_id);
        String s = "";
        for (int i=0; i<4; i++) {
            s += vt[i]+" ";
        }
        return s;
    }

    public int getId() {
        return _id;
    }
    public Node(int id) {
        _id = id;
        _server = new NodeServer(this, Main.getPort(id));
        _client = new NodeClient(this);
        _checker = new NodeChecker(this);
    }
    public void startServer() {
        _server.start();
    }

    public void startClient() {
        _client.start();
    }
}
