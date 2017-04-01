import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
	Socket requestSocket; // socket connect to the server.
	ObjectOutputStream out; // stream write to the socket.
	ObjectInputStream in; // stream read from the socket.

	public static ArrayList<File> F = new ArrayList<File>(); // Array of chunks.
	public static ArrayList<Integer> list = new ArrayList<Integer>();// Array of
																		// chunk
																		// No's.

	private static int cPort; // Client Port No.
	public static Boolean UNflag = true; // Flag whether Upload Neighbour need
											// this client.
	public static int FSport = 8000; // File Server Port No.
	public static int BSport = 7000; // BootStrap Server Port No.
	public static int N; // Download Neighbour Port No.
	public static String s;// filename
	public static int limit;// client share of chunks
	public static int total;// Total No of chunks
	public static int no; // Client No.

	/*
	 * Run Method of Client. Recieve it's share of chunks sent by the
	 * FileServer.
	 */
	void run() {
		try {
			requestSocket = new Socket("localhost", FSport);
			System.out.println("Connected to localhost in port FSport");

			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());

			no = Integer.parseInt((String) in.readObject());
			s = (String) in.readObject();
			total = Integer.parseInt((String) in.readObject());
			limit = Integer.parseInt((String) in.readObject());

			for (int i = 0; i < limit; i++) {
				File f1 = (File) in.readObject();
				int I = Integer.parseInt((String) in.readObject());

				File f = new File("C:\\Users\\rish\\Desktop\\CN\\Client" + no + "\\Chunk" + I);
				System.out.println("Received Chunk" + I);

				InputStream input = null;
				OutputStream output = null;
				try {
					input = new FileInputStream(f1);
					output = new FileOutputStream(f);
					byte[] buf = new byte[102400];
					int bytesRead;
					while ((bytesRead = input.read(buf)) > 0) {
						output.write(buf, 0, bytesRead);
					}
				} finally {
					input.close();
					output.close();
				}
				F.add(f);
				list.add(I);
			}
		} catch (ConnectException e) {
		} catch (ClassNotFoundException e) {
		} catch (UnknownHostException unknownHost) {
		} catch (IOException ioException) {
		} finally {
			try {
				in.close();
				out.close();
				requestSocket.close();
			} catch (IOException ioException) {
			}
		}
	}

	/*
	 * UploadHandler Class Extends Thread. Send the Chunk No list to Upload
	 * Neighbour. And Receive the Request list of Upload Neighbour. Send chunks
	 * to Upload Neighbour according to its Request list. Update the UNflag.
	 */
	private static class UploadHandler extends Thread {
		private Socket connection;
		private ObjectInputStream in1;
		private ObjectOutputStream out1;

		public UploadHandler(Socket connection) {
			this.connection = connection;
		}

		@Override
		public void run() {
			try {
				System.out.println("Inside Upload Handler.");
				out1 = new ObjectOutputStream(connection.getOutputStream());
				out1.flush();
				in1 = new ObjectInputStream(connection.getInputStream());

				// Send the Chunk No list to Upload Neighbour.
				sendMessage("" + list.size());
				sendList(list);

				// Receive the Request list of Upload Neighbour.
				int noChunks = Integer.parseInt((String) in1.readObject());
				ArrayList<Integer> Reqlist = new ArrayList<Integer>();
				for (int j = 0; j < noChunks; j++) {
					Reqlist.add(Integer.parseInt((String) in1.readObject()));
				}
				System.out.println("Reqlist of Upload Neighbour" + Reqlist);

				// Send chunks to Upload Neighbour according to its Request
				// list.
				for (int j = 0; j < Reqlist.size(); j++) {
					int index = list.indexOf(Reqlist.get(j));
					int I = Reqlist.get(j);
					sendFile(F.get(index));
					System.out.println("Chunk" + I + "sent to Upload Neighbour");
				}

				// Update the UNflag.
				String j1 = (String) in1.readObject();
				if (j1.equals("false"))
					UNflag = false;
				System.out.println("Flag" + UNflag);

				Thread.sleep(2000);
			} catch (UnknownHostException e1) {
			} catch (IOException e2) {
			} catch (ClassNotFoundException e3) {
			} catch (InterruptedException e4) {
			}
		}

		// To send File over the connection
		public void sendFile(File f) {
			try {
				out1.writeObject(f);
				out1.flush();
			} catch (IOException ioException) {
			}
		}

		// To send ArrayList over the connection
		void sendList(ArrayList<Integer> l1) {
			for (int j = 0; j < l1.size(); j++) {
				sendMessage("" + l1.get(j));
			}
		}

		// To send Message over the connection
		void sendMessage(String msg) {
			try {
				out1.writeObject(msg);
				out1.flush();
			} catch (IOException ioException) {
			}
		}
	}

	/*
	 * DownloadHandler Class Extends Thread. Receive the Chunk No list of
	 * Download Neighbour. Send the Reqiest list to Download Neighbour. Recieve
	 * chunks from Download Neighbour. Update the UNflag.
	 */
	private static class DownloadHandler extends Thread {
		private ObjectInputStream in1;
		private ObjectOutputStream out1;
		private String flag = "true";

		public DownloadHandler() {
		}

		@Override
		public void run() {
			try {

				Socket req = null;
				while (true) {
					try {
						req = new Socket("localhost", N);
						if (req != null) {
							break;
						}
					} catch (IOException e) {
						Thread.sleep(1000);
					}
				}

				System.out.println("Conected to Download neighbour");

				out1 = new ObjectOutputStream(req.getOutputStream());
				out1.flush();
				in1 = new ObjectInputStream(req.getInputStream());

				// Receive the Chunk No list of Download Neighbour.
				int noChunks = Integer.parseInt((String) in1.readObject());
				ArrayList<Integer> DNlist = new ArrayList<Integer>();
				for (int j = 0; j < noChunks; j++) {
					DNlist.add(Integer.parseInt((String) in1.readObject()));
				}

				System.out.println("DownloadNeighbourlist" + DNlist);
				System.out.println("Mylist" + list);

				// Send the Reqiest list to Upload Neighbour.
				ArrayList<Integer> Reqlist = compare(DNlist, list);
				System.out.println("Reqlist sent to UploadNeighbour" + Reqlist);

				sendMessage("" + Reqlist.size());
				sendList(Reqlist);

				// Recieve chunks from Download Neighbour.
				for (int k = 0; k < Reqlist.size(); k++) {
					File f1 = (File) in1.readObject();
					int I = Reqlist.get(k);
					System.out.println("list" + list);

					File f = new File("C:\\Users\\rish\\Desktop\\CN\\Client" + no + "\\Chunk" + I);
					System.out.println("Chunk" + I + "recieved from Download Neighbour");

					InputStream input = null;
					OutputStream output = null;
					try {
						input = new FileInputStream(f1);
						output = new FileOutputStream(f);
						byte[] buf = new byte[102400];
						int bytesRead;
						while ((bytesRead = input.read(buf)) > 0) {
							output.write(buf, 0, bytesRead);
						}
					} finally {
						input.close();
						output.close();
					}
					F.add(f);
					list.add(I);
				}

				// Update the UNflag.
				if (list.size() == total) {
					flag = "false";
				}
				sendMessage(flag);

				System.out.println("list after receiving" + list);
			} catch (UnknownHostException e1) {
			} catch (IOException e2) {
			} catch (ClassNotFoundException e3) {
			} catch (InterruptedException e4) {
			}
		}

		// To send list over the connection
		void sendList(ArrayList<Integer> l1) {
			for (int j = 0; j < l1.size(); j++) {
				sendMessage("" + l1.get(j));
			}
		}

		// Compare Download neighbour list with present list and generate
		// Request list
		ArrayList<Integer> compare(ArrayList<Integer> l1, ArrayList<Integer> l2) {
			ArrayList<Integer> l = new ArrayList<Integer>();
			int flag = 0;
			for (int i = 0; i < l1.size(); i++) {
				for (int j = 0; j < l2.size(); j++) {
					if (l1.get(i) == l2.get(j)) {
						flag = 1;
					}
				}
				if (flag == 0) {
					l.add(l1.get(i));
				}
				flag = 0;
			}
			return l;
		}

		// To send Message over the connection
		void sendMessage(String msg) {
			try {
				out1.writeObject(msg);
				out1.flush();
			} catch (IOException ioException) {
			}
		}
	}

	/*
	 * MergeChunks Method. Merge all the chunks into single file.
	 */
	public static void MergeChunks() throws IOException {
		File f = new File("C:\\Users\\rish\\Desktop\\CN\\Client" + no + "\\" + s);
		try {
			FileOutputStream out = new FileOutputStream(f, true);
			FileInputStream in = null;
			for (int i = 0; i < list.size(); i++) {
				int index = list.indexOf(i + 1);
				in = new FileInputStream(F.get(index));
				byte[] B = new byte[(int) F.get(index).length()];
				int b = in.read(B, 0, (int) F.get(index).length());
				out.write(B);
				out.flush();
				in.close();
			}
			out.close();
		} catch (Exception e) {
		}
	}

	/*
	 * BootStrapHandler Class Extends Thread. Send cPort and get N(Download
	 * Neighbour Port No.).
	 */
	private static class BootStrapHandler {
		private ObjectInputStream inB;
		private ObjectOutputStream outB;

		public BootStrapHandler() {
		}

		public void run() {
			try (Socket rs = new Socket("localhost", BSport)) {
				System.out.println("Connected to BootStrap in port 7000");

				outB = new ObjectOutputStream(rs.getOutputStream());
				outB.flush();
				inB = new ObjectInputStream(rs.getInputStream());

				sendMessage("" + cPort);
				N = Integer.parseInt((String) inB.readObject());

				System.out.println("Neighbour Port No. from BootStrapServer" + N);
			} catch (ClassNotFoundException e) {
			} catch (IOException e) {
			}
		}

		void sendMessage(String msg) {
			try {
				outB.writeObject(msg);
				outB.flush();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}

	// main method
	public static void main(String args[]) throws Exception {
		System.out.println("Enter Port No. for this client");
		Scanner scanner = new Scanner(System.in);
		String s1 = scanner.nextLine();
		scanner.close();
		cPort = Integer.parseInt(s1);

		Client client = new Client();
		client.run();

		System.out.println("Recieved my share of chunks.");

		BootStrapHandler BSH = new BootStrapHandler();
		BSH.run();

		while (list.size() != total || UNflag) {
			ServerSocket S1 = null;
			Thread t1 = null;
			Thread t2 = null;

			if (list.size() != total) {
				t2 = new DownloadHandler();
				t2.start();
			}

			if (UNflag) {
				try {
					S1 = new ServerSocket(cPort);
					t1 = new UploadHandler(S1.accept());
					t1.start();

				} finally {
					S1.close();
				}
			}

			if (t1 != null) {
				t1.join();
			}
			if (t2 != null) {
				t2.join();
			}
		}

		MergeChunks();
		System.out.println("Merging Chunks done.");
	}

}