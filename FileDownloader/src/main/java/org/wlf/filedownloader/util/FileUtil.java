package org.wlf.filedownloader.util;

import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import java.io.File;

/**
 * Util for {@link File}
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class FileUtil {

    /**
     * create parent dir by file path
     *
     * @param filePath file path
     * @return true mean create parent dir succeed
     */
    public static final boolean createFileParentDir(String filePath) {
        File file = new File(filePath);
        if (file != null) {
            if (file.exists()) {
                return true;// parent dir exist
            } else {
                File parentFile = file.getParentFile();
                if (parentFile != null) {
                    if (parentFile.exists()) {
                        return true;// parent dir exist
                    } else {
                        return parentFile.mkdirs();// create parent dir
                    }
                }
            }
        }
        return false;
    }

    /**
     * get file suffix by file path
     *
     * @param filePath file path
     * @return file suffix,return null means failed
     */
    public static String getFileSuffix(String filePath) {
        if (!TextUtils.isEmpty(filePath)) {
            int start = filePath.lastIndexOf(".");
            if (start != -1) {
                return filePath.substring(start + 1);
            }
        }
        return null;
    }

    /**
     * whether the path is file path
     *
     * @param path file path
     * @return true means the path is file path
     */
    public static boolean isFilePath(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        if (path.startsWith(File.separator)) {
            return true;
        }
        return false;
    }

    /**
     * whether the file path can write
     *
     * @param path file path
     * @return true means can write to
     */
    public static final boolean canWrite(String path) {
        // if sdcard,needs the permission:  android.permission.WRITE_EXTERNAL_STORAGE
        if (isSDCardPath(path)) {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                // TODO write bytes for test
                return true;
            }
        } else {
            // TODO write bytes for test
            return true;
        }
        return false;
    }

    /**
     * whether the file path is sdcard path
     *
     * @param path file path
     * @return true means the file path is sdcard path
     */
    public static final boolean isSDCardPath(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        String sdRootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (path.startsWith(sdRootPath)) {
            return true;
        }
        return false;
    }

    /**
     * get available space(free space can use) by fileDirPath
     *
     * @param fileDirPath file dir path
     * @return available space,-1 means failed
     */
    public static long getAvailableSpace(String fileDirPath) {
        try {
            final StatFs stats = new StatFs(fileDirPath);
            long result = (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

}
