package sr;

import java.io.IOException;

/**
 * SR的测试客户端。模拟向服务器发送是个消息，并接收其中的一个。
 * 
 * @author zz
 *
 */
public class Client {
	public static void main(String[] args) throws InterruptedException, IOException {
		UdpSr sr = new UdpSr("localhost", 1024);
		Thread.sleep(100);
		byte[] bs = "hello".getBytes();
		for (int i = 0; i < 10; i++) {
			Thread.sleep(100);
			sr.send(bs, bs.length);
		}

		while (sr.recieve(bs) == -1) {
			Thread.sleep(3);
		}
		System.out.println(new String(bs));
	}
}
