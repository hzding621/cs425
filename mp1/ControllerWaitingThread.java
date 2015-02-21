import java.io.*;
import java.net.*;
import java.util.*;

public class ControllerWaitingThread extends Thread {

	private long deliveryTime;
    private long waitingTime;
    private String message;  // the raw message that contains fromNode, toNode, and messageContent

    public long getDeliveryTime() {
    	return deliveryTime;
    }

    public ControllerWaitingThread(long d, long w, String m) {
        deliveryTime = d;
        waitingTime = w;
        message = m;
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