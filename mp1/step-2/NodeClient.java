import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.sql.*;

public class NodeClient extends Thread {

	private boolean DEBUG = true;
	private int controllerPort;
	private int nodeNum;
	private BlockingDeque<String> q = new LinkedBlockingDeque<String>();

	public NodeClient(int c, int i, boolean de) {
		controllerPort = c;
		nodeNum = i;
		DEBUG = de;
	}

	public void enqueue(String msg) {
		try {
			q.putLast(msg);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		} 
	}

	public void run() {

		System.out.println("Node "+nodeNum+" client ready for commands.");

		while (true) {
			
			// Channel constantly tries to dequeue and apply waiting
			try {

				
				Node.lock.lock();
				while (Node.waitingForResponse) {
					Node.shouldProceed.await();
				}
				Node.lock.unlock();

				
				String message = q.takeFirst(); // this call blocks if queue is empty


				

				Node.lock.lock();
				String[] cmds = message.split(" ");

				if (cmds[0].equals("delay")) {
					float t = Float.parseFloat(cmds[1]);
					long d = (long) (t * 1000);

					long offs = Node.lastResponseTime + d - System.currentTimeMillis();
					if (offs > 0) {
						if (DEBUG) System.out.println("Actual delay = "+offs);
						Thread.sleep(offs);
					} else {
						if (DEBUG) System.out.println("Actual delay = 0");
					}
				}

				else if (cmds[0].equals("show-all")) {
					StringBuilder sb = new StringBuilder();
					Node.storeLock.lock();
					for (Map.Entry<Integer, Integer> e: Node.store.entrySet()) {
						sb.append("<");
						sb.append(e.getKey());
						sb.append(": ");
						sb.append(e.getValue());
						sb.append(">\n");
					}
					Node.storeLock.unlock();
					if (sb.length() == 0)
						System.out.println("<>");
					else
						System.out.print(sb.toString());
				}

				else if (!cmds[0].equals("get") && 
					!cmds[0].equals("insert") && 
					!cmds[0].equals("update") && 
					!cmds[0].equals("delete") &&
					!cmds[0].equals("search")) {
					System.out.println("Unknown user command!");
				}

				else if (cmds[0].equals("delete") && cmds.length != 2) {
					System.out.println("Invalid command.");
				}

				else if (cmds[0].equals("get") && cmds.length != 3) {
					System.out.println("Invalid command.");
				}

				else if (( cmds[0].equals("insert") || cmds[0].equals("update") ) && cmds.length != 4) {
					System.out.println("Invalid command.");
				}

				else if (cmds[0].equals("get") && cmds[2].equals("2")) {
					// Special case: Sequential-consistency Read
					Node.storeLock.lock();
					int read = Integer.parseInt(cmds[1]);
					int res;
					if (!Node.store.containsKey(read)) {
						res = 0;
					}
					else
						res = Node.store.get(read);
					Node.storeLock.unlock();
					System.out.println("Read("+read+")="+res);
				}
				
				else {

					// Normal case
				
					int model;
				
					if (cmds[0].equals("delete"))
						model = 1;
					else if (cmds[0].equals("search"))
						model = 5;
					else if (cmds[0].equals("get"))
						model = Integer.parseInt(cmds[2]);
					else 
						model = Integer.parseInt(cmds[3]);

					if (model == 3) {
						Node.needResponded = 1;
					} else if (model == 4) {
						Node.needResponded = 2;
					} else if (model == 5) {
						Node.needResponded = 4;
					}

					if (model == 3 || model == 4) {
						Node.ackTimestamp = System.currentTimeMillis();
						if (cmds[0].equals("get"))
							Node.requestedKey = Integer.parseInt(cmds[1]);
						else if (cmds[0].equals("insert") || cmds[0].equals("update")) {
							Node.requestedKey = Integer.parseInt(cmds[1]);
							Node.requestedValue = Integer.parseInt(cmds[2]);
						}
						Node.responseType = cmds[0];
					}
					else if (model == 5) {
						Node.ackTimestamp = System.currentTimeMillis();
						Node.requestedKey = Integer.parseInt(cmds[1]);
						Node.responseType = cmds[0];
					}
					Node.waitingForResponse = true;
					
					try (
						Socket socket = new Socket("127.0.0.1", controllerPort);
						PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
					) {
						String output = nodeNum+";-1;"+message+";"+model;
						if (model == 3 || model == 4 || model == 5)
							output += (";"+Node.ackTimestamp);
						out.println(output);
						socket.close();

						if (DEBUG) {
							Time curTime = new Time(System.currentTimeMillis());
							System.out.println("Broadcast \"" + message + "\", system time is " + curTime.toString() );
						}
						
					} catch (UnknownHostException e) {
						System.err.println("Unknown Host");
					} catch (IOException e) {
						System.err.println("Controller Connection Failure. Command Ignored.");
						// Simply ignore the user command 
					} 
					
				}

				Node.lock.unlock();
				


			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			} 
		}
	}
}
