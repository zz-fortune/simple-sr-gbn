package gbn;

import java.io.IOException;

import segment.Model;

/**
 * ��ʱģ��Ŀ�ܡ�
 * 
 * ��������ʱ����״̬�£���һ���������߳̽��м�ʱ�����Ƿ��ֳ�ʱ���͵���
 * UdpGbnģ��ĳ�ʱ�ش�����
 * 
 * @author zz
 *
 */
public class Timer extends Thread {

	private Model model; // �����࣬���Ա�֤�̰߳�ȫ
	private UdpGbn gbn; // �����ڳ�ʱʱ�䷢��ʱ����

	/**
	 * ������
	 * 
	 * @param model ��ʱģ��ĸ���
	 * @param gbn ������ģ��
	 */
	public Timer(Model model, UdpGbn gbn) {
		this.model = model;
		this.gbn = gbn;
	}

	@Override
	public void run() {
		while (true) {
			int time = model.getTime();
			if (time != Model.STOP) { // ���Ǽ�ʱ������ֹͣ״̬���򲻼�ʱ
				if (time > 0) {
					this.model.reduceTime();  // ʣ��ʱ���һ
				} else {
					try {
						this.gbn.timeOut();// ��ʱ�ش�
						model.reset();
					} catch (IOException e) {
						e.printStackTrace();
					} 
				}
			}
			try {
				Thread.sleep(20);  // ��ʱ��ÿ������Ϊ20ms
			} catch (InterruptedException e) {
				System.out.println("��ʱ�����ִ���");
				e.printStackTrace();
			}
		}

	}
	
	/**
	 * ������ʱ��
	 */
	public void restart() {
		model.reset();
	}
	
	/**
	 * �رռ�ʱ��
	 */
	public void close() {
		this.model.stop();
	}

}
