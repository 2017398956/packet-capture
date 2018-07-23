package personal.nfl.vpn.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

import personal.nfl.vpn.Packet;
import personal.nfl.vpn.ProxyConfig;
import personal.nfl.vpn.R;
import personal.nfl.vpn.UDPServer;
import personal.nfl.vpn.VPNConstants;
import personal.nfl.vpn.VPNLog;
import personal.nfl.vpn.http.HttpRequestHeaderParser;
import personal.nfl.vpn.nat.NatSession;
import personal.nfl.vpn.nat.NatSessionManager;
import personal.nfl.vpn.processparse.AppInfoCreator;
import personal.nfl.vpn.proxy.TcpProxyServer;
import personal.nfl.vpn.tcpip.IPHeader;
import personal.nfl.vpn.tcpip.TCPHeader;
import personal.nfl.vpn.tcpip.UDPHeader;
import personal.nfl.vpn.utils.AppDebug;
import personal.nfl.vpn.utils.CommonMethods;
import personal.nfl.vpn.utils.DebugLog;
import personal.nfl.vpn.utils.ThreadProxy;
import personal.nfl.vpn.utils.TimeFormatUtil;
import personal.nfl.vpn.utils.VpnServiceHelper;

import static personal.nfl.vpn.VPNConstants.DEFAULT_PACKAGE_ID;


/**
 * 自定义 VPN 服务
 *
 * @author nfl
 */
public class FirewallVpnService extends BaseVpnService implements Runnable {

    private static final String TAG = "FirewallVpnService";

    /**
     * 用于标记新创建的 Vpn 服务
     */
    private static int ID;

    /**
     * 用来保存一个 ip 数据包
     */
    private byte[] mPacket;
    /**
     * IP 报文格式
     */
    private IPHeader mIPHeader;
    /**
     * TCP 报文格式
     */
    private TCPHeader mTCPHeader;
    /**
     * UDP 报文格式
     */
    private final UDPHeader mUDPHeader;
    /**
     * TCP 代理服务
     */
    private TcpProxyServer mTcpProxyServer;
    /**
     * UDP 代理服务
     */
    private UDPServer udpServer;
    private final ByteBuffer mDNSBuffer;

    private Thread mVPNThread;
    // private DnsProxy mDnsProxy;

    private FileOutputStream mVPNOutputStream;

    private ConcurrentLinkedQueue<Packet> udpQueue;
    private FileInputStream in;

    /**
     * 选择抓取特定 app 的包，默认是 null
     */
    private String selectPackage;

    private int mReceivedBytes;
    private int mSentBytes;
    /**
     * 抓到网络数据包的时间，每抓到一个包就更新一下时间
     */
    public static long vpnStartTime;
    public static String lastVpnStartTimeFormat = null;
    private SharedPreferences sp;
    /*
     * 用于获取 vpn 数据时的临时变量，为了避免反复 new ，作为类变量声明
     */
    private ParcelFileDescriptor parcelFileDescriptorTemp;

    public FirewallVpnService() {
        super();
        ID++;
        mPacket = new byte[MUTE_SIZE];
        mIPHeader = new IPHeader(mPacket, 0);
        // Offset = ip 报文头部长度
        mTCPHeader = new TCPHeader(mPacket, 20);
        mUDPHeader = new UDPHeader(mPacket, 20);
        // Offset = ip 报文头部长度 + udp 报文头部长度 = 28
        mDNSBuffer = ((ByteBuffer) ByteBuffer.wrap(mPacket).position(28)).slice();
        DebugLog.i("New VPNService(%d)\n", ID);

    }

    /**
     * 启动 Vpn 工作线程
     */
    @Override
    public void onCreate() {
        super.onCreate();
        DebugLog.i("VPNService(%s) created.\n", ID);
        sp = getSharedPreferences(VPNConstants.VPN_SP_NAME, Context.MODE_PRIVATE);
        VpnServiceHelper.onVpnServiceCreated(this);
        mVPNThread = new Thread(this, "VPNServiceThread");
        startVPN();
    }

    @Override
    public void run() {
        status = Status.STATUS_PREPARING;
        ProxyConfig.Instance.onVpnPreparing(this);
        try {
            DebugLog.i("VPNService(%s) work thread is Running...\n", ID);
            waitUntilPrepared();
            udpQueue = new ConcurrentLinkedQueue<>();
            // 启动 TCP 代理服务
            mTcpProxyServer = new TcpProxyServer(0);
            mTcpProxyServer.start();
            // 启动 UDP 代理服务
            udpServer = new UDPServer(this, udpQueue);
            udpServer.start();

            NatSessionManager.clearAllSession();
            AppInfoCreator.getInstance().refreshSessionInfo();
            DebugLog.i("DnsProxy started.\n");
            status = Status.STATUS_RUNNING;
            ProxyConfig.Instance.onVpnRunning(this);
            while (status == Status.STATUS_RUNNING) {
                startStream(establishVPN());
            }
        } catch (InterruptedException e) {
            if (AppDebug.IS_DEBUG) {
                e.printStackTrace();
            }
            DebugLog.e("VpnService run catch an exception %s.\n", e);
        } catch (Exception e) {
            if (AppDebug.IS_DEBUG) {
                e.printStackTrace();
            }
            DebugLog.e("VpnService run catch an exception %s.\n", e);
        } finally {
            DebugLog.i("VpnService terminated");
            status = Status.STATUS_STOPPING;
            ProxyConfig.Instance.onVpnStopping(this);
            dispose();
        }
    }

    /**
     * 停止Vpn工作线程
     */
    @Override
    public void onDestroy() {
        DebugLog.i("VPNService(%s) destroyed.\n", ID);
        super.onDestroy();
    }

    /**
     * 创建 VPN
     *
     * @return
     * @throws Exception
     */
    private ParcelFileDescriptor establishVPN() {
        selectPackage = sp.getString(DEFAULT_PACKAGE_ID, null);
        vpnStartTime = System.currentTimeMillis();
        lastVpnStartTimeFormat = TimeFormatUtil.formatYYMMDDHHMMSS(vpnStartTime);
        if (selectPackage != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                defaultBuilder.addAllowedApplication(selectPackage);
                defaultBuilder.addAllowedApplication(getPackageName());
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        // 不调用 establish() 手机不会显示 vpn 图标
        try {
            parcelFileDescriptorTemp = defaultBuilder.establish();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            parcelFileDescriptorTemp = null;
        }
        return parcelFileDescriptorTemp;
    }

    private void startStream(ParcelFileDescriptor parcelFileDescriptor) throws Exception {
        if (null == parcelFileDescriptor || true) {
            return;
        }
        int size = 0;
        mVPNOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
        in = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
        while (size != -1 && IsRunning) {
            boolean hasWrite = false;
            // 每次调用 FileInputStream.read 函数会读取一个IP数据包
            size = in.read(mPacket);
            if (size > 0) {
                if (mTcpProxyServer.Stopped) {
                    // 接收到数据包后，如果 tcp 代理服务器已经停止工作了，那么抛出异常，停止 VPN 的下一步动作
                    in.close();
                    throw new Exception("LocalServer stopped.");
                }
                //
                hasWrite = onIPPacketReceived(mIPHeader, size);
            }
            if (!hasWrite) {
                Packet packet = udpQueue.poll();
                if (packet != null) {
                    ByteBuffer bufferFromNetwork = packet.backingBuffer;
                    bufferFromNetwork.flip();
                    // 调用 FileOutputStream.write 函数会写入一个 IP数据包 到TCP/IP协议栈。
                    mVPNOutputStream.write(bufferFromNetwork.array());
                }
            }
            Thread.sleep(10);
        }
        in.close();
    }

    /**
     * 死循环知道 VPN 准备好，理论上已经在 activity 中启动了 vpn ，这里不应该再 prepare 暂时去掉，看看效果
     */
    private void waitUntilPrepared() {
//        while (prepare(this) != null) {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                if (AppDebug.IS_DEBUG) {
//                    e.printStackTrace();
//                }
//                DebugLog.e("waitUntilPrepared catch an exception %s\n", e);
//            }
//        }
    }

    /**
     * 开启 VPN
     */
    public void startVPN() {
        if (status != Status.STATUS_AVAILABLE) {
            mVPNThread.interrupt();
            status = Status.STATUS_AVAILABLE;
        }
        mVPNThread.start();
    }

    /**
     * 关闭 VPN 服务
     */
    public void stopVPN() {
        dispose();
    }

    private synchronized void dispose() {
        // 停止 TCP 代理服务
        if (mTcpProxyServer != null) {
            mTcpProxyServer.stop();
            mTcpProxyServer = null;
        }
        // 停止 UDP 代理服务
        if (udpServer != null) {
            udpServer.closeAllUDPConn();
        }
        ThreadProxy.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                AppInfoCreator.getInstance().refreshSessionInfo();
            }
        });
        if (mVPNThread != null) {
            mVPNThread.interrupt();
        }
        stopSelf();
        status = Status.STATUS_STOP;
        ProxyConfig.Instance.onVpnStop(this);
        VpnServiceHelper.onVpnServiceDestroy();
    }

    /**
     * 根据报文中的源端口获取相应的 app 信息，并将其转发给 UDPServer 或 TcpProxyServer
     *
     * @param ipHeader ip 报文
     * @param size     ip报文的字节长度
     * @return
     * @throws IOException
     */
    boolean onIPPacketReceived(IPHeader ipHeader, int size) throws IOException {
        boolean hasWrite = false;
        switch (ipHeader.getProtocol()) {
            case IPHeader.TCP:
                hasWrite = onTcpPacketReceived(ipHeader, size);
                break;
            case IPHeader.UDP:
                onUdpPacketReceived(ipHeader, size);
                break;
            default:
                break;
        }
        return hasWrite;
    }

    private void onUdpPacketReceived(IPHeader ipHeader, int size) throws UnknownHostException {
        TCPHeader tcpHeader = mTCPHeader;
        short portKey = tcpHeader.getSourcePort();

        NatSession session = NatSessionManager.getSession(portKey);
        if (session == null || session.remoteIP != ipHeader.getDestinationIP()
                || session.remotePort != tcpHeader.getDestinationPort()) {
            // 当没有记录过这条网络会话信息，或某个源端口的目的对象发生改变后，更新会话信息，并保存
            session = NatSessionManager.createSession(portKey, ipHeader.getDestinationIP(), tcpHeader
                    .getDestinationPort(), NatSession.UDP);
            session.vpnStartTime = vpnStartTime;
            AppInfoCreator.getInstance().refreshSessionInfo();
        }

        session.lastRefreshTime = System.currentTimeMillis();
        session.packetSent++; //注意顺序

        byte[] bytes = Arrays.copyOf(mPacket, mPacket.length);
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, 0, size);
        byteBuffer.limit(size);
        Packet packet = new Packet(byteBuffer);
        udpServer.processUDPPacket(packet, portKey);
    }

    private boolean onTcpPacketReceived(IPHeader ipHeader, int size) throws IOException {
        boolean hasWrite = false;
        TCPHeader tcpHeader = mTCPHeader;
        // 矫正TCPHeader里的偏移量，使它指向真正的TCP数据地址
        tcpHeader.mOffset = ipHeader.getHeaderLength();
        // 如果 tcp 报文的源端口与 tcp 代理服务器的端口一致
        if (tcpHeader.getSourcePort() == mTcpProxyServer.port) {
            VPNLog.d(TAG, "process tcp packet from net ");
            NatSession session = NatSessionManager.getSession(tcpHeader.getDestinationPort());
            Log.i("NFL", "getDestinationPort" + tcpHeader.getDestinationPort());
            if (session != null) {
                // 拼接 ip 报文，模拟以 VPN 发出报文，并且 VPN 接收
                ipHeader.setSourceIP(ipHeader.getDestinationIP());
                tcpHeader.setSourcePort(session.remotePort);
                ipHeader.setDestinationIP(LOCAL_IP);
                CommonMethods.ComputeTCPChecksum(ipHeader, tcpHeader);
                // VPN 发出报文
                mVPNOutputStream.write(ipHeader.mData, ipHeader.mOffset, size);
                mReceivedBytes += size;
            } else {
                DebugLog.i("NoSession: %s %s\n", ipHeader.toString(), tcpHeader.toString());
            }
        } else {
            VPNLog.d(TAG, "process tcp packet to net ");
            // 添加端口映射
            short portKey = tcpHeader.getSourcePort();
            NatSession session = NatSessionManager.getSession(portKey);
            if (session == null || session.remoteIP != ipHeader.getDestinationIP() || session.remotePort
                    != tcpHeader.getDestinationPort()) {
                session = NatSessionManager.createSession(portKey, ipHeader.getDestinationIP(), tcpHeader
                        .getDestinationPort(), NatSession.TCP);
                session.vpnStartTime = vpnStartTime;
                AppInfoCreator.getInstance().refreshSessionInfo();
            }
            session.lastRefreshTime = System.currentTimeMillis();
            session.packetSent++; // 注意顺序
            int tcpDataSize = ipHeader.getDataLength() - tcpHeader.getHeaderLength();
            // 丢弃tcp握手的第二个ACK报文。因为客户端发数据的时候也会带上ACK，这样可以在服务器Accept之前分析出HOST信息。
            if (session.packetSent == 2 && tcpDataSize == 0) {
                return false;
            }
            // 分析数据，找到host
            if (session.bytesSent == 0 && tcpDataSize > 10) {
                int dataOffset = tcpHeader.mOffset + tcpHeader.getHeaderLength();
                HttpRequestHeaderParser.parseHttpRequestHeader(session, tcpHeader.mData, dataOffset, tcpDataSize);
                DebugLog.i("Host: %s\n", session.remoteHost);
                DebugLog.i("Request: %s %s\n", session.method, session.requestUrl);
            } else if (session.bytesSent > 0 && !session.isHttpsSession && session.isHttp && session.remoteHost == null && session.requestUrl == null) {
                int dataOffset = tcpHeader.mOffset + tcpHeader.getHeaderLength();
                session.remoteHost = HttpRequestHeaderParser.getRemoteHost(tcpHeader.mData, dataOffset, tcpDataSize);
                session.requestUrl = "http://" + session.remoteHost + "/" + session.pathUrl;
            }
            // 转发给本地TCP服务器
            ipHeader.setSourceIP(ipHeader.getDestinationIP());
            ipHeader.setDestinationIP(LOCAL_IP);
            tcpHeader.setDestinationPort(mTcpProxyServer.port);
            CommonMethods.ComputeTCPChecksum(ipHeader, tcpHeader);
            mVPNOutputStream.write(ipHeader.mData, ipHeader.mOffset, size);
            // 注意顺序
            session.bytesSent += tcpDataSize;
            mSentBytes += size;
        }
        hasWrite = true;
        return hasWrite;
    }

}
