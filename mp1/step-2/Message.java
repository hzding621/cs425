import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.sql.*;

public class Message {
	public String message;
	public int sequence;
	public int fromNode;

	public Message(String msg, int s, int f) {
		message = msg;
		sequence = s;
		fromNode = f;
	}


}