import java.io.*;
import java.util.*;
import java.net.*;


public class ChordNodeAction extends Thread {

	ChordNode parent;
	Socket socket;
	Map<Integer, ChordNodeFinger> fingers;
	String[] messageLine;


	public ChordNodeAction(Socket s, ChordNode parent, String[] messageLine) {
		this.parent=parent;
		this.fingers=parent.fingers;
		this.socket=s;
		this.messageLine=messageLine;
	}

	public void run() {

		try (
			// BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		) 
		{
			if (messageLine[0].equals("000")) {
				// JOIN 
				int id = Integer.parseInt(messageLine[1]);
				if (id == 0) {
					join(null);
				} else {
					join(Coordinator.getNode(0));
				}
				out.println("0");
			} else if (messageLine[0].equals("001")) {
				// FIND_SUCCESSOR
				int id = Integer.parseInt(messageLine[1]);
				ChordNode ret = findSuccessor(id);
				out.println(ret.get_Id());
			} else if (messageLine[0].equals("002")) {
				// GET_PREDECESSOR
				ChordNode ret = parent.predecessor;
				out.println(ret.get_Id());
			} else if (messageLine[0].equals("003")) {
				// SET_PREDECESSOR
				int id = Integer.parseInt(messageLine[1]);
				ChordNode newPredecessor = Coordinator.getNode(id);
				parent.predecessor = newPredecessor;
				out.println("0");
			} else if (messageLine[0].equals("004")) {
				// UPDATE_FINGER_TABLE
				int ss = Integer.parseInt(messageLine[1]);
				ChordNode s = Coordinator.getNode(ss);
				int i = Integer.parseInt(messageLine[2]);
				updateFingerTable(s, i);
				out.println("0");
			} else if (messageLine[0].equals("005")) {
				// CLOSEST_PRECEDING_FINGER
				int id = Integer.parseInt(messageLine[1]);
				ChordNode ret = closestPrecedingFinger(id);
				out.println(ret.get_Id());
			} else if (messageLine[0].equals("006")) {
				// DELETE_KEYS
				int p1 = Integer.parseInt(messageLine[1]);
				int p2 = Integer.parseInt(messageLine[2]);
				HashSet<Integer> ret = deleteKeys(p1, p2);
				StringBuilder sb = new StringBuilder();
				for (int k: ret) 
					sb.append(k+"#");
				sb.deleteCharAt(sb.length()-1);
				out.println(sb.toString());
			} else if (messageLine[0].equals("007")) {
				// ADD_KEYS
				String[] lineOfKeys = messageLine[1].split("#");
				HashSet<Integer> p = new HashSet<Integer>();
				for (int i=0; i<lineOfKeys.length; i++) {
					p.add(Integer.parseInt(lineOfKeys[i]));
				}
				addKeys(p);
				out.println("0");
			} else if (messageLine[0].equals("008")) {
				// REMOVE_NODE
				ChordNode p1 = Coordinator.getNode(Integer.parseInt(messageLine[1]));
				int p2 = Integer.parseInt(messageLine[2]);
				ChordNode p3 = Coordinator.getNode(Integer.parseInt(messageLine[3]));
				remove_node(p1, p2, p3);
				out.println("0");
			} else if (messageLine[0].equals("009")) {
				// GET_SUCCESSOR
				int n = parent.successor().get_Id();
				out.println(n);
			} else if (messageLine[0].equals("010")) {
				// SHOW MY KEY
				Coordinator.getOutputStream().print(parent.get_Id());
				for (int k: parent.keys) {
					Coordinator.getOutputStream().print(" "+k);
				}
				Coordinator.getOutputStream().println();
				out.println("0");
			} else if (messageLine[0].equals("011")) {
				// LEAVE 
				leave();
				out.println("0");
			} else if (messageLine[0].equals("012")) {
				int n = locate(Integer.parseInt(messageLine[1]));
				out.println(n);
			}


			// socket.close();

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	String communicate(int targetId, String message) {
		int port = Coordinator.getNode(targetId).getPort();
		String result = null;
		try (
			Socket socket = new Socket("127.0.0.1", port);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		) {
			out.println(message);
			result = in.readLine();
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return result;
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

	public HashSet<Integer> deleteKeys(int lowerOpen, int upperClose) {
		HashSet<Integer> transferSet = new HashSet<Integer>();
		for (int k : parent.keys) {
			if (within(k, lowerOpen, upperClose, 1, 0))
				transferSet.add(k);
		}
		for (int k : transferSet)
			parent.keys.remove(k);
		return transferSet;
	}

	public void join(ChordNode someThread) {

		if (someThread != null) {
			initFingerTable(someThread);
			updateOthers();

			// HashSet<Integer> transferSet = new HashSet<Integer>();

			// // DONE
			// for (int k : successor().keys) {
			// 	if (within(k, predecessor.get_Id(), this.parent.get_Id(), 1, 0))
			// 		transferSet.add(k);
			// }
			// for (int k : transferSet) {
			// 	successor().keys.remove(k);
			// 	keys.add(k);
			// }
			String lineOfKeys = communicate(parent.successor().get_Id(), "006,"+parent.predecessor.get_Id()+","+parent.get_Id());
			String[] newKeys = lineOfKeys.split("#");
			for (int i=0; i<newKeys.length; i++) {
				int newKey = Integer.parseInt(newKeys[i]);
				parent.keys.add(newKey);
			}

		} else {
			for (int i=1; i<=8; i++) {
				fingers.get(i).node = parent;
			}
			parent.predecessor = parent;
			for (int key=0; key<=255; key++) {
				parent.keys.add(key);
			}
		}
	}



	public void initFingerTable(ChordNode someThread) {

		// DONE
		// fingers.get(1).node = someThread.findSuccessor(fingers.get(1).start);
		String res;
		res = communicate(someThread.get_Id(), "001,"+fingers.get(1).start);
		int ret = Integer.parseInt(res);
		fingers.get(1).node = Coordinator.getNode(ret);

		// DONE
		// predecessor = successor().predecessor;
		res = communicate(parent.successor().get_Id(), "002");
		int ret2 = Integer.parseInt(res);
		parent.predecessor = Coordinator.getNode(ret2);

		// DONE
		// successor().predecessor = this;
		communicate(parent.successor().get_Id(), "003,"+parent.get_Id());

		for (int i=1; i<8; i++) {
			if (within(fingers.get(i+1).start, parent.get_Id(), fingers.get(i).node.get_Id(), 0, 1)) {
				fingers.get(i+1).node = fingers.get(i).node;
			}
			else {
				// TODO
				// fingers.get(i+1).node = someThread.findSuccessor(fingers.get(i+1).start);
				res = communicate(someThread.get_Id(), "001,"+fingers.get(i+1).start);
				int ret3 = Integer.parseInt(res);
				fingers.get(i+1).node = Coordinator.getNode(ret3);
				if (!within(fingers.get(i+1).node.get_Id(), fingers.get(i+1).start, parent.get_Id(), 0, 0))
					fingers.get(i+1).node = parent;
			}
		}
	}
 	
 	public void updateOthers() {
 		for (int i=1; i<=8; i++) {
 			ChordNode p = findPredecessor( ( 1 + 256 + parent.get_Id() - (int)(Math.pow(2, i-1)) ) % 256);
 			// DONE
 			// p.updateFingerTable(this, i);
 			if (p != parent)
				communicate(p.get_Id(), "004,"+parent.get_Id()+","+i);
			else 
				updateFingerTable(parent, i);
 		}
 	}

 	public void updateFingerTable(ChordNode s, int i) {
 		if (s.get_Id() == parent.get_Id())
 			return;

 		if (within(s.get_Id(), parent.get_Id(), fingers.get(i).node.get_Id(), 1, 0)) {
 			fingers.get(i).node = s;
 			ChordNode p = parent.predecessor;
 			// DONE
 			// p.updateFingerTable(s, i);
 			if (p != parent)
	 			communicate(p.get_Id(), "004,"+s.get_Id()+","+i);
	 		else 
	 			updateFingerTable(s, i);
 		}
 	}

 	public int locate(int id) {
 		ChordNode n = findPredecessor(id);
 		String res = communicate(n.get_Id(), "009");
 		return Integer.parseInt(res);
 	}

 	public ChordNode findSuccessor(int id) {
 		ChordNode m = findPredecessor(id);
 		ChordNode ret;
 		if (m == parent) {
 			ret = parent.successor();
 		}
 		else {
 			String res = communicate(m.get_Id(), "009");
 			ret = Coordinator.getNode(Integer.parseInt(res));
 		}
 		return ret;
 	}

 	public ChordNode findPredecessor(int id) {
 		ChordNode m = parent;
 		ChordNode m_suc = parent.successor();
 		while (!within(id, m.get_Id(), m_suc.get_Id(), 1, 0)) {
 			// DONE
 			// m = m.closestPrecedingFinger(id);
 			if (m != parent) {
 				String res = communicate(m.get_Id(), "005,"+id);
 				m = Coordinator.getNode(Integer.parseInt(res));
 			}
 			else {
 				m = closestPrecedingFinger(id);
 			}
 			if (m != parent) {
	 			String res = communicate(m.get_Id(), "009");
	 			m_suc = Coordinator.getNode(Integer.parseInt(res));
 			} else {
 				m_suc = parent.successor();
 			}
 		}
 		return m;
 	}

 	public ChordNode closestPrecedingFinger(int id) {
 		for (int i=8; i>=1; i--) {
 			if (within(fingers.get(i).node.get_Id(), parent.get_Id(), id, 1, 1)) {
 				return fingers.get(i).node;
 			}
 		}
 		return parent;
 	}

 	public void addKeys(HashSet<Integer> keys) {
 		for (int k : keys) {
 			parent.keys.add(k);
 		}
 	}

 	public boolean leave() {
 		if (parent == parent.successor())
 			return false;

 		// move all my keys to successor()
 		// for (int k : keys) {
 		// 	// DONE
 		// 	successor().keys.add(k);
 		// }
 		StringBuilder sb = new StringBuilder();
 		sb.append("007,");
 		for (int k : parent.keys)
 			sb.append(k+"#");
 		sb.deleteCharAt(sb.length()-1);
 		communicate(parent.successor().get_Id(), sb.toString());

 		// DONE
 		// successor().predecessor = predecessor;
 		communicate(parent.successor().get_Id(), "003,"+parent.predecessor.get_Id());
 		for (int i=1; i<=8; i++) {
 			ChordNode p = findPredecessor(( 1 + 256 + parent.get_Id() - (int)(Math.pow(2, i-1)) ) % 256);
 			// DONE
 			if (p != parent) 
 				communicate(p.get_Id(), "008,"+parent.get_Id()+","+i+","+parent.successor().get_Id());
 			else
	 			remove_node(parent, i, parent.successor());
 		}
 		return true;
 	}

 	public void remove_node(ChordNode n, int i, ChordNode repl) {
 		if (fingers.get(i).node == n ) {
 			fingers.get(i).node = repl;
 			// DONE
 			// predecessor.remove_node(n, i, repl);
 			if (parent.predecessor != parent)
	 			communicate(parent.predecessor.get_Id(), "008,"+n.get_Id()+","+i+","+repl.get_Id());
	 		else 
	 			remove_node(n, i, repl);
 		}
 	}

}
