import java.io.*;
import java.net.*;
import java.util.*;

public class Controller {

	private static String config = "config.txt";
	private static int CONTROLLER_PORT = 8888;
	private static int NODE_NUM = 4;
	private static long MAX_DELAY = 10000;

	private static Random r = new Random();
	public static ArrayList<ArrayList<ControllerChannel>> channels = null;

	public static HashMap<Integer, Integer> ports = null;

	public static long getRandomDelay() {
		return r.nextLong() % MAX_DELAY;
	}

	public static ControllerChannel getChannel(int fromNode, int toNode) {
		return channels.get(fromNode).get(toNode);
	}

	public static void deliverMessage(int toNode, String message) {
		// Initiate client and conenct to toNode
		// @todo
		try (
			Socket socket = new Socket("127.0.0.1", ports.get(toNode));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		) {
			out.println(message);
		} catch (UnknownHostException e) {
            System.err.println("Don't know about host ");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection");
            System.exit(1);
        }
	}

	public static void main(String[] args) {
		
		if (args.length > 1) {
			System.err.println(
				"Usage: java CentralServer [config file]");
			System.exit(1);
		}
		if (args.length == 1)
			config = args[0];
		// Read parameters from config file
		ports = new HashMap<Integer, Integer>();
		try (
			BufferedReader br = new BufferedReader(new FileReader(config))
		) {
			String line = br.readLine();
			while (line != null) {
				String[] params = line.split("=");
				if (params[0].equals("CONTROLLER_PORT"))
					CONTROLLER_PORT = Integer.parseInt(params[1]);
				if (params[0].equals("NODE_NUM"))
					NODE_NUM = Integer.parseInt(params[1]);
				if (params[0].equals("MAX_DELAY"))
					MAX_DELAY = Integer.parseInt(params[1]);
				if (params[0].equals("PORTS")) {
					String[] p = params[1].split(",");
					for (int i=0; i<p.length; i++) {
						ports.put(i, Integer.parseInt(p[i]));
					}
				}
				// add more parameters here
				line = br.readLine();
			}
		}
		catch (FileNotFoundException e) {
			System.err.println("Config File Not Found");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Config File Error");
			System.exit(1);
		}

		// System.out.println("Node Number = " + NODE_NUM);

		channels = new ArrayList<ArrayList<ControllerChannel>>();
		for (int i=0; i<NODE_NUM; i++) {
			channels.add(new ArrayList<ControllerChannel>());
			for (int j=0; j<NODE_NUM; j++) {
				if (j != i) {
					ControllerChannel t = new ControllerChannel(i, j);
					channels.get(i).add(t);
					t.start();
				}
				else 
					channels.get(i).add(null);
			}
		}
		
		try (ServerSocket serverSocket = new ServerSocket(CONTROLLER_PORT)) { 
			System.out.println("Controller listens at port " + CONTROLLER_PORT);
			while (true) {
				Socket socket = serverSocket.accept();

				// connection to Controller is sequential

				try (
					// PrintWriter out = new PrintWriter(socket.getOutputStream(), true); 
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				) {
					String message = in.readLine();
					socket.close();
					String[] messageLines = message.split(";");
					int fromNode = Integer.parseInt(messageLines[0]);
					int toNode = Integer.parseInt(messageLines[1]);
					// String msgContent = messageLines[3];
					
					ControllerChannel c = getChannel(fromNode, toNode);
					long lastDeliveryTime = c.getLastDeliveryTime();     
					long randomDelay = getRandomDelay();
					long currentTime = System.currentTimeMillis();
					long actualDelay = randomDelay;
					if (lastDeliveryTime != -1) // normalize time according to previous message
						actualDelay = Math.max(0, currentTime+randomDelay-lastDeliveryTime);
					c.setLastDeliveryTime(actualDelay+currentTime);
					try {
						c.getQueue().putLast(new ControllerWaitingThread(actualDelay+currentTime, actualDelay, message));
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.exit(1);
					}


				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		} catch (IOException e) {
			System.err.println("Could not listen on port " + CONTROLLER_PORT);
			System.exit(-1);
		}

	}
}