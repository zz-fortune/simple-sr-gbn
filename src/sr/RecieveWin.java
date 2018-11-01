package sr;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

import segment.Segment;

/**
 * SRЭ��Ľ��մ��ڡ�����һֱ�����²��Ƿ��������ύ���������ݴ��ڻ������ڣ�
 * ����Ҫʱ�������ϲ㡣
 * 
 * ���մ��ڽ��յ���Ϣʱ����ͨ�淢�ʹ��ڽ�����Ӧ�Ĵ���
 * 
 * @author zz
 *
 */
public class RecieveWin extends Thread {

	private static final int BUF_SIZE = 1472; // ������Ϣ����󳤶�
	private static final int SIZE = 40; // ���մ��ڵĴ�С
	private static final int SEQ_SIZE = 256; // sequence number �����Χ

	private Segment[] buffer = new Segment[SIZE]; // ���ջ�����
	private boolean[] flags = new boolean[SIZE]; // �Ի�����ÿ��λ���Ƿ������ݵı��
	private SendWin sendWin; // ���ʹ���
	private DatagramSocket socket; // udp�Ľӿ�
	private UdpSr sr; // SRЭ��ʵ�ֵĽӿ�

	private boolean running = true; // ���ʹ����Ƿ�����

	private int expected = 0; // �����յ��� sequence number
	private int recievebase = 0; // �����յ��ĵ�һ����Ϣ�ڻ������е�λ��

	/**
	 * ��������
	 * 
	 * @param sendWin ���ʹ���
	 * @param socket  SR Э���������²�ӿ�
	 * @param sr      SR �ṩ���ϲ�ӿڣ���SR����ģ��
	 */
	public RecieveWin(SendWin sendWin, DatagramSocket socket, UdpSr sr) {
		this.sendWin = sendWin;
		this.socket = socket;
		this.sr = sr;

		// ��ʼ��������û������
		for (int i = 0; i < SIZE; i++) {
			flags[i] = false;
		}
	}

	@Override
	public void run() {
		byte[] buf = new byte[BUF_SIZE];

		System.out.println("���մ�������..." + socket.getLocalPort());

		while (running) {
			DatagramPacket packet = new DatagramPacket(buf, buf.length);  //����һ�� receive package
			try {
				socket.receive(packet);  // ������ȡ����
			} catch (IOException e) {
				System.out.println("��ȡ���ݳ���");
				e.printStackTrace();
			}
			Segment segment = new Segment(packet.getData());
			if (segment.getType() == Segment.ACK) { // ͨ�淢�ʹ��ڷ���ACK
				sendWin.ack(segment.getAcknum());
			} else if (segment.getType() == Segment.DATA) { // ���յ������ݷ��뻺����
				add(segment);
			} else if (segment.getType() == Segment.CONNECT) { // ֪ͨ��ģ��󶨿ͻ��˵ĵ�ַ
				sr.accept(packet);
			}
		}
	}

	/**
	 * ���յ������ݷ��뻺�����������ڱ�Ҫ����������÷��ʹ��ڷ���ACK��Ϣ
	 * 
	 * @param segment �յ���������Ϣ
	 */
	private void add(Segment segment) {
		int num = segment.getSeqnum();
		if (num % 10 == 7) { // ģ�ⶪ��
			return;
		}

		if ((num + SEQ_SIZE - expected) % SEQ_SIZE < SIZE) { // �������δ���ϲ���յ����ݣ���δ�������淶Χ
			buffer[(recievebase + (num + SEQ_SIZE - expected) % SEQ_SIZE) % SIZE] = segment;
			flags[(recievebase + (num + SEQ_SIZE - expected) % SEQ_SIZE) % SIZE] = true;
			try {
				sendWin.sendAck(segment.getSeqnum()); // ����AKC
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("recieve segment " + segment.getSeqnum() + "..." + socket.getLocalPort());

		} else if ((expected + SEQ_SIZE - num) % SEQ_SIZE < SIZE) { // ����������Ѿ����ϲ���չ������ݣ�������ACK
			try {
				sendWin.sendAck(segment.getSeqnum());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * ���ش���ָ�����ȵ��������������������������������ָ�����ȣ��򷵻�ȫ��
	 * 
	 * @param length �������ݵĳ���
	 * @return ����ָ�����ȵ�����������
	 */
	public List<Segment> revieve(int length) {
		List<Segment> segments = new ArrayList<>();
		int num = 0;
		int size = 0;
		for (int i = 0; i < SIZE; i++) {
			int t = (i + recievebase) % SIZE;
			if (!flags[t] || size >= length) { // û�����ݻ������ݳ������󳤶��򷵻�
				break;
			}
			segments.add(buffer[t]);
			flags[t] = false;
			num++;
			size += buffer[t].getLength();
		}
		expected = (expected + num) % SEQ_SIZE; // ���½��մ���
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
