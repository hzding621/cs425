import java.io.*;
import java.net.*;
import java.util.*;

public class ControllerChannelMessage extends Thread {

	private long deliveryTime;
	private long waitingTime;
	private String message;  // the raw message that contains fromNode, toNode, and messageContent
	private int fromNode;
	private int toNode;

	public ControllerChannelMessage(long d, long w, String m, int f, int t) {
		deliveryTime = d;
		waitingTime = w;
		message = m;
		fromNode = f;
		toNode = t;
	}

	public String getMessage() {
		return message;
	}

	public void run() {
		try {
			sleep(waitingTime);
		} catch (InterruptedException e) {
			System.err.println("Sleeping is Interrupted. Controller failure.");
			System.exit(1);
		}
	}
}