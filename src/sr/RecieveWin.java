package sr;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

import segment.Segment;

public class RecieveWin extends Thread {

	private static final int BUF_SIZE = 1472;
	private static final int SIZE = 40;
	private static final int SEQ_SIZE = 256;

	private Segment[] buffer = new Segment[SIZE];
	private boolean[] flags = new boolean[SIZE];
	private SendWin sendWin;
	private DatagramSocket socket;
	private UdpSr sr;

	private boolean running = true;

	private int expected = 0;
	private int recievebase = 0;

	/**
	 * 构造器
	 * 
	 * @param sendWin 发送窗口
	 */
	public RecieveWin(SendWin sendWin, DatagramSocket socket, UdpSr sr) {
		this.sendWin = sendWin;
		this.socket = socket;
		this.sr = sr;
		for (int i = 0; i < SIZE; i++) {
			flags[i] = false;
		}
	}

	@Override
	public void run() {
		byte[] buf = new byte[BUF_SIZE];

		System.out.println("接收窗口启动..." + socket.getLocalPort());

		while (running) {
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(packet);
			} catch (IOException e) {
				System.out.println("读取数据出错！");
				e.printStackTrace();
			}
			Segment segment = new Segment(packet.getData());
			if (segment.getType() == Segment.ACK) {
				sendWin.ack(segment.getAcknum());
			} else if (segment.getType() == Segment.DATA) {
				add(segment);
			} else if (segment.getType() == Segment.CONNECT) {
				sr.accept(packet);
			}
		}
	}

	private void add(Segment segment) {
		int num = segment.getSeqnum();
		if (num % 10 == 7) {
			return;
		}
		if ((num + SEQ_SIZE - expected) % SEQ_SIZE < SIZE) {
			buffer[(recievebase + (num + SEQ_SIZE - expected) % SEQ_SIZE) % SIZE] = segment;
			flags[(recievebase + (num + SEQ_SIZE - expected) % SEQ_SIZE) % SIZE] = true;
			try {
				sendWin.sendAck(segment.getSeqnum());
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("recieve segment " + segment.getSeqnum() + "..." + socket.getLocalPort());

		}

	}

	public List<Segment> revieve(int length) {
		List<Segment> segments = new ArrayList<>();
		int num = 0;
		int size = 0;
		for (int i = 0; i < SIZE; i++) {
			int t = (i + recievebase) % SIZE;
			if (!flags[t] || size >= length) {
				break;
			}
			segments.add(buffer[t]);
			flags[t] = false;
			num++;
			size += buffer[t].getLength();
		}
		expected = (expected + num) % SEQ_SIZE;
		recievebase = (recievebase + num) % SIZE;

		if (!segments.isEmpty()) {
			System.out.println("recieve segmengts " + segments.get(0).getSeqnum() + " - "
					+ segments.get(segments.size() - 1).getSeqnum() + "..." + socket.getLocalPort());
		}

		return segments;
	}

}
