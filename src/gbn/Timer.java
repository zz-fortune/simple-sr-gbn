package gbn;

import java.io.IOException;

import segment.Model;

/**
 * 计时模块的框架。
 * 
 * 在启动计时器的状态下，以一个独立的线程进行计时，若是发现超时，就调用
 * UdpGbn模块的超时重传方法
 * 
 * @author zz
 *
 */
public class Timer extends Thread {

	private Model model; // 辅助类，用以保证线程安全
	private UdpGbn gbn; // 用以在超时时间发生时调用

	/**
	 * 构造器
	 * 
	 * @param model 计时模块的辅助
	 * @param gbn 主功能模块
	 */
	public Timer(Model model, UdpGbn gbn) {
		this.model = model;
		this.gbn = gbn;
	}

	@Override
	public void run() {
		while (true) {
			int time = model.getTime();
			if (time != Model.STOP) { // 若是计时器处于停止状态，则不计时
				if (time > 0) {
					this.model.reduceTime();  // 剩余时间减一
				} else {
					try {
						this.gbn.timeOut();// 超时重传
						model.reset();
					} catch (IOException e) {
						e.printStackTrace();
					} 
				}
			}
			try {
				Thread.sleep(20);  // 计时器每个跳步为20ms
			} catch (InterruptedException e) {
				System.out.println("计时器出现错误！");
				e.printStackTrace();
			}
		}

	}
	
	/**
	 * 重启计时器
	 */
	public void restart() {
		model.reset();
	}
	
	/**
	 * 关闭计时器
	 */
	public void close() {
		this.model.stop();
	}

}
