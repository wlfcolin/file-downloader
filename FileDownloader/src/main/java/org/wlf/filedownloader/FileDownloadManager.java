package org.wlf.filedownloader;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.wlf.filedownloader.DownloadFileCacher.DownloadStatusRecordException;
import org.wlf.filedownloader.FileDownloadTask.OnStopDownloadFileTaskFailReason;
import org.wlf.filedownloader.FileDownloadTask.OnStopFileDownloadTaskListener;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.base.Stoppable;
import org.wlf.filedownloader.helper.FileDownloadTaskParamHelper;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener.OnDeleteDownloadFileFailReason;
import org.wlf.filedownloader.listener.OnDeleteDownloadFilesListener;
import org.wlf.filedownloader.listener.OnDetectUrlFileListener;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener.OnFileDownloadStatusFailReason;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener.OnMoveDownloadFileFailReason;
import org.wlf.filedownloader.listener.OnMoveDownloadFilesListener;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener.OnRenameDownloadFileFailReason;
import org.wlf.filedownloader.listener.OnSyncMoveDownloadFileListener;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FileDownload Manager
 * <br/>
 * 文件下载管理器
 *
 * @author wlf
 */
public class FileDownloadManager {

    /**
     * LOG TAG
     */
    private static final String TAG = FileDownloadManager.class.getSimpleName();

    /**
     * single instance
     */
    private static FileDownloadManager sInstance;

    /**
     * Configuration
     */
    private FileDownloadConfiguration mConfiguration;

    /**
     * DetectUrlFileCacher
     */
    private DetectUrlFileCacher mDetectUrlFileCacher;
    /**
     * DownloadFileCacher
     */
    private DownloadFileCacher mDownloadFileCacher;

    /**
     * all task under download,the download status will be waiting,preparing,prepared,downloading
     */
    private Map<String, FileDownloadTask> mFileDownloadTaskMap = new ConcurrentHashMap<String, FileDownloadTask>();
    /**
     * the task for delete multi DownloadFiles
     */
    private DeleteDownloadFilesTask mDeleteDownloadFilesTask = null;
    /**
     * the task for move multi DownloadFiles
     */
    private MoveDownloadFilesTask mMoveDownloadFilesTask = null;

    private OnFileDownloadStatusListener mInternalDownloadStatusListenerImpl;

    private Set<WeakReference<OnFileDownloadStatusListener>> mWeakOnFileDownloadStatusListeners = new 
            HashSet<WeakReference<OnFileDownloadStatusListener>>();

    /**
     * init lock
     */
    private Object mInitLock = new Object();

    //  constructor of FileDownloadManager,private only
    private FileDownloadManager(Context context) {
        Context appContext = context.getApplicationContext();
        // init DetectUrlFileCacher
        mDetectUrlFileCacher = new DetectUrlFileCacher();
        // init DownloadFileCacher
        mDownloadFileCacher = new DownloadFileCacher(appContext);
        // check the download status,if there is an exceptionState,try to recover it
        exceptionStateRecovery(getDownloadFiles());

        mInternalDownloadStatusListenerImpl = new OnFileDownloadStatusListenerImpl();
    }

    /**
     * get single instance
     *
     * @param context Context
     * @return single instance
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
     * init Configuration
     *
     * @param configuration Configuration
     */
    public void init(FileDownloadConfiguration configuration) {
        synchronized (mInitLock) {
            this.mConfiguration = configuration;
        }
    }

    /**
     * whether is Configuration initialized
     *
     * @return true means initialized
     */
    public boolean isInit() {
        synchronized (mInitLock) {
            return mConfiguration != null;
        }
    }

    /**
     * check whether is Configuration initialized
     */
    private void checkInit() {
        if (!isInit()) {
            throw new IllegalStateException("please init " + FileDownloadManager.class.getSimpleName());
        }
    }

    /**
     * try to recover exceptionStates
     */
    private void exceptionStateRecovery(List<DownloadFileInfo> downloadFileInfos) {

        Log.i(TAG, "exceptionStateRecovery 异常恢复检查！");

        if (CollectionUtil.isEmpty(downloadFileInfos)) {
            return;
        }

        for (DownloadFileInfo downloadFileInfo : downloadFileInfos) {
            if (downloadFileInfo == null) {
                continue;
            }

            String url = downloadFileInfo.getUrl();

            // if in the task map,ignore
            if (isInFileDownloadTaskMap(url)) {
                continue;
            }
            // check
            switch (downloadFileInfo.getStatus()) {
                case Status.DOWNLOAD_STATUS_WAITING:
                case Status.DOWNLOAD_STATUS_PREPARING:
                case Status.DOWNLOAD_STATUS_PREPARED:
                case Status.DOWNLOAD_STATUS_DOWNLOADING:
                    // recover paused
                    try {
                        mDownloadFileCacher.recordStatus(url, Status.DOWNLOAD_STATUS_PAUSED, 0);
                    } catch (DownloadStatusRecordException e) {
                        e.printStackTrace();
                    }
                    break;
                case Status.DOWNLOAD_STATUS_COMPLETED:
                case Status.DOWNLOAD_STATUS_PAUSED:
                case Status.DOWNLOAD_STATUS_ERROR:
                    // ignore
                    break;
                case Status.DOWNLOAD_STATUS_UNKNOWN:
                default:
                    // recover error
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
     * whether is in download task map
     *
     * @param url file url
     * @return true means the download task map contains the file url task
     */
    private boolean isInFileDownloadTaskMap(String url) {
        return getFileDownloadTask(url) != null;
    }

    /**
     * get download task by file url
     *
     * @param url file url
     * @return download task
     */
    private FileDownloadTask getFileDownloadTask(String url) {
        DownloadFileInfo downloadFileInfo = mDownloadFileCacher.getDownloadFile(url);
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
        return mDownloadFileCacher.getDownloadFile(url);
    }

    /**
     * get DownloadFile by file url
     *
     * @param url file url
     * @return DownloadFile
     */
    public DownloadFileInfo getDownloadFileByUrl(String url) {
        return getDownloadFile(url);
    }

    /**
     * get DownloadFile by save file path
     *
     * @param savePath save file path
     * @return DownloadFile
     */
    public DownloadFileInfo getDownloadFileBySavePath(String savePath) {
        return mDownloadFileCacher.getDownloadFileBySavePath(savePath, false);
    }

    /**
     * get DownloadFile by save file path
     *
     * @param savePath            save file path
     * @param includeTempFilePath true means try use the savePath as temp file savePath if can not get DownloadFile
     *                            by savePath
     * @return DownloadFile
     */
    public DownloadFileInfo getDownloadFileBySavePath(String savePath, boolean includeTempFilePath) {
        return mDownloadFileCacher.getDownloadFileBySavePath(savePath, includeTempFilePath);
    }

    /**
     * register an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener the OnFileDownloadStatusListener impl
     */
    public void registerDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
            if (weakReference == null) {
                continue;
            }
            OnFileDownloadStatusListener weakListener = weakReference.get();
            if (weakListener == null) {
                continue;
            }
            if (weakListener == onFileDownloadStatusListener) {
                return;// has been added
            }
        }
        WeakReference<OnFileDownloadStatusListener> weakReference = new WeakReference<OnFileDownloadStatusListener>
                (onFileDownloadStatusListener);
        // add listeners weakly
        mWeakOnFileDownloadStatusListeners.add(weakReference);
    }


    /**
     * unregister an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener the OnFileDownloadStatusListener impl
     */
    public void unregisterDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
            if (weakReference == null) {
                continue;
            }
            OnFileDownloadStatusListener weakListener = weakReference.get();
            if (weakListener == null) {
                // not need to remove may has been removed
            } else {
                if (weakListener == onFileDownloadStatusListener) {
                    mWeakOnFileDownloadStatusListeners.remove(weakListener);
                    break;
                }
            }
        }
    }

    /**
     * get all DownloadFiles
     *
     * @return all DownloadFiles
     */
    public List<DownloadFileInfo> getDownloadFiles() {
        return mDownloadFileCacher.getDownloadFiles();
    }

    /**
     * start download task
     */
    private void addAndRunFileDownloadTask(DownloadFileInfo downloadFileInfo, OnFileDownloadStatusListener 
            onFileDownloadStatusListener) {
        // create task
        FileDownloadTask fileDownloadTask = new FileDownloadTask(FileDownloadTaskParamHelper.createByDownloadFile
                (downloadFileInfo), mDownloadFileCacher, onFileDownloadStatusListener);
        // record task map
        mFileDownloadTaskMap.put(fileDownloadTask.getUrl(), fileDownloadTask);
        // start exec task
        mConfiguration.getFileDownloadEngine().execute(fileDownloadTask);
    }

    /**
     * notify download failed with task check
     */
    private void notifyFileDownloadStatusFailedWithCheck(final String url, final OnFileDownloadStatusFailReason 
            failReason, final OnFileDownloadStatusListener onFileDownloadStatusListener, final boolean recordStatus) {
        final DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        // null
        if (downloadFileInfo == null) {
            // notify callback
            if (onFileDownloadStatusListener != null) {
                OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(url, downloadFileInfo, new 
                        OnFileDownloadStatusFailReason(failReason), onFileDownloadStatusListener);
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
                        notifyFileDownloadStatusFailed(downloadUrl, downloadFileInfo, new 
                                OnFileDownloadStatusFailReason(failReason), onFileDownloadStatusListener, recordStatus);
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
    private void notifyFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, 
                                                OnFileDownloadStatusFailReason failReason, 
                                                OnFileDownloadStatusListener onFileDownloadStatusListener, boolean 
                                                        recordStatus) {
        if (recordStatus) {
            // record status
            try {
                mDownloadFileCacher.recordStatus(url, Status.DOWNLOAD_STATUS_ERROR, 0);
            } catch (DownloadStatusRecordException e) {
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
     * start a support task
     */
    private void addAndRunSupportTask(Runnable task) {
        // exec a support task
        mConfiguration.getSupportEngine().execute(task);
    }

    /**
     * get download file save dir
     *
     * @return
     */
    public String getDownloadDir() {
        return mConfiguration.getFileDownloadDir();
    }

    /**
     * 释放资源
     */
    public void release() {

        // pause all task 
        pauseAll();

        // clear cache
        mDetectUrlFileCacher.release();
        mDownloadFileCacher.release();

        sInstance = null;
    }

    // ======================================public operation methods======================================

    // --------------------------------------detect url file--------------------------------------

    /**
     * detect url file
     *
     * @param url                     file url
     * @param onDetectUrlFileListener DetectUrlFileListener
     */
    public void detect(String url, OnDetectUrlFileListener onDetectUrlFileListener) {

        checkInit();

        // 1.prepare detectUrlFileTask
        DetectUrlFileTask detectUrlFileTask = new DetectUrlFileTask(url, mConfiguration.getFileDownloadDir(), 
                mDetectUrlFileCacher, mDownloadFileCacher);
        detectUrlFileTask.setOnDetectUrlFileListener(onDetectUrlFileListener);

        // 2.start detectUrlFileTask
        addAndRunSupportTask(detectUrlFileTask);
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

        checkInit();

        // 1.get detect file
        DetectUrlFileInfo detectUrlFileInfo = getDetectUrlFile(url);
        if (detectUrlFileInfo == null) {
            // error detect file does not exist
            OnFileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason("detect file does not " + 
                    "exist!", OnFileDownloadStatusFailReason.TYPE_FILE_NOT_DETECT);
            // notifyFileDownloadStatusFailedWithCheck
            notifyFileDownloadStatusFailedWithCheck(url, failReason, mInternalDownloadStatusListenerImpl, false);
            return;
        }

        // 2.prepared to create task
        detectUrlFileInfo.setFileDir(saveDir);
        detectUrlFileInfo.setFileName(fileName);
        createAndStartByDetectUrlFile(url, detectUrlFileInfo, mInternalDownloadStatusListenerImpl);
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
    public void createAndStart(String url, String saveDir, String fileName, final OnFileDownloadStatusListener 
            onFileDownloadStatusListener) {

        registerDownloadStatusListener(onFileDownloadStatusListener);

        createAndStart(url, saveDir, fileName);
    }

    // create download task by using detectUrlFileInfo
    private void createAndStartByDetectUrlFile(String url, DetectUrlFileInfo detectUrlFileInfo, 
                                               OnFileDownloadStatusListener onFileDownloadStatusListener) {

        // 1.check detectUrlFileInfo
        if (detectUrlFileInfo == null) {
            // error detect file does not exist
            OnFileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason("detect file does not " + 
                    "exist!", OnFileDownloadStatusFailReason.TYPE_FILE_NOT_DETECT);
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
        DownloadFileInfo downloadFileInfo = new DownloadFileInfo(detectUrlFileInfo);
        // add to cache
        mDownloadFileCacher.addDownloadFile(downloadFileInfo);

        // 4.start download task
        startInternal(downloadUrl, downloadFileInfo, onFileDownloadStatusListener);
    }

    /**
     * start download task
     */
    private void startInternal(String url, DownloadFileInfo downloadFileInfo, OnFileDownloadStatusListener 
            onFileDownloadStatusListener) {

        OnFileDownloadStatusFailReason failReason = null;// null means there are not errors

        // 1.check downloadFileInfo
        if (downloadFileInfo == null) {
            if (failReason == null) {
                // error the downloadFileInfo does not exist
                failReason = new OnFileDownloadStatusFailReason("the downloadFileInfo does not exist!", 
                        OnFileDownloadStatusFailReason.TYPE_NULL_POINTER);
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

        checkInit();

        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        // has been downloaded
        if (downloadFileInfo != null) {
            // continue download task
            startInternal(downloadFileInfo.getUrl(), downloadFileInfo, mInternalDownloadStatusListenerImpl);
        }
        // not download
        else {
            DetectUrlFileInfo detectUrlFileInfo = getDetectUrlFile(url);
            // detected
            if (detectUrlFileInfo != null) {
                // create download task
                createAndStartByDetectUrlFile(detectUrlFileInfo.getUrl(), detectUrlFileInfo, 
                        mInternalDownloadStatusListenerImpl);
            }
            // not detect
            else {
                // detect
                detect(url, new OnDetectUrlFileListener() {
                    @Override
                    public void onDetectUrlFileFailed(String url, DetectUrlFileFailReason failReason) {
                        // 1.notifyFileDownloadStatusFailedWithCheck
                        notifyFileDownloadStatusFailedWithCheck(url, new OnFileDownloadStatusFailReason(failReason), 
                                mInternalDownloadStatusListenerImpl, false);
                    }

                    @Override
                    public void onDetectUrlFileExist(String url) {
                        // continue download task
                        startInternal(url, getDownloadFile(url), mInternalDownloadStatusListenerImpl);
                    }

                    @Override
                    public void onDetectNewDownloadFile(String url, String fileName, String savedDir, int fileSize) {
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

        checkInit();

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
            DownloadFileInfo downloadFileInfo = getDownloadFileByUrl(url);
            if (downloadFileInfo != null) {
                switch (downloadFileInfo.getStatus()) {
                    case Status.DOWNLOAD_STATUS_WAITING:
                    case Status.DOWNLOAD_STATUS_PREPARING:
                    case Status.DOWNLOAD_STATUS_PREPARED:
                        downloadFileInfo.setStatus(Status.DOWNLOAD_STATUS_PAUSED);
                        mDownloadFileCacher.updateDownloadFile(downloadFileInfo);
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
     * @param url file url
     */
    public void pause(String url) {
        pauseInternal(url, null);
    }

    /**
     * pause multi download
     *
     * @param urls file mUrls
     */
    public void pause(List<String> urls) {
        for (String url : urls) {
            if (TextUtils.isEmpty(url)) {
                continue;
            }
            pause(url);
        }
    }

    /**
     * pause all download
     */
    public void pauseAll() {
        Set<String> urls = mFileDownloadTaskMap.keySet();
        pause(new ArrayList<String>(urls));
    }

    // --------------------------------------restart download--------------------------------------

    /**
     * restart download
     */
    private void reStartInternal(String url) {
        // update downloaded size if need
        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo != null) {
            downloadFileInfo.setDownloadedSize(0);
            mDownloadFileCacher.updateDownloadFile(downloadFileInfo);
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
                            notifyFileDownloadStatusFailedWithCheck(url, new OnFileDownloadStatusFailReason
                                    (failReason), mInternalDownloadStatusListenerImpl, false);
                        }
                    } else {
                        // error
                        notifyFileDownloadStatusFailedWithCheck(url, new OnFileDownloadStatusFailReason(failReason), 
                                mInternalDownloadStatusListenerImpl, false);
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

    // --------------------------------------delete download--------------------------------------

    /**
     * delete download
     */
    private void deleteInternal(String url, boolean deleteDownloadedFileInPath, OnDeleteDownloadFileListener 
            onDeleteDownloadFileListener) {

        // create delete download task
        DeleteDownloadFileTask deleteDownloadFileTask = new DeleteDownloadFileTask(url, deleteDownloadedFileInPath, 
                mDownloadFileCacher);
        deleteDownloadFileTask.setOnDeleteDownloadFileListener(onDeleteDownloadFileListener);

        addAndRunSupportTask(deleteDownloadFileTask);
    }

    /**
     * delete download
     *
     * @param url                          file url
     * @param deleteDownloadedFileInPath   whether delete file in path
     * @param onDeleteDownloadFileListener DeleteDownloadFileListener
     */
    public void delete(String url, final boolean deleteDownloadedFileInPath, final OnDeleteDownloadFileListener 
            onDeleteDownloadFileListener) {

        checkInit();

        final DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo == null) {

            Log.d(TAG, "delete 文件不存在,url:" + url);

            // the DownloadFile does not exist
            if (onDeleteDownloadFileListener != null) {
                onDeleteDownloadFileListener.onDeleteDownloadFileFailed(downloadFileInfo, new 
                        OnDeleteDownloadFileFailReason("the download file doest not exist!", 
                        OnDeleteDownloadFileFailReason.TYPE_FILE_RECORD_IS_NOT_EXIST));
            }
            return;
        }

        FileDownloadTask task = getFileDownloadTask(url);
        if (task == null || task.isStopped()) {

            Log.d(TAG, "delete 直接删除,url:" + url);

            deleteInternal(url, deleteDownloadedFileInPath, onDeleteDownloadFileListener);
        } else {

            Log.d(TAG, "delete 需要先暂停后删除,url:" + url);

            // pause
            pauseInternal(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {

                    Log.d(TAG, "delete 暂停成功，开始删除,url:" + url);

                    deleteInternal(url, deleteDownloadedFileInPath, onDeleteDownloadFileListener);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {

                    Log.d(TAG, "delete 暂停失败，无法删除,url:" + url);

                    if (onDeleteDownloadFileListener != null) {
                        OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFileFailed(downloadFileInfo, 
                                new OnDeleteDownloadFileFailReason(failReason), onDeleteDownloadFileListener);
                    }
                }
            });
        }
    }

    /**
     * delete multi downloads
     *
     * @param urls                          file mUrls
     * @param deleteDownloadedFile          whether delete file in path
     * @param onDeleteDownloadFilesListener DeleteDownloadFilesListener
     */
    public void delete(List<String> urls, boolean deleteDownloadedFile, OnDeleteDownloadFilesListener 
            onDeleteDownloadFilesListener) {

        checkInit();

        if (mDeleteDownloadFilesTask != null && !mDeleteDownloadFilesTask.isStopped()) {
            // deleting
            return;
        }

        DeleteDownloadFilesTask deleteDownloadFilesTask = new DeleteDownloadFilesTask(urls, deleteDownloadedFile);
        deleteDownloadFilesTask.setOnDeleteDownloadFilesListener(onDeleteDownloadFilesListener);

        // start task
        new Thread(deleteDownloadFilesTask).start();

        this.mDeleteDownloadFilesTask = deleteDownloadFilesTask;
    }

    // --------------------------------------move download--------------------------------------

    private void moveInternal(String url, String newDirPath, OnMoveDownloadFileListener onMoveDownloadFileListener) {

        MoveDownloadFileTask moveDownloadFileTask = new MoveDownloadFileTask(url, newDirPath, mDownloadFileCacher);
        moveDownloadFileTask.setOnMoveDownloadFileListener(onMoveDownloadFileListener);

        addAndRunSupportTask(moveDownloadFileTask);
    }

    /**
     * move download
     *
     * @param url                        file url
     * @param newDirPath                 new dir path
     * @param onMoveDownloadFileListener use {@link OnMoveDownloadFileListener} for default,or use {@link
     *                                   OnSyncMoveDownloadFileListener} for do some custom sync with file-downloader,
     *                                   if custom sync failed,the file-downloader will rollback the operation
     */
    public void move(final String url, final String newDirPath, final OnMoveDownloadFileListener 
            onMoveDownloadFileListener) {

        checkInit();

        final DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo == null) {

            Log.d(TAG, "move 文件不存在,url:" + url);

            // the DownloadFile does not exist
            if (onMoveDownloadFileListener != null) {
                onMoveDownloadFileListener.onMoveDownloadFileFailed(downloadFileInfo, new 
                        OnMoveDownloadFileFailReason("the download file doest not exist!", 
                        OnMoveDownloadFileFailReason.TYPE_ORIGINAL_FILE_NOT_EXIST));
            }
            return;
        }

        FileDownloadTask task = getFileDownloadTask(url);
        if (task == null || task.isStopped()) {

            Log.d(TAG, "move 直接移动,url:" + url);

            moveInternal(url, newDirPath, onMoveDownloadFileListener);
        } else {

            Log.d(TAG, "move 需要先暂停后移动,url:" + url);

            // pause
            pauseInternal(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {

                    Log.d(TAG, "move 暂停成功，开始移动,url:" + url);

                    moveInternal(url, newDirPath, onMoveDownloadFileListener);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {

                    Log.d(TAG, "move 暂停失败，无法移动,url:" + url);

                    if (onMoveDownloadFileListener != null) {
                        OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo, new 
                                OnMoveDownloadFileFailReason(failReason), onMoveDownloadFileListener);
                    }
                }
            });
        }
    }

    /**
     * move multi downloads
     *
     * @param urls                        file mUrls
     * @param newDirPath                  new dir path
     * @param onMoveDownloadFilesListener MoveDownloadFilesListener
     */
    public void move(List<String> urls, String newDirPath, OnMoveDownloadFilesListener onMoveDownloadFilesListener) {

        checkInit();

        if (mMoveDownloadFilesTask != null && !mMoveDownloadFilesTask.isStopped()) {
            // moving
            return;
        }

        MoveDownloadFilesTask moveDownloadFilesTask = new MoveDownloadFilesTask(urls, newDirPath);
        moveDownloadFilesTask.setOnMoveDownloadFilesListener(onMoveDownloadFilesListener);

        // start task
        new Thread(moveDownloadFilesTask).start();

        this.mMoveDownloadFilesTask = moveDownloadFilesTask;
    }

    // --------------------------------------rename download--------------------------------------

    private void renameInternal(String url, String newFileName, boolean includedSuffix, OnRenameDownloadFileListener 
            onRenameDownloadFileListener) {

        RenameDownloadFileTask task = new RenameDownloadFileTask(url, newFileName, includedSuffix, mDownloadFileCacher);
        task.setOnRenameDownloadFileListener(onRenameDownloadFileListener);

        addAndRunSupportTask(task);
    }

    /**
     * rename download
     *
     * @param url                          file url
     * @param newFileName                  new file name
     * @param includedSuffix               true means the newFileName include the suffix
     * @param onRenameDownloadFileListener RenameDownloadFileListener
     */
    public void rename(String url, final String newFileName, final boolean includedSuffix, final 
    OnRenameDownloadFileListener onRenameDownloadFileListener) {

        checkInit();

        final DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo == null) {

            Log.d(TAG, "rename 文件不存在,url:" + url);

            // the DownloadFile does not exist
            if (onRenameDownloadFileListener != null) {
                onRenameDownloadFileListener.onRenameDownloadFileFailed(downloadFileInfo, new 
                        OnRenameDownloadFileFailReason("the download file is not exist!", 
                        OnRenameDownloadFileFailReason.TYPE_FILE_RECORD_IS_NOT_EXIST));
            }
            return;
        }

        FileDownloadTask task = getFileDownloadTask(url);
        if (task == null || task.isStopped()) {

            Log.d(TAG, "rename 直接重命名,url:" + url);

            renameInternal(url, newFileName, includedSuffix, onRenameDownloadFileListener);
        } else {

            Log.d(TAG, "rename 需要先暂停后重命名,url:" + url);

            // pause
            pauseInternal(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {

                    Log.d(TAG, "rename 暂停成功，开始重命名,url:" + url);

                    renameInternal(url, newFileName, includedSuffix, onRenameDownloadFileListener);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {

                    Log.d(TAG, "rename 暂停失败，无法重命名,url:" + url);

                    // error
                    if (onRenameDownloadFileListener != null) {
                        onRenameDownloadFileListener.onRenameDownloadFileFailed(downloadFileInfo, new 
                                OnRenameDownloadFileFailReason(failReason));
                    }
                }
            });
        }
    }

    // ======================================inner classes(operation tasks)======================================

    // --------------------------------------delete multi download task--------------------------------------

    /**
     * delete multi download task
     */
    private class DeleteDownloadFilesTask implements Runnable, Stoppable {

        private List<String> mUrls;
        private boolean mDeleteDownloadedFile;
        private OnDeleteDownloadFilesListener mOnDeleteDownloadFilesListener;

        private boolean mIsStop = false;
        private boolean mCompleted = false;

        private final List<DownloadFileInfo> mDownloadFilesNeedDelete = new ArrayList<DownloadFileInfo>();
        final List<DownloadFileInfo> mDownloadFilesDeleted = new ArrayList<DownloadFileInfo>();
        final List<DownloadFileInfo> mDownloadFilesSkip = new ArrayList<DownloadFileInfo>();

        public DeleteDownloadFilesTask(List<String> urls, boolean deleteDownloadedFile) {
            super();
            this.mUrls = urls;
            this.mDeleteDownloadedFile = deleteDownloadedFile;
        }

        public void setOnDeleteDownloadFilesListener(OnDeleteDownloadFilesListener onDeleteDownloadFilesListener) {
            this.mOnDeleteDownloadFilesListener = onDeleteDownloadFilesListener;
        }

        @Override
        public void stop() {
            this.mIsStop = true;
        }

        @Override
        public boolean isStopped() {
            return mIsStop;
        }

        @Override
        public void run() {

            for (String url : mUrls) {
                if (!UrlUtil.isUrl(url)) {
                    continue;
                }
                DownloadFileInfo downloadFileInfo = getDownloadFile(url);
                if (downloadFileInfo != null) {
                    mDownloadFilesNeedDelete.add(downloadFileInfo);
                }
            }

            // prepare to delete
            if (mOnDeleteDownloadFilesListener != null) {

                Log.d(TAG, "DeleteDownloadFilesTask.run 准备批量删除，大小：" + mDownloadFilesNeedDelete.size());

                OnDeleteDownloadFilesListener.MainThreadHelper.onDeleteDownloadFilePrepared(mDownloadFilesNeedDelete,
                        mOnDeleteDownloadFilesListener);
            }

            OnDeleteDownloadFileListener onDeleteDownloadFileListener = new OnDeleteDownloadFileListener() {

                private int deleteCount = 0;

                @Override
                public void onDeleteDownloadFilePrepared(DownloadFileInfo downloadFileNeedDelete) {

                    String url = null;
                    if (downloadFileNeedDelete != null) {
                        url = downloadFileNeedDelete.getUrl();
                    }

                    Log.d(TAG, "DeleteDownloadFilesTask.run 准备删除，url：" + url);

                    // start new delete
                    if (mOnDeleteDownloadFilesListener != null) {
                        mOnDeleteDownloadFilesListener.onDeletingDownloadFiles(mDownloadFilesNeedDelete, 
                                mDownloadFilesDeleted, mDownloadFilesSkip, downloadFileNeedDelete);
                    }

                    deleteCount++;
                }

                @Override
                public void onDeleteDownloadFileSuccess(DownloadFileInfo downloadFileDeleted) {

                    String url = null;
                    if (downloadFileDeleted != null) {
                        url = downloadFileDeleted.getUrl();
                    }

                    Log.d(TAG, "DeleteDownloadFilesTask.run onDeleteDownloadFileSuccess,删除成功，deleteCount：" +
                            deleteCount + ",mDownloadFilesNeedDelete.size():" + mDownloadFilesNeedDelete.size() +
                            "，url：" + url);

                    // delete succeed
                    mDownloadFilesDeleted.add(downloadFileDeleted);

                    // if the last one to delete,notify finish the operation
                    if (deleteCount == mDownloadFilesNeedDelete.size() - mDownloadFilesSkip.size()) {

                        Log.d(TAG, "DeleteDownloadFilesTask.run onDeleteDownloadFileSuccess," + 
                                "删除成功，回调onDeleteDownloadFilesCompleted");

                        onDeleteDownloadFilesCompleted();
                    }
                }

                @Override
                public void onDeleteDownloadFileFailed(DownloadFileInfo downloadFileInfo, 
                                                       OnDeleteDownloadFileFailReason failReason) {

                    String url = null;
                    if (downloadFileInfo != null) {
                        url = downloadFileInfo.getUrl();
                    }

                    String type = null;
                    if (failReason != null) {
                        type = failReason.getType();
                    }

                    Log.d(TAG, "DeleteDownloadFilesTask.run onDeleteDownloadFileFailed,删除失败，deleteCount：" +
                            deleteCount + ",mDownloadFilesNeedDelete.size():" + mDownloadFilesNeedDelete.size() +
                            "，url：" + url + ",failReason:" + type);

                    // delete failed
                    mDownloadFilesSkip.add(downloadFileInfo);

                    // if the last one to delete,notify finish the operation
                    if (deleteCount == mDownloadFilesNeedDelete.size() - mDownloadFilesSkip.size()) {

                        Log.d(TAG, "DeleteDownloadFilesTask.run onDeleteDownloadFileFailed," + 
                                "删除失败，回调onDeleteDownloadFilesCompleted");

                        onDeleteDownloadFilesCompleted();
                    }
                }
            };

            // delete every single one
            for (int i = 0; i < mDownloadFilesNeedDelete.size(); i++) {

                DownloadFileInfo downloadFileInfo = mDownloadFilesNeedDelete.get(i);
                if (downloadFileInfo == null) {
                    continue;
                }

                String url = downloadFileInfo.getUrl();

                // if the task stopped,notify completed
                if (isStopped()) {

                    Log.d(TAG, "DeleteDownloadFilesTask.run task has been sopped," + 
                            "任务已经被取消，无法继续删除，回调onDeleteDownloadFilesCompleted");

                    onDeleteDownloadFilesCompleted();
                }

                // deleting
                delete(url, mDeleteDownloadedFile, onDeleteDownloadFileListener);
            }
        }

        // on delete finish
        private void onDeleteDownloadFilesCompleted() {
            if (mCompleted) {
                return;
            }
            if (mOnDeleteDownloadFilesListener != null) {
                mOnDeleteDownloadFilesListener.onDeleteDownloadFilesCompleted(mDownloadFilesNeedDelete, 
                        mDownloadFilesDeleted);
            }
            mCompleted = true;
            mIsStop = true;
        }
    }

    // --------------------------------------move multi download task--------------------------------------

    /**
     * move multi download task
     */
    private class MoveDownloadFilesTask implements Runnable, Stoppable {

        private List<String> mUrls;
        private String mNewDirPath;
        private OnMoveDownloadFilesListener mOnMoveDownloadFilesListener;

        private boolean mIsStop = false;
        private boolean mCompleted = false;

        final List<DownloadFileInfo> mDownloadFilesNeedMove = new ArrayList<DownloadFileInfo>();
        final List<DownloadFileInfo> mDownloadFilesMoved = new ArrayList<DownloadFileInfo>();
        final List<DownloadFileInfo> mDownloadFilesSkip = new ArrayList<DownloadFileInfo>();

        public MoveDownloadFilesTask(List<String> urls, String newDirPath) {
            super();
            this.mUrls = urls;
            this.mNewDirPath = newDirPath;
        }

        public void setOnMoveDownloadFilesListener(OnMoveDownloadFilesListener onMoveDownloadFilesListener) {
            this.mOnMoveDownloadFilesListener = onMoveDownloadFilesListener;
        }

        public void stop() {
            this.mIsStop = true;
        }

        public boolean isStopped() {
            return mIsStop;
        }

        @Override
        public void run() {

            for (String url : mUrls) {
                if (!UrlUtil.isUrl(url)) {
                    continue;
                }
                DownloadFileInfo downloadFileInfo = getDownloadFile(url);
                if (downloadFileInfo != null) {
                    mDownloadFilesNeedMove.add(downloadFileInfo);
                }
            }

            // prepare to delete
            if (mOnMoveDownloadFilesListener != null) {

                Log.d(TAG, "MoveDownloadFilesTask.run 准备批量移动，大小：" + mDownloadFilesNeedMove.size());

                OnMoveDownloadFilesListener.MainThreadHelper.onMoveDownloadFilesPrepared(mDownloadFilesNeedMove, 
                        mOnMoveDownloadFilesListener);
            }

            OnMoveDownloadFileListener onMoveDownloadFileListener = new OnMoveDownloadFileListener() {

                private int deleteCount = 0;

                @Override
                public void onMoveDownloadFilePrepared(DownloadFileInfo downloadFileNeedToMove) {

                    String url = null;
                    if (downloadFileNeedToMove != null) {
                        url = downloadFileNeedToMove.getUrl();
                    }

                    Log.d(TAG, "MoveDownloadFilesTask.run 准备删除，url：" + url);

                    // start new move
                    if (mOnMoveDownloadFilesListener != null) {
                        mOnMoveDownloadFilesListener.onMovingDownloadFiles(mDownloadFilesNeedMove, 
                                mDownloadFilesMoved, mDownloadFilesSkip, downloadFileNeedToMove);
                    }

                    deleteCount++;
                }

                @Override
                public void onMoveDownloadFileSuccess(DownloadFileInfo downloadFileMoved) {

                    String url = null;
                    if (downloadFileMoved != null) {
                        url = downloadFileMoved.getUrl();
                    }

                    Log.d(TAG, "MoveDownloadFilesTask.run onMoveDownloadFileSuccess,移动成功，moveCount：" + deleteCount +
                            ",mDownloadFilesNeedMove.size():" + mDownloadFilesNeedMove.size() + "，url：" + url);

                    // move succeed
                    mDownloadFilesMoved.add(downloadFileMoved);

                    // if the last one to move,notify finish the operation
                    if (deleteCount == mDownloadFilesNeedMove.size() - mDownloadFilesSkip.size()) {

                        Log.d(TAG, "MoveDownloadFilesTask.run onMoveDownloadFileSuccess," + 
                                "移动成功，回调onMoveDownloadFilesCompleted");

                        //TODO

                        onMoveDownloadFilesCompleted();
                    }
                }

                @Override
                public void onMoveDownloadFileFailed(DownloadFileInfo downloadFileInfo, OnMoveDownloadFileFailReason 
                        failReason) {

                    String url = null;
                    if (downloadFileInfo != null) {
                        url = downloadFileInfo.getUrl();
                    }

                    String type = null;
                    if (failReason != null) {
                        type = failReason.getType();
                    }

                    Log.d(TAG, "MoveDownloadFilesTask.run onMoveDownloadFileFailed,移动失败，moveCount：" + deleteCount +
                            ",mDownloadFilesNeedMove.size():" + mDownloadFilesNeedMove.size() + "，url：" + url + "," +
                            "failReason:" + type);

                    // move failed
                    mDownloadFilesSkip.add(downloadFileInfo);

                    // if the last one to move,notify finish the operation
                    if (deleteCount == mDownloadFilesNeedMove.size() - mDownloadFilesSkip.size()) {

                        Log.d(TAG, "MoveDownloadFilesTask.run onMoveDownloadFileFailed," + 
                                "移动失败，回调onMoveDownloadFilesCompleted");

                        onMoveDownloadFilesCompleted();
                    }
                }
            };

            // move every single one
            for (int i = 0; i < mDownloadFilesNeedMove.size(); i++) {

                DownloadFileInfo downloadFileInfo = mDownloadFilesNeedMove.get(i);
                if (downloadFileInfo == null) {
                    continue;
                }

                String url = downloadFileInfo.getUrl();

                // if the task stopped,notify completed
                if (isStopped()) {

                    Log.d(TAG, "MoveDownloadFilesTask.run task has been sopped," + 
                            "任务已经被取消，无法继续移动，回调onMoveDownloadFilesCompleted");

                    onMoveDownloadFilesCompleted();
                } else {
                    // moving
                    move(url, mNewDirPath, onMoveDownloadFileListener);
                }
            }
        }

        // move finish
        private void onMoveDownloadFilesCompleted() {
            if (mCompleted) {
                return;
            }
            if (mOnMoveDownloadFilesListener != null) {
                mOnMoveDownloadFilesListener.onMoveDownloadFilesCompleted(mDownloadFilesNeedMove, mDownloadFilesMoved);
            }
            mCompleted = true;
            mIsStop = true;
        }
    }


    /**
     * the class of OnFileDownloadStatusListener impl
     */
    private class OnFileDownloadStatusListenerImpl implements OnFileDownloadStatusListener {

        @Override
        public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {
            // notify all registered listeners
            for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
                if (weakReference == null) {
                    continue;
                }
                OnFileDownloadStatusListener weakListener = weakReference.get();
                if (weakListener == null || weakListener == mInternalDownloadStatusListenerImpl) {
                    continue;
                }
                weakListener.onFileDownloadStatusWaiting(downloadFileInfo);
            }
        }

        @Override
        public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {
            // notify all registered listeners
            for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
                if (weakReference == null) {
                    continue;
                }
                OnFileDownloadStatusListener weakListener = weakReference.get();
                if (weakListener == null || weakListener == mInternalDownloadStatusListenerImpl) {
                    continue;
                }
                weakListener.onFileDownloadStatusPreparing(downloadFileInfo);
            }
        }

        @Override
        public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {
            // notify all registered listeners
            for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
                if (weakReference == null) {
                    continue;
                }
                OnFileDownloadStatusListener weakListener = weakReference.get();
                if (weakListener == null || weakListener == mInternalDownloadStatusListenerImpl) {
                    continue;
                }
                weakListener.onFileDownloadStatusPrepared(downloadFileInfo);
            }
        }

        @Override
        public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long 
                remainingTime) {
            // notify all registered listeners
            for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
                if (weakReference == null) {
                    continue;
                }
                OnFileDownloadStatusListener weakListener = weakReference.get();
                if (weakListener == null || weakListener == mInternalDownloadStatusListenerImpl) {
                    continue;
                }
                weakListener.onFileDownloadStatusDownloading(downloadFileInfo, downloadSpeed, remainingTime);
            }
        }

        @Override
        public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {
            // notify all registered listeners
            for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
                if (weakReference == null) {
                    continue;
                }
                OnFileDownloadStatusListener weakListener = weakReference.get();
                if (weakListener == null || weakListener == mInternalDownloadStatusListenerImpl) {
                    continue;
                }
                weakListener.onFileDownloadStatusPaused(downloadFileInfo);
            }
        }

        @Override
        public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {
            // notify all registered listeners
            for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
                if (weakReference == null) {
                    continue;
                }
                OnFileDownloadStatusListener weakListener = weakReference.get();
                if (weakListener == null || weakListener == mInternalDownloadStatusListenerImpl) {
                    continue;
                }
                weakListener.onFileDownloadStatusCompleted(downloadFileInfo);
            }
        }

        @Override
        public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, 
                                               OnFileDownloadStatusFailReason failReason) {
            // notify all registered listeners
            for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
                if (weakReference == null) {
                    continue;
                }
                OnFileDownloadStatusListener weakListener = weakReference.get();
                if (weakListener == null || weakListener == mInternalDownloadStatusListenerImpl) {
                    continue;
                }
                weakListener.onFileDownloadStatusFailed(url, downloadFileInfo, failReason);
            }
        }
    }
}
