package org.wlf.filedownloader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.wlf.filedownloader.DownloadFileCacher.DownloadStatusRecordException;
import org.wlf.filedownloader.base.FailReason;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.lisener.OnDeleteDownloadFileListener;
import org.wlf.filedownloader.lisener.OnDeleteDownloadFilesListener;
import org.wlf.filedownloader.lisener.OnDetectUrlFileListener;
import org.wlf.filedownloader.lisener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.lisener.OnFileDownloadStatusListener.OnFileDownloadStatusFailReason;
import org.wlf.filedownloader.lisener.OnMoveDownloadFileListener;
import org.wlf.filedownloader.lisener.OnMoveDownloadFileListener.OnMoveDownloadFileFailReason;
import org.wlf.filedownloader.lisener.OnMoveDownloadFilesListener;
import org.wlf.filedownloader.lisener.OnRenameDownloadFileListener;
import org.wlf.filedownloader.task.DeleteDownloadFileTask;
import org.wlf.filedownloader.task.DetectUrlFileTask;
import org.wlf.filedownloader.task.FileDownloadTask;
import org.wlf.filedownloader.task.FileDownloadTask.OnStopDownloadFileTaskFailReason;
import org.wlf.filedownloader.task.FileDownloadTask.OnStopFileDownloadTaskLisener;
import org.wlf.filedownloader.task.FileDownloadTaskParamHelper;
import org.wlf.filedownloader.task.MoveDownloadFileTask;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

/**
 * 文件下载管理器
 * 
 * @author wlf
 * 
 */
public class FileDownloadManager {

	/** LOG TAG */
	private static final String TAG = FileDownloadManager.class.getSimpleName();

	/** 单一实例 */
	private static FileDownloadManager sInstance;

	/** 配置文件 */
	private FileDownloadConfiguration mConfiguration;

	/** 探测文件缓存器 */
	private DetectUrlFileCacher mDetectUrlFileCacher;
	/** 下载文件缓存器 */
	private DownloadFileCacher mDownloadFileCacher;

	/** 所有下载任务信息（包括：等待下载、准备下载、正在下载 状态的所有任务） */
	private Map<String, FileDownloadTask> mFileDownloadTaskMap = new ConcurrentHashMap<String, FileDownloadTask>();
	/** 批量删除下载文件任务 */
	private DeleteDownloadFilesTask mDeleteDownloadFilesTask = null;
	/** 批量移动下载文件任务 */
	private MoveDownloadFilesTask mMoveDownloadFilesTask = null;

	/** 初始化锁 */
	private Object mInitLock = new Object();

	// 私有化构造方法
	private FileDownloadManager(Context context) {
		Context appContext = context.getApplicationContext();
		// 初始化探测文件缓存器
		mDetectUrlFileCacher = new DetectUrlFileCacher();
		// 初始化下载文件缓存器
		mDownloadFileCacher = new DownloadFileCacher(appContext);
		// 异常退出状态恢复
		exceptionStateRecovery(appContext, getDownloadFiles());
	}

	/**
	 * 获取单一实例
	 * 
	 * @param context
	 *            上下文环境
	 * @return 单一实例
	 */
	public static FileDownloadManager getInstance(Context context) {
		if (sInstance == null) {
			synchronized (FileDownloadManager.class) {
				if (sInstance == null) {
					sInstance = new FileDownloadManager(context);
				}
			}
		}
		return sInstance;
	}

	/**
	 * 初始化配置
	 * 
	 * @param configuration
	 *            文件下载配置
	 */
	public void init(FileDownloadConfiguration configuration) {
		synchronized (mInitLock) {
			this.mConfiguration = configuration;
		}
	}

	/**
	 * 是否初始化
	 * 
	 * @return true表示已经出初始化
	 */
	public boolean isInit() {
		synchronized (mInitLock) {
			return mConfiguration != null;
		}
	}

	/** 检查初始化状态 */
	private void checkInit() {
		if (!isInit()) {
			throw new IllegalStateException("请先初始化" + FileDownloadManager.class.getSimpleName());
		}
	}

	/** 异常退出恢复 */
	private void exceptionStateRecovery(Context context, List<DownloadFileInfo> downloadFileInfos) {

		Log.d(TAG, "异常恢复检查！");

		if (downloadFileInfos == null || downloadFileInfos.isEmpty()) {
			return;
		}

		for (DownloadFileInfo downloadFileInfo : downloadFileInfos) {
			if (downloadFileInfo == null) {
				continue;
			}

			String url = downloadFileInfo.getUrl();

			// 若在任务列表中，不需要检查还原状态
			if (isInFileDownloadTaskMap(url)) {
				continue;
			}
			// 检查还原状态
			switch (downloadFileInfo.getStatus()) {
			case Status.DOWNLOAD_STATUS_WAITING:
			case Status.DOWNLOAD_STATUS_PREPARING:
			case Status.DOWNLOAD_STATUS_PREPARED:
			case Status.DOWNLOAD_STATUS_DOWNLOADING:
				// 还原状态
				try {
					mDownloadFileCacher.recordStatus(url, Status.DOWNLOAD_STATUS_PAUSED, 0);
				} catch (DownloadStatusRecordException e) {
					e.printStackTrace();
				}
				break;
			case Status.DOWNLOAD_STATUS_COMPLETED:
			case Status.DOWNLOAD_STATUS_PAUSED:
			case Status.DOWNLOAD_STATUS_ERROR:
				// 这三种状态不处理
				break;
			case Status.DOWNLOAD_STATUS_UNKNOWN:
			default:
				// 还原状态
				try {
					mDownloadFileCacher.recordStatus(url, Status.DOWNLOAD_STATUS_ERROR, 0);
				} catch (DownloadStatusRecordException e) {
					e.printStackTrace();
				}
				break;
			}
		}
	}

	/**
	 * 是否存在下载列任务列表中
	 * 
	 * @param url
	 * @return true表示在下载列表中，false表示不在
	 */
	private boolean isInFileDownloadTaskMap(String url) {
		return mFileDownloadTaskMap.containsKey(url);
	}

	/**
	 * 获取正在任务列表中的任务
	 * 
	 * @param url
	 * @return
	 */
	private FileDownloadTask getFileDownloadTask(String url) {
		return mFileDownloadTaskMap.get(url);
	}

	/**
	 * 根据url获取缓存的探测文件
	 * 
	 * @param url
	 * @return
	 */
	private DetectUrlFileInfo getDetectUrlFile(String url) {
		return mDetectUrlFileCacher.getDetectUrlFile(url);
	}

	/**
	 * 根据url获取下载信息（私有）
	 * 
	 * @param url
	 * @return 下载信息
	 */
	private DownloadFileInfo getDownloadFile(String url) {
		return mDownloadFileCacher.getDownloadFile(url);
	}

	/**
	 * 根据下载地址获取下载文件信息
	 * 
	 * @param url
	 *            下载地址
	 * @return 下载文件信息
	 */
	public DownloadFileInfo getDownloadFileByUrl(String url) {
		return getDownloadFile(url);
	}

	/**
	 * 根据保存路径获取下载文件信息
	 * 
	 * @param savePath
	 *            文件保存路径
	 * @return 下载文件信息
	 */
	public DownloadFileInfo getDownloadFileBySavePath(String savePath) {
		return mDownloadFileCacher.getDownloadFileBySavePath(savePath);
	}

	/**
	 * 获取所有下载文件信息
	 * 
	 * @return 所有下载文件信息
	 */
	public List<DownloadFileInfo> getDownloadFiles() {
		return mDownloadFileCacher.getDownloadFiles();
	}

	/** 开始下载任务 */
	private void addAndRunFileDonwlaodTask(DownloadFileInfo downloadFileInfo,
			OnFileDownloadStatusListener onFileDownloadStatusListener) {
		// 创建
		FileDownloadTask fileDownloadTask = new FileDownloadTask(
				FileDownloadTaskParamHelper.createByDownloadFile(downloadFileInfo), mDownloadFileCacher);
		fileDownloadTask.setOnFileDownloadStatusListener(onFileDownloadStatusListener);
		// 记录
		mFileDownloadTaskMap.put(fileDownloadTask.getUrl(), fileDownloadTask);
		// 开始执行下载任务
		mConfiguration.getFileDownloadEngine().execute(fileDownloadTask);
	}

	/** 记录下载失失败 */
	private void notifyFileDownloadStatusFailed(final DownloadFileInfo downloadFileInfo,
			final OnFileDownloadStatusFailReason failReason,
			final OnFileDownloadStatusListener onFileDownloadStatusListener, boolean recordStatus) {
		// 空处理
		if (downloadFileInfo == null) {
			// 回调给外部
			if (onFileDownloadStatusListener != null) {
				OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(downloadFileInfo,
						new OnFileDownloadStatusFailReason(failReason), onFileDownloadStatusListener);
			}
			return;
		}
		String url = downloadFileInfo.getUrl();
		if (recordStatus && isInFileDownloadTaskMap(url)) {
			// 先暂停
			FileDownloadTask fileDownloadTask = getFileDownloadTask(url);
			fileDownloadTask.setOnStopFileDownloadTaskLisener(new OnStopFileDownloadTaskLisener() {

				@Override
				public void onStopFileDownloadTaskSucceed(String url) {
					// 更改缓存状态
					try {
						mDownloadFileCacher.recordStatus(downloadFileInfo.getUrl(), Status.DOWNLOAD_STATUS_ERROR, 0);
					} catch (DownloadStatusRecordException e) {
						e.printStackTrace();
					}
					// 回调给外部
					if (onFileDownloadStatusListener != null) {
						OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(downloadFileInfo,
								failReason, onFileDownloadStatusListener);
					}
				}

				@Override
				public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
					// 更改缓存状态
					try {
						mDownloadFileCacher.recordStatus(downloadFileInfo.getUrl(), Status.DOWNLOAD_STATUS_ERROR, 0);
					} catch (DownloadStatusRecordException e) {
						e.printStackTrace();
					}
					// 回调给外部
					if (onFileDownloadStatusListener != null) {
						OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(downloadFileInfo,
								new OnFileDownloadStatusFailReason(failReason), onFileDownloadStatusListener);
					}
				}
			});
			fileDownloadTask.stop();
		} else {
			if (recordStatus) {
				// 更改缓存状态
				try {
					mDownloadFileCacher.recordStatus(downloadFileInfo.getUrl(), Status.DOWNLOAD_STATUS_ERROR, 0);
				} catch (DownloadStatusRecordException e) {
					e.printStackTrace();
				}
			}
			// 回调给外部
			if (onFileDownloadStatusListener != null) {
				OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(downloadFileInfo, failReason,
						onFileDownloadStatusListener);
			}
		}
	}

	/** 已停止下载任务 */
	private void onFileDonwloadTaskStoped(String url) {
		// 移除
		mFileDownloadTaskMap.remove(url);
	}

	/** 开始一个辅助任务 */
	private void addAndRunSupportTask(Runnable task) {
		// 开始辅助任务
		mConfiguration.getSupportEngine().execute(task);
	}

	/**
	 * 改变状态：等待下载
	 * 
	 * @param downloadFileInfo
	 * @param onFileDownloadStatusListener
	 * @return 改变时出现的异常，null表示没有异常
	 */
	private OnFileDownloadStatusFailReason changeFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo,
			OnFileDownloadStatusListener onFileDownloadStatusListener) {
		if (downloadFileInfo == null) {
			return new OnFileDownloadStatusFailReason("下载信息为空", OnFileDownloadStatusFailReason.TYPE_NULL_POINTER);
		}
		// 更改缓存状态
		try {
			mDownloadFileCacher.recordStatus(downloadFileInfo.getUrl(), Status.DOWNLOAD_STATUS_WAITING, 0);
		} catch (DownloadStatusRecordException e) {
			e.printStackTrace();
			return new OnFileDownloadStatusFailReason(e);
		}
		// 回调给外部
		if (onFileDownloadStatusListener != null) {
			OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPrepared(downloadFileInfo,
					onFileDownloadStatusListener);
		}
		return null;
	}

	/** 释放资源 */
	public void release() {

		// 暂停所有下载
		pauseAll();

		mDetectUrlFileCacher.release();
		mDownloadFileCacher.release();

		// 置空
		sInstance = null;
	}

	// ======================================以下公共方法是暴露给外部的核心方法======================================

	// --------------------------------------创建/继续下载任务--------------------------------------

	/**
	 * 探测网络文件
	 * 
	 * @param url
	 *            下载地址
	 * @param onDetectUrlFileListener
	 *            探测网络文件监听器
	 */
	public void detect(final String url, final OnDetectUrlFileListener onDetectUrlFileListener) {

		checkInit();// 需要初始化后才能操作

		// 1、准备探测网络文件任务
		DetectUrlFileTask detectUrlFileTask = new DetectUrlFileTask(url, mConfiguration.getFileDownloadDir(),
				mDetectUrlFileCacher, mDownloadFileCacher);
		detectUrlFileTask.setOnDetectUrlFileListener(onDetectUrlFileListener);

		// 2、开始探测网络文件
		addAndRunSupportTask(detectUrlFileTask);
	}

	/**
	 * 创建并开始下载（新建下载任务），下载前请先调用{@link #detect(String, OnDetectUrlFileListener)}
	 * 进行资源探测
	 * 
	 * @param url
	 *            下载地址
	 * @param saveDir
	 *            下载文件保存目录
	 * @param fileName
	 *            下载文件名称
	 * @param onFileDownloadStatusListener
	 *            文件下载状态改变监听器
	 */
	public void createAndStart(String url, String saveDir, String fileName,
			final OnFileDownloadStatusListener onFileDownloadStatusListener) {

		checkInit();// 需要初始化后才能操作

		// 1、获取探测文件是否存在
		DetectUrlFileInfo detectUrlFileInfo = getDetectUrlFile(url);
		if (detectUrlFileInfo == null) {
			// error 没缓存有探测文件
			OnFileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason("资源未被探测到！",
					OnFileDownloadStatusFailReason.TYPE_FILE_NOT_DETECT);
			// 有异常
			notifyFileDownloadStatusFailed(getDownloadFile(url), failReason, onFileDownloadStatusListener, false);
			return;
		}

		// 2、正常创建任务下载
		detectUrlFileInfo.setFileDir(saveDir);
		detectUrlFileInfo.setFileName(fileName);
		createAndStartByDetectUrlFile(detectUrlFileInfo, onFileDownloadStatusListener);
	}

	// 根据探测文件信息创建下载
	private void createAndStartByDetectUrlFile(DetectUrlFileInfo detectUrlFileInfo,
			OnFileDownloadStatusListener onFileDownloadStatusListener) {

		// 1、检查异常
		if (detectUrlFileInfo == null) {
			// error 没缓存有探测文件
			OnFileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason("资源未被探测到！",
					OnFileDownloadStatusFailReason.TYPE_FILE_NOT_DETECT);
			// 有异常
			notifyFileDownloadStatusFailed(null, failReason, onFileDownloadStatusListener, false);
			return;
		}

		String url = detectUrlFileInfo.getUrl();

		// 2、检查是否正在下载
		if (isInFileDownloadTaskMap(url)) {
			// error 正在下载
			OnFileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason("正在下载",
					OnFileDownloadStatusFailReason.TYPE_FILE_IS_DOWNLOADING);
			// 有异常
			notifyFileDownloadStatusFailed(getDownloadFile(url), failReason, onFileDownloadStatusListener, false);
			return;
		}

		// 3、创建下载信息
		DownloadFileInfo downloadFileInfo = new DownloadFileInfo(detectUrlFileInfo);
		mDownloadFileCacher.addDownloadFile(downloadFileInfo);

		// 4、添加下载任务进行下载
		startInternal(downloadFileInfo, onFileDownloadStatusListener);
	}

	/** 开始下载 */
	private void startInternal(DownloadFileInfo downloadFileInfo,
			OnFileDownloadStatusListener onFileDownloadStatusListener) {

		OnFileDownloadStatusFailReason failReason = null;// 是否有失败异常，null表示没有

		// 1、检查异常
		if (downloadFileInfo == null) {
			if (failReason == null) {
				// error 下载信息未构建
				failReason = new OnFileDownloadStatusFailReason("下载信息未构建！",
						OnFileDownloadStatusFailReason.TYPE_NULL_POINTER);
			}
		} else {

			String url = downloadFileInfo.getUrl();

			// 检查是否正在下载
			if (failReason == null && isInFileDownloadTaskMap(url)) {
				// error 正在下载
				failReason = new OnFileDownloadStatusFailReason("正在下载",
						OnFileDownloadStatusFailReason.TYPE_FILE_IS_DOWNLOADING);
			}

		}

		// 产生了异常
		if (failReason != null) {
			// 有异常
			notifyFileDownloadStatusFailed(downloadFileInfo, failReason, onFileDownloadStatusListener, false);
			return;
		}

		// 2、新建下载任务信息
		addAndRunFileDonwlaodTask(downloadFileInfo, onFileDownloadStatusListener);
	}

	/**
	 * 开始/继续下载（新建/继续下载任务）
	 * 
	 * @param url
	 *            下载地址
	 * @param onFileDownloadStatusListener
	 *            文件下载状态改变监听器
	 */
	public void start(String url, final OnFileDownloadStatusListener onFileDownloadStatusListener) {

		checkInit();// 需要初始化后才能操作

		// 已下载文件
		DownloadFileInfo downloadFileInfo = getDownloadFile(url);
		// 已经下载过
		if (downloadFileInfo != null) {
			// 继续下载
			startInternal(downloadFileInfo, onFileDownloadStatusListener);
		}
		// 从未下载过，先探测
		else {
			DetectUrlFileInfo detectUrlFileInfo = getDetectUrlFile(url);
			// 已经探测过，但没有下载
			if (detectUrlFileInfo != null) {
				// 创建下载
				createAndStartByDetectUrlFile(detectUrlFileInfo, onFileDownloadStatusListener);
			}
			// 未探测过
			else {
				// 探测
				detect(url, new OnDetectUrlFileListener() {
					@Override
					public void onDetectUrlFileFailed(String url, DetectUrlFileFailReason failReason) {
						// 1、异常
						notifyFileDownloadStatusFailed(getDownloadFile(url), new OnFileDownloadStatusFailReason(
								failReason), onFileDownloadStatusListener, false);
					}

					@Override
					public void onDetectUrlFileExist(String url) {
						// 继续下载
						startInternal(getDownloadFile(url), onFileDownloadStatusListener);
					}

					@Override
					public void onDetectNewDownloadFile(String url, String fileName, String savedDir, int fileSize) {
						// 创建并下载
						createAndStart(url, savedDir, fileName, onFileDownloadStatusListener);
					}
				});
			}
		}
	}

	/**
	 * 批量开始/继续下载（批量新建/继续下载任务）
	 * 
	 * @param urls
	 *            批量下载地址
	 * @param onFileDownloadStatusListener
	 *            文件下载状态改变监听器
	 */
	public void start(List<String> urls, OnFileDownloadStatusListener onFileDownloadStatusListener) {
		// 循环创建任务
		for (String url : urls) {
			if (TextUtils.isEmpty(url)) {
				continue;
			}
			start(url, onFileDownloadStatusListener);
		}
	}

	// --------------------------------------暂停下载任务--------------------------------------

	/** 暂停下载 */
	private boolean pauseInternal(String url, final OnStopFileDownloadTaskLisener onStopFileDownloadTaskLisener) {

		checkInit();// 需要初始化后才能操作

		// 得到任务信息
		FileDownloadTask fileDownloadTask = mFileDownloadTaskMap.get(url);
		if (fileDownloadTask != null) {
			// 设置停止监听器
			fileDownloadTask.setOnStopFileDownloadTaskLisener(new OnStopFileDownloadTaskLisener() {

				@Override
				public void onStopFileDownloadTaskSucceed(String url) {
					onFileDonwloadTaskStoped(url);
					if (onStopFileDownloadTaskLisener != null) {
						onStopFileDownloadTaskLisener.onStopFileDownloadTaskSucceed(url);
					}
				}

				@Override
				public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
					if (onStopFileDownloadTaskLisener != null) {
						onStopFileDownloadTaskLisener.onStopFileDownloadTaskFailed(url, failReason);
					}
				}
			});
			// 停止下载
			fileDownloadTask.stop();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 暂停下载（暂停准备下载/正在下载的任务）
	 * 
	 * @param url
	 *            下载地址
	 */
	public void pause(String url) {
		// 暂停
		pauseInternal(url, null);
	}

	/**
	 * 批量暂停下载（批量暂停准备下载/正在下载的任务）
	 * 
	 * @param urls
	 *            批量下载地址
	 */
	public void pause(List<String> urls) {
		// 循环暂停
		for (String url : urls) {
			if (TextUtils.isEmpty(url)) {
				continue;
			}
			pause(url);
		}
	}

	/**
	 * 暂停全部下载（暂停全部准备下载/正在下载的任务）
	 */
	public void pauseAll() {
		// 暂停全部
		Set<String> urls = mFileDownloadTaskMap.keySet();
		pause(new ArrayList<String>(urls));
	}

	// --------------------------------------重新开始下载任务--------------------------------------

	/**
	 * 重新下载（暂停并删除下载任务/删除下载任务然后新建下载任务）
	 * 
	 * @param url
	 *            下载地址
	 * @param onFileDownloadStatusListener
	 *            文件下载状态改变监听器
	 */
	public void reStart(String url, final OnFileDownloadStatusListener onFileDownloadStatusListener) {

		// 如果正在下载，先暂停
		if (isInFileDownloadTaskMap(url)) {
			// 先暂停
			pauseInternal(url, new OnStopFileDownloadTaskLisener() {

				@Override
				public void onStopFileDownloadTaskSucceed(String url) {
					// 暂停完了再开始
					start(url, onFileDownloadStatusListener);
				}

				@Override
				public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
					// error 停止出任务错，FIXME 此处需要修复
					notifyFileDownloadStatusFailed(getDownloadFile(url),
							new OnFileDownloadStatusFailReason(failReason), onFileDownloadStatusListener, false);
				}
			});
		} else {
			// 直接开始
			start(url, onFileDownloadStatusListener);
		}

	}

	/**
	 * 批量重新下载（批量暂停并删除下载任务/删除下载任务然后新建下载任务）
	 * 
	 * @param urls
	 *            批量下载地址
	 * @param onFileDownloadStatusListener
	 *            文件下载状态改变监听器
	 */
	public void reStart(List<String> urls, OnFileDownloadStatusListener onFileDownloadStatusListener) {
		// 循环暂停
		for (String url : urls) {
			if (TextUtils.isEmpty(url)) {
				continue;
			}
			reStart(url, onFileDownloadStatusListener);
		}
	}

	// --------------------------------------删除下载任务--------------------------------------

	/** 删除 */
	private void deleteInternal(String url, final boolean deleteDownloadedFile,
			final OnDeleteDownloadFileListener onDeleteDownloadFileListener) {

		// 删除下载任务
		DeleteDownloadFileTask deleteDownloadFileTask = new DeleteDownloadFileTask(url, deleteDownloadedFile,
				mDownloadFileCacher);
		deleteDownloadFileTask.setOnDeleteDownloadFileListener(onDeleteDownloadFileListener);

		addAndRunSupportTask(deleteDownloadFileTask);
	}

	/**
	 * 删除下载（暂停并删除下载任务/删除下载任务）
	 * 
	 * @param url
	 *            下载地址
	 * @param deleteDownloadedFile
	 *            是否删除保存的文件，true表示删除保存的文件
	 * @param onDeleteDownloadFileListener
	 *            删除下载文件监听器
	 */
	public void delete(String url, final boolean deleteDownloadedFile,
			final OnDeleteDownloadFileListener onDeleteDownloadFileListener) {

		checkInit();// 需要初始化后才能操作

		FileDownloadTask task = mFileDownloadTaskMap.get(url);
		if (task == null || task.isStopped()) {
			// 直接删除
			deleteInternal(url, deleteDownloadedFile, onDeleteDownloadFileListener);
		} else {
			// 先暂停
			pauseInternal(url, new OnStopFileDownloadTaskLisener() {
				@Override
				public void onStopFileDownloadTaskSucceed(String url) {
					// 开始删除
					deleteInternal(url, deleteDownloadedFile, onDeleteDownloadFileListener);
				}

				@Override
				public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
					if (onDeleteDownloadFileListener != null) {
						// FIXME 删除异常没有单独定义
						OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFileFailed(getDownloadFile(url),
								failReason, onDeleteDownloadFileListener);
					}
				}
			});
		}
	}

	/**
	 * 批量删除下载（批量暂停并删除下载任务/删除下载任务）
	 * 
	 * @param urls
	 *            批量下载地址
	 * @param deleteDownloadedFile
	 *            是否删除保存的文件，true表示删除保存的文件
	 * @param onDeleteDownloadFilesListener
	 *            批量删除下载文件监听器
	 */
	public void delete(final List<String> urls, final boolean deleteDownloadedFile,
			final OnDeleteDownloadFilesListener onDeleteDownloadFilesListener) {
		// FIXME 需要增加返回值用于取消删除

		checkInit();// 需要初始化后才能操作

		if (mDeleteDownloadFilesTask != null && !mDeleteDownloadFilesTask.isStop()) {
			// 正在进行批量删除
			return;
		}

		DeleteDownloadFilesTask deleteDownloadFilesTask = new DeleteDownloadFilesTask(urls, deleteDownloadedFile);
		deleteDownloadFilesTask.setOnDeleteDownloadFilesListener(onDeleteDownloadFilesListener);

		new Thread(deleteDownloadFilesTask).start();// 开始执行

		this.mDeleteDownloadFilesTask = deleteDownloadFilesTask;
	}

	// --------------------------------------移动下载任务--------------------------------------

	private void moveInternal(String url, String newDirPath, OnMoveDownloadFileListener onMoveDownloadFileListener) {

		MoveDownloadFileTask moveDownloadFileTask = new MoveDownloadFileTask(url, newDirPath, mDownloadFileCacher);
		moveDownloadFileTask.setOnMoveDownloadFileListener(onMoveDownloadFileListener);

		addAndRunSupportTask(moveDownloadFileTask);
	}

	/**
	 * 
	 * 移动下载（移动已下载完成的任务）
	 * 
	 * @param url
	 *            下载地址
	 * @param newDirPath
	 *            新文件夹路径
	 * @param onMoveDownloadFileListener
	 *            移动下载文件监听接口
	 */
	public void move(final String url, final String newDirPath,
			final OnMoveDownloadFileListener onMoveDownloadFileListener) {

		checkInit();// 需要初始化后才能操作

		if (isInFileDownloadTaskMap(url)) {
			// 先暂停
			pauseInternal(url, new OnStopFileDownloadTaskLisener() {

				@Override
				public void onStopFileDownloadTaskSucceed(String url) {
					// 开始移动
					moveInternal(url, newDirPath, onMoveDownloadFileListener);
				}

				@Override
				public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
					if (onMoveDownloadFileListener != null) {
						OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(getDownloadFile(url),
								new OnMoveDownloadFileFailReason(failReason), onMoveDownloadFileListener);
					}
				}
			});
		} else {
			// 直接移动
			moveInternal(url, newDirPath, onMoveDownloadFileListener);
		}
	}

	/**
	 * 
	 * 批量移动下载（批量移动已下载完成的任务）
	 * 
	 * @param urls
	 *            批量下载地址
	 * @param newDirPath
	 *            新文件夹路径
	 * @param onMoveDownloadFilesListener
	 *            批量移动下载文件监听接口
	 */
	public void move(List<String> urls, String newDirPath, final OnMoveDownloadFilesListener onMoveDownloadFilesListener) {

		// FIXME 需要增加返回值用于取消移动

		checkInit();// 需要初始化后才能操作

		if (mMoveDownloadFilesTask != null && !mMoveDownloadFilesTask.isStop()) {
			// 正在进行批量移动
			return;
		}

		MoveDownloadFilesTask moveDownloadFilesTask = new MoveDownloadFilesTask(urls, newDirPath);
		moveDownloadFilesTask.setOnMoveDownloadFilesListener(onMoveDownloadFilesListener);

		new Thread(moveDownloadFilesTask).start();// 开始执行

		this.mMoveDownloadFilesTask = moveDownloadFilesTask;
	}

	/**
	 * 重命名下载（重命名已下载完成的任务）
	 * 
	 * @param url
	 *            下载地址
	 * @param newFileName
	 *            新文件名
	 * @param onRenameDownloadFileListener
	 *            重命名下载文件监听接口
	 */
	public void rename(String url, String newFileName, OnRenameDownloadFileListener onRenameDownloadFileListener) {

		checkInit();// 需要初始化后才能操作

		if (isInFileDownloadTaskMap(url)) {
			// 正在下载不允许重命名
		}

		// TODO 待实现
	}

	// ======================================内部类======================================

	// --------------------------------------删除多个文件任务内部类--------------------------------------

	/** 删除多个文件任务 */
	private class DeleteDownloadFilesTask implements Runnable {

		private List<String> urls;
		private boolean deleteDownloadedFile;
		private OnDeleteDownloadFilesListener mOnDeleteDownloadFilesListener;

		private boolean isStop = false;
		private boolean hasRetrun = false;

		private final List<DownloadFileInfo> downloadFilesNeedDelete = new ArrayList<DownloadFileInfo>();
		final List<DownloadFileInfo> downloadFilesDeleted = new ArrayList<DownloadFileInfo>();
		final List<DownloadFileInfo> downloadFilesSkip = new ArrayList<DownloadFileInfo>();

		public DeleteDownloadFilesTask(List<String> urls, boolean deleteDownloadedFile) {
			super();
			this.urls = urls;
			this.deleteDownloadedFile = deleteDownloadedFile;
		}

		public void setOnDeleteDownloadFilesListener(OnDeleteDownloadFilesListener onDeleteDownloadFilesListener) {
			this.mOnDeleteDownloadFilesListener = onDeleteDownloadFilesListener;
		}

		public void stop(boolean stopFlag) {
			this.isStop = stopFlag;
		}

		public boolean isStop() {
			return isStop;
		}

		// 删除完成
		private void onDeleteDownloadFilesCompleted() {
			if (hasRetrun) {
				return;
			}
			hasRetrun = true;
			if (mOnDeleteDownloadFilesListener != null) {
				mOnDeleteDownloadFilesListener.onDeleteDownloadFilesCompleted(downloadFilesNeedDelete,
						downloadFilesDeleted);
			}
			isStop = true;
		}

		@Override
		public void run() {

			// 得到要删除的文件
			for (String url : urls) {
				if (TextUtils.isEmpty(url)) {
					continue;
				}
				downloadFilesNeedDelete.add(getDownloadFile(url));
			}

			// 准备删除
			if (mOnDeleteDownloadFilesListener != null) {
				OnDeleteDownloadFilesListener.MainThreadHelper.onDeleteDownloadFilePrepared(downloadFilesNeedDelete,
						mOnDeleteDownloadFilesListener);
			}

			// 循环删除
			for (int i = 0; i < downloadFilesNeedDelete.size(); i++) {

				if (isStop) {
					// 已暂停回调
					onDeleteDownloadFilesCompleted();
					break;
				}

				DownloadFileInfo downloadFileInfo = downloadFilesNeedDelete.get(i);

				if (downloadFileInfo == null) {
					continue;
				}

				final int count = i;

				String url = downloadFileInfo.getUrl();

				// 单个删除监听器
				final OnDeleteDownloadFileListener onDeleteDownloadFileListener = new OnDeleteDownloadFileListener() {

					@Override
					public void onDeleteDownloadFilePrepared(DownloadFileInfo downloadFileNeedDelete) {

						if (isStop) {
							// 已暂停回调
							onDeleteDownloadFilesCompleted();
							return;
						}
						// 开始删除一个新文件
						if (mOnDeleteDownloadFilesListener != null) {
							mOnDeleteDownloadFilesListener.onDeletingDownloadFiles(downloadFilesNeedDelete,
									downloadFilesDeleted, downloadFilesSkip, downloadFileNeedDelete);
						}
					}

					@Override
					public void onDeleteDownloadFileSuccess(DownloadFileInfo downloadFileDeleted) {
						// 新增一个删除成功
						downloadFilesDeleted.add(downloadFileDeleted);

						if (count == downloadFilesNeedDelete.size() - 1) {// 最后一个
							onDeleteDownloadFilesCompleted();
						}
					}

					@Override
					public void onDeleteDownloadFileFailed(DownloadFileInfo downloadFileInfo, FailReason failReason) {
						// 新增一个删除失败
						downloadFilesSkip.add(downloadFileInfo);

						if (count == downloadFilesNeedDelete.size() - 1) {// 最后一个
							onDeleteDownloadFilesCompleted();
						}
					}
				};

				// 准备单个删除
				if (isInFileDownloadTaskMap(url)) {
					// 先暂停
					pauseInternal(url, new OnStopFileDownloadTaskLisener() {

						@Override
						public void onStopFileDownloadTaskSucceed(String url) {
							if (isStop) {
								// 已暂停回调
								onDeleteDownloadFilesCompleted();
								return;
							}
							// 开始删除
							deleteInternal(url, deleteDownloadedFile, onDeleteDownloadFileListener);
						}

						@Override
						public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
							if (onDeleteDownloadFileListener != null) {
								// FIXME 删除异常没有单独定义
								OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFileFailed(
										getDownloadFile(url), failReason, onDeleteDownloadFileListener);
							}
						}
					});
				} else {
					if (isStop) {
						// 已暂停回调
						onDeleteDownloadFilesCompleted();
						return;
					}
					// 直接删除
					deleteInternal(url, deleteDownloadedFile, onDeleteDownloadFileListener);
				}
			}
		}
	}

	// --------------------------------------移动多个文件任务内部类--------------------------------------

	/** 移动多个文件任务 */
	private class MoveDownloadFilesTask implements Runnable {

		private List<String> urls;
		private String newDirPath;
		private OnMoveDownloadFilesListener mOnMoveDownloadFilesListener;

		private boolean isStop = false;
		private boolean hasRetrun = false;

		final List<DownloadFileInfo> downloadFilesNeedMove = new ArrayList<DownloadFileInfo>();
		final List<DownloadFileInfo> downloadFilesMoved = new ArrayList<DownloadFileInfo>();
		final List<DownloadFileInfo> downloadFilesSkip = new ArrayList<DownloadFileInfo>();

		public MoveDownloadFilesTask(List<String> urls, String newDirPath) {
			super();
			this.urls = urls;
			this.newDirPath = newDirPath;
		}

		public void setOnMoveDownloadFilesListener(OnMoveDownloadFilesListener onMoveDownloadFilesListener) {
			this.mOnMoveDownloadFilesListener = onMoveDownloadFilesListener;
		}

		public void stop(boolean stopFlag) {
			this.isStop = stopFlag;
		}

		public boolean isStop() {
			return isStop;
		}

		// 移动完成
		private void onMoveDownloadFilesCompleted() {
			if (hasRetrun) {
				return;
			}
			hasRetrun = true;
			if (mOnMoveDownloadFilesListener != null) {
				mOnMoveDownloadFilesListener.onMoveDownloadFilesCompleted(downloadFilesNeedMove, downloadFilesMoved);
			}
			isStop = true;
		}

		@Override
		public void run() {

			// 得到要移动的文件
			for (String url : urls) {
				if (TextUtils.isEmpty(url)) {
					continue;
				}
				downloadFilesNeedMove.add(getDownloadFile(url));
			}

			// 准备移动
			if (mOnMoveDownloadFilesListener != null) {
				mOnMoveDownloadFilesListener.onMoveDownloadFilesPrepared(downloadFilesNeedMove);
			}

			// 循环移动
			for (int i = 0; i < downloadFilesNeedMove.size(); i++) {

				if (isStop) {
					// 已暂停回调
					onMoveDownloadFilesCompleted();
					break;
				}

				DownloadFileInfo downloadFileInfo = downloadFilesNeedMove.get(i);

				if (downloadFileInfo == null) {
					continue;
				}

				final int count = i;

				String url = downloadFileInfo.getUrl();

				// 单个移动文件监听器
				final OnMoveDownloadFileListener onMoveDownloadFileListener = new OnMoveDownloadFileListener() {

					@Override
					public void onMoveDownloadFilePrepared(DownloadFileInfo downloadFileNeedToMove) {
						if (isStop) {
							// 已暂停回调
							onMoveDownloadFilesCompleted();
							return;
						}
						// 开始移动一个新文件
						if (mOnMoveDownloadFilesListener != null) {
							mOnMoveDownloadFilesListener.onMovingDownloadFiles(downloadFilesNeedMove, downloadFilesMoved,
									downloadFilesSkip, downloadFileNeedToMove);
						}
					}

					@Override
					public void onMoveDownloadFileSuccess(DownloadFileInfo downloadFileMoved) {
						// 新增一个移动成功
						downloadFilesMoved.add(downloadFileMoved);

						if (count == downloadFilesNeedMove.size() - 1) {// 最后一个
							onMoveDownloadFilesCompleted();
						}
					}

					@Override
					public void onMoveDownloadFileFailed(DownloadFileInfo downloadFileInfo,
							OnMoveDownloadFileFailReason failReason) {
						// 新增一个移动失败
						downloadFilesSkip.add(downloadFileInfo);

						if (count == downloadFilesNeedMove.size() - 1) {// 最后一个
							onMoveDownloadFilesCompleted();
						}
					}

				};

				// 准备移动单个文件
				if (isInFileDownloadTaskMap(url)) {
					// 先暂停
					pauseInternal(url, new OnStopFileDownloadTaskLisener() {

						@Override
						public void onStopFileDownloadTaskSucceed(String url) {
							if (isStop) {
								// 已暂停回调
								onMoveDownloadFilesCompleted();
								return;
							}
							// 开始移动
							moveInternal(url, newDirPath, onMoveDownloadFileListener);
						}

						@Override
						public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
							if (onMoveDownloadFileListener != null) {
								OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(
										getDownloadFile(url), new OnMoveDownloadFileFailReason(failReason),
										onMoveDownloadFileListener);
							}
						}
					});
				} else {
					if (isStop) {
						// 已暂停回调
						onMoveDownloadFilesCompleted();
						return;
					}
					// 直接移动
					moveInternal(url, newDirPath, onMoveDownloadFileListener);
				}
			}
		}
	}
}
