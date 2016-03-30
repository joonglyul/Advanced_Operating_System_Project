import java.io.*;
import java.net.*;
import java.util.*;

/*
 * CS 6378: Advanced Operating System
 * Project 3
 * Module: Server
 * Topic: Dynamic Replication.
 * 
 */

public class Server{
	protected ServerSocket server = null;
	final static int portS=6035; // Server Side Port.
	final static int portM = 7588;	
	protected String masterIP = "10.176.67.83";
	
	protected Socket child = null;
	protected Socket client = null;
	final static VectorClock vc = new VectorClock();	
	
	final static List <String> svripList = new ArrayList<String>();	
	final static List <MSG> bufferList = new ArrayList<MSG>();
		
	public Server() {
		
		try{
			server = new ServerSocket(portS);	
			initIpAddrSet();
		}catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println("******** Server Start ***********");
		
	}
	
	public void server_main() {
		
		try {
			client = new Socket(masterIP, portM);
			NotifyThread nt = new NotifyThread(client, vc, svripList, bufferList);
			Thread th = new Thread(nt);
			th.start();
			
		}catch(Exception e) {
			e.printStackTrace();			
		}
		
		while(true){
			try{
				child = server.accept();
				
				ServerThread childThread = new ServerThread(child, vc, bufferList, svripList);
				Thread t = new Thread(childThread);
				t.start();
				
			}catch(Exception e){
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
	
	public void initIpAddrSet(){	
		 	
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
	         System.out.println("[Server] Server List : "+tempBuf);
	         svripList.add(tempBuf);
	     }	    
    
	}
	
	public static void main(String[] args){
		Server sv = new Server();
		sv.server_main();
	}	
	
}

class NotifyThread implements Runnable {
	protected Socket mServer=null;
	protected ObjectOutputStream oos=null;
	protected ObjectInputStream ois=null;
	
	protected List <String> ipAddrs; 
	protected VectorClock vc;
	ComputeRTT calRTT; 
	
	protected String masterIP = "10.176.67.83";
	double rtt,jValue;
	String cmd = null, ipAddr = null;
	String sendMsg = null;
	
	List <MSG> bufferList;
	
	public NotifyThread(Socket mServer, VectorClock vc, List <String> ipSet, List <MSG> bufferList){
		this.bufferList = bufferList;
		this.mServer = mServer;
		this.vc = vc;
		calRTT = new ComputeRTT();
		
		try {
			ois = new ObjectInputStream(mServer.getInputStream());
			oos = new ObjectOutputStream(mServer.getOutputStream());			
			
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
					//System.out.println("[Server] MasterServer : "+receiveData);
					parseCmd(receiveData);
					if (cmd.equals("Tail")) {
						updateIPset(ipAddr);
						
						synchronized(vc) {
							vc.setState(true);
						}
						
						rtt=calRTT.getRTT(masterIP);
						jValue=getJNum();		// first Number: 1				
						System.out.println("\n***************************************");
						System.out.println("[Server] Cmd:"+cmd+"	IpAddr:"+ipAddr);
						System.out.println("[Server] Tail RTT : "+rtt+" J Num : "+jValue);
						System.out.println("***************************************\n");
						
						Thread.sleep((long)(jValue*rtt));
						
						if (jValue == (ipAddrs.size()-1)) {
							sendMsg = "NewTail/"+getIpAddr();
							oos.writeObject(sendMsg);
							oos.flush();
						}
						
					}else if (cmd.equals("Non-Tail")) {
						updateIPset(ipAddr);
						
						synchronized(vc) {
							vc.setState(true);
						}
						
						rtt=calRTT.getRTT(masterIP);
						jValue=getJNum();		// first Number: 1				
						System.out.println("\n***************************************");
						System.out.println("[Server] Cmd:"+cmd+"	IpAddr:"+ipAddr);
						System.out.println("[Server] Non-Tail RTT : "+rtt+" J Num : "+jValue);
						System.out.println("***************************************\n");
						
						Thread.sleep((long)(jValue*rtt));
						
						if (jValue == 1) {
							sendMsg = "NewHead/"+getIpAddr();
							oos.writeObject(sendMsg);
							oos.flush();
						}
						
					}else if(cmd.equals("NewHead")){
						jValue=getJNum();
						System.out.println("\n***************************************");
						System.out.println("[Server] System Unsuspended !!!");
						System.out.println("[Server] Buffer List Size: [ "+bufferList.size()+" ]");	
						
						if (jValue == 1) {
							removeBuffer("Client");							
							System.out.println("[Server] Buffer List Size: [ "+bufferList.size()+" ]");
						}else {
							bufferList.clear();
							System.out.println("[Server] Buffer List Size: [ "+bufferList.size()+" ]");
						}
											
						System.out.println("***************************************\n");
						
						synchronized(vc) {
							vc.setRegen(true);
							//Thread.sleep(1000);
							vc.setState(false);
						}
					}else if(cmd.equals("NewTail")){
						jValue=getJNum();
						System.out.println("\n***************************************");
						System.out.println("[Server] System Unsuspended !!!");	
						System.out.println("[Server] Buffer List Size: [ "+bufferList.size()+" ]");	
						
						if (jValue == 1) {
							removeBuffer("Client");
						}else if (jValue == ipAddrs.size()) {
							System.out.println("[Server] Tail Buffer removed !!!");
							bufferList.clear();
							System.out.println("[Server] Buffer List Size: [ "+bufferList.size()+" ]");
						}else {
							bufferList.clear();
							System.out.println("[Server] Buffer List Size: [ "+bufferList.size()+" ]");
						}
						System.out.println("***************************************\n");
						
						synchronized(vc) {
							vc.setRegen(true);
							//Thread.sleep(1000);
							vc.setState(false);
						}
					}else {
						//System.out.println("[Server] Unsuspended !!!");
						synchronized(vc) {
							vc.setState(false);
						}
					}
				}
			}
		
		}catch(Exception e) {
			e.printStackTrace();
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
	    	cmd=st.nextToken().trim();
	    	//System.out.println("[Server] Fail Cmd: "+cmd);
	    }
	    
	    if (st.hasMoreTokens()) {
	    	ipAddr=st.nextToken().trim();
	    	//System.out.println("[Server] Fail ipAddr: "+ipAddr);
	    }
	    		
	}
	
	public int getJNum() {
		int nodeNum=0;
		String ipAddr;
		
		ipAddr = getIpAddr();
		
		for (int j=0; j<ipAddrs.size(); j++) {
			System.out.println("[Server] ipAddr ["+j+"]:"+ipAddrs.get(j));
			if (ipAddr.equals(ipAddrs.get(j))) {
				nodeNum = j;
			}
		}
		
		return nodeNum+1;
		
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
	
	public void removeBuffer(String type) {
		MSG tempMsg = null;
		System.out.println("[Server] Buffer removed without head !!!");
		for (int i=0; i<bufferList.size(); i++) {
			tempMsg = bufferList.get(i);
			if (tempMsg.getChannelType().equals(type)){
				
			}else {
				bufferList.remove(i);
			}
		}
		System.out.println("[Server] Buffer List Size: [ "+bufferList.size()+" ]");
		
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
	
	protected String chType = null, cmd = null, nodeID = null, objNum = null;
	protected String tempString = null, clientIP = null;
	protected String fileName = null, hostName = null, readFileName = null;
	protected String recvFileName = null;
	long timeStamp[]= new long[5];	
	long seqNum = 0;
	
	boolean commitOK = false;
	static MSG AckMsg = new MSG();
	
	List <String> ipAddrSet_Server; 
	List <ObjectOutputStream> S_ObjStreamList;	
	List <MSG> msgBufferList;
	VectorClock vc;
	
	public ServerThread(Socket s, VectorClock vc, List <MSG> bufferList, List <String> ipSet ){
		child = s;
		this.vc = vc;
		this.msgBufferList = bufferList;		
		S_ObjStreamList = new ArrayList<ObjectOutputStream>();
		ipAddrSet_Server = ipSet;	
		
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
		MSG bufData = null;
		
		try{
			while(true){				
				receiveData = (MSG) ois.readObject();
				if (receiveData != null){
					bufData = parseCmd(receiveData);
					
					if (chType.equals("Client")) {
						msgBufferList.add(receiveData);
						
						synchronized(vc) {
							if (!vc.getState()) { //#true: suspending mode
								setClockTick();
								serverConnect("Server", timeStamp); // Multicast;
							}
						}
					}else if (chType.equals("Server")) {
						msgBufferList.add(receiveData);
						
						synchronized(vc) {
							if (!vc.getState()) { //#true: suspending mode
								setClockTick();
								vectorClk(timeStamp);
								serverConnect("Tail", timeStamp);	// Unicast;
							}
						}
						
					}else if (chType.equals("Tail")) {											
						
						long [] sendTStamp = new long[5]; 
						sendTStamp=vectorClkT(timeStamp, receiveData);
						
						if (commitOK) {												
														
							clientConnect(clientIP, cmd, recvFileName);		
							
							hostName = getHostName();							
							serverConnect("Ack", sendTStamp);
														
							synchronized(vc) {
								vc.setTseqNum(vc.getTseqNum()+1);
							}
							
							commitOK = false;
						}
						
					}else if (chType.equals("Ack")) {
						
						String tempMsg= "< nodeID"+bufData.getNodeID()+"> < seq_num:"+bufData.getSeqNum()+
							"> < Obj:"+bufData.getObjNum()+"> < CSEQ:"+bufData.getTSeqNum()+" >";
						hostName = getHostName();						
						fileOperation(hostName+"_"+recvFileName, tempMsg);
						updateTseq(bufData.getTSeqNum());
						commitBuffer(bufData);						
						
					}
					
					synchronized(vc) {
						if (vc.getRegen()) { //#true: suspending mode
							vc.setRegen(false);
							Thread.sleep(50);
							serverRegenfunc();							
						}						
						Thread.sleep(100);
					} 
															
					if(receiveData.equals("/quit"))
						break;
					}
				Thread.sleep(200);
				break;
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

	public String getTName() {
	    Thread t = Thread.currentThread();
	    String name = t.getName();
	    //System.out.println("name=" + name);
	    return name;
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
				synchronized(vc) {
					vc.lClkTick(); // update receiving message.
					
					if(vc.getVClk(myID)<vc.getLClk()) {
						vc.setVClk(myID, vc.getLClk());
					}
					System.out.println("[VectorClock] Time Stamp["+myID+"]: "+vc.getLClk());
				}				
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
	
	public void commitBuffer(MSG receiveData) {
		MSG tempMsg;
		
		System.out.println();		
		System.out.println("[Server: ACK] [Commit Buffer] Client IP:"+receiveData.getClientIP()+
				"\tSeqNum: "+receiveData.getSeqNum());
		
		if (msgBufferList.size()>0) {		
			
			for (int i=0; i<msgBufferList.size(); i++) {
				tempMsg = msgBufferList.get(i);
				System.out.println("[Server: ACK] Ip Address: "+tempMsg.getClientIP()+
						"\tSeqNum: "+tempMsg.getSeqNum());
				if ((receiveData.getClientIP().equals(tempMsg.getClientIP()))
						&&(receiveData.getSeqNum()==tempMsg.getSeqNum())){
					msgBufferList.remove(i);					
					System.out.println("[Server: ACK] Message Remove [ "+i+" ]");
				}
			}			
			
		}
		System.out.println();
		
	}
	
	public void serverRegenfunc() {
		int myID = getMyID();
		MSG rengenData = null;
		
		System.out.println("");
		System.out.println("****************** System Recovery Start ***************");	
		System.out.println("");
		
		for (int i =0; i<msgBufferList.size();i++) {
			rengenData = msgBufferList.get(i);
			parseCmd(rengenData);
			
			if (chType.equals("Client")) {			
				
				synchronized(vc) {
					if (!vc.getState()) { //#true: suspending mode
						setClockTick();
						serverConnect("Server", timeStamp); // Multicast;
					}
				}
			}
			System.out.println("****************** System Recovery End ***************");	
			System.out.println("");			
			
		}
		
		
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
			synchronized(vc) {
				vcTimeStamp = vc.getLClk();
			}
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
					message.setSeqNum(seqNum);
					
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
			synchronized(vc) {
				vcTimeStamp = vc.getLClk();
			}
			System.out.println("[Server] My ID: [ "+myID+
					" ] Vector Time Stamp: [ "+ vcTimeStamp +" ]");
						
			try{			
				int tail = ipAddrSet_Server.size()-1;
				
				MSG message =new MSG();
				
				message.setChannelType(chType);
				message.setCmd(cmd);
				message.setNodeID(nodeID);
				message.setClientIP(clientIP);
				synchronized(vc) {
					for(int j=0; j<ipAddrSet_Server.size(); j++) {
						message.setTimeStamp(j, vc.getVClk(j));
					}
				}
				message.setObjNum(Integer.valueOf(objNum));
				message.setFileName(recvFileName);
				message.setMsgString(tempString);
				message.setSeqNum(seqNum);
				
				client = new Socket(ipAddrSet_Server.get(tail), port1);
				
				oos_S = new ObjectOutputStream(client.getOutputStream());			
				
				System.out.println("[Server] Tail Channel ["+ipAddrSet_Server.get(tail)+"]"+" Sending request !!!");	
								
				oos_S.writeObject(message);
				oos_S.flush();
							
				Thread.sleep(100);
				oos_S.close();
				client.close();				
				
			}catch(Exception e){
					System.out.println("[Server] Server connection not created !!!");
			}
		}else if(channelT.equals("Ack")) {
			chType=channelT;
			
			myID=getMyID();
			synchronized(vc) {
				vcTimeStamp = vc.getLClk();
			}
			System.out.println("[Server] My ID: [ "+myID+
					" ] Vector Time Stamp: [ "+ vcTimeStamp +" ]");
			
			try{
				for (int i = 0; i<ipAddrSet_Server.size(); i++){
					
					MSG message =new MSG();
					
					message.setChannelType(chType);
					message.setCmd(AckMsg.getCmd());
					message.setNodeID(AckMsg.getNodeID());
					message.setClientIP(AckMsg.getClientIP());
					for(int j=0; j<ipAddrSet_Server.size(); j++) {
						message.setTimeStamp(j, sendTStamp[j]);
					}
					message.setObjNum(Integer.valueOf(AckMsg.getObjNum()));
					message.setFileName(AckMsg.getFileName());
					message.setMsgString(AckMsg.getMsg());
					message.setSeqNum(AckMsg.getSeqNum());
					synchronized(vc){
						message.setTSeqNum(vc.getTseqNum());
					}
					ipAddr = ipAddrSet_Server.get(i);
					
					if (ipAddr.equals(getIpAddr())) {
						if (i == (ipAddrSet_Server.size()-1)) {
							String tempMsg= "< nodeID"+AckMsg.getNodeID()+"> < seq_num:"+AckMsg.getSeqNum()
								+"> < Obj:"+AckMsg.getObjNum()+"> < CSEQ:"+message.getTSeqNum()+">";
							hostName = getHostName();					
							fileOperation(hostName+"_"+recvFileName, tempMsg);
						}
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
				
				message.setFileName(recvFileName);
				message.setMsgString(tempString);
				message.setSeqNum(seqNum);
				synchronized(vc){
					message.setTSeqNum(vc.getTseqNum());
				}
			}else if (cmdType.equals("WRITE")){
				
				tempMsg = "<"+nodeID+"> < Obj:"+objNum+">";
				fileOperation(fileNameT, tempMsg);
				message.setChannelType("Server_Ack");
				message.setCmd("WRITE_GRANT");
				message.setNodeID(nodeID);
				message.setClientIP(clientIP);			
				message.setObjNum(Integer.valueOf(objNum));		
				
				message.setFileName(recvFileName);
				message.setMsgString(tempString);
				message.setSeqNum(seqNum);
				synchronized(vc){
					message.setTSeqNum(vc.getTseqNum());
				}
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
	
	public MSG parseCmd(MSG rcvData){
		
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
			
			if (rcvData.getSeqNum() != 0){							
				seqNum = rcvData.getSeqNum() ;
				System.out.println("[Server] Seq Num : "+seqNum);
			}
			
			if (rcvData.getMsg() != null){							
				tempString = rcvData.getMsg() ;
				System.out.println("[Server] Message : "+tempString);
			}
			
			
		}
				
		System.out.println();
		
		return rcvData;
	}
	
	public long[] vectorClkT(long[] timeS, MSG receiveData) {		
		MSG tempMsg1 = null;
		MSG tempMsg2 = null;
		
		//int tail = ipAddrSet_Server.size()-1;
		int interNodeNum = ipAddrSet_Server.size()-2;
		int updateFlag=0, msgCounter=0;
		
		long sendTStamp[] = new long[5];
		
		synchronized(vc) {			
			
			vc.vClockBlock();		
			
			msgBufferList.add(receiveData);
					
			vectorClk(timeS);
			System.out.println("[Tail Server] Inter Node Number : "+interNodeNum);	
			if (msgBufferList.size()>interNodeNum) {
				tempMsg1 = msgBufferList.get(0);
				System.out.println("[Tail server] Ip Address: "+tempMsg1.getClientIP());
				
				for (int i=1; i<msgBufferList.size(); i++) {
					tempMsg2 = msgBufferList.get(i);					
					if ((tempMsg1.getClientIP().equals(tempMsg2.getClientIP()))
							&&(tempMsg1.getSeqNum()==tempMsg2.getSeqNum())){
						msgCounter++;
						System.out.println("[Tail server] Message Counter: "+msgCounter);
					}
				}
				
				if (msgCounter>=interNodeNum-1) {
					for (int i=msgBufferList.size()-1; i>=0; i--) {
						tempMsg2 = msgBufferList.get(i);					
						if ((tempMsg1.getClientIP().equals(tempMsg2.getClientIP()))
								&&(tempMsg1.getSeqNum()==tempMsg2.getSeqNum())){
							msgBufferList.remove(i);
							System.out.println("[Tail server] Message Remove [ "+i+" ]");							
						}
					}
					updateFlag=1;
					commitOK=true;
					AckMsg = tempMsg1;
				}
			}
				
							
			if (updateFlag==1){
				
				setClockTick();
				updateVC();
				System.out.println();
				
				sendTStamp = vc.getVclk();
			}		
				
				vc.vClockUnblock();
			}
		
		return sendTStamp;
	}

	public void vectorClk(long[] timeS) {
		System.out.println("[VectorClock] Vector Clock Update....");
		
		synchronized(vc) {
			for(int i=0; i<ipAddrSet_Server.size(); i++) {
				if (vc.getVClk(i)< timeS[i]) {
					vc.setVClk(i, timeS[i]);
				}			
			}
		}
		updateVC();
		System.out.println("");
	}
	
	public void updateVC() {
		String ipAddr = null;		
		
		System.out.print("[VectorClock] :");
		for(int i=0; i<ipAddrSet_Server.size(); i++) {			
			ipAddr = ipAddrSet_Server.get(i);
			if (ipAddr.equals(getIpAddr())) {
				if (vc.getVClk(i) < vc.getLClk())
					vc.setVClk(i, vc.getLClk());
			}
			System.out.print("["+vc.getVClk(i)+"]");
		}
					
	}
	
	public void updateTseq(long timeSeq) {
		System.out.println("[VectorClock] Tail Sequence Update....");
		
		synchronized(vc) {
			if (vc.getTseqNum() < timeSeq) {
				vc.setTseqNum(timeSeq+1);
			}			
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
	private static boolean suspend = false;
	private static boolean reGen = false;
	private static long tSeqNum =0;
	
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
	
	public boolean getState() {
		return suspend;
	}
	
	public void setState(boolean flag) {
		suspend = flag;
	}
	
	public boolean getRegen() {
		return reGen;
	}
	
	public void setRegen(boolean flag) {
		reGen = flag;
	}
	
	public long getTseqNum() {
		return tSeqNum;
	}
	
	public void setTseqNum(long mNum) {
		tSeqNum = mNum;
	}
	
	
}

