import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.sql.*;

public class MessageComparator implements Comparator<Message> {
	@Override
    public int compare(Message x, Message y){
        if(x.dataField<y.dataField){
            return -1;
        }
        if(x.dataField>y.dataField){
            return 1;
        }
        return 0;
    }
}