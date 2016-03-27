package org.wlf.filedownloader.file_download;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.file_download.base.DownloadRecorder;
import org.wlf.filedownloader.file_download.base.DownloadTask;
import org.wlf.filedownloader.file_download.base.OnStopFileDownloadTaskListener;
import org.wlf.filedownloader.file_download.base.OnStopFileDownloadTaskListener.StopDownloadFileTaskFailReason;
import org.wlf.filedownloader.file_download.base.OnTaskRunFinishListener;
import org.wlf.filedownloader.file_download.file_saver.FileSaver;
import org.wlf.filedownloader.file_download.file_saver.FileSaver.FileSaveException;
import org.wlf.filedownloader.file_download.file_saver.FileSaver.OnFileSaveListener;
import org.wlf.filedownloader.file_download.http_downloader.ContentLengthInputStream;
import org.wlf.filedownloader.file_download.http_downloader.HttpDownloader;
import org.wlf.filedownloader.file_download.http_downloader.HttpDownloader.OnHttpDownloadListener;
import org.wlf.filedownloader.file_download.http_downloader.HttpDownloader.OnRangeChangeListener;
import org.wlf.filedownloader.file_download.http_downloader.Range;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener.FileDownloadStatusFailReason;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener.OnFileDownloadStatusFailReason;
import org.wlf.filedownloader.util.DownloadFileUtil;
import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * File Download Task Internal Impl
 * <br/>
 * 文件下载任务
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
class DownloadTaskImpl implements DownloadTask, OnHttpDownloadListener, OnFileSaveListener, OnRangeChangeListener {

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
    private OnTaskRunFinishListener mOnTaskRunFinishListener;

    private FinishState mFinishState;

    private boolean mIsTaskStop = false;
    private boolean mIsRunning = false;

    // for calculate download speed
    private long mLastDownloadingTime = -1;

    private AtomicBoolean mIsNotifyTaskFinish = new AtomicBoolean(false);// whether notify task finish

    private Thread mCurrentTaskThread;

    private ExecutorService mCloseConnectionEngine;// engine use for closing the download connection

    private int mConnectTimeout = 15 * 1000;// 15s default

    /**
     * constructor of DownloadTaskImpl
     *
     * @param taskParamInfo
     * @param downloadRecorder
     */
    public DownloadTaskImpl(FileDownloadTaskParam taskParamInfo, DownloadRecorder downloadRecorder,
                            OnFileDownloadStatusListener onFileDownloadStatusListener) {
        super();
        this.mTaskParamInfo = taskParamInfo;

        init();

        this.mDownloadRecorder = downloadRecorder;
        // listener init here because it is need to use in constructor
        this.mOnFileDownloadStatusListener = onFileDownloadStatusListener;

        // ------------start checking base conditions------------
        {
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

            if (mIsTaskStop) {
                // stop internal impl
                stopInternalImpl();
                // finish
                notifyTaskFinish();
            }
        }
        // ------------end checking base conditions------------
    }

    // 1.init task
    private void init() {

        Log.d(TAG, TAG + ".init 1、初始化新下载任务，url：" + getUrl());

        // init Downloader
        Range range = new Range(mTaskParamInfo.getStartPosInTotal(), mTaskParamInfo.getFileTotalSize());
        mDownloader = new HttpDownloader(getUrl(), range, mTaskParamInfo.getAcceptRangeType(), mTaskParamInfo.getETag
                (), mTaskParamInfo.getLastModified());
        mDownloader.setOnHttpDownloadListener(this);
        mDownloader.setCloseConnectionEngine(mCloseConnectionEngine);
        mDownloader.setConnectTimeout(mConnectTimeout);
        mDownloader.setOnRangeChangeListener(this);
        mDownloader.setRequestMethod(mTaskParamInfo.getRequestMethod());
        mDownloader.setHeaders(mTaskParamInfo.getHeaders());

        // init Saver
        mSaver = new FileSaver(getUrl(), mTaskParamInfo.getTempFilePath(), mTaskParamInfo.getFilePath(),
                mTaskParamInfo.getFileTotalSize());
        mSaver.setOnFileSaveListener(this);

        // DownloadRecorder will init by the constructor
    }

    /**
     * check whether the task can execute
     *
     * @return true means can execute
     */
    private boolean checkTaskCanExecute() {

        FileDownloadStatusFailReason failReason = null;

        String url = getUrl();

        if (mTaskParamInfo == null) {
            // error, param is null pointer
            failReason = new OnFileDownloadStatusFailReason(url, "init param is null pointer !",
                    OnFileDownloadStatusFailReason.TYPE_NULL_POINTER);
        }
        if (failReason == null && !UrlUtil.isUrl(url)) {
            // error, url illegal
            failReason = new OnFileDownloadStatusFailReason(url, "url illegal !", OnFileDownloadStatusFailReason
                    .TYPE_URL_ILLEGAL);
        }
        if (failReason == null && !FileUtil.isFilePath(mTaskParamInfo.getFilePath())) {
            // error, saveDir illegal
            failReason = new OnFileDownloadStatusFailReason(url, "saveDir illegal !", OnFileDownloadStatusFailReason
                    .TYPE_FILE_SAVE_PATH_ILLEGAL);
        }
        if (failReason == null && (!FileUtil.canWrite(mTaskParamInfo.getTempFilePath()) || !FileUtil.canWrite
                (mTaskParamInfo.getFilePath()))) {
            // error, savePath can not write
            failReason = new OnFileDownloadStatusFailReason(url, "savePath can not write !",
                    OnFileDownloadStatusFailReason.TYPE_STORAGE_SPACE_CAN_NOT_WRITE);
        }

        // check download file complete status & file not exist status
        if (failReason == null) {
            DownloadFileInfo downloadFileInfo = getDownloadFile();
            if (downloadFileInfo != null) {
                // status is completed
                if (downloadFileInfo.getStatus() == Status.DOWNLOAD_STATUS_COMPLETED) {
                    // error, the file has been downloaded completely
                    mFinishState = new FinishState(Status.DOWNLOAD_STATUS_COMPLETED);
                    return false;
                }
                // other status
                else if (downloadFileInfo.getDownloadedSizeLong() == downloadFileInfo.getFileSizeLong()) {
                    boolean isSucceed = DownloadFileUtil.tryToRenameTempFileToSaveFile(downloadFileInfo);
                    if (isSucceed) {
                        mFinishState = new FinishState(Status.DOWNLOAD_STATUS_COMPLETED);
                        return false;
                    }
                }
            }
        }

        // check storage space
        if (failReason == null) {
            try {
                String checkPath = null;
                File file = new File(mTaskParamInfo.getFilePath());
                if (file != null) {
                    checkPath = file.getParentFile().getAbsolutePath();
                }
                if (!FileUtil.isFilePath(checkPath)) {
                    failReason = new OnFileDownloadStatusFailReason(url, "file save path illegal !",
                            OnFileDownloadStatusFailReason.TYPE_FILE_SAVE_PATH_ILLEGAL);
                } else {
                    long freeSize = FileUtil.getAvailableSpace(checkPath);
                    long needDownloadSize = mTaskParamInfo.getFileTotalSize() - mTaskParamInfo.getStartPosInTotal();
                    if (freeSize == -1 || needDownloadSize > freeSize) {
                        // error storage space is full
                        failReason = new OnFileDownloadStatusFailReason(url, "storage space is full or" +
                                " storage can not " + "write !", OnFileDownloadStatusFailReason
                                .TYPE_STORAGE_SPACE_IS_FULL);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // cast Exception to OnFileDownloadStatusFailReason
                failReason = new OnFileDownloadStatusFailReason(url, e);
            }
        }

        // error occur
        if (failReason != null) {
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, failReason);
            return false;
        }
        return true;
    }

    // --------------------------------------setters--------------------------------------

    @Override
    public void setOnStopFileDownloadTaskListener(OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {
        this.mOnStopFileDownloadTaskListener = onStopFileDownloadTaskListener;
    }

    @Override
    public void setOnTaskRunFinishListener(OnTaskRunFinishListener onTaskRunFinishListener) {
        this.mOnTaskRunFinishListener = onTaskRunFinishListener;
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
     * set connect timeout
     *
     * @param connectTimeout connect timeout
     */
    public void setConnectTimeout(int connectTimeout) {
        mConnectTimeout = connectTimeout;
        if (mDownloader != null) {
            mDownloader.setConnectTimeout(mConnectTimeout);
        }
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
        return mDownloadRecorder.getDownloadFile(getUrl());
    }

    @Override
    public String getUrl() {
        if (mTaskParamInfo == null) {
            return null;
        }
        return mTaskParamInfo.getUrl();
    }

    public FinishState getFinishState() {
        return mFinishState;
    }

    // --------------------------------------run the task--------------------------------------
    @Override
    public void run() {

        String url = getUrl();

        try {
            mIsRunning = true;
            mCurrentTaskThread = Thread.currentThread();

            // ------------start checking conditions------------
            {
                if (mIsTaskStop) {
                    // stop internal impl
                    stopInternalImpl();
                    // goto finally, notifyTaskFinish()
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
                    // goto finally, notifyTaskFinish()
                    return;
                }

                Log.d(TAG, TAG + ".run 2、任务开始执行，正在获取资源，url：：" + url);

                // 1.check url
                if (!UrlUtil.isUrl(url)) {
                    // error url illegal
                    FileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason(url, "url illegal "
                            + "!", OnFileDownloadStatusFailReason.TYPE_URL_ILLEGAL);

                    mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, failReason);
                    // goto finally, url error
                    return;
                }

                if (!notifyStatusPreparing()) {
                    // stop internal impl
                    stopInternalImpl();
                    // goto finally, notifyTaskFinish()
                    return;
                }
            }
            // ------------end checking conditions------------

            mFinishState = null;// reset mFinishState
            // start download
            mDownloader.download();
            // download finished, in this case, mFinishState will not be null
        } catch (Exception e) {
            e.printStackTrace();

            int status = Status.DOWNLOAD_STATUS_ERROR;

            // special error, file not exist
            if (e instanceof FileSaveException) {
                FileSaveException fileSaveException = (FileSaveException) e;
                if (FileSaveException.TYPE_TEMP_FILE_DOES_NOT_EXIST.equals(fileSaveException.getType())) {
                    status = Status.DOWNLOAD_STATUS_FILE_NOT_EXIST;
                }
            }
            mFinishState = new FinishState(status, new OnFileDownloadStatusFailReason(url, e));
        } finally {
            // finally, notify caller by the FinishState

            // ------------start checking mFinishState------------
            {
                DownloadFileInfo downloadFileInfo = getDownloadFile();
                if (downloadFileInfo == null) {
                    FileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason(url, "the " +
                            "DownloadFile " +
                            "is" + " null, may be not deleted ?", OnFileDownloadStatusFailReason.TYPE_NULL_POINTER);
                    mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, failReason);
                } else {
                    // confirm the download file size
                    long downloadedSize = downloadFileInfo.getDownloadedSizeLong();
                    long fileSize = downloadFileInfo.getFileSizeLong();

                    if (downloadedSize == fileSize) {
                        // mFinishState.status should completed
                        if (mFinishState != null) {
                            if (mFinishState.status != Status.DOWNLOAD_STATUS_COMPLETED) {
                                mFinishState = new FinishState(Status.DOWNLOAD_STATUS_COMPLETED);
                            }
                        } else {
                            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_COMPLETED);
                        }
                    } else if (downloadedSize < fileSize) {
                        // pause download, if mFinishState.failReason is null, mFinishState.status should paused
                        if (mFinishState != null) {
                            if (mFinishState.failReason == null) {
                                if (!DownloadFileUtil.hasException(mFinishState.status)) {
                                    mFinishState = new FinishState(Status.DOWNLOAD_STATUS_PAUSED);
                                }
                            }
                        } else {
                            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_PAUSED);
                        }
                    } else {
                        // error download
                        FileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason(url, "the " +
                                "download " + "file size error !", OnFileDownloadStatusFailReason
                                .TYPE_DOWNLOAD_FILE_ERROR);
                        mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, failReason);
                    }
                }
            }
            // ------------end checking mFinishState------------

            // stop internal impl
            stopInternalImpl();

            // identify cur task stopped
            mIsTaskStop = true;
            mIsRunning = false;

            // ------------start notifying caller------------
            {
                // make sure to notify caller
                notifyTaskFinish();
                notifyStopTaskSucceedIfNecessary();
            }
            // ------------end notifying caller------------

            if (mOnTaskRunFinishListener != null) {
                mOnTaskRunFinishListener.onTaskRunFinish();
            }

            boolean hasException = (mFinishState != null && mFinishState.failReason != null && DownloadFileUtil
                    .hasException(mFinishState.status)) ? true : false;

            Log.d(TAG, TAG + ".run 7、文件下载任务【已结束】，是否有异常：" + hasException + "，url：" + url);
        }
    }

    // ----------------------all callback methods below are sync in cur task run method----------------------

    // if range changed, whether need to do something
    @Override
    public boolean onRangeChanged(Range oldRange, Range newRange) {

        if (Range.isLegal(newRange)) {
            /**
             * illegal condition:
             * <br/>
             * --------|(oldRange.startPos)--------|(newRange.startPos)--------
             */
            // do the logic
            if (newRange.startPos > oldRange.startPos && oldRange.startPos >= 0) {
                // illegal
                return false;
            } else {
                // legal, need update the start pos in record, the downloadedSize in DownloadFileInfo
                try {
                    mDownloadRecorder.resetDownloadSize(getUrl(), newRange.startPos);
                    // update succeed
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        }
        return true;
    }

    // 3.download connected
    @Override
    public void onDownloadConnected(ContentLengthInputStream inputStream, long startPosInTotal) {

        if (mIsTaskStop) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished,notifyTaskFinish()
            return;
        }

        Log.d(TAG, TAG + ".run 3、已经连接到资源，url：" + getUrl());

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
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, new OnFileDownloadStatusFailReason(getUrl(),
                    e));
        }
    }

    // 4.start save data
    @Override
    public void onSaveDataStart() {

        if (mIsTaskStop) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished, notifyTaskFinish()
            return;
        }

        Log.d(TAG, TAG + ".run 4、准备下载，url：" + getUrl());

        if (!notifyStatusDownloading(0)) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished, notifyTaskFinish()
            return;
        }
    }

    // 5.saving data
    @Override
    public void onSavingData(int increaseSize, long totalSize) {

        if (mIsTaskStop) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished, notifyTaskFinish()
            return;
        }

        Log.d(TAG, TAG + ".run 5、下载中，url：" + getUrl());

        if (!notifyStatusDownloading(increaseSize)) {
            // stop internal impl
            stopInternalImpl();
            // wait for the task run method finished, notifyTaskFinish()
            return;
        }
    }

    // 6.save end
    @Override
    public void onSaveDataEnd(int increaseSize, boolean complete) {

        if (!complete) {
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_PAUSED, increaseSize);

            Log.d(TAG, TAG + ".run 6、暂停下载，url：" + getUrl());
        } else {
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_COMPLETED, increaseSize);

            Log.d(TAG, TAG + ".run 6、下载完成，url：" + getUrl());
        }

        // save finished, wait for the task run method finished, notifyTaskFinish()
    }

    // --------------------------------------notify caller--------------------------------------

    /**
     * notify waiting status to caller
     *
     * @return true means can go on,otherwise need to stop all the operations that will occur
     */
    private boolean notifyStatusWaiting() {
        try {
            mDownloadRecorder.recordStatus(getUrl(), Status.DOWNLOAD_STATUS_WAITING, 0);
            if (mOnFileDownloadStatusListener != null) {
                mOnFileDownloadStatusListener.onFileDownloadStatusWaiting(getDownloadFile());
            }

            Log.i(TAG, "file-downloader-status 记录【等待状态】成功，url：" + getUrl());

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, new OnFileDownloadStatusFailReason(getUrl(),
                    e));
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
            mDownloadRecorder.recordStatus(getUrl(), Status.DOWNLOAD_STATUS_PREPARING, 0);
            if (mOnFileDownloadStatusListener != null) {
                mOnFileDownloadStatusListener.onFileDownloadStatusPreparing(getDownloadFile());
            }

            Log.i(TAG, "file-downloader-status 记录【正在准备状态】成功，url：" + getUrl());

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, new OnFileDownloadStatusFailReason(getUrl(),
                    e));
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
            mDownloadRecorder.recordStatus(getUrl(), Status.DOWNLOAD_STATUS_PREPARED, 0);
            if (mOnFileDownloadStatusListener != null) {
                mOnFileDownloadStatusListener.onFileDownloadStatusPrepared(getDownloadFile());
            }

            Log.i(TAG, "file-downloader-status 记录【已准备状态】成功，url：" + getUrl());

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, new OnFileDownloadStatusFailReason(getUrl(),
                    e));
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
            mDownloadRecorder.recordStatus(getUrl(), Status.DOWNLOAD_STATUS_DOWNLOADING, increaseSize);

            DownloadFileInfo downloadFileInfo = getDownloadFile();
            if (downloadFileInfo == null) {
                // if error, make sure the increaseSize is zero
                increaseSize = 0;
                FileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason(getUrl(), "the " +
                        "DownloadFile is " + "null!", OnFileDownloadStatusFailReason.TYPE_NULL_POINTER);
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
                    downloadSpeed = increaseKbs / increaseSeconds;// download speed, KB/s
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

            Log.i(TAG, "file-downloader-status 记录【正在下载状态】成功，url：" + getUrl());

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            // if error, make sure the increaseSize is zero
            increaseSize = 0;
            mFinishState = new FinishState(Status.DOWNLOAD_STATUS_ERROR, increaseSize, new
                    OnFileDownloadStatusFailReason(getUrl(), e));
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
                if (mIsNotifyTaskFinish.get()) {
                    return;
                }
                try {
                    mDownloadRecorder.recordStatus(getUrl(), status, increaseSize);
                    switch (status) {
                        case Status.DOWNLOAD_STATUS_PAUSED:
                            if (mOnFileDownloadStatusListener != null) {
                                if (mIsNotifyTaskFinish.compareAndSet(false, true)) {
                                    mOnFileDownloadStatusListener.onFileDownloadStatusPaused(getDownloadFile());

                                    Log.i(TAG, "file-downloader-status 记录【暂停状态】成功，url：" + getUrl());
                                }
                            }
                            break;
                        case Status.DOWNLOAD_STATUS_COMPLETED:
                            if (mOnFileDownloadStatusListener != null) {
                                if (mIsNotifyTaskFinish.compareAndSet(false, true)) {
                                    mOnFileDownloadStatusListener.onFileDownloadStatusCompleted(getDownloadFile());

                                    Log.i(TAG, "file-downloader-status 记录【完成状态】成功，url：" + getUrl());
                                }
                            }
                            break;
                        case Status.DOWNLOAD_STATUS_ERROR:
                            if (mOnFileDownloadStatusListener != null) {
                                if (mIsNotifyTaskFinish.compareAndSet(false, true)) {
                                    mOnFileDownloadStatusListener.onFileDownloadStatusFailed(getUrl(),
                                            getDownloadFile(), failReason);

                                    Log.i(TAG, "file-downloader-status 记录【错误状态】成功，url：" + getUrl());
                                }
                            }
                            break;
                        case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:
                            if (mOnFileDownloadStatusListener != null) {
                                if (mIsNotifyTaskFinish.compareAndSet(false, true)) {
                                    mOnFileDownloadStatusListener.onFileDownloadStatusFailed(getUrl(),
                                            getDownloadFile(), failReason);

                                    Log.i(TAG, "file-downloader-status 记录【文件不存在状态】成功，url：" + getUrl());
                                }
                            }
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (mIsNotifyTaskFinish.compareAndSet(false, true)) {
                        // error
                        try {
                            mDownloadRecorder.recordStatus(getUrl(), Status.DOWNLOAD_STATUS_ERROR, 0);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        if (mOnFileDownloadStatusListener != null) {
                            mOnFileDownloadStatusListener.onFileDownloadStatusFailed(getUrl(), getDownloadFile(), new
                                    OnFileDownloadStatusFailReason(getUrl(), e));

                            Log.e(TAG, "file-downloader-status 记录【暂停/完成/出错状态】失败，url：" + getUrl());
                        }
                    }
                } finally {
                    // if not notify finish, notify paused by default
                    if (mIsNotifyTaskFinish.compareAndSet(false, true)) {
                        // if not notify, however paused status will be recorded
                        try {
                            mDownloadRecorder.recordStatus(getUrl(), Status.DOWNLOAD_STATUS_PAUSED, 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (mOnFileDownloadStatusListener != null) {
                            mOnFileDownloadStatusListener.onFileDownloadStatusPaused(getDownloadFile());
                        }

                        Log.i(TAG, "file-downloader-status 记录【暂停状态】成功，url：" + getUrl());
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
            mOnStopFileDownloadTaskListener.onStopFileDownloadTaskSucceed(getUrl());
            // notify once only
            mOnStopFileDownloadTaskListener = null;

            Log.i(TAG, "file-downloader-status 通知【暂停任务】成功，url：" + getUrl());
        }
    }

    /**
     * notifyStopTaskFailedIfNecessary
     */
    private void notifyStopTaskFailedIfNecessary(StopDownloadFileTaskFailReason failReason) {
        if (mOnStopFileDownloadTaskListener != null) {
            mOnStopFileDownloadTaskListener.onStopFileDownloadTaskFailed(getUrl(), failReason);
            // notify once only
            mOnStopFileDownloadTaskListener = null;

            Log.e(TAG, "file-downloader-status 通知【暂停任务】失败，url：" + getUrl());
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

        Log.d(TAG, TAG + ".stop 结束任务执行，url：" + getUrl() + ",是否已经暂停：" + mIsTaskStop);

        // if it is stopped,notify stop failed
        if (isStopped()) {
            notifyStopTaskFailedIfNecessary(new StopDownloadFileTaskFailReason(getUrl(), "the task has" +
                    " " +
                    "been" + " stopped!", StopDownloadFileTaskFailReason.TYPE_TASK_HAS_BEEN_STOPPED));
            return;
        }
        // stop must be async
        if (Thread.currentThread() == mCurrentTaskThread) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mIsTaskStop = true;

                    Log.d(TAG, TAG + ".stop 结束任务执行(主线程发起)，url：" + getUrl() + ",是否已经暂停：" + mIsTaskStop);

                    stopInternalImpl();

                }
            });
        } else {
            mIsTaskStop = true;

            Log.d(TAG, TAG + ".stop 结束任务执行(其它线程发起)，url：" + getUrl() + ",是否已经暂停：" + mIsTaskStop);

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
                    if (!mIsRunning) {
                        // notify stopped
                        notifyTaskFinish();
                        notifyStopTaskSucceedIfNecessary();
                    }
                }
            });
        } else {
            if (!mSaver.isStopped()) {
                mSaver.stop();// will cause the task run method end
            }
            if (!mIsRunning) {
                // notify stopped
                notifyTaskFinish();
                notifyStopTaskSucceedIfNecessary();
            }
        }
    }

    /**
     * temp record finish state
     */
    static class FinishState {

        public final int status;
        public final int increaseSize;
        public final FileDownloadStatusFailReason failReason;

        public FinishState(int status) {
            this.status = status;
            increaseSize = 0;
            failReason = null;
        }

        public FinishState(int status, int increaseSize) {
            this.status = status;
            this.increaseSize = increaseSize;
            failReason = null;
        }

        public FinishState(int status, FileDownloadStatusFailReason failReason) {
            this.status = status;
            increaseSize = 0;
            this.failReason = failReason;
        }

        public FinishState(int status, int increaseSize, FileDownloadStatusFailReason failReason) {
            this.status = status;
            this.increaseSize = increaseSize;
            this.failReason = failReason;
        }

        public int getStatus() {
            return status;
        }

        public int getIncreaseSize() {
            return increaseSize;
        }

        public FileDownloadStatusFailReason getFailReason() {
            return failReason;
        }
    }

}
