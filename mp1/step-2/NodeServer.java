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
		String[] cmds = msg.split(" ");
		if (cmds[0].equals("get")) {

			int read = Integer.parseInt(cmds[1]);
			int res;
			if (!Node.store.containsKey(read)) {
				res = 0;
			}
			else 
				res = Node.store.get(read);

			if (msgObject.fromNode==nodeNum) {
				Node.lock.lock();
				try {
					System.out.println("Read("+read+")="+res);
					Node.lastResponseTime = System.currentTimeMillis();
					Node.waitingForResponse = false;
					Node.shouldProceed.signal();
				}
				finally {
					Node.lock.unlock();
				}
			}
		}
		else if (cmds[0].equals("insert")) {
			int key = Integer.parseInt(cmds[1]);
			int value = Integer.parseInt(cmds[2]);
			Node.store.put(key, value);

			if (msgObject.fromNode==nodeNum) {
				Node.lock.lock();
				try {
					System.out.println("Ack Insert("+key+","+value+")");
					Node.lastResponseTime = System.currentTimeMillis();
					Node.waitingForResponse = false;
					Node.shouldProceed.signal();
				}
				finally {
					Node.lock.unlock();
				}
			}
		}
		else if (cmds[0].equals("update")) {
			int key = Integer.parseInt(cmds[1]);
			int value = Integer.parseInt(cmds[2]);
			Node.store.put(key, value);

			if (msgObject.fromNode==nodeNum) {
				Node.lock.lock();
				try {
					System.out.println("Ack Update("+key+","+value+")");
					Node.lastResponseTime = System.currentTimeMillis();
					Node.waitingForResponse = false;
					Node.shouldProceed.signal();
				}
				finally {
					Node.lock.unlock();
				}
			}
		}
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
