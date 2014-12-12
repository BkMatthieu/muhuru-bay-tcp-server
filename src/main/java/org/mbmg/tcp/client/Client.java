package org.mbmg.tcp.client;
// Client Side
import java.io.*;
import java.net.*;

public class Client {
  public void run() {
	try {
		int serverPort = 6001;
		InetAddress host = InetAddress.getByName("localhost"); 
		System.out.println("Connecting to server on port " + serverPort); 

		Socket socket = new Socket(host,serverPort); 
		//Socket socket = new Socket("127.0.0.1", serverPort);
		System.out.println("Just connected to " + socket.getRemoteSocketAddress()); 
		PrintWriter toServer = 
			new PrintWriter(socket.getOutputStream(),true);
		BufferedReader fromServer = 
			new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
		//toServer.println("Packet from " + socket.getLocalSocketAddress()); 
		String packet = "STA:654321,511;L:310;TM:141203032300;D:0;T:01;C:36;A00:76.60;A01:0.003;A02:0.870;A03:234.6;A04:1.240;A05:00000;A06:00000;A07:00000;A08:00000;A09:0.167;A10:00000;A11:0.574;A12:50.02;A13:21.25;A14:24.31;P01:00000000;P02:00000000;P03:00000000;P04:00000000;P05:00000502;P06:00000120;K01:13333330000000000;O01:0000;D6";
		toServer.println(packet);
		String line = fromServer.readLine();
		System.out.println("Client received: " + line + " from Server");
		toServer.close();
		fromServer.close();
		socket.close();
	}
	catch(UnknownHostException ex) {
		ex.printStackTrace();
	}
	catch(IOException e){
		e.printStackTrace();
	}
  }
	
  public static void main(String[] args) {
		Client client = new Client();
		client.run();
  }
}