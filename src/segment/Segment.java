package segment;

/**
 * GBN协议的报文消息封装，包括头部和数据两部分。
 * 
  * 头部总共由三个字节组成。
 * <P>
  * 开始的4个bit是用来标识该GBN消息的类型，包括 {@code 确认（ACK）消息、数据（DATA）消息、建立连接（CONNECT）消息、
  * 拆除连接（CLOSE）消息以及其它（ABANDON）消息}
  * <p>随后的12个bit用来标识数据的长度
  * <p>最后的一个字节是序列号
 * 
 * 数据部分则是用户要传输的数据，有最长的数据长度限制
 * 
 * @author zz
 *
 */
public class Segment {

	// 消息的类型的常量
	public static final int ACK = 0;
	public static final int DATA = 1;
	public static final int CONNECT = 2;
	public static final int CLOSE = 3;
	public static final int ABANDON = 4;

	private int type; //消息类型
	private int acknum; //ack number，用以ACK消息
	private int seqnum; // sequence number，用以DATA消息
	private int length; // 数据的长度
	private byte[] data; //数据的内容

	/**
	 * 构造器。通过字节流数据解析出各项参数
	 * 
	 * @param buf 字节流数据
	 */
	public Segment(byte[] buf) {
		init(buf);
	}
	
	/**
	 * 构造器
	 */
	public Segment() {
	}

	/**
	 * 构造一个ACK消息，{@code ack number}由参数指定
	 * 
	 * @param num ack number
	 * @return 一个ACK消息
	 */
	public static Segment ACK(int num) {
		Segment segment = new Segment();
		segment.acknum = num;
		segment.type = Segment.ACK;
		segment.length = 0;
		return segment;
	}

	/**
	 * 构造一个{@code CONNECT}消息
	 * 
	 * @return 一个{@code CONNECT}消息
	 */
	public static Segment CONNECT() {
		Segment segment = new Segment();
		segment.type = Segment.CONNECT;
		segment.length = 0;
		return segment;
	}

	/**
	 * 通过字节流数据解析出各项参数
	 * 
	 * @param buf 字节流数据
	 */
	private void init(byte[] buf) {

		// 解析消息的种类
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

		// 解析数据的长度
		this.length = ((buf[0] & 0xF) << 8) | (buf[1] & 0xFF);

		if (this.type == DATA) {
			this.seqnum = (int) buf[2];
			this.acknum = -1;
		} else {
			this.acknum = (int) buf[2];
			this.seqnum = -1;
		}

		// 解析数据
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
