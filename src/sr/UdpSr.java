package sr;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.List;

import segment.Model;
import segment.Segment;

public class UdpSr {

	private static final int MAX_LENGTH = 1024; // ������Ϣ��������ݳ���
	private static final int SIZE = 50; // ���ʹ��ڵĴ�С

	private String remoteHost; // ͨ����һ��������
	private int remotePort; // ͨ����һ���Ķ˿�
	private int port; // ���ض˿�
	private String host; // ���ص�ַ
	private DatagramSocket socket; // �²�UDP�ӿ�
	private Timer timer; // ��ʱ��

	private SendWin sendWin = null; // ���ʹ���
	private RecieveWin recieveWin = null; // ���մ���

	private byte[] reserve = new byte[MAX_LENGTH]; // ���ݻ�����
	private int reservesize = 0; // �����������

	/**
	 * ���췽�����󶨱��ص�{@code IP},���ҽ�����{@code port} �󶨡�
	 * 
	 * @param port ��Ҫ�󶨵Ķ˿ں�
	 * @throws SocketException
	 */
	public UdpSr(int port) throws SocketException {
		this.port = port;
		this.socket = new DatagramSocket(port);
		init();

		System.out.println("�����������ڣ�" + this.host + ": " + this.port);
	}

	/**
	 * ���������󶨱��ص�{@code IP}��һ��������õĶ˿ڡ�����ָ����Ҫͨ�ŵ������� IP�Ͷ˿�
	 * 
	 * @param remoteHost ͨ����һ��������
	 * @param remotePort ͨ����һ���Ķ˿�
	 * @throws IOException
	 */
	public UdpSr(String remoteHost, int remotePort) throws IOException {
		socket = new DatagramSocket();
		this.port = socket.getLocalPort();
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		init();

		System.out.println("send CONNECT..." + socket.getLocalPort());

		// ��ͨ����һ������һ���������󣬸�֪�Լ���IP�Ͷ˿ں�
		Segment segment = Segment.CONNECT();
		sendWin.send(segment);
	}

	/**
	 * ��ʼ������
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
	 * �����ݷ��ͳ�ȥ�����������������ֱ�����ݷ������
	 * 
	 * @param buf    ���ݻ�����
	 * @param length ��Ч���ݳ���
	 */
	public void send(byte[] buf, int length) {

		int reserve = length; // ��û���͵����ݵĳ���
		Segment segment = new Segment();
		segment.setType(Segment.DATA);
		while (reserve > 0) {
			if (reserve > MAX_LENGTH) { // ���ʣ������ݴ�������ݣ������ܶ�ķ���
				segment.setLength(MAX_LENGTH);
				segment.setData(buf, length - reserve);
				reserve -= MAX_LENGTH;
			} else { // ����ȫ�����ݷ���
				segment.setLength(reserve);
				segment.setData(buf, length - reserve);
				reserve = 0;
			}
			try {
				while (sendWin.send(segment) == -1) { // �������ʧ�ܣ����һ�����ԡ�
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
	 * ������
	 * 
	 * @param buf �洢�������ݵĻ�����
	 * @return ������ֽ���
	 */
	public int recieve(byte[] buf) {

		// �����������ݴ�����������ݣ�ֱ����������
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

		// ���û���κ����ݣ�ֱ�ӷ���-1
		if ((recieveWin == null) && reservesize == 0) {
			return -1;
		}

		// �ӽ��մ�����������
		List<Segment> segments = recieveWin.revieve(buf.length - reservesize);
		if (segments.isEmpty() && reservesize == 0) { // û���κ������򷵻�
			return -1;
		}

		int datasize = reservesize;
		read = 0;
		for (int i = 0; i < reservesize; i++) { // �����������д��
			buf[read++] = reserve[i];
		}

		if (!segments.isEmpty()) { // ����ӷ��ʹ�����������Ϣ����д��
			int size = segments.size();
			for (int i = 0; i < size - 1; i++) { // �����һ����Ϣ�����ݣ�һ��С���������Ϣ��
				datasize += segments.get(i).getLength();
				byte[] bs = segments.get(i).getBytes();
				for (int j = 3; j < bs.length; j++) {
					buf[read++] = bs[j];
				}
			}
			byte[] bs = segments.get(size - 1).getBytes();

			datasize = datasize + segments.get(size - 1).getLength(); // ��¼��������
			size = datasize > buf.length ? buf.length : datasize; // ��¼�ɶ���Ϣ������
			int s = size - read;
			for (int i = 0; i < s; i++) { // ��ȡ������Ϣ
				buf[read++] = bs[i + 3];
			}
			int index = 0;
			for (int i = s + 3; i < bs.length; i++) { // ���������ݷ��뻺����
				reserve[index++] = bs[i];
			}
		}
		reservesize = datasize - read;
		return read;
	}

	/**
	 * ��¼��ͨ����һ���ĵ�ַ�˿���Ϣ�����Գ�ʼ����
	 * 
	 * @param packet ͨ����һ�����͵����Ӱ�
	 */
	public void accept(DatagramPacket packet) {
		this.remoteHost = packet.getAddress().getHostAddress();
		this.remotePort = packet.getPort();
		sendWin.setRemoteHost(remoteHost);
		sendWin.setRemotePort(remotePort);
	}

	/**
	 * ��ʱ�ش�
	 * 
	 * @throws IOException
	 */
	public void timeOut(int index) throws IOException {
		this.sendWin.timeOut(index);
	}

	/**
	 * �رյ�ǰ�� UdpSr
	 */
	public void close() {
		this.recieveWin.close();
		socket.close();
	}
}
