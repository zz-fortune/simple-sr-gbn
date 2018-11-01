package gbn;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

import segment.Segment;

/**
 * GBNЭ��Ľ��մ��ڡ�����һֱ�����²��Ƿ��������ύ���������ݴ��ڻ������ڣ�
 * ����Ҫʱ�������ϲ㡣
 * 
 * ���մ��ڽ��յ���Ϣʱ����ͨ�淢�ʹ��ڽ�����Ӧ�Ĵ���
 * 
 * @author zz
 *
 */
public class RecieveWin extends Thread {

	private static final int BUF_SIZE = 1472;  //������Ϣ����󳤶�

//	private int SIZE;
	private int SEQ_SIZE = 256;   // sequence number �����Χ
	private List<Segment> buffer = new ArrayList<>();  //���ջ�����
	private boolean available = true;  //�������Ƿ����
	private SendWin sendWin;         //���ʹ���
	private DatagramSocket socket;   // udp�Ľӿ�
	private UdpGbn gbn;             // GBNЭ��ʵ�ֵĽӿ�

	private boolean running = true;   // ���ʹ����Ƿ�����

	private int expected = 0;     // �����յ��� sequence number

	/**
	 * ��������
	 * 
	 * @param sendWin ���ʹ���
	 * @param socket GBN Э���������²�ӿ�
	 * @param gbn GBN �ṩ���ϲ�ӿڣ���GBN����ģ��
	 */
	public RecieveWin(SendWin sendWin, DatagramSocket socket, UdpGbn gbn) {
		this.sendWin = sendWin;
		this.socket = socket;
		this.gbn = gbn;
	}

	@Override
	public void run() {
		byte[] buf = new byte[BUF_SIZE];

		System.out.println("���մ�������..." + socket.getLocalPort());

		while (running) {
			DatagramPacket packet = new DatagramPacket(buf, buf.length); //����һ�� receive package
			try {
				socket.receive(packet);  // ������ȡ����
			} catch (IOException e) {
				System.out.println("��ȡ���ݳ���");
				e.printStackTrace();
			}
			Segment segment = new Segment(packet.getData());
			
			//���ദ������
			if (segment.getType() == Segment.ACK) {// ͨ�淢�ʹ��ڷ���ACK
				sendWin.ack(segment.getAcknum());  
			} else if (segment.getType() == Segment.DATA) { // ���յ������ݷ��뻺����
				add(segment);
			} else if (segment.getType() == Segment.CONNECT) { // ֪ͨ��ģ��󶨿ͻ��˵ĵ�ַ
				gbn.accept(packet);
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
		
		//���붪��
		if (num % 10 == 7) {
			return;
		}
		
		// ����յ�����Ϣ�����к������������кţ����һ��������ã������ͷ�ȷ�ϸ���Ϣ
		if ((num == expected) && available) {
			buffer.add(segment);
			available = false;
			expected = (expected + 1) % SEQ_SIZE;
		} else {         // �ظ�ȷ��֮ǰ����Ϣ
			num = (expected + SEQ_SIZE - 1) % SEQ_SIZE;
		}
		try {
			sendWin.sendAck(num);  // ����ACK
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ��������������ݣ����ء����򷵻�{@code null}
	 * 
	 * @return ����
	 */
	public Segment revieve() {
		if (available) { // ���������û����Ϣ������null
			return null;
		}
		
		// ȡ������������Ϣ���ύ
		Segment segment = buffer.get(0);
		buffer.remove(0);
		available = true;

		System.out.println("recieve segmengt " + segment.getSeqnum() + "..." + socket.getLocalPort());

		return segment;
	}

	/**
	 * �رշ��ʹ���
	 */
	public void close() {
		this.running=false;
	}
}
