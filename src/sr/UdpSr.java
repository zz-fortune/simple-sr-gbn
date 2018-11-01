package sr;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.List;

import segment.Model;
import segment.Segment;

public class UdpSr {

	private static final int MAX_LENGTH = 1024;
	private static final int SIZE = 50;

	private String remoteHost;
	private int remotePort;
	private int port;
	private String host;
	private DatagramSocket socket;
	private Timer timer;
	private Model[] models;

	private SendWin sendWin = null;
	private RecieveWin recieveWin = null;

	private byte[] reserve = new byte[MAX_LENGTH];
	private int reservesize = 0;

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
	 * ���췽�����󶨱��ص�{@code IP}��һ��������õĶ˿ڡ�
	 * 
	 * @throws IOException
	 * 
	 */
	public UdpSr(String remoteHost, int remotePort) throws IOException {
		socket = new DatagramSocket();
		this.port = socket.getLocalPort();
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		init();

		System.out.println("send CONNECT..." + socket.getLocalPort());

		Segment segment = Segment.CONNECT();
		sendWin.send(segment);
	}

	/**
	 * ��ʼ������
	 */
	private void init() {
		this.host = socket.getLocalAddress().getHostAddress();
		this.models = new Model[SIZE];
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
	 * �����ݷ��ͳ�ȥ��
	 * 
	 * @param buf    ���ݻ�����
	 * @param length ��Ч���ݳ���
	 */
	public void send(byte[] buf, int length) {

		int reserve = length;
		Segment segment = new Segment();
		segment.setType(Segment.DATA);
		while (reserve > 0) {
			if (reserve > MAX_LENGTH) {
				segment.setLength(MAX_LENGTH);
				segment.setData(buf, length - reserve);
				reserve -= MAX_LENGTH;
			} else {
				segment.setLength(reserve);
				segment.setData(buf, length - reserve);
				reserve = 0;
			}
			try {
				while (sendWin.send(segment) == -1) {
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

		if ((recieveWin == null) && reservesize == 0) {
			return -1;
		}

		List<Segment> segments = recieveWin.revieve(buf.length - reservesize);
		if (segments.isEmpty() && reservesize == 0) {
			return -1;
		}

		int index = 0;
		for (int i = 0; i < reservesize; i++) {
			buf[index++] = reserve[i];
		}

		int size = segments.size();
		for (int i = 0; i < size; i++) {
			byte[] bs = segments.get(i).getBytes();
			for (int j = 3; j < bs.length; j++) {
				buf[index++] = bs[j];
				if (index >= buf.length) {
					reservesize = j;
					break;
				}
			}
		}

		if (index == buf.length) {
			byte[] bs = segments.get(segments.size() - 1).getBytes();
			for (int i = reservesize + 1; i < bs.length; i++) {
				reserve[i - reservesize - 1] = bs[i];
			}
			reservesize = bs.length - reservesize - 1;
		}
		return index;
	}

	public void accept(DatagramPacket packet) {
		this.remoteHost = packet.getAddress().getHostAddress();
		this.remotePort = packet.getPort();
		sendWin.setRemoteHost(remoteHost);
		sendWin.setRemotePort(remotePort);
	}

	public void timeOut(int index) throws IOException {
		this.sendWin.timeOut(index);
	}

}
