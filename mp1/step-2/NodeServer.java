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
	private long sequenceNumber = 0;

	private long queuedTimestamp = -1;
	private int queuedValue = 0;

	private PriorityQueue<Message> pq = new PriorityQueue<Message>(2, new MessageComparator());

	public NodeServer(int n, int p, int d, boolean debug) {
		nodeNum = n;
		port = p;
		maxDelay = d;
		DEBUG = debug;
	}

	public void deliver(Message msgObject) {
		String msg = msgObject.message;
		// int fromNode = msgObject.fromNode;
		if (DEBUG) {
			Time curTime = new Time(System.currentTimeMillis());
			System.out.println("Recevied \"" + msg + "\" from Node " + msgObject.fromNode +" , system time is " + curTime.toString() );
		}

		Node.lock.lock();

		

		String[] cmds = msg.split(" ");
		if (msgObject.model == 1 || msgObject.model == 2) {

			if (cmds[0].equals("get")) {

				int read = Integer.parseInt(cmds[1]);
				int res;
				if (!Node.store.containsKey(read)) {
					res = 0;
				}
				else 
					res = Node.store.get(read);

				if (msgObject.fromNode==nodeNum) {
					
					System.out.println("Read("+read+")="+res);
					Node.lastResponseTime = System.currentTimeMillis();
					Node.waitingForResponse = false;
					Node.shouldProceed.signal();
					
				}
			}
			else if (cmds[0].equals("insert")) {
				int key = Integer.parseInt(cmds[1]);
				int value = Integer.parseInt(cmds[2]);
				Node.store.put(key, value);

				if (msgObject.fromNode==nodeNum) {
					
					System.out.println("Ack Insert("+key+","+value+")");
					Node.lastResponseTime = System.currentTimeMillis();
					Node.waitingForResponse = false;
					Node.shouldProceed.signal();
					
				}
			}
			else if (cmds[0].equals("update")) {
				int key = Integer.parseInt(cmds[1]);
				int value = Integer.parseInt(cmds[2]);
				Node.store.put(key, value);

				if (msgObject.fromNode==nodeNum) {
					
					System.out.println("Ack Update("+key+","+value+")");
					Node.lastResponseTime = System.currentTimeMillis();
					Node.waitingForResponse = false;
					Node.shouldProceed.signal();
					
				}
			}
			
		} else if (msgObject.model == 3 || msgObject.model == 4) {
			if (cmds[0].equals("ack")) {
				long t = Long.parseLong(cmds[1]);
				
				if (t == Node.ackTimestamp) {
					Node.currentResponded++;
					if (Node.responseType.equals("get")) {
						int value = Integer.parseInt(cmds[2]);
						long tm = Long.parseLong(cmds[3]);
						if (tm > queuedTimestamp) {
							queuedTimestamp = tm;
							queuedValue = value;
						}
					}
						
					if (Node.currentResponded == Node.needResponded) {

						String output = "Should not see this.";
						if (Node.responseType.equals("get"))
							output = "Read("+Node.requestedKey+")="+queuedValue;
						else if (Node.responseType.equals("insert"))
							output = "Ack Insert("+Node.requestedKey+"," + Node.requestedValue + ")";
						else if (Node.responseType.equals("update"))
							output = "Ack Update("+Node.requestedKey+"," + Node.requestedValue + ")";

						
						System.out.println(output);
						Node.waitingForResponse = false;
						Node.currentResponded = 0;
						Node.needResponded = 0;
						Node.ackTimestamp = -1;
						Node.shouldProceed.signal();
						
					}

				}
				
				
			} else {
				int fromNode = msgObject.fromNode;
				String response = "";
				if (cmds[0].equals("get")) {
					int key = Integer.parseInt(cmds[1]);
					int value;
					long tm;
					if (Node.store.containsKey(key)) {
						value = Node.store.get(key);
						tm = Node.timestamps.get(Integer.parseInt(cmds[1]));
					}
					else {
						value = 0;
						tm = 0;
					}
					response = "ack "+msgObject.dataField+" "+value+" "+tm;
				} else if (cmds[0].equals("insert") || cmds[0].equals("update")) {
					Node.store.put(Integer.parseInt(cmds[1]), Integer.parseInt(cmds[2]));
					Node.timestamps.put(Integer.parseInt(cmds[1]), System.currentTimeMillis());
					response = "ack "+msgObject.dataField;
				}

				try (
					Socket socket = new Socket("127.0.0.1", Node.CONTROLLER_PORT);
					PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				) {
					
					out.println(nodeNum+";"+fromNode+";"+response+";"+msgObject.model);
					socket.close();


				} catch (UnknownHostException e) {
					System.err.println("Unknown Host");
				} catch (IOException e) {
					System.err.println("Controller Connection Failure. Command Ignored.");
					// Simply ignore the user command 
				} 
			}
		}
		

		

		Node.lock.unlock();
		

	}

	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(port)) { 
			System.out.println("Node "+nodeNum+" server listens at port "+port);

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
					int model = Integer.parseInt(conts[3]);

					if (model == 1 || model == 2) {
						// Linear. or Seq-Con.

						Message msg = new Message(conts[2], Long.parseLong(conts[4]), Integer.parseInt(conts[0]), model);
						if (sequenceNumber + 1 == msg.dataField) {
							if (DEBUG) System.out.println("Deliver Message: " + msg.message);
							deliver(msg);
							sequenceNumber++;
							while (pq.peek() != null && pq.peek().dataField == 1+sequenceNumber) {
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
					} else if (model == 3 || model == 4) {
						// Eventual consistency

						Message msg = new Message(conts[2], Long.parseLong(conts[4]),Integer.parseInt(conts[0]),model);
						deliver(msg);

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
