import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
	
	public static void main(String[] args) {
		new ChatServer().go();
	}
	
	private ArrayList<ClientHandler> clients;
	private int numOfClients;
	public ChatServer() {
		clients = new ArrayList<ClientHandler>();
		numOfClients = 0;
	}
	public void go() {
		try {
			ServerSocket ss = new ServerSocket(10000);
			System.out.println("Listening at 10000");
			while(true) {
				Socket s = ss.accept();
				int thisClient = (++numOfClients); 
				ClientHandler client = new ClientHandler(thisClient, s);
				System.out.println("New connection: "+thisClient);
				clients.add(client);
				
				new Thread(client).start();
			}
		} catch (IOException e) {
		}
	}
	
	public void broadcast(String message) {
		for(ClientHandler client: clients) {
			client.send(message);
		}
	}
	
	private class ClientHandler implements Runnable {
		private BufferedReader in;
		private PrintWriter out;
		private int id;
		public ClientHandler(int clientID, Socket s) throws IOException {
			id = clientID;
			in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			out = new PrintWriter(s.getOutputStream());
		}
		public void run() {
			try {
				while(true) {
					String message = in.readLine();
					if(message == null)
						break;
					System.out.println(id+": Message '"+ message + "' received");
					broadcast(id+": "+message);
				}
			} catch (IOException e) {
			} finally {
				System.out.println("Closed: "+id);
				clients.remove(this);
			}
		}
		public void send(String message) {
			out.println(message);
			out.flush();
		}
	}

}
