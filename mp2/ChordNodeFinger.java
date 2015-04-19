import java.util.*;

public class ChordNodeFinger {
	public int id;
	public int start;
	public ChordNode node;
	public ChordNodeFinger(int id, int k) {
		this.id = id;
		this.start = (int)(id + Math.pow(2, k-1)) % 256;
	}
}
