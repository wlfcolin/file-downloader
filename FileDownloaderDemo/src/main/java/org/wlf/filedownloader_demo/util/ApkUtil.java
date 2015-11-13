package org.wlf.filedownloader_demo.util;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Apk文件操作相关工具类
 * 
 * @author wlf
 */
public class ApkUtil {

	/**
	 * 获取未安装apk的包名
	 * 
	 * @param context
	 * @param apkPath
	 *            apk路径
	 * @return apk的包名,null表示获取失败
	 */
	public static String getUnInstallApkPckageName(Context context, String apkPath) {
		PackageManager pm = context.getPackageManager();
		PackageInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
		if (info != null) {
			ApplicationInfo appInfo = info.applicationInfo;
			if (appInfo != null) {
				return appInfo.packageName;
			}
		}
		return null;
	}

	/**
	 * 根据Apk路径安装一个Apk
	 * 
	 * @param context
	 * @param apkPath
	 *            Apk路径
	 */
	public static final void installApk(Context context, String apkPath) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setDataAndType(Uri.fromFile(new File(apkPath)), "application/vnd.android.package-archive");
		context.startActivity(intent);
	}

	/**
	 * 检查App是否已经安装
	 * 
	 * @param context
	 * @param packageName
	 * @return
	 */
	public static boolean checkAppInstalled(Context context, String packageName) {

		if (TextUtils.isEmpty(packageName)) {
			return false;
		}

		try {
			context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_INSTRUMENTATION);
			return true;
		} catch (NameNotFoundException e) {
			return false;
		}
	}

}
