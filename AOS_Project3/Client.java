import java.net.*;
import java.io.*;
import java.util.*;

/*
 * CS 6378: Advanced Operating System
 * Project 3
 * Module: Client
 * Topic: Dynamic Replication.
 * 
 */

public class Client{
	protected String ipAddress=null, hostName=null;
	protected static String myHostName = null;
	protected static final int port = 6025;
	protected static final int rcvPort = 6055;
	final static int portM = 7588;	
	protected String masterIP = "10.176.67.83";
	
	protected Socket client = null;
	protected BufferedReader read;
	protected ObjectOutputStream oos_C;
	protected ObjectOutputStream oos_S;
	protected ObjectInputStream ois_S;
	
	protected String sendData, eventT, receiveData;
		
	protected String cmd = null, obj=null, nodeID= null, fileName = null, eventType = null;
	protected long timeStamp=0;
	protected int numOfNode=4;
	protected int seqNum=0;
	
	protected boolean endflag=false;
	protected boolean expflag=false;	
	
	protected MSG sendMsg;
	String [] fileNameA = new String [3];
	
	static LogicalClock lc = new LogicalClock();
	
	static List <String> ipAddrSet = new ArrayList <String> (); 	
	List <ObjectOutputStream> S_ObjStreamList = new ArrayList<ObjectOutputStream>();	
	
	public Client() {
		initIpAddrAll();
	}
	
	public void master_Cnt() {
		
		try {
			
			client = new Socket(masterIP, portM);			
			C_NotifyThread cnt = new C_NotifyThread(client, ipAddrSet, lc);			
			Thread th = new Thread(cnt);			
			th.start();
						
		}catch(Exception e) {
			System.out.println("[Client] Can't connect Master Server !!!");
			//e.printStackTrace();			
		}
		
	}
	
	public void client_main(){
		
		try{
			System.out.println("******* Start Client *******");
			
			int tempNum = 0;
			String[] tempCmd = {"WRITE", "READ", "WRITE", "WRITE", "WRITE", "READ", "WRITE", "READ", "WRITE", "WRITE"}; 					
			InetAddress addr = InetAddress.getLocalHost();        
						
			// Get IP Address
			System.out.println("IP Address: [ "+addr.getHostAddress()+" ]");
			
			ipAddress = addr.getHostAddress();
		    byte[] ipAddr = addr.getAddress();
		    System.out.println("[Client] Client Node ID : "+ipAddr[3]);
		    hostName = Byte.toString(ipAddr[3]);			
		    myHostName = Byte.toString(ipAddr[3]);	
		    		    	
		    Random randGen = new Random();
			
			while(true){
				synchronized(lc) {
					if(!lc.getFlag()) {
						sendMsg = new MSG();
					    tempNum = randGen.nextInt(9)+1;
					    
					    sendMsg.setChannelType("Client");
					    sendMsg.setCmd(tempCmd[tempNum]);
					    sendMsg.setNodeID(myHostName);
					    sendMsg.setClientIP(ipAddress);
					    sendMsg.setObjNum(seqNum);
					    sendMsg.setFileName("Data.txt");
					    sendMsg.setSeqNum(seqNum);
									    
					    reqMessage(sendMsg);
						//sendMsgSvrUni(sendMsg);
					    seqNum++;
					    
						Thread.sleep(100*tempNum);			
					    					
						//Debug Code;
						Thread.sleep(500);
					}else{
						Thread.sleep(1000);
					}
				}
			}
						
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		finally{
			try{				
				oos_C.close();
				client.close();
				System.exit(0);
			}catch(IOException e2){
				e2.printStackTrace();
			}
		}
	}
	
	/**
	 * Server Connection.
	 */
	
	public void serverConnect(){	
		int port1 = 6035;
		Socket client = null;		
		
		try{
			for (int i = 0; i<ipAddrSet.size(); i++){
				client = new Socket(ipAddrSet.get(i), port1);				
		
				ois_S = new ObjectInputStream(client.getInputStream());
				oos_S = new ObjectOutputStream(client.getOutputStream());
				
				S_ObjStreamList.add(oos_S);	
								
				//ReceiveThread rt = new ReceiveThread(client);				
				//Thread t = new Thread(rt);				
				//t.start();
				
				System.out.println("[Client] Server ["+ipAddrSet.get(i)+"]"+" connected !!!");	
			
			}
		}catch(Exception e){
				System.out.println("[Client] Server connection not created !!!");
		}
	}
	
	public void sendMsgSvr(MSG message){	
		try{
			for (int i = 0; i<S_ObjStreamList.size(); i++){			
				oos_S = S_ObjStreamList.get(i);
				oos_S.writeObject(message);
				oos_S.flush();	
			}
		}catch(IOException e){
			System.out.println("[Client] Server connection closed");
		}		
	}
	
	public void sendMsgSvrUni(MSG message){	
		Random randGen = new Random();
		int i=0;
		
		try{
			i=randGen.nextInt(4);					
			oos_S = S_ObjStreamList.get(i);
			oos_S.writeObject(message);
			oos_S.flush();				
		}catch(IOException e){
			System.out.println("[Client] Server connection closed");
		}		
	}
	
	public void reqMessage(MSG message){	
		int port1 = 6035, i=0;
		Socket client = null;
		Random randGen = new Random();
		
		try{
			
			i=randGen.nextInt(4);
			
			client = new Socket(ipAddrSet.get(i), port1);				
	
			ois_S = new ObjectInputStream(client.getInputStream());
			oos_S = new ObjectOutputStream(client.getOutputStream());			
				
			System.out.println("[Client] Server ["+ipAddrSet.get(i)+"]"+" connected !!!");	
			
			//Thread.sleep(1000);
			oos_S.writeObject(message);
			oos_S.flush();
			
			Thread.sleep(500);
			oos_S.close();
			ois_S.close();
			client.close();
			
			
		}catch(Exception e){
				System.out.println("[Client] Server connection not created !!!");
		}
	}
	
	/**
	 * File List parsing.
	 * @param fileName
	 */
	
	public void parseFileL(String fileName){
		
		if (fileName != null){
			StringTokenizer str = new StringTokenizer(fileName, "/");
			
			if (str.hasMoreTokens() == true){						
				fileNameA[0] = str.nextToken();
				System.out.print("[Client] File List [ "+fileNameA[0]+" ] ");
			}
			
			if (str.hasMoreTokens() == true){						
				fileNameA[1]  = str.nextToken();
				System.out.print("[ "+fileNameA[1]+" ] ");
			}
			
			if (str.hasMoreTokens() == true){						
				fileNameA[2]  = str.nextToken();
				System.out.print("[ "+fileNameA[2]+" ] ");
			}
			
			System.out.println();										
		}		
	}
	
	public void initIpAddrAll(){
		
		 //ist <String> list = new ArrayList<String>();
		 	
		 //***** File-servers list *****
		 
		 String toMsg=null, tempBuf=null, strLine=null;		
		
		 try {
			 FileInputStream fstream = new FileInputStream("serverList.conf");
			  
			 DataInputStream in = new DataInputStream(fstream);
			 BufferedReader br = new BufferedReader(new InputStreamReader(in));
			  			
			 while ((strLine = br.readLine()) != null){					  
				  toMsg = toMsg+strLine;
			 }
			  
			 in.close();		
		
		 }catch(Exception e){
			 e.printStackTrace();
		 }
		
       StringBuffer buffer = new StringBuffer(toMsg.length());  
	        for(int i = 0; i < toMsg.length(); i++) {  
	              char ch = toMsg.charAt(i);  
	              if((ch >= '0') && (ch <= '9') || (ch == '.') || (ch =='/')) {  
	                buffer.append(ch);  
	             }  
	         }  
	        
       toMsg =buffer.toString();  
	   
	    StringTokenizer st = new StringTokenizer(toMsg,"/");
	     while (st.hasMoreTokens()) {
	    	 tempBuf = st.nextToken().trim();
	         System.out.println("[Client] Server List : "+tempBuf);
	         ipAddrSet.add(tempBuf);
	     }
	     		    
      
	}
		
	/**
	 * Buffer remove function. 
	 * @param fileName
	 * @param evtType
	 */
			
	public static void main(String[] args){		
		try{
			
			AckReceiverThread art = new AckReceiverThread(rcvPort);				
			Thread ackRT = new Thread(art);				
			ackRT.start();
			
			Client cs = new Client();
			cs.master_Cnt();
			cs.client_main();			
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
}

class C_NotifyThread implements Runnable {
	protected Socket mServer=null;
	protected ObjectOutputStream oos=null;
	protected ObjectInputStream ois=null;
	
	protected List <String> ipAddrs;
	protected String cmd = null, ipAddr = null;
	
	protected LogicalClock lc;
	
	public C_NotifyThread(Socket mServer, List <String> ipSet,LogicalClock lc){	
		this.mServer = mServer;
		this.lc = lc;
		
		try {			
			oos = new ObjectOutputStream(mServer.getOutputStream());
			ois = new ObjectInputStream(mServer.getInputStream());			
			ipAddrs = ipSet;
		}catch(Exception e) {
			e.printStackTrace();			
		}
	}
	
	public void run() {
		
		String receiveData = null;		
		
		try {
					
			while(true) {
				receiveData = (String) ois.readObject();				
				if (receiveData != null){
					System.out.println("[Client] Fail Server:[ "+receiveData+" ]");
					parseCmd(receiveData);
					if(cmd.equals("Tail")) {
						synchronized(lc) {
							lc.setSFlag(true);
						}
						updateIPset(ipAddr);					
					}else if(cmd.equals("Non-Tail")) {
						synchronized(lc) {
							lc.setSFlag(true);
						}
						updateIPset(ipAddr);					
					}else if(cmd.equals("NewHead")){
						Thread.sleep(3000);
						System.out.println("");
						System.out.println("[Client] ******************************** ");
						System.out.println("[Client] System Unsuspended  !!!");
						System.out.println("[Client] ******************************** ");
						System.out.println("");
						synchronized(lc) {
							lc.setSFlag(false);
						}						
					}else if(cmd.equals("NewTail")){
						Thread.sleep(3000);
						System.out.println("");
						System.out.println("[Client] ******************************** ");
						System.out.println("[Client] System Unsuspended  !!!");
						System.out.println("[Client] ******************************** ");
						System.out.println("");
						synchronized(lc) {
							lc.setSFlag(false);
						}						
					}else {
						synchronized(lc) {
							lc.setSFlag(false);
						}
					}
				}
				
			}
		
		}catch(Exception e) {
			System.out.println("[Client] Master Server connection closed !!! ");
			//e.printStackTrace();
		}
	}
	
	public void updateIPset(String rvIP) {
		
		for (int i=0; i<ipAddrs.size(); i++) {
			if (rvIP.equals(ipAddrs.get(i))) {
				ipAddrs.remove(i);
			}
		}
	}
	
	public void parseCmd(String toMsg) {
		
		StringTokenizer st = new StringTokenizer(toMsg,"/");
	   	    	    
		if (st.hasMoreTokens()) {
	    	 cmd = st.nextToken().trim();
	         System.out.println("[Server] Token : "+cmd);	         
	    }
		
		if (st.hasMoreTokens()) {
	    	 ipAddr = st.nextToken().trim();
	         System.out.println("[Server] Token : "+ipAddr);	         
	     }			
	}	
	
	public String getIpAddr() {
		String ipAddr = null;
		try {
			InetAddress addr = InetAddress.getLocalHost();
			ipAddr = addr.getHostAddress();		
		}catch(Exception e) {
			
		}
		System.out.println("[Server] My ipAddr : "+ipAddr);
		return ipAddr;
	}
}

/**
 * Logical Clock Class. 
 * @author joonglyul
 *
 */

class LogicalClock {
	
	static long l_Clock = 0;
	static boolean sFlag = false;
	
	public long getLclock(long time){
		if(l_Clock < time){
			l_Clock = time;
			l_Clock++;
		}else{
			l_Clock++;
		}
		return l_Clock;
	}
	
	public void setLclock(long time){
		if(l_Clock < time){
			l_Clock = time;
			l_Clock++;
		}else{
			l_Clock++;
		}		
	}
	
	public long getLclock(){
		return l_Clock++;
	}
	
	public boolean getFlag() {
		return sFlag;
	}
	
	public void setSFlag(boolean flag) {
		sFlag = flag;
	}
		
}


/**
 * This class is communication part between client and server.
 * @author joonglyul
 *
 */

class ReceiveThread implements Runnable{
	Socket client;
	ObjectInputStream ois;	
	String chType = null, nodeID = null, ackMsg = null, cmd = null,
			objNum = null, fileString = null;	
	String mainSrvID = "83";
	
	MSG receiveData = null;
	
	int id = 0;
	
	public ReceiveThread(Socket s){
		try{			
			client = s;
			ois = new ObjectInputStream(client.getInputStream());
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	
	public void run(){
		try{
			while(true){
				receiveData = (MSG) ois.readObject();
				if (receiveData != null){					
					parseCmd(receiveData);
					System.out.println("[Client] [ID:"+nodeID+"] receive data: "+cmd);					
					if (receiveData != null){						
						
						if (cmd.equals("READ_GRANT")){							
							
						}else if (cmd.equals("WRITE_GRANT")){								
															
						}else if (cmd.equals("INIT_GRANT")){									
																							
						}else if (cmd.equals("UNKNOWN COMMAND")){
														
						}
									
					if(receiveData.equals("/quit"))
						break;
					}
							
				}	
				
				
			}
						
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}catch(IOException e1){
			e1.printStackTrace();
		}
			finally{
				try{
					ois.close();
					client.close();
				}catch(IOException e2){
					e2.printStackTrace();
				}
			}
	}
	
	public void parseCmd(MSG rcvData){
		
		if (rcvData != null){			
			
			if (rcvData.getChannelType() != null){						
				chType = rcvData.getChannelType();
				System.out.println("[Client] Channel Type: "+chType);
			}
			
			if (rcvData.getCmd() != null){							
				cmd = rcvData.getCmd();
				System.out.println("[Client] cmd: "+cmd);
			}
			
			if (rcvData.getNodeID() != null){							
				nodeID = rcvData.getNodeID();
				System.out.println("[Client] Node ID: "+nodeID);
			}
			
			if (rcvData.getObjNum() != null){							
				objNum = rcvData.getObjNum();
				System.out.println("[Client] Object: "+objNum);
			}
			
			if (rcvData.getMsg() != null){							
				fileString = rcvData.getMsg();
				System.out.println("[Client] Message: "+fileString);
			}
		}
		System.out.println();
	}
		
	
	public void condCheck() {
		
	}
	
}

class AckReceiverThread implements Runnable{
	protected ServerSocket server;
	protected Socket child;
	protected ObjectOutputStream oos;	
	protected ObjectInputStream ois;	
	
	String chType = null, nodeID = null, ackMsg = null, cmd = null,
			objNum = null, fileString = null;	
	
	MSG receiveData = null;	
	
	public AckReceiverThread(int portS ){
		try{
			server = new ServerSocket(portS);				
		}catch(Exception e){
			e.printStackTrace();
		}		
	}	
	
	public void run(){
		System.out.println("[Client] Client is receiving connection ...");
		try{
			while(true){				
				
				child = server.accept();
				ois = new ObjectInputStream(child.getInputStream());
										
				while(true){
					receiveData = (MSG) ois.readObject();
					if (receiveData != null){
						parseCmd(receiveData);
						System.out.println("[Client] [ID:"+nodeID+"] receive data: "+cmd);					
													
						if (cmd.equals("READ_GRANT")){							
							
						}else if (cmd.equals("READ_GRANT")){								
															
						}else if (cmd.equals("WRITE_GRANT")){								
															
						}else if (cmd.equals("INIT_GRANT")){									
																							
						}else if (cmd.equals("UNKNOWN COMMAND")){
														
						}					
						
						System.out.println();
							
					}
					break;
				}					
				
			}			
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}catch(IOException e1){
			e1.printStackTrace();
		}
			finally{
				try{
					ois.close();
					child.close();
				}catch(IOException e2){
					e2.printStackTrace();
				}
			}
	}
	
	public void parseCmd(MSG rcvData){
		
		if (rcvData != null){			
			
			if (rcvData.getChannelType() != null){						
				chType = rcvData.getChannelType();
				System.out.println("[Client] Channel Type: "+chType);
			}
			
			if (rcvData.getCmd() != null){							
				cmd = rcvData.getCmd();
				System.out.println("[Client] cmd: "+cmd);
			}
			
			if (rcvData.getNodeID() != null){							
				nodeID = rcvData.getNodeID();
				System.out.println("[Client] Node ID: "+nodeID);
			}
			
			if (rcvData.getObjNum() != null){							
				objNum = rcvData.getObjNum();
				System.out.println("[Client] Object: "+objNum);
			}
			
			if (rcvData.getMsg() != null){							
				fileString = rcvData.getMsg();
				System.out.println("[Client] Message: "+fileString);
			}
		}
		System.out.println();
	}
	
	
}

class MSG implements Serializable{
	private String chType =null;
	private String cmd =null;
	private String nodeID =null;
	private String clientIP =null;
	private long [] timeStamp = new long [5];
	private String objNum =null;
	private String msgString =null;
	private String fileName =null;
	private long seqNum = 0;
	private long tSeqNum = 0;
		
	synchronized public void setChannelType(String chT) {
		this.chType = chT;
	}
	
	synchronized public void setCmd(String cmdT) {
		this.cmd = cmdT;
	}
	
	synchronized public void setNodeID(String nodeT) {
		this.nodeID = nodeT;
	}
	
	synchronized public void setClientIP(String nodeIP) {
		this.clientIP = nodeIP;
	}
	
	synchronized public void setTimeStamp(int num, long timeS) {
		this.timeStamp[num] = timeS;
	}	
	
	synchronized public void setObjNum(int objT) {
		this.objNum = Integer.toString(objT);
	}
	
	synchronized public void setMsgString(String msgS) {
		this.msgString = msgS;
	}	
	
	synchronized public String getChannelType() {
		return chType;
	}
	
	synchronized public String getCmd() {
		return cmd;
	}
	
	synchronized public String getNodeID() {
		return nodeID;
	}
	
	synchronized public String getClientIP() {
		return clientIP;
	}
	
	synchronized public long getTimeStamp(int num) {
		return timeStamp[num];
	}
	
	synchronized public String getObjNum() {
		return objNum;
	}
	
	synchronized public String getMsg() {
		return msgString;
	}
	
	synchronized public String getFileName() {
		return fileName;
	}
	
	synchronized public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	synchronized public void setSeqNum(long seqNum) {
		this.seqNum = seqNum;		
	}
	
	synchronized public long getSeqNum() {
		return seqNum;
	}
	
	synchronized public void setTSeqNum(long seqNum) {
		this.tSeqNum = seqNum;		
	}
	
	synchronized public long getTSeqNum() {
		return tSeqNum;
	}
	
}

