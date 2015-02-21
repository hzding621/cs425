import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.sql.*;

public class NodeServer extends Thread {

	private int port;
	private int maxDelay;

	public NodeServer(int p, int d) {
		port = p;
		maxDelay = d;
	}

	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(port)) { 
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
					System.out.println("Received \"" + messageContent +"\" from " + fromNode +
												", Max delay is " + maxDelay*1.0/1000 + 
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
