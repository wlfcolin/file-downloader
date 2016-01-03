package org.wlf.filedownloader.file_download;

import android.os.SystemClock;
import android.util.Log;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.FailReason;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.base.Stoppable;
import org.wlf.filedownloader.file_download.DownloadTaskManager.DownloadRecorder;
import org.wlf.filedownloader.file_download.file_saver.FileSaver;
import org.wlf.filedownloader.file_download.file_saver.FileSaver.FileSaveException;
import org.wlf.filedownloader.file_download.file_saver.FileSaver.OnFileSaveListener;
import org.wlf.filedownloader.file_download.http_downloader.ContentLengthInputStream;
import org.wlf.filedownloader.file_download.http_downloader.HttpDownloader;
import org.wlf.filedownloader.file_download.http_downloader.HttpDownloader.HttpDownloadException;
import org.wlf.filedownloader.file_download.http_downloader.HttpDownloader.OnHttpDownloadListener;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener.FileDownloadStatusFailReason;
import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.io.File;
import java.io.InputStream;

/**
 * File Download Task
 * <br/>
 * 文件下载任务
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class FileDownloadTask implements Runnable, Stoppable, OnHttpDownloadListener, OnFileSaveListener {

    /**
     * LOG TAG
     */
    private static final String TAG = FileDownloadTask.class.getSimpleName();

    /**
     * Download Param Info
     */
    private FileDownloadTaskParam mTaskParamInfo;

    private HttpDownloader mDownloader;// HttpDownloader
    private FileSaver mSaver;// FileSaver
    private DownloadRecorder mDownloadRecorder;// DownloadFileRecorder

    private OnFileDownloadStatusListener mOnFileDownloadStatusListener;
    private OnStopFileDownloadTaskListener mOnStopFileDownloadTaskListener;

    // for calculate download speed
    private long mLastDownloadingTime = -1;

    private boolean mIsNotifyTaskFinish;// whether task is finish

    /**
     * create by DownloadFile
     *
     * @param downloadFileInfo DownloadFile
     * @return FileDownloadTaskParam
     */
    public static FileDownloadTaskParam createByDownloadFile(DownloadFileInfo downloadFileInfo) {
        if (downloadFileInfo == null) {
            return null;
        }
        return new FileDownloadTaskParam(downloadFileInfo.getUrl(), downloadFileInfo.getDownloadedSizeLong(), 
                downloadFileInfo.getFileSizeLong(), downloadFileInfo.getETag(), downloadFileInfo.getAcceptRangeType()
                , downloadFileInfo.getTempFilePath(), downloadFileInfo.getFilePath());
    }

    public FileDownloadTask(FileDownloadTaskParam taskParamInfo, DownloadRecorder downloadRecorder, 
                            OnFileDownloadStatusListener onFileDownloadStatusListener) {
        super();
        this.mTaskParamInfo = taskParamInfo;

        init();

        this.mDownloadRecorder = downloadRecorder;
        this.mOnFileDownloadStatusListener = onFileDownloadStatusListener;

        // check whether the task can execute
        if (!checkTaskCanExecute()) {
            return;
        }

        // notifyStatusWaiting
        notifyStatusWaiting();
    }

    // 1.init task
    private void init() {

        Log.i(TAG, "init 1、初始化下载任务，url：" + mTaskParamInfo.mUrl);

        // init Downloader
        Range range = new Range(mTaskParamInfo.mStartPosInTotal, mTaskParamInfo.mFileTotalSize);
        mDownloader = new HttpDownloader(mTaskParamInfo.mUrl, range, mTaskParamInfo.mAcceptRangeType, mTaskParamInfo
                .mETag);
        mDownloader.setOnHttpDownloadListener(this);

        // init Saver
        mSaver = new FileSaver(mTaskParamInfo.mUrl, mTaskParamInfo.mTempFilePath, mTaskParamInfo.mFilePath, 
                mTaskParamInfo.mFileTotalSize);
        mSaver.setOnFileSaveListener(this);
    }

    /**
     * set StopFileDownloadTaskListener
     *
     * @param onStopFileDownloadTaskListener StopFileDownloadTaskListener
     */
    public void setOnStopFileDownloadTaskListener(OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {
        this.mOnStopFileDownloadTaskListener = onStopFileDownloadTaskListener;
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
        return mDownloadRecorder.getDownloadFile(mTaskParamInfo.mUrl);
    }

    /**
     * get current URL
     *
     * @return current URL
     */
    public String getUrl() {
        return mTaskParamInfo.mUrl;
    }

    // 2.run task
    @Override
    public void run() {

        if (checkIsStop()) {
            return;
        }

        if (mIsNotifyTaskFinish) {
            if (!isStopped()) {
                stop();
            }
            return;
        }

        mLastDownloadingTime = -1;

        Log.i(TAG, "FileDownloadTask.run 2、任务开始执行，正在获取资源，url：：" + mTaskParamInfo.mUrl);

        boolean canNext = notifyStatusPreparing();

        if (!canNext) {
            return;
        }

        // start download
        FileDownloadStatusFailReason failReason = null;
        try {
            mDownloader.download();
        } catch (HttpDownloadException e) {
            e.printStackTrace();
            // error download error
            failReason = new FileDownloadStatusFailReason(e);
        } finally {
            if (!mIsNotifyTaskFinish) {
                // if downloaded size == file size,finish download
                // if downloaded size < file size,pause download
                // if failReason != null,error download
                if (failReason == null) {
                    DownloadFileInfo downloadFileInfo = getDownloadFile();
                    if (downloadFileInfo != null) {
                        long downloadedSize = downloadFileInfo.getDownloadedSizeLong();
                        long fileSize = downloadFileInfo.getFileSizeLong();
                        if (downloadedSize == fileSize) {
                            notifyTaskFinish(Status.DOWNLOAD_STATUS_COMPLETED, 0, null);
                        } else if (downloadedSize < fileSize) {
                            // pause download
                            notifyTaskFinish(Status.DOWNLOAD_STATUS_PAUSED, 0, null);
                        } else {
                            //error download
                            failReason = new FileDownloadStatusFailReason("download size error!", 
                                    FileDownloadStatusFailReason.TYPE_DOWNLOAD_FILE_ERROR);
                        }
                    } else {
                        //error download,null pointer
                        failReason = new FileDownloadStatusFailReason("DownloadFile is null!", 
                                FileDownloadStatusFailReason.TYPE_NULL_POINTER);
                    }
                }

                //error
                if (failReason != null && !mIsNotifyTaskFinish) {
                    notifyTaskFinish(Status.DOWNLOAD_STATUS_ERROR, 0, failReason);
                }
            }

            Log.i(TAG, "FileDownloadTask.run 7、任务结束执行，url：" + mTaskParamInfo.mUrl);
        }
    }

    // 3.download connected
    @Override
    public void onDownloadConnected(ContentLengthInputStream inputStream, long startPosInTotal) {

        if (checkIsStop()) {
            return;
        }

        if (mIsNotifyTaskFinish) {
            if (!isStopped()) {
                stop();
            }
            return;
        }

        Log.i(TAG, "FileDownloadTask.run 3、已经连接资源，url：" + mTaskParamInfo.mUrl);

        boolean canNext = notifyStatusPrepared();

        if (!canNext) {
            return;
        }

        // save data
        try {
            mSaver.saveData(inputStream, startPosInTotal);
        } catch (FileSaveException e) {
            e.printStackTrace();
            // error download
            notifyTaskFinish(Status.DOWNLOAD_STATUS_ERROR, 0, new FileDownloadStatusFailReason(e));
        }
    }

    // 4.start save data
    @Override
    public void onSaveDataStart() {

        if (checkIsStop()) {
            return;
        }

        if (mIsNotifyTaskFinish) {
            if (!isStopped()) {
                stop();
            }
            return;
        }

        Log.i(TAG, "FileDownloadTask.run 4、准备下载，url：" + mTaskParamInfo.mUrl);

        boolean canNext = notifyStatusDownloading(0);

        if (!canNext) {
            return;
        }
    }

    // 5.saving data
    @Override
    public void onSavingData(int increaseSize, long totalSize) {

        if (checkIsStop()) {
            return;
        }

        if (mIsNotifyTaskFinish) {
            if (!isStopped()) {
                stop();
            }
            return;
        }

        Log.i(TAG, "FileDownloadTask.run 5、下载中，url：" + mTaskParamInfo.mUrl);

        boolean canNext = notifyStatusDownloading(increaseSize);

        if (!canNext) {
            return;
        }
    }

    // 6.save end
    @Override
    public void onSaveDataEnd(int increaseSize, boolean complete) {

        if (checkIsStop()) {
            return;
        }

        if (mIsNotifyTaskFinish) {
            if (!isStopped()) {
                stop();
            }
            return;
        }

        if (!complete) {

            Log.i(TAG, "FileDownloadTask.run 6、暂停下载，url：" + mTaskParamInfo.mUrl);

            notifyTaskFinish(Status.DOWNLOAD_STATUS_PAUSED, increaseSize, null);
        } else {

            Log.i(TAG, "FileDownloadTask.run 6、下载完成，url：" + mTaskParamInfo.mUrl);

            notifyTaskFinish(Status.DOWNLOAD_STATUS_COMPLETED, increaseSize, null);
        }
    }

    /**
     * notify waiting status to caller
     *
     * @return true means can go on,otherwise need to stop all the operations that will occur
     */
    private boolean notifyStatusWaiting() {
        try {

            mDownloadRecorder.recordStatus(mTaskParamInfo.mUrl, Status.DOWNLOAD_STATUS_WAITING, 0);
            if (mOnFileDownloadStatusListener != null) {
                OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusWaiting(getDownloadFile(), 
                        mOnFileDownloadStatusListener);
            }

            Log.e(TAG, "记录等待状态成功，url：" + mTaskParamInfo.mUrl);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            notifyTaskFinish(Status.DOWNLOAD_STATUS_ERROR, 0, new FileDownloadStatusFailReason(e));
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
            mDownloadRecorder.recordStatus(mTaskParamInfo.mUrl, Status.DOWNLOAD_STATUS_PREPARING, 0);
            if (mOnFileDownloadStatusListener != null) {
                OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPreparing(getDownloadFile(), 
                        mOnFileDownloadStatusListener);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            notifyTaskFinish(Status.DOWNLOAD_STATUS_ERROR, 0, new FileDownloadStatusFailReason(e));
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
            mDownloadRecorder.recordStatus(mTaskParamInfo.mUrl, Status.DOWNLOAD_STATUS_PREPARED, 0);
            if (mOnFileDownloadStatusListener != null) {
                OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPrepared(getDownloadFile(), 
                        mOnFileDownloadStatusListener);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            notifyTaskFinish(Status.DOWNLOAD_STATUS_ERROR, 0, new FileDownloadStatusFailReason(e));
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
            mDownloadRecorder.recordStatus(mTaskParamInfo.mUrl, Status.DOWNLOAD_STATUS_DOWNLOADING, increaseSize);

            DownloadFileInfo downloadFileInfo = getDownloadFile();
            if (downloadFileInfo == null) {
                // if error,make sure the increaseSize is zero
                increaseSize = 0;
                notifyTaskFinish(Status.DOWNLOAD_STATUS_ERROR, increaseSize, new FileDownloadStatusFailReason
                        ("DownloadFile is null!", FileDownloadStatusFailReason.TYPE_NULL_POINTER));
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

                OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusDownloading(downloadFileInfo, 
                        (float) downloadSpeed, remainingTime, mOnFileDownloadStatusListener);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            // if error,make sure the increaseSize is zero
            increaseSize = 0;
            notifyTaskFinish(Status.DOWNLOAD_STATUS_ERROR, increaseSize, new FileDownloadStatusFailReason(e));
            return false;
        }
    }

    /**
     * notify the task finish
     */
    private void notifyTaskFinish(int status, int increaseSize, FileDownloadStatusFailReason failReason) {
        switch (status) {
            // pause,complete,error,means finish the task
            case Status.DOWNLOAD_STATUS_PAUSED:
            case Status.DOWNLOAD_STATUS_COMPLETED:
            case Status.DOWNLOAD_STATUS_ERROR:
                if (mIsNotifyTaskFinish) {
                    return;
                }
                boolean notify = false;
                try {
                    mDownloadRecorder.recordStatus(mTaskParamInfo.mUrl, status, increaseSize);
                    if (mOnFileDownloadStatusListener != null) {
                        switch (status) {
                            case Status.DOWNLOAD_STATUS_PAUSED:
                                OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPaused
                                        (getDownloadFile(), mOnFileDownloadStatusListener);
                                // notifyStopSucceed
                                notifyStopSucceed();
                                notify = true;
                                break;
                            case Status.DOWNLOAD_STATUS_COMPLETED:
                                OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusCompleted
                                        (getDownloadFile(), mOnFileDownloadStatusListener);
                                // notifyStopSucceed
                                notifyStopSucceed();
                                notify = true;
                                break;
                            case Status.DOWNLOAD_STATUS_ERROR:
                                OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(getUrl(), 
                                        getDownloadFile(), failReason, mOnFileDownloadStatusListener);
                                // notifyStopFailed
                                notifyStopFailed(new OnStopDownloadFileTaskFailReason(failReason));
                                break;
                        }
                    }
                    mIsNotifyTaskFinish = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    // error
                    OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(getUrl(), getDownloadFile(), new FileDownloadStatusFailReason(e), mOnFileDownloadStatusListener);
                    mIsNotifyTaskFinish = true;
                } finally {
                    // if notify task finish,force stop if necessary
                    if (mIsNotifyTaskFinish && !notify) {
                        stop();
                    }
                }
                break;
        }
    }

    /**
     * notifyStopSucceed
     */
    private void notifyStopSucceed() {
        if (mOnStopFileDownloadTaskListener != null) {
            mOnStopFileDownloadTaskListener.onStopFileDownloadTaskSucceed(mTaskParamInfo.mUrl);
        }
    }

    /**
     * notifyStopFailed
     */
    private void notifyStopFailed(OnStopDownloadFileTaskFailReason failReason) {
        if (mOnStopFileDownloadTaskListener != null) {
            mOnStopFileDownloadTaskListener.onStopFileDownloadTaskFailed(mTaskParamInfo.mUrl, failReason);
        }
    }

    /**
     * checkIsStop
     *
     * @return true means stopped
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
     * check whether the task can execute
     *
     * @return true means can execute
     */
    private boolean checkTaskCanExecute() {

        FileDownloadStatusFailReason failReason = null;

        if (mTaskParamInfo == null) {
            // error param is null pointer
            failReason = new FileDownloadStatusFailReason("init param is null pointer!", FileDownloadStatusFailReason
                    .TYPE_NULL_POINTER);
        }
        if (failReason == null && !UrlUtil.isUrl(mTaskParamInfo.mUrl)) {
            // error url illegal
            failReason = new FileDownloadStatusFailReason("url illegal!", FileDownloadStatusFailReason
                    .TYPE_URL_ILLEGAL);
        }
        if (failReason == null && !FileUtil.isFilePath(mTaskParamInfo.mFilePath)) {
            // error saveDir illegal
            failReason = new FileDownloadStatusFailReason("saveDir illegal!", FileDownloadStatusFailReason
                    .TYPE_FILE_SAVE_PATH_ILLEGAL);
        }
        if (failReason == null && (!FileUtil.canWrite(mTaskParamInfo.mTempFilePath) || !FileUtil.canWrite
                (mTaskParamInfo.mFilePath))) {
            // error savePath can not write
            failReason = new FileDownloadStatusFailReason("savePath can not write!", FileDownloadStatusFailReason
                    .TYPE_STORAGE_SPACE_CAN_NOT_WRITE);
        }
        if (failReason == null) {
            try {
                String checkPath;
                File file = new File(mTaskParamInfo.mFilePath);
                if (!file.exists()) {
                    checkPath = file.getParentFile().getAbsolutePath();
                } else {
                    checkPath = mTaskParamInfo.mFilePath;
                }
                long freeSize = FileUtil.getAvailableSpace(checkPath);
                long needDownloadSize = mTaskParamInfo.mFileTotalSize - mTaskParamInfo.mStartPosInTotal;
                if (freeSize == -1 || needDownloadSize > freeSize) {
                    // error storage space is full
                    failReason = new FileDownloadStatusFailReason("storage space is full!", 
                            FileDownloadStatusFailReason.TYPE_STORAGE_SPACE_IS_FULL);
                }
            } catch (Exception e) {
                e.printStackTrace();
                failReason = new FileDownloadStatusFailReason(e);
            }
        }

        if (failReason != null) {
            if (!mIsNotifyTaskFinish) {
                notifyTaskFinish(Status.DOWNLOAD_STATUS_ERROR, 0, failReason);
            }
            return false;
        }
        return true;
    }

    /**
     * whether the download task is stopped
     */
    @Override
    public boolean isStopped() {
        if (mSaver != null) {
            return mSaver.isStopped();
        }
        return true;
    }

    /**
     * stop the download task
     */
    @Override
    public void stop() {
        // if it is stopped,notify stop failed
        if (isStopped()) {
            notifyStopFailed(new OnStopDownloadFileTaskFailReason("the task has been stopped!", 
                    OnStopDownloadFileTaskFailReason.TYPE_TASK_IS_STOPPED));
            return;
        }
        if (mSaver != null) {
            mSaver.stop();
        }
    }

    /**
     * FileDownloadTask init Param
     */
    static class FileDownloadTaskParam {
        /**
         * file url
         */
        private String mUrl;
        /**
         * the position of this time to start
         */
        private long mStartPosInTotal;
        /**
         * file total size
         */
        private long mFileTotalSize;
        /**
         * file eTag
         */
        private String mETag;
        /**
         * AcceptRangeType
         */
        private String mAcceptRangeType;
        /**
         * TempFilePath
         */
        private String mTempFilePath;
        /**
         * SaveFilePath
         */
        private String mFilePath;

        public FileDownloadTaskParam(String url, long startPosInTotal, long fileTotalSize, String eTag, String 
                acceptRangeType, String tempFilePath, String filePath) {
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

    /**
     * OnStopFileDownloadTaskListener
     */
    public interface OnStopFileDownloadTaskListener {

        /**
         * StopFileDownloadTaskSucceed
         *
         * @param url file url
         */
        void onStopFileDownloadTaskSucceed(String url);

        /**
         * StopFileDownloadTaskFailed
         *
         * @param url        file url
         * @param failReason fail reason
         */
        void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason);
    }

    /**
     * OnStopDownloadFileTaskFailReason
     */
    public static class OnStopDownloadFileTaskFailReason extends FailReason {

        private static final long serialVersionUID = 6959079784746889291L;

        /**
         * the task has been stopped
         */
        public static final String TYPE_TASK_IS_STOPPED = OnStopDownloadFileTaskFailReason.class.getName() + 
                "_TYPE_TASK_IS_STOPPED";

        public OnStopDownloadFileTaskFailReason(String detailMessage, String type) {
            super(detailMessage, type);
        }

        public OnStopDownloadFileTaskFailReason(Throwable throwable) {
            super(throwable);
        }

        @Override
        protected void onInitTypeWithThrowable(Throwable throwable) {
            super.onInitTypeWithThrowable(throwable);
            // TODO
        }
    }

}
