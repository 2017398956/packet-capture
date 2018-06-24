package personal.nfl.networkcapture;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import personal.nfl.networkcapture.common.util.BuglyUtil;

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
        BuglyUtil.initBugly(context, BUGLY_APP_ID);
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
