package sr;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.List;

import segment.Model;
import segment.Segment;

public class UdpSr {

	private static final int MAX_LENGTH = 1024; // 单个消息的最大数据长度
	private static final int SIZE = 50; // 发送窗口的大小

	private String remoteHost; // 通信另一方的主机
	private int remotePort; // 通信另一方的端口
	private int port; // 本地端口
	private String host; // 本地地址
	private DatagramSocket socket; // 下层UDP接口
	private Timer timer; // 计时器

	private SendWin sendWin = null; // 发送窗口
	private RecieveWin recieveWin = null; // 接收窗口

	private byte[] reserve = new byte[MAX_LENGTH]; // 数据缓存区
	private int reservesize = 0; // 缓存的数据量

	/**
	 * 构造方法，绑定本地的{@code IP},并且将其与{@code port} 绑定。
	 * 
	 * @param port 需要绑定的端口号
	 * @throws SocketException
	 */
	public UdpSr(int port) throws SocketException {
		this.port = port;
		this.socket = new DatagramSocket(port);
		init();

		System.out.println("服务器运行在：" + this.host + ": " + this.port);
	}

	/**
	 * 构造器。绑定本地的{@code IP}和一个任意可用的端口。并且指定需要通信的主机的 IP和端口
	 * 
	 * @param remoteHost 通信另一方的主机
	 * @param remotePort 通信另一方的端口
	 * @throws IOException
	 */
	public UdpSr(String remoteHost, int remotePort) throws IOException {
		socket = new DatagramSocket();
		this.port = socket.getLocalPort();
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		init();

		System.out.println("send CONNECT..." + socket.getLocalPort());

		// 向通信另一方发送一个连接请求，告知自己的IP和端口号
		Segment segment = Segment.CONNECT();
		sendWin.send(segment);
	}

	/**
	 * 初始化参数
	 */
	private void init() {
		this.host = socket.getLocalAddress().getHostAddress();
		Model[] models = new Model[SIZE];
		for (int i = 0; i < SIZE; i++) {
			models[i] = new Model();
		}
		this.timer = new Timer(models, this, SIZE);
		this.sendWin = new SendWin(socket, remoteHost, remotePort, timer);
		this.recieveWin = new RecieveWin(sendWin, socket, this);
		this.recieveWin.start();
		timer.start();
	}

	/**
	 * 将数据发送出去。这个方法会阻塞，直到数据发送完毕
	 * 
	 * @param buf    数据缓存区
	 * @param length 有效数据长度
	 */
	public void send(byte[] buf, int length) {

		int reserve = length; // 还没发送的数据的长度
		Segment segment = new Segment();
		segment.setType(Segment.DATA);
		while (reserve > 0) {
			if (reserve > MAX_LENGTH) { // 如果剩余的数据大于最长数据，尽可能多的发送
				segment.setLength(MAX_LENGTH);
				segment.setData(buf, length - reserve);
				reserve -= MAX_LENGTH;
			} else { // 否则将全部数据发送
				segment.setLength(reserve);
				segment.setData(buf, length - reserve);
				reserve = 0;
			}
			try {
				while (sendWin.send(segment) == -1) { // 如果发送失败，则过一会再试。
					Thread.sleep(10);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * 读数据
	 * 
	 * @param buf 存储读入数据的缓存区
	 * @return 读入的字节数
	 */
	public int recieve(byte[] buf) {

		// 如果缓存的数据大于请求的数据，直接满足请求
		int read;
		if (reservesize >= buf.length) {
			read = buf.length;
			for (int i = 0; i < read; i++) {
				buf[i] = reserve[i];
			}
			for (int i = read; i < reservesize; i++) {
				reserve[i - read] = reserve[i];
			}
			reservesize -= read;
			return read;
		}

		// 如果没有任何数据，直接返回-1
		if ((recieveWin == null) && reservesize == 0) {
			return -1;
		}

		// 从接收窗口请求数据
		List<Segment> segments = recieveWin.revieve(buf.length - reservesize);
		if (segments.isEmpty() && reservesize == 0) { // 没有任何数据则返回
			return -1;
		}

		int datasize = reservesize;
		read = 0;
		for (int i = 0; i < reservesize; i++) { // 将缓存的数据写入
			buf[read++] = reserve[i];
		}

		if (!segments.isEmpty()) { // 如果从发送窗口请求到了消息，则写入
			int size = segments.size();
			for (int i = 0; i < size - 1; i++) { // 除最后一个消息的数据，一定小于请求的消息量
				datasize += segments.get(i).getLength();
				byte[] bs = segments.get(i).getBytes();
				for (int j = 3; j < bs.length; j++) {
					buf[read++] = bs[j];
				}
			}
			byte[] bs = segments.get(size - 1).getBytes();

			datasize = datasize + segments.get(size - 1).getLength(); // 记录数据总量
			size = datasize > buf.length ? buf.length : datasize; // 记录可读消息的总数
			int s = size - read;
			for (int i = 0; i < s; i++) { // 读取最后的消息
				buf[read++] = bs[i + 3];
			}
			int index = 0;
			for (int i = s + 3; i < bs.length; i++) { // 将多余数据放入缓冲区
				reserve[index++] = bs[i];
			}
		}
		reservesize = datasize - read;
		return read;
	}

	/**
	 * 记录下通信另一方的地址端口信息，用以初始化。
	 * 
	 * @param packet 通信另一方发送的连接包
	 */
	public void accept(DatagramPacket packet) {
		this.remoteHost = packet.getAddress().getHostAddress();
		this.remotePort = packet.getPort();
		sendWin.setRemoteHost(remoteHost);
		sendWin.setRemotePort(remotePort);
	}

	/**
	 * 超时重传
	 * 
	 * @throws IOException
	 */
	public void timeOut(int index) throws IOException {
		this.sendWin.timeOut(index);
	}

	/**
	 * 关闭当前的 UdpSr
	 */
	public void close() {
		this.recieveWin.close();
		socket.close();
	}
}
