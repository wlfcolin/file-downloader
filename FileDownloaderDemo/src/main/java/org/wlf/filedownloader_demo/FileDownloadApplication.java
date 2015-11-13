package org.wlf.filedownloader_demo;

import java.io.File;

import org.wlf.filedownloader.FileDownloadConfiguration;
import org.wlf.filedownloader.FileDownloadConfiguration.Builder;
import org.wlf.filedownloader.FileDownloadManager;

import android.app.Application;
import android.os.Environment;

/** 应用的Application */
public class FileDownloadApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		// 初始化FileDownloadManager
		initFileDownloadManager();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();

		// 释放FileDownloadManager
		releaseFileDownloadManager();
	}

	// 初始化
	private void initFileDownloadManager() {
		// 1、初始化FileDownloadConfiguration.Buider
		Builder config = new FileDownloadConfiguration.Builder(this);
		// 2、配置FileDownloadConfiguration.Buider
		config.configFileDownloadDir(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
				+ "FileDownload");// 配置下载路径
		config.configDownloadTaskSize(2);// 同时下载两个任务
		FileDownloadConfiguration configuration = config.build();// 创建FileDownloadConfiguration
		// 3、初始化FileDownloadManager
		FileDownloadManager.getInstance(this).init(configuration);
	}

	// 释放
	private void releaseFileDownloadManager() {
		// 释放FileDownloadManager资源
		FileDownloadManager.getInstance(this).release();
	}
}
