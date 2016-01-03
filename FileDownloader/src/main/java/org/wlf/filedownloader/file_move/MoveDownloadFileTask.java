package org.wlf.filedownloader.file_move;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.file_download.db_recorder.DownloadFileDbRecorder;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener.MoveDownloadFileFailReason;
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
    private DownloadFileMover mDownloadFileMover;
    private boolean mIsSyncCallback = false;

    private OnMoveDownloadFileListener mOnMoveDownloadFileListener;

    public MoveDownloadFileTask(String url, String newDirPath, DownloadFileMover downloadFileMover) {
        super();
        this.mUrl = url;
        this.mNewDirPath = newDirPath;
        this.mDownloadFileMover = downloadFileMover;
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
    public void enableSyncCallback() {
        mIsSyncCallback = true;
    }

    @Override
    public void run() {

        DownloadFileInfo downloadFileInfo = mDownloadFileMover.getDownloadFile(mUrl);

        // check null
        if (downloadFileInfo == null) {
            notifyFailed(downloadFileInfo, new MoveDownloadFileFailReason("the DownloadFile is empty!", 
                    MoveDownloadFileFailReason.TYPE_NULL_POINTER));
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
                notifyFailed(downloadFileInfo, new MoveDownloadFileFailReason("DownloadFile status error!", 
                        MoveDownloadFileFailReason.TYPE_FILE_STATUS_ERROR));
                return;
        }

        // check original file
        if (oldFile == null || !oldFile.exists()) {
            notifyFailed(downloadFileInfo, new MoveDownloadFileFailReason("the original file does not exist!", 
                    MoveDownloadFileFailReason.TYPE_ORIGINAL_FILE_NOT_EXIST));
            return;
        }

        // check new file
        if (newFile != null && newFile.exists()) {
            notifyFailed(downloadFileInfo, new MoveDownloadFileFailReason("the target file exist!", 
                    MoveDownloadFileFailReason.TYPE_TARGET_FILE_EXIST));
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
        //       downloadFileInfo.setFileDir(mNewDirPath);
        //     moveResult = mFileDownloadCacher.updateDownloadFile(downloadFileInfo);
        try {
            mDownloadFileMover.moveDownloadFile(downloadFileInfo.getUrl(), mNewDirPath);
            moveResult = true;
        } catch (Exception e) {
            e.printStackTrace();
            moveResult = false;
        }
        //moveResult = mDownloadFileMover.moveDownloadFile(downloadFileInfo, mNewDirPath);
        // succeed in db
        if (moveResult) {
            // move in the file system
            moveResult = oldFile.renameTo(newFile);
            if (moveResult) {// succeed
                // move success
                notifySuccess(downloadFileInfo);
            } else {// failed to move in the file system,rollback in db
                //               downloadFileInfo.setFileDir(oldDirPath);
                //             mFileDownloadCacher.updateDownloadFile(downloadFileInfo);
                // mDownloadFileMover.moveDownloadFile(downloadFileInfo, oldDirPath);
                try {
                    mDownloadFileMover.moveDownloadFile(downloadFileInfo.getUrl(), oldDirPath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                notifyFailed(downloadFileInfo, new MoveDownloadFileFailReason("update record error!", 
                        MoveDownloadFileFailReason.TYPE_UPDATE_RECORD_ERROR));
            }
        } else {
            // move in db failed
            notifyFailed(downloadFileInfo, new MoveDownloadFileFailReason("update record error!", 
                    MoveDownloadFileFailReason.TYPE_UPDATE_RECORD_ERROR));
        }
    }

    private void notifyPrepared(DownloadFileInfo downloadFileInfo) {
        if (mOnMoveDownloadFileListener == null) {
            return;
        }
        if (mIsSyncCallback) {
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
        if (mIsSyncCallback) {
            mOnMoveDownloadFileListener.onMoveDownloadFileSuccess(downloadFileInfo);
        } else {
            OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileSuccess(downloadFileInfo, 
                    mOnMoveDownloadFileListener);
        }
    }

    private void notifyFailed(DownloadFileInfo downloadFileInfo, MoveDownloadFileFailReason failReason) {
        if (mOnMoveDownloadFileListener == null) {
            return;
        }
        if (mIsSyncCallback) {
            mOnMoveDownloadFileListener.onMoveDownloadFileFailed(downloadFileInfo, failReason);
        } else {
            OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo, failReason, 
                    mOnMoveDownloadFileListener);
        }
    }

    /**
     * DownloadFile Mover
     */
    public interface DownloadFileMover extends DownloadFileDbRecorder {

        /**
         * move download file name
         *
         * @param url        download url
         * @param newDirPath new file name
         * @throws Exception any exception during move
         */
        void moveDownloadFile(String url, String newDirPath) throws Exception;
    }

}
