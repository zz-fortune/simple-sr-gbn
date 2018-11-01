package sr;

import java.io.IOException;

import segment.Model;

public class Timer extends Thread {

	private Model[] models; // ��ʱģ��
	private UdpSr sr; // ���ʹ���
	private int size;

	/**
	 * ������
	 * 
	 * @param model ��ʱģ��
	 * @param win   ���ʹ���
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
				if (time != Model.STOP) { // ����������ʱ
					if (time > 0) {
						this.models[i].reduceTime();
					} else {
						try {
							this.sr.timeOut(i);// ��ʱ�ش�
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
				System.out.println("��ʱ�����ִ���");
				e.printStackTrace();
			}

		}

	}

	public void restart(int index) {
		models[index].reset();
	}

	/**
	 * �رռ�ʱ��
	 */
	public void close(int index) {
		this.models[index].stop();
	}

}
