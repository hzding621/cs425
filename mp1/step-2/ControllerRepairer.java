import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.sql.*;

class ControllerRepairerThread extends Thread {

	ControllerRepairer ref = null;
	int port;
	int nodeNum;

	public ControllerRepairerThread(ControllerRepairer r, int p, int i) {
		ref = r;
		port = p;
		nodeNum = i;
	}
	public void run() {

		try (
			Socket socket = new Socket("127.0.0.1", port);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		) {
			out.println("repair");
			String line = in.readLine();
			while (!line.equals("END")) {
				String[] tok = line.split(" ");
				int key = Integer.parseInt(tok[0]);
				int value = Integer.parseInt(tok[1]);
				long tm = Long.parseLong(tok[2]);
				ref.lock.lock();
				if (!ref.timestamps.containsKey(key) || ref.timestamps.get(key) < tm) {
					ref.timestamps.put(key, tm);
					ref.values.put(key, value);
				}
				ref.lock.unlock();
				line = in.readLine();
			}
			ref.lock.lock();
			System.out.println("Finish collecting from node "+nodeNum);
			ref.done++;
			// ref.condition.signal();

			try {
				while (ref.done != ref.ports.size()) {
					ref.condition.await();
				}
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			} 

			ref.condition.signal();
			ref.lock.unlock();

			// System.out.println("Begin updating to node "+nodeNum);

			// Simulate channel delay
			try {
				sleep(2*Controller.getRandomDelay(5, nodeNum));
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			} 

			for (int key: ref.timestamps.keySet()) {
				int value = ref.values.get(key);
				long tm = ref.timestamps.get(key);
				out.println(key+" "+value+" "+tm);
			}
			out.println("END");

			System.out.println("Finish updating to node "+nodeNum);
				
			socket.close();
		}
		catch (UnknownHostException e) {
			System.err.println("Unknown Host");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Node Connection Failure. ");
			System.exit(1);
		} 
	}
}

public class ControllerRepairer extends Thread {


	public final Lock lock = new ReentrantLock();
	public final Condition condition = lock.newCondition();
	public int done = 0;
	public List<Integer> ports = null;
	public List<ControllerRepairerThread> threads = new ArrayList<ControllerRepairerThread>();
	public HashMap<Integer, Integer> values = null;
	public HashMap<Integer, Long> timestamps = null;
	private int interval = 30;

	public ControllerRepairer(List<Integer> p, int in) {
		ports = p;
		interval = in;

	}

	public void run() {

		while (true) {

			try {

				sleep(interval*1000);
				System.out.println("Start repairing...");
				done = 0;
				values = new HashMap<Integer, Integer>();
				timestamps = new HashMap<Integer, Long>();
				threads = new ArrayList<ControllerRepairerThread>();
				for (int i=0; i<ports.size(); i++) {
					int p = ports.get(i);
					ControllerRepairerThread t = new ControllerRepairerThread(this, p, i); 
					threads.add(t);
					t.start();
				}
				for (int i=0; i<ports.size(); i++)
					threads.get(i).join();
				System.out.println("Finish repairing...");
				
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			} 
		}

	}

}