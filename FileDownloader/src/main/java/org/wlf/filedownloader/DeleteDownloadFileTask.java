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
public class DeleteDownloadFileTask implements Runnable {

    private static final String TAG = DeleteDownloadFileTask.class.getSimpleName();

    private String mUrl;
    private boolean mDeleteDownloadedFileInPath;
    private DownloadFileCacher mFileDownloadCacher;

    private OnDeleteDownloadFileListener mOnDeleteDownloadFileListener;

    public DeleteDownloadFileTask(String url, boolean deleteDownloadedFileInPath, DownloadFileCacher 
            fileDownloadCacher) {
        super();
        this.mUrl = url;
        this.mDeleteDownloadedFileInPath = deleteDownloadedFileInPath;
        this.mFileDownloadCacher = fileDownloadCacher;
    }

    public void setOnDeleteDownloadFileListener(OnDeleteDownloadFileListener onDeleteDownloadFileListener) {
        this.mOnDeleteDownloadFileListener = onDeleteDownloadFileListener;
    }

    @Override
    public void run() {

        DownloadFileInfo downloadFileInfo = mFileDownloadCacher.getDownloadFile(mUrl);

        // 1.prepared
        OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFilePrepared(downloadFileInfo, 
                mOnDeleteDownloadFileListener);

        OnDeleteDownloadFileFailReason failReason = null;

        if (downloadFileInfo != null) {
            // delete in database record
            boolean deleteResult = mFileDownloadCacher.deleteDownloadFile(downloadFileInfo);
            if (deleteResult) {
                Log.d(TAG, "DeleteDownloadFileTask.run 数据库删除成功url：" + mUrl);
                // delete in path
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
                    Log.d(TAG, "DeleteDownloadFileTask.run 数据库+文件删除成功url：" + mUrl);
                    // 2.delete success
                    OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFileSuccess(downloadFileInfo, 
                            mOnDeleteDownloadFileListener);
                    return;
                } else {
                    failReason = new OnDeleteDownloadFileFailReason("delete file in path failed!", 
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
            OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFileFailed(downloadFileInfo, failReason, 
                    mOnDeleteDownloadFileListener);
        }
    }

}
