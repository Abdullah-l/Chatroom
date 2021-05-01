/**
 * This thread is passed a socket that it writes to.
 */
import java.io.*;
import java.net.*;

public class WriterThread implements Runnable
{
	Socket server;
	BufferedWriter toServer;
	String line;

	public WriterThread(Socket server, String line) {
        this.server = server;
		this.line = line;
	}

	public void run() {
		try {
			toServer = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()));
            toServer.write(line);
            toServer.flush();
		}
		catch (IOException ioe) { System.out.println(ioe); }

	}
}
