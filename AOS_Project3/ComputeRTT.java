import java.net.*;
import java.io.*;
import java.util.*;

public class ComputeRTT {
	protected double avgRTT =0;
	
	public double getRTT(String ipAddr) {

		
		String ip = ipAddr;
		String pingResult = "";
	
		String pingCmd = "ping " + ip;
	    int num_iter = 10; // gather 10 samples to take average
			try {
				Runtime r = Runtime.getRuntime();
				Process p = r.exec(pingCmd);
		
				BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String inputLine;
		        
		        inputLine = in.readLine();
		        for(int i=0;i<num_iter;i++){
		            if((inputLine = in.readLine()) != null){
		               // System.out.println(inputLine);
		                pingResult += inputLine;
		                StringTokenizer strtok = new StringTokenizer(inputLine);
		                String nextStr="";
		                do{
		                    nextStr = strtok.nextToken();
		                    //System.out.println(nextStr);
		                    if(nextStr.startsWith("time")){
		                        avgRTT += Double.parseDouble(nextStr.substring(5));
		                        //System.out.println(""+avgRTT+"ms");
		                        break;
		                    }
		                }while(!nextStr.startsWith("time"));
		            }
		        }
		        avgRTT /= num_iter;
		        System.out.println("[ComputerRTT] IP "+ ip+":"+avgRTT+" ms");
				in.close();
			}//try
			catch (IOException e) {
				System.out.println(e);
			}
	        catch(NoSuchElementException e){
	                System.out.println(e);
	        }
        
        return avgRTT;
	}
}