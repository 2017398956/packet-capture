package personal.nfl.networkcapture.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
    private String dir;
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
        lv_detail.setAdapter(adapter);
        pg = findViewById(R.id.pg);
        dir = getIntent().getStringExtra(CONVERSATION_DATA);
        sp = getSharedPreferences(AppConstants.DATA_SAVE, MODE_PRIVATE);
        sp.edit().putBoolean(AppConstants.HAS_FULL_USE_APP, true).apply();
        refreshView();
    }

    private void refreshView() {
        ThreadProxy.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                data.clear();
                File file = new File(dir);
                File[] files = file.listFiles();
                if (files != null || files.length > 0) {
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
                        adapter.notifyDataSetChanged();
                        pg.setVisibility(View.GONE);
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
