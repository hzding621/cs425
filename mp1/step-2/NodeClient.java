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
				try {
					while (Node.waitingForResponse) {
						Node.shouldProceed.await();
					}
				} finally {
					Node.lock.unlock();
				}
				
				String message = q.takeFirst(); // this call blocks if queue is empty
				String[] cmds = message.split(" ");

				if (cmds[0].equals("delay")) {
					float t = Float.parseFloat(cmds[1]);
					int d = (int) (t * 1000);
					sleep(d);
					continue;
				}

				if (!cmds[0].equals("get") &&
					!cmds[0].equals("insert") &&
					!cmds[0].equals("update")) {
					System.out.println("Unknown user command!");
					continue;
				}

				if (cmds[0].equals("get") && cmds.length != 3) {
					System.out.println("Invalid get command.");
					continue;
				}

				if (( cmds[0].equals("insert") || cmds[0].equals("update") ) && cmds.length != 4) {
					System.out.println("Invalid insert command.");
					continue;
				}

				if (cmds[0].equals("get") && !cmds[2].equals("1") && !cmds[2].equals("2") ) {
					System.out.println("Model "+cmds[2]+" Not Supported Yet.");
					continue;
				}
				if ( (cmds[0].equals("insert") || cmds[0].equals("update") ) && !cmds[3].equals("1") && !cmds[3].equals("2") ) {
					System.out.println("Model "+cmds[3]+" Not Supported Yet.");
					continue;
				}
				

				if (cmds[0].equals("get") && cmds[2].equals("2")) {
					// Special case: Sequential-consistency Read
					int read = Integer.parseInt(cmds[1]);
					int res;
					if (!Node.store.containsKey(read)) {
						res = 0;
					}
					else
						res = Node.store.get(read);
					System.out.println("Read("+read+")="+res);
					continue;
				}

				try (
					Socket socket = new Socket("127.0.0.1", controllerPort);
					PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				) {
					Node.waitingForResponse = true;
					out.println(message+";"+nodeNum);
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


			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			} 
		}
	}
}
