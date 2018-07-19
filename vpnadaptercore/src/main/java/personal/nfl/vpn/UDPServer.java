package personal.nfl.vpn;

import android.net.VpnService;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import personal.nfl.vpn.tunnel.UDPTunnel;
import personal.nfl.vpn.utils.AppDebug;
import personal.nfl.vpn.utils.DebugLog;
import personal.nfl.vpn.utils.MyLRUCache;

/**
 * @author nfl
 */

public class UDPServer implements Runnable {

    private String TAG = UDPServer.class.getSimpleName();
    private VpnService vpnService;
    private ConcurrentLinkedQueue<Packet> outputQueue;
    private Selector selector;
    private boolean isClose = false;
    private static final int MAX_UDP_CACHE_SIZE = 50;
    /**
     * Short 源端口
     */
    private final MyLRUCache<Short, UDPTunnel> udpConnections =
            new MyLRUCache<>(MAX_UDP_CACHE_SIZE, new MyLRUCache.CleanupCallback<UDPTunnel>() {
                @Override
                public void cleanUp(UDPTunnel udpTunnel) {
                    udpTunnel.close();
                }
            });


    public void start() {
        Thread thread = new Thread(this, "UDPServer");
        thread.start();
    }

    public UDPServer(VpnService vpnService, ConcurrentLinkedQueue<Packet> outputQueue) {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.vpnService = vpnService;
        this.outputQueue = outputQueue;
    }

    /**
     * 发送 UDP 数据
     *
     * @param packet     IP 数据包
     * @param sourcePort 源端口
     */
    public void processUDPPacket(Packet packet, short sourcePort) {
        UDPTunnel udpConn = getUDPConn(sourcePort);
        if (udpConn == null) {
            udpConn = new UDPTunnel(vpnService, selector, this, packet, outputQueue, sourcePort);
            putUDPConn(sourcePort, udpConn);
            udpConn.initConnection();
        } else {
            udpConn.processPacket(packet);
        }
    }


    public void closeAllUDPConn() {
        synchronized (udpConnections) {
            Iterator<Map.Entry<Short, UDPTunnel>> it = udpConnections.entrySet().iterator();
            while (it.hasNext()) {
                it.next().getValue().close();
                it.remove();
            }
        }
        DebugLog.i("UDPServer stopped.\n");
    }


    public void closeUDPConn(UDPTunnel connection) {
        synchronized (udpConnections) {
            connection.close();
            udpConnections.remove(connection.getPortKey());
        }
    }

    public UDPTunnel getUDPConn(short portKey) {
        synchronized (udpConnections) {
            return udpConnections.get(portKey);
        }
    }

    /**
     * 添加 UDP 数据通道
     * @param sourcePort
     * @param connection
     */
    private void putUDPConn(short sourcePort, UDPTunnel connection) {
        synchronized (udpConnections) {
            udpConnections.put(sourcePort, connection);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                int select = selector.select();
                if (select == 0) {
                    Thread.sleep(5);
                }
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isValid()) {
                        try {
                            Object attachment = key.attachment();
                            if (attachment instanceof KeyHandler) {
                                ((KeyHandler) attachment).onKeyReady(key);
                            }

                        } catch (Exception ex) {
                            if (AppDebug.IS_DEBUG) {
                                ex.printStackTrace(System.err);
                            }
                            DebugLog.e("TcpProxyServer iterate SelectionKey catch an exception: %s", ex);
                        }
                    }
                    keyIterator.remove();
                }
            }
        } catch (Exception e) {
            if (AppDebug.IS_DEBUG) {
                e.printStackTrace(System.err);
            }
            DebugLog.e("TcpProxyServer catch an exception: %s", e);
        } finally {
            this.stop();
            DebugLog.i("TcpServer thread exited.");
        }
    }

    private void stop() {
        try {
            selector.close();
            selector = null;
        } catch (Exception e) {
        }
    }

}
