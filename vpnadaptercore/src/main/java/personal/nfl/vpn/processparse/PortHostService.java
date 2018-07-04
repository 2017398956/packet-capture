package personal.nfl.vpn.processparse;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.List;

import personal.nfl.vpn.VPNLog;
import personal.nfl.vpn.nat.NatSession;
import personal.nfl.vpn.nat.NatSessionManager;
import personal.nfl.vpn.utils.VpnServiceHelper;

/**
 * @author minhui.zhu
 * Created by minhui.zhu on 2018/5/5.
 * Copyright © 2017年 Oceanwing. All rights reserved.
 */

public class PortHostService extends Service {

    private static final String TAG = "PortHostService";
    private static PortHostService instance;
    /**
     * 是否正在更新网络会话信息
     */
    private boolean isRefresh = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NetFileManager.getInstance().init();
        instance = this;
    }

    /**
     * 不能保证返回结果不是 null
     *
     * @return
     */
    public static PortHostService getInstance() {
        return instance;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    /**
     * 刷新网络会话信息，主要是为每个网络会话信息添加 app 信息
     * @return
     */
    public List<NatSession> refreshSessionInfo() {
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


    public static void startParse(Context context) {
        Intent intent = new Intent(context, PortHostService.class);
        context.startService(intent);
    }

    public static void stopParse(Context context) {
        Intent intent = new Intent(context, PortHostService.class);
        context.stopService(intent);
    }
}
