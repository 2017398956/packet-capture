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
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import personal.nfl.networkcapture.R;
import personal.nfl.networkcapture.bean.Repo;
import personal.nfl.networkcapture.bean.parcelable.PackageShowInfo;
import personal.nfl.networkcapture.common.widget.BaseActivity;
import personal.nfl.networkcapture.common.widget.BaseFragment;
import personal.nfl.networkcapture.cons.AppConstants;
import personal.nfl.networkcapture.fragment.CaptureFragment;
import personal.nfl.networkcapture.fragment.HistoryFragment;
import personal.nfl.networkcapture.fragment.SettingFragment;
import personal.nfl.networkcapture.retrofitserver.GitHubService;
import personal.nfl.vpn.ProxyConfig;
import personal.nfl.vpn.ProxyConfig.VpnStatusListener;
import personal.nfl.vpn.service.FirewallVpnService;
import personal.nfl.vpn.utils.VpnServiceHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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
    private String[] needPermissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

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
        // testRetrofit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        iv_menu_01.setImageResource(VpnServiceHelper.vpnRunningStatus() ? R.mipmap.ic_stop : R.mipmap.ic_start);
    }

    private void testRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        GitHubService service = retrofit.create(GitHubService.class);
        Call<List<Repo>> call = service.listRepos("octocat");
        call.enqueue(new Callback<List<Repo>>() {
                         @Override
                         public void onResponse(Call<List<Repo>> call, Response<List<Repo>> response) {
                             Toast.makeText(VPNCaptureActivity.this, "----", Toast.LENGTH_SHORT).show();
                         }

                         @Override
                         public void onFailure(Call<List<Repo>> call, Throwable t) {
                             Toast.makeText(VPNCaptureActivity.this, "====", Toast.LENGTH_SHORT).show();
                         }
                     }
        );
    }

    private void setListeners() {
        iv_back.setImageResource(R.drawable.switch_device_selector);
        iv_menu_01.setImageResource(R.mipmap.ic_start);
        iv_menu_01.setVisibility(View.VISIBLE);
        iv_back.setOnClickListener(onClickListener);
        iv_menu_01.setOnClickListener(onClickListener);
        tv_title.setOnClickListener(onClickListener);
    }

    private void initData() {
        sharedPreferences = getSharedPreferences(VPN_SP_NAME, MODE_PRIVATE);
        selectPackage = sharedPreferences.getString(DEFAULT_PACKAGE_ID, null);
        selectName = sharedPreferences.getString(DEFAULT_PACAGE_NAME, null);
        tv_title.setText(selectName != null ? selectName : selectPackage != null ? selectPackage : getString(R.string.all));
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

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tv_title:
                    break;
                case R.id.iv_menu_01:
                    pb_loading.setVisibility(View.VISIBLE);
                    if (VpnServiceHelper.getVpnServiceStatus() == FirewallVpnService.Status.STATUS_AVAILABLE) {
                        startVPN();
                    } else if (VpnServiceHelper.getVpnServiceStatus() == FirewallVpnService.Status.STATUS_RUNNING) {
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
                        break;
                    case 2:
                        iv_menu_01.setImageResource(R.mipmap.ic_start);
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
