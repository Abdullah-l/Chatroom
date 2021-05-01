
/**
 * This thread is passed a socket that it reads from. Whenever it gets input
 * it writes it to the Chat Screen text area using the displayMessage() method.
 */

import java.io.*;
import java.net.*;

public class ReaderThread implements Runnable {
	Socket server;
	BufferedReader fromServer;
	ChatClient screen;

	public ReaderThread(Socket server, ChatClient screen) {
		this.server = server;
		this.screen = screen;
	}

	public void run() {
		try {
			fromServer = new BufferedReader(new InputStreamReader(server.getInputStream()));
			String line;
			while ((line = fromServer.readLine()) != null) {
				String[] splitSpaces = line.split(" ", 3);
				if (!screen.loggedIn) { // check status code if not logged in
					if (splitSpaces[0].equals("STATUS")) {
						if (splitSpaces[1].equals("1")) {
							screen.loggedIn = true;
							screen.afterLogin();
						} else if (splitSpaces[1].equals("0")) {
							screen.loginText.setText("");
							screen.loginText.requestFocus();
							screen.loginLabel.setText("The username entered is already taken.");
						}
					}
				} else {
					if (splitSpaces[0].equals("LOGIN")) { // Login
						screen.displayMessage(splitSpaces[1] + " has joined the chat!");
					} else if (splitSpaces[0].equals("MSG")) { // Public message
						screen.displayMessage(splitSpaces[1] + ": " + splitSpaces[2]);
					}
					else if (splitSpaces[0].equals("PRIVMSG")) { // Private message
						splitSpaces = line.split(" ", 4);
						String user;
						if (splitSpaces[1].equals(screen.username)){
							user = "(Private To) " + splitSpaces[2];
						}
						else{
							user = "(Private From) " + splitSpaces[1];
						}
						screen.displayMessage(user + ": " + splitSpaces[3]);
					}
					else if (splitSpaces[0].equals("EXIT")) {
						screen.displayMessage(splitSpaces[1] + " has left the chat!");
					}
				}
			}

		} catch (IOException ioe) {
			System.out.println(ioe);
		}

	}
}
