/**
 * Created by hzding621 on 4/27/15.
 */
import org.omg.PortableInterceptor.INACTIVE;

import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.text.*;


/*
 *  Thread to handle countdown functionality
 *  Exit the program when return from sleep
 */
class Countdown extends Thread{
    int seconds;
    public Countdown(int s) {
        seconds = s;
    }
    public void run() {
        try {
            Thread.sleep(seconds*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.err.println("Program terminated.");
        System.exit(0);
    }
}

public class Main {

    public static final boolean DEBUG = true;   // internal use only
    public static int cs_int;
    public static int next_req;
    public static int tot_exec_time;
    public static int option;
    public static int retry_factor = 10;

    public static List<Node> _nodes = new ArrayList<Node>();

    public static int getTimeout(int id) {
        return 1000;
    }

    /*
     *  Print time utility function
     */
    public final static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    public static String getTimeFormatted() {

        Date now = new Date();
        String strDate = sdf.format(now);
        return strDate;
    }


    public static int getPort(int id) {
        return 9000 + id;
    }
    public static int[] getVotingSet(int id) {
        // Uses voting set configuration in this paper:
        // http://www.cse.cuhk.edu.hk/~ttwong/papers/mutex/icdcs97.pdf

        int[] array = {0, 1, 2, 4};
        for (int i=0; i<4; i++) {
            array[i] = ( array[i] + id ) % 9;
        }
        return array;
    }
    public static int[] getVotingPort(int id) {
        int[] array = getVotingSet(id);
        for (int i=0; i<4; i++) {
            array[i] = getPort(array[i]);
        }
        return array;
    }

    /*
     * Get the index where a the voting node "from" should be located at for node "id"
     * return -1 if voting node is invalid
     */
    public static int getIndex(int id, int from) {
        int i = (from + 9 - id) % 9;
        switch (i) {
            case 0: return 0;
            case 1: return 1;
            case 2: return 2;
            case 4: return 3;
        }
        System.out.println(id+" "+from);
        return -1;
    }

    public static void main(String[] args) {
        if (args.length != 4 && args.length != 5) {
            System.out.println("Usage: java Main <cs_int> <next_req> <tot_exec_time> <option> [retry_factor]");
            System.exit(1);
        }
        cs_int = Integer.parseInt(args[0]);
        next_req = Integer.parseInt(args[1]);
        tot_exec_time = Integer.parseInt(args[2]);
        option = Integer.parseInt(args[3]);
        if (args.length == 5)
            retry_factor = Integer.parseInt(args[4]);

        // Allocate all node
        for (int i=0; i<9; i++) {
            Node n = new Node(i);
            _nodes.add(n);
            n.startServer(); // start all servers first
        }
        for (int i=0; i<9; i++) {
            _nodes.get(i).startClient(); // start all clients
        }

        Countdown c = new Countdown(tot_exec_time);
        c.start();

        Scanner sc = new Scanner(System.in);
        while (true) {
            if (DEBUG) {
                String m = sc.next();
                if (m.equals("show")) {
                    print(-1);
                }
            }
        }
    }

    public static void print(int caller) {
        StringBuilder p = new StringBuilder();
        for (Node n : _nodes) {
            synchronized (n.responseSet) {
                p.append(n._id+":"+n.granted._from+","+n.responseSet[0]+n.responseSet[1]+n.responseSet[2]+n.responseSet[3]+"\n");
            }
        }
        String c = caller != -1? ""+caller : "";
        System.out.print("**********\n"+ c +"\n" + p.toString()+"**********\n");
    }
}
