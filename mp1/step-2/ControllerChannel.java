import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.sql.*;

public class ControllerChannel extends Thread {

	private BlockingDeque<ControllerChannelMessage> q = null;
	private int fromNode;
	private int toNode;
	private long previousScheduledTime = -1;

	public void enqueueMessage(String msg) {
		// This method is called from Controller Thread
		// Sequential call of this method is guaranteed, 
		// so there won't be race condition of using the previousScheduledTime value
		long r = Controller.getRandomDelay(fromNode, toNode);
		long currentTime = System.currentTimeMillis();
		Time t1 = new Time(currentTime);
		System.out.println();
		long actualDelay = r;
		if (currentTime < previousScheduledTime) // normalize time according to previous message
			actualDelay = Math.max(0, currentTime+r-previousScheduledTime);

		if (previousScheduledTime < currentTime)
			previousScheduledTime = currentTime + actualDelay;
		else 
			previousScheduledTime = actualDelay+previousScheduledTime;
		Time t = new Time(previousScheduledTime);
		System.out.println( fromNode + "->" + toNode +
							" M:" + msg +
							" EnQ:" + t1.toString() + 
							" r:"+ r + 
							" actualDelay:" + actualDelay + 
							" scheduledTime:" + t.toString()
							);
		try {
			// this will never block because all puts are sequential
			q.putLast(new ControllerChannelMessage(actualDelay+currentTime, actualDelay, msg, fromNode, toNode));
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public ControllerChannel(int f, int t) {
		q = new LinkedBlockingDeque<ControllerChannelMessage>();
		fromNode = f;
		toNode = t;
		previousScheduledTime = -1;
	}

	public BlockingDeque<ControllerChannelMessage> getQueue() {
		return q;
	}

	public void run() {
		while (true) {
			String msg = null;
			// Channel constantly tries to dequeue and apply waiting
			try {
				ControllerChannelMessage m = q.takeFirst(); // this call blocks if queue is empty
				m.start(); // waiting
				m.join();
				msg = m.getMessage();
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			} 
			Controller.deliverMessage(fromNode, toNode, msg); // here is sequential
		}
	}
}