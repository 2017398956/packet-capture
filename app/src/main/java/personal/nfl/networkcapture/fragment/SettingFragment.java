package personal.nfl.networkcapture.fragment;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import personal.nfl.networkcapture.R;
import personal.nfl.networkcapture.activity.AboutActivity;
import personal.nfl.networkcapture.common.util.FileUtils;
import personal.nfl.networkcapture.common.widget.BaseFragment;
import personal.nfl.vpn.service.FirewallVpnService;
import personal.nfl.vpn.utils.ThreadProxy;
import personal.nfl.vpn.VPNConstants;

import java.io.File;
import java.io.FileFilter;

/**
 * 设置界面
 * @author nfl
 */

public class SettingFragment extends BaseFragment {

    private Handler handler;
    private ProgressBar pb;
    private CheckBox includeCurrentCapture;
    private boolean isRunning;
    private SharedPreferences sp;
    private CheckBox cbShowUDP;
    private CheckBox cbSaveUDP;
    private boolean saveUDP;
    private boolean showUDP;

    @Override
    protected int getLayout() {
        return R.layout.fragment_setting;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.clear_cache_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isDeleting) {
                    return;
                }
                isDeleting = true;
                pb.setVisibility(View.VISIBLE);
                clearHistoryData();
            }
        });
        view.findViewById(R.id.about_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), AboutActivity.class));
            }
        });
        cbShowUDP = view.findViewById(R.id.show_udp);
        cbSaveUDP = view.findViewById(R.id.save_udp);
        sp = getContext().getSharedPreferences(VPNConstants.VPN_SP_NAME, Context.MODE_PRIVATE);
        saveUDP = sp.getBoolean(VPNConstants.IS_UDP_NEED_SAVE, false);
        showUDP = sp.getBoolean(VPNConstants.IS_UDP_SHOW, false);
        cbSaveUDP.setChecked(saveUDP);
        cbShowUDP.setChecked(showUDP);
        cbShowUDP.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showUDP = isChecked;
                sp.edit().putBoolean(VPNConstants.IS_UDP_SHOW, showUDP).apply();
            }
        });
        cbSaveUDP.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveUDP = isChecked;
                sp.edit().putBoolean(VPNConstants.IS_UDP_NEED_SAVE, saveUDP).apply();
            }
        });
        includeCurrentCapture = view.findViewById(R.id.check_current_capture);

        pb = view.findViewById(R.id.pb);
        handler = new Handler();
    }

    private boolean isDeleting;

    private void clearHistoryData() {
        ThreadProxy.getInstance().execute(new Runnable() {


            @Override
            public void run() {

                File file = new File(VPNConstants.BASE_DIR);
                FileUtils.deleteFile(file, new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        if (includeCurrentCapture.isChecked()) {
                            return true;
                        }
                        if (!pathname.exists()) {
                            return false;
                        }

                        String lastVpnStartTimeStr = FirewallVpnService.lastVpnStartTimeFormat;
                        if (lastVpnStartTimeStr == null) {
                            return true;
                        }
                        String absolutePath = pathname.getAbsolutePath();
                        //如果所选择文件是最近一次产生的，则不删除
                        return !absolutePath.contains(lastVpnStartTimeStr);
                    }
                });
                isDeleting = false;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        pb.setVisibility(View.GONE);
                        showMessage(getString(R.string.success_clear_history_data));
                    }
                });

            }
        });


    }

    private void showMessage(String string) {
        Toast.makeText(getActivity(), string, Toast.LENGTH_SHORT).show();
    }
}
