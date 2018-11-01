package segment;

/**
 * GBNЭ��ı�����Ϣ��װ������ͷ�������������֡�
 * 
  * ͷ���ܹ��������ֽ���ɡ�
 * <P>
  * ��ʼ��4��bit��������ʶ��GBN��Ϣ�����ͣ����� {@code ȷ�ϣ�ACK����Ϣ�����ݣ�DATA����Ϣ���������ӣ�CONNECT����Ϣ��
  * ������ӣ�CLOSE����Ϣ�Լ�������ABANDON����Ϣ}
  * <p>����12��bit������ʶ���ݵĳ���
  * <p>����һ���ֽ������к�
 * 
 * ���ݲ��������û�Ҫ��������ݣ���������ݳ�������
 * 
 * @author zz
 *
 */
public class Segment {

	// ��Ϣ�����͵ĳ���
	public static final int ACK = 0;
	public static final int DATA = 1;
	public static final int CONNECT = 2;
	public static final int CLOSE = 3;
	public static final int ABANDON = 4;

	private int type; //��Ϣ����
	private int acknum; //ack number������ACK��Ϣ
	private int seqnum; // sequence number������DATA��Ϣ
	private int length; // ���ݵĳ���
	private byte[] data; //���ݵ�����

	/**
	 * ��������ͨ���ֽ������ݽ������������
	 * 
	 * @param buf �ֽ�������
	 */
	public Segment(byte[] buf) {
		init(buf);
	}
	
	/**
	 * ������
	 */
	public Segment() {
	}

	/**
	 * ����һ��ACK��Ϣ��{@code ack number}�ɲ���ָ��
	 * 
	 * @param num ack number
	 * @return һ��ACK��Ϣ
	 */
	public static Segment ACK(int num) {
		Segment segment = new Segment();
		segment.acknum = num;
		segment.type = Segment.ACK;
		segment.length = 0;
		return segment;
	}

	/**
	 * ����һ��{@code CONNECT}��Ϣ
	 * 
	 * @return һ��{@code CONNECT}��Ϣ
	 */
	public static Segment CONNECT() {
		Segment segment = new Segment();
		segment.type = Segment.CONNECT;
		segment.length = 0;
		return segment;
	}

	/**
	 * ͨ���ֽ������ݽ������������
	 * 
	 * @param buf �ֽ�������
	 */
	private void init(byte[] buf) {

		// ������Ϣ������
		int typebuf = (buf[0]) >> 4;
		if ((typebuf & 0x8) != 0) {
			this.type = ACK;
		} else if ((typebuf & 0x4) != 0) {
			this.type = DATA;
		} else if ((typebuf & 0x2) != 0) {
			this.type = CONNECT;
		} else if ((typebuf & 0x1) != 0) {
			this.type = CLOSE;
		} else {
			this.type = ABANDON;
		}

		// �������ݵĳ���
		this.length = ((buf[0] & 0xF) << 8) | (buf[1] & 0xFF);

		if (this.type == DATA) {
			this.seqnum = (int) buf[2];
			this.acknum = -1;
		} else {
			this.acknum = (int) buf[2];
			this.seqnum = -1;
		}

		// ��������
		this.data = new byte[this.length];
		for (int i = 0; i < this.length; i++) {
			this.data[i] = buf[i + 3];
		}

	}

	/**
	 * Return the type of this segment
	 * 
	 * @return the type of this segment
	 */
	public int getType() {
		return type;
	}

	/**
	 * Return the ack number of this segment
	 * 
	 * @return the ack number of this segment
	 */
	public int getAcknum() {
		return acknum;
	}

	/**
	 * Return the sequence number of this segment
	 * 
	 * @return the sequence number of this segment
	 */
	public int getSeqnum() {
		return seqnum;
	}

	/**
	 * Return the length of this segment's data
	 * 
	 * @return the length of this segment's data
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Set the length of this segment's data
	 * 
	 * @param length the length of this segment's data
	 */
	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * Set the sequence number of this segment
	 * 
	 * @param seqnum the sequence number of this segment
	 */
	public void setSeqnum(int seqnum) {
		this.seqnum = seqnum;
	}

	/**
	 * Set the type of this segment
	 * 
	 * @param type the type of this segment
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Set the data of this segment
	 *  
	 * @param data the buffer of data
	 * @param offset data's offset in buffer
	 */
	public void setData(byte[] data, int offset) {
		this.data = new byte[this.length];
		for (int i = 0; i < this.length; i++) {
			this.data[i] = data[i + offset];
		}
	}

	/**
	 * Return this segment in byte array
	 * 
	 * @return this segment in byte array
	 */
	public byte[] getBytes() {
		
		// convert the type
		byte[] bs = new byte[this.length + 3];
		if (this.type == ACK) {
			bs[0] = (byte) (0x80 | (this.length >> 8));
		} else if (this.type == DATA) {
			bs[0] = (byte) (0x40 | (this.length >> 8));
		} else if (this.type == CONNECT) {
			bs[0] = (byte) (0x20 | (this.length >> 8));
		} else {
			bs[0] = (byte) (0x10 | (this.length >> 8));
		}
		
		// convert the length
		bs[1] = (byte) (this.length & 0xFF);
		
		//convert the sequence number (or ack number)
		if (this.type == ACK) {
			bs[2] = (byte) (this.acknum & 0xFF);
		} else {
			bs[2] = (byte) (this.seqnum & 0xFF);
		}

		// copy the data
		for (int i = 0; i < this.length; i++) {
			bs[i + 3] = this.data[i];
		}
		return bs;
	}

}
