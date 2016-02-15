package org.wlf.filedownloader;

import android.content.Context;

import org.wlf.filedownloader.DownloadStatusConfiguration.Builder;
import org.wlf.filedownloader.base.Control;
import org.wlf.filedownloader.base.Log;
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
import org.wlf.filedownloader.util.DownloadFileUtil;

import java.util.List;

/**
 * FileDownload Manager
 * <br/>
 * 文件下载管理器
 *
 * @author wlf
 * @deprecated use {@link FileDownloader} instead
 */
@Deprecated
public final class FileDownloadManager {

    private static final String TAG = FileDownloadManager.class.getSimpleName();
    /**
     * single instance
     */
    private static FileDownloadManager sInstance;
    /**
     * init lock
     */
    private Object mInitLock = new Object();

    // --------------------------------------important fields--------------------------------------

    /**
     * FileDownload Configuration, which stored global configurations
     */
    private FileDownloadConfiguration mConfiguration;
    /**
     * DownloadFileCacher, which stored download files
     */
    private DownloadCacher mDownloadFileCacher;

    /**
     * DownloadTaskManager, which to manage download tasks
     */
    private DownloadTaskManager mDownloadTaskManager;
    /**
     * DownloadMoveManager, which to move download files
     */
    private DownloadMoveManager mDownloadMoveManager;
    /**
     * DownloadDeleteManager, which to delete download files
     */
    private DownloadDeleteManager mDownloadDeleteManager;
    /**
     * DownloadRenameManager, which to rename download files
     */
    private DownloadRenameManager mDownloadRenameManager;

    // --------------------------------------lifecycle & others--------------------------------------

    //  constructor of FileDownloadManager, private only
    private FileDownloadManager(Context context) {

        Context appContext = context.getApplicationContext();

        // init DownloadFileCacher
        mDownloadFileCacher = new DownloadCacher(appContext);

        // check the download status, if there is an exception status, try to recover it
        checkAndRecoveryExceptionStatus(getDownloadFiles());
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

    /**
     * check and recovery exception status
     */
    private void checkAndRecoveryExceptionStatus(List<DownloadFileInfo> downloadFileInfos) {

        Log.i(TAG, "checkAndRecoveryExceptionStatus 异常恢复检查！");

        if (CollectionUtil.isEmpty(downloadFileInfos)) {
            return;
        }

        for (DownloadFileInfo downloadFileInfo : downloadFileInfos) {

            if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
                continue;
            }

            String url = downloadFileInfo.getUrl();

            // initialized and isDownloading, ignore
            if (isInit() && getDownloadTaskManager().isDownloading(url)) {
                continue;
            }

            // recovery status if necessary
            DownloadFileUtil.recoveryExceptionStatus(mDownloadFileCacher, downloadFileInfo);
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
            throw new IllegalStateException("Please init the file-downloader by using " + FileDownloader.class
                    .getSimpleName() + ".init(FileDownloadConfiguration) or " + FileDownloadManager.class
                    .getSimpleName() + ".init(FileDownloadConfiguration) if the version is below 0.2.0 !");
        }
    }

    /**
     * release resources
     */
    public void release() {
        getDownloadTaskManager().release(new OnReleaseListener() {
            @Override
            public void onReleased() {
                synchronized (mInitLock) {
                    if (mConfiguration != null) {
                        mConfiguration.getFileDetectEngine().shutdown();
                        mConfiguration.getFileDownloadEngine().shutdown();
                        mConfiguration.getFileOperationEngine().shutdown();
                    }
                    mDownloadFileCacher.release();
                    sInstance = null;
                }
            }
        });
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
            mDownloadDeleteManager = new DownloadDeleteManager(mConfiguration.getFileOperationEngine(), 
                    mDownloadFileCacher, getDownloadTaskManager());
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
     * @param includeTempFilePath true means try use the savePath as temp file savePath if it can not get DownloadFile
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

    // --------------------------------------register & unregister listeners--------------------------------------

    /**
     * register an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener impl
     * @since 0.2.0
     */
    public void registerDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        registerDownloadStatusListener(onFileDownloadStatusListener, null);
    }

    /**
     * unregister an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener registered OnFileDownloadStatusListener or
     *                                     OnRetryableFileDownloadStatusListener impl
     * @since 0.2.0
     */
    public void unregisterDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        getDownloadTaskManager().unregisterDownloadStatusListener(onFileDownloadStatusListener);
    }

    // --------------------------------------detect url files--------------------------------------

    /**
     * detect a url file
     *
     * @param url                     file url
     * @param onDetectUrlFileListener OnDetectUrlFileListener impl
     * @deprecated this method can not detect the url file which bigger than 2G, use {@link #detect(String,
     * OnDetectBigUrlFileListener)}instead
     */
    @Deprecated
    public void detect(String url, OnDetectUrlFileListener onDetectUrlFileListener) {
        getDownloadTaskManager().detect(url, onDetectUrlFileListener, null);
    }

    /**
     * detect a url file
     *
     * @param url                        file url
     * @param onDetectBigUrlFileListener OnDetectBigUrlFileListener impl
     */
    public void detect(String url, OnDetectBigUrlFileListener onDetectBigUrlFileListener) {
        detect(url, onDetectBigUrlFileListener, null);
    }

    // --------------------------------------create/continue downloads--------------------------------------

    /**
     * create a new download after detected a url file by using {@link #detect(String, OnDetectUrlFileListener)}
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br/>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener)}
     *
     * @param url      file url
     * @param saveDir  saveDir
     * @param fileName saveFileName
     * @since 0.2.0
     */
    public void createAndStart(String url, String saveDir, String fileName) {
        getDownloadTaskManager().createAndStart(url, saveDir, fileName, null);
    }

    /**
     * create a new download after detected a url file by using {@link #detect(String, OnDetectUrlFileListener)}
     *
     * @param url                          file url
     * @param saveDir                      saveDir
     * @param fileName                     saveFileName
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener impl
     * @deprecated use {@link #createAndStart(String, String, String)} instead
     */
    @Deprecated
    public void createAndStart(String url, String saveDir, String fileName, OnFileDownloadStatusListener 
            onFileDownloadStatusListener) {

        DownloadStatusConfiguration.Builder builder = new Builder();
        builder.addListenUrl(url);
        builder.configAutoRelease(true);
        registerDownloadStatusListener(onFileDownloadStatusListener, builder.build());

        createAndStart(url, saveDir, fileName);
    }

    /**
     * start/continue a download
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br/>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener)}
     *
     * @param url file url
     * @since 0.2.0
     */
    public void start(String url) {
        getDownloadTaskManager().start(url, null);
    }

    /**
     * start/continue a download
     *
     * @param url                          file url
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener impl
     * @deprecated use {@link #start(String)} instead
     */
    @Deprecated
    public void start(String url, OnFileDownloadStatusListener onFileDownloadStatusListener) {

        DownloadStatusConfiguration.Builder builder = new Builder();
        builder.addListenUrl(url);
        builder.configAutoRelease(true);
        registerDownloadStatusListener(onFileDownloadStatusListener, builder.build());

        start(url);
    }

    /**
     * start/continue multi downloads
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br/>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener)}
     *
     * @param urls file urls
     * @since 0.2.0
     */
    public void start(List<String> urls) {
        getDownloadTaskManager().start(urls, null);
    }

    /**
     * start/continue multi downloads
     *
     * @param urls                         file urls
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener impl
     * @deprecated use {@link #start(List)} instead
     */
    @Deprecated
    public void start(List<String> urls, OnFileDownloadStatusListener onFileDownloadStatusListener) {

        DownloadStatusConfiguration.Builder builder = new Builder();
        builder.addListenUrls(urls);
        builder.configAutoRelease(true);
        registerDownloadStatusListener(onFileDownloadStatusListener, builder.build());

        start(urls);
    }

    // --------------------------------------pause downloads--------------------------------------

    /**
     * pause a download
     *
     * @param url file url
     */
    public void pause(String url) {
        getDownloadTaskManager().pause(url, null);
    }

    /**
     * pause multi downloads
     *
     * @param urls file urls
     */
    public void pause(List<String> urls) {
        getDownloadTaskManager().pause(urls, null);
    }

    /**
     * pause all downloads
     */
    public void pauseAll() {
        getDownloadTaskManager().pauseAll(null);
    }

    // --------------------------------------restart downloads--------------------------------------

    /**
     * restart a download
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br/>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener)}
     *
     * @param url file url
     * @since 0.2.0
     */
    public void reStart(String url) {
        getDownloadTaskManager().reStart(url, null);
    }

    /**
     * restart a download
     *
     * @param url                          file url
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener impl
     * @deprecated use {@link #reStart(String)} instead
     */
    @Deprecated
    public void reStart(String url, OnFileDownloadStatusListener onFileDownloadStatusListener) {

        DownloadStatusConfiguration.Builder builder = new Builder();
        builder.addListenUrl(url);
        builder.configAutoRelease(true);
        registerDownloadStatusListener(onFileDownloadStatusListener, builder.build());

        reStart(url);
    }

    /**
     * restart multi downloads
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br/>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener)}
     *
     * @param urls file urls
     * @since 0.2.0
     */
    public void reStart(List<String> urls) {
        getDownloadTaskManager().reStart(urls, null);
    }

    /**
     * restart multi downloads
     *
     * @param urls                         file urls
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener impl
     * @deprecated use {@link #reStart(List)} instead
     */
    @Deprecated
    public void reStart(List<String> urls, OnFileDownloadStatusListener onFileDownloadStatusListener) {

        DownloadStatusConfiguration.Builder builder = new Builder();
        builder.addListenUrls(urls);
        builder.configAutoRelease(true);
        registerDownloadStatusListener(onFileDownloadStatusListener, builder.build());

        reStart(urls);
    }

    // --------------------------------------move download files--------------------------------------

    /**
     * move a download file
     *
     * @param url                        file url
     * @param newDirPath                 new dir path
     * @param onMoveDownloadFileListener OnMoveDownloadFileListener impl
     */
    public void move(String url, String newDirPath, OnMoveDownloadFileListener onMoveDownloadFileListener) {
        getDownloadMoveManager().move(url, newDirPath, onMoveDownloadFileListener);
    }

    /**
     * move multi download files
     *
     * @param urls                        file urls
     * @param newDirPath                  new dir path
     * @param onMoveDownloadFilesListener OnMoveDownloadFilesListener impl
     * @return a control for the operation
     */
    public Control move(List<String> urls, String newDirPath, OnMoveDownloadFilesListener onMoveDownloadFilesListener) {
        return getDownloadMoveManager().move(urls, newDirPath, onMoveDownloadFilesListener);
    }

    // --------------------------------------delete download files--------------------------------------

    /**
     * delete a download file
     *
     * @param url                          file url
     * @param deleteDownloadedFileInPath   whether delete file in path
     * @param onDeleteDownloadFileListener OnDeleteDownloadFileListener impl
     */
    public void delete(String url, boolean deleteDownloadedFileInPath, OnDeleteDownloadFileListener 
            onDeleteDownloadFileListener) {
        getDownloadDeleteManager().delete(url, deleteDownloadedFileInPath, onDeleteDownloadFileListener);
    }

    /**
     * delete multi download files
     *
     * @param urls                          file urls
     * @param deleteDownloadedFile          whether delete file in path
     * @param onDeleteDownloadFilesListener OnDeleteDownloadFilesListener impl
     * @return a control for the operation
     */
    public Control delete(List<String> urls, boolean deleteDownloadedFile, OnDeleteDownloadFilesListener 
            onDeleteDownloadFilesListener) {
        return getDownloadDeleteManager().delete(urls, deleteDownloadedFile, onDeleteDownloadFilesListener);
    }

    // --------------------------------------rename download--------------------------------------

    /**
     * rename a download file
     *
     * @param url                          file url
     * @param newFileName                  new file name
     * @param includedSuffix               true means the newFileName has been included the suffix, otherwise the
     *                                     newFileName not include the suffix
     * @param onRenameDownloadFileListener OnRenameDownloadFileListener impl
     */
    public void rename(String url, String newFileName, boolean includedSuffix, OnRenameDownloadFileListener 
            onRenameDownloadFileListener) {
        getDownloadRenameManager().rename(url, newFileName, includedSuffix, onRenameDownloadFileListener);
    }

    // =========================================for FileDownloader only=========================================

    // -------------------------get file download configuration-------------------------

    /**
     * get FileDownloadConfiguration
     *
     * @since 0.3.0
     */
    static FileDownloadConfiguration getFileDownloadConfiguration() {
        if (sInstance != null) {
            synchronized (sInstance.mInitLock) {
                if (sInstance != null) {
                    return sInstance.mConfiguration;
                }
            }
        }
        return null;
    }

    // -------------------------register download status listener with configuration-------------------------

    /**
     * register an OnFileDownloadStatusListener with Configuration
     *
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener impl
     * @param downloadStatusConfiguration  Configuration for the OnFileDownloadStatusListener impl
     * @since 0.3.0
     */
    void registerDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener, 
                                        DownloadStatusConfiguration downloadStatusConfiguration) {
        getDownloadTaskManager().registerDownloadStatusListener(onFileDownloadStatusListener, 
                downloadStatusConfiguration);
    }

    // -------------------------register & unregister download file change listener-------------------------

    /**
     * register a DownloadFileChangeListener with Configuration
     *
     * @param onDownloadFileChangeListener    OnDownloadFileChangeListener impl
     * @param downloadFileChangeConfiguration Configuration for the OnDownloadFileChangeListener impl
     * @since 0.3.0
     */
    void registerDownloadFileChangeListener(OnDownloadFileChangeListener onDownloadFileChangeListener, 
                                            DownloadFileChangeConfiguration downloadFileChangeConfiguration) {
        mDownloadFileCacher.registerDownloadFileChangeListener(onDownloadFileChangeListener, 
                downloadFileChangeConfiguration);
    }

    /**
     * unregister an OnDownloadFileChangeListener
     *
     * @param onDownloadFileChangeListener registered OnDownloadFileChangeListener impl
     * @since 0.3.0
     */
    void unregisterDownloadFileChangeListener(OnDownloadFileChangeListener onDownloadFileChangeListener) {
        mDownloadFileCacher.unregisterDownloadFileChangeListener(onDownloadFileChangeListener);
    }

    // -------------------------internal implements-------------------------

    /**
     * detect a big url file, which means can detect the url file bigger than 2G
     *
     * @param url                        file url
     * @param onDetectBigUrlFileListener OnDetectBigUrlFileListener impl
     * @param downloadConfiguration      download configuration
     * @since 0.3.2
     */
    void detect(String url, OnDetectBigUrlFileListener onDetectBigUrlFileListener, DownloadConfiguration 
            downloadConfiguration) {
        getDownloadTaskManager().detect(url, onDetectBigUrlFileListener, downloadConfiguration);
    }

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
     * @since 0.3.2
     */
    void createAndStart(String url, String saveDir, String fileName, DownloadConfiguration downloadConfiguration) {
        getDownloadTaskManager().createAndStart(url, saveDir, fileName, downloadConfiguration);
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
     * @since 0.3.2
     */
    void start(String url, DownloadConfiguration downloadConfiguration) {
        getDownloadTaskManager().start(url, downloadConfiguration);
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
     * @since 0.3.2
     */
    void start(List<String> urls, DownloadConfiguration downloadConfiguration) {
        getDownloadTaskManager().start(urls, downloadConfiguration);
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
     * @since 0.3.2
     */
    void reStart(String url, DownloadConfiguration downloadConfiguration) {
        getDownloadTaskManager().reStart(url, downloadConfiguration);
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
     * @since 0.3.2
     */
    void reStart(List<String> urls, DownloadConfiguration downloadConfiguration) {
        getDownloadTaskManager().reStart(urls, downloadConfiguration);

    }
}
