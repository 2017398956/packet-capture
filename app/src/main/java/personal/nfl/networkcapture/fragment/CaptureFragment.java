package personal.nfl.networkcapture.fragment;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import personal.nfl.networkcapture.adapter.ConnectionAdapter;
import personal.nfl.networkcapture.activity.PacketDetailActivity;
import personal.nfl.networkcapture.R;
import personal.nfl.networkcapture.common.widget.BaseFragment;
import personal.nfl.vpn.ProxyConfig;
import personal.nfl.vpn.VPNConstants;
import personal.nfl.vpn.nat.NatSession;
import personal.nfl.vpn.processparse.AppInfo;
import personal.nfl.vpn.utils.ThreadProxy;
import personal.nfl.vpn.utils.TimeFormatUtil;
import personal.nfl.vpn.utils.VpnServiceHelper;

import static personal.nfl.vpn.VPNConstants.DEFAULT_PACKAGE_ID;


/**
 * 抓包列表
 * @author nfl
 */

public class CaptureFragment extends BaseFragment {
    private static final String TAG = "CaptureFragment";
    private ScheduledExecutorService timer;
    private Handler handler = new Handler();
    private ConnectionAdapter connectionAdapter;
    private ListView channelList;
    private List<NatSession> allNetConnection;
    private Context context;

    @Override
    protected int getLayout() {
        return R.layout.fragment_capture;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");
        context = getContext();
        channelList = view.findViewById(R.id.channel_list);
        channelList.setOnItemClickListener(onItemClickListener);
       /* LocalBroadcastManager.getInstance(getContext()).registerReceiver(vpnStateReceiver,
                new IntentFilter(LocalVPNService.BROADCAST_VPN_STATE));*/
        ProxyConfig.Instance.registerVpnStatusListener(listener);
        if (VpnServiceHelper.vpnRunningStatus()) {
            startTimer();
        }
        getDataAndRefreshView();

    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
        ProxyConfig.Instance.unregisterVpnStatusListener(listener);
        cancelTimer();
        //    connectionAdapter = null;
    }

    private void getDataAndRefreshView() {

        ThreadProxy.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                allNetConnection = VpnServiceHelper.getAllSession();
                if (allNetConnection == null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            refreshView(allNetConnection);
                        }
                    });
                    return;
                }
                Iterator<NatSession> iterator = allNetConnection.iterator();
                String packageName = context.getPackageName();

                SharedPreferences sp = getContext().getSharedPreferences(VPNConstants.VPN_SP_NAME, Context.MODE_PRIVATE);
                boolean isShowUDP = sp.getBoolean(VPNConstants.IS_UDP_SHOW, false);
                String selectPackage = sp.getString(DEFAULT_PACKAGE_ID, null);
                while (iterator.hasNext()) {
                    NatSession next = iterator.next();
                    if (next.bytesSent == 0 && next.receiveByteNum == 0) {
                        iterator.remove();
                        continue;
                    }
                    if (NatSession.UDP.equals(next.type) && !isShowUDP) {
                        iterator.remove();
                        continue;
                    }
                    AppInfo appInfo = next.appInfo;

                    if (appInfo != null) {
                        String appPackageName = appInfo.pkgs.getAt(0);
                        if (packageName.equals(appPackageName)) {
                            iterator.remove();
                            continue;
                        }
                        if ((selectPackage != null && !selectPackage.equals(appPackageName))) {
                            iterator.remove();
                        }


                    }
                }
                if (handler == null) {
                    return;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        refreshView(allNetConnection);
                    }
                });
            }
        });
    }

    private void startTimer() {
        timer = Executors.newSingleThreadScheduledExecutor();

        timer.scheduleAtFixedRate(new TimerTask() {


            @Override
            public void run() {
                getDataAndRefreshView();
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    private void refreshView(List<NatSession> allNetConnection) {
        if (connectionAdapter == null) {
            connectionAdapter = new ConnectionAdapter(context, allNetConnection);
            channelList.setAdapter(connectionAdapter);
        } else {
            connectionAdapter.setNetConnections(allNetConnection);
            if (channelList.getAdapter() == null) {
                channelList.setAdapter(connectionAdapter);
            }
            connectionAdapter.notifyDataSetChanged();
        }


    }

    private void cancelTimer() {
        if (timer == null) {
            return;
        }
        timer.shutdownNow();
        timer = null;
    }

    private ProxyConfig.VpnStatusListener listener = new ProxyConfig.VpnStatusListener() {

        @Override
        public void onVpnStart(Context context) {
            startTimer();
        }

        @Override
        public void onVpnEnd(Context context) {
            cancelTimer();
        }
    };

    private OnItemClickListener onItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (allNetConnection == null) {
                return;
            }
            if (position > allNetConnection.size() - 1) {
                return;
            }
            NatSession connection = allNetConnection.get(position);
            if (connection.isHttpsSession) {
                return;
            }
            if (!NatSession.TCP.equals(connection.type)) {
                return;
            }
            String dir = VPNConstants.DATA_DIR
                    + TimeFormatUtil.formatYYMMDDHHMMSS(connection.vpnStartTime)
                    + "/"
                    + connection.getUniqueName();
            PacketDetailActivity.startActivity(getActivity(), dir);
        }
    } ;
}

