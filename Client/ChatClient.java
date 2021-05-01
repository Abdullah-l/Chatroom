/* 
* This is a chat client that sends commands based on this protocol: https://tinyurl.com/TuesdayProtocol
* @author Abdullah A & Jamin P
* CMPT 352 - Greg Gagne
* HW6
*/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.net.*;

public class ChatClient extends JFrame implements ActionListener, KeyListener {
	
	// Login components
	JTextField loginText;
	JLabel loginLabel;
	JButton loginButton;
	Boolean loggedIn;

	// Chat Screen components
	private JButton sendButton;
	private JButton exitButton;
	private JTextField sendText;
	private JTextArea displayArea;
	private JLabel privmsgLabel;
	
	// Other vars
	private String host;
	JPanel p;
	String username;
	Socket server;
	public static final int DEFAULT_PORT = 63546;

	public ChatClient(String host) {
		this.host = host;
		this.loggedIn = false;
		promptLogin();
	}

	/* Starts the login screen.
	 */
	public void promptLogin() {
		// set up panel and layout
		p = new JPanel(new GridBagLayout());
		p.setBackground(new Color(255, 235, 171));

		Border etched = BorderFactory.createEtchedBorder();
		Border titled = BorderFactory.createTitledBorder(etched, "Welcome to the Chat Client!");
		
		p.setBorder(titled);

		// set up components
		loginText = new JTextField(30);
		loginButton = new JButton("Login");
		loginButton.setBackground(Color.ORANGE);
		loginLabel = new JLabel("Please enter an alphanumeric username: ");
		loginLabel.setForeground(new Color(153, 44, 2));

		// set up constraints
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(10, 10, 10, 10);
		constraints.gridx = 0;
		constraints.gridy = 2;

		// listeners
		loginText.addKeyListener(this);
		loginButton.addActionListener(this);

		// adding to panel
		p.add(loginLabel, constraints);

		constraints.gridx = 0;
		constraints.gridy = 30;

		p.add(loginText, constraints);
		constraints.gridx = 20;
		constraints.gridy = 30;
		p.add(loginButton, constraints);

		getContentPane().add(p);

		setTitle("Chatroom Client");
		pack();

		setVisible(true);
		loginText.requestFocus();
	}

	/* Starts up the chat screen after login */
	public void afterLogin() {
		// remove the login panel
		getContentPane().removeAll();
		p.removeAll();
		revalidate();
		repaint();
		loginButton.setEnabled(false);
		loginText.setEnabled(false);
		loginLabel.setEnabled(false);

		// set up panel and layout
		p = new JPanel(new GridBagLayout());
		p.setBackground(new Color(255, 228, 163));


		// set up components
		sendText = new JTextField(30);
		sendButton = new JButton("Send");
		sendButton.setBackground(new Color(194, 132, 0));
		sendButton.setForeground(new Color(255, 255, 255));
		exitButton = new JButton("Exit");
		exitButton.setBackground(new Color(200, 0, 0));
		exitButton.setForeground(new Color(255, 255, 255));
		privmsgLabel = new JLabel("To send a private message type in: @username message");
		privmsgLabel.setForeground(new Color(153, 44, 2));

		// listeners
		sendText.addKeyListener(this);
		sendButton.addActionListener(this);
		exitButton.addActionListener(this);

		// constraints 
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(1, 3, 1, 0);
		constraints.anchor = GridBagConstraints.LINE_START;
		constraints.gridx = 0;
		constraints.gridy = 0;

		// adding to panel 
		p.add(sendText, constraints);
		constraints.gridx = 1;
		constraints.insets.left = 1;
		p.add(sendButton, constraints);
		constraints.insets = new Insets(1, 5, 1, 5);
		constraints.gridx = 2;
		p.add(exitButton, constraints);
		constraints.anchor = GridBagConstraints.LAST_LINE_START;
		constraints.gridx = 0;
		constraints.gridy = 1;
		p.add(privmsgLabel, constraints);

		
		getContentPane().add(p, "South");


		// add display area for seeing messages
		displayArea = new JTextArea(15, 40);
		displayArea.setEditable(false);
		displayArea.setFont(new Font("SansSerif", Font.PLAIN, 14));

		JScrollPane scrollPane = new JScrollPane(displayArea);
		getContentPane().add(scrollPane, "Center");

		displayArea.setBackground(new Color(255, 244, 217));


		setTitle("Chatroom Client");
		pack();

		setVisible(true);
		sendText.requestFocus();

		/** anonymous inner class to handle window closing events */
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				exitButton.doClick();
			}
		});
	}

	/**
	 * Displays a message
	 */
	public void displayMessage(String message) {
		displayArea.append(message + "\n");
	}

	/**
	 * This gets the text the user entered and outputs it in the display area.
	 */
	public void displayText() {
		String message = sendText.getText();

		// error checking
		if (message.length() > 512 || message.isEmpty()) {
			displayMessage("Your message cannot be empty or more than 512 characters");
		} else {
			String line;
			if(message.charAt(0) == '@'){ // private message
				String[] splitSpaces = message.split(" ", 2);
				line = "PRIVMSG " + this.username + " " + splitSpaces[0].substring(1).toUpperCase() + " " + splitSpaces[1];
			}

			else{ // public message
				line = "MSG " + this.username + " " + message;
			}

			Thread WriterThread = new Thread(
					new WriterThread(this.server, (line + "\r\n")));
			WriterThread.run();
		}
		revalidate();
		repaint();
		sendText.setText("");
		sendText.requestFocus();
	}

	/**
	 * This method responds to action events .... i.e. button clicks and fulfills
	 * the contract of the ActionListener interface.
	 */
	public void actionPerformed(ActionEvent evt) {
		Object source = evt.getSource();

		if (source == sendButton)
			displayText();
		else if (source == exitButton) {
			if (!loginButton.isEnabled()) {
				Thread WriterThread = new Thread(new WriterThread(this.server, ("EXIT " + this.username + "\r\n")));
				WriterThread.run();
			}
			System.exit(0);
		} else if (source == loginButton) {
			this.username = loginText.getText().toUpperCase();
			// error checking
			if (username.isEmpty() || username.contains(" ") || !username.matches("^[a-zA-Z0-9]*$")) {
				loginLabel.setText("The username you entered is invalid.");
				loginText.setText("");
				loginText.requestFocus();
				pack();
			} else {
				try {
					// only connect to server after passing error check
					this.server = new Socket(host, DEFAULT_PORT);
					Thread WriterThread = new Thread(
							new WriterThread(this.server, ("LOGIN " + this.username + "\r\n")));
					WriterThread.run();
					Thread ReaderThread = new Thread(new ReaderThread(this.server, this));
					ReaderThread.start();
				} catch (Exception e) {
					loginLabel.setText("Could not connect to the server.");
					loginText.setText("");
					loginText.requestFocus();
					pack();
				}

			}

		}
	}

	/**
	 * These methods responds to keystroke events and fulfills the contract of the
	 * KeyListener interface.
	 */

	/**
	 * This is invoked when the user presses the ENTER key.
	 */
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			if (loginButton.isEnabled())
				loginButton.doClick();
			else if (sendButton.isEnabled())
				displayText();
		}
	}

	/** Not implemented */
	public void keyReleased(KeyEvent e) {
	}

	/** Not implemented */
	public void keyTyped(KeyEvent e) {
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java ChatClient [host]");
		} else {
			new ChatClient(args[0]);
		}
	}
}
