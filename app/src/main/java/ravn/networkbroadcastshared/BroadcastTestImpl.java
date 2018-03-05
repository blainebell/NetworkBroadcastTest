package ravn.networkbroadcastshared;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Random;

/**
 * Created by bell on 12/26/17.
 */

public class BroadcastTestImpl {
	public int computerID = -1;
	static int packet_size = 1024;
	static int udp_port = 2254, multicast_port = 2253;
	static String multicast_address = "224.0.0.24";
	DatagramSocket _udpSocket = null;
	MulticastSocket _multicastSocket = null;
	InetAddress multicastGroupAddress = null;
	int udp_message_cnt = 0, multicast_message_cnt = 0;
	public boolean udpIsConnected = false, multicastIsConnected = false;
	public boolean isInitialized = false;
	public void init(){
		Random rand = new Random(System.currentTimeMillis());
		computerID = Math.abs(rand.nextInt()); // just use positive numbers, easier to read
		isInitialized = true;
	}
	long lastUDPSendTime = 0l;
	public void sendUDPMessage(String broadcastAddressString){
		if (_udpSocket!=null) {
			byte ba[] = new byte[packet_size];
			int offset[] = new int[]{0};
			offset[0] += 2;
			StreamUtils.writeIntIntoByteArrayWithOffset(computerID, ba, offset);
			StreamUtils.writeStringToByteArrayWithOffset("receiving UDP message #" + udp_message_cnt, ba, offset);
			long curTime = System.currentTimeMillis();
			System.out.println("sending    UDP    message #" + udp_message_cnt + "\ttime=" + curTime + "\tsince last=" + (curTime-lastUDPSendTime));
			lastUDPSendTime = curTime;

			udp_message_cnt++;
			StreamUtils.computeAndWriteCheckSum(ba);
			try {
				InetAddress broadcastAddress = InetAddress.getByName(broadcastAddressString);
				DatagramPacket hi = new DatagramPacket(ba, ba.length, broadcastAddress, udp_port);
				_udpSocket.send(hi);
			} catch (IOException ex){
				System.out.println("sendUDPMessage: WARNING: send failed: address=" + broadcastAddressString + " packet_size=" + packet_size + " ex.message=" + ex.getMessage());
					
			}
		}
	}
	public long lastMulticastSendTime = 0l;
	public void sendMulticastMessage(){
		if (_multicastSocket!=null) {
			byte ba[] = new byte[packet_size];
			int offset[] = new int[]{0};
			offset[0] += 2;
			StreamUtils.writeIntIntoByteArrayWithOffset(computerID, ba, offset);
			StreamUtils.writeStringToByteArrayWithOffset("receiving Multicast message #" + multicast_message_cnt, ba, offset);
			long curTime = System.currentTimeMillis();
			System.out.println("sending Multicast message #" + multicast_message_cnt + "\ttime=" + curTime + "\tsince last=" + (curTime-lastMulticastSendTime));
			lastMulticastSendTime = curTime;
			multicast_message_cnt++;
			StreamUtils.computeAndWriteCheckSum(ba);
			DatagramPacket hi = new DatagramPacket(ba, ba.length, multicastGroupAddress, multicast_port);
			try {
				_multicastSocket.send(hi);
			} catch (IOException ex){
				System.out.println("sendMulticastMessage: WARNING: send failed: ex.message=" + ex.getMessage());
			}
		}
	}
	public interface MessageCallback {
		public void call(String msg);
		public void addTextLineToLog(String txt);
		public void udpConnected(boolean con);
		public void multicastConnected(boolean con);
	}
	class StoppableThread extends Thread {
		public boolean cont=true;
		public StoppableThread(){
		}
	};
	StoppableThread receiveUDPThread = null, receiveMulticastThread = null;
	StoppableThread repeatUDPSendThread = null, repeatMulticastSendThread = null;
	public static MessageCallback messageCall = null;
	public static void sendMessage(String msg){
		if (messageCall!=null){
			messageCall.call(msg);
		}
	}
	public void clearListeningThreads() {
		udp_message_cnt = multicast_message_cnt = 0;
		if (_udpSocket!=null){
			_udpSocket.close();
			_udpSocket = null;
		}
		if (receiveUDPThread!=null) {
			receiveUDPThread.cont = false;
			try {
				System.out.println("joining receiveUDPThread");
				receiveUDPThread.join();
			} catch (InterruptedException ie){
				ie.printStackTrace();
			}
			System.out.println("receiveUDPThread finished");
			receiveUDPThread = null;
		}
		udpIsConnected = false;
		if (_multicastSocket!=null){
			_multicastSocket.close();
			_multicastSocket = null;
		}
		if (receiveMulticastThread!=null) {
			receiveMulticastThread.cont = false;
			try {
				System.out.println("joining receiveMulticastThread");
				receiveMulticastThread.join();
			} catch (InterruptedException ie){
				ie.printStackTrace();
			}
			System.out.println("receiveMulticastThread finished");
			receiveMulticastThread = null;
		}
		multicastIsConnected = false;
	}
	public void setupListeningThreads() {
		System.out.println("setupListeningThreads called");
		clearListeningThreads();
		try {
			_udpSocket = new DatagramSocket(udp_port);
			_udpSocket.setBroadcast(true);
			_udpSocket.setReuseAddress(true);
            _udpSocket.setSoTimeout(0);
			udpIsConnected = true;
		} catch (BindException e){
			sendMessage("Error creating binding UDP Socket, is it already in use : " + e.getMessage());
		} catch (Exception e){
			sendMessage("Error creating UDP Socket");
			e.printStackTrace();
		}
		if (messageCall!=null)
			messageCall.udpConnected(udpIsConnected);
		if (udpIsConnected) {
			receiveUDPThread = new StoppableThread(){
				public void run() {
					byte[] buf = new byte[packet_size];
					DatagramPacket recv = new DatagramPacket(buf, buf.length);
					while (cont){
						try {
                            recv.setData(buf);
							_udpSocket.receive(recv);
							int offset [] = new int[] { 0 };
							byte checksum[] = new byte[2];
							StreamUtils.computeCheckSum(buf, checksum);
							if (checksum[0] == buf[0] && checksum[1] == buf[1]) {
								offset[0] = 2;// skip checksum
								int from = StreamUtils.readIntFromByteArrayWithOffset(buf, offset);
								String readString = StreamUtils.readStringFromByteArrayWithOffset(buf, offset);
								if (from != computerID){
									if (messageCall!=null) {
										messageCall.addTextLineToLog("UDP\tFrom: " + from + " : " + readString);
									}
								}
							} else {
								if (messageCall!=null) {
									messageCall.addTextLineToLog("Received incorrect checksum on UDP");
								}
							}
						} catch (IOException ioe){
							sendMessage("Error receiving UDP Packet message='" + ioe.getMessage() + "'");
							ioe.printStackTrace();
						}
						try {
							Thread.currentThread().sleep(100);
						} catch (	Exception ex2){
							ex2.printStackTrace();
						}
					}
				}
			};
			receiveUDPThread.start();
		}
		try {
			_multicastSocket = new MulticastSocket(multicast_port);
			String ipAddress = SocketUtils.getLocalIPAddress();
			System.out.println("ipAddress=" + ipAddress);
			multicastGroupAddress = InetAddress.getByName(multicast_address);
			System.out.println("multicast_address=" + multicast_address + " multicastGroupAddress=" + multicastGroupAddress + " multicast_port=" + multicast_port);
			_multicastSocket.joinGroup(multicastGroupAddress);
			NetworkInterface curNI = SocketUtils.getLocalNetworkInterface();
			_multicastSocket.setNetworkInterface(curNI);
			_multicastSocket.setSoTimeout(0);
			System.out.println("Multicast Socket created: ipAddress=" + ipAddress + " interface: " + _multicastSocket.getInterface() + " network interface: " + _multicastSocket.getNetworkInterface());
			multicastIsConnected = true;
		} catch (Exception e){
			sendMessage("Error creating Multicast Socket");
			e.printStackTrace();
		}
		if (messageCall!=null)
			messageCall.multicastConnected(multicastIsConnected);

		if (multicastIsConnected) {
			receiveMulticastThread = new StoppableThread(){
				public void run() {
					byte[] buf = new byte[packet_size];
					DatagramPacket recv = new DatagramPacket(buf, buf.length);
					while (cont){
						try {
                            recv.setData(buf);
							_multicastSocket.receive(recv);
							int offset [] = new int[] { 0 };
							byte checksum[] = new byte[2];
							StreamUtils.computeCheckSum(buf, checksum);
							if (checksum[0] == buf[0] && checksum[1] == buf[1]) {
								offset[0] = 2;// skip checksum
								int from = StreamUtils.readIntFromByteArrayWithOffset(buf, offset);
								String readString = StreamUtils.readStringFromByteArrayWithOffset(buf, offset);
								if (from != computerID){
									if (messageCall!=null) {
										messageCall.addTextLineToLog("Multicast\tFrom: " + from + " : " + readString);
									}
								}
							} else {
								if (messageCall!=null) {
									messageCall.addTextLineToLog("Received incorrect checksum on Multicast");
								}
							}
						} catch (IOException ioe){
							sendMessage("Error receiving Multicast Packet message='" + ioe.getMessage() + "'");
							ioe.printStackTrace();
						}
						try {
							Thread.currentThread().sleep(100);
						} catch (	Exception ex2){
							ex2.printStackTrace();
						}
					}
				}
			};
			receiveMulticastThread.start();
		}
	}
	public void repeatMulticastSendThreadSet(boolean val){
		if (repeatMulticastSendThread!=null){
			repeatMulticastSendThread.cont = false;
			repeatMulticastSendThread = null;
		}
		if (val){
			repeatMulticastSendThread = new StoppableThread(){
				public void run() {
					while (cont){
						sendMulticastMessage();
						try {
							Thread.currentThread().sleep(2000L);
						} catch (Exception ex){}
					}
				}
			};
			repeatMulticastSendThread.start();
		}
	}
	public void repeatUDPSendThreadSet(String broadcastAddressString, boolean val){
		if (repeatUDPSendThread!=null){
			repeatUDPSendThread.cont = false;
			repeatUDPSendThread = null;
		}
		if (val){
			final String broadcastAddress = broadcastAddressString;
			repeatUDPSendThread = new StoppableThread(){
				public void run() {
					while (cont){
						sendUDPMessage(broadcastAddress);
						try {
							Thread.currentThread().sleep(2000L);
						} catch (Exception ex){}
					}
				}
			};
			repeatUDPSendThread.start();
		}
	}
}
