package segment;

/**
 * 这个方法用于计时模块内，用以保证计时模块在计时时的线程安全
 * 
 * @author zz
 *
 */
public class Model {

	public static final int STOP = -1; // 停止计时标志

	private int ALL_TIME=50; // 初始时剩余时间，默认为计时器的50跳步
	private volatile int time; // 剩余时间

	/**
	 * 构造器
	 */
	public Model() {
		this.time=STOP;
	}

	/**
	 * 构造器，并将最大剩余的时间设为指定值
	 * 
	 * @param allTime 指定时间的默认值
	 */
	public Model(int allTime) {
		this.ALL_TIME = allTime;
		this.time=STOP;
	}

	/**
	 * 重置剩余时间
	 */
	public synchronized void reset() {
		this.time = this.ALL_TIME;
	}

	/**
	 * 获取剩余的时间
	 * 
	 * @return 剩余的时间
	 */
	public synchronized int getTime() {
		return this.time;
	}

	/**
	 * 将剩余的时间减一
	 */
	public synchronized void reduceTime() {
		this.time--;
	}

	/**
	 * 设置为停止状态
	 */
	public void stop() {
		this.time = STOP;
	}
}
