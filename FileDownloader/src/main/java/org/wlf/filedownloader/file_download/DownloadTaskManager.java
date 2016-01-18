package org.wlf.filedownloader.file_download;

import android.text.TextUtils;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.DownloadStatusConfiguration;
import org.wlf.filedownloader.FileDownloadConfiguration;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.file_download.OnStopFileDownloadTaskListener.StopDownloadFileTaskFailReason;
import org.wlf.filedownloader.file_download.base.DownloadRecorder;
import org.wlf.filedownloader.file_download.base.DownloadTask;
import org.wlf.filedownloader.file_download.base.Pauseable;
import org.wlf.filedownloader.listener.OnDetectBigUrlFileListener;
import org.wlf.filedownloader.listener.OnDetectBigUrlFileListener.DetectBigUrlFileFailReason;
import org.wlf.filedownloader.listener.OnDetectUrlFileListener;
import org.wlf.filedownloader.listener.OnDetectUrlFileListener.DetectUrlFileFailReason;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener.FileDownloadStatusFailReason;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener.OnFileDownloadStatusFailReason;
import org.wlf.filedownloader.util.DownloadFileUtil;
import org.wlf.filedownloader.util.NetworkUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DownloadTaskManager,to manage download tasks
 *
 * @author wlf(Andy)
 * @datetime 2015-12-09 18:14 GMT+8
 * @email 411086563@qq.com
 * @since 0.2.0
 */
public class DownloadTaskManager implements Pauseable {

    /**
     * LOG TAG
     */
    private static final String TAG = DownloadTaskManager.class.getSimpleName();

    /**
     * FileDownload Configuration, which stored global configurations
     */
    private FileDownloadConfiguration mConfiguration;
    /**
     * DownloadRecorder, which to record download files info
     */
    private DownloadRecorder mDownloadRecorder;
    /**
     * DetectUrlFileCacher, which stored detect url files
     */
    private DetectUrlFileCacher mDetectUrlFileCacher;

    /**
     * DownloadStatusObserver, which to observe download file status in the download tasks
     */
    private DownloadStatusObserver mDownloadStatusObserver;

    /**
     * all download tasks those are running
     */
    private Map<String, DownloadTask> mRunningDownloadTaskMap = new ConcurrentHashMap<String, DownloadTask>();

    // --------------------------------------lifecycle--------------------------------------

    /**
     * constructor of DownloadTaskManager
     *
     * @param configuration
     * @param downloadRecorder
     */
    public DownloadTaskManager(FileDownloadConfiguration configuration, DownloadRecorder downloadRecorder) {

        this.mConfiguration = configuration;
        this.mDownloadRecorder = downloadRecorder;

        // init DetectUrlFileCacher
        mDetectUrlFileCacher = new DetectUrlFileCacher();
        // init DownloadFileStatusObserver
        mDownloadStatusObserver = new DownloadStatusObserver();
    }

    /**
     * release resources
     *
     * @param onReleaseListener OnReleaseListener impl for listening release completed
     */
    public void release(final OnReleaseListener onReleaseListener) {

        final Set<String> runningUrls = mRunningDownloadTaskMap.keySet();

        // pause all running download tasks
        pause(new ArrayList<String>(runningUrls), new OnStopFileDownloadTaskListener() {

            private List<String> mSucceed = new ArrayList<String>();
            private List<String> mFailed = new ArrayList<String>();

            private boolean mIsNotify = false;

            @Override
            public void onStopFileDownloadTaskSucceed(String url) {
                mSucceed.add(url);
                if (runningUrls.size() == mSucceed.size() + mFailed.size()) {
                    if (mIsNotify) {
                        return;
                    }
                    // notify caller
                    notifyReleased(onReleaseListener);
                    mIsNotify = true;
                }
            }

            @Override
            public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason failReason) {
                mFailed.add(url);
                if (runningUrls.size() == mSucceed.size() + mFailed.size()) {
                    if (mIsNotify) {
                        return;
                    }
                    // notify caller
                    notifyReleased(onReleaseListener);
                }
            }
        });
    }

    // --------------------------------------getters--------------------------------------

    /**
     * get DetectUrlFile by file url
     *
     * @param url file url
     * @return DetectUrlFile
     */
    private DetectUrlFileInfo getDetectUrlFile(String url) {
        return mDetectUrlFileCacher.getDetectUrlFile(url);
    }

    /**
     * get DownloadFile by file url,private
     *
     * @param url file url
     * @return DownloadFile
     */
    private DownloadFileInfo getDownloadFile(String url) {
        return mDownloadRecorder.getDownloadFile(url);
    }

    /**
     * get running download task by file url
     *
     * @param url file url
     * @return running download task
     */
    private DownloadTask getRunningDownloadTask(String url) {

        DownloadFileInfo downloadFileInfo = getDownloadFile(url);

        // not in recorder, maybe the DownloadFileInfo not create(download status before prepared), look up the 
        // DownloadTask in the memory
        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            return mRunningDownloadTaskMap.get(url);
        }

        // check status, only downloading status are available
        if (DownloadFileUtil.isDownloadingStatus(downloadFileInfo)) {
            // get task
            DownloadTask task = mRunningDownloadTaskMap.get(url);
            if (task != null && !task.isStopped()) {
                return task;
            }
        }

        // remove the task when status is not allowed
        if (mRunningDownloadTaskMap.containsKey(url)) {
            mRunningDownloadTaskMap.remove(url);
        }
        return null;
    }

    // --------------------------------------register & unregister listeners--------------------------------------

    /**
     * register an OnFileDownloadStatusListener with Configuration
     *
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener impl
     * @param downloadStatusConfiguration  Configuration for the OnFileDownloadStatusListener impl
     */
    public void registerDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener, 
                                               DownloadStatusConfiguration downloadStatusConfiguration) {
        mDownloadStatusObserver.addOnFileDownloadStatusListener(onFileDownloadStatusListener, 
                downloadStatusConfiguration);
    }

    /**
     * unregister an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener registered OnFileDownloadStatusListener or
     *                                     OnRetryableFileDownloadStatusListener impl
     * @since 0.2.0
     */
    public void unregisterDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        mDownloadStatusObserver.removeOnFileDownloadStatusListener(onFileDownloadStatusListener);
    }

    // --------------------------------------exec tasks--------------------------------------

    /**
     * start a detect url file task
     */
    private void addAndRunDetectUrlFileTask(String url, boolean forceDetect, OnDetectBigUrlFileListener 
            onDetectBigUrlFileListener) {

        // ------------start checking conditions & notifying caller if necessary------------
        {
            DetectBigUrlFileFailReason failReason = null;// null means there are not errors

            // 1.check url
            if (!UrlUtil.isUrl(url)) {
                failReason = new DetectUrlFileFailReason("url illegal !", DetectUrlFileFailReason.TYPE_URL_ILLEGAL);
            }

            // 2.check network
            if (failReason == null && !NetworkUtil.isNetworkAvailable(mConfiguration.getContext())) {
                failReason = new DetectUrlFileFailReason("network not available !", DetectUrlFileFailReason
                        .TYPE_NETWORK_DENIED);

            }

            // error occur
            if (failReason != null) {
                // notify caller
                notifyDetectUrlFileFailed(url, failReason, onDetectBigUrlFileListener);
                return;
            }
        }
        // ------------end checking conditions & notifying caller if necessary------------

        // prepare the DetectUrlFileTask
        DetectUrlFileTask detectUrlFileTask = new DetectUrlFileTask(url, mConfiguration.getFileDownloadDir(), 
                mDetectUrlFileCacher, mDownloadRecorder);
        detectUrlFileTask.setOnDetectBigUrlFileListener(onDetectBigUrlFileListener);
        // set the CloseConnectionEngine
        detectUrlFileTask.setCloseConnectionEngine(mConfiguration.getFileOperationEngine());
        detectUrlFileTask.setConnectTimeout(mConfiguration.getConnectTimeout());
        if (forceDetect) {
            // enableForceDetectMode
            detectUrlFileTask.enableForceDetect();
        }

        // exec the DetectUrlFileTask
        mConfiguration.getFileDetectEngine().execute(detectUrlFileTask);
    }

    /**
     * start a download task
     */
    private void addAndRunDownloadTask(String callerUrl, DownloadFileInfo downloadFileInfo) {

        FileDownloadStatusFailReason failReason = null;// null means there are not errors

        // 1.check url
        if (!UrlUtil.isUrl(callerUrl)) {
            failReason = new OnFileDownloadStatusFailReason("url illegal !", OnFileDownloadStatusFailReason
                    .TYPE_URL_ILLEGAL);
        }

        // 2.network check
        if (failReason == null && !NetworkUtil.isNetworkAvailable(mConfiguration.getContext())) {
            failReason = new OnFileDownloadStatusFailReason("network not available !", OnFileDownloadStatusFailReason
                    .TYPE_NETWORK_DENIED);
        }

        // 3.check downloadFileInfo
        if (failReason == null) {
            if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
                failReason = new OnFileDownloadStatusFailReason("the download file does not exist or illegal !", 
                        OnFileDownloadStatusFailReason.TYPE_DOWNLOAD_FILE_ERROR);
            }
            if (failReason == null) {
                if (TextUtils.isEmpty(callerUrl) || !callerUrl.equals(downloadFileInfo.getUrl()) || !downloadFileInfo
                        .equals(getDownloadFile(callerUrl))) {
                    failReason = new OnFileDownloadStatusFailReason("the download file does not exist or illegal !", 
                            OnFileDownloadStatusFailReason.TYPE_DOWNLOAD_FILE_ERROR);
                }
            }
        }

        // error occur
        if (failReason != null) {
            // notify caller
            notifyDownloadStatusFailed(callerUrl, failReason, downloadFileInfo != null);
            return;
        }

        // downloading, ignore
        if (isDownloading(callerUrl)) {
            return;
        }

        //        // create download task
        //        DownloadTaskImpl downloadTask = new DownloadTaskImpl(FileDownloadTaskParam.createByDownloadFile
        //                (downloadFileInfo), mDownloadRecorder, mDownloadStatusObserver, mConfiguration
        // .getFileDetectEngine());

        // create retryable download task
        RetryableDownloadTaskImpl downloadTask = new RetryableDownloadTaskImpl(FileDownloadTaskParam
                .createByDownloadFile(downloadFileInfo), mDownloadRecorder, mDownloadStatusObserver);
        // set RetryDownloadTimes
        downloadTask.setRetryDownloadTimes(mConfiguration.getRetryDownloadTimes());
        downloadTask.setCloseConnectionEngine(mConfiguration.getFileOperationEngine());
        downloadTask.setConnectTimeout(mConfiguration.getConnectTimeout());

        // record in the task map
        mRunningDownloadTaskMap.put(downloadTask.getUrl(), downloadTask);
        // exec the task
        mConfiguration.getFileDownloadEngine().execute(downloadTask);
    }

    // --------------------------------------notify caller--------------------------------------

    /**
     * notifyDetectUrlFileFailed
     */
    private void notifyDetectUrlFileFailed(String url, DetectBigUrlFileFailReason failReason, 
                                           OnDetectBigUrlFileListener onDetectBigUrlFileListener) {

        Log.d(TAG, "探测文件失败，url：" + url);

        // main thread notify caller
        OnDetectBigUrlFileListener.MainThreadHelper.onDetectUrlFileFailed(url, failReason, onDetectBigUrlFileListener);
    }

    /**
     * notifyDownloadStatusFailed
     */
    private boolean notifyDownloadStatusFailed(String url, final FileDownloadStatusFailReason failReason, final 
    boolean recordStatus) {
        // check whether the download task of the url is running
        if (isDownloading(url)) {
            // pause task first
            pause(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {
                    // continue notify caller
                    notifyDownloadStatusFailedInternal(url, failReason, recordStatus);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason stopFailReason) {
                    // continue notify caller
                    FileDownloadStatusFailReason notifyFailReason = failReason == null ? new 
                            OnFileDownloadStatusFailReason(stopFailReason) : failReason;
                    notifyDownloadStatusFailedInternal(url, notifyFailReason, recordStatus);
                }
            });
            return true;
        } else {
            // notify caller directly
            return notifyDownloadStatusFailedInternal(url, failReason, recordStatus);
        }
    }

    /**
     * notifyDownloadStatusFailedInternal
     */
    private boolean notifyDownloadStatusFailedInternal(String url, FileDownloadStatusFailReason failReason, boolean 
            recordStatus) {
        if (recordStatus) {
            // record status
            try {
                mDownloadRecorder.recordStatus(url, Status.DOWNLOAD_STATUS_ERROR, 0);
                // notify observer
                mDownloadStatusObserver.onFileDownloadStatusFailed(url, getDownloadFile(url), failReason);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            // notify observer
            mDownloadStatusObserver.onFileDownloadStatusFailed(url, getDownloadFile(url), failReason);
            return true;
        }
    }

    /**
     * notifyStopDownloadTaskSucceed
     */
    private void notifyStopDownloadTaskSucceed(String url, OnStopFileDownloadTaskListener 
            onStopFileDownloadTaskListener) {
        // if task stopped, remove the task of the url from download map
        if (mRunningDownloadTaskMap.containsKey(url)) {
            mRunningDownloadTaskMap.remove(url);
        }
        // notify caller
        if (onStopFileDownloadTaskListener != null) {
            onStopFileDownloadTaskListener.onStopFileDownloadTaskSucceed(url);
        }
    }

    /**
     * notifyStopDownloadTaskFailed
     */
    private void notifyStopDownloadTaskFailed(String url, StopDownloadFileTaskFailReason failReason, 
                                              OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {
        // if task stopped, remove the task of the url from download map
        DownloadTask downloadTask = getRunningDownloadTask(url);
        if (downloadTask != null && downloadTask.isStopped()) {
            if (mRunningDownloadTaskMap.containsKey(url)) {
                mRunningDownloadTaskMap.remove(url);
            }
        }

        // notify caller
        if (onStopFileDownloadTaskListener != null) {
            onStopFileDownloadTaskListener.onStopFileDownloadTaskFailed(url, failReason);
        }
    }

    /**
     * notifyReleased
     */
    private void notifyReleased(OnReleaseListener onReleaseListener) {
        // clear detect cache
        mDetectUrlFileCacher.release();
        // observer release
        mDownloadStatusObserver.release();

        // notify caller
        if (onReleaseListener != null) {
            onReleaseListener.onReleased();
        }
    }

    // --------------------------------------detect url files--------------------------------------

    /**
     * detect a big url file
     */
    private void detectInternal(String url, boolean forceDetect, OnDetectBigUrlFileListener 
            onDetectBigUrlFileListener) {
        // start detect task
        addAndRunDetectUrlFileTask(url, forceDetect, onDetectBigUrlFileListener);
    }

    /**
     * detect a url file
     *
     * @param url                     file url
     * @param onDetectUrlFileListener OnDetectUrlFileListener impl
     * @deprecated this method can not detect the url file which bigger than 2G, use {@link #detect(String,
     * OnDetectBigUrlFileListener)}instead
     */
    @Deprecated
    public void detect(String url, final OnDetectUrlFileListener onDetectUrlFileListener) {
        detect(url, new OnDetectBigUrlFileListener() {// inner change callback
            @Override
            public void onDetectNewDownloadFile(String url, String fileName, String saveDir, long fileSize) {
                // continue notify caller
                if (onDetectUrlFileListener != null) {
                    onDetectUrlFileListener.onDetectNewDownloadFile(url, fileName, saveDir, (int) fileSize);
                }
            }

            @Override
            public void onDetectUrlFileExist(String url) {
                // continue notify caller
                if (onDetectUrlFileListener != null) {
                    onDetectUrlFileListener.onDetectUrlFileExist(url);
                }
            }

            @Override
            public void onDetectUrlFileFailed(String url, DetectBigUrlFileFailReason failReason) {
                // continue notify caller
                if (onDetectUrlFileListener != null) {
                    onDetectUrlFileListener.onDetectUrlFileFailed(url, new DetectUrlFileFailReason(failReason));
                }
            }
        });
    }

    /**
     * detect a big url file, which means can detect the url file bigger than 2G
     *
     * @param url                        file url
     * @param onDetectBigUrlFileListener OnDetectBigUrlFileListener impl
     */
    public void detect(String url, OnDetectBigUrlFileListener onDetectBigUrlFileListener) {
        detectInternal(url, false, onDetectBigUrlFileListener);
    }

    // --------------------------------------create/continue downloads--------------------------------------

    /**
     * create a new download after detected a url file by using {@link #detect(String, OnDetectBigUrlFileListener)}
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener, DownloadStatusConfiguration)}
     *
     * @param url      file url
     * @param saveDir  saveDir
     * @param fileName saveFileName
     */
    public void createAndStart(String url, String saveDir, String fileName) {
        // 1.get detected file
        DetectUrlFileInfo detectUrlFileInfo = getDetectUrlFile(url);
        if (detectUrlFileInfo != null) {
            detectUrlFileInfo.setFileDir(saveDir);
            detectUrlFileInfo.setFileName(fileName);
        }

        // 2.create detect task
        createAndStartByDetectUrlFile(url, detectUrlFileInfo);
    }

    /**
     * create download task by using DetectUrlFileInfo
     */
    private void createAndStartByDetectUrlFile(String callerUrl, DetectUrlFileInfo detectUrlFileInfo) {

        FileDownloadStatusFailReason failReason = null;// null means there are not errors

        // 1.check detectUrlFileInfo
        if (!DownloadFileUtil.isLegal(detectUrlFileInfo)) {
            failReason = new OnFileDownloadStatusFailReason("detect file does not exist, please use detect(String," +
                    "OnDetectBigUrlFileListener) first !", OnFileDownloadStatusFailReason.TYPE_FILE_NOT_DETECT);
            if (failReason == null) {
                if (!UrlUtil.isUrl(callerUrl) || !callerUrl.equals(detectUrlFileInfo.getUrl())) {
                    failReason = new OnFileDownloadStatusFailReason("detect file does not exist, please use detect" +
                            "(String,OnDetectBigUrlFileListener) first !", OnFileDownloadStatusFailReason
                            .TYPE_FILE_NOT_DETECT);
                }
            }
        }

        // error occur
        if (failReason != null) {
            // notify caller
            notifyDownloadStatusFailed(callerUrl, failReason, getDownloadFile(callerUrl) != null);
            return;
        }

        // 2.create new downloadFileInfo
        DownloadFileInfo downloadFileInfo = mDownloadRecorder.createDownloadFileInfo(detectUrlFileInfo);

        // delete detected file if succeed create DownloadFileInfo
        if (DownloadFileUtil.isLegal(downloadFileInfo)) {
            // remove the detected cache
            removeDetectUrlFile(detectUrlFileInfo.getUrl());
        }

        // 3.start download task
        startInternal(callerUrl, downloadFileInfo);
    }

    /**
     * start a download task
     */
    private void startInternal(String callerUrl, DownloadFileInfo downloadFileInfo) {
        // start a download task
        addAndRunDownloadTask(callerUrl, downloadFileInfo);
    }

    /**
     * start/continue a download
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br/>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener, DownloadStatusConfiguration)}
     *
     * @param url file url
     */
    public void start(String url) {
        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        // has been downloaded
        if (downloadFileInfo != null) {
            // continue download task
            startInternal(url, downloadFileInfo);
        }
        // not download
        else {
            DetectUrlFileInfo detectUrlFileInfo = getDetectUrlFile(url);
            // detected
            if (detectUrlFileInfo != null) {
                // create download task
                createAndStartByDetectUrlFile(url, detectUrlFileInfo);
            }
            // not detect
            else {
                final String finalUrl = url;
                // detect first
                detect(finalUrl, new OnDetectBigUrlFileListener() {
                    @Override
                    public void onDetectUrlFileFailed(String url, DetectBigUrlFileFailReason failReason) {
                        // notify download status caller
                        notifyDownloadStatusFailed(finalUrl, new OnFileDownloadStatusFailReason(failReason), false);
                    }

                    @Override
                    public void onDetectUrlFileExist(String url) {
                        // continue download task
                        startInternal(finalUrl, getDownloadFile(finalUrl));
                    }

                    @Override
                    public void onDetectNewDownloadFile(String url, String fileName, String savedDir, long fileSize) {
                        // create and start download
                        createAndStart(finalUrl, savedDir, fileName);
                    }
                });
            }
        }
    }

    /**
     * start/continue multi downloads
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener, DownloadStatusConfiguration)}
     *
     * @param urls file urls
     */
    public void start(List<String> urls) {
        for (String url : urls) {
            start(url);
        }
    }

    // --------------------------------------pause downloads--------------------------------------

    /**
     * pause a download task
     */
    private void pauseInternal(String url, final OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {

        // get download task info
        DownloadTask downloadTask = getRunningDownloadTask(url);

        Log.d(TAG, "pauseInternal fileDownloadTask是否已经停止：" + (downloadTask != null ? downloadTask.isStopped() : true) +
                ",url：" + url);

        final String finalUrl = url;

        // if the download task is running, need to pause
        if (downloadTask != null && !downloadTask.isStopped()) {
            // set a OnStopFileDownloadTaskListener first
            downloadTask.setOnStopFileDownloadTaskListener(new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {

                    Log.d(TAG, "pauseInternal 暂停成功，url：" + finalUrl);

                    // notify caller
                    notifyStopDownloadTaskSucceed(finalUrl, onStopFileDownloadTaskListener);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason failReason) {

                    Log.d(TAG, "pauseInternal 暂停失败，url：" + finalUrl + ",failReason:" + failReason.getType());

                    // notify caller
                    notifyStopDownloadTaskFailed(finalUrl, failReason, onStopFileDownloadTaskListener);
                }
            });
            // stop the download task
            downloadTask.stop();
        } else {
            StopDownloadFileTaskFailReason failReason = new StopDownloadFileTaskFailReason("the download task has " +
                    "been paused !", StopDownloadFileTaskFailReason.TYPE_TASK_HAS_BEEN_STOPPED);

            Log.d(TAG, "pauseInternal 任务已经被暂停，url：" + url + ",failReason:" + failReason.getType());

            // confirm the download status
            DownloadFileInfo downloadFileInfo = getDownloadFile(url);
            if (DownloadFileUtil.isDownloadingStatus(downloadFileInfo)) {
                try {
                    mDownloadRecorder.recordStatus(url, Status.DOWNLOAD_STATUS_PAUSED, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // notify caller
            notifyStopDownloadTaskFailed(url, failReason, onStopFileDownloadTaskListener);
        }
    }

    // --------------Pauseable interface methods--------------

    @Override
    public boolean isDownloading(String url) {
        return getRunningDownloadTask(url) != null;
    }

    @Override
    public void pause(String url, OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {
        pauseInternal(url, onStopFileDownloadTaskListener);
    }

    /**
     * pause multi downloads
     *
     * @param urls                           file urls
     * @param onStopFileDownloadTaskListener OnStopFileDownloadTaskListener impl
     */
    public void pause(List<String> urls, OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {
        for (String url : urls) {
            pause(url, onStopFileDownloadTaskListener);
        }
    }

    /**
     * pause all downloads
     *
     * @param onStopFileDownloadTaskListener OnStopFileDownloadTaskListener impl
     */
    public void pauseAll(OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {
        Set<String> urls = mRunningDownloadTaskMap.keySet();
        pause(new ArrayList<String>(urls), onStopFileDownloadTaskListener);
    }

    // --------------------------------------restart downloads--------------------------------------

    /**
     * restart a download
     */
    private void reStartInternal(String url, final boolean isDelete) {

        // 1.check url
        if (!UrlUtil.isUrl(url)) {
            FileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason("url illegal !", 
                    OnFileDownloadStatusFailReason.TYPE_URL_ILLEGAL);
            // notify caller
            notifyDownloadStatusFailed(url, failReason, getDownloadFile(url) != null);
            return;
        }

        // 2.get download file
        final DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (DownloadFileUtil.isLegal(downloadFileInfo)) {

            final String finalUrl = url;
            final String oldFileName = downloadFileInfo.getFileName();
            final String oldFileDir = downloadFileInfo.getFileDir();

            // 2.detect
            detectInternal(url, true, new OnDetectBigUrlFileListener() {
                @Override
                public void onDetectNewDownloadFile(String url, String fileName, String saveDir, long fileSize) {
                    try {
                        // delete old one
                        mDownloadRecorder.resetDownloadFile(finalUrl, isDelete);
                        // create new one
                        createAndStart(finalUrl, oldFileDir, oldFileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        // TODO need a special exception ?
                        FileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason(e);
                        // notify caller
                        notifyDownloadStatusFailed(finalUrl, failReason, getDownloadFile(finalUrl) != null);
                    }
                }

                @Override
                public void onDetectUrlFileExist(String url) {
                    try {
                        // delete old one
                        mDownloadRecorder.resetDownloadFile(finalUrl, isDelete);
                        // start download
                        start(url);
                    } catch (Exception e) {
                        e.printStackTrace();
                        // TODO need a special exception ?
                        FileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason(e);
                        // notify caller
                        notifyDownloadStatusFailed(finalUrl, failReason, getDownloadFile(finalUrl) != null);
                    }
                }

                @Override
                public void onDetectUrlFileFailed(String url, DetectBigUrlFileFailReason failReason) {
                    // notify caller
                    notifyDownloadStatusFailed(finalUrl, new OnFileDownloadStatusFailReason(failReason), 
                            getDownloadFile(finalUrl) != null);
                }
            });
        } else {
            // download do not exist
            // start download
            start(url);
        }
    }

    /**
     * restart a download
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener, DownloadStatusConfiguration)}
     *
     * @param url file url
     */
    public void reStart(String url) {
        // downloading, pause first
        if (isDownloading(url)) {
            final String finalUrl = url;
            // pause
            pause(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {
                    // stop succeed, restart
                    reStartInternal(finalUrl, true);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason failReason) {

                    if (failReason != null) {
                        if (StopDownloadFileTaskFailReason.TYPE_TASK_HAS_BEEN_STOPPED.equals(failReason.getType())) {
                            // has been stopped, so can restart normally
                            reStartInternal(finalUrl, true);
                            return;
                        }
                    }

                    // otherwise error occur, notify caller
                    notifyDownloadStatusFailed(finalUrl, failReason, getDownloadFile(finalUrl) != null);
                }
            });
        } else {
            // has been stopped, restart directly
            reStartInternal(url, true);
        }
    }

    /**
     * restart multi downloads
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br/>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener, DownloadStatusConfiguration)}
     *
     * @param urls file urls
     */
    public void reStart(List<String> urls) {
        for (String url : urls) {
            reStart(url);
        }
    }

    // --------------------------------------others--------------------------------------

    /**
     * remove the detect url file
     */
    private void removeDetectUrlFile(String url) {
        // remove the detected cache
        mDetectUrlFileCacher.removeDetectUrlFile(url);
    }

    // --------------------------------------interfaces--------------------------------------

    /**
     * OnReleaseListener
     */
    public interface OnReleaseListener {
        void onReleased();
    }
}
