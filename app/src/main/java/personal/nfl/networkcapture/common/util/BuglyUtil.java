package personal.nfl.networkcapture.common.util;

import android.content.Context;
import android.support.annotation.Nullable;

import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.crashreport.CrashReport;

import personal.nfl.networkcapture.BuildConfig;

/**
 * @author nfl
 */
public class BuglyUtil {

    /**
     * 可以制造一个Crash（建议通过“按键”来触发），来体验 Bugly
     * 执行到这段代码时会发生一个Crash，可以查看 Logcat 的 TAG = CrashReportInfo
     */
    public static void testJavaCrash() {
        CrashReport.testJavaCrash();
    }

    public static void initBugly(@Nullable Context context, @Nullable String buglyAppId) {
        /**
         * 第三个参数为 SDK 调试模式开关，调试模式的行为特性如下：
         * 输出详细的 Bugly SDK 的 Log；
         * 每一条 Crash 都会被立即上报；
         * 自定义日志将会在 Logcat 中输出。
         * 建议在测试阶段建议设置成 true，发布时设置为 false。
         */
        // 获取当前包名
        String packageName = context.getPackageName();
        // 获取当前进程名
        String processName = PackageUtil.getProcessName(android.os.Process.myPid());
        // 设置是否为上报进程
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(context);
        strategy.setUploadProcess(processName == null || processName.equals(packageName));
        // 初始化Bugly
        CrashReport.initCrashReport(context, buglyAppId, BuildConfig.bugly);
        // 如果通过“AndroidManifest.xml”来配置APP信息，初始化方法如下
        // CrashReport.initCrashReport(context, strategy);
    }

    /**
     * @param isManual  用户手动点击检查，非用户点击操作请传false
     * @param isSilence 是否显示弹窗等交互，[true:没有弹窗和toast] [false:有弹窗或toast]
     */
    public static void checkUpgrade(boolean isManual, boolean isSilence) {
        Beta.checkUpgrade(false, false);
    }
}
