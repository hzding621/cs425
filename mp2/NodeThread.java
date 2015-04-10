import java.io.*;
import java.util.*;

class NodeFinger {
	public int id;
	public int start;
	public NodeThread node;
	public NodeFinger(int id, int k) {
		this.id = id;
		this.start = (int)(id + Math.pow(2, k-1)) % 256;
	}
}

public class NodeThread extends Thread {

	public List<NodeFinger> fingers = new List<NodeFinger>();
	public int identifier;
	public NodeThread successor;
	public NodeThread predecessor;
	public List<Integer> keys = new ArrayList<Integer>();

	boolean within(int n, int a, int b, int leftOpen, int rightOpen) {
		b = b - rightOpen;
		a = a + leftOpen;
		if (a <= b)
			return n >= a && n <= b;
		else {
			return n >= a || n <= b;
		}
	}

	public NodeThread(int id, NodeThread someThread) {
		identifier = id;
		for (int i=1;i<=8;i++)
			fingers.add(new NodeFinger(identifier, i));
		join(someThread);
	}

	public void join(NodeThread someThread) {

		if (someThread != null) {
			initFingerTable(someThread);
			updateOthers();
		} else {
			for (int i=1; i<=8; i++) {
				fingers.get(i).node = this;
			}
		}
	}

	public void initFingerTable(NodeThread someThread) {

		fingers.get(1).node = successor = someThread.findSuccessor(finger.get(1).start);
		predecessor = successor.predecessor;
		successor.predecessor = this;
		for (int i=1; i<8; i++) {
			if (within(fingers.get(i+1).start, identifier, fingers.get(i).node.identifier, 0, 1)) {
				fingers.get(i+1).node = fingers.get(i).node;
			}
			else {
				fingers.get(i+1).node = someThread.findSuccessor(fingers.get(i+1).start);
			}
		}
	}
 	
 	public void updateOthers() {
 		for (int i=1; i<=8; i++) {
 			NodeThread p = findPredecessor(identifier - (int)(Math.pow(2, i-1)));
 			p.updateFingerTable(this, );
 		}
 	}

 	public void updateFingerTable(NodeThread s, int i) {
 		if (within(s.identifier, identifier, fingers.get(i).node.identifier, 0, 1)) {
 			fingers.get(i).node = s;
 			NodeThread p = predecessor;
 			p.updateFingerTable(s, i);
 		}
 	}

 	public NodeThread findSuccessor(int id) {
 		NodeThread m = findPredecessor(id);
 		return m.successor;
 	}

 	public NodeThread findPredecessor(int id) {
 		NodeThread m = this;
 		while (!within(id, m.identifier, m.successor.identifier, 1, 0)) {
 			m = m.closestPrecedingFinger(id);
 		}
 	}

 	public NodeThread closestPrecedingFinger(int id) {
 		for (int i=m; i>=1; i--) {
 			if (within(fingers.get(i).node.identifier), identifier, id, 1, 1) {
 				return fingers.get(i).node;
 			}
 		}
 		return this;
 	}
}