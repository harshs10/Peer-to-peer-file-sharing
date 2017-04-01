import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Server {

	private static final int sPort = 8000; // The server will be listening on
											// this port number
	public static ArrayList<File> F = new ArrayList<File>(); // Store the Chunks
	public static int ChunkCnt = 0; // No of chunks
	public static String s; // filename

	/*
	 * Main Method Call fileChunks to divide file into chunks. Use a
	 * Serversocket to listen to clients. Start a Handler(thread) for each
	 * client.
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("The server is running.");

		System.out.println("Enter file to divide.");
		Scanner scanner = new Scanner(System.in);
		s = scanner.nextLine();
		scanner.close();
		fileChunks(new File("C:\\Users\\rish\\Desktop\\CN\\" + s));

		ServerSocket listener = new ServerSocket(sPort);
		int clientNum = 1;

		try {
			while (true) {
				new Handler(listener.accept(), clientNum).start();
				System.out.println("Client " + clientNum + " is connected!");
				clientNum++;
			}
		} finally {
			listener.close();
		}
	}

	/*
	 * fileChunks Method Take file to divide as input arguement Divide file into
	 * chunks. Store in ArrayList F.
	 */
	public static void fileChunks(File f) throws IOException {
		int chunkSize = 1024 * 100;// 100KB
		byte[] buf = new byte[chunkSize];

		try (BufferedInputStream B = new BufferedInputStream(new FileInputStream(f))) {
			int full = 0;

			while ((full = B.read(buf)) > 0) {
				ChunkCnt = ChunkCnt + 1;
				String name = "Chunk" + ChunkCnt;
				File newFile = new File(f.getParent(), name);
				FileOutputStream out = new FileOutputStream(newFile);
				out.write(buf, 0, full);
				System.out.println(newFile.length());

				F.add(newFile);
			}
		} catch (IOException e) {
		}
	}

	/*
	 * A handler thread class. Each thread deals with a single client's
	 * requests. Send the share of chunks of that client.
	 */
	private static class Handler extends Thread {
		private Socket connection;
		private ObjectInputStream in; // stream read from the socket
		private ObjectOutputStream out; // stream write to the socket
		private int no; // The index number of the client

		public Handler(Socket connection, int no) {
			this.connection = connection;
			this.no = no;
		}

		@Override
		public void run() {
			try {
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());

				int limit = ChunkCnt / 5;
				int temp = 4 * limit;
				if (no == 5) {
					limit = ChunkCnt - temp;
				}
				System.out.println("limit of client" + no + "is" + limit);

				sendMessage("" + no);
				sendMessage(s);
				sendMessage("" + ChunkCnt);
				sendMessage("" + limit);

				int I = 3 * (no - 1);
				for (int i = 0; i < limit; i++) {
					sendFile(F.get(I));
					sendMessage("" + ++I);
				}
			} catch (IOException e) {
			} finally {
				try {
					in.close();
					out.close();
					connection.close();
				} catch (IOException e) {
				}
			}
		}

		// To send File over the connection
		public void sendFile(File f) {
			try {
				out.writeObject(f);
				out.flush();
				System.out.println("Send File: " + " to Client " + no);
			} catch (IOException e) {
			}
		}

		// To send Message over the connection
		public void sendMessage(String msg) {
			try {
				out.writeObject(msg);
				out.flush();
				System.out.println("Send message: " + msg + " to Client " + no);
			} catch (IOException e) {
			}
		}

	}

}
