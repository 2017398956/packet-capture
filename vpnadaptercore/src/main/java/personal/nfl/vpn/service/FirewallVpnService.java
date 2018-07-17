package personal.nfl.vpn.service;

import android.content.Context;
import android.content.SharedPreferences;
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


/**
 * 自定义 VPN 服务
 * @author nfl
 */
public class FirewallVpnService extends VpnService implements Runnable {

    private static final String TAG = "FirewallVpnService";

    public static final String ACTION_START_VPN = "personal.nfl.START_VPN";
    public static final String ACTION_CLOSE_VPN = "personal.nfl.roav.CLOSE_VPN";
    private static final String FACEBOOK_APP = "com.facebook.katana";
    private static final String YOUTUBE_APP = "com.google.android.youtube";
    private static final String GOOGLE_MAP_APP = "com.google.android.apps.maps";
    public static final String BROADCAST_VPN_STATE = "personal.nfl.localvpn.VPN_STATE";
    public static final String SELECT_PACKAGE_ID = "select_protect_package_id";

    private VpnService.Builder builder;
    /**
     * 设置 VPN 的 IP 地址（这里只支持 IPv4）
     * 这个地址可以去查查，360 流量卫士里面的地址为 192.168.*.*;
     * 好多也使用10.0.2.0；不确定，都可以试试。这里使用的是 {@linkplain #LOCAL_IP}
     */
    private static final String VPN_ADDRESS = "10.0.0.2";
    /**
     * VPN IP 地址；
     */
    private static int LOCAL_IP;
    /**
     * 只有匹配上的 IP包 ，才会被路由到虚拟端口上去。如果是 0.0.0.0/0 的话，则会将所有的IP包都路由到虚拟端口上去；
     */
    private static final String VPN_ROUTE = "0.0.0.0";
    /**
     * 下面是一些常见的 DNS 地址
     */
    private static final String GOOGLE_DNS_FIRST = "8.8.8.8";
    private static final String GOOGLE_DNS_SECOND = "8.8.4.4";
    private static final String AMERICA = "208.67.222.222";
    private static final String HK_DNS_SECOND = "205.252.144.228";
    private static final String CHINA_DNS_FIRST = "114.114.114.114";
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
    /**
     * VPNService 是否再运行
     */
    private boolean IsRunning = false;

    /**
     * VPN 的运行状态
     */
    public enum Status {
        STATUS_AVAILABLE , STATUS_PREPARING, STATUS_RUNNING, STATUS_STOPPING , STATUS_STOP
    }

    /**
     * VPN 默认状态是 stop
     */
    private Status status = Status.STATUS_AVAILABLE;
    private Thread mVPNThread;
    // private DnsProxy mDnsProxy;

    private FileOutputStream mVPNOutputStream;

    private ConcurrentLinkedQueue<Packet> udpQueue;
    private FileInputStream in;

    /**
     * 选择抓取特定 app 的包，默认是 null
     */
    private String selectPackage;
    /**
     * 虚拟网络端口的最大传输单元，如果发送的包长度超过这个数字，则会被分包；一般设为 1500
     */
    public static final int MUTE_SIZE = 2560;
    private int mReceivedBytes;
    private int mSentBytes;
    /**
     * 抓到网络数据包的时间，每抓到一个包就更新一下时间
     */
    public static long vpnStartTime;
    public static String lastVpnStartTimeFormat = null;
    private SharedPreferences sp;

    public FirewallVpnService() {
        ID++;
        mPacket = new byte[MUTE_SIZE];
        mIPHeader = new IPHeader(mPacket, 0);
        // Offset = ip 报文头部长度
        mTCPHeader = new TCPHeader(mPacket, 20);
        mUDPHeader = new UDPHeader(mPacket, 20);
        // Offset = ip 报文头部长度 + udp 报文头部长度 = 28
        mDNSBuffer = ((ByteBuffer) ByteBuffer.wrap(mPacket).position(28)).slice();
        DebugLog.i("New VPNService(%d)\n", ID);
        builder = new Builder()
                // 设置 VPN 最大传输单位
                .setMtu(MUTE_SIZE)
                // 设置 VPN 路由
                .addRoute(VPN_ROUTE, 0)
                // 配置 DNS
                .addDnsServer(GOOGLE_DNS_FIRST)
                .addDnsServer(GOOGLE_DNS_SECOND)
                .addDnsServer(CHINA_DNS_FIRST)
                .addDnsServer(AMERICA)
        // 就是添加 DNS 域名的自动补齐。DNS服务器必须通过全域名进行搜索，
        // 但每次查找都输入全域名太麻烦了，可以通过配置域名的自动补齐规则予以简化；
        // .addSearchDomain()
        /*
         * Set the name of this session. It will be displayed in system-managed dialogs
         * and notifications. This is recommended not required.
         */
        // .setSession(getString(R.string.app_name))
        ;
        DebugLog.i("setMtu: %d\n", ProxyConfig.Instance.getMTU());
        ProxyConfig.IPAddress ipAddress = ProxyConfig.Instance.getDefaultLocalIP();
        LOCAL_IP = CommonMethods.ipStringToInt(ipAddress.Address);
        // ipAddress.PrefixLength 默认值是 32 ，现只支持 IPv4 不支持 IPv6
        builder.addAddress(ipAddress.Address, ipAddress.PrefixLength);
        DebugLog.i("addAddress: %s/%d\n", ipAddress.Address, ipAddress.PrefixLength);
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
            status = Status.STATUS_RUNNING ;
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
            status = Status.STATUS_STOPPING ;
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
    private ParcelFileDescriptor establishVPN() throws Exception {
        selectPackage = sp.getString(VPNConstants.DEFAULT_PACKAGE_ID, null);
        vpnStartTime = System.currentTimeMillis();
        lastVpnStartTimeFormat = TimeFormatUtil.formatYYMMDDHHMMSS(vpnStartTime);
        if (selectPackage != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.addAllowedApplication(selectPackage);
            builder.addAllowedApplication(getPackageName());
        }
        return builder.establish();
    }

    private void startStream(ParcelFileDescriptor parcelFileDescriptor) throws Exception {
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
     * 死循环知道 VPN 准备好
     */
    private void waitUntilPrepared() {
        while (prepare(this) != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                if (AppDebug.IS_DEBUG) {
                    e.printStackTrace();
                }
                DebugLog.e("waitUntilPrepared catch an exception %s\n", e);
            }
        }
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
        status = Status.STATUS_STOP ;
        ProxyConfig.Instance.onVpnStop(this);
        VpnServiceHelper.onVpnServiceDestroy();
    }

    public boolean vpnRunningStatus() {
        return IsRunning;
    }

    public void setVpnRunningStatus(boolean isRunning) {
        IsRunning = isRunning;
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

    public Status getStatus() {
        return status;
    }
}
