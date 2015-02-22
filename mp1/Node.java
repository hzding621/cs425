import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.sql.*;

public class Node {

	private static String config = "config.txt";

	private static int CONTROLLER_PORT = 8888;
	private static int MY_NODE_PORT = - 1;
	private static int MY_NODE_NUM = -1;
	private static int MY_MAX_DELAY = -1;

	private static HashMap<Integer, Integer> ports, delays;

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
		NodeServer server = new NodeServer(MY_NODE_NUM, MY_NODE_PORT, MY_MAX_DELAY);
		server.start();


		// Accept user command and send to controller through socket
		Scanner stdIn = new Scanner(System.in);
		stdIn.useDelimiter(System.getProperty("line.separator"));
		while (stdIn.hasNext()) {

			String userCommand = stdIn.next();
			String[] cmds = userCommand.split(" ");
			if (cmds.length != 3 || !cmds[0].equals("Send")) {
				System.out.println("User command unknown.");
				continue;
			}
			String message = cmds[1];
			int toNode = Integer.parseInt(cmds[2]);
			int fromNode = MY_NODE_NUM;
			try (
				Socket socket = new Socket("127.0.0.1", CONTROLLER_PORT);
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			) {
				out.println(fromNode+";"+toNode+";"+message+";");
				socket.close();
				Time curTime = new Time(System.currentTimeMillis());
				System.out.println("Sent " + message + " to " + toNode + ", system time is " + curTime.toString() ) ;
			} catch (UnknownHostException e) {
				System.err.println("Unknown Host");
			} catch (IOException e) {
				System.err.println("Controller Connection Failure. Command Ignored.");
				// Simply ignore the user command 
			} 
		}
		
	}
}