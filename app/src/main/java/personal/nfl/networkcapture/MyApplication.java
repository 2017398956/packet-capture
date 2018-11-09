package personal.nfl.networkcapture;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.multidex.MultiDex;

import personal.nfl.networkcapture.common.util.BaseActivityLifecycleCallbacks;
import personal.nfl.networkcapture.common.util.BuglyUtil;
import personal.nfl.networkcapture.common.util.PackageUtil;
import personal.nfl.permission.support.constant.ApplicationConstant;
import personal.nfl.permission.support.util.AbcPermission;

/**
 * @author nfl
 */

public class MyApplication extends Application {
    public static final String BUGLY_APP_ID = "47a443716c";
    public static final String BUGLY_APP_KEY = "156fdd48-3b6d-4a64-9429-f7d004640b2c";
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        // PackageUtil.test(this);
        AbcPermission.install(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                ApplicationConstant.nowActivity = activity;
            }

            @Override
            public void onActivityStarted(Activity activity) {
                ApplicationConstant.nowActivity = activity;
            }

            @Override
            public void onActivityResumed(Activity activity) {
                ApplicationConstant.nowActivity = activity;
            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
        BuglyUtil.initBugly(context, BUGLY_APP_ID);
        registerActivityLifecycleCallbacks(new BaseActivityLifecycleCallbacks());
        // 获取当前包名
        String packageName = getPackageName();
        // 获取当前进程名
        String processName = PackageUtil.getProcessName(android.os.Process.myPid());
        if (processName == null || processName.equals(packageName)) {
            // 主进程
            // TODO 只有在主进程中才使用 vpn 服务
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static Context getContext() {
        return context;
    }

}
