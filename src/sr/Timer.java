package sr;

import java.io.IOException;

import segment.Model;

public class Timer extends Thread {

	private Model[] models; // 计时模块
	private UdpSr sr; // 发送窗口
	private int size;

	/**
	 * 构造器
	 * 
	 * @param model 计时模块
	 * @param win   发送窗口
	 */
	public Timer(Model[] models, UdpSr sr, int size) {
		this.models = models;
		this.sr = sr;
		this.size = size;
	}

	@Override
	public void run() {
		while (true) {
			for (int i = 0; i < size; i++) {
				int time = models[i].getTime();
				if (time != Model.STOP) { // 若在正常计时
					if (time > 0) {
						this.models[i].reduceTime();
					} else {
						try {
							this.sr.timeOut(i);// 超时重传
							models[i].reset();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				System.out.println("计时器出现错误！");
				e.printStackTrace();
			}

		}

	}

	public void restart(int index) {
		models[index].reset();
	}

	/**
	 * 关闭计时器
	 */
	public void close(int index) {
		this.models[index].stop();
	}

}
