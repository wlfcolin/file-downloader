package org.wlf.filedownloader;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.wlf.filedownloader.DownloadFileCacher.DownloadStatusRecordException;
import org.wlf.filedownloader.base.Status;
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
import org.wlf.filedownloader.task.DeleteDownloadFileTask;
import org.wlf.filedownloader.task.DetectUrlFileTask;
import org.wlf.filedownloader.task.FileDownloadTask;
import org.wlf.filedownloader.task.FileDownloadTask.OnStopDownloadFileTaskFailReason;
import org.wlf.filedownloader.task.FileDownloadTask.OnStopFileDownloadTaskListener;
import org.wlf.filedownloader.task.FileDownloadTaskParamHelper;
import org.wlf.filedownloader.task.MoveDownloadFileTask;

import java.util.ArrayList;
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
    private DeleteDownloadFilesTask mDeleteDownloadFilesTask = null;// FIXME pause task
    /**
     * the task for move multi DownloadFiles
     */
    private MoveDownloadFilesTask mMoveDownloadFilesTask = null;// FIXME pause task

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
     * @return true表示已经出初始化
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

        Log.d(TAG, "异常恢复检查！");

        if (downloadFileInfos == null || downloadFileInfos.isEmpty()) {
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
                    // recover
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
                    // recover
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
        return mFileDownloadTaskMap.containsKey(url);
    }

    /**
     * get download task by file url
     *
     * @param url file url
     * @return download task
     */
    private FileDownloadTask getFileDownloadTask(String url) {
        return mFileDownloadTaskMap.get(url);
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
        return mDownloadFileCacher.getDownloadFileBySavePath(savePath);
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
    private void addAndRunFileDownloadTask(DownloadFileInfo downloadFileInfo, OnFileDownloadStatusListener onFileDownloadStatusListener) {
        // create
        FileDownloadTask fileDownloadTask = new FileDownloadTask(FileDownloadTaskParamHelper.createByDownloadFile(downloadFileInfo), mDownloadFileCacher);
        fileDownloadTask.setOnFileDownloadStatusListener(onFileDownloadStatusListener);
        // record
        mFileDownloadTaskMap.put(fileDownloadTask.getUrl(), fileDownloadTask);
        // start exec task
        mConfiguration.getFileDownloadEngine().execute(fileDownloadTask);
    }

    /**
     * notify download failed
     */
    private void notifyFileDownloadStatusFailed(final DownloadFileInfo downloadFileInfo, final OnFileDownloadStatusFailReason failReason, final OnFileDownloadStatusListener onFileDownloadStatusListener, boolean recordStatus) {
        // null
        if (downloadFileInfo == null) {
            // notify callback
            if (onFileDownloadStatusListener != null) {
                OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(downloadFileInfo, new OnFileDownloadStatusFailReason(failReason), onFileDownloadStatusListener);
            }
            return;
        }
        String url = downloadFileInfo.getUrl();
        if (recordStatus && isInFileDownloadTaskMap(url)) {
            // pause
            FileDownloadTask fileDownloadTask = getFileDownloadTask(url);
            fileDownloadTask.setOnStopFileDownloadTaskListener(new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {
                    // record status
                    try {
                        mDownloadFileCacher.recordStatus(downloadFileInfo.getUrl(), Status.DOWNLOAD_STATUS_ERROR, 0);
                    } catch (DownloadStatusRecordException e) {
                        e.printStackTrace();
                    }
                    // notify callback
                    if (onFileDownloadStatusListener != null) {
                        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(downloadFileInfo, failReason, onFileDownloadStatusListener);
                    }
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
                    // record status
                    try {
                        mDownloadFileCacher.recordStatus(downloadFileInfo.getUrl(), Status.DOWNLOAD_STATUS_ERROR, 0);
                    } catch (DownloadStatusRecordException e) {
                        e.printStackTrace();
                    }
                    // notify callback
                    if (onFileDownloadStatusListener != null) {
                        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(downloadFileInfo, new OnFileDownloadStatusFailReason(failReason), onFileDownloadStatusListener);
                    }
                }
            });
            fileDownloadTask.stop();
        } else {
            if (recordStatus) {
                // record status
                try {
                    mDownloadFileCacher.recordStatus(downloadFileInfo.getUrl(), Status.DOWNLOAD_STATUS_ERROR, 0);
                } catch (DownloadStatusRecordException e) {
                    e.printStackTrace();
                }
            }
            // notify callback
            if (onFileDownloadStatusListener != null) {
                OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(downloadFileInfo, failReason, onFileDownloadStatusListener);
            }
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
    public void detect(final String url, final OnDetectUrlFileListener onDetectUrlFileListener) {

        checkInit();

        // 1.prepare detectUrlFileTask
        DetectUrlFileTask detectUrlFileTask = new DetectUrlFileTask(url, mConfiguration.getFileDownloadDir(), mDetectUrlFileCacher, mDownloadFileCacher);
        detectUrlFileTask.setOnDetectUrlFileListener(onDetectUrlFileListener);

        // 2.start detectUrlFileTask
        addAndRunSupportTask(detectUrlFileTask);
    }

    // --------------------------------------create/continue download--------------------------------------

    /**
     * create download task,please use {@link #detect(String, OnDetectUrlFileListener)} to detect url file
     *
     * @param url                          file url
     * @param saveDir                      saveDir
     * @param fileName                     saveFileName
     * @param onFileDownloadStatusListener FileDownloadStatusListener
     */
    public void createAndStart(String url, String saveDir, String fileName, final OnFileDownloadStatusListener onFileDownloadStatusListener) {

        checkInit();

        // 1.get detect file
        DetectUrlFileInfo detectUrlFileInfo = getDetectUrlFile(url);
        if (detectUrlFileInfo == null) {
            // error detect file does not exist
            OnFileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason("detect file does not exist!", OnFileDownloadStatusFailReason.TYPE_FILE_NOT_DETECT);
            // notifyFileDownloadStatusFailed
            notifyFileDownloadStatusFailed(getDownloadFile(url), failReason, onFileDownloadStatusListener, false);
            return;
        }

        // 2.prepared to create task
        detectUrlFileInfo.setFileDir(saveDir);
        detectUrlFileInfo.setFileName(fileName);
        createAndStartByDetectUrlFile(detectUrlFileInfo, onFileDownloadStatusListener);
    }

    // create download task by using detectUrlFileInfo
    private void createAndStartByDetectUrlFile(DetectUrlFileInfo detectUrlFileInfo, OnFileDownloadStatusListener onFileDownloadStatusListener) {

        // 1.check detectUrlFileInfo
        if (detectUrlFileInfo == null) {
            // error detect file does not exist
            OnFileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason("detect file does not exist!", OnFileDownloadStatusFailReason.TYPE_FILE_NOT_DETECT);
            // notifyFileDownloadStatusFailed
            notifyFileDownloadStatusFailed(null, failReason, onFileDownloadStatusListener, false);
            return;
        }

        String url = detectUrlFileInfo.getUrl();

        // 2.whether task is in download map
        if (isInFileDownloadTaskMap(url)) {
            // error downloading
            OnFileDownloadStatusFailReason failReason = new OnFileDownloadStatusFailReason("the task has been in download task map!", OnFileDownloadStatusFailReason.TYPE_FILE_IS_DOWNLOADING);
            // notifyFileDownloadStatusFailed
            notifyFileDownloadStatusFailed(getDownloadFile(url), failReason, onFileDownloadStatusListener, false);
            return;
        }

        // 3.create downloadFileInfo
        DownloadFileInfo downloadFileInfo = new DownloadFileInfo(detectUrlFileInfo);
        // add to cache
        mDownloadFileCacher.addDownloadFile(downloadFileInfo);

        // 4.start download task
        startInternal(downloadFileInfo, onFileDownloadStatusListener);
    }

    /**
     * start download task
     */
    private void startInternal(DownloadFileInfo downloadFileInfo, OnFileDownloadStatusListener onFileDownloadStatusListener) {

        OnFileDownloadStatusFailReason failReason = null;// null means there are not errors

        // 1.check downloadFileInfo
        if (downloadFileInfo == null) {
            if (failReason == null) {
                // error the downloadFileInfo does not exist
                failReason = new OnFileDownloadStatusFailReason("the downloadFileInfo does not exist!", OnFileDownloadStatusFailReason.TYPE_NULL_POINTER);
            }
        } else {
            String url = downloadFileInfo.getUrl();
            // 2.whether task is in download map
            if (failReason == null && isInFileDownloadTaskMap(url)) {
                // error downloading
                failReason = new OnFileDownloadStatusFailReason("the task has been in download task map!", OnFileDownloadStatusFailReason.TYPE_FILE_IS_DOWNLOADING);
            }

        }

        // error
        if (failReason != null) {
            // notifyFileDownloadStatusFailed
            notifyFileDownloadStatusFailed(downloadFileInfo, failReason, onFileDownloadStatusListener, false);
            return;
        }

        // 2.start download task
        addAndRunFileDownloadTask(downloadFileInfo, onFileDownloadStatusListener);
    }

    /**
     * start/continue download
     *
     * @param url                          file url
     * @param onFileDownloadStatusListener FileDownloadStatusListener
     */
    public void start(String url, final OnFileDownloadStatusListener onFileDownloadStatusListener) {

        checkInit();

        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        // has been downloaded
        if (downloadFileInfo != null) {
            // continue download task
            startInternal(downloadFileInfo, onFileDownloadStatusListener);
        }
        // not download
        else {
            DetectUrlFileInfo detectUrlFileInfo = getDetectUrlFile(url);
            // detected
            if (detectUrlFileInfo != null) {
                // create download task
                createAndStartByDetectUrlFile(detectUrlFileInfo, onFileDownloadStatusListener);
            }
            // not detect
            else {
                // detect
                detect(url, new OnDetectUrlFileListener() {
                    @Override
                    public void onDetectUrlFileFailed(String url, DetectUrlFileFailReason failReason) {
                        // 1.notifyFileDownloadStatusFailed
                        notifyFileDownloadStatusFailed(getDownloadFile(url), new OnFileDownloadStatusFailReason(failReason), onFileDownloadStatusListener, false);
                    }

                    @Override
                    public void onDetectUrlFileExist(String url) {
                        // continue download task
                        startInternal(getDownloadFile(url), onFileDownloadStatusListener);
                    }

                    @Override
                    public void onDetectNewDownloadFile(String url, String fileName, String savedDir, int fileSize) {
                        // create and start
                        createAndStart(url, savedDir, fileName, onFileDownloadStatusListener);
                    }
                });
            }
        }
    }

    /**
     * start/continue multi download
     *
     * @param urls                         file urls
     * @param onFileDownloadStatusListener FileDownloadStatusListener
     */
    public void start(List<String> urls, OnFileDownloadStatusListener onFileDownloadStatusListener) {
        for (String url : urls) {
            if (TextUtils.isEmpty(url)) {
                continue;
            }
            start(url, onFileDownloadStatusListener);
        }
    }

    // --------------------------------------pause download--------------------------------------

    /**
     * pause download task
     */
    private boolean pauseInternal(String url, final OnStopFileDownloadTaskListener onStopFileDownloadTaskListener) {

        checkInit();

        // get download task info
        FileDownloadTask fileDownloadTask = mFileDownloadTaskMap.get(url);
        if (fileDownloadTask != null) {
            // set OnStopFileDownloadTaskListener
            fileDownloadTask.setOnStopFileDownloadTaskListener(new OnStopFileDownloadTaskListener() {

                @Override
                public void onStopFileDownloadTaskSucceed(String url) {
                    onFileDownloadTaskStopped(url);
                    if (onStopFileDownloadTaskListener != null) {
                        onStopFileDownloadTaskListener.onStopFileDownloadTaskSucceed(url);
                    }
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
                    if (onStopFileDownloadTaskListener != null) {
                        onStopFileDownloadTaskListener.onStopFileDownloadTaskFailed(url, failReason);
                    }
                }
            });
            // stop download
            fileDownloadTask.stop();
            return true;
        } else {
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
     * @param urls file urls
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
     *
     * @param url                          file url
     * @param onFileDownloadStatusListener FileDownloadStatusListener
     */
    public void reStart(String url, final OnFileDownloadStatusListener onFileDownloadStatusListener) {

        // downloading
        if (isInFileDownloadTaskMap(url)) {
            // pause
            pauseInternal(url, new OnStopFileDownloadTaskListener() {

                @Override
                public void onStopFileDownloadTaskSucceed(String url) {
                    start(url, onFileDownloadStatusListener);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
                    // error FIXME
                    notifyFileDownloadStatusFailed(getDownloadFile(url), new OnFileDownloadStatusFailReason(failReason), onFileDownloadStatusListener, false);
                }
            });
        } else {
            start(url, onFileDownloadStatusListener);
        }
    }

    /**
     * restart multi download
     *
     * @param urls                         file urls
     * @param onFileDownloadStatusListener FileDownloadStatusListener
     */
    public void reStart(List<String> urls, OnFileDownloadStatusListener onFileDownloadStatusListener) {
        for (String url : urls) {
            if (TextUtils.isEmpty(url)) {
                continue;
            }
            reStart(url, onFileDownloadStatusListener);
        }
    }

    // --------------------------------------delete download--------------------------------------

    /**
     * delete download
     */
    private void deleteInternal(String url, final boolean deleteDownloadedFileInPath, final OnDeleteDownloadFileListener onDeleteDownloadFileListener) {

        // create delete download task
        DeleteDownloadFileTask deleteDownloadFileTask = new DeleteDownloadFileTask(url, deleteDownloadedFileInPath, mDownloadFileCacher);
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
    public void delete(String url, final boolean deleteDownloadedFileInPath, final OnDeleteDownloadFileListener onDeleteDownloadFileListener) {

        checkInit();

        FileDownloadTask task = mFileDownloadTaskMap.get(url);
        if (task == null || task.isStopped()) {
            deleteInternal(url, deleteDownloadedFileInPath, onDeleteDownloadFileListener);
        } else {
            // pause
            pauseInternal(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {
                    deleteInternal(url, deleteDownloadedFileInPath, onDeleteDownloadFileListener);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
                    if (onDeleteDownloadFileListener != null) {
                        OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFileFailed(getDownloadFile(url), new OnDeleteDownloadFileFailReason(failReason), onDeleteDownloadFileListener);
                    }
                }
            });
        }
    }

    /**
     * delete multi downloads
     *
     * @param urls                          file urls
     * @param deleteDownloadedFile          whether delete file in path
     * @param onDeleteDownloadFilesListener DeleteDownloadFilesListener
     */
    public void delete(final List<String> urls, final boolean deleteDownloadedFile, final OnDeleteDownloadFilesListener onDeleteDownloadFilesListener) {

        checkInit();

        if (mDeleteDownloadFilesTask != null && !mDeleteDownloadFilesTask.isStop()) {
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
     * @param onMoveDownloadFileListener MoveDownloadFileListener
     */
    public void move(final String url, final String newDirPath, final OnMoveDownloadFileListener onMoveDownloadFileListener) {

        checkInit();
        if (isInFileDownloadTaskMap(url)) {
            // pause
            pauseInternal(url, new OnStopFileDownloadTaskListener() {

                @Override
                public void onStopFileDownloadTaskSucceed(String url) {
                    moveInternal(url, newDirPath, onMoveDownloadFileListener);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
                    if (onMoveDownloadFileListener != null) {
                        OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(getDownloadFile(url), new OnMoveDownloadFileFailReason(failReason), onMoveDownloadFileListener);
                    }
                }
            });
        } else {
            moveInternal(url, newDirPath, onMoveDownloadFileListener);
        }
    }

    /**
     * move multi downloads
     *
     * @param urls                        file urls
     * @param newDirPath                  new dir path
     * @param onMoveDownloadFilesListener MoveDownloadFilesListener
     */
    public void move(List<String> urls, String newDirPath, final OnMoveDownloadFilesListener onMoveDownloadFilesListener) {

        checkInit();

        if (mMoveDownloadFilesTask != null && !mMoveDownloadFilesTask.isStop()) {
            // moving
            return;
        }

        MoveDownloadFilesTask moveDownloadFilesTask = new MoveDownloadFilesTask(urls, newDirPath);
        moveDownloadFilesTask.setOnMoveDownloadFilesListener(onMoveDownloadFilesListener);

        // start task
        new Thread(moveDownloadFilesTask).start();

        this.mMoveDownloadFilesTask = moveDownloadFilesTask;
    }

    /**
     * rename download
     *
     * @param url                          file url
     * @param newFileName                  new file name
     * @param onRenameDownloadFileListener RenameDownloadFileListener
     */
    public void rename(String url, String newFileName, OnRenameDownloadFileListener onRenameDownloadFileListener) {

        checkInit();

        // TODO 待实现

        if (isInFileDownloadTaskMap(url)) {
            //            if(onRenameDownloadFileListener != null){
            //                onRenameDownloadFileListener.onRenameDownloadFileFailed(getDownloadFile(url),);
            //            }
            // 正在下载不允许重命名
        }

    }

    // ======================================inner classes(operation tasks)======================================

    // --------------------------------------delete multi download task--------------------------------------

    /**
     * delete multi download task
     */
    private class DeleteDownloadFilesTask implements Runnable {

        private List<String> urls;
        private boolean deleteDownloadedFile;
        private OnDeleteDownloadFilesListener mOnDeleteDownloadFilesListener;

        private boolean isStop = false;
        private boolean hasReturn = false;

        private final List<DownloadFileInfo> downloadFilesNeedDelete = new ArrayList<DownloadFileInfo>();
        final List<DownloadFileInfo> downloadFilesDeleted = new ArrayList<DownloadFileInfo>();
        final List<DownloadFileInfo> downloadFilesSkip = new ArrayList<DownloadFileInfo>();

        public DeleteDownloadFilesTask(List<String> urls, boolean deleteDownloadedFile) {
            super();
            this.urls = urls;
            this.deleteDownloadedFile = deleteDownloadedFile;
        }

        public void setOnDeleteDownloadFilesListener(OnDeleteDownloadFilesListener onDeleteDownloadFilesListener) {
            this.mOnDeleteDownloadFilesListener = onDeleteDownloadFilesListener;
        }

        public void stop(boolean stopFlag) {
            this.isStop = stopFlag;
        }

        public boolean isStop() {
            return isStop;
        }

        // on delete finish
        private void onDeleteDownloadFilesCompleted() {
            if (hasReturn) {
                return;
            }
            if (mOnDeleteDownloadFilesListener != null) {
                mOnDeleteDownloadFilesListener.onDeleteDownloadFilesCompleted(downloadFilesNeedDelete, downloadFilesDeleted);
            }
            hasReturn = true;
            isStop = true;
        }

        @Override
        public void run() {

            // get DownloadFiles by urls
            for (String url : urls) {
                if (TextUtils.isEmpty(url)) {
                    continue;
                }
                downloadFilesNeedDelete.add(getDownloadFile(url));
            }

            // prepare to delete
            if (mOnDeleteDownloadFilesListener != null) {
                OnDeleteDownloadFilesListener.MainThreadHelper.onDeleteDownloadFilePrepared(downloadFilesNeedDelete, mOnDeleteDownloadFilesListener);
            }

            // delete
            for (int i = 0; i < downloadFilesNeedDelete.size(); i++) {
                if (isStop) {
                    // notify delete finish
                    onDeleteDownloadFilesCompleted();
                    break;
                }

                DownloadFileInfo downloadFileInfo = downloadFilesNeedDelete.get(i);

                if (downloadFileInfo == null) {
                    continue;
                }

                final int count = i;

                String url = downloadFileInfo.getUrl();

                // listen single download file deleted
                final OnDeleteDownloadFileListener onDeleteDownloadFileListener = new OnDeleteDownloadFileListener() {

                    @Override
                    public void onDeleteDownloadFilePrepared(DownloadFileInfo downloadFileNeedDelete) {
                        if (isStop) {
                            // notify delete finish
                            onDeleteDownloadFilesCompleted();
                            return;
                        }
                        // start new delete
                        if (mOnDeleteDownloadFilesListener != null) {
                            mOnDeleteDownloadFilesListener.onDeletingDownloadFiles(downloadFilesNeedDelete, downloadFilesDeleted, downloadFilesSkip, downloadFileNeedDelete);
                        }
                    }

                    @Override
                    public void onDeleteDownloadFileSuccess(DownloadFileInfo downloadFileDeleted) {
                        // delete succeed
                        downloadFilesDeleted.add(downloadFileDeleted);

                        // if the last one to delete,notify finish the operation
                        if (count == downloadFilesNeedDelete.size() - 1) {
                            onDeleteDownloadFilesCompleted();
                        }
                    }

                    @Override
                    public void onDeleteDownloadFileFailed(DownloadFileInfo downloadFileInfo, OnDeleteDownloadFileFailReason failReason) {
                        // delete failed
                        downloadFilesSkip.add(downloadFileInfo);

                        // if the last one to delete,notify finish the operation
                        if (count == downloadFilesNeedDelete.size() - 1) {
                            onDeleteDownloadFilesCompleted();
                        }
                    }
                };

                // start to delete single download
                if (isInFileDownloadTaskMap(url)) {
                    // pause
                    pauseInternal(url, new OnStopFileDownloadTaskListener() {

                        @Override
                        public void onStopFileDownloadTaskSucceed(String url) {
                            if (isStop) {
                                // notify delete finish
                                onDeleteDownloadFilesCompleted();
                                return;
                            }
                            // start delete
                            deleteInternal(url, deleteDownloadedFile, onDeleteDownloadFileListener);
                        }

                        @Override
                        public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
                            if (onDeleteDownloadFileListener != null) {
                                OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFileFailed(getDownloadFile(url), new OnDeleteDownloadFileFailReason(failReason), onDeleteDownloadFileListener);
                            }
                        }
                    });
                } else {
                    if (isStop) {
                        // notify delete finish
                        onDeleteDownloadFilesCompleted();
                        return;
                    }
                    // delete
                    deleteInternal(url, deleteDownloadedFile, onDeleteDownloadFileListener);
                }
            }
        }
    }

    // --------------------------------------move multi download task--------------------------------------

    /**
     * move multi download task
     */
    private class MoveDownloadFilesTask implements Runnable {

        private List<String> urls;
        private String newDirPath;
        private OnMoveDownloadFilesListener mOnMoveDownloadFilesListener;

        private boolean isStop = false;
        private boolean hasReturn = false;

        final List<DownloadFileInfo> downloadFilesNeedMove = new ArrayList<DownloadFileInfo>();
        final List<DownloadFileInfo> downloadFilesMoved = new ArrayList<DownloadFileInfo>();
        final List<DownloadFileInfo> downloadFilesSkip = new ArrayList<DownloadFileInfo>();

        public MoveDownloadFilesTask(List<String> urls, String newDirPath) {
            super();
            this.urls = urls;
            this.newDirPath = newDirPath;
        }

        public void setOnMoveDownloadFilesListener(OnMoveDownloadFilesListener onMoveDownloadFilesListener) {
            this.mOnMoveDownloadFilesListener = onMoveDownloadFilesListener;
        }

        public void stop(boolean stopFlag) {
            this.isStop = stopFlag;
        }

        public boolean isStop() {
            return isStop;
        }

        // move finish
        private void onMoveDownloadFilesCompleted() {
            if (hasReturn) {
                return;
            }
            hasReturn = true;
            if (mOnMoveDownloadFilesListener != null) {
                mOnMoveDownloadFilesListener.onMoveDownloadFilesCompleted(downloadFilesNeedMove, downloadFilesMoved);
            }
            isStop = true;
        }

        @Override
        public void run() {

            // get DownloadFiles by urls
            for (String url : urls) {
                if (TextUtils.isEmpty(url)) {
                    continue;
                }
                downloadFilesNeedMove.add(getDownloadFile(url));
            }

            // prepare to move
            if (mOnMoveDownloadFilesListener != null) {
                mOnMoveDownloadFilesListener.onMoveDownloadFilesPrepared(downloadFilesNeedMove);
            }

            // move
            for (int i = 0; i < downloadFilesNeedMove.size(); i++) {
                if (isStop) {
                    // notify move finish
                    onMoveDownloadFilesCompleted();
                    break;
                }

                DownloadFileInfo downloadFileInfo = downloadFilesNeedMove.get(i);

                if (downloadFileInfo == null) {
                    continue;
                }

                final int count = i;

                String url = downloadFileInfo.getUrl();

                // listen single download file moved
                final OnMoveDownloadFileListener onMoveDownloadFileListener = new OnMoveDownloadFileListener() {

                    @Override
                    public void onMoveDownloadFilePrepared(DownloadFileInfo downloadFileNeedToMove) {
                        if (isStop) {
                            // notify move finish
                            onMoveDownloadFilesCompleted();
                            return;
                        }
                        // start move
                        if (mOnMoveDownloadFilesListener != null) {
                            mOnMoveDownloadFilesListener.onMovingDownloadFiles(downloadFilesNeedMove, downloadFilesMoved, downloadFilesSkip, downloadFileNeedToMove);
                        }
                    }

                    @Override
                    public void onMoveDownloadFileSuccess(DownloadFileInfo downloadFileMoved) {
                        // move succeed
                        downloadFilesMoved.add(downloadFileMoved);

                        // if the last one to delete,notify finish the operation
                        if (count == downloadFilesNeedMove.size() - 1) {
                            onMoveDownloadFilesCompleted();
                        }
                    }

                    @Override
                    public void onMoveDownloadFileFailed(DownloadFileInfo downloadFileInfo, OnMoveDownloadFileFailReason failReason) {
                        // move failed
                        downloadFilesSkip.add(downloadFileInfo);

                        // if the last one to delete,notify finish the operation
                        if (count == downloadFilesNeedMove.size() - 1) {
                            onMoveDownloadFilesCompleted();
                        }
                    }

                };

                // start move
                if (isInFileDownloadTaskMap(url)) {
                    // pause
                    pauseInternal(url, new OnStopFileDownloadTaskListener() {

                        @Override
                        public void onStopFileDownloadTaskSucceed(String url) {
                            if (isStop) {
                                // notify move finish
                                onMoveDownloadFilesCompleted();
                                return;
                            }
                            moveInternal(url, newDirPath, onMoveDownloadFileListener);
                        }

                        @Override
                        public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {
                            if (onMoveDownloadFileListener != null) {
                                OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(getDownloadFile(url), new OnMoveDownloadFileFailReason(failReason), onMoveDownloadFileListener);
                            }
                        }
                    });
                } else {
                    if (isStop) {
                        // notify move finish
                        onMoveDownloadFilesCompleted();
                        return;
                    }
                    moveInternal(url, newDirPath, onMoveDownloadFileListener);
                }
            }
        }
    }
}
