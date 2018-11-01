package sr;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import segment.Segment;

/**
 * SRЭ��ķ��ʹ��ڣ��������ϲ㽻�������ݣ�ͨ���ش�ȷ�ϵĻ��Ʊ�֤���ݴ���Ŀɿ��ԡ�
 * 
 * @author zz
 *
 */
public class SendWin {

	private static final int SEQ_SIZE = 256; // sequence number �����Χ

	private int SIZE = 50; // ���ʹ��ڵĴ�С
	private Segment[] buffer = new Segment[SIZE]; // ���ʹ��ڵĻ�����
	private boolean[] flags = new boolean[SIZE]; // ���͵���Ϣ�Ƿ�ȷ�ϵı��
	private int base = 0; // ��ȷ�ϵĵ�һ����Ϣ�ڻ�������λ��
	private int current = 0; // ��ǰ���õĻ�����λ��
	private int seqnum = 0; // ��ǰ���õĵ�һ�����к�
	private int seqbase = 0; // ��ȷ�ϵĵ�һ����Ϣ�����к�

	private DatagramSocket socket; // �²�UDP�ӿ�
	private String remoteHost; // ͨ����һ��������
	private int remotePort; // ͨ����һ���Ķ˿�

	private Timer timer; // ��ʱ��

	/**
	 * ������
	 * 
	 * @param socket     �²�UDP�ӿ�
	 * @param remoteHost ͨ����һ��������
	 * @param remotePort ͨ����һ���Ķ˿�
	 * @param timer      ��ʱ��
	 */
	public SendWin(DatagramSocket socket, String remoteHost, int remotePort, Timer timer) {
		this.socket = socket;
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		this.timer = timer;
		
		// ��ʼʱû����Ϣ��ȷ��
		for (int i = 0; i < SIZE; i++) {
			flags[i] = false;
		}
	}
	
	/**
	 * ����ͨ����һ������������ʼ��ʱ����
	 * 
	 * @param remoteHost ͨ����һ��������
	 */
	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	/**
	 * ����ͨ����һ���Ķ˿ڡ���ʼ��ʱ����
	 * 
	 * @param remotePort ͨ����һ���Ķ˿�
	 */
	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	/**
	 * �ش�ָ������Ϣ
	 * 
	 * @param index �ش�����Ϣ�ڻ������е�λ��
	 * @throws IOException
	 */
	public void timeOut(int index) throws IOException {

		System.out.println("time out...resend..." + socket.getLocalPort());
		DatagramPacket packet = new DatagramPacket(buffer[index].getBytes(), buffer[index].getLength() + 3,
				InetAddress.getByName(remoteHost), remotePort);
		socket.send(packet);

		System.out.println("resend segment " + buffer[index].getSeqnum() + "..." + socket.getLocalPort());
	}
	
	/**
	 * ȷ��ACK
	 * 
	 * @param acknum ack number 
	 */
	public void ack(int acknum) {
		int num = (acknum + SEQ_SIZE - seqbase) % SEQ_SIZE;
		if (num < SIZE) {  //������Ҫȷ�ϵ���Ϣ����Ч��Χ��
			num = (num + base) % SIZE;
			if (((current < base) && !(num < base && num >= current))
					|| (current > base && num >= base && num < current)) {//������Ҫȷ�ϵ���Ϣ�ڵȴ�ȷ�ϵ���Ϣ��Χ��
				flags[num] = true;
				timer.close(num);  // �رռ�ʱ��
				int number = 0;
				for (int i = 0; i < SIZE; i++) { // ��鴰���Ƿ���Ի����������ԣ��򻬶������ñ�־
					int t = (i + base) % SIZE;
					if (!flags[t]) {
						break;
					}
					flags[t] = false;
				}
				this.base = (base + number) % SIZE; // ����base
				this.seqbase = (seqbase + number) % SEQ_SIZE;
			}
		}

		System.out.println("recieve ack " + acknum + "..." + socket.getLocalPort());
	}

	/**
	 * ����ACK��Ϣ
	 * 
	 * @param seqnum ȷ�ϵ����к�
	 * @throws IOException
	 */
	public void sendAck(int seqnum) throws IOException {

		System.out.println("send ack " + seqnum + "..." + socket.getLocalPort());
		Segment segment = Segment.ACK(seqnum);
		send(segment);
	}

	/**
	 * ����һ��SR��Ϣ��������ͳɹ������ط������ݵĳ��ȣ����򷵻�-1
	 * 
	 * @param segment ��Ҫ���͵�GBN��Ϣ
	 * @return �������ݵĳ��ȣ����߲��ɹ�ʱ��-1
	 * @throws IOException
	 */
	public int send(Segment segment) throws IOException {
		if (segment.getType() == Segment.DATA) {  //����������Ϣ����ҪΪ��������к�
			if ((current + 1) % SIZE == base) {  // ��������������-1
				return -1;
			}
			segment.setSeqnum(this.seqnum);
			buffer[current] = segment;
			timer.restart(current); // ��������Ϣ�ļ�ʱ��

			System.out.println("send segment " + segment.getSeqnum() + "..." + socket.getLocalPort());

			this.current = (current + 1) % SIZE;
			this.seqnum = (seqnum + 1) % SEQ_SIZE;
		}

		DatagramPacket packet = new DatagramPacket(segment.getBytes(), segment.getLength() + 3,
				InetAddress.getByName(remoteHost), remotePort);
		socket.send(packet);
		return segment.getLength();
	}

}
