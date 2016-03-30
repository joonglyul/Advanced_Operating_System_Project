import java.io.*;
import java.net.*;
import java.util.*;

/*
 * CS 6378: Advanced Operating System
 * Project 2
 * Module: Server
 * Topic: Dynamic Replication.
 * 
 */

public class Server{
	ServerSocket server;	
	final static int portS=6035; // Server Side Port.	
	Socket child;
	final static VectorClock vc = new VectorClock();
	
	final static List <long[] > bufferList = new ArrayList<long[]>();
	
	public Server() {
		
	}
	
	public void server_main() {
		try{
			server = new ServerSocket(portS);			
		}catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println("******** Server Start ***********");
		
		while(true){
			try{
				child = server.accept();
				
				ServerThread childThread = new ServerThread(child, vc, bufferList);
				Thread t = new Thread(childThread);
				t.start();
				
			}catch(Exception e){
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
	
	public static void main(String[] args){
		Server sv = new Server();
		sv.server_main();
	}	
	
}

/**
 * This class is server's receive thread. 
 * @author joonglyul
 *
 */

class ServerThread implements Runnable {
	Socket child;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	
	private ObjectOutputStream oos_S;
	
	String chType = null, cmd = null, nodeID = null, objNum = null;
	String tempString = null, clientIP = null;
	String fileName = null, hostName = null, readFileName = null;
	String recvFileName = null;
	long timeStamp[]= new long[5];
	//long sendTStamp[]= new long[5];
	
	List <String> ipAddrSet_Server; 
	List <ObjectOutputStream> S_ObjStreamList;
	List <long[]> bufferList;
	VectorClock vc;
	
	public ServerThread(Socket s, VectorClock vc,List <long[]> bufferList ){
		child = s;
		this.vc = vc;
		this.bufferList = bufferList;
		ipAddrSet_Server = new ArrayList<String>();
		S_ObjStreamList = new ArrayList<ObjectOutputStream>();
		severAddrInit();
		
		try{
			System.out.println("Connected from"+child.getInetAddress());
			
			oos = new ObjectOutputStream(child.getOutputStream());
			ois = new ObjectInputStream(child.getInputStream());			
			
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void run(){
		
		MSG receiveData = null;
		
		try{
			while(true){				
				receiveData = (MSG) ois.readObject();
				if (receiveData != null){
					parseCmd(receiveData);
					
					if (chType.equals("Client")) {	
						setClockTick();
						serverConnect("Server", timeStamp); // Multicast;
						
					}else if (chType.equals("Server")) {
						setClockTick();
						vectorClk(timeStamp);
						serverConnect("Tail", timeStamp);	// Unicast;
						
					}else if (chType.equals("Tail")) {											
						
						long [] sendTStamp = new long[5]; 
						sendTStamp=vectorClkT(timeStamp);
						
						if (sendTStamp[0]!=0) {												
														
							clientConnect(clientIP, cmd, recvFileName);		
							
							hostName = getHostName();							
							fileOperation(hostName+"_File.log", getInfo(0, sendTStamp));
							
							serverConnect("Ack", sendTStamp);							
						
						}else {
							vectorClkP();
						}
						
					}else if (chType.equals("Ack")) {
						String tempMsg= "< nodeID"+nodeID+"> < Obj:"+objNum+">";
						hostName = getHostName();						
						fileOperation(hostName+"_File.log", getInfo(1, timeStamp));
						fileOperation(hostName+"_"+recvFileName, tempMsg);
						
					}					
															
					if(receiveData.equals("/quit"))
						break;
					}
				Thread.sleep(500);
			}
		}catch(Exception e){
			System.out.println("[Server] Client closed");
		}
		finally{
			try{
				if (oos !=null)
					oos.close();
				if (ois !=null)
					ois.close();
				if(child !=null)
					child.close();
			}catch(Exception e){}
		}
	}
	
	public String getInfo(int flag, long [] sendTStamp) {
		String msg=null;
		String nID=getHostName();
		
		msg = "< Node ID: "+nID+" > < Seq Num: "+objNum+" > < Vector Time: ";
		for(int i=0; i<5; i++) {
			if (flag == 1) {
				msg=msg+"["+timeStamp[i]+"]";
			}else {
				msg=msg+"["+sendTStamp[i]+"]";
			}
		}
		msg = msg +" > < Object: "+objNum+" >";
		return msg;
	}
	
	public String getHostName(){
		String hostName = null;
		
		try{			
			InetAddress addr = InetAddress.getLocalHost(); 			
						
		    byte[] ipAddr = addr.getAddress();
		    System.out.println("[Server] My Node ID : "+ipAddr[3]);
		    hostName = Byte.toString(ipAddr[3]);
		}catch(Exception e){
			e.printStackTrace();
		}
	    return hostName;
	}
	
	public String getIpAddr() {
		String ipAddr = null;
		try {
			InetAddress addr = InetAddress.getLocalHost();
			ipAddr = addr.getHostAddress();		
		}catch(Exception e) {
			
		}		
		return ipAddr;
	}
	
	
	public void setClockTick() {
		String ipAddr = null;
		int myID=0;
		
		System.out.println("");
		System.out.println("[VectorClock] Logical Clock Update....");
				
		for (int i = 0; i<ipAddrSet_Server.size(); i++){
			ipAddr = ipAddrSet_Server.get(i);
			if (ipAddr.equals(getIpAddr())) {
				myID = i;
				vc.lClkTick(); // update receiving message.
				
				if(vc.getVClk(myID)<vc.getLClk()) {
					vc.setVClk(myID, vc.getLClk());
				}
				
				System.out.println("[VectorClock] Time Stamp["+myID+"]: "+vc.getLClk());
			}
		}
		System.out.println("");
						
	}
	
	public int getMyID() {
		String ipAddr = null;
		int myID=0;
		
		for (int i = 0; i<ipAddrSet_Server.size(); i++){
			ipAddr = ipAddrSet_Server.get(i);
			if (ipAddr.equals(getIpAddr())) {
				myID = i;
			}
		}
		
		return myID;
	}
	
	public void serverConnect(String channelT, long [] sendTStamp){	
		int port1 = 6035;
		String ipAddr = null;
		Socket client = null;
		
		int myID = 0;
		long vcTimeStamp = 0;
				
		if (channelT.equals("Server")) {
			chType=channelT;
						
			myID=getMyID();
			vcTimeStamp = vc.getLClk();
			System.out.println("[Server] My ID: [ "+myID+
					" ] Vector Time Stamp: [ "+ vcTimeStamp +" ]");
			try{
				for (int i = 0; i<ipAddrSet_Server.size()-1; i++){
					
					MSG message =new MSG();
					
					message.setChannelType(chType);
					message.setCmd(cmd);
					message.setNodeID(nodeID);
					message.setClientIP(clientIP);					
					message.setTimeStamp(myID, vcTimeStamp);
					message.setObjNum(Integer.valueOf(objNum));
					message.setFileName(recvFileName);
					message.setMsgString(tempString);
					
					ipAddr = ipAddrSet_Server.get(i);
					
					if (ipAddr.equals(getIpAddr())) {
						
					}else {
						client = new Socket(ipAddr, port1);
						
						oos_S = new ObjectOutputStream(client.getOutputStream());					
						
						System.out.println("[Server] Server Channel ["+i+": "+ipAddr+"]"+" sending request  !!!");
						
						oos_S.writeObject(message);
						oos_S.flush();
													
						Thread.sleep(100);
						oos_S.close();
						client.close();						
						
					}
					
				}
			}catch(Exception e){
					System.out.println("[Server] Server connection not created !!!");
			}
		}else if(channelT.equals("Tail")) {
			chType=channelT;
			
			myID=getMyID();
			vcTimeStamp = vc.getLClk();
			System.out.println("[Server] My ID: [ "+myID+
					" ] Vector Time Stamp: [ "+ vcTimeStamp +" ]");
						
			try{			
				int tail = ipAddrSet_Server.size()-1;
				
				MSG message =new MSG();
				
				message.setChannelType(chType);
				message.setCmd(cmd);
				message.setNodeID(nodeID);
				message.setClientIP(clientIP);				
				for(int j=0; j<5; j++) {
					message.setTimeStamp(j, vc.getVClk(j));
				}				
				message.setObjNum(Integer.valueOf(objNum));
				message.setFileName(recvFileName);
				message.setMsgString(tempString);
				
				client = new Socket(ipAddrSet_Server.get(tail), port1);
				
				oos_S = new ObjectOutputStream(client.getOutputStream());			
				
				System.out.println("[Server] Tail Channel ["+ipAddrSet_Server.get(tail)+"]"+" Sending request !!!");	
								
				oos_S.writeObject(message);
				oos_S.flush();
							
				Thread.sleep(200);
				oos_S.close();
				client.close();				
				
			}catch(Exception e){
					System.out.println("[Server] Server connection not created !!!");
			}
		}else if(channelT.equals("Ack")) {
			chType=channelT;
			
			myID=getMyID();
			vcTimeStamp = vc.getLClk();
			System.out.println("[Server] My ID: [ "+myID+
					" ] Vector Time Stamp: [ "+ vcTimeStamp +" ]");
			
			try{
				for (int i = 0; i<ipAddrSet_Server.size()-1; i++){
					
					MSG message =new MSG();
					
					message.setChannelType(chType);
					message.setCmd(cmd);
					message.setNodeID(nodeID);
					message.setClientIP(clientIP);
					for(int j=0; j<5; j++) {
						message.setTimeStamp(j, sendTStamp[j]);
					}
					message.setObjNum(Integer.valueOf(objNum));
					message.setFileName(recvFileName);
					message.setMsgString(tempString);
					
					ipAddr = ipAddrSet_Server.get(i);
					
					if (ipAddr.equals(getIpAddr())) {
						
					}else {
						client = new Socket(ipAddr, port1);
						
						oos_S = new ObjectOutputStream(client.getOutputStream());					
						
						System.out.println("[Server] Ack Channel ["+i+": "+ipAddr+"]"+" Sending ack message !!!");
						
						oos_S.writeObject(message);
						oos_S.flush();
													
						Thread.sleep(100);
						oos_S.close();
						client.close();						
						
					}
					
				}
			}catch(Exception e){
					System.out.println("[Server] Server connection not created !!!");
			}
		}
	}
	
	public void clientConnect(String ipAddr, String cmdType, String readFileName){	
		int port1 = 6055;
		String fileNameT = readFileName;
		Socket client = null;
		String tempMsg = null;
								
		try{		
			MSG message =new MSG();
			
			if (cmdType.equals("READ")) {
				
				tempMsg = readFile(fileNameT);
				message.setChannelType("Server_Ack");
				if (tempMsg !=null) {
					message.setCmd("READ_GRANT");
					message.setMsgString(tempMsg);
				}else {
					message.setCmd("READ_FAIL");
					message.setMsgString("File Not Exist !!!");
				}
				message.setNodeID(nodeID);
				message.setClientIP(clientIP);			
				message.setObjNum(Integer.valueOf(objNum));				
			}else if (cmdType.equals("WRITE")){
				
				tempMsg = "<"+nodeID+"> < Obj:"+objNum+">";
				fileOperation(fileNameT, tempMsg);
				message.setChannelType("Server_Ack");
				message.setCmd("WRITE_GRANT");
				message.setNodeID(nodeID);
				message.setClientIP(clientIP);			
				message.setObjNum(Integer.valueOf(objNum));
			}
			
			if (ipAddr != null) {
				System.out.println("[Server] Client IP ["+ipAddr+"]"+ "connected !!!");
				
				client = new Socket(ipAddr, port1);
				if (client != null) {
					oos_S = new ObjectOutputStream(client.getOutputStream());			
					
					oos_S.writeObject(message);
					oos_S.flush();
						
					Thread.sleep(200);
					
					oos_S.close();
					client.close();
					System.out.println("[Server] Client ["+ipAddr+"]"+" connection closed !!!");
				}
			}			
						
		}catch(Exception e){
				e.printStackTrace();
				System.out.println("[Server] Client connection broken !!!");
		}		
		
	}
	
	public void severAddrInit() {
		ipAddrSet_Server.add("10.176.67.83");		
		ipAddrSet_Server.add("10.176.67.84");
		ipAddrSet_Server.add("10.176.67.85");
		ipAddrSet_Server.add("10.176.67.86");
		ipAddrSet_Server.add("10.176.67.87");		
	}
	
	public void parseCmd(MSG rcvData){
		
		if (rcvData != null){			
			System.out.println("");
			
			if (rcvData.getChannelType() != null){						
				chType = rcvData.getChannelType();
				System.out.println("[Server] Channel Type: "+chType);
			}
			
			if (rcvData.getCmd() != null){							
				cmd = rcvData.getCmd();
				System.out.println("[Server] cmd: "+cmd);
			}
			
			if (rcvData.getNodeID() != null){							
				nodeID = rcvData.getNodeID();
				System.out.println("[Server] Node ID: "+nodeID);
			}
			
			if (rcvData.getClientIP() != null){							
				clientIP = rcvData.getClientIP();
				System.out.println("[Server] Client IP: "+clientIP);
			}
			
			if (rcvData != null){	
			System.out.print("[Server] Vector timeStamp: ");			
				for (int i=0;i<5; i++) {
					if (rcvData.getTimeStamp(i) != 0){					
						timeStamp[i] = rcvData.getTimeStamp(i);
						System.out.print(" ["+i+": "+timeStamp[i]+"]");
					}
				}
			System.out.println("");
			}
			
			if (rcvData.getObjNum() != null){							
				objNum = rcvData.getObjNum() ;
				System.out.println("[Server] Message Obj: "+objNum);
			}
			
			if (rcvData.getFileName() != null){							
				recvFileName = rcvData.getFileName();
				System.out.println("[Server] File Name : "+recvFileName);
			}
			
			if (rcvData.getMsg() != null){							
				tempString = rcvData.getMsg() ;
				System.out.println("[Server] Message : "+tempString);
			}
		}
		System.out.println();
	}
	
	public long[] vectorClkT(long[] timeS) {		
		int tail = ipAddrSet_Server.size()-1;
		int bufferFlag=0;
		long sendTStamp[] = new long[5];
		
		if (vc.getMutex()==1) {
			while(true) {
				if (vc.getMutex()==0)
					break;
			}
		}else {
		
			vc.vClockBlock();		
		
			for(int i=0; i<5; i++) {
				if (timeS[i]>(vc.getVClk(tail)+1))
					bufferFlag=1;						
			}
			
			if (bufferFlag==1) {
				bufferList.add(timeS);
				System.out.println("[VectorClock Buffer] Vector Clock buffering....");
			}else {
				vectorClk(timeS);
			}
						
			if ((vc.getVClk(0)==vc.getVClk(1))&&
					(vc.getVClk(2)==vc.getVClk(3))&&
							(vc.getVClk(1)==vc.getVClk(2))){
				
				setClockTick();
				updateVC();
				System.out.println();
				
				sendTStamp = vc.getVclk();
			}		
			
			vc.vClockUnblock();
		}
		return sendTStamp;
	}
	
	public void vectorClkP() {		
		int tail = ipAddrSet_Server.size()-1;
		int processFlag=0;
		
		for (int j=0; j<bufferList.size(); j++) {
			long [] timeVC = bufferList.get(j);
			
			for(int i=0; i<5; i++) {
				if (timeVC[i]<=vc.getVClk(tail))
					processFlag=1;						
			}
			
			if (processFlag==1) {
				vectorClk(timeVC);
				System.out.println("[VectorClock Buffer]["+bufferList.size()
						+"] Vector Clock Processed....");
				bufferList.remove(j);
				
				setClockTick();
				updateVC();				
				processFlag=0;
			}
		}
		
	}
	
	public void vectorClk(long[] timeS) {
		System.out.println("[VectorClock] Vector Clock Update....");
		
		for(int i=0; i<5; i++) {
			if (vc.getVClk(i)< timeS[i]) {
				vc.setVClk(i, timeS[i]);
			}			
		}		
		updateVC();
		System.out.println("");
	}
	
	public void updateVC() {
		String ipAddr = null;		
		
		System.out.print("[VectorClock] :");
		for(int i=0; i<5; i++) {			
			ipAddr = ipAddrSet_Server.get(i);
			if (ipAddr.equals(getIpAddr())) {
				if (vc.getVClk(i)< vc.getLClk())
					vc.setVClk(i, vc.getLClk());
			}
			System.out.print("["+vc.getVClk(i)+"]");
		}
					
	}
	/**
	 * This is function to write files.
	 * @param fileName
	 * @param fileMessage
	 */
	
	public void fileOperation(String fileName, String fileMessage) {		
	
		if (fileExist(fileName)) {
			writeFile(fileName, fileMessage);
		}else {
			createFile(fileName, fileMessage);
		}
	}
	
	public void writeFile(String fileName, String fileMessage)
	  {		
			
		  try{
			  System.out.println("[Server] Append file !!!");
			  
			  // Append file 
			  FileWriter fstream = new FileWriter(fileName,true);
			  BufferedWriter out = new BufferedWriter(fstream);
			  
			  String logMsg = fileMessage+"\n";
			  out.write(logMsg);
			  System.out.println(fileMessage);
			  
			  out.close();
		  }catch (Exception e){
			  System.err.println("Error: " + e.getMessage());
		  }
	  }	
	
	/**
	 * This is function to create new file.
	 * @param fileName
	 * @param fileMessage
	 */
	
	public void createFile(String fileName, String fileMessage)
	  {
		  try{
			  System.out.println("[Server] New file create !!!");
			  
			  // Create file 
			  FileWriter fstream = new FileWriter(fileName);
			  BufferedWriter out = new BufferedWriter(fstream);
			  
			  String logMsg = fileMessage+"\n";
			  out.write(logMsg);
			  System.out.println(fileMessage);
			  
			  out.close();
		  }catch (Exception e){
			  System.err.println("Error: " + e.getMessage());
		  }
	  }	
	
	/**
	 * This is function to read file. 
	 * @param fileName
	 * @return
	 */
	
	public String readFile(String fileName)
	  {			
		String strLine=null, bufStr=null;
		if (fileName !=null) {
			if (fileExist(fileName)) {
		
			  try{
				  FileInputStream fstream = new FileInputStream(fileName);
				 
				  DataInputStream in = new DataInputStream(fstream);
				  BufferedReader br = new BufferedReader(new InputStreamReader(in));
				  
				  //Read File Line By Line			  
				  while ((bufStr = br.readLine()) != null)   {				  
					  strLine = bufStr;
				  }
				  
				  in.close();
				  System.out.println ("[Server] File Read : ");
				  System.out.println (strLine);;
			    }catch (Exception e){
			    	System.err.println("Error: " + e.getMessage());
			  }
			   
			}else {
				strLine ="File Not Exist !!!";
			}
		}
		 return strLine;
	  }
	
	public String getDir(){
		
	  String path = null;
		
	  try {
		  path = new java.io.File(".").getCanonicalPath();
		  System.out.println("[Server] File path : "+path);			 
	  }catch (IOException e){
		  e.printStackTrace();
	  }
	  return path;			  
	}	
	
	public boolean fileExist (String fileName){
		
		File file=new File(fileName);
		boolean exists = file.exists();
		
		return exists;		
	}
	
	/**
	 * This is function to get file list.
	 * @return
	 */
	
	public String listFile() {
		
		File[] fileList = null;
		String fileNL = null;
		
		try {		
			String curDir = getDir();
			File file = new File(curDir); // new File("."); current directory			
	
			FilenameFilter textFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					String lowercaseName = name.toLowerCase();
					if (lowercaseName.endsWith(".txt")) {
						return true;
					} else {
						return false;
					}
				}
			};
					
			fileList = file.listFiles(textFilter);
					
			for (int i = 0; i < fileList.length; i++)  
			 {  
				String dumyS = fileList[i].toString();
				String[] st = dumyS.split("/");
				if (i==0){
					fileNL = "/"+st[st.length-1];
				}else{
					fileNL = fileNL+"/"+st[st.length-1];
				}
				System.out.println("[Server] File list : "+fileList[i].toString());  
			 } 
			
			
		}catch (Exception e){
			e.printStackTrace();
		}
		
		return fileNL;
	}	
	
}

class VectorClock {
	private static long V_Clock = 0;	
	private static long V_timeStamp[]= new long[5];
	private static int mutex =0;
	
	public long getLClk() {
		return V_Clock;
	}
	
	public void lClkTick() {
		V_Clock++;		
	}
	
	public long getVClk(int num) {
		return V_timeStamp[num];
	}
	
	public long[] getVclk() {
		return V_timeStamp;
	}
	
	public void setVClk(int num, long time) {
		V_timeStamp[num]=time;
	}	
	
	public void vClockBlock() {
		mutex = 1;
	}
	
	public void vClockUnblock() {
		mutex = 0;
	}
	
	public int getMutex() {
		return mutex;
	}
	
}

