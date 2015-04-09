import java.io.*;
import java.util.*;

class NodeFinger {
	public int start;
	public int end;
	public int node;
}

public class NodeThread extends Thread {

	public List<NodeFinger> fingers = new ArrayList<NodeFinger>();
	public int successor;
	public int predecessor;
	public List<Integer> keys = new ArrayList<Integer>();
	
}