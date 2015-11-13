package org.wlf.filedownloader.lisener;

import android.os.Handler;
import android.os.Looper;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.FailReason;

/**
 * 文件下载状态改变监听器
 * 
 * @author wlf
 * 
 */
public interface OnFileDownloadStatusListener {

	/**
	 * 等待下载（已添加任务，有别的任务进行，暂时不能安排下载）
	 * 
	 * @param downloadFileInfo
	 *            下载文件信息
	 */
	void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo);

	/**
	 * 正在准备下载（任务已启动，正在做准备工作，如文件检验等工作）
	 * 
	 * @param downloadFileInfo
	 *            下载文件信息
	 */
	void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo);

	/**
	 * 已准备好，可以开始下载（任务正在进行，正在准备做数据处理）
	 * 
	 * @param downloadFileInfo
	 *            下载文件信息
	 */
	void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo);

	/**
	 * 正在下载（任务正在进行，下载数据处理中）
	 * 
	 * @param downloadFileInfo
	 *            下载文件信息
	 * @param downloadSpeed
	 *            下载速度，单位KB/s
	 * @param remainingTime
	 *            剩余时间（根据当前速度计算出来），单位秒
	 */
	void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long remainingTime);

	/**
	 * 已经暂停下载（任务已暂停）
	 * 
	 * @param downloadFileInfo
	 *            下载文件信息
	 */
	void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo);

	/**
	 * 下载完成（下载任务完成，整个文件已经完整的下载完了）
	 * 
	 * @param downloadFileInfo
	 *            下载文件信息
	 */
	void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo);

	/**
	 * 下载出错（下载任务中断，下载过程中出现了各种异常）
	 * 
	 * @param downloadFileInfo
	 *            下载文件信息
	 * @param failReason
	 *            失败原因
	 */
	void onFileDownloadStatusFailed(DownloadFileInfo downloadFileInfo, OnFileDownloadStatusFailReason failReason);

	/** 针对回调到主线程的帮助类 */
	public static class MainThreadHelper {

		/**
		 * 等待下载（已添加任务，有别的任务进行，暂时不能安排下载）
		 * 
		 * @param downloadFileInfo
		 *            下载文件信息
		 */
		public static void onFileDownloadStatusWaiting(final DownloadFileInfo downloadFileInfo,
				final OnFileDownloadStatusListener onFileDownloadStatusListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onFileDownloadStatusListener.onFileDownloadStatusWaiting(downloadFileInfo);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}

		/**
		 * 正在准备下载（任务已启动，正在做准备工作，如文件检验等工作）
		 * 
		 * @param downloadFileInfo
		 *            下载文件信息
		 */
		public static void onFileDownloadStatusPreparing(final DownloadFileInfo downloadFileInfo,
				final OnFileDownloadStatusListener onFileDownloadStatusListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onFileDownloadStatusListener.onFileDownloadStatusPreparing(downloadFileInfo);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}

		/**
		 * 已准备好，可以开始下载（任务正在进行，正在准备做数据处理）
		 * 
		 * @param downloadFileInfo
		 *            下载文件信息
		 */
		public static void onFileDownloadStatusPrepared(final DownloadFileInfo downloadFileInfo,
				final OnFileDownloadStatusListener onFileDownloadStatusListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onFileDownloadStatusListener.onFileDownloadStatusPrepared(downloadFileInfo);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}

		/**
		 * 正在下载（任务正在进行，下载数据处理中）
		 * 
		 * @param downloadFileInfo
		 *            下载文件信息
		 * @param downloadSpeed
		 *            下载速度，单位KB/s
		 * @param remainingTime
		 *            剩余时间（根据当前速度计算出来），单位秒
		 */
		public static void onFileDownloadStatusDownloading(final DownloadFileInfo downloadFileInfo,
				final float downloadSpeed, final long remainingTime,
				final OnFileDownloadStatusListener onFileDownloadStatusListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onFileDownloadStatusListener.onFileDownloadStatusDownloading(downloadFileInfo, downloadSpeed,
							remainingTime);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}

		/**
		 * 已经暂停下载（任务已暂停）
		 * 
		 * @param downloadFileInfo
		 *            下载文件信息
		 */
		public static void onFileDownloadStatusPaused(final DownloadFileInfo downloadFileInfo,
				final OnFileDownloadStatusListener onFileDownloadStatusListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onFileDownloadStatusListener.onFileDownloadStatusPaused(downloadFileInfo);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}

		/**
		 * 下载完成（下载任务完成，整个文件已经完整的下载完了）
		 * 
		 * @param downloadFileInfo
		 *            下载文件信息
		 */
		public static void onFileDownloadStatusCompleted(final DownloadFileInfo downloadFileInfo,
				final OnFileDownloadStatusListener onFileDownloadStatusListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onFileDownloadStatusListener.onFileDownloadStatusCompleted(downloadFileInfo);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}

		/**
		 * 下载出错（下载任务中断，下载过程中出现了各种异常）
		 * 
		 * @param downloadFileInfo
		 *            下载文件信息
		 * @param failReason
		 *            失败原因
		 */
		public static void onFileDownloadStatusFailed(final DownloadFileInfo downloadFileInfo,
				final OnFileDownloadStatusFailReason failReason,
				final OnFileDownloadStatusListener onFileDownloadStatusListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onFileDownloadStatusListener.onFileDownloadStatusFailed(downloadFileInfo, failReason);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}
	}

	/** 文件下载失败原因 */
	public static class OnFileDownloadStatusFailReason extends FailReason {

		private static final long serialVersionUID = -8178297554707996481L;

		/** URL不合法 */
		public static final String TYPE_NETWORK_DENIED = OnFileDownloadStatusFailReason.class.getName()
				+ "_TYPE_NETWORK_DENIED";

		// task中报错
		/** URL不合法 */
		public static final String TYPE_URL_ILLEGAL = OnFileDownloadStatusFailReason.class.getName()
				+ "_TYPE_URL_ILLEGAL";
		/** 文件保存路径不合法 */
		public static final String TYPE_FILE_SAVE_PATH_ILLEGAL = OnFileDownloadStatusFailReason.class.getName()
				+ "_TYPE_FILE_SAVE_PATH_ILLEGAL";
		/** 存储空间不可写，请确保已经挂载 */
		public static final String TYPE_STORAGE_SPACE_CAN_NOT_WRITE = OnFileDownloadStatusFailReason.class.getName()
				+ "_TYPE_STORAGE_SPACE_CAN_NOT_WRITE";
		/** 存储空间不足 */
		public static final String TYPE_STORAGE_SPACE_IS_FULL = OnFileDownloadStatusFailReason.class.getName()
				+ "_TYPE_STORAGE_SPACE_IS_FULL";

		// /** 下载已经存在 */
		// public static final String TYPE_FILE_DOWNLOAD_EXIST =
		// OnFileDownloadStatusFailReason.class.getName()
		// + "_TYPE_FILE_DOWNLOAD_EXIST";
		// /** 无法获取下载资源 */
		// public static final String TYPE_CAN_NOT_ATTACH_DOWNLOAD_FILE =
		// OnFileDownloadStatusFailReason.class.getName()
		// + "_TYPE_CAN_NOT_ATTACH_DOWNLOAD_FILE";

		// file_download_manager中保报错
		/** 资源没有探测 */
		public static final String TYPE_FILE_NOT_DETECT = OnFileDownloadStatusFailReason.class.getName()
				+ "_TYPE_FILE_NOT_DETECT";
		/** 正在下载 */
		public static final String TYPE_FILE_IS_DOWNLOADING = OnFileDownloadStatusFailReason.class.getName()
				+ "_TYPE_FILE_IS_DOWNLOADING";

		public OnFileDownloadStatusFailReason(String detailMessage, String type) {
			super(detailMessage, type);
		}

		public OnFileDownloadStatusFailReason(Throwable throwable) {
			super(throwable);
		}

		@Override
		protected void onInitTypeWithThrowable(Throwable throwable) {
			super.onInitTypeWithThrowable(throwable);
			// TODO Auto-generated constructor stub
		}

	}
}
