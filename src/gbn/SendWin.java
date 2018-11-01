package gbn;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import segment.Segment;

/**
 * GBN协议的发送窗口，负责发送上层交付的数据，通过重传确认的机制保证数据传输的可靠性。
 * 
 * @author zz
 *
 */
public class SendWin {

	private static final int SEQ_SIZE = 256; // sequence number 的最大范围

	private int SIZE = 50; // 发送窗口的大小
	private Segment[] buffer = new Segment[SIZE]; // 发送窗口的缓存区
	private int base = 0; // 待确认的第一个消息在缓存区的位置
	private int current = 0; // 当前可用的缓存区位置
	private int seqnum = 0; // 当前可用的第一个序列号
	private int seqbase = 0; // 待确认的第一个消息的序列号

	private DatagramSocket socket; // 下层UDP接口
	private String remoteHost; // 通信另一方的主机
	private int remotePort; // 通信另一方的端口

	private Timer timer; // 计时器

	/**
	 * 构造器
	 * 
	 * @param socket     下层UDP接口
	 * @param remoteHost 通信另一方的主机
	 * @param remotePort 通信另一方的端口
	 * @param timer      计时器
	 */
	public SendWin(DatagramSocket socket, String remoteHost, int remotePort, Timer timer) {
		this.socket = socket;
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		this.timer = timer;
	}

	/**
	 * 设置通信另一方的主机。初始化时调用
	 * 
	 * @param remoteHost 通信另一方的主机
	 */
	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	/**
	 * 设置通信另一方的端口。初始化时调用
	 * 
	 * @param remotePort 通信另一方的端口
	 */
	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	/**
	 * 超时重传。重传未确认已发送的所有消息
	 * 
	 * @throws IOException
	 */
	public void timeOut() throws IOException {

		System.out.println("time out...resend..." + socket.getLocalPort());

		for (int i = base; i < (current + SIZE - base) % SIZE; i++) {
			int t = i % SIZE;
			DatagramPacket packet = new DatagramPacket(buffer[t].getBytes(), buffer[t].getLength() + 3,
					InetAddress.getByName(remoteHost), remotePort);
			socket.send(packet);

			System.out.println("resend segment " + buffer[t].getSeqnum() + "..." + socket.getLocalPort());
		}
	}

	/**
	 * 确认ACK
	 * 
	 * @param acknum ack number 
	 */
	public void ack(int acknum) {
		int num = (acknum + SEQ_SIZE - seqbase) % SEQ_SIZE;
		if (num < SIZE) {  //如果收到的ACK在有效范围内
			num = (num + base) % SIZE;
			if (((current < base) && !(num < base && num >= current))
					|| (current > base && num >= base && num < current)) { // 如果是待确认的消息的序列号，滑动窗口
				this.base = (num + 1) % SIZE;
				this.seqbase = (acknum + 1) % SEQ_SIZE;
				if (base == current) { //若无待确认消息，关闭计时器
					timer.close();
				} else { // 否则重启计时器
					timer.restart();
				}

			}
		}

		System.out.println("recieve ack " + acknum + "..." + socket.getLocalPort());
	}

	/**
	 * 发送ACK消息
	 * 
	 * @param seqnum 确认的序列号
	 * @throws IOException
	 */
	public void sendAck(int seqnum) throws IOException {

		System.out.println("send ack " + seqnum + "..." + socket.getLocalPort());
		Segment segment = Segment.ACK(seqnum);
		send(segment);
	}

	/**
	 * 发送一个GBN消息。如果发送成功，返回发送数据的长度，否则返回-1
	 * 
	 * @param segment 需要发送的GBN消息
	 * @return 发送数据的长度，或者不成功时的-1
	 * @throws IOException
	 */
	public int send(Segment segment) throws IOException {
		if (segment.getType() == Segment.DATA) { //若是数据消息，需要为其添加序列号
			if ((current + 1) % SIZE == base) {  // 缓存区满，返回-1
				return -1;
			}
			segment.setSeqnum(this.seqnum);
			buffer[current] = segment;
			if (current == base) { // 这是第一个消息，则启动计时器
				timer.restart();
			}

			System.out.println("send segment " + seqnum + "..." + socket.getLocalPort());

			this.current = (current + 1) % SIZE;
			this.seqnum = (seqnum + 1) % SEQ_SIZE;
		}

		DatagramPacket packet = new DatagramPacket(segment.getBytes(), segment.getLength() + 3,
				InetAddress.getByName(remoteHost), remotePort);
		socket.send(packet);
		return segment.getLength();
	}

}
