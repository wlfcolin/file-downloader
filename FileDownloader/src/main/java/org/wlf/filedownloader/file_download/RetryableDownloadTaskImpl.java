package org.wlf.filedownloader.file_download;

import android.os.Handler;
import android.os.Looper;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.file_download.DownloadTaskImpl.FinishState;
import org.wlf.filedownloader.file_download.OnStopFileDownloadTaskListener.StopDownloadFileTaskFailReason;
import org.wlf.filedownloader.file_download.base.DownloadRecorder;
import org.wlf.filedownloader.file_download.base.RetryableDownloadTask;
import org.wlf.filedownloader.file_download.http_downloader.Range;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.OnRetryableFileDownloadStatusListener;
import org.wlf.filedownloader.util.DownloadFileUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * File Download Task
 * <br/>
 * 文件下载任务
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
class RetryableDownloadTaskImpl implements RetryableDownloadTask, OnFileDownloadStatusListener {

    /**
     * LOG TAG
     */
    private static final String TAG = RetryableDownloadTaskImpl.class.getSimpleName();

    private final FileDownloadTaskParam mOriginalTaskParamInfo;// Download Param Info original
    private FileDownloadTaskParam mTaskParamInfo;// Download Param Info
    private DownloadRecorder mDownloadRecorder;// DownloadRecorder

    private DownloadTaskImpl mFileDownloadTaskImpl;

    // for retry download
    private int mRetryDownloadTimes = 0;// retry times
    private Range mRecordedRange;// recordedRange by the task
    private int mHasRetriedTimes = 0;// has been retry times

    private boolean mIsTaskStop = false;
    private boolean mIsRunning = false;

    private boolean mIsNotifyTaskFinish;// whether notify task finish

    private OnFileDownloadStatusListener mOnFileDownloadStatusListener;
    private OnStopFileDownloadTaskListener mOnStopFileDownloadTaskListener;

    private FinishState mFinishState;

    private Thread mCurrentTaskThread;

    private ExecutorService mCloseConnectionEngine;// engine use for closing the download connection

    private int mConnectTimeout = 15 * 1000;// 15s default

    /**
     * FileDownloadTask
     *
     * @param taskParamInfo
     * @param downloadRecorder
     */
    public RetryableDownloadTaskImpl(FileDownloadTaskParam taskParamInfo, DownloadRecorder downloadRecorder, 
                                     OnFileDownloadStatusListener onFileDownloadStatusListener) {
        super();

        this.mOriginalTaskParamInfo = taskParamInfo;
        this.mDownloadRecorder = downloadRecorder;
        // listener init here because it is need to use in constructor
        this.mOnFileDownloadStatusListener = onFileDownloadStatusListener;

        mIsTaskStop = false;

        init();

        // ------------start checking base conditions------------
        {
            // make sure before the task run exec, the internal impl can not be stopped
            if (mFileDownloadTaskImpl == null || mFileDownloadTaskImpl.isStopped()) {
                // stop self
                stop();
                DownloadTaskImpl.FinishState finishStateInternal = mFileDownloadTaskImpl.getFinishState();
                if (finishStateInternal != null) {
                    mFinishState = new FinishState(finishStateInternal.getStatus(), finishStateInternal.getFailReason
                            ());
                }
                // finish
                notifyTaskFinish();
                return;
            }
        }
        // ------------end checking base conditions------------
    }

    private void init() {
        if (mRecordedRange == null) {// first time to start internal impl task
            mRecordedRange = new Range(mOriginalTaskParamInfo.startPosInTotal, mOriginalTaskParamInfo.startPosInTotal);
        }
        // init
        mTaskParamInfo = new FileDownloadTaskParam(mOriginalTaskParamInfo.url, mOriginalTaskParamInfo.startPosInTotal
                + mRecordedRange.getLength(), mOriginalTaskParamInfo.fileTotalSize, mOriginalTaskParamInfo.eTag, 
                mOriginalTaskParamInfo.
                acceptRangeType, mOriginalTaskParamInfo.tempFilePath, mOriginalTaskParamInfo.filePath);
        mFileDownloadTaskImpl = new DownloadTaskImpl(mTaskParamInfo, mDownloadRecorder, this);
        mFileDownloadTaskImpl.setCloseConnectionEngine(mCloseConnectionEngine);
        mFileDownloadTaskImpl.setConnectTimeout(mConnectTimeout);
    }

    // --------------------------------------setters--------------------------------------

    @Override
    public void setOnStopFileDownloadTaskListener(OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {
        mOnStopFileDownloadTaskListener = onStopFileDownloadTaskListener;
    }

    /**
     * set CloseConnectionEngine
     *
     * @param closeConnectionEngine CloseConnectionEngine
     */
    public void setCloseConnectionEngine(ExecutorService closeConnectionEngine) {
        mCloseConnectionEngine = closeConnectionEngine;
        if (mFileDownloadTaskImpl != null) {
            mFileDownloadTaskImpl.setCloseConnectionEngine(mCloseConnectionEngine);
        }
    }

    /**
     * set connect timeout
     *
     * @param connectTimeout connect timeout
     */
    public void setConnectTimeout(int connectTimeout) {
        mConnectTimeout = connectTimeout;
        if (mFileDownloadTaskImpl != null) {
            mFileDownloadTaskImpl.setConnectTimeout(mConnectTimeout);
        }
    }

    @Override
    public void setRetryDownloadTimes(int retryDownloadTimes) {
        mRetryDownloadTimes = retryDownloadTimes;
    }

    // --------------------------------------getters--------------------------------------

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

    // --------------------------------------notify caller--------------------------------------

    /**
     * notify retrying status to caller
     *
     * @return true means can go on, otherwise need to stop all the operations that will occur
     */
    private boolean notifyStatusRetrying() {
        try {
            // if OnFileDownloadStatusListener2, notify retrying
            if (mOnFileDownloadStatusListener instanceof OnRetryableFileDownloadStatusListener) {
                OnRetryableFileDownloadStatusListener onRetryableFileDownloadStatusListener = 
                        (OnRetryableFileDownloadStatusListener) mOnFileDownloadStatusListener;
                mDownloadRecorder.recordStatus(mTaskParamInfo.url, Status.DOWNLOAD_STATUS_RETRYING, 0);
                if (onRetryableFileDownloadStatusListener != null) {
                    onRetryableFileDownloadStatusListener.onFileDownloadStatusRetrying(getDownloadFile(), 
                            mHasRetriedTimes);
                }

                Log.i(TAG, "file-downloader-status 记录【重试状态】成功，url：" + mTaskParamInfo.url);

                return true;
            }
            // if OnFileDownloadStatusListener, notify waiting
            else {
                return notifyStatusWaiting();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, new OnFileDownloadStatusFailReason(e));
            return false;
        }
    }

    /**
     * notify waiting status to caller
     *
     * @return true means can go on, otherwise need to stop all the operations that will occur
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
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, new OnFileDownloadStatusFailReason(e));
            return false;
        }
    }

    /**
     * notify the task finish
     */
    private void notifyTaskFinish() {

        if (mFinishState == null) {
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_PAUSED);// default paused
        }

        int status = mFinishState.status;
        int increaseSize = mFinishState.increaseSize;
        FileDownloadStatusFailReason failReason = mFinishState.failReason;

        switch (status) {
            // pause,complete,error,means finish the task
            case Status.DOWNLOAD_STATUS_PAUSED:
            case Status.DOWNLOAD_STATUS_COMPLETED:
            case Status.DOWNLOAD_STATUS_ERROR:
            case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:
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
                        case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:
                            if (mOnFileDownloadStatusListener != null) {
                                mOnFileDownloadStatusListener.onFileDownloadStatusFailed(getUrl(), getDownloadFile(),
                                        failReason);
                            }

                            Log.i(TAG, "file-downloader-status 记录【文件不存在状态】成功，url：" + mTaskParamInfo.url);

                            mIsNotifyTaskFinish = true;
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // error
                    try {
                        mDownloadRecorder.recordStatus(mTaskParamInfo.url, Status.DOWNLOAD_STATUS_ERROR, 0);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    if (mOnFileDownloadStatusListener != null) {
                        mOnFileDownloadStatusListener.onFileDownloadStatusFailed(getUrl(), getDownloadFile(), new 
                                OnFileDownloadStatusFailReason(e));
                    }

                    Log.e(TAG, "file-downloader-status 记录【暂停/完成/出错状态】失败，url：" + mTaskParamInfo.url);

                    mIsNotifyTaskFinish = true;
                } finally {
                    // if not notify finish, notify paused by default
                    if (!mIsNotifyTaskFinish) {
                        // if not notify, however paused status will be recorded
                        try {
                            mDownloadRecorder.recordStatus(mTaskParamInfo.url, Status.DOWNLOAD_STATUS_PAUSED, 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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

    // --------------------------------------run the task--------------------------------------

    @Override
    public void run() {

        try {
            mIsRunning = true;
            mCurrentTaskThread = Thread.currentThread();

            if (mIsTaskStop) {
                // stop internal impl
                stopInternalImpl();
                // goto finally, notifyTaskFinish()
                return;
            } else {
                if (mFileDownloadTaskImpl == null || mFileDownloadTaskImpl.isStopped()) {
                    init();
                }
            }

            // make sure before the task run exec, the internal impl can not be stopped
            if (mFileDownloadTaskImpl == null || mFileDownloadTaskImpl.isStopped()) {
                // stop internal impl
                stopInternalImpl();
                // goto finally, notifyTaskFinish()
                return;
            }

            mFinishState = null;// reset mFinishState
            // first internal impl run
            mFileDownloadTaskImpl.run();
            // first internal impl run finished, in this case, mFinishState will not be null

            // check retry
            final AtomicBoolean isInternalStop = new AtomicBoolean(false);
            // condition：cur task not stop,retried times not over max retry times, mFinishState.status == Status
            // .DOWNLOAD_STATUS_ERROR because only error can be retried
            DownloadFileInfo downloadFileInfo = getDownloadFile();

            while (DownloadFileUtil.isTempFileExist(downloadFileInfo) && !mIsTaskStop && mHasRetriedTimes < 
                    mRetryDownloadTimes && mRetryDownloadTimes > 0 && mFinishState.status == Status
                    .DOWNLOAD_STATUS_ERROR) {
                // get internal impl stop status
                isInternalStop.set(mFileDownloadTaskImpl.isStopped());

                if (!isInternalStop.get()) {
                    // wait for internal impl stopped
                    stopInternalImpl();
                    try {
                        Thread.sleep(2000);// FIXME sleep 2s
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                // re-init
                init();

                // make sure before the task run exec, the internal impl can not be stopped
                if (mFileDownloadTaskImpl == null || mFileDownloadTaskImpl.isStopped()) {
                    // stop internal impl
                    stopInternalImpl();
                    // goto finally, notifyTaskFinish()
                    return;
                }

                isInternalStop.set(false);// false
                mHasRetriedTimes++;// try once

                // notifyStatusRetrying
                if (!notifyStatusRetrying()) {
                    // goto finally, notifyTaskFinish()
                    return;
                }

                Log.d(TAG, TAG + ".run 正在重试，url：" + mTaskParamInfo.url);

                try {
                    Thread.sleep(2000);// FIXME sleep 2s
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (mIsTaskStop) {
                    // stop internal impl
                    stopInternalImpl();
                    // clear the error status
                    mFinishState = new FinishState(Status.DOWNLOAD_STATUS_PAUSED);
                    // goto finally, notifyTaskFinish()
                    return;
                } else {
                    if (mFileDownloadTaskImpl == null || mFileDownloadTaskImpl.isStopped()) {
                        init();
                    }
                }

                mFinishState = null;// reset mFinishState

                // internal impl run again
                mFileDownloadTaskImpl.run();
                // internal impl run finished, in this case, mFinishState will not be null
            }
            // retry while case finished
        } catch (Exception e) {
            e.printStackTrace();
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, new OnFileDownloadStatusFailReason(e));
        } finally {
            // identify cur task stop
            mIsTaskStop = true;
            mIsRunning = false;
            // stop internal impl
            stopInternalImpl();

            // make sure to notify caller
            notifyTaskFinish();
            notifyStopTaskSucceedIfNecessary();

            boolean hasException = (mFinishState != null && mFinishState.failReason != null && DownloadFileUtil
                    .hasException(mFinishState.status)) ? true : false;

            Log.d(TAG, TAG + ".run 文件下载任务【已结束】，是否有异常：" + hasException + "，url：" + mTaskParamInfo.url);
        }
    }

    /**
     * whether the download task is stopped
     */
    @Override
    public boolean isStopped() {
        if (mIsTaskStop) {
            if (!mFileDownloadTaskImpl.isStopped()) {
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
                    if (!mFileDownloadTaskImpl.isStopped()) {
                        mFileDownloadTaskImpl.stop();// will cause the task run method end
                    }
                    if (!mIsRunning) {
                        // notify stopped
                        notifyTaskFinish();
                        notifyStopTaskSucceedIfNecessary();
                    }
                }
            });
        } else {
            if (!mFileDownloadTaskImpl.isStopped()) {
                mFileDownloadTaskImpl.stop();// will cause the task run method end
            }
            if (!mIsRunning) {
                // notify stopped
                notifyTaskFinish();
                notifyStopTaskSucceedIfNecessary();
            }
        }
    }

    // ----------------------all callback methods below are sync in cur task run method----------------------

    @Override
    public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {
        // if cur task stopped,stop internal impl, wait notifyTaskFinish()
        if (mIsTaskStop) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished, notifyTaskFinish()
            return;
        }
        if (mOnFileDownloadStatusListener != null) {
            mOnFileDownloadStatusListener.onFileDownloadStatusWaiting(downloadFileInfo);
        }
    }

    @Override
    public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {
        // if cur task stopped,stop internal impl, wait notifyTaskFinish()
        if (mIsTaskStop) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished, notifyTaskFinish()
            return;
        }
        if (mOnFileDownloadStatusListener != null) {
            mOnFileDownloadStatusListener.onFileDownloadStatusPreparing(downloadFileInfo);
        }
    }

    @Override
    public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {
        // if cur task stopped,stop internal impl, wait notifyTaskFinish()
        if (mIsTaskStop) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished, notifyTaskFinish()
            return;
        }
        if (mOnFileDownloadStatusListener != null) {
            mOnFileDownloadStatusListener.onFileDownloadStatusPrepared(downloadFileInfo);
        }

        if (DownloadFileUtil.isLegal(downloadFileInfo)) {
            // record download range
            mRecordedRange = new Range(downloadFileInfo.getDownloadedSizeLong(), mRecordedRange.endPos);
        }
    }

    @Override
    public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long 
            remainingTime) {
        // if cur task stopped, stop internal impl, wait notifyTaskFinish()
        if (mIsTaskStop) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished, notifyTaskFinish()
            return;
        }
        if (mOnFileDownloadStatusListener != null) {
            mOnFileDownloadStatusListener.onFileDownloadStatusDownloading(downloadFileInfo, downloadSpeed, 
                    remainingTime);
        }
    }

    @Override
    public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {
        // record the state and not notify caller, wait notifyTaskFinish()
        mFinishState = new FinishState(Status.DOWNLOAD_STATUS_PAUSED);

        if (DownloadFileUtil.isLegal(downloadFileInfo)) {
            // record download range
            mRecordedRange = new Range(mRecordedRange.startPos, downloadFileInfo.getDownloadedSizeLong());
        }
    }

    @Override
    public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {
        // record the state and not notify caller, wait notifyTaskFinish()
        mFinishState = new FinishState(Status.DOWNLOAD_STATUS_COMPLETED);

        if (DownloadFileUtil.isLegal(downloadFileInfo)) {
            // record download range
            mRecordedRange = new Range(mRecordedRange.startPos, downloadFileInfo.getDownloadedSizeLong());
        }
    }

    @Override
    public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, 
                                           FileDownloadStatusFailReason failReason) {
        if (downloadFileInfo.getStatus() == Status.DOWNLOAD_STATUS_FILE_NOT_EXIST) {
            // record the state and not notify caller, wait notifyTaskFinish()
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_FILE_NOT_EXIST, failReason);
        } else {
            // record the state and not notify caller, wait notifyTaskFinish()
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, failReason);
        }

        if (DownloadFileUtil.isLegal(downloadFileInfo)) {
            // record download range
            mRecordedRange = new Range(mRecordedRange.startPos, downloadFileInfo.getDownloadedSizeLong());
        }
    }

}
