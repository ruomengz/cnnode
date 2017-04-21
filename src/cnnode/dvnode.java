package cnnode;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;

/** 
 * @author rz2357
 * @version 1.0 
 */ 

public class dvnode {
	static int localPort;
	static HashMap<Integer, RoutingTable> routingTable = new HashMap<Integer, RoutingTable>();
	public static boolean initFlag;

	public static void main(String[] args) throws UnknownHostException {
		
		localPort = Integer.parseInt(args[0]);
		dvnode.initFlag = true;
		// 4444 2222 .8 3333 .5 last
		if(1024 <= localPort && localPort <= 65534) {
			for (int i = 2; i < args.length; i += 2) {
				int port = Integer.parseInt(args[i-1]);
				float dis = Float.parseFloat(args[i]);
				if (port >= 1024 && port <= 65534) {
					routingTable.put(port, new RoutingTable(port, dis, true, port));
				}
				else {
					System.out.println("Port number range is 1024 ~ 65534"); 
					System.exit(0);
				}
			}
			// show routing table
			ShowRouting();
			if (args[args.length-1].equals("last")) {
				// the last node
				dvnode.initFlag = false;
				new Thread(new Broadcast(routingTable, localPort)).start(); 
			}
			
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
				while(true) {
					byte[] receiveData = new byte[64];
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					receiveSocket.receive(receivePacket);
					String recvString = new String(receiveData, 0, receivePacket.getLength());
					if(recvString != null) {
						// get a new message
						Thread t = new Thread((new dvnode()).new UpdateTable(recvString));
						t.start();
						t.join();
					}
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("Port number range is 1024 ~ 65534"); 
			}
	}

	
	public static void ShowRouting() {
		System.out.println("[" + Calendar.getInstance().getTimeInMillis() + "] Node " + localPort + " Routing Table");
		for(RoutingTable table: routingTable.values()) {
			System.out.print("- (" + table.getDis() + ") -> Node " + table.getPort()); 
			// if don't need jump
			if(table.getPort() == table.getNext()) {
				System.out.println("");
			}
			else {
				System.out.println(" ; Next hop -> Node " + table.getNext());
			}
		}
	}

	public class UpdateTable implements Runnable{
		String recvString;
		public UpdateTable(String recv) {
			recvString = recv;
		}
		
		@Override
		public void run() {
			boolean isUpdate = false;
			String[] recvData = recvString.split(" ");
			// [<timestamp>] Message received at Node <port-vvvv> from Node <port-xxxx>
			System.out.println("[" + Calendar.getInstance().getTimeInMillis() + "] Message received at Node " + localPort + " from Node " +  recvData[0] );
			RoutingTable tempTable = routingTable.get(Integer.parseInt(recvData[0]));
			if(tempTable != null && tempTable.setTime(Long.parseLong(recvData[1]))) {
				float d = tempTable.neiDis();
				// w is only eighbor distance
				for(int i = 2; i < recvData.length; i += 2) {
					int tempPort = Integer.parseInt(recvData[i]);
					if(routingTable.containsKey(tempPort)) {
						isUpdate = isUpdate || routingTable.get(tempPort).setDis( (d+Float.parseFloat(recvData[i+1])) , Integer.parseInt(recvData[0]));
					}
					else {
						if( tempPort != localPort) {
							routingTable.put(tempPort, new RoutingTable(tempPort, (d+Float.parseFloat(recvData[i+1]) ), false, Integer.parseInt(recvData[0])));
							isUpdate = true;
						}
					}
				}
			}
			
			if(isUpdate || dvnode.initFlag) {
				if(isUpdate) {
					ShowRouting();
				}
				dvnode.initFlag = false;
				new Thread(new Broadcast(routingTable, localPort)).start();
			}
				
		}
	}
}

// broadcast thread
class Broadcast implements Runnable {
	private HashMap<Integer, RoutingTable> Table;
	private int localPort;
    public Broadcast(HashMap<Integer, RoutingTable> table, int localport){  
        Table = table;
        localPort = localport;
    } 
    
	@Override
	public void run() {
		try {
			InetAddress serverAddress = InetAddress.getByName("localhost");
			byte[] sendData = new byte[64];
			// sending data, format <local> <time> <port> <distance>
			String sendString = "" + localPort + " " + Calendar.getInstance().getTimeInMillis();
			for(RoutingTable table: Table.values()) {
				sendString += " " + table.getPort() + " " + table.getDis();
			}
			sendData = sendString.getBytes();
			DatagramSocket sendSocket = new DatagramSocket();
			for(RoutingTable table: Table.values()) {
				if(table.isNeighbor()) {
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, table.getPort());
					sendSocket.send(sendPacket);
					System.out.println("[" + Calendar.getInstance().getTimeInMillis() + "] Message sent from Node " + localPort + " to Node " +  table.getPort() );
				}
			}
			sendSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		
	}
}

// class for routing table
class RoutingTable {
	private int destPort;
	private float Distance;
	private float neighborDis;
	private int nextHop;
	private boolean isNeighbor;
	private long timeStamp;
	
	public RoutingTable(int destport, float distance, boolean neighbor, int next) {
		destPort = destport;
		Distance = distance;
		timeStamp = Calendar.getInstance().getTimeInMillis();
		isNeighbor = neighbor;
		nextHop = next;
		if(isNeighbor) {
			neighborDis = distance;
		}
	}
	public float getDis() {
		return Distance;
	}
	public float neiDis() {
		return neighborDis;
	}
	public boolean setDis(float d, int next) {
		if(d < Distance) {
			Distance = d;
			nextHop = next;
			return true;
		}
		return false;
	}
	public int getPort() {
		return destPort;
	}
	public boolean isNeighbor() {
		return isNeighbor;
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