package personal.nfl.vpn.tunnel;


import personal.nfl.vpn.nat.NatSession;
import personal.nfl.vpn.nat.NatSessionManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Created by nfl
 */
public class TunnelFactory {

	/**
	 * 一般用于创建用户使用的 APP 到 本地TCP服务器的 TcpTunnel
	 * @param channel
	 * @param selector
	 * @return
	 */
	public static TcpTunnel wrap(SocketChannel channel, Selector selector) {
		TcpTunnel tunnel = new RawTcpTunnel(channel, selector);
		// 由于在发送到 tcp 服务器前没有修改原报文的源端口号（即：使用的 app 所占用的端口），所以这里
		// 通过 channel.socket().getPort() 拿到这个 app 使用的端口号，然后获得会话信息
		NatSession session = NatSessionManager.getSession((short) channel.socket().getPort());
		if (session != null) {
			tunnel.setIsHttpsRequest(session.isHttpsSession);
		}
		return tunnel;
	}

	/**
	 * 一般用与闯将 tcp服务器 到 目标服务器 的 TcpTunnel
	 * @param destAddress
	 * @param selector
	 * @param portKey
	 * @return
	 * @throws IOException
	 */
	public static TcpTunnel createTunnelByConfig(InetSocketAddress destAddress, Selector selector, short portKey) throws IOException {
		return new RemoteTcpTunnel(destAddress, selector,portKey);
	}
}
