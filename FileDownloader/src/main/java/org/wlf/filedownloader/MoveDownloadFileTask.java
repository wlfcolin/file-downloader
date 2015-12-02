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

    @Override
    public void run() {

        DownloadFileInfo downloadFileInfo = mFileDownloadCacher.getDownloadFile(mUrl);

        if (downloadFileInfo == null) {
            OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo, new OnMoveDownloadFileFailReason("the DownloadFile is empty!", OnMoveDownloadFileFailReason.TYPE_NULL_POINTER), mOnMoveDownloadFileListener);
            return;
        }

        OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFilePrepared(downloadFileInfo, mOnMoveDownloadFileListener);

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
                OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo, new OnMoveDownloadFileFailReason("DownloadFile status error!", OnMoveDownloadFileFailReason.TYPE_FILE_STATUS_ERROR), mOnMoveDownloadFileListener);
                return;
        }

        if (oldFile == null || !oldFile.exists()) {
            OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo, new OnMoveDownloadFileFailReason("the original fie does not exist!", OnMoveDownloadFileFailReason.TYPE_ORIGINAL_FILE_NOT_EXIST), mOnMoveDownloadFileListener);
            return;
        }

        // create ParentFile of the newFile
        if (newFile != null && !newFile.getParentFile().exists()) {
            FileUtil.createFileParentDir(newFile.getAbsolutePath());
        }

        if (newFile != null && newFile.exists()) {
            OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo, new OnMoveDownloadFileFailReason("the target fie exist!", OnMoveDownloadFileFailReason.TYPE_TARGET_FILE_EXIST), mOnMoveDownloadFileListener);
            return;
        }

        boolean moveResult = false;

        String oldDirPath = downloadFileInfo.getFileDir();

        // change record in database
        downloadFileInfo.setFileDir(mNewDirPath);
        moveResult = mFileDownloadCacher.updateDownloadFile(downloadFileInfo);
        if (moveResult) {
            // record changed
            if (mOnMoveDownloadFileListener instanceof OnSyncMoveDownloadFileListener) {//OnSyncMoveDownloadFileListener,that means the caller hopes to sync something
                moveResult = ((OnSyncMoveDownloadFileListener) mOnMoveDownloadFileListener).onDoSyncMoveDownloadFile(downloadFileInfo);
            }
            if (!moveResult) {// rollback,sync with the caller FIXME,use transaction to control the update is better
                downloadFileInfo.setFileDir(oldDirPath);
                mFileDownloadCacher.updateDownloadFile(downloadFileInfo);
            }
        }

        if (moveResult) {// move in the file system
            moveResult = oldFile.renameTo(newFile);
            if (moveResult) {
                // move success
                OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileSuccess(downloadFileInfo, mOnMoveDownloadFileListener);
            } else {
                // move in the file system failed,rollback in database FIXME,use transaction to control the update is better
                downloadFileInfo.setFileDir(oldDirPath);
                mFileDownloadCacher.updateDownloadFile(downloadFileInfo);
                OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo, new OnMoveDownloadFileFailReason("update record error!", OnMoveDownloadFileFailReason.TYPE_UPDATE_RECORD_ERROR), mOnMoveDownloadFileListener);
            }
        } else {
            // move failed
            OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo, null, mOnMoveDownloadFileListener);
        }

    }
}
