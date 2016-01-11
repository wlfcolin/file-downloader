package org.wlf.common;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 * LogHelper
 *
 * @author wlf(Andy)
 * @datetime 2015-11-17 09:50 GMT+8
 * @email 411086563@qq.com
 */
public class LogHelper {

    // the log file size can not bigger than 100M
    public static final int NEED_DELETE_SIZE = 1024 * 1024 * 100;
    // the log file dir free size can not smaller than (total size * 0.1f)
    public static final float NEED_DELETE_FREE_RATE = 0.1f;
    // every time to delete log size, (total log size * 0.1f)
    public static final float DELETE_RATE = 0.1f;

    /**
     * check the log file dir size, and make sure the dir size are under control
     *
     * @param dir log dir
     */
    public static void checkLogFileDirSize(File dir) {
        if (dir == null || dir.exists() || dir.listFiles().length == 0) {
            return;
        }
        // delete some logs that are out of date if needed
        if (getFileDirSize(dir) > NEED_DELETE_SIZE || dir.getFreeSpace() < dir.getTotalSpace() * 
                NEED_DELETE_FREE_RATE) {
            File[] files = dir.listFiles();
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File lhs, File rhs) {
                    // sort by inverted last modify time
                    if (lhs.lastModified() > rhs.lastModified()) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            });
            int length = files.length;
            int deleteIndex = (int) (length * DELETE_RATE);
            for (int i = 0; i < deleteIndex; i++) {
                files[i].delete();
            }
        }
    }

    /**
     * get file dir size
     *
     * @param fileDir file dir
     * @return file dir size, -1 means failed
     */
    private static long getFileDirSize(File fileDir) {
        try {
            if (fileDir == null || !fileDir.exists()) {
                return -1;
            }
            if (fileDir.isFile()) {
                return fileDir.length();
            }

            long size = 0;
            File[] files = fileDir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    size += getFileDirSize(file);
                } else {
                    size += file.length();
                }
            }
            return size;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
