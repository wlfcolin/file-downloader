package org.wlf.filedownloader.util;

import java.io.File;

import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

/**
 * 文件工具类
 * 
 * @author wlf
 * 
 */
public class FileUtil {

	/**
	 * 根据文件路径创建父文件夹
	 * 
	 * @param filePath
	 *            文件路径
	 * @return true表示创建父文件夹成功
	 */
	public static final boolean createFileParentDir(String filePath) {
		File file = new File(filePath);
		if (file != null) {
			if (file.exists()) {
				return true;// 肯定有ParentDir
			} else {
				File parentFile = file.getParentFile();
				if (parentFile != null) {
					if (parentFile.exists()) {
						return true;// ParentDir已存在
					} else {
						return parentFile.mkdirs();// 创建ParentDir
					}
				}
			}
		}
		return false;
	}

	/**
	 * 根据文件名获取后缀名
	 * 
	 * @param filePath
	 *            文件全名或者文件路径
	 * @return 文件后缀名，返回null表示获取失败
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
	 * 判断是否是文件路径
	 * 
	 * @param path
	 * @return true则说明是文件路径，否则不是
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
	 * 是否可以写文件
	 * 
	 * @param path
	 * @return true表示该路径可以写文件，否则不可写
	 */
	public static final boolean canWrite(String path) {
		// SD卡，需要挂载和权限
		if (isSDCardPath(path)) {
			if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				// TODO 写一个文件测试
				return true;
			}
		} else {
			// TODO 写一个文件测试
			return true;
		}
		return false;
	}

	/**
	 * 是否问SD卡路径
	 * 
	 * @param path
	 * @return
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
	 * 获取某一指定文件路径下的可用空间大小
	 * 
	 * @param fileDirPath
	 * @return 可用空间大小，返回-1表示获取失败
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
