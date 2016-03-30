
import java.net.*;
import java.util.*;
import java.io.*;

/*
 * CS 6378: Advanced Operating System
 * Project 1
 * Topic: Lamport Mutual Exclusion Implementation.
 * 
 */

public class L_Server{
	ServerSocket server;
	final static int port=6035;
	Socket child;	
	
	
	public L_Server() {
		try{
			server = new ServerSocket(port);			
		}catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println("******** Server Start ***********");
		
		while(true){
			try{
				child = server.accept();
				
				ServerThread childThread = new ServerThread(child);
				Thread t = new Thread(childThread);
				t.start();
				
			}catch(Exception e){
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
	
	public static void main(String[] args){
		new L_Server();
	}	
	
}

/**
 * This class is server's receive thread. 
 * @author joonglyul
 *
 */

class ServerThread implements Runnable {
	Socket child;
	ObjectInputStream ois;
	ObjectOutputStream oos;
	
	String cmd = null, nodeID = null, fileName = null, tempString = null, msg = null;
	long timeStamp= 0;
	
	
	public ServerThread(Socket s){
		child = s;		
		
		try{
			System.out.println("Connected from"+child.getInetAddress());
			
			oos = new ObjectOutputStream(child.getOutputStream());
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
				if (receiveData != null){
					parseCmd(receiveData);					
					
					if (cmd.equals("READ")){							
									
						tempString=readFile(fileName);						
						oos.writeObject("ACK:READ_GRANT:"+fileName+":"+tempString);
						oos.flush();
						
					}else if (cmd.equals("WRITE")){						
						
						String nodeNum = getHostName();
						tempString = "< "+nodeID+" > < "+timeStamp+" > " + msg;
						if(fileExist(fileName)){
							writeFile(fileName,tempString);
						}else{
							createFile(fileName,tempString);
						}						
						oos.writeObject(nodeNum+":WRITE_GRANT:"+fileName);
						oos.flush();					
						
					}else if (cmd.equals("Init Message")){						
						
						String nodeNum = getHostName();
						tempString=listFile();
						oos.writeObject(nodeNum+":INIT_GRANT:"+tempString);
						oos.flush();										
						
					}else {
						
						oos.writeObject("ACK:UNKNOWN COMMAND");
						oos.flush();
						
					}
										
					if(receiveData.equals("/quit"))
						break;
					}
			}
		}catch(Exception e){
			System.out.println("[Server] Client closed");
		}
		finally{
			try{
				if(child !=null)
					child.close();
			}catch(Exception e){}
		}
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
		
	public void parseCmd(String rcvData){
		
		if (rcvData != null){
			StringTokenizer str = new StringTokenizer(rcvData, "/");
			
			if (str.hasMoreTokens() == true){						
				nodeID = str.nextToken();
				System.out.println("[Server] nodeID is "+nodeID);
			}
			
			if (str.hasMoreTokens() == true){						
				cmd = str.nextToken();
				System.out.println("[Server] cmd is "+cmd);
			}
			
			if (str.hasMoreTokens() == true){						
				fileName = str.nextToken();
				System.out.println("[Server] fileName is "+fileName);
			}			
			
			if (str.hasMoreTokens() == true){						
				timeStamp = Long.parseLong(str.nextToken());
				System.out.println("[Server] timeStamp is "+timeStamp);
			}
			
			if (str.hasMoreTokens() == true){						
				msg = str.nextToken();
				System.out.println("[Server] Message is "+msg);
			}
		}
		System.out.println();
	}
	
	/**
	 * This is function to write files.
	 * @param fileName
	 * @param fileMessage
	 */
	
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
