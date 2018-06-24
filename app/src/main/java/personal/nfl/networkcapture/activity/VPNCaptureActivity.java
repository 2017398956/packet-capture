package personal.nfl.networkcapture.activity;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import personal.nfl.networkcapture.R;
import personal.nfl.networkcapture.bean.parcelable.PackageShowInfo;
import personal.nfl.networkcapture.common.widget.BaseFragment;
import personal.nfl.networkcapture.cons.AppConstants;
import personal.nfl.networkcapture.fragment.CaptureFragment;
import personal.nfl.networkcapture.fragment.HistoryFragment;
import personal.nfl.networkcapture.fragment.SettingFragment;
import personal.nfl.vpn.ProxyConfig;
import personal.nfl.vpn.ProxyConfig.VpnStatusListener;
import personal.nfl.vpn.utils.VpnServiceHelper;

import static personal.nfl.vpn.VPNConstants.DEFAULT_PACAGE_NAME;
import static personal.nfl.vpn.VPNConstants.DEFAULT_PACKAGE_ID;
import static personal.nfl.vpn.VPNConstants.VPN_SP_NAME;
import static personal.nfl.vpn.utils.VpnServiceHelper.START_VPN_SERVICE_REQUEST_CODE;

/**
 * 主界面
 *
 * @author nfl
 */
public class VPNCaptureActivity extends FragmentActivity {

    private static String TAG = "VPNCaptureActivity";
    private static final int VPN_REQUEST_CODE = 101;
    private static final int REQUEST_PACKAGE = 103;
    private static final int REQUEST_STORAGE_PERMISSION = 104;
    private String selectPackage;
    private String selectName;
    private String[] needPermissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    private ImageView iv_filter_rulers, iv_vpn_switch;
    private TextView tv_title;
    private TabLayout tb_titles;
    private ViewPager vp_container;

    private SharedPreferences sharedPreferences;
    private ArrayList<BaseFragment> baseFragments;
    private FragmentPagerAdapter simpleFragmentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpn_capture);
        initView();
        setListeners();
        initData();
        ProxyConfig.Instance.registerVpnStatusListener(vpnStatusListener);
        initChildFragment();
        initViewPager();
        initTab();
    }

    private void initView() {
        iv_filter_rulers = findViewById(R.id.iv_filter_rulers);
        iv_vpn_switch = findViewById(R.id.iv_vpn_switch);
        tv_title = findViewById(R.id.tv_title);
    }

    private void setListeners() {
        iv_filter_rulers.setOnClickListener(onClickListener);
        iv_vpn_switch.setOnClickListener(onClickListener);
    }

    private void initData() {
        sharedPreferences = getSharedPreferences(VPN_SP_NAME, MODE_PRIVATE);
        selectPackage = sharedPreferences.getString(DEFAULT_PACKAGE_ID, null);
        selectName = sharedPreferences.getString(DEFAULT_PACAGE_NAME, null);
        tv_title.setText(selectName != null ? selectName : selectPackage != null ? selectPackage : getString(R.string.all));
        iv_vpn_switch.setEnabled(true);
        //推荐用户进行留评
        boolean hasFullUseApp = sharedPreferences.getBoolean(AppConstants.HAS_FULL_USE_APP, false);
        if (hasFullUseApp) {
            boolean hasShowRecommand = sharedPreferences.getBoolean(AppConstants.HAS_SHOW_RECOMMAND, false);
            if (!hasShowRecommand) {
                sharedPreferences.edit().putBoolean(AppConstants.HAS_SHOW_RECOMMAND, true).apply();
                showRecommand();
            } else {
                requestStoragePermission();
            }
        } else {
            requestStoragePermission();
        }
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, needPermissions, REQUEST_STORAGE_PERMISSION);
    }

    private void showRecommand() {
        new AlertDialog
                .Builder(this)
                .setTitle(getString(R.string.do_you_like_the_app))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showGotoStarDialog();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showGotoDiscussDialog();
                        dialog.dismiss();
                    }

                })
                .show();


    }

    private void showGotoStarDialog() {
        new AlertDialog
                .Builder(this)
                .setTitle(getString(R.string.do_you_want_star))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String url = "https://github.com/huolizhuminh/NetWorkPacketCapture";

                        launchBrowser(url);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }

                })
                .show();
    }

    private void showGotoDiscussDialog() {
        new AlertDialog
                .Builder(this)
                .setTitle(getString(R.string.go_to_give_the_issue))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String url = "https://github.com/huolizhuminh/NetWorkPacketCapture/issues";
                        launchBrowser(url);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }

                })
                .show();
    }

    public void launchBrowser(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri content_url = Uri.parse(url);
        intent.setData(content_url);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.d(TAG, "failed to launchBrowser " + e.getMessage());
        }
    }

    private void initViewPager() {
        simpleFragmentAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return baseFragments.get(position);
            }

            @Override
            public int getCount() {
                return baseFragments.size();
            }
        };
        vp_container = findViewById(R.id.vp_container);
        vp_container.setAdapter(simpleFragmentAdapter);
    }

    private void initTab() {
        tb_titles = findViewById(R.id.tb_titles);
        tb_titles.setupWithViewPager(vp_container);
        tb_titles.setTabMode(TabLayout.MODE_FIXED);
        String[] tabTitle = getResources().getStringArray(R.array.tabs);
        for (int i = 0; i < tb_titles.getTabCount(); i++) {
            TabLayout.Tab tab = tb_titles.getTabAt(i);
            tab.setText(tabTitle[i]);
        }
        tb_titles.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                //   vp_container.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void initChildFragment() {
        baseFragments = new ArrayList<>();
        // BaseFragment captureFragment = new SimpleFragment();
        BaseFragment captureFragment = new CaptureFragment();
        BaseFragment historyFragment = new HistoryFragment();
        BaseFragment settingFragment = new SettingFragment();
        baseFragments.add(captureFragment);
        baseFragments.add(historyFragment);
        baseFragments.add(settingFragment);
    }

    private void closeVpn() {
        VpnServiceHelper.changeVpnRunningStatus(this, false);
    }

    private void startVPN() {
        VpnServiceHelper.changeVpnRunningStatus(this, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ProxyConfig.Instance.unregisterVpnStatusListener(vpnStatusListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == START_VPN_SERVICE_REQUEST_CODE && resultCode == RESULT_OK) {
            VpnServiceHelper.startVpnService(getApplicationContext());
        } else if (requestCode == REQUEST_PACKAGE && resultCode == RESULT_OK) {
            PackageShowInfo showInfo = data.getParcelableExtra(PackageListActivity.SELECT_PACKAGE);
            if (showInfo == null) {
                selectPackage = null;
                selectName = null;
            } else {
                selectPackage = showInfo.packageName;
                selectName = showInfo.appName;
            }
            tv_title.setText(selectName != null ? selectName :
                    selectPackage != null ? selectPackage : getString(R.string.all));
            iv_vpn_switch.setEnabled(true);
            sharedPreferences.edit().putString(DEFAULT_PACKAGE_ID, selectPackage)
                    .putString(DEFAULT_PACAGE_NAME, selectName).apply();
        }
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.iv_vpn_switch:
                    if (VpnServiceHelper.vpnRunningStatus()) {
                        closeVpn();
                    } else {
                        startVPN();
                    }
                    break;
                case R.id.iv_filter_rulers:
                    Intent intent = new Intent(VPNCaptureActivity.this, PackageListActivity.class);
                    startActivityForResult(intent, REQUEST_PACKAGE);
                    break;
            }
        }
    };

    private VpnStatusListener vpnStatusListener = new VpnStatusListener() {

        private Consumer<Integer> consumer = new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                switch (integer) {
                    case 1:
                        iv_vpn_switch.setImageResource(R.mipmap.ic_stop);
                        break;
                    case 2:
                        iv_vpn_switch.setImageResource(R.mipmap.ic_start);
                        break;
                }
            }
        };

        @Override
        public void onVpnStart(Context context) {
            Observable
                    .just(1)
                    //.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(consumer);
        }

        @Override
        public void onVpnEnd(Context context) {
            Observable
                    .just(2)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(consumer);
        }
    };
}
