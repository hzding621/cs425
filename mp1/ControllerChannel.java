import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ControllerChannel extends Thread {

	private BlockingDeque<ControllerWaitingThread> q = null;
	private int fromNode;
	private int toNode;
	private long lastDeliveryTime = -1;

	public long getLastDeliveryTime() {
		return lastDeliveryTime;
	}

	public void setLastDeliveryTime(long t) {
		lastDeliveryTime = t;
	}

    public ControllerChannel(int f, int t) {
        q = new LinkedBlockingDeque<ControllerWaitingThread>();
        fromNode = f;
        toNode = t;
        lastDeliveryTime = -1;
    }

    public BlockingDeque<ControllerWaitingThread> getQueue() {
    	return q;
    }

    public void run() {
    	while (true) {
    		String msg = null;
    		// Channel constantly tries to dequeue and apply waiting
    		try {
	    		ControllerWaitingThread w = q.takeFirst(); // this call blocks if queue is empty
	    		w.start(); // waiting
	    		w.join();
	    		msg = w.getMessage();
    		} catch (InterruptedException e) {
    			e.printStackTrace();
				System.exit(1);
    		} 
    		Controller.deliverMessage(toNode, msg); // here is sequential
    		

    	}
    }
}