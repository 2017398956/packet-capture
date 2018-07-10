package personal.nfl.vpn;


import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import personal.nfl.vpn.service.FirewallVpnService;

/**
 * 代理配置
 */
public class ProxyConfig {

    public static final ProxyConfig Instance = new ProxyConfig();
    String mSessionName;
    int mMtu;
    private List<VpnStatusListener> mVpnStatusListeners = new ArrayList<>();


    private ProxyConfig() {
    }

    public String getSessionName() {
        if (mSessionName == null) {
            mSessionName = "Easy Firewall";
        }
        return mSessionName;
    }

    public int getMTU() {
        if (mMtu > 1400 && mMtu <= 20000) {
            return mMtu;
        } else {
            return 20000;
        }
    }

    public void registerVpnStatusListener(VpnStatusListener vpnStatusListener) {
        mVpnStatusListeners.add(vpnStatusListener);
    }

    public void unregisterVpnStatusListener(VpnStatusListener vpnStatusListener) {
        mVpnStatusListeners.remove(vpnStatusListener);
    }

    public void onVpnAvailable(Context context) {
        onVpnStatusChange(context, FirewallVpnService.Status.STATUS_AVAILABLE);
    }

    public void onVpnPreparing(Context context) {
        onVpnStatusChange(context, FirewallVpnService.Status.STATUS_PREPARING);
    }

    public void onVpnRunning(Context context) {
        onVpnStatusChange(context, FirewallVpnService.Status.STATUS_RUNNING);
    }

    public void onVpnStopping(Context context) {
        onVpnStatusChange(context, FirewallVpnService.Status.STATUS_STOPPING);
    }

    public void onVpnStop(Context context) {
        onVpnStatusChange(context, FirewallVpnService.Status.STATUS_STOP);
    }

    private void onVpnStatusChange(Context context, FirewallVpnService.Status status) {
        if (mVpnStatusListeners.size() > 0) {
            for (VpnStatusListener listener : mVpnStatusListeners) {
                if (null != listener) {
                    if (status == FirewallVpnService.Status.STATUS_AVAILABLE) {
                        listener.onVpnPreParing(context);
                    } else if (status == FirewallVpnService.Status.STATUS_PREPARING) {
                        listener.onVpnPreParing(context);
                    } else if (status == FirewallVpnService.Status.STATUS_RUNNING) {
                        listener.onVpnRunning(context);
                    } else if (status == FirewallVpnService.Status.STATUS_STOPPING) {
                        listener.onVpnStopping(context);
                    } else if (status == FirewallVpnService.Status.STATUS_STOP) {
                        listener.onVpnStop(context);
                    }
                }
            }
        }
    }

    public IPAddress getDefaultLocalIP() {
        return new IPAddress("10.8.0.2", 32);
    }

    public static class IPAddress {

        public final String Address;
        public final int PrefixLength;

        public IPAddress(String address, int prefixLength) {
            Address = address;
            PrefixLength = prefixLength;
        }

        public IPAddress(String ipAddressString) {
            String[] arrStrings = ipAddressString.split("/");
            String address = arrStrings[0];
            int prefixLength = 32;
            if (arrStrings.length > 1) {
                prefixLength = Integer.parseInt(arrStrings[1]);
            }
            this.Address = address;
            this.PrefixLength = prefixLength;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof IPAddress)) {
                return false;
            } else {
                return this.toString().equals(o.toString());
            }
        }

        @Override
        public String toString() {
            return String.format("%s/%d", Address, PrefixLength);
        }
    }

    public interface VpnStatusListener {

        void onVpnAvailable(Context context);

        void onVpnPreParing(Context context);

        void onVpnRunning(Context context);

        void onVpnStopping(Context context);

        void onVpnStop(Context context);
    }
}
