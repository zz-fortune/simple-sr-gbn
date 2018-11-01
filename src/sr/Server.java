package sr;

import java.net.SocketException;

/**
 * SR�Ĳ��Է������ˣ�ģ��һ��������������ѭ���������Կͻ��˵����ݣ����ҽ����յ������ݷ���.
 * 
 * @author zz
 *
 */
public class Server {
	public static void main(String[] args) throws SocketException, InterruptedException {
		UdpSr sr= new UdpSr(1024);
		byte[] bs = new byte[1024];
		int length;
		while (true) {
			length = sr.recieve(bs);
			Thread.sleep(1);
			if (length!=-1) {
				sr.send(bs, length);
			}
		}

	}
}
