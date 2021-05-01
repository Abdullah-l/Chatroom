
/* 
* This is a chat server that receives commands based on this protocol: https://tinyurl.com/TuesdayProtocol
* @author Abdullah A & Jamin P
* CMPT 352 - Greg Gagne
* HW6
*/
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.HashMap;
import java.util.concurrent.*;

public class ChatServer {
	public static final int DEFAULT_PORT = 63546;

	// construct a thread pool for concurrency
	private static final Executor exec = Executors.newCachedThreadPool();

	public static void main(String[] args) throws IOException {
		Clients clientsObj = new Clients(new HashMap<String, Socket>());
		ServerSocket sock = null;
		Socket client = null;
		try {
			// establish the socket
			sock = new ServerSocket(DEFAULT_PORT);

			while (true) {
				/**
				 * now listen for connections and service the connection in a separate thread.
				 */
				client = sock.accept();
				Runnable task = new Connection(client, clientsObj);
				exec.execute(task);
			}
		} catch (IOException ioe) {
			System.err.println(ioe);
		} finally {
			if (sock != null)
				sock.close();
		}
	}
}

class Handler {

	/**
	 * this method is invoked by a separate thread
	 */
	public void process(Socket client, Clients clientsObj) throws java.io.IOException {
		BufferedReader readClient = null;
		BufferedWriter writeClient = null;
		String username = "";

		try {
			readClient = new BufferedReader(new InputStreamReader(client.getInputStream()));

			String line;
			while (!client.isClosed() && (line = readClient.readLine()) != null) {

				System.out.println("Received: " + line);

				Iterator<Socket> itr = null;

				// Parse command from client
				String[] splitSpaces = line.split(" ");
				username = splitSpaces[1];

				if (splitSpaces[0].equals("LOGIN")) { // LOGIN

					writeClient = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));

					// Check if username is in map
					// if so, send STATUS 0, otherwise STATUS 1
					if (clientsObj.getClients().containsKey(splitSpaces[1])) {
						writeClient.write("STATUS 0" + "\r\n");
						writeClient.flush();
						continue;
					} else {
						writeClient.write("STATUS 1" + "\r\n");
						writeClient.flush();
						clientsObj.add(splitSpaces[1], client); // Add to client pool
					}

				}

				else if (splitSpaces[0].equals("EXIT")) { // EXIT
					clientsObj.remove(splitSpaces[1]); // Remove from client pool
					client.close();
				}

				else if (splitSpaces[0].equals("PRIVMSG")) { // Private message
					if (clientsObj.getClients().containsKey(splitSpaces[2])) {
						// create list to store pm sender and receiver
						ArrayList<Socket> privmsgSkts = new ArrayList<Socket>();
						privmsgSkts.add(clientsObj.get(splitSpaces[1]));
						privmsgSkts.add(clientsObj.get(splitSpaces[2]));
						itr = privmsgSkts.iterator();
					} else {
						// receiving username does not exist
						continue;
					}
				}

				// itr is null in all cases except PRIVMSG
				if (itr == null)
					itr = clientsObj.getClients().values().iterator();

				while (itr.hasNext()) { // loop through map and write to every client
					Socket skt = itr.next();
					writeClient = new BufferedWriter(new OutputStreamWriter(skt.getOutputStream()));
					writeClient.write(line + "\r\n");
					writeClient.flush();
				}

				Thread.sleep(500);

			}
		} catch (Exception e) {
			if (e instanceof SocketException){
				System.out.println("Client dropped connection abruptly");
				clientsObj.remove(username);
			}
			else
				e.printStackTrace();
		} finally {
			// close socket and streams
			try {
				if (client != null)
					client.close();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

class Connection implements Runnable {
	private Socket client;
	private static Handler handler = new Handler();
	Clients serverInfo;

	public Connection(Socket client, Clients serverInfo) {
		this.client = client;
		this.serverInfo = serverInfo;
	}

	/**
	 * This method runs in a separate thread.
	 */
	public void run() {
		try {

			handler.process(client, serverInfo);
		} catch (java.io.IOException ioe) {
			System.err.println(ioe);
		}
	}
}

/*
 * This class represents all clients that login to the server.
 */
class Clients {
	private Map<String, Socket> users = new HashMap<String, Socket>();

	public Clients(Map<String, Socket> users) {
		this.users = users;
	}

	public Socket get(String user) {
		return this.users.get(user);
	}

	public void remove(String user) {
		this.users.remove(user);
	}

	public void add(String user, Socket skt) {
		this.users.put(user, skt);
	}

	public Map<String, Socket> getClients() {
		return this.users;
	}
}