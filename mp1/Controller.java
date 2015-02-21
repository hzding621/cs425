import java.io.*;
import java.net.*;
import java.util.*;

public class Controller {

	private static String config = "config.txt";
	private static int CONTROLLER_PORT = 8888;
	private static int NODE_NUM = 4;

	private static Random r = new Random();
	public static ArrayList<ArrayList<ControllerChannel>> channels = null;

	public static HashMap<Integer, Integer> ports = null;
	public static HashMap<Integer, Integer> delays = null;

	public static long getRandomDelay(int toNode) {
		return r.nextLong() % ( delays.get(toNode) / 2) + delays.get(toNode) / 2;
	}

	public static ControllerChannel getChannel(int fromNode, int toNode) {
		return channels.get(fromNode).get(toNode);
	}

	public static void deliverMessage(int toNode, String message) {
		// Initiate client and conenct to toNode
		try (
			Socket socket = new Socket("127.0.0.1", ports.get(toNode));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		) {
			out.println(message);
		} catch (UnknownHostException e) {
            System.err.println("Unknown Host");
        } catch (IOException e) {
            System.err.println("Node connection failure. Message dumped.");
            // Simply dump the message
        }
	}

	public static void main(String[] args) {
		
		if (args.length != 1) {
			System.err.println(
				"Usage: java CentralServer [config file]");
			System.exit(1);
		}

		// Read parameters from config file
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
				if (params[0].equals("NODE_NUM"))
					NODE_NUM = Integer.parseInt(params[1]);
				if (params[0].equals("MAX_DELAY")) {
					String[] p = params[1].split(",");
					for (int i=0; i<p.length; i++) {
						delays.put(i, Integer.parseInt(p[i]));
					}
				}
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


		// Initialize the channel threads
		channels = new ArrayList<ArrayList<ControllerChannel>>();
		for (int i=0; i<NODE_NUM; i++) {
			channels.add(new ArrayList<ControllerChannel>());
			for (int j=0; j<NODE_NUM; j++) {

				// Also create self-communication channels
				ControllerChannel t = new ControllerChannel(i, j);
				channels.get(i).add(t);
				t.start();
				
			}
		}
		
		// Listens to Node connections and enqueue messages
		try (ServerSocket serverSocket = new ServerSocket(CONTROLLER_PORT)) { 
			System.out.println("Controller listens at port " + CONTROLLER_PORT);
			while (true) {
				Socket socket = serverSocket.accept();

				// Controller accpets node connection sequentially
				try (
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				) {
					String message = in.readLine();
					socket.close();
					String[] messageLines = message.split(";");
					int fromNode = Integer.parseInt(messageLines[0]);
					int toNode = Integer.parseInt(messageLines[1]);
					getChannel(fromNode, toNode).enqueueMessage(message);

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