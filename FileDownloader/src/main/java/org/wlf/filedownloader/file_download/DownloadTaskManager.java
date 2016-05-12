package org.wlf.filedownloader.file_download;

import android.text.TextUtils;

import org.wlf.filedownloader.DownloadConfiguration;
import org.wlf.filedownloader.DownloadConfiguration.Builder;
import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.DownloadStatusConfiguration;
import org.wlf.filedownloader.FileDownloadConfiguration;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.file_download.base.DownloadRecorder;
import org.wlf.filedownloader.file_download.base.DownloadTask;
import org.wlf.filedownloader.file_download.base.OnStopFileDownloadTaskListener;
import org.wlf.filedownloader.file_download.base.OnStopFileDownloadTaskListener.StopDownloadFileTaskFailReason;
import org.wlf.filedownloader.file_download.base.OnTaskRunFinishListener;
import org.wlf.filedownloader.file_download.base.Pauseable;
import org.wlf.filedownloader.listener.OnDetectBigUrlFileListener;
import org.wlf.filedownloader.listener.OnDetectBigUrlFileListener.DetectBigUrlFileFailReason;
import org.wlf.filedownloader.listener.OnDetectUrlFileListener;
import org.wlf.filedownloader.listener.OnDetectUrlFileListener.DetectUrlFileFailReason;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener.FileDownloadStatusFailReason;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener.OnFileDownloadStatusFailReason;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.DownloadFileUtil;
import org.wlf.filedownloader.util.FileUtil;
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

    private Object mDownloadTaskLock = new Object();

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
                    mIsNotify = true;
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
        return getRunningDownloadTask(url, false);
    }

    /**
     * get running download task by file url
     *
     * @param url            file url
     * @param includeStopped true means included stopped task that not remove yet
     * @return running download task
     */
    private DownloadTask getRunningDownloadTask(String url, boolean includeStopped) {

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
            if (task != null) {
                if (!includeStopped) {
                    if (!task.isStopped()) {
                        return task;
                    }
                } else {
                    return task;
                }
            }
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
            onDetectBigUrlFileListener, DownloadConfiguration downloadConfiguration) {

        // ------------start checking conditions & notifying caller if necessary------------
        {
            DetectBigUrlFileFailReason failReason = null;// null means there are not errors

            // 1.check url
            if (!UrlUtil.isUrl(url)) {
                failReason = new DetectUrlFileFailReason(url, "url illegal !", DetectUrlFileFailReason
                        .TYPE_URL_ILLEGAL);
            }

            // 2.check network
            if (failReason == null && !NetworkUtil.isNetworkAvailable(mConfiguration.getContext())) {
                failReason = new DetectUrlFileFailReason(url, "network not available !", DetectUrlFileFailReason
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
        if (downloadConfiguration != null) {
            detectUrlFileTask.setRequestMethod(downloadConfiguration.getRequestMethod(url));
            // set headers
            detectUrlFileTask.setHeaders(downloadConfiguration.getHeaders(url));
        }
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
    private void addAndRunDownloadTask(final String callerUrl, DownloadFileInfo downloadFileInfo,
                                       DownloadConfiguration downloadConfiguration) {

        FileDownloadStatusFailReason failReason = null;// null means there are not errors

        // 1.check url
        if (!UrlUtil.isUrl(callerUrl)) {
            failReason = new OnFileDownloadStatusFailReason(callerUrl, "url illegal !",
                    OnFileDownloadStatusFailReason.TYPE_URL_ILLEGAL);
        }

        // 2.network check
        if (failReason == null && !NetworkUtil.isNetworkAvailable(mConfiguration.getContext())) {
            failReason = new OnFileDownloadStatusFailReason(callerUrl, "network not available !",
                    OnFileDownloadStatusFailReason.TYPE_NETWORK_DENIED);
        }

        // 3.check downloadFileInfo
        if (failReason == null) {
            if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
                failReason = new OnFileDownloadStatusFailReason(callerUrl, "the download file does not exist or " +
                        "illegal !", OnFileDownloadStatusFailReason.TYPE_DOWNLOAD_FILE_ERROR);
            }
            if (failReason == null) {
                if (TextUtils.isEmpty(callerUrl) || !callerUrl.equals(downloadFileInfo.getUrl()) || !downloadFileInfo
                        .equals(getDownloadFile(callerUrl))) {
                    failReason = new OnFileDownloadStatusFailReason(callerUrl, "the download file does not exist or "
                            + "illegal !", OnFileDownloadStatusFailReason.TYPE_DOWNLOAD_FILE_ERROR);
                }
            }
        }

        // 4.check download size
        if (failReason == null && downloadFileInfo.getDownloadedSizeLong() > downloadFileInfo.getFileSizeLong()) {
            failReason = new OnFileDownloadStatusFailReason(callerUrl, "download size illegal, please delete or " +
                    "re-download !", OnFileDownloadStatusFailReason.TYPE_DOWNLOAD_FILE_ERROR);
        }

        // error occur
        if (failReason != null) {
            // notify caller
            notifyDownloadStatusFailed(callerUrl, failReason, downloadFileInfo != null);
            return;
        }

        synchronized (mDownloadTaskLock) {
            // check the old task
            DownloadTask taskInMap = mRunningDownloadTaskMap.get(callerUrl);
            if (taskInMap != null) {
                // running, ignore
                Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
                Log.d(TAG, "mRunningDownloadTaskMap，忽略1：" + callerUrl + "，old task：" + taskInMap.hashCode() +
                        "，线程数：" + threads.size());
                return;
            }
        }

        // use global configuration first
        int retryDownloadTimes = mConfiguration.getRetryDownloadTimes();
        int connectTimeout = mConfiguration.getConnectTimeout();
        String requestMethod = DownloadConfiguration.DEFAULT_REQUEST_METHOD;
        Map<String, String> headers = null;

        if (downloadConfiguration != null) {
            int localRetryDownloadTimes = downloadConfiguration.getRetryDownloadTimes(callerUrl);
            if (localRetryDownloadTimes != Builder.DEFAULT_RETRY_DOWNLOAD_TIMES) {
                retryDownloadTimes = localRetryDownloadTimes;
            }
            int localConnectTimeout = downloadConfiguration.getConnectTimeout(callerUrl);
            if (localConnectTimeout != Builder.DEFAULT_CONNECT_TIMEOUT) {
                connectTimeout = localConnectTimeout;
            }
            String localRequestMethod = downloadConfiguration.getRequestMethod(callerUrl);
            if (TextUtils.isEmpty(localRequestMethod)) {
                localRequestMethod = DownloadConfiguration.DEFAULT_REQUEST_METHOD;
            }
            if (!TextUtils.isEmpty(localRequestMethod)) {
                requestMethod = localRequestMethod;
            }
            headers = downloadConfiguration.getHeaders(callerUrl);
        }

        // create retryable download task
        final RetryableDownloadTaskImpl downloadTask = new RetryableDownloadTaskImpl(FileDownloadTaskParam
                .createByDownloadFile(downloadFileInfo, requestMethod, headers), mDownloadRecorder,
                mDownloadStatusObserver);
        downloadTask.setCloseConnectionEngine(mConfiguration.getFileOperationEngine());
        // set RetryDownloadTimes
        downloadTask.setRetryDownloadTimes(retryDownloadTimes);
        downloadTask.setConnectTimeout(connectTimeout);
        downloadTask.setOnTaskRunFinishListener(new OnTaskRunFinishListener() {
            @Override
            public void onTaskRunFinish() {

                synchronized (mDownloadTaskLock) {
                    mRunningDownloadTaskMap.remove(downloadTask.getUrl());

                    Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
                    Log.e(TAG, "mRunningDownloadTaskMap，--移除--：" + downloadTask.getUrl() + "，task：" + downloadTask
                            .hashCode() + "，线程数：" + threads.size());
                }
            }
        });

        synchronized (mDownloadTaskLock) {

            // check the old task again
            DownloadTask taskInMap = mRunningDownloadTaskMap.get(downloadTask.getUrl());
            if (taskInMap != null) {
                // running, ignore
                Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
                Log.d(TAG, "mRunningDownloadTaskMap，忽略2：" + downloadTask.getUrl() + "，old task：" + taskInMap.hashCode
                        () + "，线程数：" + threads.size());
                return;
            }

            // record in the task map
            mRunningDownloadTaskMap.put(downloadTask.getUrl(), downloadTask);

            Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
            Log.d(TAG, "mRunningDownloadTaskMap，--增加--：" + downloadTask.getUrl() + "，task：" + downloadTask.hashCode()
                    + "，线程数：" + threads.size());
        }
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
        final String finalUrl = url;
        // check whether the download task of the url is running
        if (isDownloading(url)) {
            // pause task first
            pause(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {
                    // continue notify caller
                    notifyDownloadStatusFailedInternal(finalUrl, failReason, recordStatus);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason stopFailReason) {
                    // continue notify caller
                    FileDownloadStatusFailReason notifyFailReason = failReason == null ? new
                            OnFileDownloadStatusFailReason(finalUrl, stopFailReason) : failReason;
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
            onDetectBigUrlFileListener, DownloadConfiguration downloadConfiguration) {
        // start detect task
        addAndRunDetectUrlFileTask(url, forceDetect, onDetectBigUrlFileListener, downloadConfiguration);
    }

    /**
     * detect a url file
     *
     * @param url                     file url
     * @param onDetectUrlFileListener OnDetectUrlFileListener impl
     * @param downloadConfiguration   download configuration
     * @deprecated this method can not detect the url file which bigger than 2G, use {@link #detect(String,
     * OnDetectBigUrlFileListener, DownloadConfiguration)}instead
     */
    @Deprecated
    public void detect(String url, final OnDetectUrlFileListener onDetectUrlFileListener, DownloadConfiguration
            downloadConfiguration) {
        final String finalUrl = url;
        detect(finalUrl, new OnDetectBigUrlFileListener() {// inner change callback
            @Override
            public void onDetectNewDownloadFile(String url, String fileName, String saveDir, long fileSize) {
                // continue notify caller
                if (onDetectUrlFileListener != null) {
                    onDetectUrlFileListener.onDetectNewDownloadFile(finalUrl, fileName, saveDir, (int) fileSize);
                }
            }

            @Override
            public void onDetectUrlFileExist(String url) {
                // continue notify caller
                if (onDetectUrlFileListener != null) {
                    onDetectUrlFileListener.onDetectUrlFileExist(finalUrl);
                }
            }

            @Override
            public void onDetectUrlFileFailed(String url, DetectBigUrlFileFailReason failReason) {
                // continue notify caller
                if (onDetectUrlFileListener != null) {
                    onDetectUrlFileListener.onDetectUrlFileFailed(finalUrl, new DetectUrlFileFailReason(finalUrl,
                            failReason));
                }
            }
        }, downloadConfiguration);
    }

    /**
     * detect a big url file, which means can detect the url file bigger than 2G
     *
     * @param url                        file url
     * @param onDetectBigUrlFileListener OnDetectBigUrlFileListener impl
     * @param downloadConfiguration      download configuration
     */
    public void detect(String url, OnDetectBigUrlFileListener onDetectBigUrlFileListener, DownloadConfiguration
            downloadConfiguration) {
        detectInternal(url, false, onDetectBigUrlFileListener, downloadConfiguration);
    }

    // --------------------------------------create/continue downloads--------------------------------------

    /**
     * create a new download after detected a url file by using {@link #detect(String, OnDetectBigUrlFileListener,
     * DownloadConfiguration)}
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener, DownloadStatusConfiguration)}
     *
     * @param url                   file url
     * @param saveDir               saveDir
     * @param fileName              saveFileName
     * @param downloadConfiguration download configuration
     */
    public void createAndStart(String url, String saveDir, String fileName, DownloadConfiguration
            downloadConfiguration) {
        // 1.get detected file
        DetectUrlFileInfo detectUrlFileInfo = getDetectUrlFile(url);
        if (detectUrlFileInfo != null) {
            if (FileUtil.isFilePath(saveDir)) {
                detectUrlFileInfo.setFileDir(saveDir);
            }
            if (!TextUtils.isEmpty(fileName)) {
                detectUrlFileInfo.setFileName(fileName);
            }
            // check fileNamePath, may the same
            int index = checkFileName(detectUrlFileInfo.getFilePath());
            if (index > 0) {
                // need rename file name
                detectUrlFileInfo.setFileName(detectUrlFileInfo.getFileName() + index);
            }
        }

        // 2.create detect task
        createAndStartByDetectUrlFile(url, detectUrlFileInfo, downloadConfiguration);
    }

    private int checkFileName(String fileNamePath) {
        return checkFileNameInternal(fileNamePath, 0);
    }

    private int checkFileNameInternal(String fileNamePath, int index) {
        String checkFileNamePath = fileNamePath + (index > 0 ? index : "");
        if (!TextUtils.isEmpty(checkFileNamePath)) {
            List<DownloadFileInfo> downloadFileInfos = mDownloadRecorder.getDownloadFiles();
            if (!CollectionUtil.isEmpty(downloadFileInfos)) {
                boolean isFindSame = false;
                for (DownloadFileInfo info : downloadFileInfos) {
                    if (info == null) {
                        continue;
                    }
                    if (checkFileNamePath.equals(info.getFilePath())) {
                        // there is a same one
                        isFindSame = true;
                        break;
                    }
                }

                if (isFindSame) {
                    // recursion
                    return checkFileNameInternal(fileNamePath, ++index);
                } else {
                    return index;
                }
            }
        }
        return index;
    }

    /**
     * create download task by using DetectUrlFileInfo
     */
    private void createAndStartByDetectUrlFile(String callerUrl, DetectUrlFileInfo detectUrlFileInfo,
                                               DownloadConfiguration downloadConfiguration) {

        FileDownloadStatusFailReason failReason = null;// null means there are not errors

        // 1.check detectUrlFileInfo
        if (!DownloadFileUtil.isLegal(detectUrlFileInfo)) {
            failReason = new OnFileDownloadStatusFailReason(callerUrl, "detect file does not exist, please use detect" +
                    "(String," + "OnDetectBigUrlFileListener) first !", OnFileDownloadStatusFailReason
                    .TYPE_FILE_NOT_DETECT);
            if (failReason == null) {
                if (!UrlUtil.isUrl(callerUrl) || !callerUrl.equals(detectUrlFileInfo.getUrl())) {
                    failReason = new OnFileDownloadStatusFailReason(callerUrl, "detect file does not exist, please " +
                            "use detect" + "(String,OnDetectBigUrlFileListener) first !",
                            OnFileDownloadStatusFailReason.TYPE_FILE_NOT_DETECT);
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
        startInternal(callerUrl, downloadFileInfo, downloadConfiguration);
    }

    /**
     * start a download task
     */
    private void startInternal(String callerUrl, DownloadFileInfo downloadFileInfo, DownloadConfiguration
            downloadConfiguration) {
        // start a download task
        addAndRunDownloadTask(callerUrl, downloadFileInfo, downloadConfiguration);
    }

    /**
     * start/continue a download
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br/>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener, DownloadStatusConfiguration)}
     *
     * @param url                   file url
     * @param downloadConfiguration download configuration
     */
    public void start(String url, final DownloadConfiguration downloadConfiguration) {
        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        // has been downloaded
        if (downloadFileInfo != null) {
            // continue download task
            startInternal(url, downloadFileInfo, downloadConfiguration);
        }
        // not download
        else {
            DetectUrlFileInfo detectUrlFileInfo = getDetectUrlFile(url);
            // detected
            if (detectUrlFileInfo != null) {
                // create download task
                createAndStartByDetectUrlFile(url, detectUrlFileInfo, downloadConfiguration);
            }
            // not detect
            else {
                final String finalUrl = url;
                // detect first
                detect(finalUrl, new OnDetectBigUrlFileListener() {
                    @Override
                    public void onDetectUrlFileFailed(String url, DetectBigUrlFileFailReason failReason) {
                        // notify download status caller
                        notifyDownloadStatusFailed(finalUrl, new OnFileDownloadStatusFailReason(finalUrl, failReason)
                                , false);
                    }

                    @Override
                    public void onDetectUrlFileExist(String url) {
                        // continue download task
                        startInternal(finalUrl, getDownloadFile(finalUrl), downloadConfiguration);
                    }

                    @Override
                    public void onDetectNewDownloadFile(String url, String fileName, String savedDir, long fileSize) {
                        // create and start download
                        createAndStart(finalUrl, savedDir, fileName, downloadConfiguration);
                    }
                }, downloadConfiguration);
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
     * @param urls                  file urls
     * @param downloadConfiguration download configuration
     */
    public void start(List<String> urls, DownloadConfiguration downloadConfiguration) {
        for (String url : urls) {
            start(url, downloadConfiguration);
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
            StopDownloadFileTaskFailReason failReason = new StopDownloadFileTaskFailReason(finalUrl, "the download "
                    + "task has been paused !", StopDownloadFileTaskFailReason.TYPE_TASK_HAS_BEEN_STOPPED);

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
    private void reStartInternal(String url, final boolean isDelete, final DownloadConfiguration
            downloadConfiguration) {

        // 1.check url
        if (!UrlUtil.isUrl(url)) {
            FileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason(url, "url illegal !",
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
                        createAndStart(finalUrl, oldFileDir, oldFileName, downloadConfiguration);
                    } catch (Exception e) {
                        e.printStackTrace();
                        // TODO need a special exception ?
                        FileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason(finalUrl, e);
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
                        start(url, downloadConfiguration);
                    } catch (Exception e) {
                        e.printStackTrace();
                        // TODO need a special exception ?
                        FileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason(finalUrl, e);
                        // notify caller
                        notifyDownloadStatusFailed(finalUrl, failReason, getDownloadFile(finalUrl) != null);
                    }
                }

                @Override
                public void onDetectUrlFileFailed(String url, DetectBigUrlFileFailReason failReason) {
                    // notify caller
                    notifyDownloadStatusFailed(finalUrl, new OnFileDownloadStatusFailReason(finalUrl, failReason),
                            getDownloadFile(finalUrl) != null);
                }
            }, downloadConfiguration);
        } else {
            // download do not exist
            // start download
            start(url, downloadConfiguration);
        }
    }

    /**
     * restart a download
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener, DownloadStatusConfiguration)}
     *
     * @param url                   file url
     * @param downloadConfiguration download configuration
     */
    public void reStart(String url, final DownloadConfiguration downloadConfiguration) {
        // downloading, pause first
        if (isDownloading(url)) {
            final String finalUrl = url;
            // pause
            pause(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {
                    // stop succeed, restart
                    reStartInternal(finalUrl, true, downloadConfiguration);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason failReason) {

                    if (failReason != null) {
                        if (StopDownloadFileTaskFailReason.TYPE_TASK_HAS_BEEN_STOPPED.equals(failReason.getType())) {
                            // has been stopped, so can restart normally
                            reStartInternal(finalUrl, true, downloadConfiguration);
                            return;
                        }
                    }

                    // otherwise error occur, notify caller
                    notifyDownloadStatusFailed(finalUrl, failReason, getDownloadFile(finalUrl) != null);
                }
            });
        } else {
            // has been stopped, restart directly
            reStartInternal(url, true, downloadConfiguration);
        }
    }

    /**
     * restart multi downloads
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br/>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener, DownloadStatusConfiguration)}
     *
     * @param urls                  file urls
     * @param downloadConfiguration download configuration
     */
    public void reStart(List<String> urls, DownloadConfiguration downloadConfiguration) {
        for (String url : urls) {
            reStart(url, downloadConfiguration);
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
