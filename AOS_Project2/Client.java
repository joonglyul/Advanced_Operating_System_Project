import java.net.*;
import java.io.*;
import java.util.*;

/*
 * CS 6378: Advanced Operating System
 * Project 2
 * Module: Client
 * Topic: Dynamic Replication.
 * 
 */

public class Client{
	protected String ipAddress=null, hostName=null;
	static String myHostName = null;
	static final int port = 6025;
	static final int rcvPort = 6055;
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
	
	String [] fileNameA = new String [3];
	
	static LogicalClock lc = new LogicalClock();
	
	List <String> ipAddrSet = getIpAddrAll();	
 	
	List <ObjectOutputStream> S_ObjStreamList = new ArrayList<ObjectOutputStream>();	
	
	public Client() {
		
	}
	
	public void client_main(){
		
		try{
			System.out.println("******* Start Client *******");
						
			Random randGen = new Random();
			int tempRandom =0;
			tempRandom = randGen.nextInt(1000);
			
			InetAddress addr = InetAddress.getLocalHost();        
						
			// Get IP Address
			System.out.println("IP Address: [ "+addr.getHostAddress()+" ]");
			
			ipAddress = addr.getHostAddress();
		    byte[] ipAddr = addr.getAddress();
		    System.out.println("[Client] Client Node ID : "+ipAddr[3]);
		    hostName = Byte.toString(ipAddr[3]);			
		    myHostName = Byte.toString(ipAddr[3]);	
		    
		    MSG sendMsg = new MSG();
		    	
			sendData = "keep going";
			while(true){	
				
				sendMsg = new MSG();
			    
			    sendMsg.setChannelType("Client");
			    sendMsg.setCmd("WRITE");
			    sendMsg.setNodeID(myHostName);
			    sendMsg.setClientIP(ipAddress);
			    sendMsg.setObjNum(seqNum);
			    sendMsg.setFileName("Data.txt");
							    
			    reqMessage(sendMsg);
				//sendMsgSvrUni(sendMsg);
			    seqNum++;
			    
				Thread.sleep(1000);
				
				sendMsg = new MSG();
			    
			    sendMsg.setChannelType("Client");
			    sendMsg.setCmd("READ");
			    sendMsg.setNodeID(myHostName);
			    sendMsg.setClientIP(ipAddress);
			    sendMsg.setObjNum(seqNum);
			    sendMsg.setFileName("Data.txt");
				
			    
			    reqMessage(sendMsg);
				
			    Thread.sleep(900);
			    
				if(sendData.equals("/quit")){
					endflag = true;
					break;
				}
				seqNum++;
			    Thread.sleep(tempRandom);
			}
			System.out.println(" Client closed !!!! ");				
			
			
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
								
				ReceiveThread rt = new ReceiveThread(client);				
				Thread t = new Thread(rt);				
				t.start();
				
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
	
	List <String> getIpAddrAll(){
		
		 List <String> list = new ArrayList<String>();
		 	
		 //***** File-servers list *****
		 
		 list.add("10.176.67.83");		
		 list.add("10.176.67.84");
		 list.add("10.176.67.85");
		 list.add("10.176.67.86");
		 list.add("10.176.67.87");
		
		return list;    
      
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
			cs.client_main();			
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
}

/**
 * Logical Clock Class. 
 * @author joonglyul
 *
 */

class LogicalClock {
	
	static long l_Clock = 0;
	
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
	ServerSocket server;
	Socket child;
	ObjectOutputStream oos;	
	ObjectInputStream ois;	
	
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
	
}

