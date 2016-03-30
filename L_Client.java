
import java.net.*;
import java.io.*;
import java.util.*;

/*
 * CS 6378: Advanced Operating System
 * Project 1
 * Topic: Lamport Mutual Exclusion Implementation.
 * 
 */

public class L_Client{
	protected String ipAddress=null, hostName=null;
	static String myHostName = null;
	static final int port = 6025;
	protected Socket client = null;
	protected BufferedReader read;
	protected ObjectOutputStream oos_C;
	protected ObjectOutputStream oos_S;
	
	protected String sendData, eventT, receiveData;
	
	protected String cmd = null, nodeID= null, fileName = null, eventType = null;
	protected long timeStamp=0;
	protected int numOfNode=4;
	
	protected boolean endflag=false;
	protected boolean expflag=false;
	
	String [] fileNameA = new String [3];
	
	static LogicalClock lc = new LogicalClock();
	MsgQueue msgQ = new MsgQueue();	
	List <String> ipAddrSet = getIpAddrAll();
	List <String> ipAddrSet_Server = new ArrayList<String>(); 
 	
	List <ObjectOutputStream> S_ObjStreamList = new ArrayList<ObjectOutputStream>();	
	
	static List <ReqMsg> reqList = new ArrayList<ReqMsg>();
	static List <ReqMsg> repList = new ArrayList<ReqMsg>();
	static List <MsgQueue> bufferList = new ArrayList<MsgQueue>();
	
	public L_Client(){
		
		try{
			System.out.println("******* Start Client *******");
						
			InetAddress addr = InetAddress.getLocalHost();        
						
			// Get IP Address
		    byte[] ipAddr = addr.getAddress();
		    System.out.println("[Client] Client Node ID : "+ipAddr[3]);
		    hostName = Byte.toString(ipAddr[3]);			
		    myHostName = Byte.toString(ipAddr[3]);
		    
			serverConnect();
			
			sendMsgSvr("/"+hostName+"/Init Message/");
			Thread.sleep(500);
			
			checkFileList();
				
			Thread.sleep(2000);
			L_ClientThread lCt = new L_ClientThread(ipAddrSet, bufferList, reqList, 
					lc, fileNameA);				
			Thread l_CT = new Thread(lCt);				
			l_CT.start();
			
			sendData = "keep going";
			while(true){				
				
				lamportMutex();	
				
				Thread.sleep(1000);
				
				if(sendData.equals("/quit")){
					endflag = true;
					break;
				}
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
		
		ipAddrSet_Server.add("10.176.67.83");		
		ipAddrSet_Server.add("10.176.67.84");
		ipAddrSet_Server.add("10.176.67.85");
		
		try{
			for (int i = 0; i<ipAddrSet_Server.size(); i++){
				client = new Socket(ipAddrSet_Server.get(i), port1);				
		
				oos_S = new ObjectOutputStream(client.getOutputStream());				
				S_ObjStreamList.add(oos_S);	
								
				ReceiveThread rt = new ReceiveThread(client, bufferList);				
				Thread t = new Thread(rt);				
				t.start();
				
				System.out.println("[Client] Server ["+ipAddrSet_Server.get(i)+"]"+" connected !!!");	
			
			}
		}catch(Exception e){
				System.out.println("[Client] Server connection not created !!!");
		}
	}
	
	/**
	 * Lamport Mutual Exclusion Algorithm
	 */
	
	public void lamportMutex(){
		String cmd = null, fileName = null, evtType = null, nodeID = null;
		ReqMsg temMsg = new ReqMsg();
		int file1=0, file2=0, file3=0, file4=0;
		
		System.out.println("[Client] R_BUFFER SIZE: "+repList.size());
		
		for (int i = 0; i<repList.size(); i++){	
			temMsg=repList.get(i);
			
			cmd=temMsg.getCmd();
			fileName=temMsg.getFileName();
			nodeID=temMsg.getNodeID();
			evtType = temMsg.getEventType();
			
			System.out.println("[Client] BUFFER[ "+i+" ] : [ "+nodeID+" ] [ "+
					cmd+" ] [ "+fileName+" ] [ "+evtType+" ]");
						
			if (fileName.equals(fileNameA[0])){
				file1++;			
				
				if (file1>=numOfNode){
					fileOperation(evtType, fileNameA[0]);				
				}
			}else if (fileName.equals(fileNameA[1])){
				file2++;				
				
				if (file2>=numOfNode){
					fileOperation(evtType, fileNameA[1]);				
				}
			}else if (fileName.equals(fileNameA[2])){
				file3++;				
				
				if (file3>=numOfNode){
					fileOperation(evtType, fileNameA[2]);				
				}
			}else if (fileName.equals("file4")){
				file4++;				
				
				if (file4>=numOfNode){
					fileOperation(evtType, fileNameA[3]);					
				}
			}			
					
		}		
		
	}
	
	/**
	 * Lamport Mutual Exclusion Algorithm's sub part.
	 * @param evtTypeF
	 * @param fileName
	 */
	
	public void fileOperation(String evtTypeF, String fileName){
		ReqMsg filReqMsg = new ReqMsg();
		if (!reqList.isEmpty()){
			filReqMsg = reqList.get(0);
			String fnodeID = filReqMsg.getNodeID();
			
			if (fnodeID.equals(myHostName)){
				if (evtTypeF.equals("READ")){			
					removeRepBuf(fileName, evtTypeF);
					serverProc("/"+myHostName+"/"+evtTypeF+"/"+fileName+"/"+
							lc.getLclock()+"/", evtTypeF);			
				}else{
					removeRepBuf(fileName, evtTypeF);
					serverProc("/"+myHostName+"/"+evtTypeF+"/"+fileName+"/"+
							lc.getLclock()+"/This is message!!!/", evtTypeF);			
				}	
			}
			
		}else{
			return ;
		}		
		
	}
	
	/**
	 * Server's meta data.
	 */
	
	public void checkFileList(){
		String evtTypeR = null, fileNameR=null;
		MsgQueue msgQ = new MsgQueue();			
		
		for (int i = 0; i<bufferList.size(); i++){	
			msgQ=bufferList.get(i);			
			
			evtTypeR=msgQ.getEvtType();
			fileNameR=msgQ.getfileName();					
			
			if (evtTypeR.equals("INIT_GRANT")){				
				parseFileL(fileNameR);
				bufferList.remove(i);
			}
		}		
	}
				
	public void serverProc(String msg, String evtType){
		
		try{			
			if (evtType.equals("READ")){
				sendMsgSvrUni(msg);
			}else{
				sendMsgSvr(msg);
			}			
		}catch (Exception e){}
	}	
	
	public void sendMsgSvr(String message){	
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
	
	public void sendMsgSvrUni(String message){	
		Random randGen = new Random();
		int i=0;
		
		try{
			i=randGen.nextInt(3);					
			oos_S = S_ObjStreamList.get(i);
			oos_S.writeObject(message);
			oos_S.flush();				
		}catch(IOException e){
			System.out.println("[Client] Server connection closed");
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
		 String toMsg=null, tempBuf=null, strLine=null;		
		
		 try {
			 FileInputStream fstream = new FileInputStream("ClientList.conf");
			  
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
	         System.out.println("[Client] Client List : "+tempBuf);
	         list.add(tempBuf);
	     }		
		
		return list;    
      
	}
		
	/**
	 * Buffer remove function. 
	 * @param fileName
	 * @param evtType
	 */
	
	public void removeRepBuf(String fileName, String evtType) {
		ReqMsg tmpReqMsg = new ReqMsg();
		
		for (int j=0; j<numOfNode; j++){
			for (int i=0; i< repList.size(); i++){
				tmpReqMsg = repList.get(i);
				if (tmpReqMsg.getFileName().equals(fileName)&&
						tmpReqMsg.getEventType().equals(evtType)){
					synchronized(repList){
						repList.remove(i);	
					}					
					System.out.println("[Client] [ Delete:REPLY ]: [fileName: "+
							fileName+" ] [Event type: "+evtType+" ]");
				}
			}
		}
	}
	
	public static void main(String[] args){		
		try{
			
			L_CServer lCS = new L_CServer(reqList, repList, lc);				
			Thread tCS = new Thread(lCS);				
			tCS.start();
			
			Thread.sleep(3000);
			
			new L_Client();
			
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
 * This thread is communication part between client and client.
 * @author joonglyul
 *
 */

class L_ClientThread implements Runnable{
	Socket client;
	ObjectOutputStream oos_C;
	
	String receiveData = null, eventT = null, ackMsg = null, cmdMsg = null,
			fileName = null, fileString = null;	
	String sendData = null, hostName = null;
	
	int port = 6025;
	
	String [] fileNameA;
	MsgQueue msgQ;
	LogicalClock lc;
	
	List <String> ipAddrSet;
	List <MsgQueue> bufferList;	
	List <ReqMsg> reqList;
	List <ObjectOutputStream> ObjStreamList = new ArrayList<ObjectOutputStream>();
	
	int id = 0;
	
	public L_ClientThread(List <String> ipAddrSet, List <MsgQueue> bufferList, 
			List <ReqMsg> reqList, LogicalClock lc, String [] fileNameA){
		this.ipAddrSet = ipAddrSet;
		this.bufferList = bufferList;
		this.reqList = reqList;
		this.lc = lc;
		this.fileNameA = fileNameA;
			
	}	
	
	public void run(){
		try{
			hostName = getHostName();
			int randInt = 0, randfile = 0;			
			
			String dummy[]={"READ", "WRITE","WRITE", "READ", "WRITE","WRITE", "READ"};
						
			Random randGen = new Random();
			clientConnet();
			
			while(true){
				
				randInt = randGen.nextInt(3);
				randfile = randGen.nextInt(7);
				checkRelease();
				
				Thread.sleep(1000);
				long t_Time = lc.getLclock();
				sendData="/"+hostName+"/REQUEST/"+fileNameA[randInt]+"/"+lc.getLclock()+"/"+dummy[randfile]+"/";
				
				ReqMsg reqestMsg = new ReqMsg();				
				synchronized(reqestMsg){
					reqestMsg.setReqMsg(hostName, "REQUEST", fileNameA[randInt], t_Time, dummy[randfile]);
					reqestMsg.setProcFlag(1);
				}
				
				synchronized(reqList){
					reqList.add(reqestMsg);	
				}
				clientProc(sendData); 	// send request message for client				
				Thread.sleep(1000);
				
				checkJob(); 			// reply for client request.
				Thread.sleep(1000);				
					
			}				
		}catch(Exception e){
			e.printStackTrace();
		}
			finally{
				try{
					oos_C.close();
					//client.close();
				}catch(IOException e2){
					e2.printStackTrace();
				}
			}
	}
	
	public String getHostName(){
		String hostName = null;
		
		try{		
			InetAddress addr = InetAddress.getLocalHost();        
						
			// Get IP Address
		    byte[] ipAddr = addr.getAddress();
		    System.out.println("[Client] Client Node ID : "+ipAddr[3]);
		    hostName = Byte.toString(ipAddr[3]);
		}catch(Exception e){
			e.printStackTrace();
		}
	    return hostName;
	}
	
	public void clientConnet() {
		try{
			for (int i = 0; i<ipAddrSet.size(); i++){
				client = new Socket(ipAddrSet.get(i), port);				
		
				oos_C = new ObjectOutputStream(client.getOutputStream());				
				ObjStreamList.add(oos_C);							
								
				System.out.println("[Client] Client ["+ipAddrSet.get(i)+"]"+" connected !!!");	
			
			}
			
			sendMsg("/Init Message/"); // Initialization in Communication
			
		}catch(Exception e){
			System.out.println("[Client] Client Connection Error !!!");
		}			
	}
	
	/**
	 * when every client send message...
	 * @param message
	 */
	
	public void sendMsg(String message){	
		try{
			for (int i = 0; i<ObjStreamList.size(); i++){			
				oos_C = ObjStreamList.get(i);
				oos_C.writeObject(message);
				oos_C.flush();	
			}
		}catch(IOException e){
			System.out.println("[Client] Client closed");
		}		
	}
	
	/**
	 * when client send message only one node....
	 * @param message
	 * @param sNodeID
	 */
	
	public void sendMsg(String message, String sNodeID){	
		try{
			int jIndex = 0;
			String lastDigit = null;
			for (int i = 0; i<ipAddrSet.size(); i++){
				String addrIp = ipAddrSet.get(i).toString().trim();				
				String[] st = addrIp.split(".");				
				StringTokenizer st1 = new StringTokenizer(addrIp,".");
							
				while (st1.hasMoreTokens()) {			         
			         lastDigit = st1.nextToken();			        
			     }
				
				if (lastDigit.equals(sNodeID)){
					jIndex = i;					
					break;
				}					
			
			}
								
			oos_C = ObjStreamList.get(jIndex);
			oos_C.writeObject(message);
			oos_C.flush();	
			
		}catch(IOException e){
			System.out.println("[Client] client connection closed");
		}		
	}
	
	/**
	 * this function works for checking read grant and write grant message.
	 * and send RELEASE message to every client.
	 */
	public void checkRelease(){
		String ackMsgR = null, evtTypeR = null, stringMsgR = null, fileNameR=null;
		MsgQueue msgQ = new MsgQueue();			
		
		System.out.println("[Client] [ JOB LIST SIZE: "+bufferList.size()+" ]");
		
		for (int i = 0; i<bufferList.size(); i++){	
			msgQ=bufferList.get(i);
			
			ackMsgR=msgQ.getAckMsg();
			evtTypeR=msgQ.getEvtType();
			fileNameR=msgQ.getfileName();
			stringMsgR=msgQ.getfileString();
					
			if(evtTypeR.equals("READ_GRANT")||(evtTypeR.equals("WRITE_GRANT"))){
				removeReqBuf(hostName, fileNameR, "REQUEST", evtTypeR);				
				synchronized(bufferList){			
					bufferList.remove(i);
				}
				sendMsg("/"+hostName+"/"+"RELEASE"+"/"+fileNameR+"/"+lc.getLclock());
			}
		}		
	}
	
	/**
	 * this function works for checking request queue.
	 */
	public void checkJob(){
		String cmd = null, chfileName = null, chHostID = null, chevtT = null;
		ReqMsg temMsg = new ReqMsg();
		int flag=0;
		
		System.out.println("[Client] [ REQUEST QUEUE SIZE: "+reqList.size()+" ]");
		
		for (int i = 0; i<reqList.size(); i++){	
			temMsg=reqList.get(i);
			
			cmd = temMsg.getCmd();
			chfileName = temMsg.getFileName();
			flag = temMsg.getProcFlag();
			chHostID = temMsg.getNodeID();
			chevtT = temMsg.getEventType();
			
			System.out.println("[Client] QUEUE LIST [ "+i+" ] : [ "+chHostID+" ] [ "+
					cmd+" ] [ "+chfileName+" ] [ "+chevtT+" ]");
			
			if(cmd.equals("REQUEST")&&(flag ==0)){
				sendMsg("/"+hostName+"/"+"REPLY"+"/"+chfileName+"/"+lc.getLclock()+"/"+chevtT,chHostID);
				synchronized(temMsg){			
					temMsg.setProcFlag(1);
				}				
			}				
		}		
	}
	
	public void removeReqBuf(String nodeID, String fileName, String cmd, String evtT) {
		ReqMsg tmpReqMsg = new ReqMsg();
		
		for (int i=0; i< reqList.size(); i++){
			tmpReqMsg = reqList.get(i);
			if (tmpReqMsg.getNodeID().equals(nodeID)&&
					tmpReqMsg.getFileName().equals(fileName)&&
						tmpReqMsg.getCmd().equals(cmd)){
				synchronized(reqList){
					reqList.remove(i);	
				}				
				System.out.println("[Client] [ Delete:Request ]: [ NodeID: "+nodeID+
						" fileName: "+fileName+" Cmd: "+cmd+" ]");
			}
		}
	}
		
	public void clientProc(String msg){	
		
		try{
			
			sendMsg(msg); 			
			
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
}

/**
 *	This class is Lamport Mutex's Message Buffer.
 * @author joonglyul
 *
 */

class MsgQueue implements Serializable{
	protected String ackMsg = null;	
	protected String evtType = null;
	protected String fileName = null;
	protected String fileString = null;	
	
	synchronized void setMessage(String ackMsg, String evtType, 
			String fileName, String fileString){
		this.ackMsg = ackMsg;	
		this.evtType = evtType;
		this.fileName = fileName;
		this.fileString = fileString;		
	}
	
	synchronized void setAckMsg(String ackMsg){
		this.ackMsg = ackMsg;		
	}
	
	synchronized String getAckMsg(){
		return ackMsg;		
	}
	
	synchronized void setEvtType(String evtType){
		this.evtType = evtType;		
	}
	
	synchronized String getEvtType(){
		return evtType;		
	}
	
	synchronized void setFileName(String fileName){
		this.fileName = fileName;		
	}
	
	synchronized String getfileName(){
		return fileName;		
	}
	
	synchronized void setFileString(String fileString){
		this.fileString = fileString;		
	}
	
	synchronized String getfileString(){
		return fileString;		
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
	String receiveData = null, eventT = null, ackMsg = null, cmdMsg = null,
			fileName = null, fileString = null;	
	String mainSrvID = "83";
	MsgQueue msgQ;	
	List <MsgQueue> bufferList;
	
	int id = 0;
	
	public ReceiveThread(Socket s, List <MsgQueue> bufferList){
		try{
			client = s;				
			ois = new ObjectInputStream(s.getInputStream());
			this.bufferList = bufferList;			
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	
	public void run(){
		try{
			while(true){
				if ((receiveData = (String) ois.readObject()) != null){			
					System.out.println("[Client] [ID:"+id+"] receive data: "+receiveData);					
					if (receiveData != null){
						
						parseCmd(receiveData);					
						if (cmdMsg !=null){
							if (cmdMsg.equals("READ_GRANT")){						
								MsgQueue msgQ = new MsgQueue();								
								msgQ.setMessage(ackMsg, cmdMsg, fileName, fileString);
																
								synchronized(bufferList){
									bufferList.add(msgQ);	
								}
								
							}else if (cmdMsg.equals("WRITE_GRANT")){								
								if (ackMsg.trim().equals(mainSrvID)){	
									MsgQueue msgQ = new MsgQueue();								
									msgQ.setMessage(ackMsg, cmdMsg, fileName, fileString);
									
									synchronized(bufferList){
										bufferList.add(msgQ);	
									}
									
								}								
							}else if (cmdMsg.equals("INIT_GRANT")){	
								
								if (ackMsg.trim().equals(mainSrvID)){								
									MsgQueue msgQ = new MsgQueue();									
									msgQ.setMessage(ackMsg, cmdMsg, fileName, fileString);
									
									synchronized(bufferList){
										bufferList.add(msgQ);	
									}
								}																
							}else if (cmdMsg.equals("UNKNOWN COMMAND")){
															
							}
						}
					
						if(receiveData.equals("/quit"))
							break;
						}
					id++;				
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
					//client.close();
				}catch(IOException e2){
					e2.printStackTrace();
				}
			}
	}
	
	public void parseCmd(String rcvData){
		
		if (rcvData != null){
			StringTokenizer str = new StringTokenizer(rcvData, ":");
			
			if (str.hasMoreTokens() == true){						
				ackMsg = str.nextToken();
				System.out.print("[Client] [ "+ackMsg+" ] ");
			}
			
			if (str.hasMoreTokens() == true){						
				cmdMsg = str.nextToken();
				System.out.print("[ "+cmdMsg+" ] ");
			}
			
			if (str.hasMoreTokens() == true){						
				fileName = str.nextToken();
				System.out.print("[ "+fileName+" ] ");
			}
			
			System.out.println();
			
			if (str.hasMoreTokens() == true){						
				fileString = str.nextToken();
				System.out.println("[Client] [ READ Message : "+fileString+" ]");
			}			
		}		
	}
		
	
	public void condCheck() {
		
	}
	
}


/**
 * This class is client's server part to accept connection from the other client.
 * 
 * @author joonglyul
 *
 */


class L_CServer extends Thread{
	ServerSocket server;
	final static int port=6025;
	Socket child;	

	List <ReqMsg> reqList;
	List <ReqMsg> repList;
	
	LogicalClock lc;
	
	public L_CServer(List <ReqMsg> reqList, List <ReqMsg> repList, LogicalClock lc) {
		try{
			this.reqList = reqList;
			this.repList = repList;
			this.lc = lc;
			server = new ServerSocket(port);			
		}catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println("******** Client: Server Module Start ***********");
	}
	
	public void run(){
		while(true){
			try{
				child = server.accept();
				
				CServerThread childThread = new CServerThread(child, reqList, repList, lc);
				Thread t = new Thread(childThread);
				t.start();
				
			}catch(Exception e){
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
	
}

class CServerThread implements Runnable {
	Socket child;
	ObjectInputStream ois;	
	
	String nodeID = null, cmd = null, fileName = null, eventType = null;
	long timeStamp = 0;
	int nodeCt[] = new int[4];
	
	List <ReqMsg> reqList;
	List <ReqMsg> repList;
	LogicalClock lc;
	public CServerThread(Socket s, List <ReqMsg> reqList, 
			List <ReqMsg> repList, LogicalClock lc){
		child = s;
		this.reqList = reqList;
		this.repList = repList;
		this.lc = lc;
		
		try{
			System.out.println("Connected from"+child.getInetAddress());			
			ois = new ObjectInputStream(child.getInputStream());			
			
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void run(){
		
		String receiveData = null;
		
		try{
			while(true){
				receiveData = (String) ois.readObject();
				System.out.println("[Client] Recive Message: "+receiveData);
				if (receiveData != null){
					parseCmd(receiveData);					
					if (cmd !=null){
						if (cmd.equals("REQUEST")){						
							ReqMsg reqestMsg = new ReqMsg();
							reqestMsg.setReqMsg(nodeID, cmd, fileName, timeStamp, eventType);
														
							synchronized(reqList){
								reqList.add(reqestMsg);		
							}
							
							lc.setLclock(timeStamp);
							
						}else if (cmd.equals("REPLY")){							
							ReqMsg reqestMsg = new ReqMsg();
							reqestMsg.setReqMsg(nodeID, cmd, fileName, timeStamp, eventType);
														
							synchronized(repList){
								repList.add(reqestMsg);		
							}
							
							lc.setLclock(timeStamp);
							
						}else if (cmd.equals("RELEASE")){													
							removeBuf(nodeID, fileName, "REQUEST", eventType);
							
							lc.setLclock(timeStamp);
						}
					}
					if(receiveData.equals("/quit"))
						break;
					}
			}
		}catch(Exception e){
			System.out.println("[Client] Client closed");
		}
		finally{
			try{
				if(child !=null)
					child.close();
			}catch(Exception e){}
		}
	}	
	
	public void removeBuf(String nodeID, String fileName, String cmd, String evtT) {
		ReqMsg tmpReqMsg = new ReqMsg();
		
		for (int i=0; i< reqList.size(); i++){
			tmpReqMsg = reqList.get(i);
			if (tmpReqMsg.getNodeID().equals(nodeID)&&
					tmpReqMsg.getFileName().equals(fileName)&&
						tmpReqMsg.getCmd().equals(cmd)){
				synchronized(reqList){
					reqList.remove(i);	
				}				
				System.out.println("[Client] [ Delete:Request ]: [ NodeID: "+nodeID+
						" fileName: "+fileName+" Cmd: "+cmd+" ]");
			}
		}
	}
	
	public void parseCmd(String rcvData){
		
		if (rcvData != null){
			StringTokenizer str = new StringTokenizer(rcvData, "/");
			
			if (str.hasMoreTokens() == true){						
				nodeID = str.nextToken();
				System.out.println("[Client] [Node ID : "+nodeID+" ]");
			}
			
			if (str.hasMoreTokens() == true){						
				cmd = str.nextToken();
				System.out.println("[Client] [Comand : "+cmd+" ]");
			}
			
			if (str.hasMoreTokens() == true){						
				fileName = str.nextToken();
				System.out.println("[Client] [File Name : "+fileName+" ]");
			}
			
			if (str.hasMoreTokens() == true){						
				timeStamp = Long.parseLong(str.nextToken());			
				System.out.println("[Client] [Time stamp : "+timeStamp+" ]");
			}
			
			if (str.hasMoreTokens() == true){						
				eventType = str.nextToken();
				System.out.println("[Client] [Event Type : "+eventType+" ]");
			}
			
			System.out.println();
			
		}		
	}	
	
}

/**
 * This class is Request Queue's data object.
 * @author joonglyul
 *
 */

class ReqMsg {
	
	protected String nodeID = null,cmd = null, fileName = null, eventT=null;
	protected long timeStamp= 0;
	protected int p_flag=0;
	
	synchronized void setReqMsg(String clientNum, String cmd, String fileName, long timeStamp, String eventT){
		
		this.nodeID = clientNum;
		this.cmd = cmd;
		this.fileName = fileName;
		this.timeStamp = timeStamp;
		this.eventT = eventT;
	}
	
	synchronized String getNodeID(){
		return nodeID;		
	}
	
	synchronized String getCmd(){
		return cmd;		
	}
	
	synchronized String getFileName(){
		return fileName;		
	}
	
	synchronized long getTimeStamp(){
		return timeStamp;		
	}
	
	synchronized String getEventType(){
		return eventT;		
	}
	
	synchronized int getProcFlag(){
		return p_flag;		
	}
	
	synchronized void setProcFlag(int flag){
		this.p_flag = flag;		
	}
	
}