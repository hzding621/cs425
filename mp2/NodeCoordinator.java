import java.io.*;
import java.util.*;

public class NodeCoordinator {

	private static TreeMap<Integer, NodeThread> nodeMap = new TreeMap<Integer, NodeThread>();	
	private static List<Integer> nodelist = new ArrayList<Integer>();

	public static void main(String[] args) {

		Random r = new Random();

		Scanner stdin = new Scanner(System.in);
		while (stdin.hasNext()) {

			String cmd = stdin.nextLine();
			String[] ops = cmd.split(" ");
			if (ops[0].equals("join")) {
				int id = Integer.parseInt(ops[1]);
				NodeThread n = null;
				if (nodeMap.isEmpty())
					n = new NodeThread(id, null);
				else {
					NodeThread someThread = nodeMap.get(nodelist.get(r.nextInt(nodelist.size()) ) );
					n = new NodeThread(id, someThread);
				}
				nodeMap.put(id, n);		
				nodelist.add(id);		
			} else if (ops[0].equals("show")) {

				if (ops[1].equals("all")) {
					for (int id : nodeMap.keySet()) {
						nodeMap.get(id).printKeys();
					}
				}

				else {

					int id = Integer.parseInt(ops[1]);
					if (!nodeMap.containsKey(id)) {
						System.out.println("Node not exist.");
					} else {
						nodeMap.get(id).printKeys();
						// nodeMap.get(id).printDetails();
					}
				
				}
			}


		}

		
	}

}