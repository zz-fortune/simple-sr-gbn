package gbn;

import java.io.IOException;

/**
 * GBN�Ĳ��Կͻ��ˡ�ģ��������������Ǹ���Ϣ�����������е�һ����
 * 
 * @author zz
 *
 */
public class Client {
	public static void main(String[] args) throws InterruptedException, IOException {
		UdpGbn gbn =new UdpGbn("localhost", 1024);
		Thread.sleep(100);
		byte[] bs = "hello".getBytes();
		for(int i=0;i<10;i++) {
			Thread.sleep(100);
			gbn.send(bs, bs.length);
		}
		
		while(gbn.recieve(bs)==-1) {
			Thread.sleep(3);
		}
		System.out.println(new String(bs));
	}

}
