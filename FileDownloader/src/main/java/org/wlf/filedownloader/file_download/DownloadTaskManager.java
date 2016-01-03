package org.wlf.filedownloader.file_download;

import android.text.TextUtils;
import android.util.Log;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloadConfiguration;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.file_download.FileDownloadTask.OnStopDownloadFileTaskFailReason;
import org.wlf.filedownloader.file_download.FileDownloadTask.OnStopFileDownloadTaskListener;
import org.wlf.filedownloader.file_download.db_recorder.DownloadFileDbRecorder;
import org.wlf.filedownloader.listener.OnDetectBigUrlFileListener;
import org.wlf.filedownloader.listener.OnDetectUrlFileListener;
import org.wlf.filedownloader.listener.OnDetectUrlFileListener.DetectUrlFileFailReason;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener.FileDownloadStatusFailReason;
import org.wlf.filedownloader.util.NetworkUtil;

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
public class DownloadTaskManager {

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
    private Map<String, FileDownloadTask> mFileDownloadTaskMap = new ConcurrentHashMap<String, FileDownloadTask>();

    //  constructor of DownloadTaskManager
    public DownloadTaskManager(FileDownloadConfiguration configuration, DownloadRecorder downloadRecorder) {

        this.mConfiguration = configuration;
        this.mDownloadRecorder = downloadRecorder;

        // init DetectUrlFileCacher
        mDetectUrlFileCacher = new DetectUrlFileCacher();
        // init DownloadFileStatusObserver
        mDownloadStatusObserver = new DownloadStatusObserver();
    }

    /**
     * whether is in download task map
     *
     * @param url file url
     * @return true means the download task map contains the file url task
     */
    public boolean isInFileDownloadTaskMap(String url) {
        return getFileDownloadTask(url) != null;
    }

    /**
     * get download task by file url
     *
     * @param url file url
     * @return download task
     */
    private FileDownloadTask getFileDownloadTask(String url) {
        DownloadFileInfo downloadFileInfo = mDownloadRecorder.getDownloadFile(url);
        if (downloadFileInfo == null) {// not in cache,may in memory
            return mFileDownloadTaskMap.get(url);
        }
        // check status,only waiting,preparing,prepared,downloading status are available
        switch (downloadFileInfo.getStatus()) {
            case Status.DOWNLOAD_STATUS_WAITING:
            case Status.DOWNLOAD_STATUS_PREPARING:
            case Status.DOWNLOAD_STATUS_PREPARED:
            case Status.DOWNLOAD_STATUS_DOWNLOADING:
                FileDownloadTask task = mFileDownloadTaskMap.get(url);
                if (task != null && !task.isStopped()) {
                    return task;
                }
                break;
        }

        // status is not allowed in map,clear
        if (mFileDownloadTaskMap.containsKey(url)) {
            mFileDownloadTaskMap.remove(url);
        }
        return null;
    }

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
     * register an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener the OnFileDownloadStatusListener impl
     * @param downloadStatusConfiguration
     */
    public void registerDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener, 
                                               DownloadStatusConfiguration downloadStatusConfiguration) {
        mDownloadStatusObserver.addOnFileDownloadStatusListener(onFileDownloadStatusListener, 
                downloadStatusConfiguration);
    }

    /**
     * register an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener the OnFileDownloadStatusListener impl
     */
    public void registerDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        registerDownloadStatusListener(onFileDownloadStatusListener, null);
    }

    /**
     * unregister an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener the OnFileDownloadStatusListener impl
     */
    public void unregisterDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        mDownloadStatusObserver.removeOnFileDownloadStatusListener(onFileDownloadStatusListener);
    }

    /**
     * start a detect url file task
     */
    private void addAndRunDetectUrlFileTask(Runnable task) {
        // exec a detect url file task
        mConfiguration.getFileDetectEngine().execute(task);
    }

    /**
     * start download task
     */
    private void addAndRunFileDownloadTask(DownloadFileInfo downloadFileInfo, OnFileDownloadStatusListener 
            onFileDownloadStatusListener) {
        if (!NetworkUtil.isNetworkAvailable(mConfiguration.getContext())) {
            if (onFileDownloadStatusListener != null) {
                String url = "";
                if (downloadFileInfo != null) {
                    url = downloadFileInfo.getUrl();
                }
                onFileDownloadStatusListener.onFileDownloadStatusFailed(url, downloadFileInfo, new FileDownloadStatusFailReason("network error!", FileDownloadStatusFailReason
                        .TYPE_NETWORK_DENIED));
                return;
            }
        }
        // create task
        FileDownloadTask fileDownloadTask = new FileDownloadTask(FileDownloadTask.createByDownloadFile(downloadFileInfo), mDownloadRecorder, onFileDownloadStatusListener);
        // record task map
        mFileDownloadTaskMap.put(fileDownloadTask.getUrl(), fileDownloadTask);
        // start exec task
        mConfiguration.getFileDownloadEngine().execute(fileDownloadTask);
    }

    /**
     * notify download failed with task check
     */
    private void notifyFileDownloadStatusFailedWithCheck(final String url, final FileDownloadStatusFailReason 
            failReason, final OnFileDownloadStatusListener onFileDownloadStatusListener, final boolean recordStatus) {
        final DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        // null
        if (downloadFileInfo == null) {
            // notify callback
            if (onFileDownloadStatusListener != null) {
                OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(url, downloadFileInfo, new FileDownloadStatusFailReason(failReason), onFileDownloadStatusListener);
            }
            return;
        }

        final String downloadUrl = downloadFileInfo.getUrl();

        // downloading,pause first
        if (isInFileDownloadTaskMap(downloadUrl)) {
            // pause
            FileDownloadTask fileDownloadTask = getFileDownloadTask(downloadUrl);
            if (fileDownloadTask != null) {
                fileDownloadTask.setOnStopFileDownloadTaskListener(new OnStopFileDownloadTaskListener() {
                    @Override
                    public void onStopFileDownloadTaskSucceed(String url) {
                        // notify
                        notifyFileDownloadStatusFailed(downloadUrl, downloadFileInfo, failReason, 
                                onFileDownloadStatusListener, recordStatus);
                    }

                    @Override
                    public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
                        // notify
                        notifyFileDownloadStatusFailed(downloadUrl, downloadFileInfo, new FileDownloadStatusFailReason(failReason), onFileDownloadStatusListener, recordStatus);
                    }
                });

                // pause
                fileDownloadTask.stop();
            } else {
                // notify
                notifyFileDownloadStatusFailed(downloadUrl, downloadFileInfo, failReason, 
                        onFileDownloadStatusListener, recordStatus);
            }
        } else {
            // notify
            notifyFileDownloadStatusFailed(downloadUrl, downloadFileInfo, failReason, onFileDownloadStatusListener, 
                    recordStatus);
        }
    }

    /**
     * notify download failed
     */
    private void notifyFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, FileDownloadStatusFailReason failReason, OnFileDownloadStatusListener onFileDownloadStatusListener, boolean recordStatus) {
        if (recordStatus) {
            // record status
            try {
                mDownloadRecorder.recordStatus(url, Status.DOWNLOAD_STATUS_ERROR, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // notify callback
        if (onFileDownloadStatusListener != null) {
            OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(url, downloadFileInfo, 
                    failReason, onFileDownloadStatusListener);
        }
    }

    /**
     * on DownloadTask Stopped
     */
    private void onFileDownloadTaskStopped(String url) {
        // remove from the download task map
        mFileDownloadTaskMap.remove(url);
    }

    /**
     * release resources
     *
     * @param onReleaseListener the listener for listening release completed
     */
    public void release(final OnReleaseListener onReleaseListener) {

        final Set<String> urls = mFileDownloadTaskMap.keySet();

        pause(new ArrayList<String>(urls), new OnStopFileDownloadTaskListener() {

            private List<String> succeed = new ArrayList<String>();
            private List<String> failed = new ArrayList<String>();

            @Override
            public void onStopFileDownloadTaskSucceed(String url) {
                succeed.add(url);
                if (urls.size() == succeed.size() + failed.size()) {
                    clear();
                    if (onReleaseListener != null) {
                        onReleaseListener.onReleased();
                    }
                }
            }

            @Override
            public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
                failed.add(url);
                if (urls.size() == succeed.size() + failed.size()) {
                    clear();
                    if (onReleaseListener != null) {
                        onReleaseListener.onReleased();
                    }
                }
            }
        });
    }

    /**
     * clear
     */
    private void clear() {
        // clear cache
        mDetectUrlFileCacher.release();
        // observer release
        mDownloadStatusObserver.release();
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
        if (!NetworkUtil.isNetworkAvailable(mConfiguration.getContext())) {
            if (onDetectUrlFileListener != null) {
                onDetectUrlFileListener.onDetectUrlFileFailed(url, new DetectUrlFileFailReason("network error!", 
                        DetectUrlFileFailReason.TYPE_NETWORK_DENIED));
                return;
            }
        }
        // 1.prepare detectUrlFileTask
        DetectUrlFileTask detectUrlFileTask = new DetectUrlFileTask(url, mConfiguration.getFileDownloadDir(), mDetectUrlFileCacher, mDownloadRecorder);
        detectUrlFileTask.setOnDetectUrlFileListener(onDetectUrlFileListener);
        // 2.start detectUrlFileTask
        addAndRunDetectUrlFileTask(detectUrlFileTask);
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
        if (detectUrlFileInfo == null) {
            // error detect file does not exist
            FileDownloadStatusFailReason failReason = new FileDownloadStatusFailReason("detect file does not " + "exist!", FileDownloadStatusFailReason.TYPE_FILE_NOT_DETECT);
            // notifyFileDownloadStatusFailedWithCheck
            notifyFileDownloadStatusFailedWithCheck(url, failReason, mDownloadStatusObserver, false);
            return;
        }

        // 2.prepared to create task
        detectUrlFileInfo.setFileDir(saveDir);
        detectUrlFileInfo.setFileName(fileName);
        createAndStartByDetectUrlFile(url, detectUrlFileInfo, mDownloadStatusObserver);
    }

    /**
     * create download task,please use {@link #detect(String, OnDetectUrlFileListener)} to detect url file first
     *
     * @param url                          file url
     * @param saveDir                      saveDir
     * @param fileName                     saveFileName
     * @param onFileDownloadStatusListener FileDownloadStatusListener
     * @deprecated use {@link #createAndStart(String, String, String)} instead
     */
    @Deprecated
    public void createAndStart(String url, String saveDir, String fileName, final OnFileDownloadStatusListener onFileDownloadStatusListener) {
        registerDownloadStatusListener(onFileDownloadStatusListener);
        createAndStart(url, saveDir, fileName);
    }

    // create download task by using detectUrlFileInfo
    private void createAndStartByDetectUrlFile(String url, DetectUrlFileInfo detectUrlFileInfo, 
                                               OnFileDownloadStatusListener onFileDownloadStatusListener) {
        // 1.check detectUrlFileInfo
        if (detectUrlFileInfo == null) {
            // error detect file does not exist
            FileDownloadStatusFailReason failReason = new FileDownloadStatusFailReason("detect file does not " + "exist!", FileDownloadStatusFailReason.TYPE_FILE_NOT_DETECT);
            // notifyFileDownloadStatusFailedWithCheck
            notifyFileDownloadStatusFailedWithCheck(url, failReason, onFileDownloadStatusListener, false);
            return;
        }

        String downloadUrl = detectUrlFileInfo.getUrl();

        // 2.whether task is in download map
        if (isInFileDownloadTaskMap(downloadUrl)) {
            // downloading,ignore
            // OnFileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason("the task has been in " + 
            //       "download task map!", OnFileDownloadStatusFailReason.TYPE_FILE_IS_DOWNLOADING);
            // notifyFileDownloadStatusFailedWithCheck
            // notifyFileDownloadStatusFailedWithCheck(downloadUrl, failReason, onFileDownloadStatusListener, false);
            return;
        }

        // 3.create downloadFileInfo
        // DownloadFileInfo downloadFileInfo = new DownloadFileInfo(detectUrlFileInfo);
        // add to cache
        // mDownloadFileRecorder.addDownloadFile(downloadFileInfo);
        DownloadFileInfo downloadFileInfo = mDownloadRecorder.createDownloadFileInfo(detectUrlFileInfo);

        // 4.start download task
        startInternal(downloadUrl, downloadFileInfo, onFileDownloadStatusListener);
    }

    /**
     * start download task
     */
    private void startInternal(String url, DownloadFileInfo downloadFileInfo, OnFileDownloadStatusListener 
            onFileDownloadStatusListener) {

        FileDownloadStatusFailReason failReason = null;// null means there are not errors

        // 1.check downloadFileInfo
        if (downloadFileInfo == null) {
            if (failReason == null) {
                // error the downloadFileInfo does not exist
                failReason = new FileDownloadStatusFailReason("the downloadFileInfo does not exist!", FileDownloadStatusFailReason.TYPE_NULL_POINTER);
            }
        } else {
            String downloadUrl = downloadFileInfo.getUrl();
            // 2.whether task is in download map
            if (failReason == null && isInFileDownloadTaskMap(downloadUrl)) {
                // downloading,ignore
                // failReason = new OnFileDownloadStatusFailReason("the task has been in download task map!", 
                //        OnFileDownloadStatusFailReason.TYPE_FILE_IS_DOWNLOADING);
                // url = downloadUrl;
                return;
            }
        }

        // error
        if (failReason != null) {
            // notifyFileDownloadStatusFailedWithCheck
            notifyFileDownloadStatusFailedWithCheck(url, failReason, onFileDownloadStatusListener, false);
            return;
        }

        // 2.start download task
        addAndRunFileDownloadTask(downloadFileInfo, onFileDownloadStatusListener);
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
            startInternal(downloadFileInfo.getUrl(), downloadFileInfo, mDownloadStatusObserver);
        }
        // not download
        else {
            DetectUrlFileInfo detectUrlFileInfo = getDetectUrlFile(url);
            // detected
            if (detectUrlFileInfo != null) {
                // create download task
                createAndStartByDetectUrlFile(detectUrlFileInfo.getUrl(), detectUrlFileInfo, mDownloadStatusObserver);
            }
            // not detect
            else {
                // detect
                detect(url, new OnDetectBigUrlFileListener() {
                    @Override
                    public void onDetectUrlFileFailed(String url, DetectUrlFileFailReason failReason) {
                        // 1.notifyFileDownloadStatusFailedWithCheck
                        notifyFileDownloadStatusFailedWithCheck(url, new FileDownloadStatusFailReason(failReason), mDownloadStatusObserver, false);
                    }

                    @Override
                    public void onDetectUrlFileExist(String url) {
                        // continue download task
                        startInternal(url, getDownloadFile(url), mDownloadStatusObserver);
                    }

                    @Override
                    public void onDetectNewDownloadFile(String url, String fileName, String savedDir, long fileSize) {
                        // create and start
                        createAndStart(url, savedDir, fileName);
                    }
                });
            }
        }
    }

    /**
     * start/continue download
     *
     * @param url                          file url
     * @param onFileDownloadStatusListener FileDownloadStatusListener
     * @deprecated use {@link #start(String)} instead
     */
    @Deprecated
    public void start(String url, final OnFileDownloadStatusListener onFileDownloadStatusListener) {
        registerDownloadStatusListener(onFileDownloadStatusListener);
        start(url);
    }

    /**
     * start/continue multi download
     *
     * @param urls file Urls
     * @since 0.2.0
     */
    public void start(List<String> urls) {
        for (String url : urls) {
            if (TextUtils.isEmpty(url)) {
                continue;
            }
            start(url);
        }
    }

    /**
     * start/continue multi download
     *
     * @param urls                         file Urls
     * @param onFileDownloadStatusListener FileDownloadStatusListener
     * @deprecated use {@link #start(String)} instead
     */
    @Deprecated
    public void start(List<String> urls, OnFileDownloadStatusListener onFileDownloadStatusListener) {
        registerDownloadStatusListener(onFileDownloadStatusListener);
        start(urls);
    }

    // --------------------------------------pause download--------------------------------------

    /**
     * pause download task
     */
    private boolean pauseInternal(String url, final OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {

        // get download task info
        FileDownloadTask fileDownloadTask = getFileDownloadTask(url);
        // in the download task,need to pause
        if (fileDownloadTask != null) {
            // set OnStopFileDownloadTaskListener
            fileDownloadTask.setOnStopFileDownloadTaskListener(new OnStopFileDownloadTaskListener() {

                @Override
                public void onStopFileDownloadTaskSucceed(String url) {

                    Log.d(TAG, "pauseInternal 暂停成功url：" + url);

                    onFileDownloadTaskStopped(url);
                    if (onStopFileDownloadTaskListener != null) {
                        onStopFileDownloadTaskListener.onStopFileDownloadTaskSucceed(url);
                    }
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {

                    Log.d(TAG, "pauseInternal 暂停失败url：" + url + ",failReason:" + failReason);

                    if (onStopFileDownloadTaskListener != null) {
                        onStopFileDownloadTaskListener.onStopFileDownloadTaskFailed(url, failReason);
                    }
                }
            });
            // stop download
            fileDownloadTask.stop();
            return true;
        } else {

            OnStopDownloadFileTaskFailReason failReason = new OnStopDownloadFileTaskFailReason("the task has been " +
                    "paused!", OnStopDownloadFileTaskFailReason.TYPE_TASK_IS_STOPPED);

            Log.d(TAG, "pauseInternal 已经暂停url：" + url + ",failReason:" + failReason);

            // confirm the status is paused
            DownloadFileInfo downloadFileInfo = getDownloadFile(url);
            if (downloadFileInfo != null) {
                switch (downloadFileInfo.getStatus()) {
                    case Status.DOWNLOAD_STATUS_WAITING:
                    case Status.DOWNLOAD_STATUS_PREPARING:
                    case Status.DOWNLOAD_STATUS_PREPARED:
                        //downloadFileInfo.setStatus(Status.DOWNLOAD_STATUS_PAUSED);
                        // mDownloadFileCacher.updateDownloadFile(downloadFileInfo);
                        try {
                            mDownloadRecorder.recordStatus(url, Status.DOWNLOAD_STATUS_PAUSED, 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }

            if (onStopFileDownloadTaskListener != null) {
                onStopFileDownloadTaskListener.onStopFileDownloadTaskFailed(url, failReason);
            }
            return false;
        }
    }

    /**
     * pause download
     *
     * @param url                            file url
     * @param onStopFileDownloadTaskListener
     */
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
            if (TextUtils.isEmpty(url)) {
                continue;
            }
            pause(url, onStopFileDownloadTaskListener);
        }
    }

    /**
     * pause all download
     *
     * @param urlsWillPause
     * @param onStopFileDownloadTaskListener
     */
    public void pauseAll(List<String> urlsWillPause, OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {
        Set<String> urls = mFileDownloadTaskMap.keySet();
        if (urlsWillPause != null) {
            urlsWillPause.addAll(urls);
            pause(urlsWillPause, onStopFileDownloadTaskListener);
        } else {
            pause(new ArrayList<String>(urls), onStopFileDownloadTaskListener);
        }
    }

    // --------------------------------------restart download--------------------------------------

    /**
     * restart download
     */
    private void reStartInternal(String url) {
        // update downloaded size if need
        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo != null) {
            // downloadFileInfo.setDownloadedSize(0);
            // downloadFileInfo.setFileDir(mConfiguration.getFileDownloadDir());
            // mDownloadFileCacher.updateDownloadFile(downloadFileInfo);
            try {
                mDownloadRecorder.resetDownloadFile(downloadFileInfo.getUrl());
            } catch (Exception e) {
                e.printStackTrace();
                // TODO need notify callback ?
            }
            //mDownloadFileRecorder.resetDownloadFile(downloadFileInfo);
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

        // downloading
        if (isInFileDownloadTaskMap(url)) {
            // pause
            pauseInternal(url, new OnStopFileDownloadTaskListener() {

                @Override
                public void onStopFileDownloadTaskSucceed(String url) {
                    reStartInternal(url);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
                    if (failReason != null) {
                        if (OnStopDownloadFileTaskFailReason.TYPE_TASK_IS_STOPPED.equals(failReason.getType())) {
                            // has been stopped
                            reStartInternal(url);
                        } else {
                            // error
                            notifyFileDownloadStatusFailedWithCheck(url, new FileDownloadStatusFailReason(failReason), mDownloadStatusObserver, false);
                        }
                    } else {
                        // error
                        notifyFileDownloadStatusFailedWithCheck(url, new FileDownloadStatusFailReason(failReason), 
                                mDownloadStatusObserver, false);
                    }
                }
            });
        } else {
            reStartInternal(url);
        }
    }

    /**
     * restart download
     *
     * @param url                          file url
     * @param onFileDownloadStatusListener FileDownloadStatusListener
     * @deprecated use {@link #reStart(String)} instead
     */
    @Deprecated
    public void reStart(String url, final OnFileDownloadStatusListener onFileDownloadStatusListener) {
        registerDownloadStatusListener(onFileDownloadStatusListener);
        reStart(url);
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
            if (TextUtils.isEmpty(url)) {
                continue;
            }
            reStart(url);
        }
    }

    /**
     * restart multi download
     *
     * @param urls                         file mUrls
     * @param onFileDownloadStatusListener FileDownloadStatusListener
     * @deprecated use {@link #reStart(String)} instead
     */
    @Deprecated
    public void reStart(List<String> urls, OnFileDownloadStatusListener onFileDownloadStatusListener) {
        registerDownloadStatusListener(onFileDownloadStatusListener);
        reStart(urls);
    }

    /**
     * OnReleaseListener
     */
    public interface OnReleaseListener {
        void onReleased();
    }

    public interface DownloadRecorder extends DownloadFileDbRecorder {

        /**
         * record download file
         *
         * @param url
         * @throws Exception any exception during record
         */
        void resetDownloadFile(String url) throws Exception;

        DownloadFileInfo createDownloadFileInfo(DetectUrlFileInfo detectUrlFileInfo);
    }
}
