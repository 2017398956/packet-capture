package personal.nfl.vpn;

import android.os.Environment;

/**
 * VPN 配置
 */

public interface VPNConstants {
    int BUFFER_SIZE = 2560;
    int MAX_PAYLOAD_SIZE = 2520;
    String BASE_DIR = Environment.getExternalStorageDirectory() + "/VpnCapture/Conversation/";
    String DATA_DIR = BASE_DIR + "data/";
    String CONFIG_DIR=BASE_DIR+"config/";
    String VPN_SP_NAME="vpn_sp_name";// 保存 vpn 信息的 sp 文件名
    String IS_UDP_NEED_SAVE="isUDPNeedSave";
    String IS_UDP_SHOW = "isUDPShow";
    String DEFAULT_PACKAGE_ID = "default_package_id";// 保存选定的 app 的 id
    String DEFAULT_PACAGE_NAME = "default_package_name";// 保存选定的 app 的 name
}
