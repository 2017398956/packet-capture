package personal.nfl.vpn.http;

import android.text.TextUtils;

import java.util.Locale;

import personal.nfl.vpn.nat.NatSession;
import personal.nfl.vpn.utils.AppDebug;
import personal.nfl.vpn.utils.CommonMethods;
import personal.nfl.vpn.utils.DebugLog;

/**
 * Created by zengzheying on 15/12/30.
 */
public class HttpRequestHeaderParser {

    public static void parseHttpRequestHeader(NatSession session, byte[] buffer, int offset, int count) {
        try {
            switch (buffer[offset]) {

                case 'G': //GET
                case 'H': //HEAD
                case 'P': //POST, PUT
                case 'D': //DELETE
                case 'O': //OPTIONS
                case 'T': //TRACE
                case 'C': //CONNECT
                    getHttpHostAndRequestUrl(session, buffer, offset, count);
                    break;
                //SSL
                case 0x16:
                    session.remoteHost = getSNI(session, buffer, offset, count);
                    session.isHttpsSession = true;
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            if (AppDebug.IS_DEBUG) {
                ex.printStackTrace(System.err);
            }
            DebugLog.e("Error: parseHost: %s", ex);
        }
    }

    public static void getHttpHostAndRequestUrl(NatSession session, byte[] buffer, int offset, int count) {
        session.isHttp = true;
        session.isHttpsSession = false;
        String headerString = new String(buffer, offset, count);
        // Log.i("NFL" , "用户传送的网络数据：" + headerString) ;
        String[] headerLines = headerString.split("\\r\\n");
        String host = getHttpHost(headerLines);
        if (!TextUtils.isEmpty(host)) {
            session.remoteHost = host;
        }
        paresRequestLine(session, headerLines[0]);
    }

    /**
     * @param buffer
     * @param offset
     * @param count
     * @return 网络访问 host 但不包括协议和端口
     */
    public static String getRemoteHost(byte[] buffer, int offset, int count) {
        String headerString = new String(buffer, offset, count);
        String[] headerLines = headerString.split("\\r\\n");
        return getHttpHost(headerLines);
    }

    /**
     * @param headerLines
     * @return ip 报文中的 host 地址，但不包括端口
     */
    public static String getHttpHost(String[] headerLines) {
        for (int i = 1; i < headerLines.length; i++) {
            String[] nameValueStrings = headerLines[i].split(":");
            // IP 报文中 host 如果不是 80 端口会有 2 个 “：”，例如 Host: 192.168.100.103:901
            if (nameValueStrings.length == 2 || nameValueStrings.length == 3) {
                String name = nameValueStrings[0].toLowerCase(Locale.ENGLISH).trim();
                String value = nameValueStrings[1].trim();
                if ("host".equals(name)) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * 解析网络请求地址，但 session.requestUrl 中不包含端口信息
     *
     * @param session
     * @param requestLine
     */
    public static void paresRequestLine(NatSession session, String requestLine) {
        String[] parts = requestLine.trim().split(" ");
        if (parts.length == 3) {
            // 网络访问方式 GET，POST 等
            session.method = parts[0];
            // url 中带的参数，例如：访问的是 http://192.168.100.103:901/?a=2 那么 url 就是 /?a=2
            String url = parts[1];
            session.pathUrl = url;
            if (url.startsWith("/")) {
                if (session.remoteHost != null) {
                    session.requestUrl = "http://" + session.remoteHost + url;
                }
            } else {
                if (session.requestUrl.startsWith("http")) {
                    session.requestUrl = url;
                } else {
                    session.requestUrl = "http://" + url;
                }
            }
        }
    }

    public static String getSNI(NatSession session, byte[] buffer, int offset, int count) {
        int limit = offset + count;
        //TLS Client Hello
        if (count > 43 && buffer[offset] == 0x16) {
            //Skip 43 byte header
            offset += 43;

            //read sessionID
            if (offset + 1 > limit) {
                return null;
            }
            int sessionIDLength = buffer[offset++] & 0xFF;
            offset += sessionIDLength;

            //read cipher suites
            if (offset + 2 > limit) {
                return null;
            }

            int cipherSuitesLength = CommonMethods.readShort(buffer, offset) & 0xFFFF;
            offset += 2;
            offset += cipherSuitesLength;

            //read Compression method.
            if (offset + 1 > limit) {
                return null;
            }
            int compressionMethodLength = buffer[offset++] & 0xFF;
            offset += compressionMethodLength;
            if (offset == limit) {
                DebugLog.w("TLS Client Hello packet doesn't contains SNI info.(offset == limit)");
                return null;
            }

            //read Extensions
            if (offset + 2 > limit) {
                return null;
            }
            int extensionsLength = CommonMethods.readShort(buffer, offset) & 0xFFFF;
            offset += 2;

            if (offset + extensionsLength > limit) {
                DebugLog.w("TLS Client Hello packet is incomplete.");
                return null;
            }

            while (offset + 4 <= limit) {
                int type0 = buffer[offset++] & 0xFF;
                int type1 = buffer[offset++] & 0xFF;
                int length = CommonMethods.readShort(buffer, offset) & 0xFFFF;
                offset += 2;
                //have SNI
                if (type0 == 0x00 && type1 == 0x00 && length > 5) {
                    offset += 5;
                    length -= 5;
                    if (offset + length > limit) {
                        return null;
                    }
                    String serverName = new String(buffer, offset, length);
                    DebugLog.i("SNI: %s\n", serverName);
                    session.isHttpsSession = true;
                    return serverName;
                } else {
                    offset += length;
                }

            }
            DebugLog.e("TLS Client Hello packet doesn't contains Host field info.");
            return null;
        } else {
            DebugLog.e("Bad TLS Client Hello packet.");
            return null;
        }
    }


}
