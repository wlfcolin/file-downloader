package org.wlf.filedownloader.file_download;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.file_download.OnStopFileDownloadTaskListener.StopDownloadFileTaskFailReason;
import org.wlf.filedownloader.file_download.base.DownloadRecorder;
import org.wlf.filedownloader.file_download.base.DownloadTask;
import org.wlf.filedownloader.file_download.file_saver.FileSaver;
import org.wlf.filedownloader.file_download.file_saver.FileSaver.FileSaveException;
import org.wlf.filedownloader.file_download.file_saver.FileSaver.OnFileSaveListener;
import org.wlf.filedownloader.file_download.http_downloader.ContentLengthInputStream;
import org.wlf.filedownloader.file_download.http_downloader.HttpDownloader;
import org.wlf.filedownloader.file_download.http_downloader.HttpDownloader.OnHttpDownloadListener;
import org.wlf.filedownloader.file_download.http_downloader.Range;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener.FileDownloadStatusFailReason;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener.OnFileDownloadStatusFailReason;
import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * File Download Task Internal Impl
 * <br/>
 * 文件下载任务
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
class DownloadTaskImpl implements DownloadTask, OnHttpDownloadListener, OnFileSaveListener {

    /**
     * LOG TAG
     */
    private static final String TAG = DownloadTaskImpl.class.getSimpleName();

    /**
     * Download Param Info
     */
    private FileDownloadTaskParam mTaskParamInfo;

    private HttpDownloader mDownloader;// HttpDownloader
    private FileSaver mSaver;// FileSaver
    private DownloadRecorder mDownloadRecorder;// DownloadRecorder

    private OnFileDownloadStatusListener mOnFileDownloadStatusListener;
    private OnStopFileDownloadTaskListener mOnStopFileDownloadTaskListener;

    private FinishState mFinishState;

    private boolean mIsTaskStop = false;

    // for calculate download speed
    private long mLastDownloadingTime = -1;

    private boolean mIsNotifyTaskFinish;// whether notify task finish

    private Thread mCurrentTaskThread;

    private ExecutorService mCloseConnectionEngine;// engine use for closing the download connection

    /**
     * @param taskParamInfo
     * @param downloadRecorder
     */
    public DownloadTaskImpl(FileDownloadTaskParam taskParamInfo, DownloadRecorder downloadRecorder) {
        super();
        this.mTaskParamInfo = taskParamInfo;

        init();

        this.mDownloadRecorder = downloadRecorder;

        // check whether the task can be executed
        if (!checkTaskCanExecute()) {
            // stop self
            stop();
            // finish
            notifyTaskFinish();
            return;
        }

        // notifyStatusWaiting
        if (!notifyStatusWaiting()) {
            // stop self
            stop();
            // finish
            notifyTaskFinish();
            return;
        }

        // make sure before the task run exec, the internal impl can not be stopped
        if (mSaver == null || mSaver.isStopped()) {
            // stop self
            stop();
            // finish
            notifyTaskFinish();
            return;
        }
    }

    // 1.init task
    private void init() {

        Log.d(TAG, TAG + ".init 1、初始化新下载任务，url：" + mTaskParamInfo.url);

        // init Downloader
        Range range = new Range(mTaskParamInfo.startPosInTotal, mTaskParamInfo.fileTotalSize);
        mDownloader = new HttpDownloader(mTaskParamInfo.url, range, mTaskParamInfo.acceptRangeType, mTaskParamInfo
                .eTag);
        mDownloader.setOnHttpDownloadListener(this);
        mDownloader.setCloseConnectionEngine(mCloseConnectionEngine);

        // init Saver
        mSaver = new FileSaver(mTaskParamInfo.url, mTaskParamInfo.tempFilePath, mTaskParamInfo.filePath, 
                mTaskParamInfo.fileTotalSize);
        mSaver.setOnFileSaveListener(this);

        // DownloadRecorder will init by the constructor
    }

    /**
     * set FileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener FileDownloadStatusListener
     */
    public void setOnFileDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        mOnFileDownloadStatusListener = onFileDownloadStatusListener;
    }

    @Override
    public void setOnStopFileDownloadTaskListener(OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {
        this.mOnStopFileDownloadTaskListener = onStopFileDownloadTaskListener;
    }

    /**
     * set CloseConnectionEngine
     *
     * @param closeConnectionEngine CloseConnectionEngine
     */
    public void setCloseConnectionEngine(ExecutorService closeConnectionEngine) {
        mCloseConnectionEngine = closeConnectionEngine;
        if (mDownloader != null) {
            mDownloader.setCloseConnectionEngine(mCloseConnectionEngine);
        }
    }

    /**
     * get current DownloadFile
     *
     * @return current DownloadFile
     */
    private DownloadFileInfo getDownloadFile() {
        if (mDownloadRecorder == null) {
            return null;
        }
        return mDownloadRecorder.getDownloadFile(mTaskParamInfo.url);
    }

    @Override
    public String getUrl() {
        if (mTaskParamInfo == null) {
            return null;
        }
        return mTaskParamInfo.url;
    }

    /**
     * check whether the task can execute
     *
     * @return true means can execute
     */
    private boolean checkTaskCanExecute() {

        FileDownloadStatusFailReason failReason = null;

        if (mTaskParamInfo == null) {
            // error param is null pointer
            failReason = new OnFileDownloadStatusFailReason("init param is null pointer!", 
                    OnFileDownloadStatusFailReason.TYPE_NULL_POINTER);
        }
        if (failReason == null && !UrlUtil.isUrl(mTaskParamInfo.url)) {
            // error url illegal
            failReason = new OnFileDownloadStatusFailReason("url illegal!", OnFileDownloadStatusFailReason
                    .TYPE_URL_ILLEGAL);
        }
        if (failReason == null && !FileUtil.isFilePath(mTaskParamInfo.filePath)) {
            // error saveDir illegal
            failReason = new OnFileDownloadStatusFailReason("saveDir illegal!", OnFileDownloadStatusFailReason
                    .TYPE_FILE_SAVE_PATH_ILLEGAL);
        }
        if (failReason == null && (!FileUtil.canWrite(mTaskParamInfo.tempFilePath) || !FileUtil.canWrite
                (mTaskParamInfo.filePath))) {
            // error savePath can not write
            failReason = new OnFileDownloadStatusFailReason("savePath can not write!", OnFileDownloadStatusFailReason
                    .TYPE_STORAGE_SPACE_CAN_NOT_WRITE);
        }
        if (failReason == null) {
            try {
                String checkPath;
                File file = new File(mTaskParamInfo.filePath);
                if (!file.exists()) {
                    checkPath = file.getParentFile().getAbsolutePath();
                } else {
                    checkPath = mTaskParamInfo.filePath;
                }
                long freeSize = FileUtil.getAvailableSpace(checkPath);
                long needDownloadSize = mTaskParamInfo.fileTotalSize - mTaskParamInfo.startPosInTotal;
                if (freeSize == -1 || needDownloadSize > freeSize) {
                    // error storage space is full
                    failReason = new OnFileDownloadStatusFailReason("storage space is full!", 
                            OnFileDownloadStatusFailReason.TYPE_STORAGE_SPACE_IS_FULL);
                }
            } catch (Exception e) {
                e.printStackTrace();
                failReason = new OnFileDownloadStatusFailReason(e);
            }
        }

        if (failReason != null) {
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, 0, failReason);
            return false;
        }
        return true;
    }

    // 2.run task
    @Override
    public void run() {

        try {
            mCurrentTaskThread = Thread.currentThread();

            if (mIsTaskStop) {
                // stop internal impl
                stopInternalImpl();
                // goto finally,notifyTaskFinish()
                return;
            } else {
                if (mSaver == null || mSaver.isStopped()) {
                    init();
                }
            }

            // make sure before the task run exec, the internal impl can not be stopped
            if (mSaver == null || mSaver.isStopped()) {
                // stop internal impl
                stopInternalImpl();
                // goto finally,notifyTaskFinish()
                return;
            }

            Log.d(TAG, TAG + ".run 2、任务开始执行，正在获取资源，url：：" + mTaskParamInfo.url);

            if (!notifyStatusPreparing()) {
                // stop internal impl
                stopInternalImpl();
                // goto finally,notifyTaskFinish()
                return;
            }

            mFinishState = null;// reset mFinishState
            // start download
            mDownloader.download();
            // download finished, in this case, mFinishState will not be null
        } catch (Exception e) {
            e.printStackTrace();
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, 0, new OnFileDownloadStatusFailReason(e));
        } finally {
            DownloadFileInfo downloadFileInfo = getDownloadFile();
            if (downloadFileInfo == null) {
                FileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason("the DownloadFile is " +
                        "null,may be deleted?", OnFileDownloadStatusFailReason.TYPE_NULL_POINTER);
                mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, 0, failReason);
            } else {
                // confirm the download file size legal
                long downloadedSize = downloadFileInfo.getDownloadedSizeLong();
                long fileSize = downloadFileInfo.getFileSizeLong();
                if (downloadedSize == fileSize) {
                    // mFinishState.mStatus should completed
                    if (mFinishState != null) {
                        if (mFinishState.mStatus != Status.DOWNLOAD_STATUS_COMPLETED) {
                            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_COMPLETED, 0, null);
                        }
                    } else {
                        mFinishState = new FinishState(Status.DOWNLOAD_STATUS_COMPLETED, 0, null);
                    }
                } else if (downloadedSize < fileSize) {
                    // pause download, if mFinishState.mFailReason is null, mFinishState.mStatus should paused
                    if (mFinishState != null) {
                        if (mFinishState.mFailReason == null) {
                            if (mFinishState.mStatus != Status.DOWNLOAD_STATUS_PAUSED) {
                                mFinishState = new FinishState(Status.DOWNLOAD_STATUS_PAUSED, 0, null);
                            }
                        }
                    } else {
                        mFinishState = new FinishState(Status.DOWNLOAD_STATUS_PAUSED, 0, null);
                    }
                } else {
                    // error download
                    FileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason("the download file "
                            + "size error!", OnFileDownloadStatusFailReason.TYPE_DOWNLOAD_FILE_ERROR);
                    mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, 0, failReason);
                }
            }

            // identify cur task stopped
            mIsTaskStop = true;
            // stop internal impl
            stopInternalImpl();

            // make sure to notify caller
            notifyTaskFinish();
            notifyStopTaskSucceedIfNecessary();

            boolean hasException = (mFinishState != null && mFinishState.mFailReason != null && mFinishState.mStatus 
                    == Status.DOWNLOAD_STATUS_ERROR) ? true : false;

            Log.d(TAG, TAG + ".run 7、文件任务【已结束】，是否有异常：" + hasException + "，url：" + mTaskParamInfo.url);
        }
    }

    // ----------------------all callback methods below are sync in cur task run method----------------------

    // 3.download connected
    @Override
    public void onDownloadConnected(ContentLengthInputStream inputStream, long startPosInTotal) {

        if (mIsTaskStop) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished,notifyTaskFinish()
            return;
        }

        Log.d(TAG, TAG + ".run 3、已经连接到资源，url：" + mTaskParamInfo.url);

        if (!notifyStatusPrepared()) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished,notifyTaskFinish()
            return;
        }

        // save data
        try {
            mSaver.saveData(inputStream, startPosInTotal);
        } catch (FileSaveException e) {
            e.printStackTrace();
            // error download
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, 0, new OnFileDownloadStatusFailReason(e));
        }
    }

    // 4.start save data
    @Override
    public void onSaveDataStart() {

        if (mIsTaskStop) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished,notifyTaskFinish()
            return;
        }

        Log.d(TAG, TAG + ".run 4、准备下载，url：" + mTaskParamInfo.url);

        if (!notifyStatusDownloading(0)) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished,notifyTaskFinish()
            return;
        }
    }

    // 5.saving data
    @Override
    public void onSavingData(int increaseSize, long totalSize) {

        if (mIsTaskStop) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished,notifyTaskFinish()
            return;
        }

        Log.d(TAG, TAG + ".run 5、下载中，url：" + mTaskParamInfo.url);

        if (!notifyStatusDownloading(increaseSize)) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished,notifyTaskFinish()
            return;
        }
    }

    // 6.save end
    @Override
    public void onSaveDataEnd(int increaseSize, boolean complete) {

        if (!complete) {
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_PAUSED, increaseSize, null);

            Log.d(TAG, TAG + ".run 6、暂停下载，url：" + mTaskParamInfo.url);
        } else {
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_COMPLETED, increaseSize, null);

            Log.d(TAG, TAG + ".run 6、下载完成，url：" + mTaskParamInfo.url);
        }

        // save finished, wait for the task run method finished,notifyTaskFinish()
    }

    /**
     * notify waiting status to caller
     *
     * @return true means can go on,otherwise need to stop all the operations that will occur
     */
    private boolean notifyStatusWaiting() {
        try {
            mDownloadRecorder.recordStatus(mTaskParamInfo.url, Status.DOWNLOAD_STATUS_WAITING, 0);
            if (mOnFileDownloadStatusListener != null) {
                mOnFileDownloadStatusListener.onFileDownloadStatusWaiting(getDownloadFile());
            }

            Log.i(TAG, "file-downloader-status 记录【等待状态】成功，url：" + mTaskParamInfo.url);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, 0, new OnFileDownloadStatusFailReason(e));
            return false;
        }
    }

    /**
     * notify preparing status to callback
     *
     * @return true means can go on,otherwise need to stop all the operations that will occur
     */
    private boolean notifyStatusPreparing() {
        try {
            mDownloadRecorder.recordStatus(mTaskParamInfo.url, Status.DOWNLOAD_STATUS_PREPARING, 0);
            if (mOnFileDownloadStatusListener != null) {
                mOnFileDownloadStatusListener.onFileDownloadStatusPreparing(getDownloadFile());
            }

            Log.i(TAG, "file-downloader-status 记录【正在准备状态】成功，url：" + mTaskParamInfo.url);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, 0, new OnFileDownloadStatusFailReason(e));
            return false;
        }
    }

    /**
     * notify prepared status to callback
     *
     * @return true means can go on,otherwise need to stop all the operations that will occur
     */
    private boolean notifyStatusPrepared() {
        try {
            mDownloadRecorder.recordStatus(mTaskParamInfo.url, Status.DOWNLOAD_STATUS_PREPARED, 0);
            if (mOnFileDownloadStatusListener != null) {
                mOnFileDownloadStatusListener.onFileDownloadStatusPrepared(getDownloadFile());
            }

            Log.i(TAG, "file-downloader-status 记录【已准备状态】成功，url：" + mTaskParamInfo.url);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, 0, new OnFileDownloadStatusFailReason(e));
            return false;
        }
    }

    /**
     * notify downloading status to callback
     *
     * @return true means can go on,otherwise need to stop all the operations that will occur
     */
    private boolean notifyStatusDownloading(int increaseSize) {
        try {
            mDownloadRecorder.recordStatus(mTaskParamInfo.url, Status.DOWNLOAD_STATUS_DOWNLOADING, increaseSize);

            DownloadFileInfo downloadFileInfo = getDownloadFile();
            if (downloadFileInfo == null) {
                // if error,make sure the increaseSize is zero
                increaseSize = 0;
                FileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason("the DownloadFile is " +
                        "null!", OnFileDownloadStatusFailReason.TYPE_NULL_POINTER);
                mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, increaseSize, failReason);

                return false;
            }

            if (mOnFileDownloadStatusListener != null) {
                double downloadSpeed = 0;
                long remainingTime = -1;
                long curDownloadingTime = SystemClock.elapsedRealtime();// current time
                // calculate download speed
                if (mLastDownloadingTime != -1) {
                    double increaseKbs = (double) increaseSize / 1024;
                    double increaseSeconds = (curDownloadingTime - mLastDownloadingTime) / (double) 1000;
                    downloadSpeed = increaseKbs / increaseSeconds;// speed,KB/s
                }
                // calculate remain time
                if (downloadSpeed > 0) {
                    long remainingSize = downloadFileInfo.getFileSizeLong() - downloadFileInfo.getDownloadedSizeLong();
                    if (remainingSize > 0) {
                        remainingTime = (long) (((double) remainingSize / 1024) / downloadSpeed);
                    }
                }
                mLastDownloadingTime = curDownloadingTime;

                if (mOnFileDownloadStatusListener != null) {
                    mOnFileDownloadStatusListener.onFileDownloadStatusDownloading(downloadFileInfo, (float) 
                            downloadSpeed, remainingTime);
                }
            }

            Log.i(TAG, "file-downloader-status 记录【正在下载状态】成功，url：" + mTaskParamInfo.url);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            // if error,make sure the increaseSize is zero
            increaseSize = 0;
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, increaseSize, new 
                    OnFileDownloadStatusFailReason(e));
            return false;
        }
    }

    /**
     * notify the task finish
     */
    private void notifyTaskFinish() {

        if (mFinishState == null) {
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_PAUSED, 0, null);// default paused
        }

        int status = mFinishState.mStatus;
        int increaseSize = mFinishState.mIncreaseSize;
        FileDownloadStatusFailReason failReason = mFinishState.mFailReason;

        switch (status) {
            // pause,complete,error,means finish the task
            case Status.DOWNLOAD_STATUS_PAUSED:
            case Status.DOWNLOAD_STATUS_COMPLETED:
            case Status.DOWNLOAD_STATUS_ERROR:
                if (mIsNotifyTaskFinish) {
                    return;
                }
                try {
                    mDownloadRecorder.recordStatus(mTaskParamInfo.url, status, increaseSize);
                    switch (status) {
                        case Status.DOWNLOAD_STATUS_PAUSED:
                            if (mOnFileDownloadStatusListener != null) {
                                mOnFileDownloadStatusListener.onFileDownloadStatusPaused(getDownloadFile());
                            }

                            Log.i(TAG, "file-downloader-status 记录【暂停状态】成功，url：" + mTaskParamInfo.url);

                            mIsNotifyTaskFinish = true;
                            break;
                        case Status.DOWNLOAD_STATUS_COMPLETED:
                            if (mOnFileDownloadStatusListener != null) {
                                mOnFileDownloadStatusListener.onFileDownloadStatusCompleted(getDownloadFile());
                            }

                            Log.i(TAG, "file-downloader-status 记录【完成状态】成功，url：" + mTaskParamInfo.url);

                            mIsNotifyTaskFinish = true;
                            break;
                        case Status.DOWNLOAD_STATUS_ERROR:
                            if (mOnFileDownloadStatusListener != null) {
                                mOnFileDownloadStatusListener.onFileDownloadStatusFailed(getUrl(), getDownloadFile(),
                                        failReason);
                            }

                            Log.i(TAG, "file-downloader-status 记录【错误状态】成功，url：" + mTaskParamInfo.url);

                            mIsNotifyTaskFinish = true;
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // error
                    // FIXME
                    // mDownloadRecorder.recordStatus(mTaskParamInfo.url, Status.DOWNLOAD_STATUS_ERROR, 0);
                    if (mOnFileDownloadStatusListener != null) {
                        mOnFileDownloadStatusListener.onFileDownloadStatusFailed(getUrl(), getDownloadFile(), new 
                                OnFileDownloadStatusFailReason(e));
                    }

                    Log.e(TAG, "file-downloader-status 记录【暂停/完成/出错状态】失败，url：" + mTaskParamInfo.url);

                    mIsNotifyTaskFinish = true;
                } finally {
                    // if not notify finish, notify paused by default
                    if (!mIsNotifyTaskFinish) {
                        // FIXME
                        // mDownloadRecorder.recordStatus(mTaskParamInfo.url, Status.DOWNLOAD_STATUS_PAUSED, 0);
                        if (mOnFileDownloadStatusListener != null) {
                            mOnFileDownloadStatusListener.onFileDownloadStatusPaused(getDownloadFile());
                        }

                        Log.i(TAG, "file-downloader-status 记录【暂停状态】成功，url：" + mTaskParamInfo.url);

                        mIsNotifyTaskFinish = true;
                    }
                }
                break;
        }
    }

    /**
     * notifyStopTaskSucceedIfNecessary
     */
    private void notifyStopTaskSucceedIfNecessary() {
        if (mOnStopFileDownloadTaskListener != null) {
            mOnStopFileDownloadTaskListener.onStopFileDownloadTaskSucceed(mTaskParamInfo.url);
            // notify once only
            mOnStopFileDownloadTaskListener = null;

            Log.i(TAG, "file-downloader-status 通知【暂停任务】成功，url：" + mTaskParamInfo.url);
        }
    }

    /**
     * notifyStopTaskFailedIfNecessary
     */
    private void notifyStopTaskFailedIfNecessary(StopDownloadFileTaskFailReason failReason) {
        if (mOnStopFileDownloadTaskListener != null) {
            mOnStopFileDownloadTaskListener.onStopFileDownloadTaskFailed(mTaskParamInfo.url, failReason);
            // notify once only
            mOnStopFileDownloadTaskListener = null;

            Log.e(TAG, "file-downloader-status 通知【暂停任务】失败，url：" + mTaskParamInfo.url);
        }
    }

    /**
     * whether the download task is stopped
     */
    @Override
    public boolean isStopped() {
        if (mIsTaskStop) {
            if (!mSaver.isStopped()) {
                // stop internal impl
                stopInternalImpl();
            }
        }
        return mIsTaskStop;
    }

    /**
     * stop the download task
     */
    @Override
    public void stop() {

        Log.d(TAG, TAG + ".stop 结束任务执行，url：" + mTaskParamInfo.url + ",是否已经暂停：" + mIsTaskStop);

        // if it is stopped,notify stop failed
        if (isStopped()) {
            notifyStopTaskFailedIfNecessary(new StopDownloadFileTaskFailReason("the task has been stopped!", 
                    StopDownloadFileTaskFailReason.TYPE_TASK_HAS_BEEN_STOPPED));
            return;
        }
        // stop must be async
        if (Thread.currentThread() == mCurrentTaskThread) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mIsTaskStop = true;

                    Log.d(TAG, TAG + ".stop 结束任务执行(主线程发起)，url：" + mTaskParamInfo.url + ",是否已经暂停：" + mIsTaskStop);

                    stopInternalImpl();
                }
            });
        } else {
            mIsTaskStop = true;

            Log.d(TAG, TAG + ".stop 结束任务执行(其它线程发起)，url：" + mTaskParamInfo.url + ",是否已经暂停：" + mIsTaskStop);

            stopInternalImpl();
        }
    }

    private void stopInternalImpl() {
        // stop must be async
        if (Thread.currentThread() == mCurrentTaskThread) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!mSaver.isStopped()) {
                        mSaver.stop();// will cause the task run method end
                    }
                }
            });
        } else {
            if (!mSaver.isStopped()) {
                mSaver.stop();// will cause the task run method end
            }
        }
    }

    /**
     * temp record finish state
     */
    private static class FinishState {

        private final int mStatus;
        private final int mIncreaseSize;
        private final FileDownloadStatusFailReason mFailReason;

        public FinishState(int status, int increaseSize, FileDownloadStatusFailReason failReason) {
            mStatus = status;
            mIncreaseSize = increaseSize;
            mFailReason = failReason;
        }
    }

}
