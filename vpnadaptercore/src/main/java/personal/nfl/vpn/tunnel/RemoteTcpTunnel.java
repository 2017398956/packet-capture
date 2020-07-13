package personal.nfl.vpn.tunnel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import personal.nfl.vpn.Packet;
import personal.nfl.vpn.VPNConstants;
import personal.nfl.vpn.nat.NatSession;
import personal.nfl.vpn.nat.NatSessionManager;
import personal.nfl.vpn.processparse.AppInfoCreator;
import personal.nfl.vpn.tcpip.IPHeader;
import personal.nfl.vpn.tcpip.TCPHeader;
import personal.nfl.vpn.utils.ACache;
import personal.nfl.vpn.utils.TcpDataSaveHelper;
import personal.nfl.vpn.utils.ThreadProxy;
import personal.nfl.vpn.utils.TimeFormatUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;

/**
 * 用于 tcp 服务器到目标服务器的 tunnel
 */
public class RemoteTcpTunnel extends RawTcpTunnel {
    TcpDataSaveHelper helper;
    NatSession session;
    private final Handler handler;

    public RemoteTcpTunnel(InetSocketAddress serverAddress, Selector selector, short portKey) throws IOException {
        super(serverAddress, selector, portKey);
        // 根据所使用 app 的端口获取相应的会话信息
        session = NatSessionManager.getSession(portKey);
        // 将这个会话保存到这个目录
        String helperDir = new StringBuilder()
                .append(VPNConstants.DATA_DIR)
                .append(TimeFormatUtil.formatYYMMDDHHMMSS(session.vpnStartTime))
                .append("/")
                .append(session.getUniqueName())
                .toString();
        helper = new TcpDataSaveHelper(helperDir);
        handler = new Handler(Looper.getMainLooper());
    }


    @Override
    protected void afterReceived(ByteBuffer buffer , boolean isHttpsRequest) throws Exception {
        super.afterReceived(buffer , isHttpsRequest);
        refreshSessionAfterRead(buffer.limit());
        if(isHttpsRequest){
            // Log.i("NFL" , "目标服务器返回的数据：" + new String(buffer.array() , 0 , buffer.limit())) ;
        }else {
            IPHeader ipHeader = new IPHeader(buffer.array() , 0) ;
            TCPHeader tcpHeader = new TCPHeader(ipHeader.mData , ipHeader.getHeaderLength());
            int topLength = ipHeader.getHeaderLength() + tcpHeader.getHeaderLength() ;
            topLength = 0 ;
            Log.i("NFL" , "目标服务器返回的数据：" + new String( ipHeader.mData , topLength , buffer.limit() - topLength)) ;
        }

        // 将 目标服务器 返回的数据保存下来
        TcpDataSaveHelper.SaveData saveData = new TcpDataSaveHelper
                .SaveData
                .Builder()
                .isRequest(false)
                .needParseData(buffer.array())
                .length(buffer.limit())
                .offSet(0)
                .build();
        helper.addData(saveData);
    }

    @Override
    protected void beforeSend(ByteBuffer buffer , boolean isHttpsRequest) throws Exception {
        super.beforeSend(buffer , isHttpsRequest);
        TcpDataSaveHelper.SaveData saveData = new TcpDataSaveHelper
                .SaveData
                .Builder()
                .isRequest(true)
                .needParseData(buffer.array())
                .length(buffer.limit())
                .offSet(0)
                .build();
        helper.addData(saveData);
        refreshAppInfo();
    }

    private void refreshAppInfo() {
        if (session.appInfo != null) {
            return;
        }
        if (AppInfoCreator.getInstance() != null) {
            ThreadProxy.getInstance().execute(new Runnable() {
                @Override
                public void run() {
                    AppInfoCreator.getInstance().refreshSessionInfo();
                }
            });
        }
    }

    private void refreshSessionAfterRead(int size) {
        session.receivePacketNum++;
        session.receiveByteNum += size;
    }

    @Override
    protected void onDispose() {
        super.onDispose();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ThreadProxy.getInstance().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (session.receiveByteNum == 0 && session.bytesSent == 0) {
                            return;
                        }

                        String configFileDir = VPNConstants.CONFIG_DIR
                                +TimeFormatUtil.formatYYMMDDHHMMSS(session.vpnStartTime) ;
                        File parentFile = new File(configFileDir);
                        if (!parentFile.exists()) {
                            parentFile.mkdirs();
                        }
                        //说已经存了
                        File file = new File(parentFile, session.getUniqueName());
                        if (file.exists()) {
                            return;
                        }
                        ACache configACache = ACache.get(parentFile);
                        configACache.put(session.getUniqueName(), session);
                    }
                });
            }
        }, 1000);
    }
}
