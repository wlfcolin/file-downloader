package org.wlf.filedownloader.lisener;

import android.os.Handler;
import android.os.Looper;

import org.wlf.filedownloader.DownloadFileInfo;

import java.util.List;

/**
 * 批量删除下载文件监听器
 * 
 * @author wlf
 * 
 */
public interface OnDeleteDownloadFilesListener {

	/**
	 * 准备批量删除下载文件
	 * 
	 * @param downloadFilesNeedDelete
	 *            需要批量删除的下载文件
	 */
	void onDeleteDownloadFilesPrepared(List<DownloadFileInfo> downloadFilesNeedDelete);

	/**
	 * 正在批量删除下载文件
	 * 
	 * @param downloadFilesNeedDelete
	 *            需要批量删除的下载文件
	 * @param downloadFilesDeleted
	 *            已删除的下载文件
	 * @param downloadFilesSkip
	 *            已跳过删除的下载文件
	 * @param downloadFileDeleting
	 *            正在删除的下载文件
	 */
	void onDeletingDownloadFiles(List<DownloadFileInfo> downloadFilesNeedDelete,
			List<DownloadFileInfo> downloadFilesDeleted, List<DownloadFileInfo> downloadFilesSkip,
			DownloadFileInfo downloadFileDeleting);

	/**
	 * 批量删除下载文件完成
	 * 
	 * @param downloadFilesNeedDelete
	 *            需要批量删除的下载文件信息
	 * @param downloadFilesDeleted
	 *            已成功删除的批量下载文件信息
	 */
	void onDeleteDownloadFilesCompleted(List<DownloadFileInfo> downloadFilesNeedDelete,
			List<DownloadFileInfo> downloadFilesDeleted);

	/** 针对回调到主线程的帮助类 */
	public static class MainThreadHelper {

		/**
		 * 准备批量删除下载文件
		 * 
		 * @param downloadFilesNeedDelete
		 *            需要批量删除的下载文件
		 */
		public static void onDeleteDownloadFilePrepared(final List<DownloadFileInfo> downloadFilesNeedDelete,
				final OnDeleteDownloadFilesListener onDeleteDownloadFilesListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onDeleteDownloadFilesListener.onDeleteDownloadFilesPrepared(downloadFilesNeedDelete);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}

		/**
		 * 正在批量删除下载文件
		 * 
		 * @param downloadFilesNeedDelete
		 *            需要批量删除的下载文件
		 * @param downloadFilesDeleted
		 *            已删除的下载文件
		 * @param downloadFilesSkip
		 *            已跳过删除的下载文件
		 * @param downloadFileDeleting
		 *            正在删除的下载文件
		 */
		public static void onDeletingDownloadFiles(final List<DownloadFileInfo> downloadFilesNeedDelete,
				final List<DownloadFileInfo> downloadFilesDeleted, final List<DownloadFileInfo> downloadFilesSkip,
				final DownloadFileInfo downloadFileDeleting,
				final OnDeleteDownloadFilesListener onDeleteDownloadFilesListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onDeleteDownloadFilesListener.onDeletingDownloadFiles(downloadFilesNeedDelete, downloadFilesDeleted,
							downloadFilesSkip, downloadFileDeleting);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}

		/**
		 * 批量删除下载文件完成
		 * 
		 * @param downloadFilesNeedDelete
		 *            需要批量删除的下载文件信息
		 * @param downloadFilesDeleted
		 *            已成功删除的批量下载文件信息
		 */
		public static void onDeleteDownloadFilesCompleted(final List<DownloadFileInfo> downloadFilesNeedDelete,
				final List<DownloadFileInfo> downloadFilesDeleted,
				final OnDeleteDownloadFilesListener onDeleteDownloadFilesListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onDeleteDownloadFilesListener.onDeleteDownloadFilesCompleted(downloadFilesNeedDelete,
							downloadFilesDeleted);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}
	}

}
