package org.wlf.filedownloader.lisener;

import android.os.Handler;
import android.os.Looper;

import org.wlf.filedownloader.base.FailReason;

/**
 * 探测网络文件监听器
 * 
 * @author wlf
 * 
 */
public interface OnDetectUrlFileListener {

	/**
	 * 探测到网络文件是新的（本地没有下载记录）
	 * 
	 * @param url
	 *            下载地址
	 * @param fileName
	 *            文件名
	 * @param saveDir
	 *            保存目录
	 * @param fileSize
	 *            文件大小
	 */
	void onDetectNewDownloadFile(String url, String fileName, String saveDir, int fileSize);

	/**
	 * 探测的网络文件已经存在下载列记录（本地有下载记录，可继续/重新/删除下载）
	 * 
	 * @param url
	 *            下载地址
	 */
	void onDetectUrlFileExist(String url);// FIXME 应该回调一个下载文件信息？

	/**
	 * 探测网络文件出错（可能url不正确或者网络暂时无法访问等）
	 * 
	 * @param url
	 *            下载地址
	 * @param failReason
	 *            失败原因
	 */
	void onDetectUrlFileFailed(String url, DetectUrlFileFailReason failReason);

	/** 针对回调到主线程的帮助类 */
	public static class MainThreadHelper {

		/**
		 * 探测到网络文件是新的（本地没有下载记录）
		 * 
		 * @param url
		 *            下载地址
		 * @param fileName
		 *            文件名
		 * @param saveDir
		 *            保存目录
		 * @param fileSize
		 *            文件大小
		 */
		public static void onDetectNewDownloadFile(final String url, final String fileName, final String saveDir,
				final int fileSize, final OnDetectUrlFileListener onDetectUrlFileListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onDetectUrlFileListener.onDetectNewDownloadFile(url, fileName, saveDir, fileSize);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}

		/**
		 * 探测的网络文件已经存在下载列记录（本地有下载记录，可继续/重新/删除下载）
		 * 
		 * @param url
		 *            下载地址
		 */
		public static void onDetectUrlFileExist(final String url, final OnDetectUrlFileListener onDetectUrlFileListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onDetectUrlFileListener.onDetectUrlFileExist(url);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}

		/**
		 * 探测网络文件出错（可能url不正确或者网络暂时无法访问等）
		 * 
		 * @param url
		 *            下载地址
		 * @param failReason
		 *            失败原因
		 */
		public static void onDetectUrlFileFailed(final String url, final DetectUrlFileFailReason failReason,
				final OnDetectUrlFileListener onDetectUrlFileListener) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					onDetectUrlFileListener.onDetectUrlFileFailed(url, failReason);
					// 释放handler对message queue的引用
					handler.removeCallbacksAndMessages(null);
				}
			});
		}

	}

	/** 探测网络文件失败原因 */
	public static class DetectUrlFileFailReason extends FailReason {

		private static final long serialVersionUID = -6863373572721814857L;

		/** URL不合法 */
		public static final String TYPE_URL_ILLEGAL = DetectUrlFileFailReason.class.getName() + "_TYPE_URL_ILLEGAL";
		/** URL重定向超过指定次数错误 */
		public static final String TYPE_URL_OVER_REDIRECT_COUNT = DetectUrlFileFailReason.class.getName()
				+ "_TYPE_URL_OVER_REDIRECT_COUNT";
		/** Http响应错误 */
		public static final String TYPE_BAD_HTTP_RESPONSE_CODE = DetectUrlFileFailReason.class.getName()
				+ "_TYPE_BAD_HTTP_RESPONSE_CODE";
		/** 要下载的文件不存在 */
		public static final String TYPE_HTTP_FILE_NOT_EXIST = DetectUrlFileFailReason.class.getName()
				+ "_TYPE_HTTP_FILE_NOT_EXIST";

		public DetectUrlFileFailReason(String detailMessage, String type) {
			super(detailMessage, type);
		}

		public DetectUrlFileFailReason(Throwable throwable) {
			super(throwable);
		}

		@Override
		protected void onInitTypeWithThrowable(Throwable throwable) {
			super.onInitTypeWithThrowable(throwable);
			// TODO Auto-generated constructor stub
		}

	}
}
