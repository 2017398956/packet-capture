package personal.nfl.networkcapture.common.util;

public class StringUtil {

    public static String getSocketSize(long size) {
        String showSum;
        if (size > 1000000) {
            showSum = String.valueOf((int) (size / 1000000.0 + 0.5)) + "mb";
        } else if (size > 1000) {
            showSum = String.valueOf((int) (size / 1000.0 + 0.5)) + "kb";
        } else {
            showSum = String.valueOf(size) + "b";
        }
        return showSum;
    }
}
