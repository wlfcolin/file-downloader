package org.wlf.filedownloader.task;

import java.io.File;
import java.io.InputStream;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.FailReason;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.base.Stoppable;
import org.wlf.filedownloader.db_recoder.DbRecorder;
import org.wlf.filedownloader.file_saver.FileSaver;
import org.wlf.filedownloader.file_saver.FileSaver.FileSaveException;
import org.wlf.filedownloader.file_saver.FileSaver.OnFileSaveLisener;
import org.wlf.filedownloader.http_downlaoder.HttpDownloader;
import org.wlf.filedownloader.http_downlaoder.HttpDownloader.HttpDownloadException;
import org.wlf.filedownloader.http_downlaoder.HttpDownloader.OnHttpDownloadLisener;
import org.wlf.filedownloader.http_downlaoder.Range;
import org.wlf.filedownloader.lisener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.lisener.OnFileDownloadStatusListener.OnFileDownloadStatusFailReason;
import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader.util.UrlUtil;

import android.os.SystemClock;
import android.util.Log;

/**
 * 文件下载任务
 * 
 * @author wlf
 * 
 */
public class FileDownloadTask implements Runnable, Stoppable, OnHttpDownloadLisener, OnFileSaveLisener {

	/** LOG TAG */
	private static final String TAG = FileDownloadTask.class.getSimpleName();

	/** 下载文件任务的参数信息 */
	private FileDownloadTaskParam mTaskInfo;

	// 下载辅助类
	private HttpDownloader mDownloader;// Http下载器
	private FileSaver mSaver;// 文件保存器
	private DbRecorder mRecorder;// 文件状态记录器

	// 文件下载状态监听器
	private OnFileDownloadStatusListener mOnFileDownloadStatusListener;
	// 下载任务停止监听器
	private OnStopFileDownloadTaskLisener mOnStopFileDownloadTaskLisener;

	// 用于计算下载速度
	private long mLastDownloadingTime = -1;

	private boolean mIsNotifyTaskFinish;// 是否已经通知外部任务已经结束

	public FileDownloadTask(FileDownloadTaskParam fileDownloadTaskInfo, DbRecorder recorder) {
		super();
		this.mTaskInfo = fileDownloadTaskInfo;

		// 初始化
		init();

		this.mRecorder = recorder;

		// 检查任务是否可以执行
		if (!checkCanTaskExecute()) {
			return;
		}

		// 等待下载状态
		notifyStatusWatting();
	}

	// 初始化
	private void init() {

		Log.i(TAG, "1、初始化下载任务，url：" + mTaskInfo.mUrl);

		// 初始化文件下载器
		Range range = new Range(mTaskInfo.mStartPosInTotal, mTaskInfo.mFileTotalSize);
		mDownloader = new HttpDownloader(mTaskInfo.mUrl, range, mTaskInfo.mAcceptRangeType, mTaskInfo.mETag);
		mDownloader.setOnHttpDownloadLisener(this);// 让FileDownloadTask监听文件下载过程

		// 初始化下载数据保存器
		mSaver = new FileSaver(mTaskInfo.mUrl, mTaskInfo.mTempFilePath, mTaskInfo.mFilePath, mTaskInfo.mFileTotalSize);
		mSaver.setOnFileSaveLisener(this);// 让FileDownloadTask监听文件保存过程
	}

	public void setOnFileDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
		this.mOnFileDownloadStatusListener = onFileDownloadStatusListener;
	}

	public void setOnStopFileDownloadTaskLisener(OnStopFileDownloadTaskLisener onStopFileDownloadTaskLisener) {
		this.mOnStopFileDownloadTaskLisener = onStopFileDownloadTaskLisener;
	}

	/**
	 * 获取下载文件信息
	 * 
	 * @return
	 */
	private DownloadFileInfo getDownloadFile() {
		if (mRecorder == null) {
			return null;
		}
		return mRecorder.getDownloadFile(mTaskInfo.mUrl);
	}

	/**
	 * 获取URL
	 * 
	 * @return
	 */
	public String getUrl() {
		return mTaskInfo.mUrl;
	}

	// 第一步，启动文件下载
	@Override
	public void run() {

		if (checkIsStop()) {
			return;
		}

		mLastDownloadingTime = -1;

		Log.i(TAG, "2、任务开始执行，正在获取资源，url：：" + mTaskInfo.mUrl);

		boolean canNext = notifyStatusPreparing();

		if (!canNext) {
			return;
		}

		// 2、开始下载
		try {
			mDownloader.download();
		} catch (HttpDownloadException e) {
			e.printStackTrace();
			// error 下载过程中出现错误
			notifyTaskFinish(Status.DOWNLOAD_STATUS_ERROR, 0, new OnFileDownloadStatusFailReason(e));
		} finally {

			if (!mIsNotifyTaskFinish) {
				// 当做暂停处理
				notifyTaskFinish(Status.DOWNLOAD_STATUS_PAUSED, 0, null);
			}

			Log.i(TAG, "7、任务结束执行，url：" + mTaskInfo.mUrl);
		}
	}

	// 第二步，启动文件数据保存
	@Override
	public void onDownloadConnected(InputStream inputStream, int startPosInTotal) {

		if (checkIsStop()) {
			return;
		}

		Log.i(TAG, "3、已经连接资源，url：" + mTaskInfo.mUrl);

		boolean canNext = notifyStatusPrepared();

		if (!canNext) {
			return;
		}

		// 3、保存数据
		try {
			mSaver.saveData(inputStream, startPosInTotal);
		} catch (FileSaveException e) {
			e.printStackTrace();
			// error 回调给外部
			notifyTaskFinish(Status.DOWNLOAD_STATUS_ERROR, 0, new OnFileDownloadStatusFailReason(e));
		}
	}

	// 第三步，启动文件状态记录和回调下载进度给外部
	@Override
	public void onSaveDataStart() {

		if (checkIsStop()) {
			return;
		}

		Log.i(TAG, "4、准备下载，url：" + mTaskInfo.mUrl);

		boolean canNext = notifyStatusDonwloading(0);

		if (!canNext) {
			return;
		}
	}

	@Override
	public void onSavingData(int increaseSize, int totalSize) {

		if (checkIsStop()) {
			return;
		}

		Log.i(TAG, "5、下载中，url：" + mTaskInfo.mUrl);

		boolean canNext = notifyStatusDonwloading(increaseSize);

		if (!canNext) {
			return;
		}
	}

	@Override
	public void onSaveDataEnd(int increaseSize, boolean complete) {

		if (checkIsStop()) {
			return;
		}

		// 6、记录 下载完成（文件已完整保存好）/暂停下载（已暂停下载） 状态

		if (!complete) {

			Log.i(TAG, "6、暂停下载，url：" + mTaskInfo.mUrl);

			notifyTaskFinish(Status.DOWNLOAD_STATUS_PAUSED, increaseSize, null);
		} else {

			Log.i(TAG, "6、下载完成，url：" + mTaskInfo.mUrl);

			notifyTaskFinish(Status.DOWNLOAD_STATUS_COMPLETED, increaseSize, null);
		}
	}

	/**
	 * 通知状态：等待下载（等待其它任务完成） 给外部
	 * 
	 * @return true表示可以继续进入下一个状态，false表示需要结束操作
	 */
	private boolean notifyStatusWatting() {

		try {
			// 0、记录等待下载（等待其它任务完成）状态
			mRecorder.recordStatus(mTaskInfo.mUrl, Status.DOWNLOAD_STATUS_WAITING, 0);
			// 0、等待下载
			if (mOnFileDownloadStatusListener != null) {
				OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusWaiting(getDownloadFile(),
						mOnFileDownloadStatusListener);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			notifyTaskFinish(Status.DOWNLOAD_STATUS_ERROR, 0, new OnFileDownloadStatusFailReason(e));
			return false;
		}
	}

	/**
	 * 通知状态：准备下载（正在获取资源） 给外部
	 * 
	 * @return true表示可以继续进入下一个状态，false表示需要结束操作
	 */
	private boolean notifyStatusPreparing() {

		try {
			// 1、记录准备下载（正在获取资源）状态
			mRecorder.recordStatus(mTaskInfo.mUrl, Status.DOWNLOAD_STATUS_PREPARING, 0);
			// 1、正准备下载
			if (mOnFileDownloadStatusListener != null) {
				OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPreparing(getDownloadFile(),
						mOnFileDownloadStatusListener);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			notifyTaskFinish(Status.DOWNLOAD_STATUS_ERROR, 0, new OnFileDownloadStatusFailReason(e));
			return false;
		}
	}

	/**
	 * 通知状态：开始下载（已连接资源） 给外部
	 * 
	 * @return true表示可以继续进入下一个状态，false表示需要结束操作
	 */
	private boolean notifyStatusPrepared() {

		try {
			// 2、记录开始下载（已连接资源）状态
			mRecorder.recordStatus(mTaskInfo.mUrl, Status.DOWNLOAD_STATUS_PREPARED, 0);

			// 2、已准备好下载
			if (mOnFileDownloadStatusListener != null) {
				OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPrepared(getDownloadFile(),
						mOnFileDownloadStatusListener);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			notifyTaskFinish(Status.DOWNLOAD_STATUS_ERROR, 0, new OnFileDownloadStatusFailReason(e));
			return false;
		}
	}

	/**
	 * 通知状态：正在下载（下载中） 给外部
	 * 
	 * @return true表示可以继续进入下一个状态，false表示需要结束操作
	 */
	private boolean notifyStatusDonwloading(int increaseSize) {

		try {
			// 3、记录正在下载（下载中）
			mRecorder.recordStatus(mTaskInfo.mUrl, Status.DOWNLOAD_STATUS_DOWNLOADING, increaseSize);

			// 3、正在下载，计算下载速度、下载剩余时间
			DownloadFileInfo downloadFileInfo = getDownloadFile();
			if (downloadFileInfo != null) {
				if (mOnFileDownloadStatusListener != null) {
					double downloadSpeed = 0;
					long remainingTime = -1;
					long curDownloadingTime = SystemClock.elapsedRealtime();// 当前时间
					// 计算下载速度
					if (mLastDownloadingTime != -1) {
						double increaseKbs = (double) increaseSize / 1024;
						double increaseSeconds = (curDownloadingTime - mLastDownloadingTime) / (double) 1000;
						downloadSpeed = increaseKbs / increaseSeconds;// 单位KB/s
					}
					// 计算剩余下载时间
					if (downloadSpeed > 0) {
						int remainingSize = downloadFileInfo.getFileSize() - downloadFileInfo.getDownloadedSize();
						if (remainingSize > 0) {
							remainingTime = (long) (((double) remainingSize / 1024) / downloadSpeed);
						}
					}

					mLastDownloadingTime = curDownloadingTime;

					OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusDownloading(downloadFileInfo,
							(float) downloadSpeed, remainingTime, mOnFileDownloadStatusListener);
				}
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();

			// 出错后，为了安全不保存人任何新增数据
			increaseSize = 0;

			notifyTaskFinish(Status.DOWNLOAD_STATUS_ERROR, increaseSize, new OnFileDownloadStatusFailReason(e));
			return false;
		}
	}

	/** 通知外部任务结束了 */
	private void notifyTaskFinish(int status, int increaseSize, OnFileDownloadStatusFailReason failReason) {

		switch (status) {
		// 出现：暂停、完成、出错 都认为该任务已经完成了使命
		case Status.DOWNLOAD_STATUS_PAUSED:
		case Status.DOWNLOAD_STATUS_COMPLETED:
		case Status.DOWNLOAD_STATUS_ERROR:
			if (mIsNotifyTaskFinish) {
				return;
			}
			try {
				mRecorder.recordStatus(mTaskInfo.mUrl, status, increaseSize);
				if (mOnFileDownloadStatusListener != null) {
					switch (status) {
					case Status.DOWNLOAD_STATUS_PAUSED:
						OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPaused(getDownloadFile(),
								mOnFileDownloadStatusListener);
						// 通知停止成功
						notifyStopSucceed();
						break;
					case Status.DOWNLOAD_STATUS_COMPLETED:
						OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusCompleted(getDownloadFile(),
								mOnFileDownloadStatusListener);
						// 通知停止成功
						notifyStopSucceed();
						break;
					case Status.DOWNLOAD_STATUS_ERROR:
						OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(getDownloadFile(),
								failReason, mOnFileDownloadStatusListener);
						// 通知停止失败
						notifyStopFailed(new OnStopDownloadFileTaskFailReason(failReason));
						break;
					}
				}
				mIsNotifyTaskFinish = true;
			} catch (Exception e) {
				e.printStackTrace();
				// 保存记录出错
				OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(getDownloadFile(),
						new OnFileDownloadStatusFailReason(e), mOnFileDownloadStatusListener);
				mIsNotifyTaskFinish = true;
			} finally {
				// 若已经通知外部了
				if (mIsNotifyTaskFinish) {
					// 强行停止任务
					stop();
				}
			}
			break;
		}
	}

	/** 通知停止成功 */
	private void notifyStopSucceed() {
		if (mOnStopFileDownloadTaskLisener != null) {
			mOnStopFileDownloadTaskLisener.onStopFileDownloadTaskSucceed(mTaskInfo.mUrl);
		}
	}

	/** 通知停止失败（任务已经停止，或者处理数据过程中遇到异常导致停止） */
	private void notifyStopFailed(OnStopDownloadFileTaskFailReason failReason) {
		if (mOnStopFileDownloadTaskLisener != null) {
			mOnStopFileDownloadTaskLisener.onStopFileDownloadTaskFailed(mTaskInfo.mUrl, failReason);
		}
	}

	/**
	 * 检查是否已经停止
	 * 
	 * @return true表示已经停止
	 */
	private boolean checkIsStop() {
		if (isStopped()) {
			if (!mIsNotifyTaskFinish) {
				notifyTaskFinish(Status.DOWNLOAD_STATUS_PAUSED, 0, null);
			}
		}
		return isStopped();
	}

	/**
	 * 检查任务是否可以执行
	 * 
	 * @return true表示可以执行
	 */
	private boolean checkCanTaskExecute() {

		OnFileDownloadStatusFailReason failReason = null;

		if (mTaskInfo == null) {
			// error 参数不合法
			failReason = new OnFileDownloadStatusFailReason("初始化参数为空！",
					OnFileDownloadStatusFailReason.TYPE_NULL_POINTER);
		}
		if (failReason == null && !UrlUtil.isUrl(mTaskInfo.mUrl)) {
			// error url不合法
			failReason = new OnFileDownloadStatusFailReason("Url不合法！", OnFileDownloadStatusFailReason.TYPE_URL_ILLEGAL);
		}
		if (failReason == null && !FileUtil.isFilePath(mTaskInfo.mFilePath)) {
			// error saveDir不合法
			failReason = new OnFileDownloadStatusFailReason("文件保存路径不合法！",
					OnFileDownloadStatusFailReason.TYPE_FILE_SAVE_PATH_ILLEGAL);
		}
		if (failReason == null
				&& (!FileUtil.canWrite(mTaskInfo.mTempFilePath) || !FileUtil.canWrite(mTaskInfo.mFilePath))) {
			// error 不能写
			failReason = new OnFileDownloadStatusFailReason("SD存储空间不可写，请确保已经挂载！",
					OnFileDownloadStatusFailReason.TYPE_STORAGE_SPACE_CAN_NOT_WRITE);
		}
		if (failReason == null) {
			try {
				String checkPath;
				File file = new File(mTaskInfo.mFilePath);
				if (!file.exists()) {
					checkPath = file.getParentFile().getAbsolutePath();
				} else {
					checkPath = mTaskInfo.mFilePath;
				}
				long freeSize = FileUtil.getAvailableSpace(checkPath);
				long needDownloadSize = mTaskInfo.mFileTotalSize - mTaskInfo.mStartPosInTotal;
				if (freeSize == -1 || needDownloadSize > freeSize) {
					// error 空间不足
					failReason = new OnFileDownloadStatusFailReason("存储空间不足！",
							OnFileDownloadStatusFailReason.TYPE_STORAGE_SPACE_IS_FULL);
				}
			} catch (Exception e) {
				failReason = new OnFileDownloadStatusFailReason(e);
				e.printStackTrace();
			}
		}

		if (failReason != null) {
			if (!mIsNotifyTaskFinish) {
				notifyTaskFinish(Status.DOWNLOAD_STATUS_PAUSED, 0, failReason);
			}
			return false;
		}

		return true;
	}

	/**
	 * 下载任务是否已经停止
	 */
	@Override
	public boolean isStopped() {
		// 目前只关心Saver是否停止处理
		if (mSaver != null) {
			return mSaver.isStopped();
		}
		return true;
	}

	/** 停止下载任务 */
	@Override
	public void stop() {
		// 已经停止了
		if (isStopped()) {
			notifyStopFailed(new OnStopDownloadFileTaskFailReason("已经停止了！",
					OnStopDownloadFileTaskFailReason.TYPE_IS_STOP));
			return;
		}
		if (mSaver != null) {
			// 目前只关心Saver的停止
			mSaver.stop();
		}
	}

	/** 下载文件任务的参数信息 */
	public static class FileDownloadTaskParam {
		/** 下载路径 */
		private String mUrl;
		/** 需要从什么位置开始下载 */
		private int mStartPosInTotal;
		/** 文件总大小 */
		private int mFileTotalSize;
		/** 文件对应的eTag */
		private String mETag;
		/** 接收的分段下载范围类型 */
		private String mAcceptRangeType;
		/** 临时文件保存路径 */
		private String mTempFilePath;
		/** 文件保存路径 */
		private String mFilePath;

		public FileDownloadTaskParam(String url, int startPosInTotal, int fileTotalSize, String eTag,
				String acceptRangeType, String tempFilePath, String filePath) {
			super();
			this.mUrl = url;
			this.mStartPosInTotal = startPosInTotal;
			this.mFileTotalSize = fileTotalSize;
			this.mETag = eTag;
			this.mAcceptRangeType = acceptRangeType;
			this.mTempFilePath = tempFilePath;
			this.mFilePath = filePath;
		}
	}

	/** 停止下载任务监听器 */
	public interface OnStopFileDownloadTaskLisener {

		/**
		 * 停止下载任务成功（暂停下载）
		 * 
		 * @param url
		 */
		void onStopFileDownloadTaskSucceed(String url);

		/**
		 * 停止下载任务失败（暂停下载），有可能已经停止了，也有可能是异常导致失败
		 * 
		 * @param url
		 * @param failReason
		 */
		void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason);
	}

	public static class OnStopDownloadFileTaskFailReason extends FailReason {

		private static final long serialVersionUID = 6959079784746889291L;

		/** 已经停止了 */
		public static final String TYPE_IS_STOP = OnStopDownloadFileTaskFailReason.class.getName() + "_TYPE_IS_STOP";

		public OnStopDownloadFileTaskFailReason(String detailMessage, String type) {
			super(detailMessage, type);
		}

		public OnStopDownloadFileTaskFailReason(Throwable throwable) {
			super(throwable);
		}

		@Override
		protected void onInitTypeWithThrowable(Throwable throwable) {
			super.onInitTypeWithThrowable(throwable);
			// TODO Auto-generated constructor stub
		}
	}

}
