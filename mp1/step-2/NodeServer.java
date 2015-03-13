import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.sql.*;

public class NodeServer extends Thread {

	private boolean DEBUG = true;

	private int nodeNum;
	private int port;
	private int maxDelay;
	private int sequenceNumber = 0;

	public HashMap<Integer, Integer> store = new HashMap<Integer, Integer>();

	private PriorityQueue<Message> pq = null;

	public NodeServer(int n, int p, int d, boolean debug) {
		nodeNum = n;
		port = p;
		maxDelay = d;
		pq = new PriorityQueue<Message>(5, new MessageComparator());
		DEBUG = debug;
	}

	public void deliver(Message msgObject) {
		String msg = msgObject.message;
		int fromNode = msgObject.fromNode;
		Time curTime = new Time(System.currentTimeMillis());
		System.out.println("Recevied \"" + msg + "\" from Node " + fromNode +" , system time is " + curTime.toString() );
	}

	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(port)) { 
			System.out.println("Node "+nodeNum+" listens at port "+port);

			while (true) {
				Socket socket = serverSocket.accept();

				// Controller accpets node connection sequentially
				try (
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				) {
					String message = in.readLine();
					if (DEBUG) System.out.println(message);
					socket.close();
					String[] conts = message.split(";");
					Message msg = new Message(conts[2], Integer.parseInt(conts[3]), Integer.parseInt(conts[0]));

					if (msg.sequence == -1) {
						// Non-total-ordered message
						// Deliver immediately 
						deliver(msg);
						continue;
					}

					if (sequenceNumber + 1 == msg.sequence) {
						if (DEBUG) System.out.println("Deliver Message: " + msg.message);
						deliver(msg);
						sequenceNumber++;
						while (pq.peek() != null && pq.peek().sequence == 1+sequenceNumber) {
							Message next = pq.poll();
							if (DEBUG) System.out.println("Dequeue/Deliver Message: " + next.message);
							deliver(next);
							sequenceNumber++;
						}
					}
					else {
						if (DEBUG) System.out.println("Delay Message: " + msg.message);
						pq.add(msg);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		} catch (IOException e) {
			System.err.println("Could not listen on port " + port);
			System.exit(-1);
		}
	}
}
