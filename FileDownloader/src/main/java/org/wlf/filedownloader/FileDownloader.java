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

    /**
     * get FileDownloadManager
     *
     * @return FileDownloadManager
     */
    private static FileDownloadManager getFileDownloadManager() {
        if (FileDownloadManager.getConfiguration() == null) {
            throw new IllegalStateException("please init the file-downloader by using " + FileDownloader.class
                    .getSimpleName() + " or " + FileDownloadManager.class.getSimpleName() + " if the version is below" +
                    " 0.2.0");
        }
        return FileDownloadManager.getInstance(FileDownloadManager.getConfiguration().getContext());
    }

    /**
     * init with a Configuration
     *
     * @param configuration Configuration
     * @see FileDownloadManager#init(FileDownloadConfiguration)
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
     * @return true means initialized
     * @see FileDownloadManager#isInit()
     */
    public static boolean isInit() {
        if (FileDownloadManager.getConfiguration() == null) {
            return false;
        }
        return getFileDownloadManager().isInit();
    }

    // --------------------------------------getters--------------------------------------

    /**
     * get DownloadFile by file url
     *
     * @param url file url
     * @return DownloadFile
     * @see FileDownloadManager#getDownloadFileByUrl(String)
     */
    public static DownloadFileInfo getDownloadFile(String url) {
        return getFileDownloadManager().getDownloadFile(url);
    }

    /**
     * get DownloadFile by save file path
     *
     * @param savePath save file path
     * @return DownloadFile
     * @see FileDownloadManager#getDownloadFileBySavePath(String)
     */
    public static DownloadFileInfo getDownloadFileBySavePath(String savePath) {
        return getFileDownloadManager().getDownloadFileBySavePath(savePath);
    }

    /**
     * get DownloadFile by temp file path
     *
     * @param tempPath temp file path
     * @return DownloadFile
     * @see FileDownloadManager#getDownloadFileByTempPath(String)
     */
    public static DownloadFileInfo getDownloadFileByTempPath(String tempPath) {
        return getFileDownloadManager().getDownloadFileByTempPath(tempPath);
    }

    /**
     * get all DownloadFiles
     *
     * @return all DownloadFiles
     * @see FileDownloadManager#getDownloadFiles()
     */
    public static List<DownloadFileInfo> getDownloadFiles() {
        return getFileDownloadManager().getDownloadFiles();
    }

    /**
     * get download file save dir
     *
     * @return download file save dir
     * @see FileDownloadManager#getDownloadDir()
     */
    public static String getDownloadDir() {
        return getFileDownloadManager().getDownloadDir();
    }

    // --------------------------------------register listeners--------------------------------------

    /**
     * register an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener the OnFileDownloadStatusListener impl
     * @see FileDownloadManager#registerDownloadStatusListener(OnFileDownloadStatusListener)
     */
    public static void registerDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        getFileDownloadManager().registerDownloadStatusListener(onFileDownloadStatusListener);
    }

    /**
     * unregister an OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener the OnFileDownloadStatusListener impl
     * @see FileDownloadManager#unregisterDownloadStatusListener(OnFileDownloadStatusListener)
     */
    public static void unregisterDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        getFileDownloadManager().unregisterDownloadStatusListener(onFileDownloadStatusListener);
    }

    /**
     * register an OnDownloadFileChangeListener
     *
     * @param onDownloadFileChangeListener the OnDownloadFileChangeListener impl
     * @see FileDownloadManager#registerDownloadFileChangeListener(OnDownloadFileChangeListener)
     */
    public static void registerDownloadFileChangeListener(OnDownloadFileChangeListener onDownloadFileChangeListener) {
        getFileDownloadManager().registerDownloadFileChangeListener(onDownloadFileChangeListener);
    }

    /**
     * unregister an OnDownloadFileChangeListener
     *
     * @param onDownloadFileChangeListener the OnDownloadFileChangeListener impl
     * @see FileDownloadManager#unregisterDownloadFileChangeListener(OnDownloadFileChangeListener)
     */
    public static void unregisterDownloadFileChangeListener(OnDownloadFileChangeListener onDownloadFileChangeListener) {
        getFileDownloadManager().unregisterDownloadFileChangeListener(onDownloadFileChangeListener);
    }

    // --------------------------------------detect url file--------------------------------------

    /**
     * detect url file
     *
     * @param url                     file url
     * @param onDetectUrlFileListener DetectUrlFileListener,recommend to use {@link OnDetectBigUrlFileListener} 
     *                                instead to support downloading the file more than 2G
     * @see FileDownloadManager#detect(String, OnDetectUrlFileListener)
     */
    public static void detect(String url, OnDetectUrlFileListener onDetectUrlFileListener) {
        getFileDownloadManager().detect(url, onDetectUrlFileListener);
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
     * @see FileDownloadManager#createAndStart(String, String, String)
     */
    public static void createAndStart(String url, String saveDir, String fileName) {
        getFileDownloadManager().createAndStart(url, saveDir, fileName);
    }

    /**
     * start/continue download
     * <br/>
     * if the caller cares for the download status，please ues {@link #registerDownloadStatusListener
     * (OnFileDownloadStatusListener)} to register the callback
     *
     * @param url file url
     * @see FileDownloadManager#start(String)
     */
    public static void start(String url) {
        getFileDownloadManager().start(url);
    }

    /**
     * start/continue multi download
     *
     * @param urls file urls
     * @see FileDownloadManager#start(List)
     */
    public static void start(List<String> urls) {
        getFileDownloadManager().start(urls);
    }

    // --------------------------------------pause download--------------------------------------

    /**
     * pause download
     *
     * @param url file url
     * @see FileDownloadManager#pause(String)
     */
    public static void pause(String url) {
        getFileDownloadManager().pause(url);
    }

    /**
     * pause multi download
     *
     * @param urls file urls
     * @see FileDownloadManager#pause(List)
     */
    public static void pause(List<String> urls) {
        getFileDownloadManager().pause(urls);
    }

    /**
     * pause all download
     *
     * @see FileDownloadManager#pauseAll()
     */
    public static void pauseAll() {
        getFileDownloadManager().pauseAll();
    }

    // --------------------------------------restart download--------------------------------------

    /**
     * restart download
     * <br/>
     * if the caller cares for the download status，please ues {@link #registerDownloadStatusListener
     * (OnFileDownloadStatusListener)} to register the callback
     *
     * @param url file url
     * @see FileDownloadManager#reStart(String)
     */
    public static void reStart(String url) {
        getFileDownloadManager().reStart(url);
    }

    /**
     * restart multi download
     * <br/>
     * if the caller cares for the download status，please ues {@link #registerDownloadStatusListener
     * (OnFileDownloadStatusListener)} to register the callback
     *
     * @param urls file urls
     * @see FileDownloadManager#reStart(List)
     */
    public static void reStart(List<String> urls) {
        getFileDownloadManager().reStart(urls);
    }

    // --------------------------------------move download--------------------------------------

    /**
     * move download
     *
     * @param url                        file url
     * @param newDirPath                 new dir path
     * @param onMoveDownloadFileListener MoveDownloadFileListener
     * @see FileDownloadManager#move(String, String, OnMoveDownloadFileListener)
     */
    public static void move(String url, String newDirPath, OnMoveDownloadFileListener onMoveDownloadFileListener) {
        getFileDownloadManager().move(url, newDirPath, onMoveDownloadFileListener);
    }

    /**
     * move multi downloads
     *
     * @param urls                        file mUrls
     * @param newDirPath                  new dir path
     * @param onMoveDownloadFilesListener MoveDownloadFilesListener
     * @return the control for the operation
     * @see FileDownloadManager#move(List, String, OnMoveDownloadFilesListener)
     */
    public static Control move(List<String> urls, String newDirPath, OnMoveDownloadFilesListener 
            onMoveDownloadFilesListener) {
        return getFileDownloadManager().move(urls, newDirPath, onMoveDownloadFilesListener);
    }

    // --------------------------------------delete download--------------------------------------

    /**
     * delete download
     *
     * @param url                          file url
     * @param deleteDownloadedFileInPath   whether delete file in path
     * @param onDeleteDownloadFileListener DeleteDownloadFileListener
     * @see FileDownloadManager#delete(String, boolean, OnDeleteDownloadFileListener)
     */
    public static void delete(String url, boolean deleteDownloadedFileInPath, OnDeleteDownloadFileListener 
            onDeleteDownloadFileListener) {
        getFileDownloadManager().delete(url, deleteDownloadedFileInPath, onDeleteDownloadFileListener);
    }

    /**
     * delete multi downloads
     *
     * @param urls                          file mUrls
     * @param deleteDownloadedFile          whether delete file in path
     * @param onDeleteDownloadFilesListener DeleteDownloadFilesListener
     * @return the control for the operation
     * @see FileDownloadManager#delete(List, boolean, OnDeleteDownloadFilesListener)
     */
    public static Control delete(List<String> urls, boolean deleteDownloadedFile, OnDeleteDownloadFilesListener 
            onDeleteDownloadFilesListener) {
        return getFileDownloadManager().delete(urls, deleteDownloadedFile, onDeleteDownloadFilesListener);
    }

    // --------------------------------------rename download--------------------------------------

    /**
     * rename download
     *
     * @param url                          file url
     * @param newFileName                  new file name
     * @param includedSuffix               true means the newFileName include the suffix
     * @param onRenameDownloadFileListener RenameDownloadFileListener
     * @see FileDownloadManager#rename(String, String, boolean, OnRenameDownloadFileListener)
     */
    public static void rename(String url, String newFileName, boolean includedSuffix, OnRenameDownloadFileListener 
            onRenameDownloadFileListener) {
        getFileDownloadManager().rename(url, newFileName, includedSuffix, onRenameDownloadFileListener);
    }

    /**
     * release resources
     *
     * @see FileDownloadManager#release()
     */
    public static void release() {
        if (FileDownloadManager.getConfiguration() != null && isInit()) {
            getFileDownloadManager().release();
        }
    }
}
