import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.sql.*;

public class NodeServer extends Thread {

	private int nodeNum;
	private int port;
	private int maxDelay;

	public NodeServer(int n, int p, int d) {
		nodeNum = n;
		port = p;
		maxDelay = d;
	}

	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(port)) { 
			System.out.println("Node "+nodeNum+" listens at port "+port);

			while (true) {
				Socket socket = serverSocket.accept();

				// Controller accpets node connection sequentially
				try (
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				) {
					String message = in.readLine();
					socket.close();
					String[] messageLines = message.split(";");
					int fromNode = Integer.parseInt(messageLines[0]);
					String messageContent = messageLines[2];
					Time curTime = new Time(System.currentTimeMillis());

					double md = nodeNum == fromNode ? 0 : maxDelay*1.0/1000;
					System.out.println("Received \"" + messageContent +"\" from " + fromNode +
												", Max delay is " + md + 
												"s, system time is " + curTime.toString());

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
