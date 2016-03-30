import java.io.*;
import java.net.*;
import java.util.*;

/*
 * CS 6378: Advanced Operating System
 * Project 3
 * Module: Master Server
 * Topic: Dynamic Replication.
 * 
 */

public class MasterServer {
	
	protected ServerSocket server;
	protected Socket child;
	
	final static int portM = 7588;	
	
	protected List <String> svrIpList;
	final static NotifyData nfData = new NotifyData();
	final static List <String> ipList = new ArrayList<String>();
	final static List <Socket> socketList = new ArrayList<Socket>();
	final static List <ObjectOutputStream> objectList = new ArrayList<ObjectOutputStream>();
	
	final static List <Socket> clientsktList = new ArrayList<Socket>();
	final static List <ObjectOutputStream> clientobjList = new ArrayList<ObjectOutputStream>();
	
	public MasterServer() {
		try {
			server = new ServerSocket (portM);
			svrIpList = getIpAddrAll();
		}catch(IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void masterMain() {
		
		String ipAddr= null;
		boolean svrFlag = false;
		
		try {
			System.out.println("[MasterServer] ============ Start =========== ");
			
			MasterThread mt = new MasterThread(socketList, objectList, ipList,
					clientsktList, clientobjList, nfData);
			Thread t = new Thread(mt);
			t.start();
		
			while(true) {
				
				child = server.accept();
				ipAddr = child.getInetAddress().getHostAddress();			
				System.out.println("[MasterServer] [Connected IP Address: "+ipAddr+" ]");
				
				for(int i =0; i<svrIpList.size(); i++) {
					if (ipAddr.equals(svrIpList.get(i))) {
						socketList.add(child);
						ipList.add(child.getInetAddress().getHostAddress());
						objectList.add(new ObjectOutputStream(child.getOutputStream()));
						
						MasterRcvThread mrt = new MasterRcvThread(new ObjectInputStream(child.getInputStream()), nfData);
						Thread mrtTh = new Thread(mrt);
						mrtTh.start();
						
						System.out.println("[MasterServer] [Server IP Address: "+ipAddr+" ]");
						svrFlag = true;
					}
				}
				
				if (!svrFlag) {					
					
					clientsktList.add(child);					
					clientobjList.add(new ObjectOutputStream(child.getOutputStream()));
					
					System.out.println("[MasterServer] [Client IP Address: "+ipAddr+" ]");
					
				}
				
				svrFlag = false;
				
			}
		
		}catch(IOException e) {
			e.printStackTrace();			
		}
	}
	
	List <String> getIpAddrAll(){
		
		 List <String> list = new ArrayList<String>();
		 	
		 //***** File-servers list *****		
		 
		 list.add("10.176.67.74");		
		 list.add("10.176.67.75");
		 list.add("10.176.67.76");
		 list.add("10.176.67.77");
		 list.add("10.176.67.78");
		
		return list;    
    
	}
	
	public String getIpAddr() {
		String ipAddr = null;
		try {
			InetAddress addr = InetAddress.getLocalHost();
			ipAddr = addr.getHostAddress();		
		}catch(Exception e) {
			
		}
		System.out.println("[MasterServer] My ipAddr : "+ipAddr);
		return ipAddr;
	}
	
	public static void main(String[] args) {
		MasterServer ms = new MasterServer();
		ms.masterMain();
	}
}

class MasterThread implements Runnable {
	protected Socket child;
	//protected ObjectInputStream ois;
	protected ObjectOutputStream oos;
	protected int maxNum = 0, tempNum =0, diff = 0;
	
	NotifyData nfData;
	List <String> ipAddrList;
	List <Socket> socketList;	
	List <ObjectOutputStream> objectList;
	
	List <Socket> clientsktList;
	List <ObjectOutputStream> clientobjList;
	
	public MasterThread(List <Socket> chList, List <ObjectOutputStream> objList, 
			List <String> ipList, List <Socket> clSokList, 
			List <ObjectOutputStream> clobjList, NotifyData nfData) {
		ipAddrList = ipList;
		socketList = chList;
		objectList = objList;
		
		clientsktList = clSokList;
		clientobjList = clobjList;
		maxNum = chList.size();
		
		this.nfData = nfData;
	}
	
	public void run() {		
		
		try {
			while(true) {
				maxNum = objectList.size();			
				
				if(maxNum !=0) {
					for(int i = 0; i<maxNum; i++) {
						//child = socketList.get(i);
											
						keepAliveNode(i);					
					}
				}
				
				System.out.println(" ");
				Thread.sleep(200);				
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void keepAliveNode(int nodeNum) {
		
		System.out.println("[Master Server] Keep Alive ....  "+nodeNum);			
		
		try {			
			oos = objectList.get(nodeNum);
			//oos.writeObject("Keep Alive !!! [ "+ipAddrList.get(nodeNum)+" ]");
			oos.writeObject("...");
			oos.flush();			
		}catch(Exception e) {
			
			socketList.remove(nodeNum);
			objectList.remove(nodeNum);		
			notifyNode(nodeNum);
			
			System.out.println("[Master Server] Server closed [ "+ipAddrList.get(nodeNum)+" ] ");
			maxNum = 0;	
			ipAddrList.remove(nodeNum);
			//e.printStackTrace();
		}		
		
	}
	
	public void errorDetection(int nodeNum) {

		socketList.remove(nodeNum);
		objectList.remove(nodeNum);		
		notifyNode(nodeNum);
		
		System.out.println("[Master Server] Server closed [ "+ipAddrList.get(nodeNum)+" ] ");
		maxNum = 0;	
		ipAddrList.remove(nodeNum);
		
	}
	
	public void notifyNode(int nodeNum) {
		int maxNum = objectList.size();
		int tempNodeNum = 0;
		
		String msg = null;
		System.out.println("[Master Server] Node Num: "+nodeNum+
				" Server IPAddr: "+ipAddrList.get(nodeNum)+
				" Node Max: "+maxNum);			
		
		if (nodeNum == maxNum) {
			msg="Tail / "+ipAddrList.get(nodeNum)+"";
		}else {
			msg="Non-Tail / "+ipAddrList.get(nodeNum)+"";
		}
		
		try {
		
			for (int j=0; j<objectList.size(); j++) {	
				tempNodeNum = j;
				oos = objectList.get(j);
				oos.writeObject(msg);
				oos.flush();
			}
			
			for (int j=0; j<clientobjList.size(); j++) {
				tempNodeNum = j;
				oos = clientobjList.get(j);
				oos.writeObject(msg);
				oos.flush();
			}
			
		}catch(Exception e) {
			//e.printStackTrace();
			//errorDetection(tempNodeNum);
		}
		
		releaseNode();
	}
	
	public void releaseNode() {
		
		String msg = null;
		boolean flag = true;
		
		try {
		
			while(flag) {				
				synchronized(nfData) {
					if (nfData.getEvent() !=null) {
						System.out.println(nfData.getEvent());
						flag = false;
						Thread.sleep(100);
					}
				}				
			}
		
			System.out.println("[Master Server] Event : "+nfData.getEvent()+
					" New IPAddr: "+nfData.getIpAddr());		
			msg = nfData.getEvent()+" / "+nfData.getIpAddr();
				
			for (int j=0; j<objectList.size(); j++) {				
				oos = objectList.get(j);
				oos.writeObject(msg);
				oos.flush();
			}
			
			for (int j=0; j<clientobjList.size(); j++) {				
				oos = clientobjList.get(j);
				oos.writeObject(msg);
				oos.flush();
			}
			
		}catch(Exception e) {
			//e.printStackTrace();
			System.out.println("[Master Server] Network Connection Closed !!!");
		}
		
		synchronized(nfData) {
			nfData.setEvent(null);
			nfData.setIpAddr(null);
		}
		
	}
	
	public void getObject(int maxNum, int diff) {
		
		try {
			if (diff>0) {
				for (int i=maxNum-diff; i<maxNum; i++) {
					child = socketList.get(i);
					oos = new ObjectOutputStream(child.getOutputStream());
					oos.writeObject("Initiate Message !!! [ "+ipAddrList.get(i)+" ]");
					oos.flush();
					objectList.add(oos);
				}
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
}

class MasterRcvThread implements Runnable {		
	protected ObjectInputStream ois=null;
	
	protected List <String> ipAddrs; 
	NotifyData nfData;
	
	String cmd = null, ipAddr = null;
	String sendMsg = null;
		
	public MasterRcvThread(ObjectInputStream ois,NotifyData nfData){		
		this.ois = ois;
		this.nfData = nfData;
	}
	
	public void run() {
		
		String receiveData = null;		
		
		try {
					
			while(true) {
				receiveData = (String) ois.readObject();
				if (receiveData != null){
					System.out.println("[Master Server] File Server : "+receiveData);
					parseCmd(receiveData);
					if (cmd.equals("NewHead")) {
						System.out.println("[Master Server] New Head Elected !!!");	
						synchronized(nfData) {
							nfData.setEvent(cmd);
							nfData.setIpAddr(ipAddr);
						}
						
					}else if (cmd.equals("NewTail")) {
						System.out.println("[Master Server] New Tail Elected !!!");
						synchronized(nfData) {
							nfData.setEvent(cmd);
							nfData.setIpAddr(ipAddr);
						}								
						
					}else {
						System.out.println("[Master Server] The others  !!!");
						synchronized(nfData) {
							nfData.setEvent(cmd);
							nfData.setIpAddr(ipAddr);
						}
					}
				}
			}
		
		}catch(Exception e) {
			System.out.println("[Master Server] File Server Connection closed !!!");
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
	    	System.out.println("[Master Server] Fail Cmd: "+cmd);
	    }
	    
	    if (st.hasMoreTokens()) {
	    	ipAddr=st.nextToken().trim();
	    	System.out.println("[Master Server] Fail ipAddr: "+ipAddr);
	    }
	    		
	}
	
	public int getJNum() {
		int nodeNum=0;
		String ipAddr;
		
		ipAddr = getIpAddr();
		
		for (int j=0; j<ipAddrs.size(); j++) {
			System.out.println("[Master Server] ipAddr ["+j+"]:"+ipAddrs.get(j));
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
		System.out.println("[Master Server] My ipAddr : "+ipAddr);
		return ipAddr;
	}
}

class NotifyData {
	private String event=null;
	private String ipAddr=null;
	
	public void setEvent(String evt) {
		event = evt;
	}
	
	public String getEvent() {
		return event;
	}
	
	public void setIpAddr(String ip) {
		ipAddr = ip;
	}
	
	public String getIpAddr() {
		return ipAddr;
	}
}
