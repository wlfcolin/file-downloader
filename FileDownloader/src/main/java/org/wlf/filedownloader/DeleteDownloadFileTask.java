package org.wlf.filedownloader;

import android.util.Log;

import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener.OnDeleteDownloadFileFailReason;

import java.io.File;

/**
 * DeleteDownloadFile Task
 * <br/>
 * 删除下载文件任务
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
class DeleteDownloadFileTask implements Runnable {

    private static final String TAG = DeleteDownloadFileTask.class.getSimpleName();

    private String mUrl;
    private boolean mDeleteDownloadedFileInPath;
    private DownloadFileCacher mFileDownloadCacher;
    private boolean mIsSyncCallback = false;

    private OnDeleteDownloadFileListener mOnDeleteDownloadFileListener;

    DeleteDownloadFileTask(String url, boolean deleteDownloadedFileInPath, DownloadFileCacher fileDownloadCacher) {
        super();
        this.mUrl = url;
        this.mDeleteDownloadedFileInPath = deleteDownloadedFileInPath;
        this.mFileDownloadCacher = fileDownloadCacher;
    }

    void setOnDeleteDownloadFileListener(OnDeleteDownloadFileListener onDeleteDownloadFileListener) {
        this.mOnDeleteDownloadFileListener = onDeleteDownloadFileListener;
    }

    // package use only

    /**
     * enable the callback sync
     */
    void enableSyncCallback() {
        mIsSyncCallback = true;
    }

    @Override
    public void run() {

        DownloadFileInfo downloadFileInfo = mFileDownloadCacher.getDownloadFile(mUrl);

        // 1.prepared
        notifyPrepared(downloadFileInfo);

        OnDeleteDownloadFileFailReason failReason = null;

        if (downloadFileInfo != null) {
            // delete in database record
            boolean deleteResult = mFileDownloadCacher.deleteDownloadFile(downloadFileInfo);
            if (deleteResult) {

                Log.d(TAG, "DeleteDownloadFileTask.run 数据库删除成功url：" + mUrl);

                // delete in path
                if (deleteResult) {
                    if (mDeleteDownloadedFileInPath) {
                        File file = new File(downloadFileInfo.getFileDir(), downloadFileInfo.getFileName());
                        if (file != null) {
                            if (file.exists()) {
                                deleteResult = file.delete();
                            } else {
                                // has been deleted in file path or not complete,look up the temp file
                                file = new File(downloadFileInfo.getFileDir(), downloadFileInfo.getTempFileName());
                                if (file.exists()) {
                                    deleteResult = file.delete();
                                }
                            }
                        } else {
                            // look up the temp file
                            file = new File(downloadFileInfo.getFileDir(), downloadFileInfo.getTempFileName());
                            if (file.exists()) {
                                deleteResult = file.delete();
                            }
                        }
                    }
                    if (deleteResult) {
                        Log.d(TAG, "DeleteDownloadFileTask.run 件删除成功url：" + mUrl);
                        // 2.delete success
                        notifySuccess(downloadFileInfo);
                        return;
                    } else {
                        failReason = new OnDeleteDownloadFileFailReason("delete file in path failed!", 
                                OnDeleteDownloadFileFailReason.TYPE_UNKNOWN);
                    }
                } else {
                    failReason = new OnDeleteDownloadFileFailReason("delete file failed!", 
                            OnDeleteDownloadFileFailReason.TYPE_UNKNOWN);
                }
            } else {
                failReason = new OnDeleteDownloadFileFailReason("delete file in record failed!", 
                        OnDeleteDownloadFileFailReason.TYPE_UNKNOWN);
            }
        } else {
            failReason = new OnDeleteDownloadFileFailReason("file record is not exist!", 
                    OnDeleteDownloadFileFailReason.TYPE_FILE_RECORD_IS_NOT_EXIST);
        }

        if (failReason != null) {
            Log.d(TAG, "DeleteDownloadFileTask.run 删除失败url：" + mUrl + ",failReason:" + failReason.getType());
            // 2.delete failed
            notifyFailed(downloadFileInfo, failReason);
        }
    }

    private void notifyPrepared(DownloadFileInfo downloadFileInfo) {
        if (mOnDeleteDownloadFileListener == null) {
            return;
        }
        if (mIsSyncCallback) {
            mOnDeleteDownloadFileListener.onDeleteDownloadFilePrepared(downloadFileInfo);
        } else {
            OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFilePrepared(downloadFileInfo, 
                    mOnDeleteDownloadFileListener);
        }
    }

    private void notifySuccess(DownloadFileInfo downloadFileInfo) {
        if (mOnDeleteDownloadFileListener == null) {
            return;
        }
        if (mIsSyncCallback) {
            mOnDeleteDownloadFileListener.onDeleteDownloadFileSuccess(downloadFileInfo);
        } else {
            OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFileSuccess(downloadFileInfo, 
                    mOnDeleteDownloadFileListener);
        }
    }

    private void notifyFailed(DownloadFileInfo downloadFileInfo, OnDeleteDownloadFileFailReason failReason) {
        if (mOnDeleteDownloadFileListener == null) {
            return;
        }
        if (mIsSyncCallback) {
            mOnDeleteDownloadFileListener.onDeleteDownloadFileFailed(downloadFileInfo, failReason);
        } else {
            OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFileFailed(downloadFileInfo, failReason, 
                    mOnDeleteDownloadFileListener);
        }
    }

}
