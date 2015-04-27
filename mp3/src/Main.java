/**
 * Created by hzding621 on 4/27/15.
 */
import org.omg.PortableInterceptor.INACTIVE;

import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.sql.*;

public class Main {

    public static int cs_int;
    public static int next_req;
    public static int tot_exec_time;
    public static int option;
    public static List<Node> _nodes = new ArrayList<Node>();

    public static int getPort(int id) {
        return 9000 + id;
    }
    public static int[] getVotingSet(int id) {
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

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: java Main <cs_int> <next_req> <tot_exec_time> <option>");
            System.exit(1);
        }
        cs_int = Integer.parseInt(args[0]);
        next_req = Integer.parseInt(args[1]);
        tot_exec_time = Integer.parseInt(args[2]);
        option = Integer.parseInt(args[3]);
        for (int i=0; i<9; i++) {
            Node n = new Node(i);
            _nodes.add(n);
            n.startServer();
        }
        for (int i=0; i<9; i++) {
            _nodes.get(i).startClient();
        }
        while (true) {

        }
    }
}
