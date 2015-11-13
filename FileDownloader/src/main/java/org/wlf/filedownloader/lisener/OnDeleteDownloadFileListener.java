package org.wlf.filedownloader.lisener;

import android.os.Handler;
import android.os.Looper;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.FailReason;

/**
 * 删除下载文件监听器
 * 
 * @author wlf
 * 
 */
public interface OnDeleteDownloadFileListener {

	/**
	 * 准备删除下载文件
	 * 
	 * @param downloadFileNeedDelete
	 *            需要删除的下载文件
	 */
	void onDeleteDownloadFilePrepared(DownloadFileInfo downloadFileNeedDelete);

	/**
	 * 删除下载文件成功
	 * 
	 * @param downloadFileDeleted
	 *            已删除的下载文件信息
	 */
	void onDeleteDownloadFileSuccess(DownloadFileInfo downloadFileDeleted);

	/**
	 * 删除下载文件失败
	 * 
	 * @param downloadFileInfo
	 *            需要删除的下载文件信息，可能为null
	 * @param failReason
	 *            失败原因
	 */
	void onDeleteDownloadFileFailed(DownloadFileInfo downloadFileInfo, FailReason failReason);

	/** 针对回调到主线程的帮助类 */
	public static class MainThreadHelper {

		/**
		 * 准备删除下载文件
		 * 
		 * @param downloadFileNeedDelete
		 *            需要删除的下载文件
		 */
		public static void onDeleteDownloadFilePrepared(final DownloadFileInfo downloadFileNeedDelete,
				final OnDeleteDownloadFileListener onDeleteDownloadFileListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onDeleteDownloadFileListener.onDeleteDownloadFilePrepared(downloadFileNeedDelete);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}

		/**
		 * 删除下载文件成功
		 * 
		 * @param downloadFileDeleted
		 *            已删除的下载文件信息
		 */
		public static void onDeleteDownloadFileSuccess(final DownloadFileInfo downloadFileDeleted,
				final OnDeleteDownloadFileListener onDeleteDownloadFileListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onDeleteDownloadFileListener.onDeleteDownloadFileSuccess(downloadFileDeleted);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}

		/**
		 * 删除下载文件失败
		 * 
		 * @param downloadFileInfo
		 *            需要删除的下载文件信息，可能为null
		 * @param failReason
		 *            失败原因
		 */
		public static void onDeleteDownloadFileFailed(final DownloadFileInfo downloadFileInfo,
				final FailReason failReason, final OnDeleteDownloadFileListener onDeleteDownloadFileListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onDeleteDownloadFileListener.onDeleteDownloadFileFailed(downloadFileInfo, failReason);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}
	}
}
