package org.wlf.filedownloader;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

/**
 * 文件下载的配置类
 * 
 * @author wlf
 * 
 */
public class FileDownloadConfiguration {

	/** LOG TAG */
	private static final String TAG = FileDownloadConfiguration.class.getSimpleName();

	/** 下载目录 */
	private String mFileDownloadDir;
	/** 文件下载引擎 */
	private ExecutorService mFileDownloadEngine;
	/** 辅助引擎 */
	private ExecutorService mSupportEngine;

	/**
	 * 创建默认的文件下载的配置文件，推荐{@link Builder#build()}方法创建
	 * 
	 * @param context
	 * 
	 * @return 默认配置的FileDownloadConfiguration实例
	 */
	public static FileDownloadConfiguration createDefault(Context context) {
		return new Builder(context).build();
	}

	/**
	 * 根据{@link Builder}创建定制的下载的配置文件
	 * 
	 * @param builder
	 *            创建器
	 */
	private FileDownloadConfiguration(Builder builder) {
		if (builder == null) {
			throw new NullPointerException("builder不能为空！");
		}
		this.mFileDownloadDir = builder.mFileDownloadDir;
		this.mFileDownloadEngine = Executors.newFixedThreadPool(builder.mDownloadTaskSize);
		this.mSupportEngine = Executors.newSingleThreadExecutor();// 默认单一线程，不可配置
	}

	/**
	 * 获取文件下载目录
	 * 
	 * @return 文件下载目录
	 */
	String getFileDownloadDir() {
		return mFileDownloadDir;
	}

	/** 获取文件下载引擎 */
	ExecutorService getFileDownloadEngine() {
		return mFileDownloadEngine;
	}

	/** 获取辅助引擎 */
	ExecutorService getSupportEngine() {
		return mSupportEngine;
	}

	// ===============文件下载的配置文件创建器===============

	/** 文件下载的配置文件创建器 */
	public static class Builder {

		/** 最大文件下载数量限制，10个 */
		public static final int MAX_DOWNLOAD_TASK_SIZE = 10;
		/** 默认文件下载数量，3个 */
		public static final int DEFAULT_DOWNLOAD_TASK_SIZE = 3;// 默认最大3个下载任务

		private Context mContext;// 上下文
		private String mFileDownloadDir;// 文件下载保存目录
		private int mDownloadTaskSize = -1;// 同时下载的任务数量

		public Builder(Context context) {
			super();
			this.mContext = context.getApplicationContext();
			// 默认SDCard/Android/data/{package_name}/files/file_downloader
			 try {
					mFileDownloadDir = this.mContext.getExternalFilesDir(null).getAbsolutePath() + File.separator
							+ "file_downloader";
	            }catch (Exception e){
	                e.printStackTrace();
	    			mFileDownloadDir = this.mContext.getFilesDir().getAbsolutePath() + File.separator
	    					+ "file_downloader";
	            }
			mDownloadTaskSize = DEFAULT_DOWNLOAD_TASK_SIZE;// 默认任务
		}

		/**
		 * 配置文件下载存在目录
		 * 
		 * @param fileDownloadDir
		 *            文件下载保存目录，若使用SD卡，请加上权限：android.permission.
		 *            WRITE_EXTERNAL_STORAGE
		 * @return 当前Builder
		 */
		public Builder configFileDownloadDir(String fileDownloadDir) {

			if (!TextUtils.isEmpty(fileDownloadDir)) {

				File file = new File(fileDownloadDir);

				// 如果文件夹不存在则创建
				if (!file.exists()) {

					Log.i(TAG, "要设置的文件下载保存目录：" + fileDownloadDir + " 还不存在，需要创建！");

					boolean isCreateSuccess = file.mkdirs();

					if (isCreateSuccess) {
						Log.i(TAG, "要设置的文件下载保存目录：" + fileDownloadDir + " 创建成功！");
					} else {
						Log.w(TAG, "要设置的文件下载保存目录：" + fileDownloadDir + " 创建失败！");
					}

				} else {
					Log.i(TAG, "要设置的文件下载保存目录：" + fileDownloadDir + " 已存在，不需要创建！");
				}

				this.mFileDownloadDir = fileDownloadDir;
			}
			return this;
		}

		/**
		 * 配置同时下载任务的数量
		 * 
		 * @param downloadTaskSize
		 *            同时下载任务的数量，允许设置1~{@link #MAX_DOWNLOAD_TASK_SIZE}，不设置默认为
		 *            {@link #DEFAULT_DOWNLOAD_TASK_SIZE}
		 * @return 当前Builder
		 */
		public Builder configDownloadTaskSize(int downloadTaskSize) {
			if (downloadTaskSize > 0 && downloadTaskSize <= MAX_DOWNLOAD_TASK_SIZE) {
				this.mDownloadTaskSize = downloadTaskSize;
			} else if (downloadTaskSize > MAX_DOWNLOAD_TASK_SIZE) {
				downloadTaskSize = MAX_DOWNLOAD_TASK_SIZE;
			} else {
				Log.w(TAG, "配置同时下载任务的数量失败，downloadTaskSize：" + downloadTaskSize);
			}
			return this;
		}

		/**
		 * 创建FileDownloadConfiguration
		 * 
		 * @return FileDownloadConfiguration实例
		 */
		public FileDownloadConfiguration build() {
			return new FileDownloadConfiguration(this);
		}
	}
}
