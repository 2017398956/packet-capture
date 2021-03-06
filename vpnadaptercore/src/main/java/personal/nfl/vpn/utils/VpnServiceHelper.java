package personal.nfl.vpn.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import personal.nfl.vpn.VPNConstants;
import personal.nfl.vpn.nat.NatSession;
import personal.nfl.vpn.processparse.AppInfoCreator;
import personal.nfl.vpn.service.BaseVpnService;
import personal.nfl.vpn.service.FirewallVpnService;

/**
 * VPN 服务帮助类
 *
 * @author nfl
 */
public class VpnServiceHelper {

    private static Context context;
    public static final int START_VPN_SERVICE_REQUEST_CODE = 2015;
    private static BaseVpnService sVpnService;
    private static SharedPreferences sp;

    public static void onVpnServiceCreated(BaseVpnService vpnService) {
        sVpnService = vpnService;
        if (context == null) {
            context = vpnService.getApplicationContext();
        }
    }

    public static void onVpnServiceDestroy() {
        sVpnService = null;
    }

    public static boolean isUDPDataNeedSave() {

        sp = context.getSharedPreferences(VPNConstants.VPN_SP_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(VPNConstants.IS_UDP_NEED_SAVE, false);
    }

    public static boolean protect(Socket socket) {
        if (sVpnService != null) {
            return sVpnService.protect(socket);
        }
        return false;
    }

    public static boolean protect(DatagramSocket socket) {
        if (sVpnService != null) {
            return sVpnService.protect(socket);
        }
        return false;
    }

    public static boolean vpnIsRunning() {
        if (sVpnService != null) {
            return sVpnService.isRunning();
        }
        return false;
    }

    public static void changeVpnRunningStatus(Context context, boolean isStart) {
        if (context == null) {
            return;
        }
        if (isStart) {
            Intent intent = FirewallVpnService.prepare(context);
            if (intent == null) {
                Log.e("NFL" , "这个 App 的 VPN 已经连接，或有其它 VPN 正在使用") ;
                startVpnService(context);
            } else {
                Log.e("NFL" , "准备申请 VPN 服务") ;
                if (context instanceof Activity) {
                    ((Activity) context).startActivityForResult(intent, START_VPN_SERVICE_REQUEST_CODE);
                }
            }
        } else if (sVpnService != null) {
            stopVpnService();
        }
    }

    public static List<NatSession> getAllSession() {
        if (FirewallVpnService.lastVpnStartTimeFormat == null) {
            return null;
        }
        try {
            File file = new File(VPNConstants.CONFIG_DIR + FirewallVpnService.lastVpnStartTimeFormat);
            ACache aCache = ACache.get(file);
            String[] list = file.list();
            ArrayList<NatSession> baseNetSessions = new ArrayList<>();
            if (list != null) {
                for (String fileName : list) {
                    NatSession netConnection = (NatSession) aCache.getAsObject(fileName);
                    baseNetSessions.add(netConnection);
                }
            }

            AppInfoCreator portHostService = AppInfoCreator.getInstance();
            if (portHostService != null) {
                List<NatSession> aliveConnInfo = portHostService.refreshSessionInfo();
                if (aliveConnInfo != null) {
                    baseNetSessions.addAll(aliveConnInfo);
                }
            }
            Collections.sort(baseNetSessions, new NatSession.NatSesionComparator());
            return baseNetSessions;
        } catch (Exception e) {
            return null;
        }
    }

    public static void startVpnService(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("VpnServiceHelper : context cannot be null");
        }
        if (null == sVpnService) {
            Log.i("NFL" , "代理服务还未创建，");
            context.startService(new Intent(context, FirewallVpnService.class));
        } else if (sVpnService.getStatus() == BaseVpnService.Status.STATUS_AVAILABLE) {
            Log.i("NFL" , "代理服务已创建，重新启动服务");
            sVpnService.startVPN();
        }
    }

    public static void stopVpnService() {
        if (null != sVpnService) {
            sVpnService.stopVPN();
            sVpnService = null;
        }
    }

    public static Context getContext() {
        return context;
    }

    public static FirewallVpnService.Status getVpnServiceStatus() {
        if (null != sVpnService) {
            return sVpnService.getStatus();
        } else {
            return FirewallVpnService.Status.STATUS_AVAILABLE;
        }
    }
}
