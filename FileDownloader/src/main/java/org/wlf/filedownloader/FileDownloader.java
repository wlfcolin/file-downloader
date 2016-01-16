package org.wlf.filedownloader;

import android.content.Context;

import org.wlf.filedownloader.base.Control;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener;
import org.wlf.filedownloader.listener.OnDeleteDownloadFilesListener;
import org.wlf.filedownloader.listener.OnDetectBigUrlFileListener;
import org.wlf.filedownloader.listener.OnDetectUrlFileListener;
import org.wlf.filedownloader.listener.OnDownloadFileChangeListener;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener;
import org.wlf.filedownloader.listener.OnMoveDownloadFilesListener;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener;
import org.wlf.filedownloader.listener.OnRetryableFileDownloadStatusListener;

import java.util.List;

/**
 * FileDownloader
 * <br/>
 * 文件下载框架
 *
 * @author wlf(Andy)
 * @datetime 2015-12-10 14:24 GMT+8
 * @email 411086563@qq.com
 * @since 0.2.0
 */
public final class FileDownloader {

    // --------------------------------------lifecycle--------------------------------------

    /**
     * init with a Configuration
     *
     * @param configuration Configuration
     */
    public static void init(FileDownloadConfiguration configuration) {
        if (configuration == null) {
            return;
        }
        Context context = configuration.getContext();
        FileDownloadManager.getInstance(context).init(configuration);
    }

    /**
     * whether the file-downloader is initialized
     *
     * @return true means the file-downloader has been initialized
     */
    public static boolean isInit() {
        if (FileDownloadManager.getFileDownloadConfiguration() == null) {
            return false;
        }
        return getFileDownloadManager().isInit();
    }

    /**
     * release resources
     */
    public static void release() {
        if (FileDownloadManager.getFileDownloadConfiguration() != null && isInit()) {
            getFileDownloadManager().release();
        }
    }

    // --------------------------------------getters--------------------------------------

    /**
     * get FileDownloadManager
     *
     * @return FileDownloadManager
     */
    private static FileDownloadManager getFileDownloadManager() {
        if (FileDownloadManager.getFileDownloadConfiguration() == null) {
            throw new IllegalStateException("Please init the file-downloader by using " + FileDownloader.class
                    .getSimpleName() + ".init(FileDownloadConfiguration) !");
        }
        return FileDownloadManager.getInstance(FileDownloadManager.getFileDownloadConfiguration().getContext());
    }

    /**
     * get DownloadFile by file url
     *
     * @param url file url
     * @return DownloadFile
     */
    public static DownloadFileInfo getDownloadFile(String url) {
        return getFileDownloadManager().getDownloadFile(url);
    }

    /**
     * get DownloadFile by save file path
     *
     * @param savePath save file path
     * @return DownloadFile
     */
    public static DownloadFileInfo getDownloadFileBySavePath(String savePath) {
        return getFileDownloadManager().getDownloadFileBySavePath(savePath);
    }

    /**
     * get DownloadFile by temp file path
     *
     * @param tempPath temp file path
     * @return DownloadFile
     */
    public static DownloadFileInfo getDownloadFileByTempPath(String tempPath) {
        return getFileDownloadManager().getDownloadFileByTempPath(tempPath);
    }

    /**
     * get all DownloadFiles
     *
     * @return all DownloadFiles
     */
    public static List<DownloadFileInfo> getDownloadFiles() {
        return getFileDownloadManager().getDownloadFiles();
    }

    /**
     * get download save dir
     *
     * @return download save dir
     */
    public static String getDownloadDir() {
        return getFileDownloadManager().getDownloadDir();
    }

    // --------------------------------------register & unregister listeners--------------------------------------

    /**
     * register an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener impl, or use {@link
     *                                     OnRetryableFileDownloadStatusListener}, which can retry download when
     *                                     download failed once when use {@link FileDownloadConfiguration
     *                                     .Builder#configRetryDownloadTimes(int)} to config the retry times
     */
    public static void registerDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        getFileDownloadManager().registerDownloadStatusListener(onFileDownloadStatusListener);
    }

    /**
     * register an OnFileDownloadStatusListener with Configuration
     *
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener impl, or use {@link
     *                                     OnRetryableFileDownloadStatusListener}, which can retry download when
     *                                     download failed once when use {@link FileDownloadConfiguration
     *                                     .Builder#configRetryDownloadTimes(int)} to config the retry times
     * @param downloadStatusConfiguration  Configuration for the OnFileDownloadStatusListener impl
     * @since 0.3.0
     */
    public static void registerDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener, 
                                                      DownloadStatusConfiguration downloadStatusConfiguration) {
        getFileDownloadManager().registerDownloadStatusListener(onFileDownloadStatusListener, 
                downloadStatusConfiguration);
    }

    //    /**
    //     * register an OnRetryableFileDownloadStatusListener, which can retry download when download failed once
    //     * <br/>
    //     * use {@link FileDownloadConfiguration.Builder#configRetryDownloadTimes(int)} to config the retry times
    //     *
    //     * @param onRetryableFileDownloadStatusListener OnRetryableFileDownloadStatusListener impl
    //     * @since 0.3.0
    //     */
    //    public static void registerDownloadStatusListener(OnRetryableFileDownloadStatusListener 
    //                                                              onRetryableFileDownloadStatusListener) {
    //        getFileDownloadManager().registerDownloadStatusListener(onRetryableFileDownloadStatusListener);
    //    }
    //
    //    /**
    //     * register an OnRetryableFileDownloadStatusListener with Configuration, which can retry download when 
    // download
    //     * failed once
    //     * <br/>
    //     * use {@link FileDownloadConfiguration.Builder#configRetryDownloadTimes(int)} to config the retry times
    //     *
    //     * @param onRetryableFileDownloadStatusListener OnRetryableFileDownloadStatusListener impl
    //     * @param downloadStatusConfiguration           Configuration for the OnRetryableFileDownloadStatusListener
    // impl
    //     * @since 0.3.0
    //     */
    //    public static void registerDownloadStatusListener(OnRetryableFileDownloadStatusListener 
    //                                                              onRetryableFileDownloadStatusListener, 
    //                                                      DownloadStatusConfiguration downloadStatusConfiguration) {
    //        getFileDownloadManager().registerDownloadStatusListener(onRetryableFileDownloadStatusListener);
    //    }

    /**
     * unregister an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener registered OnFileDownloadStatusListener or
     *                                     OnRetryableFileDownloadStatusListener impl
     */
    public static void unregisterDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        getFileDownloadManager().unregisterDownloadStatusListener(onFileDownloadStatusListener);
    }

    /**
     * register an OnDownloadFileChangeListener
     *
     * @param onDownloadFileChangeListener OnDownloadFileChangeListener impl
     */
    public static void registerDownloadFileChangeListener(OnDownloadFileChangeListener onDownloadFileChangeListener) {
        getFileDownloadManager().registerDownloadFileChangeListener(onDownloadFileChangeListener, null);
    }

    /**
     * register a DownloadFileChangeListener with Configuration
     *
     * @param onDownloadFileChangeListener    OnDownloadFileChangeListener impl
     * @param downloadFileChangeConfiguration Configuration for the OnDownloadFileChangeListener impl
     * @since 0.3.0
     */
    public void registerDownloadFileChangeListener(OnDownloadFileChangeListener onDownloadFileChangeListener, 
                                                   DownloadFileChangeConfiguration downloadFileChangeConfiguration) {
        getFileDownloadManager().registerDownloadFileChangeListener(onDownloadFileChangeListener, 
                downloadFileChangeConfiguration);
    }

    /**
     * unregister an OnDownloadFileChangeListener
     *
     * @param onDownloadFileChangeListener registered OnDownloadFileChangeListener impl
     */
    public static void unregisterDownloadFileChangeListener(OnDownloadFileChangeListener onDownloadFileChangeListener) {
        getFileDownloadManager().unregisterDownloadFileChangeListener(onDownloadFileChangeListener);
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
    public static void detect(String url, OnDetectUrlFileListener onDetectUrlFileListener) {
        getFileDownloadManager().detect(url, onDetectUrlFileListener);
    }

    /**
     * detect a big url file, which means can detect the url file bigger than 2G
     *
     * @param url                        file url
     * @param onDetectBigUrlFileListener OnDetectBigUrlFileListener impl
     * @since 0.3.0
     */
    public static void detect(String url, OnDetectBigUrlFileListener onDetectBigUrlFileListener) {
        getFileDownloadManager().detect(url, onDetectBigUrlFileListener);
    }

    // --------------------------------------create/continue downloads--------------------------------------

    /**
     * create a new download after detected a url file by using {@link #detect(String, OnDetectBigUrlFileListener)}
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br/>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener)}
     * <br>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener, DownloadStatusConfiguration)}
     *
     * @param url      file url
     * @param saveDir  saveDir
     * @param fileName saveFileName
     */
    public static void createAndStart(String url, String saveDir, String fileName) {
        getFileDownloadManager().createAndStart(url, saveDir, fileName);
    }

    /**
     * start/continue a download
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br/>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener)}
     * <br>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener, DownloadStatusConfiguration)}
     *
     * @param url file url
     */
    public static void start(String url) {
        getFileDownloadManager().start(url);
    }

    /**
     * start/continue multi downloads
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br/>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener)}
     * <br>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener, DownloadStatusConfiguration)}
     *
     * @param urls file urls
     */
    public static void start(List<String> urls) {
        getFileDownloadManager().start(urls);
    }

    // --------------------------------------pause downloads--------------------------------------

    /**
     * pause a download
     *
     * @param url file url
     */
    public static void pause(String url) {
        getFileDownloadManager().pause(url);
    }

    /**
     * pause multi downloads
     *
     * @param urls file urls
     */
    public static void pause(List<String> urls) {
        getFileDownloadManager().pause(urls);
    }

    /**
     * pause all downloads
     */
    public static void pauseAll() {
        getFileDownloadManager().pauseAll();
    }

    // --------------------------------------restart downloads--------------------------------------

    /**
     * restart a download
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br/>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener)}
     * <br>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener, DownloadStatusConfiguration)}
     *
     * @param url file url
     */
    public static void reStart(String url) {
        getFileDownloadManager().reStart(url);
    }

    /**
     * restart multi downloads
     * <br/>
     * if the caller cares for the download status, please register an listener before by using
     * <br/>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener)}
     * <br>
     * {@link #registerDownloadStatusListener(OnFileDownloadStatusListener, DownloadStatusConfiguration)}
     *
     * @param urls file urls
     */
    public static void reStart(List<String> urls) {
        getFileDownloadManager().reStart(urls);
    }

    // --------------------------------------move download files--------------------------------------

    /**
     * move a download file
     *
     * @param url                        file url
     * @param newDirPath                 new dir path
     * @param onMoveDownloadFileListener OnMoveDownloadFileListener impl
     */
    public static void move(String url, String newDirPath, OnMoveDownloadFileListener onMoveDownloadFileListener) {
        getFileDownloadManager().move(url, newDirPath, onMoveDownloadFileListener);
    }

    /**
     * move multi download files
     *
     * @param urls                        file urls
     * @param newDirPath                  new dir path
     * @param onMoveDownloadFilesListener OnMoveDownloadFilesListener impl
     * @return a control for the operation
     */
    public static Control move(List<String> urls, String newDirPath, OnMoveDownloadFilesListener 
            onMoveDownloadFilesListener) {
        return getFileDownloadManager().move(urls, newDirPath, onMoveDownloadFilesListener);
    }

    // --------------------------------------delete download files--------------------------------------

    /**
     * delete a download file
     *
     * @param url                          file url
     * @param deleteDownloadedFileInPath   whether delete file in path
     * @param onDeleteDownloadFileListener OnDeleteDownloadFileListener impl
     */
    public static void delete(String url, boolean deleteDownloadedFileInPath, OnDeleteDownloadFileListener 
            onDeleteDownloadFileListener) {
        getFileDownloadManager().delete(url, deleteDownloadedFileInPath, onDeleteDownloadFileListener);
    }

    /**
     * delete multi download files
     *
     * @param urls                          file urls
     * @param deleteDownloadedFile          whether delete file in path
     * @param onDeleteDownloadFilesListener OnDeleteDownloadFilesListener impl
     * @return a control for the operation
     */
    public static Control delete(List<String> urls, boolean deleteDownloadedFile, OnDeleteDownloadFilesListener 
            onDeleteDownloadFilesListener) {
        return getFileDownloadManager().delete(urls, deleteDownloadedFile, onDeleteDownloadFilesListener);
    }

    // --------------------------------------rename download files--------------------------------------

    /**
     * rename a download file
     *
     * @param url                          file url
     * @param newFileName                  new file name
     * @param includedSuffix               true means the newFileName has been included the suffix, otherwise the
     *                                     newFileName not include the suffix
     * @param onRenameDownloadFileListener OnRenameDownloadFileListener impl
     */
    public static void rename(String url, String newFileName, boolean includedSuffix, OnRenameDownloadFileListener 
            onRenameDownloadFileListener) {
        getFileDownloadManager().rename(url, newFileName, includedSuffix, onRenameDownloadFileListener);
    }
}
