package org.wlf.filedownloader.file_delete;

import android.util.Log;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.file_download.db_recorder.DownloadFileDbRecorder;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener.DeleteDownloadFileFailReason;

import java.io.File;

/**
 * DeleteDownloadFile Task
 * <br/>
 * 删除下载文件任务
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DeleteDownloadFileTask implements Runnable {

    private static final String TAG = DeleteDownloadFileTask.class.getSimpleName();

    private String mUrl;
    private boolean mDeleteDownloadedFileInPath;
    private DownloadFileDeleter mDownloadFileDeleter;
    private boolean mIsSyncCallback = false;

    private OnDeleteDownloadFileListener mOnDeleteDownloadFileListener;

    DeleteDownloadFileTask(String url, boolean deleteDownloadedFileInPath, DownloadFileDeleter downloadFileDeleter) {
        super();
        this.mUrl = url;
        this.mDeleteDownloadedFileInPath = deleteDownloadedFileInPath;
        this.mDownloadFileDeleter = downloadFileDeleter;
    }

    public void setOnDeleteDownloadFileListener(OnDeleteDownloadFileListener onDeleteDownloadFileListener) {
        this.mOnDeleteDownloadFileListener = onDeleteDownloadFileListener;
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

        DownloadFileInfo downloadFileInfo = mDownloadFileDeleter.getDownloadFile(mUrl);

        // 1.prepared
        notifyPrepared(downloadFileInfo);

        DeleteDownloadFileFailReason failReason = null;

        if (downloadFileInfo != null) {
            // delete in database record
            boolean deleteResult = false;
            try {
                mDownloadFileDeleter.deleteDownloadFile(downloadFileInfo.getUrl());
                deleteResult = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            // boolean deleteResult = mDownloadFileDeleter.deleteDownloadFile(mUrl);
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
                        failReason = new DeleteDownloadFileFailReason("delete file in path failed!", 
                                DeleteDownloadFileFailReason.TYPE_UNKNOWN);
                    }
                } else {
                    failReason = new DeleteDownloadFileFailReason("delete file failed!", DeleteDownloadFileFailReason
                            .TYPE_UNKNOWN);
                }
            } else {
                failReason = new DeleteDownloadFileFailReason("delete file in record failed!", 
                        DeleteDownloadFileFailReason.TYPE_UNKNOWN);
            }
        } else {
            failReason = new DeleteDownloadFileFailReason("file record is not exist!", DeleteDownloadFileFailReason
                    .TYPE_FILE_RECORD_IS_NOT_EXIST);
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

    private void notifyFailed(DownloadFileInfo downloadFileInfo, DeleteDownloadFileFailReason failReason) {
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

    public interface DownloadFileDeleter extends DownloadFileDbRecorder {

        /**
         * delete download file
         *
         * @param url download url
         * @throws Exception any exception during delete
         */
        void deleteDownloadFile(String url) throws Exception;
    }

}
