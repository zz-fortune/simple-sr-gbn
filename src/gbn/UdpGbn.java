package gbn;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import segment.Model;
import segment.Segment;

public class UdpGbn {

	private static final int MAX_LENGTH = 1024;

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
	public UdpGbn(int port) throws SocketException {
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
	public UdpGbn(String remoteHost, int remotePort) throws IOException {
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
		this.host = socket.getLocalAddress().getHostAddress(); // ��ñ��ص�ַ
		Model model = new Model();
		this.timer = new Timer(model, this); // ������ʱ��
		this.sendWin = new SendWin(socket, remoteHost, remotePort, timer);
		this.recieveWin = new RecieveWin(sendWin, socket, this);
		this.recieveWin.start();
		timer.start(); // ������ʱ���߳�
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
					Thread.sleep(20);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * �����ݡ����������������
	 * 
	 * @param buf �洢�������ݵĻ�����
	 * @return ������ֽ���
	 */
	public int recieve(byte[] buf) {

		int read = reservesize > buf.length ? buf.length : reservesize;
		for (int i = 0; i < read; i++) {
			buf[i] = reserve[i];
		}
		reservesize-=read;

		if ((recieveWin == null) && read == 0) {
			return -1;
		}
		
		Segment segment = recieveWin.revieve();
		if ((segment == null) && read == 0) {
			return -1;
		}else if () {
			
		}
		
		

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
		Segment segment = recieveWin.revieve();
		if ((segment == null) && reservesize == 0) {
			return -1;
		}
		byte[] bs = segment.getBytes();
		int size = segment == null ? reservesize : segment.getLength() + reservesize;
		read = size > buf.length ? buf.length : size;
		if (read < reservesize) {
			for (int i = 0; i < reservesize; i++) {
				buf[i] = reserve[i];
			}
			for (int i = reservesize; i < read; i++) {
				buf[i] = bs[i - reservesize + 3];
			}
			int index = 0;
			for (int i = read - reservesize; i < size; i++) {
				reserve[index++] = bs[i];
			}
			reservesize = size - read;
		}
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
	public void timeOut() throws IOException {
		this.sendWin.timeOut();
	}

	/**
	 * �رյ�ǰ�� UdpGbn
	 */
	public void close() {
		this.recieveWin.close();
		socket.close();
	}

}
