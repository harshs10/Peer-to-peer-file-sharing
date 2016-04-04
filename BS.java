import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class BS{

	private static final int BSport = 7000;   //The Bootstrap server will be listening on this port number. 
	public static int T = 5; //Total no. of Clients.
	public static ArrayList<Integer> Cports = new ArrayList<Integer>(); //Array of client ports
	public static ArrayList<Integer> Neighbours = new ArrayList<Integer>(); //Network stored in Neighbours Array
	
	//Main method
	public static void main(String[] args) throws Exception {
		System.out.println("The Bootstrap server is running.");
		
		//Forming a Ring network
		Neighbours.add(T-1);
		for(int i=0;i<T-1;i++){
			Neighbours.add(i);
		}
		System.out.println("Neighbours"+Neighbours);
		
		//Bootstrap listening for clients
        ServerSocket listen = new ServerSocket(BSport);
		int clientNum = 1;

        try {
            	while(true) {
               		new ClientHandler(listen.accept(),clientNum).start();
					System.out.println("Client "  + clientNum + " is connected!");
					clientNum++;
            	}
        	} finally {
            		listen.close();
        	}
	}
	
	/*
     * ClientHandler Class Extends Thread. 
	 * Get cPort and send Correspoing N(Download Neighbour Port No.),
	 * based on ring network in Neighbours Array.
     */
	private static class ClientHandler extends Thread {
		private Socket connection;
        private ObjectInputStream in;
        private ObjectOutputStream out;
		private int no;		

        public ClientHandler(Socket connection, int no) {
           	this.connection = connection;
			this.no = no;
        }

        public void run() {
			try{
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				
				//Get cPort
				int ClientPort = Integer.parseInt((String)in.readObject());
				Cports.add(ClientPort);
				
				while(Cports.size() != 5){
					Thread.sleep(1000);
				}	
				
				int index = Cports.indexOf(ClientPort);
				int i = Neighbours.get(index);
				
				//send Correspoing N(Download Neighbour Port No.)
				sendMessage(""+Cports.get(i));
					
			}catch(IOException e){}
			catch(ClassNotFoundException e){}
			catch(InterruptedException e){}
				
 		}
		
		void sendMessage(String msg)
		{
			try{
				out.writeObject(msg);
				out.flush();
			}catch(IOException ioException){
			ioException.printStackTrace();}
		}
		
	}
}

