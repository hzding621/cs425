import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.sql.*;

public class Message {
	public String message;
	public long dataField;
	public int fromNode;
	public int model;

	public Message(String msg, long data, int f, int m) {
		message = msg;
		dataField = data;
		fromNode = f;
		model = m;
	}


}