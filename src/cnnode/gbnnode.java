package cnnode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/** 
 * @author rz2357
 * @version 1.0 
 */ 

public class gbnnode {

	public static int winSize;
	public static int localPort, peerPort;
	public static float lossP;
	public static String Mode;
	
	public static void main(String[] args) throws UnknownHostException {
		if(args.length == 5 && (args[3].equals("-d") || args[3].equals("-p")) ) {
			localPort = Integer.parseInt(args[0]);
			peerPort = Integer.parseInt(args[1]);
			winSize = Integer.parseInt(args[2]);
			Mode = args[3];
			lossP = Float.parseFloat(args[4]);
		}
		else {
			System.out.println("gbnnode <self-port> <peer-port> <window-size> [ -d <value-of-n> | -p <value-of-p> ]");
			System.exit(0);
		}
		InetAddress serverAddress = InetAddress.getByName("localhost");
		try {
			SenderThread Sender = new SenderThread(serverAddress, peerPort, winSize);
			new Thread(Sender).start();
			
			DatagramSocket receiveSocket = new DatagramSocket(localPort);
			new Thread(new ReceiverThread(serverAddress, receiveSocket, peerPort, Mode, lossP, Sender)).start();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class SenderThread implements Runnable {
	
//	private static int ack_seq;
	int timeoutFlag = 1;
	int timeoutVal;
	int winSize;
	int peerPort;
	int seqNum;
	InetAddress serverAddress;
	int countSend;
	int countDrop;
	int startPoint = 0;
	int endPoint = 0;
	String message = "";
	static Timer timer;

	public SenderThread(InetAddress serveraddress, int peerport, int winsize) {
		timeoutFlag = 1;
//		ack_seq = -1; //no ask is set to -1
		timeoutVal = 500;
		seqNum = 0;
		serverAddress = serveraddress;
		peerPort = peerport;
		winSize = winsize;
		
	}
	
	@Override
	public void run() {
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("node> ");
		while(true) {
			String inputString;
			try {
				inputString = input.readLine();
				String[] command = inputString.split(" ", 2);
				if (command.length == 2 && command[0].equals("send")) {
					message = command[1];
					startPoint = 0;
					Send();
				}
				else {
					System.out.println("send <message>");
					System.out.print("node> ");

				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void Send() throws IOException, InterruptedException {
		endPoint = startPoint;
		byte[] sendData = new byte[64];
		int dataCount = message.length();
		DatagramSocket sendSocket = new DatagramSocket();

		while(endPoint < startPoint + winSize && endPoint < dataCount) {
			int seq = seqNum + endPoint - startPoint;
			sendData = ("" + seq + " " + (char) message.charAt(endPoint)).getBytes();
//			sendData = Arrays.copyOf(ByteBuffer.allocate(4).putInt(seq).array(), 5);
//			sendData[4] = (byte) message.charAt(endPoint);
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, peerPort);
			sendSocket.send(sendPacket);
			System.out.println("[" + Calendar.getInstance().getTimeInMillis() + "]" + " packet" + seq 
					      		+ " " + (char) message.charAt(endPoint) + " sent");
			countSend++;
       	if(endPoint == startPoint) {
				timer = new Timer();
				timer.schedule(new TimeoutTask(this), timeoutVal);
			}
			endPoint++;
		}
		sendSocket.close();
	}
	
	public synchronized void ACK(int ack) throws IOException, InterruptedException {
		byte[] sendData = new byte[64];
		int dataCount = message.length();
		DatagramSocket sendSocket = new DatagramSocket();
		if (ack == seqNum) {
			// [1353035731.062] ACK0 received, window moves to 1
			startPoint++;
			seqNum++;
			System.out.println("[" + Calendar.getInstance().getTimeInMillis() + "]" + " ACK" 
					+ (seqNum - 1) + " received, window moves to " + seqNum);
			if (startPoint == dataCount){
				// [Summary] 1/12 packets discarded, loss rate = 0.083
				timer.cancel();
				sendData = ("f i n").getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, peerPort);
				sendSocket.send(sendPacket);
				System.out.println("[Summary] " + countDrop + "/" + countSend +
						" packets discarded, loss rate = " + (float)((float)countDrop/(float)countSend) );
				countDrop = 0;
				countSend = 0;
				sendSocket.close();
				System.out.print("node> ");
				return;
			}
			timer.cancel();
			timer = new Timer();
			timer.schedule(new TimeoutTask(this), timeoutVal);
			if (endPoint < dataCount) {
				int seq = seqNum + endPoint - startPoint;
				sendData = ("" + seq + " " + (char) message.charAt(endPoint)).getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, peerPort);
				sendSocket.send(sendPacket);
				countSend++;
				System.out.println("[" + Calendar.getInstance().getTimeInMillis() + "]" + " packet" + seq + " " + (char) message.charAt(endPoint) + " sent");
				endPoint++;
			}
		}
		sendSocket.close();
	}
	
}

class TimeoutTask extends TimerTask {
	SenderThread S;
	public TimeoutTask(SenderThread s) {
		S = s;
	}
	
	@Override
   public void run() {
		//S.timeoutFlag = true;
		try {
			S.Send();
	    	System.out.println("[" + Calendar.getInstance().getTimeInMillis() + "]" + " packet" + S.seqNum + " timeout");
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
   }
}

class ReceiverThread implements Runnable {
	DatagramSocket receiveSocket;
	private static String Mode;
	private static float lossP;
	private SenderThread Sender;
	private int countD;
	private int countDrop;
	private int countRecv;
	private int ackNum;
	private InetAddress serverAddress;
	private int peerPort;

	
	public ReceiverThread(InetAddress serveraddress, DatagramSocket receivesocket, int peerport, String mode, float lossp, SenderThread sender) {
		receiveSocket = receivesocket;
		serverAddress = serveraddress;
		peerPort = peerport;
		Mode = mode;
		lossP = lossp;
		Sender = sender;
		countD = 0;
		countDrop = 0;
		countRecv = 0;
		ackNum = 0;
	}
	@Override
	public void run() {
		while(true) {
			byte[] recvData = new byte[64];
			DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);
			try {
				receiveSocket.receive(recvPacket);
				String[] recv = new String(recvData, 0, recvPacket.getLength()).split(" ");
					if(recv.length == 2) { // received data
					int ack_seq = Integer.parseInt(recv[0]);
					countRecv++;
					if((Mode.equals("-p") && (float)Math.random() < lossP) || (Mode.equals("-d") && ++countD%lossP == 0)) {
						//[1353035731.072] packet2 c discarded
						System.out.println( "[" + Calendar.getInstance().getTimeInMillis() + "] packet" + ack_seq +
								" " + recv[1] +" discarded" );
						countDrop++;
					}
					else {
						System.out.println( "[" + Calendar.getInstance().getTimeInMillis() + "] packet" + ack_seq +
								" " + recv[1] +" received" );
						if(ackNum >= ack_seq) {
							ackNum = ack_seq + 1;
						}
						byte[] sendData = new byte[64];
						sendData = ("" + (ackNum-1)).getBytes();
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, peerPort);
						try {
							receiveSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
						}
						System.out.println( "[" + Calendar.getInstance().getTimeInMillis() + "] ACK" + (ackNum-1) + " sent, expecting packet" + ackNum );
					}
				}
				else if(recv.length == 1) { // receive ACK
					int ack_seq = Integer.parseInt(recv[0]);
					if((Mode.equals("-p") && (float)Math.random() < lossP) || (Mode.equals("-d") && ++countD%lossP == 0)) {
						//[1353035731.605] ACK3 discarded
						System.out.println( "[" + Calendar.getInstance().getTimeInMillis() + "] ACK" + ack_seq + " discarded" );
						Sender.countDrop++;
					}
					else {
						Sender.ACK(ack_seq);
					}
				}
				else { // finish sending
					// [Summary] 1/13 packets dropped, loss rate = 0.076
					System.out.println("[Summary]" + countDrop + "/" + (countRecv-1) + 
							" packets dropped, loss rate = " + (float)((float) countDrop / (float) (countRecv-1)) );
					//ackNum = 0;
					countDrop = 0;
					countD = 0;
					countRecv = 0;
					System.out.print("node> ");
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
			
		}
	}
}

