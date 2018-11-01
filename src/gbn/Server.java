package gbn;

import java.net.SocketException;

/**
 * GBN�Ĳ��Է������ˣ�ģ��һ��������������ѭ���������Կͻ��˵����ݣ����ҽ����յ������ݷ���.
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
