package personal.nfl.vpn.utils;

public class StringUtil {

    public static String ByteToString(byte[] bytes , int start , int end) {

        StringBuilder strBuilder = new StringBuilder();
        for (int i = start; i < end ; i++) {
            if (bytes[i] != 0) {
                strBuilder.append((char) bytes[i]);
            } else {
                // break;
            }

        }
        return strBuilder.toString();
    }
}
