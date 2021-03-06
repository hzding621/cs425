import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.sql.*;

public class Node {

	private static boolean DEBUG_MODE = true;

	private static String config = "config.txt";

	public static int CONTROLLER_PORT = 8888;
	public static int NODE_NUM = 4;

	private static int MY_NODE_PORT = - 1;
	private static int MY_NODE_NUM = -1;
	private static int MY_MAX_DELAY = -1;
	private static int TERMINAL_DELAY = 0;



	public static NodeServer server = null;
	public static NodeClient client = null;

	private static HashMap<Integer, Integer> ports, delays;

	public final static Lock storeLock = new ReentrantLock();
	public static HashMap<Integer, Integer> store = new HashMap<Integer, Integer>();
	public static HashMap<Integer, Long> timestamps = new HashMap<Integer, Long>();

	public final static Lock lock = new ReentrantLock();
	public final static Condition shouldProceed = lock.newCondition();
	public static boolean waitingForResponse = false;
	public static long lastResponseTime = -1;

	public static long ackTimestamp = -1;
	public static int currentResponded = 0;
	public static int needResponded = 0;
	public static int requestedKey = 0;
	public static int requestedValue = 0;
	public static String responseType = "";

	// public final static Lock utilLock = new ReentrantLock();
	// public final static Condition utilRestart = lock.newCondition();
	// public static boolean utilHalt = false;

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
				if (params[0].equals("NODE_NUM"))
					NODE_NUM = Integer.parseInt(params[1]);
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
		server = new NodeServer(MY_NODE_NUM, MY_NODE_PORT, MY_MAX_DELAY, DEBUG_MODE);
		client = new NodeClient(CONTROLLER_PORT, MY_NODE_NUM, DEBUG_MODE);
		server.start();

		try (
			Socket socket = new Socket("127.0.0.1", CONTROLLER_PORT);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		) {
			out.println("READY");
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Accept user command and send to controller through socket
		Scanner stdIn = new Scanner(System.in);
		stdIn.useDelimiter(System.getProperty("line.separator"));
		while (stdIn.hasNext()) {

			String message = stdIn.next();
			client.enqueue(message);
			
		}
		
	}
}