package personal.nfl.networkcapture.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.Nullable;

import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import personal.nfl.networkcapture.R;
import personal.nfl.networkcapture.adapter.DetailAdapter;
import personal.nfl.networkcapture.common.widget.BaseActivity;
import personal.nfl.networkcapture.cons.AppConstants;
import personal.nfl.vpn.utils.SaveDataFileParser;
import personal.nfl.vpn.utils.ThreadProxy;

/**
 * 抓包详情界面
 *
 * @author nfl
 */

public class PacketDetailActivity extends BaseActivity {
    public static final String CONVERSATION_DATA = "conversation_data";
    private static final String TAG = "PacketDetailActivity";
    /**
     * 包数据的存放路径
     */
    private String socketDataDir;
    private SharedPreferences sp;
    private ProgressBar pg;
    private ListView lv_detail;
    private DetailAdapter adapter;
    private List<SaveDataFileParser.ShowData> data = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_packet_detail);
        setActionBarTitle(R.string.socket_detail);
        lv_detail = findViewById(R.id.lv_detail);
        adapter = new DetailAdapter(context, data);

        /**
         * 为了加载大文件时不卡顿，这里不能直接调用 setAdapter 再后面 notifyDataSetChanged
         * 应该用的时候再设置。
         * TODO 这里先将刷新放在 onResume 中，测试导致这种情况的原因
         */
        lv_detail.setAdapter(adapter);

        pg = findViewById(R.id.pg);
        socketDataDir = getIntent().getStringExtra(CONVERSATION_DATA);
        sp = getSharedPreferences(AppConstants.DATA_SAVE, MODE_PRIVATE);
        sp.edit().putBoolean(AppConstants.HAS_FULL_USE_APP, true).apply();
//        refreshView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshView();
    }

    private void refreshView() {
        ThreadProxy.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                data.clear();
                File file = new File(socketDataDir);
                File[] files = file.listFiles();
                if (files != null && files.length > 0) {
                    List<File> filesList = new ArrayList<>();
                    for (File childFile : files) {
                        filesList.add(childFile);
                    }
                    Collections.sort(filesList, new Comparator<File>() {
                        @Override
                        public int compare(File o1, File o2) {
                            return (int) (o1.lastModified() - o2.lastModified());
                        }
                    });
                    SaveDataFileParser.ShowData showData;
                    for (File childFile : filesList) {
                        showData = SaveDataFileParser.parseSaveFile(childFile);
                        if (showData != null) {
                            data.add(showData);
                        }
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        lv_detail.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                        pg.setVisibility(View.GONE);
                        changeNoDataView(data);
                    }
                });
            }
        });
    }


    public static void startActivity(Activity context, String dir) {
        Intent intent = new Intent(context, PacketDetailActivity.class);
        intent.putExtra(CONVERSATION_DATA, dir);
        context.startActivity(intent);
    }
}
