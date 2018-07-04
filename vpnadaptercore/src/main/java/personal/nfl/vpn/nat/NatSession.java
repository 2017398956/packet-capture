package personal.nfl.vpn.nat;


import personal.nfl.vpn.processparse.AppInfo;
import personal.nfl.vpn.utils.CommonMethods;

import java.io.Serializable;

/**
 *
 * @author nfl
 * 网络会话信息
 */
public class NatSession implements Serializable {
    public static final String TCP = "TCP";
    public static final String UDP = "UPD";
    public String type;
    public String ipAndPort;
    public int remoteIP;
    public short remotePort;
    public String remoteHost;
    public short localPort;
    public int bytesSent;
    /**
     * 记录当前网络会话中已经发送了多少个网络包
     */
    public int packetSent;
    public long receiveByteNum;
    public long receivePacketNum;
    public long lastRefreshTime;
    public boolean isHttpsSession;
    public String requestUrl;
    public String pathUrl;
    public String method;
    public AppInfo appInfo;
    public long connectionStartTime = System.currentTimeMillis();
    public long vpnStartTime;
    public boolean isHttp;


    @Override
    public String toString() {
        return String.format("%s/%s:%d packet: %d", remoteHost, CommonMethods.ipIntToString(remoteIP),
                remotePort & 0xFFFF, packetSent);
    }

    public String getUniqueName() {
        String uinID = ipAndPort + connectionStartTime;
        return String.valueOf(uinID.hashCode());
    }

    public void refreshIpAndPort() {
        int remoteIPStr1 = (remoteIP & 0XFF000000) >> 24 & 0XFF;
        int remoteIPStr2 = (remoteIP & 0X00FF0000) >> 16;
        int remoteIPStr3 = (remoteIP & 0X0000FF00) >> 8;
        int remoteIPStr4 = remoteIP & 0X000000FF;
        String remoteIPStr = "" + remoteIPStr1 + ":" + remoteIPStr2 + ":" + remoteIPStr3 + ":" + remoteIPStr4;
        ipAndPort = type + ":" + remoteIPStr + ":" + remotePort + " " + ((int) localPort & 0XFFFF);
    }

    public String getType() {
        return type;
    }

    public String getIpAndPort() {
        return ipAndPort;
    }

    public int getRemoteIP() {
        return remoteIP;
    }

    public short getRemotePort() {
        return remotePort;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public short getLocalPort() {
        return localPort;
    }

    public int getBytesSent() {
        return bytesSent;
    }

    public int getPacketSent() {
        return packetSent;
    }

    public long getReceiveByteNum() {
        return receiveByteNum;
    }

    public long getReceivePacketNum() {
        return receivePacketNum;
    }

    public long getRefreshTime() {
        return lastRefreshTime;
    }

    public boolean isHttpsSession() {
        return isHttpsSession;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public String getPathUrl() {
        return pathUrl;
    }

    public String getMethod() {
        return method;
    }

    public AppInfo getAppInfo() {
        return appInfo;
    }

    public long getConnectionStartTime() {
        return connectionStartTime;
    }

    public long getVpnStartTime() {
        return vpnStartTime;
    }

    public static class NatSesionComparator implements java.util.Comparator<NatSession> {

        @Override
        public int compare(NatSession o1, NatSession o2) {
            if (o1 == o2) {
                return 0;
            }
            return Long.compare(o2.lastRefreshTime, o1.lastRefreshTime);
        }
    }
}
