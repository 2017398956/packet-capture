package personal.nfl.networkcapture.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import personal.nfl.networkcapture.R;
import personal.nfl.networkcapture.adapter.ConnectionAdapter;
import personal.nfl.networkcapture.common.widget.BaseActivity;
import personal.nfl.vpn.VPNConstants;
import personal.nfl.vpn.nat.NatSession;
import personal.nfl.vpn.utils.ACache;
import personal.nfl.vpn.utils.TimeFormatUtil;

/**
 * 连接列表界面
 *
 * @author nfl
 */

public class ConnectionListActivity extends BaseActivity {

    public static final String FILE_DIRNAME = "file_dirname";
    // 抓到的包存放配置路径
    private String socketConfigDir;
    private ListView lv_apps;
    private SwipeRefreshLayout srl_loading;
    private ConnectionAdapter adapter;
    private List<NatSession> data = new ArrayList<>();
    private SharedPreferences sp;
    boolean showUDP;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_list);
        setActionBarTitle(R.string.capture_data);
        srl_loading = findViewById(R.id.srl_loading);
        srl_loading.setRefreshing(true);
        lv_apps = findViewById(R.id.lv_apps);
        socketConfigDir = getIntent().getStringExtra(FILE_DIRNAME);
        sp = getSharedPreferences(VPNConstants.VPN_SP_NAME, Context.MODE_PRIVATE);
        showUDP = sp.getBoolean(VPNConstants.IS_UDP_SHOW, false);
        adapter = new ConnectionAdapter(this, data);
        lv_apps.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startPacketDetailActivity(data.get(position));
            }
        });
        /*
         * 考虑到这个界面要加载大量文件，如果直接设置 adapter 后通过 notifyDataSetChanged 来刷新界面，
         * 则需要在主线程中操作，有可能会卡死，如果异步操作，data 又不能在非主线程中修改（会报错），
         * 所以每次这里采用 setAdapter 的方式实现。
         */
        // lv_apps.setAdapter(adapter);
        getDataAndRefreshView();
    }

    private void getDataAndRefreshView() {
        Observable
                .create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        data.clear();
                        File file = new File(socketConfigDir);
                        ACache aCache = ACache.get(file);
                        String[] list = file.list();
                        if (list != null && list.length > 0) {
                            for (String fileName : list) {
                                NatSession netConnection = (NatSession) aCache.getAsObject(fileName);
                                if (!showUDP && NatSession.UDP.equals(netConnection.type)) {
                                    continue;
                                }
                                data.add(netConnection);
                            }
                            Collections.sort(data, new NatSession.NatSesionComparator());
                        }
                        emitter.onNext(1);
                        emitter.onComplete();
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        if (integer == 1) {
                            lv_apps.setAdapter(adapter);
                            changeNoDataView(data);
                            srl_loading.setRefreshing(false);
                            srl_loading.setEnabled(false);
                        }
                    }
                }).subscribe();
    }

    public static void openActivity(Context context, String dir) {
        Intent intent = new Intent(context, ConnectionListActivity.class);
        intent.putExtra(FILE_DIRNAME, dir);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    private void startPacketDetailActivity(NatSession connection) {
        String dir = VPNConstants.DATA_DIR
                + TimeFormatUtil.formatYYMMDDHHMMSS(connection.getVpnStartTime())
                + "/"
                + connection.getUniqueName();
        PacketDetailActivity.startActivity(this, dir);
    }
}
