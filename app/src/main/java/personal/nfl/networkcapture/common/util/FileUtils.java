package personal.nfl.networkcapture.common.util;

import java.io.File;
import java.io.FileFilter;

/**
 * 文件处理工具
 *
 * @author nfl
 */

public class FileUtils {

    /**
     * 删除 file 及其下的所有文件
     *
     * @param file
     * @param fileFilter
     */
    public static void deleteFile(File file, FileFilter fileFilter) {
        if (file == null) {
            return;
        }
        if (!fileFilter.accept(file)) {
            return;
        }
        if (file.isFile()) {
            file.delete();
            return;
        }
        File[] files = file.listFiles();
        if (files == null) {
            file.delete();
            return;
        }
        for (File childFile : files) {
            deleteFile(childFile, fileFilter);
        }
        file.delete();
    }
}
