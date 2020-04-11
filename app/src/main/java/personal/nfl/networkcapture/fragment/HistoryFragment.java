package personal.nfl.networkcapture.fragment;


import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import personal.nfl.networkcapture.R;
import personal.nfl.networkcapture.adapter.HistoryListAdapter;
import personal.nfl.networkcapture.common.widget.BaseFragment;
import personal.nfl.vpn.VPNConstants;
import personal.nfl.vpn.utils.ThreadProxy;

/**
 * 历史界面
 *
 * @author nfl
 */

public class HistoryFragment extends BaseFragment {

    private SwipeRefreshLayout refreshContainer;
    private RecyclerView timeList;
    private String[] list;
    private Handler handler = new Handler();
    private HistoryListAdapter historyListAdapter;
    private String[] rawList;
    private List<String> data = new ArrayList<>();

    @Override
    protected int getLayout() {
        return R.layout.fragment_history;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshContainer = view.findViewById(R.id.refresh_container);
        timeList = view.findViewById(R.id.time_list);
        timeList.setLayoutManager(new LinearLayoutManager(getActivity()));
        refreshContainer.setEnabled(true);
        refreshContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getDataAndRefreshView();
            }
        });
        historyListAdapter = new HistoryListAdapter(getContext(), data);
        getDataAndRefreshView();
    }

    private void getDataAndRefreshView() {
        ThreadProxy.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                File file = new File(VPNConstants.CONFIG_DIR);
                // 获取 VPNCapture/conversation/config 下的所有文件夹
                File[] files = file.listFiles();
                if (files != null && files.length > 0) {
                    List<File> fileList = new ArrayList<>();
                    Collections.addAll(fileList, files);
                    Collections.sort(fileList, new Comparator<File>() {
                        @Override
                        public int compare(File o1, File o2) {
                            return (int) (o2.lastModified() - o1.lastModified());
                        }
                    });
                    for (File fileTemp : fileList) {
                        data.add(fileTemp.getName());
                    }
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        timeList.setAdapter(historyListAdapter);
                        refreshContainer.setRefreshing(false);
                    }
                });
            }
        });
    }
}
