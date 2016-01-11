package org.wlf.filedownloader.file_download;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.file_download.OnStopFileDownloadTaskListener.StopDownloadFileTaskFailReason;
import org.wlf.filedownloader.file_download.base.DownloadRecorder;
import org.wlf.filedownloader.file_download.base.RetryableDownloadTask;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.OnRetryableFileDownloadStatusListener;

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
    private long mRecordedSize = 0;// recordedSize by the task
    private int mHasRetriedTimes = 0;// has been retry times
    private boolean mIsTaskStop = false;

    private boolean mIsNotifyTaskFinish;// whether notify task finish

    private OnFileDownloadStatusListener mOnFileDownloadStatusListener;
    private OnStopFileDownloadTaskListener mOnStopFileDownloadTaskListener;

    private FinishState mFinishState;

    private Thread mCurrentTaskThread;

    private ExecutorService mCloseConnectionEngine;// engine use for closing the download connection

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

        // make sure before the task run exec, the internal impl can not be stopped
        if (mFileDownloadTaskImpl == null || mFileDownloadTaskImpl.isStopped()) {
            // stop self
            stop();
            DownloadTaskImpl.FinishState finishStateInternal = mFileDownloadTaskImpl.getFinishState();
            if (finishStateInternal != null) {
                mFinishState = new FinishState(finishStateInternal.getStatus(), finishStateInternal.getFailReason());
            }
            // finish
            notifyTaskFinish();
            return;
        }
    }

    private void init() {
        mTaskParamInfo = new FileDownloadTaskParam(mOriginalTaskParamInfo.url, mOriginalTaskParamInfo.startPosInTotal
                - mRecordedSize, mOriginalTaskParamInfo.fileTotalSize, mOriginalTaskParamInfo.eTag, 
                mOriginalTaskParamInfo.
                acceptRangeType, mOriginalTaskParamInfo.tempFilePath, mOriginalTaskParamInfo.filePath);
        mFileDownloadTaskImpl = new DownloadTaskImpl(mTaskParamInfo, mDownloadRecorder, this);
        mFileDownloadTaskImpl.setCloseConnectionEngine(mCloseConnectionEngine);
    }

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

    @Override
    public void setRetryDownloadTimes(int retryDownloadTimes) {
        mRetryDownloadTimes = retryDownloadTimes;
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
     * notify retrying status to caller
     *
     * @return true means can go on,otherwise need to stop all the operations that will occur
     */
    private boolean notifyStatusRetrying() {
        try {
            // if OnFileDownloadStatusListener2,notify retrying
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
            // if OnFileDownloadStatusListener,notify waiting
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
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, new OnFileDownloadStatusFailReason(e));
            return false;
        }
    }

    /**
     * notify the task finish
     */
    private void notifyTaskFinish() {

        if (mFinishState == null) {
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_PAUSED, null);// default paused
        }

        int status = mFinishState.mStatus;
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
                    mDownloadRecorder.recordStatus(mTaskParamInfo.url, status, 0);
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

                    Log.e(TAG, "file-downloader-status 记录【暂停/完成/错误状态】失败，url：" + mTaskParamInfo.url);

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
     * run task
     */
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
                if (mFileDownloadTaskImpl == null || mFileDownloadTaskImpl.isStopped()) {
                    init();
                }
            }

            // make sure before the task run exec, the internal impl can not be stopped
            if (mFileDownloadTaskImpl == null || mFileDownloadTaskImpl.isStopped()) {
                // stop internal impl
                stopInternalImpl();
                // goto finally,notifyTaskFinish()
                return;
            }

            mFinishState = null;// reset mFinishState
            // first internal impl run
            mFileDownloadTaskImpl.run();
            // first internal impl run finished, in this case, mFinishState will not be null

            // check retry
            final AtomicBoolean isInternalStop = new AtomicBoolean(false);
            // condition：cur task not stop,retried times not over max retry times,mFinishState.mStatus == Status
            // .DOWNLOAD_STATUS_ERROR because only error can retry
            while (!mIsTaskStop && mHasRetriedTimes < mRetryDownloadTimes && mRetryDownloadTimes > 0 &&
                    mFinishState.mStatus == Status.DOWNLOAD_STATUS_ERROR) {
                // get internal impl stop mStatus
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
                    // goto finally,notifyTaskFinish()
                    return;
                }

                isInternalStop.set(false);// false
                mHasRetriedTimes++;// try once

                // notifyStatusRetrying
                if (!notifyStatusRetrying()) {
                    // goto finally,notifyTaskFinish()
                    return;
                }

                try {
                    Thread.sleep(2000);// FIXME sleep 2s
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (mIsTaskStop) {
                    // stop internal impl
                    stopInternalImpl();
                    // clear the error status
                    mFinishState = new FinishState(Status.DOWNLOAD_STATUS_PAUSED, null);
                    // goto finally,notifyTaskFinish()
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
            // stop internal impl
            stopInternalImpl();

            // make sure to notify caller
            notifyTaskFinish();
            notifyStopTaskSucceedIfNecessary();

            boolean hasException = (mFinishState != null && mFinishState.mFailReason != null && mFinishState.mStatus 
                    == Status.DOWNLOAD_STATUS_ERROR) ? true : false;

            Log.d(TAG, TAG + ".run 文件任务【已结束】，是否有异常：" + hasException + "，url：" + mTaskParamInfo.url);
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
                }
            });
        } else {
            if (!mFileDownloadTaskImpl.isStopped()) {
                mFileDownloadTaskImpl.stop();// will cause the task run method end
            }
        }
    }

    // ----------------------all callback methods below are sync in cur task run method----------------------

    @Override
    public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {
        // if cur task stopped,stop internal impl,wait notifyTaskFinish()
        if (mIsTaskStop) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished,notifyTaskFinish()
            return;
        }
        if (mOnFileDownloadStatusListener != null) {
            mOnFileDownloadStatusListener.onFileDownloadStatusWaiting(downloadFileInfo);
        }
    }

    @Override
    public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {
        // if cur task stopped,stop internal impl,wait notifyTaskFinish()
        if (mIsTaskStop) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished,notifyTaskFinish()
            return;
        }
        if (mOnFileDownloadStatusListener != null) {
            mOnFileDownloadStatusListener.onFileDownloadStatusPreparing(downloadFileInfo);
        }
    }

    @Override
    public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {
        // if cur task stopped,stop internal impl,wait notifyTaskFinish()
        if (mIsTaskStop) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished,notifyTaskFinish()
            return;
        }
        if (mOnFileDownloadStatusListener != null) {
            mOnFileDownloadStatusListener.onFileDownloadStatusPrepared(downloadFileInfo);
        }
    }

    @Override
    public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long 
            remainingTime) {
        // if cur task stopped,stop internal impl,wait notifyTaskFinish()
        if (mIsTaskStop) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished,notifyTaskFinish()
            return;
        }
        if (mOnFileDownloadStatusListener != null) {
            mOnFileDownloadStatusListener.onFileDownloadStatusDownloading(downloadFileInfo, downloadSpeed, 
                    remainingTime);
        }
    }

    @Override
    public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {
        // record the state and not notify caller,wait notifyTaskFinish()
        mFinishState = new FinishState(Status.DOWNLOAD_STATUS_PAUSED, null);
    }

    @Override
    public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {
        // record the state and not notify caller,wait notifyTaskFinish()
        mFinishState = new FinishState(Status.DOWNLOAD_STATUS_COMPLETED, null);
    }

    @Override
    public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, 
                                           FileDownloadStatusFailReason failReason) {
        // record the state and not notify caller,wait notifyTaskFinish()
        mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, failReason);
    }

    /**
     * temp record finish state
     */
    private static class FinishState {

        private final int mStatus;
        private final FileDownloadStatusFailReason mFailReason;

        public FinishState(int status, FileDownloadStatusFailReason failReason) {
            this.mStatus = status;
            this.mFailReason = failReason;
        }
    }
}
