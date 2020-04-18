package personal.nfl.vpn.service;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import personal.nfl.vpn.utils.StringUtil;

public class VpnTestService extends VpnService {

    private static final String TAG = "NFL";
    private ParcelFileDescriptor vpnInterface = null;
    private static final String VPN_ADDRESS = "10.0.0.2"; // Only IPv4 support for now
    private static final String VPN_ROUTE = "0.0.0.0"; // Intercept everything
    /**
     * 下面是一些常见的 DNS 地址
     */
    static final String GOOGLE_DNS_FIRST = "8.8.8.8";
    static final String GOOGLE_DNS_SECOND = "8.8.4.4";
    static final String AMERICA = "208.67.222.222";
    static final String HK_DNS_SECOND = "205.252.144.228";
    static final String CHINA_DNS_FIRST = "114.114.114.114";
    public static final int MUTE_SIZE = 2560;
    private ParcelFileDescriptor mInterface;
    private VpnService.Builder builder;
    private String mParameters;

    public VpnTestService() {
        Log.i("NFL", "VpnTestService's VpnTestService.");
        initConfig();
    }

    private void initConfig() {
        // 为 VPN 配置适当的参数
        builder = new Builder();
        // 即表示虚拟网络端口的最大传输单元，如果发送的包长度超过这个数字，则会被分包；一般设为1500
        builder.setMtu(1500);
        // 即这个虚拟网络端口的IP地址；这个地址可以去查查，我参考的360流量卫士里面的地址为192.168.*.*;
        // 好多也使用 10.0.2.0；不确定，都可以试试。
        builder.addAddress(VPN_ADDRESS, 32);
        // 只有匹配上的 IP 包，才会被路由到虚拟端口上去。如果是 0.0.0.0/0 的话，则会将所有的 IP 包都路由到虚拟端口上去；
        builder.addRoute(VPN_ROUTE, 0);
        // 就是该端口的DNS服务器地址
        // 配置 DNS
        builder.addDnsServer(GOOGLE_DNS_FIRST);
        builder.addDnsServer(GOOGLE_DNS_SECOND);
        builder.addDnsServer(CHINA_DNS_FIRST);
        builder.addDnsServer(AMERICA);
        // 就是添加 DNS 域名的自动补齐。DNS服务器必须通过全域名进行搜索，但每次查找都输入全域名太麻烦了，
        // 可以通过配置域名的自动补齐规则予以简化；
        // builder.addSearchDomain(...);
        // 就是你要建立的VPN连接的名字，它将会在系统管理的与 VPN 连接相关的通知栏和对话框中显示出来
        builder.setSession("VPNTest");
        // 这个 intent 指向一个配置页面，用来配置VPN链接。它不是必须的，如果没设置的话，
        // 则系统弹出的 VPN 相关对话框中不会出现配置按钮。
        // builder.setConfigureIntent(...);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.addAllowedApplication("personal.nfl.networkcapture");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("NFL", "VpnTestService's onBind.");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("NFL", "VpnTestService's onCreate.");
        establishVPN();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // VpnTestService.this.run(new InetSocketAddress(VPN_ADDRESS, 0));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void establishVPN() {
        if (vpnInterface == null) {
            // 最后调用 Builder.establish 函数，如果一切正常的话，tun0 虚拟网络接口就建立完成了。并且，
            // 同时还会通过 iptables 命令，修改 NAT 表，将所有数据转发到 tun0 接口上
            vpnInterface = builder.establish();
            Log.i("NFL", null == vpnInterface ? "vpn 建立失败" : "vpn 建立成功");
            if (null != vpnInterface) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FileInputStream fileInputStream = null;
                        fileInputStream = new FileInputStream(vpnInterface.getFileDescriptor());
                        byte[] bytes = new byte[1024];
                        boolean run = true;
                        while (run) {

                            int totalSize = 0;
                            try {
                                totalSize = fileInputStream.read(bytes);
                            } catch (IOException e) {
                                Log.i("NFL", "发生了异常：" + e.getMessage());
                                e.printStackTrace();
                            }
                            Log.i("NFL", "数据的长度" + totalSize + " 开始输出数据：");
                            while (totalSize > 0) {
                                Log.i("NFL", "传输的数据：" + StringUtil.ByteToString(bytes , 0 , totalSize));
                                try {
                                    totalSize = fileInputStream.read(bytes);
                                } catch (IOException e) {
                                    Log.i("NFL", "发生了异常：" + e.getMessage());
                                    e.printStackTrace();
                                }
                            }

                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
            // 这之后，就可以通过读写 VpnService.Builder 返回的 ParcelFileDescriptor 实例来获得设备上所有向外发送的
            // IP 数据包和返回处理过后的 IP 数据包到 TCP/IP 协议栈
            // Packets received need to be written to this output stream.
            // FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("NFL", "VpnTestService's onUnbind.");
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("NFL", "VpnTestService's onStartCommand.");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.i("NFL", "VpnTestService's onRebind.");
    }

    @Override
    public void onRevoke() {
        super.onRevoke();
        Log.i("NFL", "VpnTestService's onRevoke.");
    }

    private boolean run(InetSocketAddress server) throws Exception {
        DatagramChannel tunnel = null;
        boolean connected = false;
        try {
            // Create a DatagramChannel as the VPN tunnel.
            tunnel = DatagramChannel.open();
            // Protect the tunnel before connecting to avoid loopback.
            if (!protect(tunnel.socket())) {
                throw new IllegalStateException("Cannot protect the tunnel");
            }
            // Connect to the server.
            tunnel.connect(server);
            // For simplicity, we use the same thread for both reading and
            // writing. Here we put the tunnel into non-blocking mode.
            tunnel.configureBlocking(false);
            // Authenticate and configure the virtual network interface.
            handshake(tunnel);
            // Now we are connected. Set the flag and show the message.
            connected = true;
            // Packets to be sent are queued in this input stream.
            FileInputStream in = new FileInputStream(mInterface.getFileDescriptor());
            // Packets received need to be written to this output stream.
            FileOutputStream out = new FileOutputStream(mInterface.getFileDescriptor());
            // Allocate the buffer for a single packet.
            ByteBuffer packet = ByteBuffer.allocate(32767);
            // We use a timer to determine the status of the tunnel. It
            // works on both sides. A positive value means sending, and
            // any other means receiving. We start with receiving.
            int timer = 0;
            // We keep forwarding packets till something goes wrong.
            while (true) {
                // Assume that we did not make any progress in this iteration.
                boolean idle = true;
                // Read the outgoing packet from the input stream.
                int length = in.read(packet.array());
                if (length > 0) {
                    // Write the outgoing packet to the tunnel.
                    packet.limit(length);
                    tunnel.write(packet);
                    packet.clear();
                    // There might be more outgoing packets.
                    idle = false;
                    // If we were receiving, switch to sending.
                    if (timer < 1) {
                        timer = 1;
                    }
                }
                // Read the incoming packet from the tunnel.
                length = tunnel.read(packet);
                if (length > 0) {
                    // Ignore control messages, which start with zero.
                    if (packet.get(0) != 0) {
                        // Write the incoming packet to the output stream.
                        out.write(packet.array(), 0, length);
                    }
                    packet.clear();
                    // There might be more incoming packets.
                    idle = false;
                    // If we were sending, switch to receiving.
                    if (timer > 0) {
                        timer = 0;
                    }
                }

                // If we are idle or waiting for the network, sleep for a
                // fraction of time to avoid busy looping.
                if (idle) {
                    Thread.sleep(100);
                    // Increase the timer. This is inaccurate but good enough,
                    // since everything is operated in non-blocking mode.
                    timer += (timer > 0) ? 100 : -100;
                    // We are receiving for a long time but not sending.
                    if (timer < -15000) {
                        // Send empty control messages.
                        packet.put((byte) 0).limit(1);
                        for (int i = 0; i < 3; ++i) {
                            packet.position(0);
                            tunnel.write(packet);
                        }
                        packet.clear();
                        // Switch to sending.
                        timer = 1;
                    }
                    // We are sending for a long time but not receiving.
                    if (timer > 20000) {
                        throw new IllegalStateException("Timed out");
                    }
                }
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Got " + e.toString());
        } finally {
            try {
                tunnel.close();
            } catch (Exception e) {
                // ignore
            }
        }
        return connected;
    }

    private void handshake(DatagramChannel tunnel) throws Exception {
        // To build a secured tunnel, we should perform mutual authentication
        // and exchange session keys for encryption. To keep things simple in
        // this demo, we just send the shared secret in plaintext and wait
        // for the server to send the parameters.

        // Allocate the buffer for handshaking.
        ByteBuffer packet = ByteBuffer.allocate(1024);
        // Control messages always start with zero.
        packet.put((byte) 0).put((byte) 0).flip();
        // Send the secret several times in case of packet loss.
        for (int i = 0; i < 3; ++i) {
            packet.position(0);
            tunnel.write(packet);
        }
        packet.clear();
        // Wait for the parameters within a limited time.
        for (int i = 0; i < 50; ++i) {
            Thread.sleep(100);
            // Normally we should not receive random packets.
            int length = tunnel.read(packet);
            if (length > 0 && packet.get(0) == 0) {
                configure(new String(packet.array(), 1, length - 1).trim());
                return;
            }
        }
        throw new IllegalStateException("Timed out");
    }

    private void configure(String parameters) throws Exception {
        // If the old interface has exactly the same parameters, use it!
        if (mInterface != null) {
            Log.i(TAG, "Using the previous interface");
            return;
        }
        // Configure a builder while parsing the parameters.
        Builder builder = new Builder();
        for (String parameter : parameters.split(" ")) {
            String[] fields = parameter.split(",");
            try {
                switch (fields[0].charAt(0)) {
                    case 'm':
                        builder.setMtu(Short.parseShort(fields[1]));
                        break;
                    case 'a':
                        builder.addAddress(fields[1], Integer.parseInt(fields[2]));
                        break;
                    case 'r':
                        builder.addRoute(fields[1], Integer.parseInt(fields[2]));
                        break;
                    case 'd':
                        builder.addDnsServer(fields[1]);
                        break;
                    case 's':
                        builder.addSearchDomain(fields[1]);
                        break;
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Bad parameter: " + parameter);
            }
        }

        // Close the old interface since the parameters have been changed.
        try {
            mInterface.close();
        } catch (Exception e) {
            // ignore
        }

        // Create a new interface using the builder and save the parameters.
        mInterface = builder.setSession(VPN_ADDRESS)
                .establish();
        mParameters = parameters;
        Log.i(TAG, "New interface: " + parameters);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("NFL", "VpnTestService's onDestroy.");
    }
}
