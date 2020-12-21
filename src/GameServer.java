import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;

public class GameServer{

	class Control {
		public volatile String lastReceived = "oof";
	}

	final Control control = new Control();
	
	public ArrayList<ObjectOutputStream> outputs = new ArrayList<ObjectOutputStream>();

	public GameServer() {
		System.out.println("Your online IP is: " + getIpAddress());
		try {
			openServer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ClientReceiveHandler class
	class ClientReceiveHandler extends Thread { // receives input from specific client
		final ObjectInputStream dis;
		final ObjectOutputStream dos;

		// Constructor
		public ClientReceiveHandler(ObjectInputStream dis, ObjectOutputStream dos) {
			this.dis = dis;
			this.dos = dos;
		}

		@Override
		public synchronized void run() { // checks for messages from clients
			boolean run = true;
			while (run) {

				// checks if your opponent has sent
				try {
					String repeat = null;
					try {
						repeat = dis.readObject().toString();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch(java.net.SocketException e1) {
						System.out.println("Client is disconnected!");
						outputs.remove(dos);
						run = false;
						break;
					}
					
					
					while (control.lastReceived != repeat) { // make sure every thread sees the new message by repeatedly writing to the global variable
						control.lastReceived = repeat;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// ClientSendHandler class
	class ClientSendHandler extends Thread { // sends to specific client
		String lastSend = "oof";

		// Constructor
		public ClientSendHandler() {

		}

		@Override
		public synchronized void run() { // sends message
			while (true) {

				// checks if your opponent has sent
				if (!lastSend.equals(control.lastReceived)) {
					for(ObjectOutputStream each : outputs) {
						try {
							each.writeObject(control.lastReceived);
							each.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					while(!lastSend.equals(control.lastReceived)) {
						lastSend = control.lastReceived;
					}
				}
			}
		}
	}

	private void openServer() throws IOException { // become serverHost
		System.out.println("Creating ServerSocket");
		ServerSocket ss = new ServerSocket(22222);
		Thread send = new ClientSendHandler();
		send.start();
		
		while (true) {
			Socket s = null;
			try {
				// socket object to receive incoming client requests
				s = ss.accept();

				System.out.println("A new client is connected : " + s);

				// obtaining input and out streams
				ObjectInputStream dis = new ObjectInputStream(s.getInputStream());
				ObjectOutputStream dos = new ObjectOutputStream(s.getOutputStream());

				System.out.println("Assigning new thread for this client");

				// create a new thread object
				Thread t = new ClientReceiveHandler(dis, dos);
				outputs.add(dos);

				// Invoking the start() method
				t.start();

			} catch (Exception e) {
				s.close();
				e.printStackTrace();
			}
		}
	}
	
	public static String getIpAddress() {
			try {
				InetAddress inetAddress = InetAddress.getLocalHost();
				String hostName = inetAddress.getHostName(); //Get Host Name
				String ipAddress = inetAddress.getHostAddress(); // Get IP Address
				return ipAddress;

		} catch (UnknownHostException e) {
				e.printStackTrace();
							}


		return null;
	}}



