package personal.nfl.vpn.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by nfl 2018/11/9 10:56
 */
public class WebUtil {
    private static final String TAG = "WebUtil";

    public static void launchBrowser(Activity activity, String url) {
        if (null == activity || TextUtils.isEmpty(url)) {
            Log.d(TAG, "failed to launchBrowser ");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri content_url = Uri.parse(url);
        intent.setData(content_url);
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.d(TAG, "failed to launchBrowser " + e.getMessage());
        }
    }

}
