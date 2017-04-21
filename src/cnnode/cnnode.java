package cnnode;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author rz2357
 * @version 1.0
 */

public class cnnode {

	static int localPort;
	static HashMap<Integer, RoutingTable2> routingTable = new HashMap<Integer, RoutingTable2>();
	static boolean initFlag;
	static InetAddress serverAddress;

	public static void main(String[] args) {
		localPort = Integer.parseInt(args[0]);
		try {
			serverAddress = InetAddress.getByName("localhost");
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		;
		if (1024 <= localPort && localPort <= 65534) {
			int i = 2;
			boolean sendFlag = false;
			while (i < args.length) {
				if (args[i].equals("last")) {
					break;
				}

				if (args[i].equals("send")) {
					sendFlag = true;
					i++;
					continue;
				}
				int port = Integer.parseInt(args[i]);
				if (port < 1024 || port > 65534) {
					System.out.println("Port number range is 1024 ~ 65534");
					System.exit(0);
				}
				if (sendFlag) {
					routingTable.put(port, new RoutingTable2(port, 0, true, false, port));
					i++;
				} else {
					float dis = Float.parseFloat(args[i + 1]);
					i += 2;
					routingTable.put(port, new RoutingTable2(port, dis, false, true, port));
				}

			}

			// show routing table
			ShowRouting();

			try {
				final DatagramSocket receiveSocket = new DatagramSocket(localPort);
				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run() {
						// shut down close socket
						receiveSocket.close();
						System.out.println("The server is shut down!");
					}
				});

				if (args[args.length - 1].equals("last")) {
					// the last node
					new Thread(new Broadcast2(routingTable, localPort)).start();

				} else {
					byte[] receiveData = new byte[128];
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					try {
						receiveSocket.receive(receivePacket);
					} catch (IOException e) {
						e.printStackTrace();
					}

				}

				for (RoutingTable2 table : routingTable.values()) {
					if (table.isSend()) {
						try {
							table.sender.Send();
						} catch (IOException | InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				Timer ShowLossTimer = new Timer();
				ShowLossTimer.schedule(new DisplayLoss(), 1000, 1000);
				Timer UpdateRouteTimer = new Timer();
				UpdateRouteTimer.schedule(new UpdateRoute(), 1000, 5000);

				while (true) {
					byte[] receiveData = new byte[128];
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					receiveSocket.receive(receivePacket);
					String recvString = new String(receiveData, 0, receivePacket.getLength());
					if (recvString != null) {
						// get a new message
						Thread t = new Thread(new UpdateTable(recvString));
						t.start();
						t.join();
					}
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Port number range is 1024 ~ 65534");
		}

	}

	public static void ShowRouting() {
		System.out.println("[" + Calendar.getInstance().getTimeInMillis() + "] Node " + localPort + " Routing Table");
		for (RoutingTable2 table : routingTable.values()) {
			System.out.print("- (" + table.getDis() + ") -> Node " + table.getPort());
			// if don't need jump
			if (table.getPort() == table.getNext()) {
				System.out.println("");
			} else {
				System.out.println(" ; Next hop -> Node " + table.getNext());
			}
		}
	}

}

class UpdateTable implements Runnable {
	String recvString;

	public UpdateTable(String recv) {
		recvString = recv;
	}

	@Override
	public void run() {

		String[] recvData = recvString.split(" ");
		// [<timestamp>] Message received at Node <port-vvvv> from Node
		// <port-xxxx>
		if (recvData[0].equals("r")) {
			boolean isUpdate = false;
			RoutingTable2 tempTable = cnnode.routingTable.get(Integer.parseInt(recvData[1]));
			if (tempTable != null && tempTable.setTime(Long.parseLong(recvData[2]))) {
				float d = tempTable.neiDis();
				// w is only eighbor distance
				for (int i = 3; i < recvData.length; i += 2) {
					int tempPort = Integer.parseInt(recvData[i]);
					if (cnnode.routingTable.containsKey(tempPort)) {
						isUpdate = isUpdate || cnnode.routingTable.get(tempPort)
								.setDis((d + Float.parseFloat(recvData[i + 1])), Integer.parseInt(recvData[1]));
					} else {
						if (tempPort != cnnode.localPort) {
							cnnode.routingTable.put(tempPort,
									new RoutingTable2(tempPort, (d + Float.parseFloat(recvData[i + 1])), false, false,
											Integer.parseInt(recvData[1])));
							isUpdate = true;
						}
					}
				}
			}

			if (isUpdate) {
				cnnode.ShowRouting();
				new Thread(new Broadcast2(cnnode.routingTable, cnnode.localPort)).start();
			}
		}
		// a <port> sequenceNumber
		else if (recvData[0].equals("a")) {
			RoutingTable2 tempTable = cnnode.routingTable.get(Integer.parseInt(recvData[1]));
			int ack_seq = Integer.parseInt(recvData[2]);
			try {
				tempTable.sender.ACK(ack_seq);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		// p <port> sequenceNumber
		else if (recvData[0].equals("p")) { // receive a probe data
			RoutingTable2 tempTable = cnnode.routingTable.get(Integer.parseInt(recvData[1]));

			int ack_seq = Integer.parseInt(recvData[2]);
			if (tempTable.isRecv() && (float) Math.random() > tempTable.neiDis()) {
				// System.out.println("receive from" + tempTable.getPort() + "
				// dis" + tempTable.getDis());
				if (tempTable.ackNum >= ack_seq) {
					tempTable.ackNum = ack_seq + 1;
				}
				byte[] sendData = new byte[64];
				sendData = ("a " + cnnode.localPort + " " + (tempTable.ackNum - 1)).getBytes();
				// a <localPort> ackNum
				try {
					DatagramSocket sendSocket = new DatagramSocket();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, cnnode.serverAddress,
							tempTable.getPort());
					sendSocket.send(sendPacket);
					sendSocket.close();

				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
}

class Broadcast2 implements Runnable {
	private HashMap<Integer, RoutingTable2> Table;
	private int localPort;

	public Broadcast2(HashMap<Integer, RoutingTable2> table, int localport) {
		Table = table;
		localPort = localport;
	}

	@Override
	public void run() {
		try {
			InetAddress serverAddress = InetAddress.getByName("localhost");
			byte[] sendData = new byte[64];
			// sending data, format <local> <time> <port> <distance>
			String sendString = "r " + localPort + " " + Calendar.getInstance().getTimeInMillis();
			for (RoutingTable2 table : Table.values()) {
				if (table.getDis() != 0) {
					sendString += " " + table.getPort() + " " + table.getDis();
				}
			}
			sendData = sendString.getBytes();
			DatagramSocket sendSocket = new DatagramSocket();
			for (RoutingTable2 table : Table.values()) {
				if (table.isNeighbor()) {
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress,
							table.getPort());
					sendSocket.send(sendPacket);
					// System.out.println("[" +
					// Calendar.getInstance().getTimeInMillis() + "] Message
					// sent from Node " + localPort + " to Node " +
					// table.getPort() );
				}
			}
			sendSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}

class Sender {
	int timeoutVal;
	int winSize = 5;
	int peerPort;
	int seqNum;
	InetAddress serverAddress;
	Timer timer;

	public Sender(int peerport) throws UnknownHostException {
		timeoutVal = 500;
		seqNum = 0;
		serverAddress = InetAddress.getByName("localhost");
		peerPort = peerport;

	}

	public synchronized void Send() throws IOException, InterruptedException {
		byte[] sendData = new byte[64];
		DatagramSocket sendSocket = new DatagramSocket();
		// System.out.println("send data to " + peerPort);
		for (int i = 0; i < winSize; i++) {
			int seq = seqNum + i;
			// p <port> sequenceNumber
			sendData = ("p " + cnnode.localPort + " " + seq).getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, peerPort);
			Thread.sleep(50);
			sendSocket.send(sendPacket);
			if (i == 0) {
				timer = new Timer();
				timer.schedule(new TimeoutTask2(this), timeoutVal);
			}
			cnnode.routingTable.get(peerPort).sendCount++;
		}
		sendSocket.close();
	}

	public synchronized void ACK(int ack) throws IOException, InterruptedException {
		byte[] sendData = new byte[64];
		DatagramSocket sendSocket = new DatagramSocket();
		seqNum++;
		cnnode.routingTable.get(peerPort).ackCount++;
		int seq = seqNum + winSize;
		sendData = ("p " + cnnode.localPort + " " + seq).getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, peerPort);
		timer.cancel();
		Thread.sleep(100);
		sendSocket.send(sendPacket);
		timer = new Timer();
		timer.schedule(new TimeoutTask2(this), timeoutVal);
		cnnode.routingTable.get(peerPort).sendCount++;
		sendSocket.close();
	}

}

class TimeoutTask2 extends TimerTask {
	Sender S;

	public TimeoutTask2(Sender sender) {
		S = sender;
	}

	@Override
	public void run() {
		try {
			// System.out.println("time out " + S.peerPort);
			S.Send();
			// System.out.println("[" + Calendar.getInstance().getTimeInMillis()
			// + "]" + " packet" + S.seqNum + " timeout");
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}

class DisplayLoss extends TimerTask {
	@Override
	public void run() {
		for (RoutingTable2 table : cnnode.routingTable.values()) {
			if (table.isSend()) {
				// [1353035952.259] Link to 2222: 10 packets sent, 5 packets
				// lost, loss rate .50
				float lossRate = (float) (table.sendCount - table.ackCount) / table.sendCount;
				lossRate = (float) (Math.round(lossRate * 100.0) / 100.0);
				table.setNeiDis(lossRate);
				System.out.println("[" + Calendar.getInstance().getTimeInMillis() + "] Link to Node " + table.getPort()
						+ ": " + table.sendCount + " packets sent, " + (table.sendCount - table.ackCount)
						+ " packets lost, loss rate " + lossRate);
			}
		}

	}
}

class UpdateRoute extends TimerTask {
	public synchronized void run() {
		new Thread(new Broadcast2(cnnode.routingTable, cnnode.localPort)).start();
		cnnode.ShowRouting();
	}
}

// class for routing table
class RoutingTable2 {
	private int destPort;
	private float Distance;
	private float neighborDis;
	private int nextHop;
	private boolean sendNeighbor;
	private boolean recvNeighbor;
	private long timeStamp;
	public int sendCount = 0;
	public int ackCount = 0;
	public int ackNum = 0;
	public Sender sender;

	public RoutingTable2(int destport, float distance, boolean sendneighbor, boolean recvneighbor, int next) {
		destPort = destport;
		Distance = (float) (Math.round(distance * 100.0) / 100.0);
		timeStamp = Calendar.getInstance().getTimeInMillis();
		sendNeighbor = sendneighbor;
		recvNeighbor = recvneighbor;
		nextHop = next;
		if (sendNeighbor || recvNeighbor) {
			neighborDis = distance;
		}
		if (sendNeighbor) {
			try {
				sender = new Sender(destPort);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isRecv() {
		return recvNeighbor;
	}

	public boolean isSend() {
		return sendNeighbor;
	}

	public float getDis() {
		return Distance;
	}

	public float neiDis() {
		return neighborDis;
	}

	public boolean setDis(float d, int next) {
		d = (float) (Math.round(d * 100.0) / 100.0);
		if((nextHop == next && d != Distance) || d < Distance) {
			Distance = d;
			nextHop = next;
			return true;
		}
		return false;
	}

	public void setNeiDis(float neiD) {
		neighborDis = neiD;
		if (nextHop == destPort) {
			Distance = neighborDis;
		} else {
			this.setDis(neiD, destPort);
		}
	}

	public int getPort() {
		return destPort;
	}

	public boolean isNeighbor() {
		return (sendNeighbor || recvNeighbor);
	}

	public int getNext() {
		return nextHop;
	}

	public long getTime() {
		return timeStamp;
	}

	public boolean setTime(long time) {
		if (time > timeStamp) {
			timeStamp = time;
			return true;
		}
		return false;
	}
}
