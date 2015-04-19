import java.util.*;
import java.net.*;
import java.io.*;



public class ChordNode extends Thread{

	public int id;
	public int port;
	public ServerSocket serverSocket;

	public Map<Integer, ChordNodeFinger> fingers = new HashMap<Integer, ChordNodeFinger>();
	public ChordNode successor() {
		return fingers.get(1).node;
	}
	public ChordNode predecessor;
	public Set<Integer> keys = new TreeSet<Integer>();

	public int getPort() {
		return port;
	}
	public int get_Id(){
		return id;
	}

	public ChordNode(int id, int port) {
		this.port = port;
		this.id = id;
		// Allocate finger table, which is 1-indexed
		for (int i=1;i<=8;i++)
			fingers.put(i, new ChordNodeFinger(id, i));
	}

	public void run() {

		try (ServerSocket serverSocket = new ServerSocket(port)) 
		{ 
			while (true) {
				Socket s = serverSocket.accept();
				ChordNodeAction t = new ChordNodeAction(s, this);
				t.start();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	// public void printDetails() {
	// 	System.out.println("P:"+predecessor.identifier+", S:"+successor().identifier);

	// 	System.out.print("Finger: ");
	// 	for (int i=1; i<=8; i++) {
	// 		System.out.print(fingers.get(i).node.identifier+ " ");
	// 	}
	// 	System.out.println("");
	// }

	// public void printKeys() {
	// 	System.out.print(identifier);
	// 	for (int key : keys) {
	// 		System.out.print(" " + key);
	// 	}
	// 	System.out.println();
	// }

}