package segment;

/**
 * ����������ڼ�ʱģ���ڣ����Ա�֤��ʱģ���ڼ�ʱʱ���̰߳�ȫ
 * 
 * @author zz
 *
 */
public class Model {

	public static final int STOP = -1; // ֹͣ��ʱ��־

	private int ALL_TIME=50; // ��ʼʱʣ��ʱ�䣬Ĭ��Ϊ��ʱ����50����
	private volatile int time; // ʣ��ʱ��

	/**
	 * ������
	 */
	public Model() {
		this.time=STOP;
	}

	/**
	 * ���������������ʣ���ʱ����Ϊָ��ֵ
	 * 
	 * @param allTime ָ��ʱ���Ĭ��ֵ
	 */
	public Model(int allTime) {
		this.ALL_TIME = allTime;
		this.time=STOP;
	}

	/**
	 * ����ʣ��ʱ��
	 */
	public synchronized void reset() {
		this.time = this.ALL_TIME;
	}

	/**
	 * ��ȡʣ���ʱ��
	 * 
	 * @return ʣ���ʱ��
	 */
	public synchronized int getTime() {
		return this.time;
	}

	/**
	 * ��ʣ���ʱ���һ
	 */
	public synchronized void reduceTime() {
		this.time--;
	}

	/**
	 * ����Ϊֹͣ״̬
	 */
	public void stop() {
		this.time = STOP;
	}
}
