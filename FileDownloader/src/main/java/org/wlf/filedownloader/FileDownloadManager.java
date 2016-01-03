package org.wlf.filedownloader;

import android.content.Context;
import android.util.Log;

import org.wlf.filedownloader.base.Control;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.file_delete.DownloadDeleteManager;
import org.wlf.filedownloader.file_download.DownloadTaskManager;
import org.wlf.filedownloader.file_download.DownloadTaskManager.OnReleaseListener;
import org.wlf.filedownloader.file_move.DownloadMoveManager;
import org.wlf.filedownloader.file_rename.DownloadRenameManager;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener;
import org.wlf.filedownloader.listener.OnDeleteDownloadFilesListener;
import org.wlf.filedownloader.listener.OnDetectBigUrlFileListener;
import org.wlf.filedownloader.listener.OnDetectUrlFileListener;
import org.wlf.filedownloader.listener.OnDownloadFileChangeListener;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener;
import org.wlf.filedownloader.listener.OnMoveDownloadFilesListener;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener;
import org.wlf.filedownloader.util.CollectionUtil;

import java.util.List;

/**
 * FileDownload Manager
 * <br/>
 * 文件下载管理器
 *
 * @author wlf
 * @deprecated use {@link FileDownloader} instead if the version you are using is reach 0.2.0
 */
@Deprecated
public final class FileDownloadManager {

    /**
     * LOG TAG
     */
    private static final String TAG = FileDownloadManager.class.getSimpleName();
    /**
     * single instance
     */
    private static FileDownloadManager sInstance;
    /**
     * init lock
     */
    private Object mInitLock = new Object();

    // --------------------------------------important fields for the manager--------------------------------------
    /**
     * Configuration,to config download parameters
     */
    private FileDownloadConfiguration mConfiguration;
    /**
     * DownloadFileCacher,to storage download files
     */
    private DownloadCacher mDownloadFileCacher;
    /**
     * DownloadTaskManager,to manage download tasks
     */
    private DownloadTaskManager mDownloadTaskManager;
    /**
     * DownloadMoveManager,to manage move download files
     */
    private DownloadMoveManager mDownloadMoveManager;
    /**
     * mDownloadDeleteManager,to manage delete download files
     */
    private DownloadDeleteManager mDownloadDeleteManager;
    /**
     * DownloadRenameManager,to manage rename download files
     */
    private DownloadRenameManager mDownloadRenameManager;

    //  constructor of FileDownloadManager,private only
    private FileDownloadManager(Context context) {

        Context appContext = context.getApplicationContext();

        // init DownloadFileCacher
        mDownloadFileCacher = new DownloadCacher(appContext);

        // check the download status,if there is an exception status,try to recover it
        exceptionStateRecovery(getDownloadFiles());
    }

    /**
     * get FileDownloadManager single instance
     *
     * @param context Context
     * @return the FileDownloadManager single instance
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

    // package access, FileDownloader use only
    static FileDownloadConfiguration getConfiguration() {
        if (sInstance != null) {
            synchronized (sInstance.mInitLock) {
                if (sInstance != null) {
                    return sInstance.mConfiguration;
                }
            }
        }
        return null;
    }

    /**
     * try to recover exception states
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

            // initialized and in the download task,ignore
            if (isInit() && getDownloadTaskManager().isInFileDownloadTaskMap(url)) {
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
                    } catch (Exception e) {
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    /**
     * init with a Configuration
     *
     * @param configuration Configuration
     */
    public void init(FileDownloadConfiguration configuration) {
        synchronized (mInitLock) {
            this.mConfiguration = configuration;
        }
    }

    /**
     * whether the file-downloader is initialized
     *
     * @return true means initialized
     */
    public boolean isInit() {
        synchronized (mInitLock) {
            return mConfiguration != null;
        }
    }

    /**
     * check whether the file-downloader is initialized
     */
    private void checkInit() {
        if (!isInit()) {
            throw new IllegalStateException("please init the file-downloader by using " + FileDownloader.class
                    .getSimpleName() + " or " + FileDownloadManager.class.getSimpleName() + " if the version is below" +
                    " 0.2.0");
        }
    }

    // --------------------------------------getters--------------------------------------

    /**
     * get DownloadTaskManager
     *
     * @return DownloadTaskManager
     */
    private DownloadTaskManager getDownloadTaskManager() {
        checkInit();
        if (mDownloadTaskManager == null) {
            mDownloadTaskManager = new DownloadTaskManager(mConfiguration, mDownloadFileCacher);
        }
        return mDownloadTaskManager;
    }

    /**
     * get DownloadMoveManager
     *
     * @return DownloadMoveManager
     */
    private DownloadMoveManager getDownloadMoveManager() {
        checkInit();
        if (mDownloadMoveManager == null) {
            mDownloadMoveManager = new DownloadMoveManager(mConfiguration.getFileOperationEngine(), 
                    mDownloadFileCacher, getDownloadTaskManager());
        }
        return mDownloadMoveManager;
    }

    /**
     * get DownloadDeleteManager
     *
     * @return DownloadDeleteManager
     */
    private DownloadDeleteManager getDownloadDeleteManager() {
        checkInit();
        if (mDownloadDeleteManager == null) {
            mDownloadDeleteManager = new DownloadDeleteManager(mConfiguration.getFileOperationEngine(), mDownloadFileCacher, getDownloadTaskManager());
        }
        return mDownloadDeleteManager;
    }

    /**
     * get DownloadRenameManager
     *
     * @return DownloadRenameManager
     */
    private DownloadRenameManager getDownloadRenameManager() {
        checkInit();
        if (mDownloadRenameManager == null) {
            mDownloadRenameManager = new DownloadRenameManager(mConfiguration.getFileOperationEngine(), 
                    mDownloadFileCacher, getDownloadTaskManager());
        }
        return mDownloadRenameManager;
    }

    /**
     * get DownloadFile by file url
     *
     * @param url file url
     * @return DownloadFile
     */
    public DownloadFileInfo getDownloadFile(String url) {
        return mDownloadFileCacher.getDownloadFile(url);
    }

    /**
     * get DownloadFile by file url
     *
     * @param url file url
     * @return DownloadFile
     * @deprecated use {@link #getDownloadFile(String)} instead
     */
    @Deprecated
    public DownloadFileInfo getDownloadFileByUrl(String url) {
        return mDownloadFileCacher.getDownloadFile(url);
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
     * get DownloadFile by temp file path
     *
     * @param tempPath temp file path
     * @return DownloadFile
     */
    public DownloadFileInfo getDownloadFileByTempPath(String tempPath) {
        return mDownloadFileCacher.getDownloadFileBySavePath(tempPath, true);
    }

    /**
     * get DownloadFile by save file path
     *
     * @param savePath            save file path
     * @param includeTempFilePath true means try use the savePath as temp file savePath if can not get DownloadFile
     *                            by savePath
     * @return DownloadFile
     * @deprecated use {@link #getDownloadFileBySavePath(String)} and {@link #getDownloadFileByTempPath(String)} instead
     */
    @Deprecated
    public DownloadFileInfo getDownloadFileBySavePath(String savePath, boolean includeTempFilePath) {
        return mDownloadFileCacher.getDownloadFileBySavePath(savePath, includeTempFilePath);
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
     * get download file save dir
     *
     * @return download file save dir
     */
    public String getDownloadDir() {
        checkInit();
        return mConfiguration.getFileDownloadDir();
    }

    // --------------------------------------register listeners--------------------------------------

    /**
     * register an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener the OnFileDownloadStatusListener impl
     * @since 0.2.0
     */
    void registerDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        getDownloadTaskManager().registerDownloadStatusListener(onFileDownloadStatusListener);
    }

    /**
     * unregister an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener the OnFileDownloadStatusListener impl
     * @since 0.2.0
     */
    void unregisterDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        getDownloadTaskManager().unregisterDownloadStatusListener(onFileDownloadStatusListener);
    }

    /**
     * register an OnDownloadFileChangeListener
     *
     * @param onDownloadFileChangeListener the OnDownloadFileChangeListener impl
     * @since 0.2.0
     */
    void registerDownloadFileChangeListener(OnDownloadFileChangeListener onDownloadFileChangeListener) {
        mDownloadFileCacher.registerDownloadFileChangeListener(onDownloadFileChangeListener, null);
    }

    /**
     * unregister an OnDownloadFileChangeListener
     *
     * @param onDownloadFileChangeListener the OnDownloadFileChangeListener impl
     * @since 0.2.0
     */
    void unregisterDownloadFileChangeListener(OnDownloadFileChangeListener onDownloadFileChangeListener) {
        mDownloadFileCacher.unregisterDownloadFileChangeListener(onDownloadFileChangeListener);
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
        getDownloadTaskManager().detect(url, onDetectUrlFileListener);
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
        getDownloadTaskManager().createAndStart(url, saveDir, fileName);
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
    public void createAndStart(String url, String saveDir, String fileName, OnFileDownloadStatusListener 
            onFileDownloadStatusListener) {
        getDownloadTaskManager().createAndStart(url, saveDir, fileName, onFileDownloadStatusListener);
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
        getDownloadTaskManager().start(url);
    }

    /**
     * start/continue download
     *
     * @param url                          file url
     * @param onFileDownloadStatusListener FileDownloadStatusListener
     * @deprecated use {@link #start(String)} instead
     */
    @Deprecated
    public void start(String url, OnFileDownloadStatusListener onFileDownloadStatusListener) {
        getDownloadTaskManager().start(url, onFileDownloadStatusListener);
    }

    /**
     * start/continue multi download
     *
     * @param urls file urls
     * @since 0.2.0
     */
    public void start(List<String> urls) {
        getDownloadTaskManager().start(urls);
    }

    /**
     * start/continue multi download
     *
     * @param urls                         file urls
     * @param onFileDownloadStatusListener FileDownloadStatusListener
     * @deprecated use {@link #start(List)} instead
     */
    @Deprecated
    public void start(List<String> urls, OnFileDownloadStatusListener onFileDownloadStatusListener) {
        getDownloadTaskManager().start(urls, onFileDownloadStatusListener);
    }

    // --------------------------------------pause download--------------------------------------

    /**
     * pause download
     *
     * @param url file url
     */
    public void pause(String url) {
        getDownloadTaskManager().pause(url, null);
    }

    /**
     * pause multi download
     *
     * @param urls file urls
     */
    public void pause(List<String> urls) {
        getDownloadTaskManager().pause(urls, null);
    }

    /**
     * pause all download
     */
    public void pauseAll() {
        getDownloadTaskManager().pauseAll(null, null);
    }

    // --------------------------------------restart download--------------------------------------

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
        getDownloadTaskManager().reStart(url);
    }

    /**
     * restart download
     *
     * @param url                          file url
     * @param onFileDownloadStatusListener FileDownloadStatusListener
     * @deprecated use {@link #reStart(String)} instead
     */
    @Deprecated
    public void reStart(String url, OnFileDownloadStatusListener onFileDownloadStatusListener) {
        getDownloadTaskManager().reStart(url, onFileDownloadStatusListener);
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
        getDownloadTaskManager().reStart(urls);
    }

    /**
     * restart multi download
     *
     * @param urls                         file mUrls
     * @param onFileDownloadStatusListener FileDownloadStatusListener
     * @deprecated use {@link #reStart(List)} instead
     */
    @Deprecated
    public void reStart(List<String> urls, OnFileDownloadStatusListener onFileDownloadStatusListener) {
        getDownloadTaskManager().reStart(urls, onFileDownloadStatusListener);
    }

    // --------------------------------------move download--------------------------------------

    /**
     * move download
     *
     * @param url                        file url
     * @param newDirPath                 new dir path
     * @param onMoveDownloadFileListener MoveDownloadFileListener
     */
    public void move(String url, String newDirPath, OnMoveDownloadFileListener onMoveDownloadFileListener) {
        getDownloadMoveManager().move(url, newDirPath, onMoveDownloadFileListener);
    }

    /**
     * move multi downloads
     *
     * @param urls                        file mUrls
     * @param newDirPath                  new dir path
     * @param onMoveDownloadFilesListener MoveDownloadFilesListener
     * @return the control for the operation
     */
    public Control move(List<String> urls, String newDirPath, OnMoveDownloadFilesListener onMoveDownloadFilesListener) {
        return getDownloadMoveManager().move(urls, newDirPath, onMoveDownloadFilesListener);
    }

    // --------------------------------------delete download--------------------------------------

    /**
     * delete download
     *
     * @param url                          file url
     * @param deleteDownloadedFileInPath   whether delete file in path
     * @param onDeleteDownloadFileListener DeleteDownloadFileListener
     */
    public void delete(String url, boolean deleteDownloadedFileInPath, OnDeleteDownloadFileListener 
            onDeleteDownloadFileListener) {
        getDownloadDeleteManager().delete(url, deleteDownloadedFileInPath, onDeleteDownloadFileListener);
    }

    /**
     * delete multi downloads
     *
     * @param urls                          file mUrls
     * @param deleteDownloadedFile          whether delete file in path
     * @param onDeleteDownloadFilesListener DeleteDownloadFilesListener
     * @return the control for the operation
     */
    public Control delete(List<String> urls, boolean deleteDownloadedFile, OnDeleteDownloadFilesListener 
            onDeleteDownloadFilesListener) {
        return getDownloadDeleteManager().delete(urls, deleteDownloadedFile, onDeleteDownloadFilesListener);
    }

    // --------------------------------------rename download--------------------------------------

    /**
     * rename download
     *
     * @param url                          file url
     * @param newFileName                  new file name
     * @param includedSuffix               true means the newFileName include the suffix
     * @param onRenameDownloadFileListener RenameDownloadFileListener
     */
    public void rename(String url, String newFileName, boolean includedSuffix, OnRenameDownloadFileListener 
            onRenameDownloadFileListener) {
        getDownloadRenameManager().rename(url, newFileName, includedSuffix, onRenameDownloadFileListener);
    }

    /**
     * release resources
     */
    public void release() {
        getDownloadTaskManager().release(new OnReleaseListener() {
            @Override
            public void onReleased() {
                if (mConfiguration != null) {
                    mConfiguration.getFileDetectEngine().shutdown();
                    mConfiguration.getFileDownloadEngine().shutdown();
                    mConfiguration.getFileOperationEngine().shutdown();
                }
                mDownloadFileCacher.release();
                sInstance = null;
            }
        });
    }

}
