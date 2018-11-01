package gbn;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

import segment.Segment;

/**
 * GBN协议的接收窗口。负责一直监听下层是否有数据提交，并将数据存在缓存区内，
 * 在需要时交付给上层。
 * 
 * 接收窗口接收到消息时，会通告发送窗口进行相应的处理
 * 
 * @author zz
 *
 */
public class RecieveWin extends Thread {

	private static final int BUF_SIZE = 1472;  //单个消息的最大长度

//	private int SIZE;
	private int SEQ_SIZE = 256;   // sequence number 的最大范围
	private List<Segment> buffer = new ArrayList<>();  //接收缓存区
	private boolean available = true;  //缓存区是否可用
	private SendWin sendWin;         //发送窗口
	private DatagramSocket socket;   // udp的接口
	private UdpGbn gbn;             // GBN协议实现的接口

	private boolean running = true;   // 发送窗口是否运行

	private int expected = 0;     // 期望收到的 sequence number

	/**
	 * 构造器。
	 * 
	 * @param sendWin 发送窗口
	 * @param socket GBN 协议依赖的下层接口
	 * @param gbn GBN 提供的上层接口，是GBN的主模块
	 */
	public RecieveWin(SendWin sendWin, DatagramSocket socket, UdpGbn gbn) {
		this.sendWin = sendWin;
		this.socket = socket;
		this.gbn = gbn;
	}

	@Override
	public void run() {
		byte[] buf = new byte[BUF_SIZE];

		System.out.println("接收窗口启动..." + socket.getLocalPort());

		while (running) {
			DatagramPacket packet = new DatagramPacket(buf, buf.length); //构造一个 receive package
			try {
				socket.receive(packet);  // 阻塞读取数据
			} catch (IOException e) {
				System.out.println("读取数据出错！");
				e.printStackTrace();
			}
			Segment segment = new Segment(packet.getData());
			
			//分类处理数据
			if (segment.getType() == Segment.ACK) {// 通告发送窗口发送ACK
				sendWin.ack(segment.getAcknum());  
			} else if (segment.getType() == Segment.DATA) { // 将收到的数据放入缓存区
				add(segment);
			} else if (segment.getType() == Segment.CONNECT) { // 通知主模块绑定客户端的地址
				gbn.accept(packet);
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
		
		//引入丢包
		if (num % 10 == 7) {
			return;
		}
		
		// 如果收到的消息的序列号是期望的序列号，并且缓存区可用，则向发送方确认该消息
		if ((num == expected) && available) {
			buffer.add(segment);
			available = false;
			expected = (expected + 1) % SEQ_SIZE;
		} else {         // 重复确认之前的消息
			num = (expected + SEQ_SIZE - 1) % SEQ_SIZE;
		}
		try {
			sendWin.sendAck(num);  // 发送ACK
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 如果缓存区有数据，返回。否则返回{@code null}
	 * 
	 * @return 数据
	 */
	public Segment revieve() {
		if (available) { // 如果缓冲区没有消息，返回null
			return null;
		}
		
		// 取出缓存区的消息，提交
		Segment segment = buffer.get(0);
		buffer.remove(0);
		available = true;

		System.out.println("recieve segmengt " + segment.getSeqnum() + "..." + socket.getLocalPort());

		return segment;
	}

	/**
	 * 关闭发送窗口
	 */
	public void close() {
		this.running=false;
	}
}
