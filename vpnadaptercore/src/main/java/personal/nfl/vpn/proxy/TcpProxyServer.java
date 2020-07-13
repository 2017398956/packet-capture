package personal.nfl.vpn.proxy;


import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import personal.nfl.vpn.KeyHandler;
import personal.nfl.vpn.VPNLog;
import personal.nfl.vpn.constants.DebugConstants;
import personal.nfl.vpn.nat.NatSession;
import personal.nfl.vpn.nat.NatSessionManager;
import personal.nfl.vpn.tunnel.TcpTunnel;
import personal.nfl.vpn.tunnel.TunnelFactory;
import personal.nfl.vpn.utils.AppDebug;
import personal.nfl.vpn.utils.CommonMethods;
import personal.nfl.vpn.utils.DebugLog;

/**
 * TCP 代理服务
 *
 * @author nfl
 */
public class TcpProxyServer implements Runnable {

    private static final String TAG = "TcpProxyServer";
    public boolean Stopped;
    public short port;

    Selector mSelector;
    ServerSocketChannel mServerSocketChannel;

    Thread mServerThread;

    /**
     * 当建立新的 TCP和 UDP socket连接时，需要给它们指定端口号。为了避免这种写死端口号的做法
     * 或者说为了从本地系统中找到可用端口。网络编程员们可以以端口号 0 来作为连接参数。
     * 这样的话操作系统就会从动态端口号范围内搜索接下来可以使用的端口号。
     * windows系统和其他操作系统在处理端口号0时有一些细微的差别。
     * @param port 传 0 即可
     */
    public TcpProxyServer(int port) throws IOException {
        mSelector = Selector.open();
        mServerSocketChannel = ServerSocketChannel.open();
        mServerSocketChannel.socket().bind(new InetSocketAddress(port));
        // 注册 Channel 到 Selector , Channel 必须是非阻塞的。
        // 所以 FileChannel 不适用 Selector，因为 FileChannel 不能切换为非阻塞模式，更准确的来说是
        // 因为 FileChannel 没有继承SelectableChannel。Socket channel 可以正常使用。
        mServerSocketChannel.configureBlocking(false);
        // register() 方法的第二个参数是一个 “interest集合”，意思是在通过 Selector 监听 Channel 时对什么事件感兴趣。
        // 可以监听四种不同类型的事件：
        // Connect
        // Accept
        // Read
        // Write
        // 通道触发了一个事件意思是该事件已经就绪。比如
        // 某个 Channel 成功连接到另一个服务器称为 “连接就绪 ”。
        // 一个 ServerSocketChannel 准备好接收新进入的连接称为 “接收就绪”。
        // 一个有数据可读的通道可以说是 “读就绪”。
        // 等待写数据的通道可以说是 “写就绪”。
        // 这四种事件用SelectionKey的四个常量来表示：
        // SelectionKey.OP_CONNECT
        // SelectionKey.OP_ACCEPT
        // SelectionKey.OP_READ
        // SelectionKey.OP_WRITE
        // 如果你对不止一种事件感兴趣，使用或运算符即可，如下：
        // int interestSet = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
        mServerSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT);
        // 当传递的 port 为 0 时，系统会随机分发一个可以使用的端口
        this.port = (short) mServerSocketChannel.socket().getLocalPort();
        DebugLog.i("AsyncTcpServer listen on %s:%d success.\n", mServerSocketChannel.socket().getInetAddress()
                .toString(), this.port & 0xFFFF);
    }

    /**
     * 启动 TcpProxyServer 线程
     */
    public void start() {
        mServerThread = new Thread(this, "TcpProxyServerThread");
        mServerThread.start();
    }

    public void stop() {
        this.Stopped = true;
        if (mSelector != null) {
            try {
                mSelector.close();
                mSelector = null;
            } catch (Exception ex) {
                DebugLog.e("TcpProxyServer mSelector.close() catch an exception: %s", ex);
            }
        }
        if (mServerSocketChannel != null) {
            try {
                mServerSocketChannel.close();
                mServerSocketChannel = null;
            } catch (Exception ex) {
                if (AppDebug.IS_DEBUG) {
                    ex.printStackTrace(System.err);
                }
                DebugLog.e("TcpProxyServer mServerSocketChannel.close() catch an exception: %s", ex);
            }
        }
        DebugLog.i("TcpProxyServer stopped.\n");
    }

    @Override
    public void run() {
        try {
            while (true) {
                if(null == mSelector){
                    continue;
                }
                // 阻塞到至少有一个通道在注册的事件上就绪了。
                // select()方法返回的int值表示有多少通道已经就绪,是自上次调用select()方法后有多少通道变成就绪状态。
                // 之前在select（）调用时进入就绪的通道不会在本次调用中被记入，而在前一次select（）调用进入就绪
                // 但现在已经不在处于就绪的通道也不会被记入。
                int select = mSelector.select();
                if (select == 0) {
                    Thread.sleep(5);
                    continue;
                }
                Set<SelectionKey> selectionKeys = mSelector.selectedKeys();
                if (selectionKeys == null) {
                    continue;
                }

                Iterator<SelectionKey> keyIterator = mSelector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isValid()) {
                        try {
                            if (key.isAcceptable()) {
                                VPNLog.d(TAG, "isAcceptable");
                                // 从本地 tcp 服务器连接到目标服务器
                                onAccepted();
                            } else {
                                // 本地tcp服务器连接到目标服务器后，接到返回信息的处理，key 绑定的是 RemoteTcpTunnel
                                Object attachment = key.attachment();
                                if (attachment instanceof KeyHandler) {
                                    ((KeyHandler) attachment).onKeyReady(key);
                                }
                            }
                        } catch (Exception ex) {
                            if (AppDebug.IS_DEBUG) {
                                ex.printStackTrace(System.err);
                            }
                            DebugLog.e("tcp iterate SelectionKey catch an exception: %s", ex);
                        }
                    }
                    keyIterator.remove();
                }
            }
        } catch (Exception e) {
            if (AppDebug.IS_DEBUG) {
                e.printStackTrace(System.err);
            }
            DebugLog.e("TcpServer catch an exception: %s", e);
        } finally {
            this.stop();
            DebugLog.i("TcpServer thread exited.");
        }
    }

    /**
     * 获得一个从 tcp 服务器连接到原目的地址接口的 InetSocketAddress
     * @param localChannel
     * @return
     */
    InetSocketAddress getDestAddress(SocketChannel localChannel) {
        // 使用的 app 所占用的端口号
        short portKey = (short) localChannel.socket().getPort();
        // 获取该 app 的网络会话信息
        NatSession session = NatSessionManager.getSession(portKey);
        if (session != null) {
            return new InetSocketAddress(localChannel.socket().getInetAddress(), session.remotePort & 0xFFFF);
        }
        return null;
    }

    void onAccepted() {
        TcpTunnel localTunnel = null;
        try {
            SocketChannel localChannel = mServerSocketChannel.accept();
            localTunnel = TunnelFactory.wrap(localChannel, mSelector);
            // 使用的 app 所占用的端口号
            short portKey = (short) localChannel.socket().getPort();
            InetSocketAddress destAddress = getDestAddress(localChannel);

            if (destAddress != null) {
                TcpTunnel remoteTunnel = TunnelFactory.createTunnelByConfig(destAddress, mSelector, portKey);
                remoteTunnel.setIsHttpsRequest(localTunnel.isHttpsRequest());
                // 关联兄弟
                remoteTunnel.setBrotherTunnel(localTunnel);
                localTunnel.setBrotherTunnel(remoteTunnel);
                // 开始连接（本地 tcp 服务器连接到 目标服务器）
                remoteTunnel.connect(destAddress);
            }
        } catch (Exception ex) {
            if (AppDebug.IS_DEBUG) {
                ex.printStackTrace(System.err);
            }
            DebugLog.e("TcpProxyServer onAccepted catch an exception: %s", ex);
            if (localTunnel != null) {
                localTunnel.dispose();
            }
        }
    }

}
