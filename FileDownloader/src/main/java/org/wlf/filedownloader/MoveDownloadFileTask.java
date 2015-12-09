package org.wlf.filedownloader;

import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener.OnMoveDownloadFileFailReason;
import org.wlf.filedownloader.listener.OnSyncMoveDownloadFileListener;
import org.wlf.filedownloader.util.FileUtil;

import java.io.File;

/**
 * move download file
 * <br/>
 * 移动下载文件任务
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class MoveDownloadFileTask implements Runnable {

    private static final String TAG = MoveDownloadFileTask.class.getSimpleName();

    private String mUrl;
    private String mNewDirPath;
    private DownloadFileCacher mFileDownloadCacher;
    private boolean isSyncCallback = false;

    private OnMoveDownloadFileListener mOnMoveDownloadFileListener;

    public MoveDownloadFileTask(String url, String newDirPath, DownloadFileCacher fileDownloadCacher) {
        super();
        this.mUrl = url;
        this.mNewDirPath = newDirPath;
        this.mFileDownloadCacher = fileDownloadCacher;
    }

    /**
     * set MoveDownloadFileListener
     *
     * @param onMoveDownloadFileListener MoveDownloadFileListener
     */
    public void setOnMoveDownloadFileListener(OnMoveDownloadFileListener onMoveDownloadFileListener) {
        this.mOnMoveDownloadFileListener = onMoveDownloadFileListener;
    }

    // package use only

    /**
     * enable the callback sync
     */
    void enableSyncCallback() {
        isSyncCallback = true;
    }

    @Override
    public void run() {

        DownloadFileInfo downloadFileInfo = mFileDownloadCacher.getDownloadFile(mUrl);

        // check null
        if (downloadFileInfo == null) {
            notifyFailed(downloadFileInfo, new OnMoveDownloadFileFailReason("the DownloadFile is empty!", 
                    OnMoveDownloadFileFailReason.TYPE_NULL_POINTER));
            return;
        }

        // prepared
        notifyPrepared(downloadFileInfo);

        // check status
        File oldFile = null;
        File newFile = null;
        switch (downloadFileInfo.getStatus()) {
            // complete download,get the saveFilePath
            case Status.DOWNLOAD_STATUS_COMPLETED:
                oldFile = new File(downloadFileInfo.getFileDir(), downloadFileInfo.getFileName());
                newFile = new File(mNewDirPath, downloadFileInfo.getFileName());
                break;
            // paused,not complete
            case Status.DOWNLOAD_STATUS_PAUSED:
                oldFile = new File(downloadFileInfo.getFileDir(), downloadFileInfo.getTempFileName());
                newFile = new File(mNewDirPath, downloadFileInfo.getTempFileName());
                break;
            default:
                // status error
                notifyFailed(downloadFileInfo, new OnMoveDownloadFileFailReason("DownloadFile status error!", 
                        OnMoveDownloadFileFailReason.TYPE_FILE_STATUS_ERROR));
                return;
        }

        // check original file
        if (oldFile == null || !oldFile.exists()) {
            notifyFailed(downloadFileInfo, new OnMoveDownloadFileFailReason("the original file does not exist!", 
                    OnMoveDownloadFileFailReason.TYPE_ORIGINAL_FILE_NOT_EXIST));
            return;
        }

        // check new file
        if (newFile != null && newFile.exists()) {
            notifyFailed(downloadFileInfo, new OnMoveDownloadFileFailReason("the target file exist!", 
                    OnMoveDownloadFileFailReason.TYPE_TARGET_FILE_EXIST));
            return;
        }

        // create ParentFile of the newFile if not exists
        if (newFile != null && !newFile.getParentFile().exists()) {
            FileUtil.createFileParentDir(newFile.getAbsolutePath());
        }

        // backup oldDirPath;
        String oldDirPath = downloadFileInfo.getFileDir();

        // move result
        boolean moveResult = false;

        // change record in db
        downloadFileInfo.setFileDir(mNewDirPath);
        moveResult = mFileDownloadCacher.updateDownloadFile(downloadFileInfo);
        // succeed in db
        if (moveResult) {
            // move in the file system
            moveResult = oldFile.renameTo(newFile);
            if (moveResult) {// succeed
                if (mOnMoveDownloadFileListener instanceof OnSyncMoveDownloadFileListener) {
                    // OnSyncMoveDownloadFileListener,that means the caller hopes to sync something
                    moveResult = ((OnSyncMoveDownloadFileListener) mOnMoveDownloadFileListener)
                            .onDoSyncMoveDownloadFile(downloadFileInfo);
                    if (moveResult) {// sync with the caller succeed
                        // move success
                        notifySuccess(downloadFileInfo);
                    } else {
                        // rollback db,sync with the caller
                        downloadFileInfo.setFileDir(oldDirPath);
                        mFileDownloadCacher.updateDownloadFile(downloadFileInfo);
                        // rollback file system,sync with the caller
                        newFile.renameTo(oldFile);
                    }
                } else {// succeed
                    // move success
                    notifySuccess(downloadFileInfo);
                }
            } else {// failed to move in the file system,rollback in db
                downloadFileInfo.setFileDir(oldDirPath);
                mFileDownloadCacher.updateDownloadFile(downloadFileInfo);
                notifyFailed(downloadFileInfo, new OnMoveDownloadFileFailReason("update record error!", 
                        OnMoveDownloadFileFailReason.TYPE_UPDATE_RECORD_ERROR));
            }
        } else {
            // move in db failed
            notifyFailed(downloadFileInfo, new OnMoveDownloadFileFailReason("update record error!", 
                    OnMoveDownloadFileFailReason.TYPE_UPDATE_RECORD_ERROR));
        }
    }

    private void notifyPrepared(DownloadFileInfo downloadFileInfo) {
        if (mOnMoveDownloadFileListener == null) {
            return;
        }
        if (isSyncCallback) {
            mOnMoveDownloadFileListener.onMoveDownloadFilePrepared(downloadFileInfo);
        } else {
            OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFilePrepared(downloadFileInfo, 
                    mOnMoveDownloadFileListener);
        }
    }

    private void notifySuccess(DownloadFileInfo downloadFileInfo) {
        if (mOnMoveDownloadFileListener == null) {
            return;
        }
        if (isSyncCallback) {
            mOnMoveDownloadFileListener.onMoveDownloadFileSuccess(downloadFileInfo);
        } else {
            OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileSuccess(downloadFileInfo, 
                    mOnMoveDownloadFileListener);
        }
    }

    private void notifyFailed(DownloadFileInfo downloadFileInfo, OnMoveDownloadFileFailReason failReason) {
        if (mOnMoveDownloadFileListener == null) {
            return;
        }
        if (isSyncCallback) {
            mOnMoveDownloadFileListener.onMoveDownloadFileFailed(downloadFileInfo, failReason);
        } else {
            OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo, failReason, 
                    mOnMoveDownloadFileListener);
        }
    }
}
