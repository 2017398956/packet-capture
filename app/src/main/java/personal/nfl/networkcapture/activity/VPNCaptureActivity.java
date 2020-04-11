package personal.nfl.networkcapture.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import personal.nfl.networkcapture.R;
import personal.nfl.networkcapture.bean.parcelable.PackageShowInfo;
import personal.nfl.networkcapture.common.widget.BaseActivity;
import personal.nfl.networkcapture.common.widget.BaseFragment;
import personal.nfl.networkcapture.fragment.CaptureFragment;
import personal.nfl.networkcapture.fragment.HistoryFragment;
import personal.nfl.networkcapture.fragment.SettingFragment;
import personal.nfl.networkcapture.utils.DialogUtil;
import personal.nfl.permission.annotation.GetPermissions4AndroidX;
import personal.nfl.permission.annotation.GetPermissionsAuto;
import personal.nfl.vpn.ProxyConfig;
import personal.nfl.vpn.ProxyConfig.VpnStatusListener;
import personal.nfl.vpn.service.FirewallVpnService;
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
public class VPNCaptureActivity extends BaseActivity {

    private static String TAG = "VPNCaptureActivity";
    private static final int VPN_REQUEST_CODE = 101;
    private static final int REQUEST_PACKAGE = 103;
    private static final int REQUEST_STORAGE_PERMISSION = 104;
    private String selectPackage;
    private String selectName;

    private TabLayout tb_titles;
    private ViewPager vp_container;

    private SharedPreferences sharedPreferences;
    private ArrayList<BaseFragment> baseFragments;
    private FragmentPagerAdapter simpleFragmentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpn_capture);
        setListeners();
        initData();
        ProxyConfig.Instance.registerVpnStatusListener(vpnStatusListener);
        initChildFragment();
        initViewPager();
        initTab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        iv_menu_01.setImageResource(VpnServiceHelper.vpnRunningStatus() ? R.mipmap.ic_stop : R.mipmap.ic_start);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case START_VPN_SERVICE_REQUEST_CODE:
                    VpnServiceHelper.startVpnService(getApplicationContext());
                    break;
                // 选择特定的 app 来抓包
                case REQUEST_PACKAGE:
                    PackageShowInfo showInfo = data.getParcelableExtra(PackageListActivity.SELECT_PACKAGE);
                    if (showInfo == null) {
                        selectPackage = null;
                        selectName = null;
                    } else {
                        selectPackage = showInfo.packageName;
                        selectName = showInfo.appName;
                    }
                    tv_title.setText(selectName != null ? selectName : selectPackage != null ? selectPackage : getString(R.string.all));
                    // 将选择要抓包的 app 信息保存到 sp 中
                    sharedPreferences.edit().putString(DEFAULT_PACKAGE_ID, selectPackage).putString(DEFAULT_PACAGE_NAME, selectName).commit();
                    // TODO 重启 vpn
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ProxyConfig.Instance.unregisterVpnStatusListener(vpnStatusListener);
    }

    private void setListeners() {
        iv_back.setImageResource(R.drawable.switch_device_selector);
        iv_menu_01.setImageResource(R.mipmap.ic_start);
        iv_menu_01.setVisibility(View.VISIBLE);
        iv_back.setOnClickListener(onClickListener);
        iv_menu_01.setOnClickListener(onClickListener);
        tv_title.setOnClickListener(onClickListener);
        pb_loading.setOnClickListener(onClickListener);
    }

    @GetPermissions4AndroidX({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE})
    private void initData() {
        sharedPreferences = getSharedPreferences(VPN_SP_NAME, MODE_PRIVATE);
        selectPackage = sharedPreferences.getString(DEFAULT_PACKAGE_ID, null);
        selectName = sharedPreferences.getString(DEFAULT_PACAGE_NAME, null);
        tv_title.setText(selectName != null ? selectName : selectPackage != null ? selectPackage : getString(R.string.all));
        //推荐用户进行留评
        DialogUtil.showRecommand(this);
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

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tv_title:
                    break;
                case R.id.pb_loading:
                    VpnServiceHelper.stopVpnService();
                    break;
                case R.id.iv_menu_01:
                    pb_loading.setVisibility(View.VISIBLE);
                    if (VpnServiceHelper.getVpnServiceStatus() == FirewallVpnService.Status.STATUS_AVAILABLE) {
                        Log.e("NFL", "准备打开 VPN");
                        startVPN();
                    } else if (VpnServiceHelper.getVpnServiceStatus() == FirewallVpnService.Status.STATUS_RUNNING) {
                        Log.e("NFL", "准备关闭 VPN");
                        closeVpn();
                    } else {

                    }
                    Log.d("NFL", VpnServiceHelper.getVpnServiceStatus() + "");
                    break;
                case R.id.iv_back:
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
                        iv_menu_01.setImageResource(R.mipmap.ic_stop);
                        pb_loading.setVisibility(View.GONE);
                        break;
                    case 2:
                        iv_menu_01.setImageResource(R.mipmap.ic_start);
                        pb_loading.setVisibility(View.GONE);
                        break;
                    case 3:
                        pb_loading.setVisibility(View.VISIBLE);
                        iv_menu_01.setVisibility(View.GONE);
                        break;
                    case 4:
                        pb_loading.setVisibility(View.GONE);
                        iv_menu_01.setVisibility(View.VISIBLE);
                        break;
                }
            }
        };

        @Override
        public void onVpnAvailable(Context context) {
            Observable
                    .just(4)
                    //.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(consumer);
        }

        @Override
        public void onVpnPreParing(Context context) {
            Observable
                    .just(3)
                    //.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(consumer);
        }

        @Override
        public void onVpnRunning(Context context) {
            Observable
                    .just(1)
                    //.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(consumer);
            Observable
                    .just(4)
                    //.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(consumer);
        }

        @Override
        public void onVpnStopping(Context context) {
            Observable
                    .just(3)
                    //.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(consumer);
        }

        @Override
        public void onVpnStop(Context context) {
            Observable
                    .just(2)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(consumer);
            Observable
                    .just(4)
                    //.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(consumer);
        }
    };
}
