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

	public Map<Integer, NodeFinger> fingers = new HashMap<Integer, NodeFinger>();
	public int identifier;
	public NodeThread successor() {
		return fingers.get(1).node;
	}
	public NodeThread predecessor;
	public Set<Integer> keys = new TreeSet<Integer>();

	public void printDetails() {
		System.out.println("P:"+predecessor.identifier+", S:"+successor().identifier);

		System.out.print("Finger: ");
		for (int i=1; i<=8; i++) {
			System.out.print(fingers.get(i).node.identifier+ " ");
		}
		System.out.println("");
	}

	public void printKeys() {
		System.out.print(identifier);
		for (int key : keys) {
			System.out.print(" " + key);
		}
		System.out.println();
	}

	boolean within(int n, int a, int b, int leftOpen, int rightOpen) {

		b = b - rightOpen;
		b = b == -1 ? 255 : b;
		a = a + leftOpen;
		a = a == 256 ? 0 : a;
		if (a <= b)
			return n >= a && n <= b;
		else {
			return n >= a || n <= b;
		}
	}

	public NodeThread(int id, NodeThread someThread) {
		identifier = id;

		// Allocate finger table, which is 1-indexed
		for (int i=1;i<=8;i++)
			fingers.put(i, new NodeFinger(identifier, i));
		join(someThread);
	}

	public void join(NodeThread someThread) {

		if (someThread != null) {
			initFingerTable(someThread);
			updateOthers();

			HashSet<Integer> transferSet = new HashSet<Integer>();

			// TODO
			for (int k : successor().keys) {
				if (within(k, predecessor.identifier, identifier, 1, 0))
					transferSet.add(k);
			}
			for (int k : transferSet) {
				successor().keys.remove(k);
				keys.add(k);
			}
		} else {
			for (int i=1; i<=8; i++) {
				fingers.get(i).node = this;
			}
			predecessor = this;
			for (int key=0; key<=255; key++) {
				keys.add(key);
			}
		}
	}



	public void initFingerTable(NodeThread someThread) {

		// System.out.println(fingers.get(1).start);

		// TODO
		fingers.get(1).node = someThread.findSuccessor(fingers.get(1).start);
		// TODO
		predecessor = successor().predecessor;
		// TODO
		successor().predecessor = this;
		for (int i=1; i<8; i++) {
			if (within(fingers.get(i+1).start, identifier, fingers.get(i).node.identifier, 0, 1)) {
				fingers.get(i+1).node = fingers.get(i).node;
			}
			else {
				// TODO
				fingers.get(i+1).node = someThread.findSuccessor(fingers.get(i+1).start);
				if (!within(fingers.get(i+1).node.identifier, fingers.get(i+1).start, identifier, 0, 0))
					fingers.get(i+1).node = this;
			}
		}
	}
 	
 	public void updateOthers() {
 		for (int i=1; i<=8; i++) {
 			NodeThread p = findPredecessor( ( 1 + 256 + identifier - (int)(Math.pow(2, i-1)) ) % 256);
 			// TODO
 			p.updateFingerTable(this, i);
 		}
 	}

 	public void updateFingerTable(NodeThread s, int i) {
 		if (s.identifier == identifier)
 			return;

 		if (within(s.identifier, identifier, fingers.get(i).node.identifier, 1, 0)) {
 			fingers.get(i).node = s;
 			NodeThread p = predecessor;
 			// TODO
 			p.updateFingerTable(s, i);
 		}
 	}

 	public NodeThread findSuccessor(int id) {
 		NodeThread m = findPredecessor(id);
 		return m.successor();
 	}

 	public NodeThread findPredecessor(int id) {
 		NodeThread m = this;
 		while (!within(id, m.identifier, m.successor().identifier, 1, 0)) {
 			// TODO
 			m = m.closestPrecedingFinger(id);
 		}
 		return m;
 	}

 	public NodeThread closestPrecedingFinger(int id) {
 		for (int i=8; i>=1; i--) {
 			if (within(fingers.get(i).node.identifier, identifier, id, 1, 1)) {
 				return fingers.get(i).node;
 			}
 		}
 		return this;
 	}

 	public boolean leave() {
 		if (this == successor())
 			return false;

 		// move all my keys to successor()
 		for (int k : keys) {
 			// TODO
 			successor().keys.add(k);
 		}

 		successor().predecessor = predecessor;
 		for (int i=1; i<=8; i++) {
 			NodeThread p = findPredecessor(( 1 + 256 + identifier - (int)(Math.pow(2, i-1)) ) % 256);
 			// TODO
 			p.remove_node(this, i, successor());
 		}
 		return true;
 	}

 	public void remove_node(NodeThread n, int i, NodeThread repl) {
 		if (fingers.get(i).node == n ) {
 			fingers.get(i).node = repl;
 			// TODO
 			predecessor.remove_node(n, i, repl);
 		}
 	}

}
