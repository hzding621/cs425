import java.io.*;
import java.util.*;
import java.net.*;

public class Coordinator {

	private static TreeMap<Integer, ChordNode> nodeList = new TreeMap<Integer, ChordNode>();	

	private static int largestPort = 8000;
	public static int nextPort() {
		return largestPort++;
	}

	public static ChordNode getNode(int id ) {
		return nodeList.get(id);
	}

	public static void main(String[] args) {

		Random r = new Random();

		Scanner stdin = new Scanner(System.in);
		while (stdin.hasNext()) {

			String cmd = stdin.nextLine();
			String[] ops = cmd.split(" ");
			if (ops[0].equals("join")) {
			
				int id = Integer.parseInt(ops[1]);
				int port = nextPort();
				ChordNode newNode = new ChordNode(id, port);
				nodeList.put(id, newNode);
				newNode.start();
				while (true) {

					try (
						Socket socket = new Socket("127.0.0.1", port);
						PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
						BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					) {
						out.println("000,"+id);
						String res = in.readLine();
						if (res.equals("0"))
							break;
						
					} catch (UnknownHostException e) {
						continue;
					} catch (IOException e) {
						continue;	
					}
				}
			} else if (ops[0].equals("show")) {
				if (ops[1].equals("all")) {

				} else {

					int id = Integer.parseInt(ops[1]);
					ChordNode n = getNode(id);
					if (n == null)
						continue;
					int port = n.getPort();

					while (true) {

					try (
						Socket socket = new Socket("127.0.0.1", port);
						PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
						BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					) {
						out.println("010");
						String res = in.readLine();
						if (res.equals("0"))
							break;
						
					} catch (UnknownHostException e) {
						continue;
					} catch (IOException e) {
						continue;	
					}
				}


				}



			}


		}

		
	}

}