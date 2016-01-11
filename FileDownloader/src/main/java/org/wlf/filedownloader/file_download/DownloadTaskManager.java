package org.wlf.filedownloader.file_download;

import android.text.TextUtils;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloadConfiguration;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.file_download.OnStopFileDownloadTaskListener.StopDownloadFileTaskFailReason;
import org.wlf.filedownloader.file_download.base.DownloadRecorder;
import org.wlf.filedownloader.file_download.base.DownloadTask;
import org.wlf.filedownloader.file_download.base.Pauseable;
import org.wlf.filedownloader.listener.OnDetectBigUrlFileListener;
import org.wlf.filedownloader.listener.OnDetectUrlFileListener;
import org.wlf.filedownloader.listener.OnDetectUrlFileListener.DetectUrlFileFailReason;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener.FileDownloadStatusFailReason;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener.OnFileDownloadStatusFailReason;
import org.wlf.filedownloader.listener.OnRetryableFileDownloadStatusListener;
import org.wlf.filedownloader.util.DownloadFileUtil;
import org.wlf.filedownloader.util.NetworkUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DownloadTaskManager，manage the download task
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
     * Configuration
     */
    private FileDownloadConfiguration mConfiguration;
    /**
     * DetectUrlFileCacher
     */
    private DetectUrlFileCacher mDetectUrlFileCacher;
    /**
     * DownloadFileRecorder
     */
    private DownloadRecorder mDownloadRecorder;
    /**
     * DownloadFileStatusObserver
     */
    private DownloadStatusObserver mDownloadStatusObserver;

    /**
     * all task under download,the download status will be waiting,preparing,prepared,downloading
     */
    private Map<String, DownloadTask> mDownloadTaskMap = new ConcurrentHashMap<String, DownloadTask>();

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
     * get download task by file url
     *
     * @param url file url
     * @return download task
     */
    private DownloadTask getDownloadTask(String url) {
        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo == null) {// not in cache,maybe the DownloadFileInfo not create,see DownloadTask in memory
            return mDownloadTaskMap.get(url);
        }
        // check status,only waiting,retrying,preparing,prepared,downloading status are available
        switch (downloadFileInfo.getStatus()) {
            case Status.DOWNLOAD_STATUS_WAITING:
            case Status.DOWNLOAD_STATUS_RETRYING:
            case Status.DOWNLOAD_STATUS_PREPARING:
            case Status.DOWNLOAD_STATUS_PREPARED:
            case Status.DOWNLOAD_STATUS_DOWNLOADING:
                DownloadTask task = mDownloadTaskMap.get(url);
                if (task != null && !task.isStopped()) {
                    return task;
                }
                break;
        }

        // status is not allowed in map,clear
        if (mDownloadTaskMap.containsKey(url)) {
            mDownloadTaskMap.remove(url);
        }
        return null;
    }

    // --------------------------------------register listeners--------------------------------------

    /**
     * register an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener,recommend to use {@link
     *                                     OnRetryableFileDownloadStatusListener} instead to support retrying
     *                                     download status
     * @param downloadStatusConfiguration  Configuration for FileDownloadStatusListener
     * @since 0.3.0
     */
    public void registerDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener, 
                                               DownloadStatusConfiguration downloadStatusConfiguration) {
        mDownloadStatusObserver.addOnFileDownloadStatusListener(onFileDownloadStatusListener, 
                downloadStatusConfiguration);
    }

    /**
     * register an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener,recommend to use {@link
     *                                     OnRetryableFileDownloadStatusListener} instead to support retrying
     *                                     download status
     */
    public void registerDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        registerDownloadStatusListener(onFileDownloadStatusListener, null);
    }

    /**
     * unregister an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener,recommend to use {@link
     *                                     OnRetryableFileDownloadStatusListener} instead to support retrying
     *                                     download status
     */
    public void unregisterDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        mDownloadStatusObserver.removeOnFileDownloadStatusListener(onFileDownloadStatusListener);
    }

    // --------------------------------------exec task--------------------------------------

    /**
     * start a detect url file task
     */
    private void addAndRunDetectUrlFileTask(String url, OnDetectUrlFileListener onDetectUrlFileListener) {

        DetectUrlFileFailReason failReason = null;// null means there are not errors

        // 1.check url
        if (!UrlUtil.isUrl(url)) {
            failReason = new DetectUrlFileFailReason("url illegal!", DetectUrlFileFailReason.TYPE_URL_ILLEGAL);
        }

        // 2.check network
        if (failReason == null && !NetworkUtil.isNetworkAvailable(mConfiguration.getContext())) {
            // notify caller
            failReason = new DetectUrlFileFailReason("network error!", DetectUrlFileFailReason.TYPE_NETWORK_DENIED);
        }

        // error occur
        if (failReason != null) {
            // notify caller
            notifyDetectUrlFileFailed(url, failReason, onDetectUrlFileListener);
            return;
        }

        // prepare detectUrlFileTask
        DetectUrlFileTask detectUrlFileTask = new DetectUrlFileTask(url, mConfiguration.getFileDownloadDir(), 
                mDetectUrlFileCacher, mDownloadRecorder);
        detectUrlFileTask.setOnDetectUrlFileListener(onDetectUrlFileListener);
        detectUrlFileTask.setCloseConnectionEngine(mConfiguration.getFileDetectEngine());

        // start detectUrlFileTask
        mConfiguration.getFileDetectEngine().execute(detectUrlFileTask);
    }

    /**
     * start download task
     */
    private void addAndRunDownloadTask(String callerUrl, DownloadFileInfo downloadFileInfo) {

        FileDownloadStatusFailReason failReason = null;// null means there are not errors

        // 1.check url
        if (!UrlUtil.isUrl(callerUrl)) {
            failReason = new FileDownloadStatusFailReason("url illegal!", FileDownloadStatusFailReason
                    .TYPE_URL_ILLEGAL);
        }

        // 2.network check
        if (failReason == null && !NetworkUtil.isNetworkAvailable(mConfiguration.getContext())) {
            failReason = new FileDownloadStatusFailReason("network error!", FileDownloadStatusFailReason
                    .TYPE_NETWORK_DENIED);
        }

        // 3.downloadFileInfo check
        if (failReason == null) {
            if (downloadFileInfo == null || !UrlUtil.isUrl(downloadFileInfo.getUrl())) {
                failReason = new FileDownloadStatusFailReason("the downloadFileInfo does not exist!", 
                        FileDownloadStatusFailReason.TYPE_DOWNLOAD_FILE_ERROR);
            }
            if (failReason == null) {
                if (TextUtils.isEmpty(callerUrl) || !callerUrl.equals(downloadFileInfo.getUrl()) || !downloadFileInfo
                        .equals(getDownloadFile(callerUrl))) {
                    failReason = new FileDownloadStatusFailReason("the downloadFileInfo does not exist!", 
                            FileDownloadStatusFailReason.TYPE_DOWNLOAD_FILE_ERROR);
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


        // create task

        //        DownloadTaskImpl downloadTask = new DownloadTaskImpl(FileDownloadTaskParam.createByDownloadFile
        //                (downloadFileInfo), mDownloadRecorder, onFileDownloadStatusListener);
        //        downloadTask.setCloseConnectionEngine(mConfiguration.getFileDetectEngine());

        RetryableDownloadTaskImpl downloadTask = new RetryableDownloadTaskImpl(FileDownloadTaskParam
                .createByDownloadFile(downloadFileInfo), mDownloadRecorder, mDownloadStatusObserver);
        downloadTask.setCloseConnectionEngine(mConfiguration.getFileDetectEngine());
        downloadTask.setRetryDownloadTimes(mConfiguration.getRetryDownloadTimes());

        // record in task map
        mDownloadTaskMap.put(downloadTask.getUrl(), downloadTask);
        // start exec task
        mConfiguration.getFileDownloadEngine().execute(downloadTask);
    }

    // --------------------------------------notify caller--------------------------------------

    /**
     * notify detect url file failed
     */
    private void notifyDetectUrlFileFailed(String url, DetectUrlFileFailReason failReason, OnDetectUrlFileListener 
            onDetectUrlFileListener) {
        // main thread notify caller
        OnDetectUrlFileListener.MainThreadHelper.onDetectUrlFileFailed(url, new DetectUrlFileFailReason("network " +
                "" + "error!", DetectUrlFileFailReason.TYPE_NETWORK_DENIED), onDetectUrlFileListener);
    }

    /**
     * notify download failed
     */
    private boolean notifyDownloadStatusFailed(String url, final FileDownloadStatusFailReason failReason, final 
    boolean recordStatus) {
        // check download task is running
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
            // notify caller
            return notifyDownloadStatusFailedInternal(url, failReason, recordStatus);
        }
    }

    /**
     * notify download failed
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
     * stop task succeed
     */
    private void notifyStopDownloadTaskSucceed(String url, OnStopFileDownloadTaskListener 
            onStopFileDownloadTaskListener) {
        // remove from the download task map
        mDownloadTaskMap.remove(url);
        // notify caller
        if (onStopFileDownloadTaskListener != null) {
            onStopFileDownloadTaskListener.onStopFileDownloadTaskSucceed(url);
        }
    }

    /**
     * stop task failed
     */
    private void notifyStopDownloadTaskFailed(String url, StopDownloadFileTaskFailReason failReason, 
                                              OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {
        // remove from the download task map
        mDownloadTaskMap.remove(url);
        // notify caller
        if (onStopFileDownloadTaskListener != null) {
            onStopFileDownloadTaskListener.onStopFileDownloadTaskFailed(url, failReason);
        }
    }

    /**
     * released
     */
    private void notifyReleased(OnReleaseListener onReleaseListener) {

        // clear cache
        mDetectUrlFileCacher.release();
        // observer release
        mDownloadStatusObserver.release();

        if (onReleaseListener != null) {
            onReleaseListener.onReleased();
        }
    }

    // --------------------------------------detect url file--------------------------------------

    /**
     * detect url file
     *
     * @param url                     file url
     * @param onDetectUrlFileListener DetectUrlFileListener,recommend to use {@link OnDetectBigUrlFileListener}
     *                                instead to support downloading the file more than 2G
     */
    public void detect(String url, OnDetectUrlFileListener onDetectUrlFileListener) {
        // run task
        addAndRunDetectUrlFileTask(url, onDetectUrlFileListener);
    }

    // --------------------------------------create/continue download--------------------------------------

    /**
     * create download task,please use {@link #detect(String, OnDetectUrlFileListener)} to detect url file first
     * <br/>
     * if the caller cares for the download status，please ues {@link #registerDownloadStatusListener
     * (OnFileDownloadStatusListener)} to register the callback
     *
     * @param url      file url
     * @param saveDir  saveDir
     * @param fileName saveFileName
     * @since 0.2.0
     */
    public void createAndStart(String url, String saveDir, String fileName) {
        // 1.get detect file
        DetectUrlFileInfo detectUrlFileInfo = getDetectUrlFile(url);
        if (detectUrlFileInfo != null) {
            detectUrlFileInfo.setFileDir(saveDir);
            detectUrlFileInfo.setFileName(fileName);
        }

        // 2.prepared to create task
        createAndStartByDetectUrlFile(url, detectUrlFileInfo);
    }

    // create download task by using detectUrlFileInfo
    private void createAndStartByDetectUrlFile(String callerUrl, DetectUrlFileInfo detectUrlFileInfo) {

        FileDownloadStatusFailReason failReason = null;// null means there are not errors

        // 1.detectUrlFileInfo check
        if (detectUrlFileInfo == null || !UrlUtil.isUrl(detectUrlFileInfo.getUrl())) {
            failReason = new FileDownloadStatusFailReason("detect file does not exist!", FileDownloadStatusFailReason
                    .TYPE_FILE_NOT_DETECT);
        }
        if (failReason == null) {
            if (!UrlUtil.isUrl(callerUrl) || !callerUrl.equals(detectUrlFileInfo.getUrl())) {
                failReason = new FileDownloadStatusFailReason("detect file does not exist!", 
                        FileDownloadStatusFailReason.TYPE_FILE_NOT_DETECT);
            }
        }

        // error occur
        if (failReason != null) {
            // notify caller
            notifyDownloadStatusFailed(callerUrl, failReason, false);
            return;
        }

        // 2.create new downloadFileInfo
        DownloadFileInfo downloadFileInfo = mDownloadRecorder.createDownloadFileInfo(detectUrlFileInfo);

        // 3.start download task
        startInternal(callerUrl, downloadFileInfo);
    }

    /**
     * start download task
     */
    private void startInternal(String callerUrl, DownloadFileInfo downloadFileInfo) {
        // start download task
        addAndRunDownloadTask(callerUrl, downloadFileInfo);
    }

    /**
     * start/continue download
     * <br/>
     * if the caller cares for the download status，please ues {@link #registerDownloadStatusListener
     * (OnFileDownloadStatusListener)} to register the callback
     *
     * @param url file url
     * @since 0.2.0
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
                // detect
                detect(finalUrl, new OnDetectBigUrlFileListener() {
                    @Override
                    public void onDetectUrlFileFailed(String url, DetectUrlFileFailReason failReason) {
                        // notify download caller
                        notifyDownloadStatusFailed(finalUrl, new OnFileDownloadStatusFailReason(failReason), false);
                    }

                    @Override
                    public void onDetectUrlFileExist(String url) {
                        // continue download task
                        startInternal(finalUrl, getDownloadFile(finalUrl));
                    }

                    @Override
                    public void onDetectNewDownloadFile(String url, String fileName, String savedDir, long fileSize) {
                        // create and start
                        createAndStart(finalUrl, savedDir, fileName);
                    }
                });
            }
        }
    }

    /**
     * start/continue multi download
     *
     * @param urls file Urls
     * @since 0.2.0
     */
    public void start(List<String> urls) {
        for (String url : urls) {
            start(url);
        }
    }

    // --------------------------------------pause download--------------------------------------

    /**
     * pause download task
     */
    private boolean pauseInternal(String url, final OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {

        // get download task info
        DownloadTask downloadTask = getDownloadTask(url);

        Log.d(TAG, "pauseInternal fileDownloadTask" + downloadTask + ",暂停url：" + url);

        final String finalUrl = url;

        // if the download task running, need to pause
        if (downloadTask != null && !downloadTask.isStopped()) {
            // set OnStopFileDownloadTaskListener
            downloadTask.setOnStopFileDownloadTaskListener(new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {

                    Log.d(TAG, "pauseInternal 暂停成功url：" + finalUrl);

                    // notify caller
                    notifyStopDownloadTaskSucceed(finalUrl, onStopFileDownloadTaskListener);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason failReason) {

                    Log.d(TAG, "pauseInternal 暂停失败url：" + finalUrl + ",failReason:" + failReason);

                    notifyStopDownloadTaskFailed(finalUrl, failReason, onStopFileDownloadTaskListener);
                }
            });
            // stop download
            downloadTask.stop();
            return true;
        } else {

            StopDownloadFileTaskFailReason failReason = new StopDownloadFileTaskFailReason("the task has been " + 
                    "paused!", StopDownloadFileTaskFailReason.TYPE_TASK_HAS_BEEN_STOPPED);

            Log.d(TAG, "pauseInternal 已经暂停url：" + url + ",failReason:" + failReason);

            // confirm the status is paused
            DownloadFileInfo downloadFileInfo = getDownloadFile(url);
            if (DownloadFileUtil.canPause(downloadFileInfo)) {
                try {
                    mDownloadRecorder.recordStatus(url, Status.DOWNLOAD_STATUS_PAUSED, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            notifyStopDownloadTaskFailed(url, failReason, onStopFileDownloadTaskListener);
            return false;
        }
    }

    @Override
    public boolean isDownloading(String url) {
        return getDownloadTask(url) != null;
    }

    @Override
    public void pause(String url, OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {
        pauseInternal(url, onStopFileDownloadTaskListener);
    }

    /**
     * pause multi download
     *
     * @param urls file mUrls
     */
    public void pause(List<String> urls, OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {
        for (String url : urls) {
            pause(url, onStopFileDownloadTaskListener);
        }
    }

    /**
     * pause all download
     *
     * @param onStopFileDownloadTaskListener
     */
    public void pauseAll(OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {
        Set<String> urls = mDownloadTaskMap.keySet();
        pause(new ArrayList<String>(urls), onStopFileDownloadTaskListener);
    }

    // --------------------------------------restart download--------------------------------------

    /**
     * restart download
     */
    private void reStartInternal(String url) {

        FileDownloadStatusFailReason failReason = null;// null means there are not errors

        // 1.check url
        if (!UrlUtil.isUrl(url)) {
            failReason = new FileDownloadStatusFailReason("url illegal!", FileDownloadStatusFailReason
                    .TYPE_URL_ILLEGAL);
        }

        // 2.update downloaded size if need
        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (failReason == null && downloadFileInfo != null) {
            try {
                mDownloadRecorder.resetDownloadFile(url);
            } catch (Exception e) {
                e.printStackTrace();
                failReason = new OnFileDownloadStatusFailReason(e);// TODO need a special exception ?
            }
        }

        if (failReason != null) {
            notifyDownloadStatusFailed(url, failReason, downloadFileInfo != null);
            return;
        }

        start(url);
    }


    /**
     * restart download
     * <br/>
     * if the caller cares for the download status，please ues {@link #registerDownloadStatusListener
     * (OnFileDownloadStatusListener)} to register the callback
     *
     * @param url file url
     * @since 0.2.0
     */
    public void reStart(String url) {
        // downloading, pause first
        if (isDownloading(url)) {
            final String finalUrl = url;
            // pause
            pause(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {
                    // stop succeed
                    reStartInternal(finalUrl);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason failReason) {

                    if (failReason != null) {
                        if (StopDownloadFileTaskFailReason.TYPE_TASK_HAS_BEEN_STOPPED.equals(failReason.getType())) {
                            // has been stopped, so can restart normally
                            reStartInternal(finalUrl);
                            return;
                        }
                    }

                    // other error
                    notifyDownloadStatusFailed(finalUrl, failReason, getDownloadFile(finalUrl) != null);
                }
            });
        } else {
            // has been stopped
            reStartInternal(url);
        }
    }

    /**
     * restart multi download
     * <br/>
     * if the caller cares for the download status，please ues {@link #registerDownloadStatusListener
     * (OnFileDownloadStatusListener)} to register the callback
     *
     * @param urls file urls
     * @since 0.2.0
     */
    public void reStart(List<String> urls) {
        for (String url : urls) {
            reStart(url);
        }
    }

    /**
     * release resources
     *
     * @param onReleaseListener the listener for listening release completed
     */
    public void release(final OnReleaseListener onReleaseListener) {

        final Set<String> urls = mDownloadTaskMap.keySet();

        pause(new ArrayList<String>(urls), new OnStopFileDownloadTaskListener() {

            private List<String> succeed = new ArrayList<String>();
            private List<String> failed = new ArrayList<String>();

            @Override
            public void onStopFileDownloadTaskSucceed(String url) {
                succeed.add(url);
                if (urls.size() == succeed.size() + failed.size()) {
                    // notify caller
                    notifyReleased(onReleaseListener);
                }
            }

            @Override
            public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason failReason) {
                failed.add(url);
                if (urls.size() == succeed.size() + failed.size()) {
                    // notify caller
                    notifyReleased(onReleaseListener);
                }
            }
        });
    }

    /**
     * OnReleaseListener
     */
    public interface OnReleaseListener {
        void onReleased();
    }
}
