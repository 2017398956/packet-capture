package personal.nfl.networkcapture.utils;

public class StringUtil {

    public static String ByteToString(byte[] bytes) {

        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != 0) {
                strBuilder.append((char) bytes[i]);
            } else {
                break;
            }

        }
        return strBuilder.toString();
    }
}
