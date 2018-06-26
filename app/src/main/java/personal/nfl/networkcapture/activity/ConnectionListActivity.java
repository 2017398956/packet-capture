package personal.nfl.networkcapture.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import personal.nfl.networkcapture.R;
import personal.nfl.networkcapture.adapter.ConnectionAdapter;
import personal.nfl.networkcapture.common.widget.BaseActivity;
import personal.nfl.vpn.VPNConstants;
import personal.nfl.vpn.nat.NatSession;
import personal.nfl.vpn.utils.ACache;
import personal.nfl.vpn.utils.ThreadProxy;
import personal.nfl.vpn.utils.TimeFormatUtil;

/**
 * 连接列表界面
 *
 * @author nfl
 */

public class ConnectionListActivity extends BaseActivity {

    public static final String FILE_DIRNAME = "file_dirname";
    private String fileDir;
    private ListView lv_apps;
    private ConnectionAdapter adapter;
    private List<NatSession> data = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_list);
        setActionBarTitle(R.string.capture_data);
        lv_apps = findViewById(R.id.lv_apps);
        fileDir = getIntent().getStringExtra(FILE_DIRNAME);
        adapter = new ConnectionAdapter(this, data);
        lv_apps.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startPacketDetailActivity(data.get(position));
            }
        });
        lv_apps.setAdapter(adapter);
        getDataAndRefreshView();
    }

    private void getDataAndRefreshView() {
        ThreadProxy.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                data.clear();
                File file = new File(fileDir);
                ACache aCache = ACache.get(file);
                String[] list = file.list();
                if (list != null && list.length > 0) {
                    SharedPreferences sp = getSharedPreferences(VPNConstants.VPN_SP_NAME, Context.MODE_PRIVATE);
                    boolean isShowUDP = sp.getBoolean(VPNConstants.IS_UDP_SHOW, false);
                    for (String fileName : list) {
                        NatSession netConnection = (NatSession) aCache.getAsObject(fileName);
                        if (NatSession.UDP.equals(netConnection.type) && !isShowUDP) {
                            continue;
                        }
                        data.add(netConnection);
                    }
                    Collections.sort(data, new NatSession.NatSesionComparator());
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                        changeNoDataView(data);
                    }
                });
            }
        });

    }

    public static void openActivity(Activity activity, String dir) {
        Intent intent = new Intent(activity, ConnectionListActivity.class);
        intent.putExtra(FILE_DIRNAME, dir);
        activity.startActivity(intent);
    }

    private void startPacketDetailActivity(NatSession connection) {
        String dir = VPNConstants.DATA_DIR
                + TimeFormatUtil.formatYYMMDDHHMMSS(connection.getVpnStartTime())
                + "/"
                + connection.getUniqueName();
        PacketDetailActivity.startActivity(this, dir);
    }
}
