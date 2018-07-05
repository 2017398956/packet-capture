package personal.nfl.vpn.processparse;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import personal.nfl.vpn.VPNLog;

/**
 * 获取每个应用访问过的 ip 和 端口
 *
 * @author nfl
 */
public class NetFileManager {

    private static final String TAG = "NetFileManager";

    /**
     * 系统配置文件中每条信息最少可以被分割的份数
     */
    public final static int DATA_MIN_LENGTH = 9;
    /**
     * 从系统配置文件的每条信息中提取的 ip 地址 和 端口号是以 “：”分割的，所以校验分割后的长度应为 2
     */
    public final static int DATA_IP_ADDRESS_PARTS = 2;
    /**
     * ip 地址的 16 进制最小长度，8 是 IPv4 的
     */
    public final static int IP_ADDRESS_MIN_LENGTH = 8;
    public final static String SYSTEM_DEFAULT_IP_ADDRESS = "0.0.0.0";
    public final static int TYPE_TCP = 0;
    public final static int TYPE_TCP6 = 1;
    public final static int TYPE_UDP = 2;
    public final static int TYPE_UDP6 = 3;
    public final static int TYPE_RAW = 4;
    public final static int TYPE_RAW6 = 5;
    public final static int TYPE_MAX = 6;

    private final static int DATA_LOCAL = 2;
    private final static int DATA_REMOTE = 3;
    private final static int DATA_UID = 8;
    /**
     * key : app 进程使用的端口号（sourcePort） value : UID
     */
    private Map<Integer, Integer> processHost = new ConcurrentHashMap<>();
    private File[] file;
    private long[] lastTime;
    private StringBuilder sbBuilder = new StringBuilder();

    static class InnerClass {
        static NetFileManager instance = new NetFileManager();
    }

    public static NetFileManager getInstance() {
        InnerClass.instance.init();
        return InnerClass.instance;
    }

    private void init() {
        final String PATH_TCP = "/proc/net/tcp";
        final String PATH_TCP6 = "/proc/net/tcp6";
        final String PATH_UDP = "/proc/net/udp";
        final String PATH_UDP6 = "/proc/net/udp6";
        final String PATH_RAW = "/proc/net/raw";
        final String PATH_RAW6 = "/proc/net/raw6";

        file = new File[TYPE_MAX];
        file[0] = new File(PATH_TCP);
        file[1] = new File(PATH_TCP6);
        file[2] = new File(PATH_UDP);
        file[3] = new File(PATH_UDP6);
        file[4] = new File(PATH_RAW);
        file[5] = new File(PATH_RAW6);

        lastTime = new long[TYPE_MAX];
        // 初始化 lastTime 中的各个值
        Arrays.fill(lastTime, 0);
    }

    public void execute(String[] command, String directory, int type) throws IOException {
        NetInfo netInfo = null;
        String sTmp = null;

        ProcessBuilder builder = new ProcessBuilder(command);

        if (directory != null) {
            builder.directory(new File(directory));
        }
        builder.redirectErrorStream(true);
        Process process = builder.start();
        InputStream is = process.getInputStream();

        Scanner s = new Scanner(is);
        s.useDelimiter("\n");
        while (s.hasNextLine()) {
            sTmp = s.nextLine();
            netInfo = parseDataNew(sTmp);
            if (netInfo != null) {
                netInfo.setType(type);
                saveToMap(netInfo);
            }
        }
    }

    /**
     * 将字符串转换为 int 数据，并在传入字符串 str 为 null 时返回默认值 iDefault ,在本类中的一个作用是：
     * 将从 ip 地址中获取的字符串格式端口号转换为 int 类型
     *
     * @param str      端口号的字符串类型，一般为 16 进制 xxxx 四位。
     * @param iHex     str 是几进制的
     * @param iDefault 如果 str 为 null 的默认返回值
     * @return
     */
    private int strToInt(String str, int iHex, int iDefault) {
        int iValue = iDefault;
        if (str == null) {
            return iValue;
        }
        try {
            iValue = Integer.parseInt(str, iHex);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return iValue;
    }

    private long strToLong(String value, int iHex, int iDefault) {
        long iValue = iDefault;
        if (value == null) {
            return iValue;
        }
        try {
            iValue = Long.parseLong(value, iHex);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return iValue;
    }

    /**
     * 这是 tcp6 文件中关于本 app 访问 github 的一个示例，其中 10126 是本 app 的 uid 已验证。
     * sl  local_address                         remote_address                        st tx_queue          rx_queue    tr       tm->when retrnsmt   uid  timeout inode
     * 读取的每条信息为（引号内）：
     * " 3: 0000000000000000FFFF00000F03000A:C204  0000000000000000FFFF000075D06071:0050 08 00000000:00000001 00:00000000 00000000 10126        0 38012 1 00000000 20 4 26 10 -1"
     * 0 1  2                                      3                                     4  5                 6           7        8
     * 注意：由于会用空格截取字符串，所以“3:”开始的位置是 1 。
     * 因为有些项不只有一个空格，我们不能只是简单的用 " " 去区分。这里我们使用正则表达式: \s+ 分隔。可以分隔多个或一个空格。
     * 其中第 3 项即是远程 Ip 地址 和 端口项，第 8 项为 UID
     *
     * @param sData
     * @return
     */
    private NetInfo parseDataNew(@NonNull String sData) {
        String[] sSplitItem = sData.split("\\s+");
        if (sSplitItem.length < DATA_MIN_LENGTH) {
            return null;
        }
        String sTmp = null;
        NetInfo netInfo = new NetInfo();
        // 取得本地 ip 和 端口号
        sTmp = sSplitItem[DATA_LOCAL];
        String[] sSourceItem = sTmp.split(":");
        if (sSourceItem.length < DATA_IP_ADDRESS_PARTS) {
            return null;
        }
        netInfo.setSourPort(strToInt(sSourceItem[1], 16, 0));
        // 取得远程 ip 和 端口号
        sTmp = sSplitItem[DATA_REMOTE];
        String[] sDesItem = sTmp.split(":");
        if (sDesItem.length < DATA_IP_ADDRESS_PARTS) {
            return null;
        }
        netInfo.setPort(strToInt(sDesItem[1], 16, 0));
        sTmp = sDesItem[0];
        int len = sTmp.length();
        if (len < IP_ADDRESS_MIN_LENGTH) {
            return null;
        }
        /**
         * 截取 ip 地址的后 8 位，是为了适配 IPv6 。应为 IPv6 中这个地址是很长的，
         * 详见 {@link #parseDataNew(String)} 的注释
         */
        sTmp = sTmp.substring(len - 8);
        // 将 ip 地址转换为 long 后保存
        netInfo.setIp(strToLong(sTmp, 16, 0));
        sbBuilder.setLength(0);
        // 获得形如 192.168.1.1 的 ip 地址
        sbBuilder.append(strToInt(sTmp.substring(6, 8), 16, 0))
                .append(".")
                .append(strToInt(sTmp.substring(4, 6), 16, 0))
                .append(".")
                .append(strToInt(sTmp.substring(2, 4), 16, 0))
                .append(".")
                .append(strToInt(sTmp.substring(0, 2), 16, 0));

        sTmp = sbBuilder.toString();
        netInfo.setAddress(sTmp);

        if (SYSTEM_DEFAULT_IP_ADDRESS.equals(sTmp)) {
            return null;
        }
        sTmp = sSplitItem[DATA_UID];
        netInfo.setUid(strToInt(sTmp, 10, 0));
        return netInfo;
    }

    private void saveToMap(NetInfo netInfo) {
        if (netInfo == null) {
            return;
        }
        VPNLog.d(TAG, "saveToMap  port " + netInfo.getSourPort() + " uid " + netInfo.getUid());
        processHost.put(netInfo.getSourPort(), netInfo.getUid());
    }

    /**
     * 根据 协议类型 type 刷新 app 网络请求端口与 uid 的对应关系
     *
     * @param type
     */
    public void read(int type) {
        try {
            switch (type) {
                case TYPE_TCP:
                    String[] ARGS = {"cat", "/proc/net/tcp"};
                    execute(ARGS, "/", TYPE_TCP);
                    break;
                case TYPE_TCP6:
                    String[] ARGS1 = {"cat", "/proc/net/tcp6"};
                    execute(ARGS1, "/", TYPE_TCP6);
                    break;
                case TYPE_UDP:
                    String[] ARGS2 = {"cat", "/proc/net/udp"};
                    execute(ARGS2, "/", TYPE_UDP);
                    break;
                case TYPE_UDP6:
                    String[] ARGS3 = {"cat", "/proc/net/udp6"};
                    execute(ARGS3, "/", TYPE_UDP6);
                    break;
                case TYPE_RAW:
                    String[] ARGS4 = {"cat", "/proc/net/raw"};
                    execute(ARGS4, "/", TYPE_UDP);
                    break;
                case TYPE_RAW6:
                    String[] ARGS5 = {"cat", "/proc/net/raw6"};
                    execute(ARGS5, "/", TYPE_UDP6);
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 当 Android 系统有网络请求时，会改变配置文件，如果检测到配置文件的最后修改时间发生改变，就刷新
     * 相应文件中的信息。因为 {@linkplain #lastTime} 的初始值都是 0 ，所以第一次调用时会遍历所有的配置文件
     */
    public void refresh() {
        for (int i = 0; i < TYPE_MAX; i++) {
            long iTime = file[i].lastModified();
            if (iTime != lastTime[i]) {
                read(i);
                lastTime[i] = iTime;
            }
        }
    }

    /**
     * 根据 sourcePort（进程访问网络使用的端口）返回进程的 uid
     *
     * @param sourcePort
     * @return
     */
    public Integer getUid(int sourcePort) {
        return processHost.get(sourcePort);
    }
}
