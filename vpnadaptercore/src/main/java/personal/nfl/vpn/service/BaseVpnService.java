package personal.nfl.vpn.service;

import android.net.VpnService;

import personal.nfl.vpn.ProxyConfig;
import personal.nfl.vpn.utils.CommonMethods;
import personal.nfl.vpn.utils.DebugLog;

/**
 * @author fuli.niu
 */
public abstract class BaseVpnService extends VpnService {

    public static final String ACTION_START_VPN = "personal.nfl.START_VPN";
    public static final String ACTION_CLOSE_VPN = "personal.nfl.roav.CLOSE_VPN";
    private static final String FACEBOOK_APP = "com.facebook.katana";
    private static final String YOUTUBE_APP = "com.google.android.youtube";
    private static final String GOOGLE_MAP_APP = "com.google.android.apps.maps";
    public static final String BROADCAST_VPN_STATE = "personal.nfl.localvpn.VPN_STATE";
    public static final String SELECT_PACKAGE_ID = "select_protect_package_id";

    /**
     * 设置 VPN 的 IP 地址（这里只支持 IPv4）
     * 这个地址可以去查查，360 流量卫士里面的地址为 192.168.*.*;
     * 好多也使用10.0.2.0；不确定，都可以试试。这里使用的是 {@linkplain #LOCAL_IP}
     */
    static final String VPN_ADDRESS = "10.0.0.2";

    /**
     * VPN IP 地址；
     */
    static int LOCAL_IP;

    Builder defaultBuilder;
    /**
     * 只有匹配上的 IP包 ，才会被路由到虚拟端口上去。如果是 0.0.0.0/0 的话，则会将所有的IP包都路由到虚拟端口上去；
     */
    static final String VPN_ROUTE = "0.0.0.0";
    /**
     * 下面是一些常见的 DNS 地址
     */
    static final String GOOGLE_DNS_FIRST = "8.8.8.8";
    static final String GOOGLE_DNS_SECOND = "8.8.4.4";
    static final String AMERICA = "208.67.222.222";
    static final String HK_DNS_SECOND = "205.252.144.228";
    static final String CHINA_DNS_FIRST = "114.114.114.114";

    /**
     * 虚拟网络端口的最大传输单元，如果发送的包长度超过这个数字，则会被分包；一般设为 1500
     */
    public static final int MUTE_SIZE = 2560;

    /**
     * VPN 的运行状态
     */
    public enum Status {
        STATUS_AVAILABLE, STATUS_PREPARING, STATUS_RUNNING, STATUS_STOPPING, STATUS_STOP
    }

    /**
     * VPN 默认状态是 stop
     */
    Status status = Status.STATUS_AVAILABLE;

    /**
     * VPNService 是否再运行
     */
    boolean IsRunning = false;

    public BaseVpnService() {
        defaultBuilder = new Builder()
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
        defaultBuilder.addAddress(ipAddress.Address, ipAddress.PrefixLength);
        DebugLog.i("addAddress: %s/%d\n", ipAddress.Address, ipAddress.PrefixLength);
    }

    @Override
    public void onRevoke() {
        super.onRevoke();
        // TODO VPN 授权被撤销，可以在这提示下用户
    }

    public boolean vpnRunningStatus() {
        return IsRunning;
    }

    public void setVpnRunningStatus(boolean isRunning) {
        IsRunning = isRunning;
    }

    public Status getStatus() {
        return status;
    }

    public void startVPN() {
    }

    public void stopVPN() {
    }

}
