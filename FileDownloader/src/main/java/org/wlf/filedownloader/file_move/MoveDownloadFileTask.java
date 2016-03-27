package org.wlf.filedownloader.file_move;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener.MoveDownloadFileFailReason;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener.OnMoveDownloadFileFailReason;
import org.wlf.filedownloader.util.DownloadFileUtil;
import org.wlf.filedownloader.util.FileUtil;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * move download file
 * <br/>
 * 移动下载文件任务
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
class MoveDownloadFileTask implements Runnable {

    private static final String TAG = MoveDownloadFileTask.class.getSimpleName();

    private String mUrl;
    private String mNewDirPath;
    private DownloadFileMover mDownloadFileMover;
    private boolean mIsSyncCallback = false;
    private AtomicBoolean mIsNotifyFinish = new AtomicBoolean(false);

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

    /**
     * enable the callback sync
     */
    public void enableSyncCallback() {
        mIsSyncCallback = true;
    }

    // --------------------------------------run the task--------------------------------------

    @Override
    public void run() {

        DownloadFileInfo downloadFileInfo = null;
        MoveDownloadFileFailReason failReason = null;

        try {

            downloadFileInfo = mDownloadFileMover.getDownloadFile(mUrl);

            // ------------start checking conditions------------
            // check null
            if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
                failReason = new OnMoveDownloadFileFailReason(mUrl, "the DownloadFile is empty !",
                        OnMoveDownloadFileFailReason.TYPE_NULL_POINTER);
                // goto finally, notifyFailed()
                return;
            }

            // 1.prepared
            notifyPrepared(downloadFileInfo);

            // check status
            if (!DownloadFileUtil.canMove(downloadFileInfo)) {
                failReason = new OnMoveDownloadFileFailReason(mUrl, "the download file status error !",
                        OnMoveDownloadFileFailReason.TYPE_FILE_STATUS_ERROR);
                // goto finally, notifyFailed()
                return;
            }

            // check status
            File oldFile = null;
            File newFile = null;

            if (DownloadFileUtil.isCompleted(downloadFileInfo)) {
                oldFile = new File(downloadFileInfo.getFileDir(), downloadFileInfo.getFileName());
                newFile = new File(mNewDirPath, downloadFileInfo.getFileName());
            } else {
                oldFile = new File(downloadFileInfo.getFileDir(), downloadFileInfo.getTempFileName());
                newFile = new File(mNewDirPath, downloadFileInfo.getTempFileName());
            }

            // check original file
            if (oldFile == null || !oldFile.exists()) {
                failReason = new OnMoveDownloadFileFailReason(mUrl, "the original file does not exist !",
                        OnMoveDownloadFileFailReason.TYPE_ORIGINAL_FILE_NOT_EXIST);
                // goto finally, notifyFailed()
                return;
            }

            // check new file
            if (newFile != null && newFile.exists()) {
                failReason = new OnMoveDownloadFileFailReason(mUrl, "the target file exist !",
                        OnMoveDownloadFileFailReason.TYPE_TARGET_FILE_EXIST);
                // goto finally, notifyFailed()
                return;
            }

            // create ParentFile of the newFile if it is not exists
            if (newFile != null && newFile.getParentFile() != null && !newFile.getParentFile().exists()) {
                FileUtil.createFileParentDir(newFile.getAbsolutePath());
            }
            // ------------end checking conditions------------

            // backup oldDirPath
            String oldDirPath = downloadFileInfo.getFileDir();

            // move result
            boolean moveResult = false;

            try {
                mDownloadFileMover.moveDownloadFile(downloadFileInfo.getUrl(), mNewDirPath);
                moveResult = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!moveResult) {
                // move in db failed
                failReason = new OnMoveDownloadFileFailReason(mUrl, "update record error !",
                        OnMoveDownloadFileFailReason.TYPE_UPDATE_RECORD_ERROR);
                // goto finally, notifyFailed()
                return;
            }

            // move file in the file system
            moveResult = oldFile.renameTo(newFile);

            if (!moveResult) {
                // rollback in db
                try {
                    mDownloadFileMover.moveDownloadFile(downloadFileInfo.getUrl(), oldDirPath);
                } catch (Exception e) {
                    e.printStackTrace();
                    // try again
                    try {
                        mDownloadFileMover.moveDownloadFile(downloadFileInfo.getUrl(), oldDirPath);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        // ignore   
                    }
                }
                failReason = new OnMoveDownloadFileFailReason(mUrl, "update record error !",
                        OnMoveDownloadFileFailReason.TYPE_UPDATE_RECORD_ERROR);
                // goto finally, notifyFailed()
                return;
            }

            // move success
        } catch (Exception e) {
            e.printStackTrace();
            failReason = new OnMoveDownloadFileFailReason(mUrl, e);
        } finally {
            // ------------start notifying caller------------
            {
                // move succeed
                if (failReason == null) {
                    // 2.move success
                    notifySuccess(downloadFileInfo);

                    Log.d(TAG, TAG + ".run 移动成功，url：" + mUrl);
                } else {
                    // 2.move failed
                    notifyFailed(downloadFileInfo, failReason);

                    Log.d(TAG, TAG + ".run 移动失败，url：" + mUrl + ",failReason:" + failReason.getType());
                }
            }
            // ------------end notifying caller------------

            Log.d(TAG, TAG + ".run 文件移动任务【已结束】，是否有异常：" + (failReason == null) + "，url：" + mUrl);
        }
    }

    // --------------------------------------notify caller--------------------------------------

    /**
     * notifyPrepared
     */
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

    /**
     * notifySuccess
     */
    private void notifySuccess(DownloadFileInfo downloadFileInfo) {
        if (mIsNotifyFinish.get()) {
            return;
        }
        if (mIsNotifyFinish.compareAndSet(false, true)) {
            if (mOnMoveDownloadFileListener != null) {
                if (mIsSyncCallback) {
                    mOnMoveDownloadFileListener.onMoveDownloadFileSuccess(downloadFileInfo);
                } else {
                    OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileSuccess(downloadFileInfo,
                            mOnMoveDownloadFileListener);
                }
            }
        }
    }

    /**
     * notifyFailed
     */
    private void notifyFailed(DownloadFileInfo downloadFileInfo, MoveDownloadFileFailReason failReason) {
        if (mIsNotifyFinish.get()) {
            return;
        }
        if (mIsNotifyFinish.compareAndSet(false, true)) {
            if (mOnMoveDownloadFileListener != null) {
                if (mIsSyncCallback) {
                    mOnMoveDownloadFileListener.onMoveDownloadFileFailed(downloadFileInfo, failReason);
                } else {
                    OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo,
                            failReason, mOnMoveDownloadFileListener);
                }
            }
        }
    }
}
