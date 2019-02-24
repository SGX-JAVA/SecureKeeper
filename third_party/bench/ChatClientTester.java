import java.io.*;
import java.net.*;

public class ChatClientTester extends Thread  {

	public static int counter=0;
	public static void main(String[] args) {
		for(int i=0;i<50;++i) {
			new ChatClientTester().start();
		}
	}
	private PrintWriter out;
	private BufferedReader in;
	public ChatClientTester() {
		counter++;
	}
	public void run() {
		try {
			Socket s = new Socket("localhost", 10000);
			out = new PrintWriter(s.getOutputStream());
			in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			new Thread(new ResponseHandler()).start();
			for(int i=0; i<50; ++i) {
				out.println("test");
				out.flush();
				if(Math.random()<0.2) {
					break;
				}
			}
			counter--;
			in.close();
			out.close();
			s.close();

		} catch(IOException e) {
		} finally {
			if(counter == 0) {
				counter--;
				System.out.println("Test completed");
			}
		}
		
	}
	private class ResponseHandler implements Runnable {
		public void run() {
			try {
				while(true) {
					in.readLine();
				}
			} catch(IOException e) {
			}
		}
	}
}
