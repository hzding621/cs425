import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.sql.*;

public class Node {

	private static boolean DEBUG_MODE = true;

	private static String config = "config.txt";

	private static int CONTROLLER_PORT = 8888;
	private static int MY_NODE_PORT = - 1;
	private static int MY_NODE_NUM = -1;
	private static int MY_MAX_DELAY = -1;
	private static int TERMINAL_DELAY = 0;

	private static HashMap<Integer, Integer> ports, delays;

	public static Hashtable<Integer, Integer> store = new Hashtable<Integer, Integer>();

	public final static Lock lock = new ReentrantLock();
	public final static Condition shouldProceed = lock.newCondition();
	public static boolean waitingForResponse = false;
	public static long lastResponseTime = -1;

	// public final static Lock utilLock = new ReentrantLock();
	// public final static Condition utilRestart = lock.newCondition();
	// public static boolean utilHault = false;

	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println(
				"Usage: java Node <node number> <config file>");
			System.exit(1);
		}
		config = args[1];

		MY_NODE_NUM = Integer.parseInt(args[0]);
		ports = new HashMap<Integer, Integer>();
		delays = new HashMap<Integer, Integer>();
		try (
			BufferedReader br = new BufferedReader(new FileReader(config))
		) {
			String line = br.readLine();
			while (line != null) {
				String[] params = line.split("=");
				if (params[0].equals("CONTROLLER_PORT"))
					CONTROLLER_PORT = Integer.parseInt(params[1]);
				if (params[0].equals("PORTS")) {
					String[] p = params[1].split(",");
					for (int i=0; i<p.length; i++) {
						ports.put(i, Integer.parseInt(p[i]));
					}
				}
				if (params[0].equals("MAX_DELAY")) {
					String[] p = params[1].split(",");
					for (int i=0; i<p.length; i++) {
						delays.put(i, Integer.parseInt(p[i]));
					}
				}
				if (params[0].equals("VERBOSE")) 
					if (Integer.parseInt(params[1]) == 0)
						DEBUG_MODE = false;
				// add more parameters here
				line = br.readLine();
			}
		} catch (FileNotFoundException e) {
			System.err.println("Config file is not found.");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to 127.0.0.1");
			System.exit(1);
		}

		if (!ports.containsKey(MY_NODE_NUM) || !delays.containsKey(MY_NODE_NUM)) {
			System.err.println("Node number error.");
			System.exit(1);
		}


		// Start node server thread
		MY_NODE_PORT = ports.get(MY_NODE_NUM);
		MY_MAX_DELAY = delays.get(MY_NODE_NUM);
		NodeServer server = new NodeServer(MY_NODE_NUM, MY_NODE_PORT, MY_MAX_DELAY, DEBUG_MODE);
		server.start();

		NodeClient client = new NodeClient(CONTROLLER_PORT, MY_NODE_NUM, DEBUG_MODE);
		client.start();

		// Accept user command and send to controller through socket
		Scanner stdIn = new Scanner(System.in);
		stdIn.useDelimiter(System.getProperty("line.separator"));
		while (stdIn.hasNext()) {

			String message = stdIn.next();
			client.enqueue(message);
			
		}
		
	}
}