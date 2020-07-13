package personal.nfl.vpn.processparse;

import java.util.List;

import personal.nfl.vpn.VPNLog;
import personal.nfl.vpn.nat.NatSession;
import personal.nfl.vpn.nat.NatSessionManager;
import personal.nfl.vpn.utils.VpnServiceHelper;

/**
 * @author nfl
 * 根据端口刷新 app 信息
 */

public class AppInfoCreator {

    private static final String TAG = "AppInfoCreator";
    private final static AppInfoCreator instance = new AppInfoCreator();
    /**
     * 是否正在更新网络会话信息
     */
    private boolean isRefresh = false;

    private AppInfoCreator(){
        VPNLog.e(TAG , "PortHostService 新建成功");
    }

    public static AppInfoCreator getInstance() {
        return instance;
    }

    /**
     * 刷新网络会话信息，主要是为每个网络会话信息添加 app 信息
     * @return
     */
    public List<NatSession> refreshSessionInfo() {
        NetFileManager.getInstance().refresh();
        List<NatSession> netConnections = NatSessionManager.getAllSession();
        if (isRefresh || netConnections == null) {
            return netConnections;
        }
        boolean needRefresh = false;
        for (NatSession connection : netConnections) {
            if (connection.appInfo == null) {
                // 如果存在一个没有 app 信息的网络会话，则需要更新网络会话信息
                needRefresh = true;
                break;
            }
        }
        if (!needRefresh) {
            return netConnections;
        }
        isRefresh = true;
        try {
            NetFileManager.getInstance().refresh();
            for (NatSession connection : netConnections) {
                if (connection.appInfo == null) {
                    int searchPort = connection.localPort & 0XFFFF;
                    Integer uid = NetFileManager.getInstance().getUid(searchPort);

                    if (uid != null) {
                        connection.appInfo = AppInfo.createFromUid(VpnServiceHelper.getContext(), uid);
                    }
                }
            }
        } catch (Exception e) {
            VPNLog.d(TAG, "failed to refreshSessionInfo " + e.getMessage());
        }
        isRefresh = false;
        return netConnections;
    }
}
