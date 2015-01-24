import java.io.*;
import java.net.*;

class FileServerThread extends Thread {
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private int connection;

	public FileServerThread(Socket s, int c) throws IOException {
		socket = s;
		connection = c;
		in = new BufferedReader( new InputStreamReader( socket.getInputStream()));
		// Enable auto-flush:
		out = new PrintWriter( new BufferedWriter( new OutputStreamWriter(
						socket.getOutputStream())), true);
		start(); // Calls run()
	}

	String readFileForClient(String filename) {

		String everything = null;
		BufferedReader br = null;
	    try {	
	    	br = new BufferedReader(new FileReader(filename));
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();

	        while (line != null) {
	            sb.append(line);
	            sb.append(System.getProperty("line.separator"));
	            line = br.readLine();
	        }
	        everything = sb.toString();
	        // System.out.println(everything);
	    } 
	    catch (FileNotFoundException e) {
	    	System.err.println("File not found.");
	    }
	    catch (IOException e){
	    	System.err.println("File read error.");
	    } finally {
	    	try {
	    		br.close();
	    	}	
	    	catch (IOException e){
		    	System.err.println("Close error");
	    	}
	    }
	    return everything;
	}

	public void run() {
		try {

			System.out.println("Client " + connection + " connected");

			while (true) {
				System.out.println("Sending message to client " + connection );
				String msg = readFileForClient("test.txt");

				out.println("LENGTH");
				out.println(msg.length());

				out.println("CONTENT");
				out.println(msg);

				out.println("END");
				
				String str = in.readLine();
				if (str.equals("END")) break;
			}
		}
		catch(IOException e) {
			System.err.println("IO Exception");
		} 
		finally {
			try {
				socket.close();
			} catch(IOException e) {
				System.err.println("Socket not closed");
			}
		}
	}
}

public class FileServer {
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
            System.err.println(
                "Usage: java FileServer <port number>");
            System.exit(1);
        }

		int port = Integer.parseInt(args[0]);
		ServerSocket s = new ServerSocket(port);
		String serverPort = s.getLocalPort()+"\n";
		System.out.println("Server Started at Port: " + serverPort);

		int connection = 0;
		try {
			while (true) {
				Socket socket = s.accept();
				connection++;
				try {
					new FileServerThread (socket, connection);
				} catch(IOException e) {
					socket.close();
				} 
			} 
		}
		finally {
			s.close();
		}
	}
}