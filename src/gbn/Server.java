package gbn;

import java.net.SocketException;

/**
 * GBN的测试服务器端，模拟一个回声服务器。循环接收来自客户端的数据，并且将接收到的数据返回.
 * 
 * @author zz
 *
 */
public class Server {

	public static void main(String[] args) throws SocketException, InterruptedException {
		UdpGbn gbn = new UdpGbn(1024);
		byte[] bs = new byte[1024];
		int length;
		while (true) {
			length = gbn.recieve(bs);
			Thread.sleep(1);
			if (length!=-1) {
				gbn.send(bs, length);
			}
		}

	}

}
