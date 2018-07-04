package personal.nfl.vpn.nat;

import personal.nfl.vpn.utils.CommonMethods;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author nfl.
 * NAT管理对象
 */
public class NatSessionManager {

    /**
     * 会话保存的最大个数
     */
    static final int MAX_SESSION_COUNT = 64;
    /**
     * 会话保存时间
     */
    private static final long SESSION_TIME_OUT_NS = 60 * 1000L;
    /**
     * Short 是 {@link NatSession#localPort} 源端口
     */
    private static final ConcurrentHashMap<Short, NatSession> sessions = new ConcurrentHashMap<>();

    /**
     * 通过本地端口获取会话信息
     *
     * @param portKey 本地端口
     * @return 会话信息
     */
    public static NatSession getSession(short portKey) {
        return sessions.get(portKey);
    }

    /**
     * 获取会话个数
     *
     * @return 会话个数
     */
    public static int getSessionCount() {
        return sessions.size();
    }

    /**
     * 清除过期的会话
     */
    static void clearExpiredSessions() {
        long now = System.currentTimeMillis();
        Set<Map.Entry<Short, NatSession>> entries = sessions.entrySet();
        Iterator<Map.Entry<Short, NatSession>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<Short, NatSession> next = iterator.next();
            if (now - next.getValue().lastRefreshTime > SESSION_TIME_OUT_NS) {
                iterator.remove();
            }
        }
    }

    public static void clearAllSession() {
        sessions.clear();
    }

    public static List<NatSession> getAllSession() {
        ArrayList<NatSession> natSessions = new ArrayList<>();
        Set<Map.Entry<Short, NatSession>> entries = sessions.entrySet();
        Iterator<Map.Entry<Short, NatSession>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<Short, NatSession> next = iterator.next();
            natSessions.add(next.getValue());
        }
        return natSessions;
    }

    /**
     * 创建会话
     *
     * @param portKey    源端口
     * @param remoteIP   远程ip
     * @param remotePort 远程端口
     * @return NatSession对象
     */
    public static NatSession createSession(short portKey, int remoteIP, short remotePort, String type) {
        if (sessions.size() > MAX_SESSION_COUNT) {
            clearExpiredSessions(); //清除过期的会话
        }

        NatSession session = new NatSession();
        // session.lastRefreshTime = System.currentTimeMillis();
        session.lastRefreshTime = System.currentTimeMillis();
        session.remoteIP = remoteIP;
        session.remotePort = remotePort;
        session.localPort = portKey;
        // 如果没有目的 ip ，则将数字形式的 ip 转化为 192.168.0.1 的形式
        if (session.remoteHost == null) {
            session.remoteHost = CommonMethods.ipIntToString(remoteIP);
        }
        session.type = type;
        session.refreshIpAndPort();
        sessions.put(portKey, session);
        return session;
    }

    public static void removeSession(short portKey) {
        sessions.remove(portKey);
    }
}
