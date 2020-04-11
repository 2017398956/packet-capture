package personal.nfl.networkcapture.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;

import personal.nfl.networkcapture.R;
import personal.nfl.networkcapture.cons.AppConstants;
import personal.nfl.vpn.utils.WebUtil;

import static android.content.Context.MODE_PRIVATE;
import static personal.nfl.vpn.VPNConstants.VPN_SP_NAME;

/**
 * Created by nfl 2018/11/9 10:51
 */
public class DialogUtil {

    public static void showRecommand(Context context) {
        if (null == context) {
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(VPN_SP_NAME, MODE_PRIVATE);
        //推荐用户进行留评
        boolean hasFullUseApp = sharedPreferences.getBoolean(AppConstants.HAS_FULL_USE_APP, false);
        if (hasFullUseApp) {
            boolean hasShowRecommand = sharedPreferences.getBoolean(AppConstants.HAS_SHOW_RECOMMAND, false);
            if (!hasShowRecommand) {
                sharedPreferences.edit().putBoolean(AppConstants.HAS_SHOW_RECOMMAND, true).apply();
                new AlertDialog
                        .Builder(context)
                        .setTitle(context.getString(R.string.do_you_like_the_app))
                        .setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showGotoStarDialog(context);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(context.getString(R.string.no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showGotoDiscussDialog(context);
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        }
    }

    private static void showGotoStarDialog(Context context) {
        new AlertDialog
                .Builder(context)
                .setTitle(context.getString(R.string.do_you_want_star))
                .setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String url = "https://github.com/huolizhuminh/NetWorkPacketCapture";
                        WebUtil.launchBrowser((Activity) context, url);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }

                })
                .show();
    }

    private static void showGotoDiscussDialog(Context context) {
        new AlertDialog
                .Builder(context)
                .setTitle(context.getString(R.string.go_to_give_the_issue))
                .setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String url = "https://github.com/huolizhuminh/NetWorkPacketCapture/issues";
                        WebUtil.launchBrowser((Activity) context, url);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }

                })
                .show();
    }

}
