package org.wlf.filedownloader.file_delete;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener.DeleteDownloadFileFailReason;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener.OnDeleteDownloadFileFailReason;
import org.wlf.filedownloader.util.DownloadFileUtil;

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
    private DownloadFileDeleter mDownloadFileDeleter;
    private boolean mIsSyncCallback = false;

    private OnDeleteDownloadFileListener mOnDeleteDownloadFileListener;

    public DeleteDownloadFileTask(String url, boolean deleteDownloadedFileInPath, DownloadFileDeleter 
            downloadFileDeleter) {
        super();
        this.mUrl = url;
        this.mDeleteDownloadedFileInPath = deleteDownloadedFileInPath;
        this.mDownloadFileDeleter = downloadFileDeleter;
    }

    /**
     * set OnDeleteDownloadFileListener
     *
     * @param onDeleteDownloadFileListener OnDeleteDownloadFileListener
     */
    public void setOnDeleteDownloadFileListener(OnDeleteDownloadFileListener onDeleteDownloadFileListener) {
        this.mOnDeleteDownloadFileListener = onDeleteDownloadFileListener;
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
        DeleteDownloadFileFailReason failReason = null;

        try {
            downloadFileInfo = mDownloadFileDeleter.getDownloadFile(mUrl);

            // ------------start checking conditions------------
            {
                if (downloadFileInfo == null) {
                    failReason = new OnDeleteDownloadFileFailReason("the download file not exist !", 
                            OnDeleteDownloadFileFailReason.TYPE_FILE_RECORD_IS_NOT_EXIST);

                    // goto finally, notifyFailed()
                    return;
                }

                // check status
                if (!DownloadFileUtil.canDelete(downloadFileInfo)) {
                    failReason = new OnDeleteDownloadFileFailReason("the download file status error !", 
                            OnDeleteDownloadFileFailReason.TYPE_FILE_STATUS_ERROR);
                    // goto finally, notifyFailed()
                    return;
                }
            }
            // ------------end checking conditions------------

            // 1.prepared
            notifyPrepared(downloadFileInfo);

            // delete in database record
            boolean deleteResult = false;
            try {
                mDownloadFileDeleter.deleteDownloadFile(mUrl);
                deleteResult = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!deleteResult) {
                failReason = new OnDeleteDownloadFileFailReason("delete file in record failed !", OnDeleteDownloadFileFailReason.TYPE_UNKNOWN);
                // goto finally, notifyFailed()
                return;
            }

            Log.d(TAG, TAG + ".run 数据库删除成功url：" + mUrl);

            // delete in path
            if (mDeleteDownloadedFileInPath) {
                File file = new File(downloadFileInfo.getFileDir(), downloadFileInfo.getFileName());
                if (file != null && file.exists()) {
                    deleteResult = file.delete();
                }
                // has been deleted in file path or not complete, look up the temp file
                else {
                    file = new File(downloadFileInfo.getFileDir(), downloadFileInfo.getTempFileName());
                    if (file.exists()) {
                        deleteResult = file.delete();
                    }
                }
            }

            if (!deleteResult) {
                failReason = new OnDeleteDownloadFileFailReason("delete file in path failed !", 
                        OnDeleteDownloadFileFailReason.TYPE_UNKNOWN);
                // goto finally, notifyFailed()
                return;
            }

            Log.d(TAG, TAG + ".run 文件删除成功url：" + mUrl);
        } catch (Exception e) {
            e.printStackTrace();
            failReason = new OnDeleteDownloadFileFailReason(e);
        } finally {
            // ------------start notifying caller------------
            {
                // delete succeed
                if (failReason == null) {
                    // 2.delete success
                    notifySuccess(downloadFileInfo);

                    Log.d(TAG, TAG + ".run 删除成功，url：" + mUrl);
                } else {
                    // 2.delete failed
                    notifyFailed(downloadFileInfo, failReason);

                    Log.d(TAG, TAG + ".run 删除失败，url：" + mUrl + "，failReason:" + failReason.getType());
                }
            }
            // ------------end notifying caller------------

            Log.d(TAG, TAG + ".run 文件删除任务【已结束】，是否有异常：" + (failReason == null) + "，url：" + mUrl);
        }
    }

    // --------------------------------------notify caller--------------------------------------

    /**
     * notifyPrepared
     */
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

    /**
     * notifySuccess
     */
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

    /**
     * notifyFailed
     */
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
}
