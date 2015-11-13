package org.wlf.filedownloader.lisener;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.FailReason;

import android.os.Handler;
import android.os.Looper;

/**
 * 移动下载文件监听接口
 * 
 * @author wlf
 * 
 */
public interface OnMoveDownloadFileListener {

	/**
	 * 准备移动下载文件
	 * 
	 * @param downloadFileNeedToMove
	 *            需要移动的下载文件
	 */
	void onMoveDownloadFilePrepared(DownloadFileInfo downloadFileNeedToMove);

	/**
	 * 移动下载文件成功
	 * 
	 * @param downloadFileMoved
	 *            已移动到新位置的下载文件信息
	 */
	void onMoveDownloadFileSuccess(DownloadFileInfo downloadFileMoved);

	/**
	 * 移动下载文件失败
	 * 
	 * @param downloadFileInfo
	 *            下载文件信息
	 * @param failReason
	 *            失败原因
	 */
	void onMoveDownloadFileFailed(DownloadFileInfo downloadFileInfo, OnMoveDownloadFileFailReason failReason);

	/** 文件下载失败原因 */
	public static class OnMoveDownloadFileFailReason extends FailReason {

		private static final long serialVersionUID = -5988401984979760118L;

		/** 目标文件已存在 */
		public static final String TYPE_TARGET_FILE_EXIST = OnMoveDownloadFileFailReason.class.getName()
				+ "_TYPE_TARGET_FILE_EXIST";
		/** 原始文件不存在 */
		public static final String TYPE_ORIGINAL_FILE_NOT_EXIST = OnMoveDownloadFileFailReason.class.getName()
				+ "_TYPE_ORIGINAL_FILE_NOT_EXIST";

		public OnMoveDownloadFileFailReason(String detailMessage, String type) {
			super(detailMessage, type);
		}

		public OnMoveDownloadFileFailReason(Throwable throwable) {
			super(throwable);
		}

		@Override
		protected void onInitTypeWithThrowable(Throwable throwable) {
			super.onInitTypeWithThrowable(throwable);
			// TODO Auto-generated constructor stub
		}

	}

	/** 针对回调到主线程的帮助类 */
	public static class MainThreadHelper {

		/**
		 * 准备移动下载文件
		 * 
		 * @param downloadFileNeedToMove
		 *            需要移动的下载文件
		 */
		public static void onMoveDownloadFilePrepared(final DownloadFileInfo downloadFileMoved,
				final OnMoveDownloadFileListener onMoveDownloadFileListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onMoveDownloadFileListener.onMoveDownloadFilePrepared(downloadFileMoved);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}

		/**
		 * 移动下载文件成功
		 * 
		 * @param downloadFileMoved
		 *            已移动到新位置的下载文件信息
		 */
		public static void onMoveDownloadFileSuccess(final DownloadFileInfo downloadFileMoved,
				final OnMoveDownloadFileListener onMoveDownloadFileListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onMoveDownloadFileListener.onMoveDownloadFileSuccess(downloadFileMoved);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}

		/**
		 * 移动下载文件失败
		 * 
		 * @param downloadFileInfo
		 *            下载文件信息
		 * @param failReason
		 *            失败原因
		 */
		public static void onMoveDownloadFileFailed(final DownloadFileInfo downloadFileInfo,
				final OnMoveDownloadFileFailReason failReason, final OnMoveDownloadFileListener onMoveDownloadFileListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onMoveDownloadFileListener.onMoveDownloadFileFailed(downloadFileInfo, failReason);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}
	}
}