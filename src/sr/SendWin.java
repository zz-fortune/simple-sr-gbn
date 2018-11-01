package sr;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import segment.Segment;

public class SendWin {

	private static final int SEQ_SIZE = 256;

	private int SIZE = 50;
	private Segment[] buffer = new Segment[SIZE];
	private boolean[] flags = new boolean[SIZE];
	private int base = 0;
	private int current = 0;
	private int seqnum = 0;
	private int seqbase = 0;

	private DatagramSocket socket;
	private String remoteHost;
	private int remotePort;

	private Timer timer;

	/**
	 * udp连接
	 * 
	 * @param socket
	 */
	public SendWin(DatagramSocket socket, String remoteHost, int remotePort, Timer timer) {
		this.socket = socket;
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		this.timer = timer;
		for (int i = 0; i < SIZE; i++) {
			flags[i] = false;
		}
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	/**
	 * 超时重传
	 * 
	 * @throws IOException
	 */
	public void timeOut(int index) throws IOException {

		System.out.println("time out...resend..." + socket.getLocalPort());
		DatagramPacket packet = new DatagramPacket(buffer[index].getBytes(), buffer[index].getLength() + 3,
				InetAddress.getByName(remoteHost), remotePort);
		socket.send(packet);

		System.out.println("resend segment " + buffer[index].getSeqnum() + "..." + socket.getLocalPort());
	}

	public void ack(int acknum) {
		int num = (acknum + SEQ_SIZE - seqbase) % SEQ_SIZE;
		if (num < SIZE) {
			num = (num + base) % SIZE;
			if (((current < base) && !(num < base && num >= current))
					|| (current > base && num >= base && num < current)) {
				flags[num] = true;
				timer.close(num);
				int number = 0;
				for (int i = 0; i < SIZE; i++) {
					int t = (i + base) % SIZE;
					if (!flags[t]) {
						break;
					}
					flags[t] = false;
				}
				this.base = (base + number) % SIZE;
				this.seqbase = (seqbase + number) % SEQ_SIZE;
			}
		}

		System.out.println("recieve ack " + acknum + "..." + socket.getLocalPort());
	}

	public void sendAck(int seqnum) throws IOException {

		System.out.println("send ack " + seqnum + "..." + socket.getLocalPort());
		Segment segment = Segment.ACK(seqnum);
		send(segment);
	}

	public int send(Segment segment) throws IOException {
		if (segment.getType() == Segment.DATA) {
			if ((current + 1) % SIZE == base) {
				return -1;
			}
			segment.setSeqnum(this.seqnum);
			buffer[current] = segment;
			timer.restart(current);

			System.out.println("send segment " + segment.getSeqnum() + "..." + socket.getLocalPort());

			this.current = (current + 1) % SIZE;
			this.seqnum = (seqnum + 1) % SEQ_SIZE;
		}

		DatagramPacket packet = new DatagramPacket(segment.getBytes(), segment.getLength() + 3,
				InetAddress.getByName(remoteHost), remotePort);
		socket.send(packet);
		return segment.getLength();
	}

}
