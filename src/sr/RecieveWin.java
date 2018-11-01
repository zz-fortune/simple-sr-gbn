package sr;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

import segment.Segment;

/**
 * SR协议的接收窗口。负责一直监听下层是否有数据提交，并将数据存在缓存区内，
 * 在需要时交付给上层。
 * 
 * 接收窗口接收到消息时，会通告发送窗口进行相应的处理
 * 
 * @author zz
 *
 */
public class RecieveWin extends Thread {

	private static final int BUF_SIZE = 1472; // 单个消息的最大长度
	private static final int SIZE = 40; // 接收窗口的大小
	private static final int SEQ_SIZE = 256; // sequence number 的最大范围

	private Segment[] buffer = new Segment[SIZE]; // 接收缓存区
	private boolean[] flags = new boolean[SIZE]; // 对缓存区每个位置是否有数据的标记
	private SendWin sendWin; // 发送窗口
	private DatagramSocket socket; // udp的接口
	private UdpSr sr; // SR协议实现的接口

	private boolean running = true; // 发送窗口是否运行

	private int expected = 0; // 期望收到的 sequence number
	private int recievebase = 0; // 期望收到的第一个信息在缓存区中的位置

	/**
	 * 构造器。
	 * 
	 * @param sendWin 发送窗口
	 * @param socket  SR 协议依赖的下层接口
	 * @param sr      SR 提供的上层接口，是SR的主模块
	 */
	public RecieveWin(SendWin sendWin, DatagramSocket socket, UdpSr sr) {
		this.sendWin = sendWin;
		this.socket = socket;
		this.sr = sr;

		// 初始缓存区中没有数据
		for (int i = 0; i < SIZE; i++) {
			flags[i] = false;
		}
	}

	@Override
	public void run() {
		byte[] buf = new byte[BUF_SIZE];

		System.out.println("接收窗口启动..." + socket.getLocalPort());

		while (running) {
			DatagramPacket packet = new DatagramPacket(buf, buf.length);  //构造一个 receive package
			try {
				socket.receive(packet);  // 阻塞读取数据
			} catch (IOException e) {
				System.out.println("读取数据出错！");
				e.printStackTrace();
			}
			Segment segment = new Segment(packet.getData());
			if (segment.getType() == Segment.ACK) { // 通告发送窗口发送ACK
				sendWin.ack(segment.getAcknum());
			} else if (segment.getType() == Segment.DATA) { // 将收到的数据放入缓存区
				add(segment);
			} else if (segment.getType() == Segment.CONNECT) { // 通知主模块绑定客户端的地址
				sr.accept(packet);
			}
		}
	}

	/**
	 * 将收到的数据放入缓存区，并且在必要的情况下利用发送窗口发送ACK消息
	 * 
	 * @param segment 收到的数据消息
	 */
	private void add(Segment segment) {
		int num = segment.getSeqnum();
		if (num % 10 == 7) { // 模拟丢包
			return;
		}

		if ((num + SEQ_SIZE - expected) % SEQ_SIZE < SIZE) { // 如果数据未被上层接收的数据，且未超出缓存范围
			buffer[(recievebase + (num + SEQ_SIZE - expected) % SEQ_SIZE) % SIZE] = segment;
			flags[(recievebase + (num + SEQ_SIZE - expected) % SEQ_SIZE) % SIZE] = true;
			try {
				sendWin.sendAck(segment.getSeqnum()); // 发送AKC
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("recieve segment " + segment.getSeqnum() + "..." + socket.getLocalPort());

		} else if ((expected + SEQ_SIZE - num) % SEQ_SIZE < SIZE) { // 如果数据是已经被上层接收过的数据，仅发送ACK
			try {
				sendWin.sendAck(segment.getSeqnum());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * 返回大于指定长度的最少数据量。如果缓存区中数据少于指定长度，则返回全部
	 * 
	 * @param length 请求数据的长度
	 * @return 大于指定长度的最少数据量
	 */
	public List<Segment> revieve(int length) {
		List<Segment> segments = new ArrayList<>();
		int num = 0;
		int size = 0;
		for (int i = 0; i < SIZE; i++) {
			int t = (i + recievebase) % SIZE;
			if (!flags[t] || size >= length) { // 没有数据或者数据超过请求长度则返回
				break;
			}
			segments.add(buffer[t]);
			flags[t] = false;
			num++;
			size += buffer[t].getLength();
		}
		expected = (expected + num) % SEQ_SIZE; // 更新接收窗口
		recievebase = (recievebase + num) % SIZE;

		if (!segments.isEmpty()) {
			System.out.println("recieve segmengts " + segments.get(0).getSeqnum() + " - "
					+ segments.get(segments.size() - 1).getSeqnum() + "..." + socket.getLocalPort());
		}

		return segments;
	}
	
	public void close() {
		this.running=false;
	}

}
