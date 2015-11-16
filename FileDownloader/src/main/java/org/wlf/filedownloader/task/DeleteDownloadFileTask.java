package org.wlf.filedownloader.task;

import org.wlf.filedownloader.DownloadFileCacher;
import org.wlf.filedownloader.DownloadFileInfo;
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

    private String url;
    private boolean deleteDownloadedFileInPath;
    private DownloadFileCacher fileDownloadCacher;

    private OnDeleteDownloadFileListener mOnDeleteDownloadFileListener;

    public DeleteDownloadFileTask(String url, boolean deleteDownloadedFileInPath, DownloadFileCacher fileDownloadCacher) {
        super();
        this.url = url;
        this.deleteDownloadedFileInPath = deleteDownloadedFileInPath;
        this.fileDownloadCacher = fileDownloadCacher;
    }

    public void setOnDeleteDownloadFileListener(OnDeleteDownloadFileListener onDeleteDownloadFileListener) {
        this.mOnDeleteDownloadFileListener = onDeleteDownloadFileListener;
    }

    @Override
    public void run() {

        DownloadFileInfo downloadFileInfo = fileDownloadCacher.getDownloadFile(url);

        // 1.Prepared
        OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFilePrepared(downloadFileInfo, mOnDeleteDownloadFileListener);

        OnDeleteDownloadFileFailReason failReason = null;

        if (downloadFileInfo != null) {
            // delete in database record
            boolean deleteResult = fileDownloadCacher.deleteDownloadFile(downloadFileInfo);
            if (deleteResult) {
                // delete in path
                if (deleteDownloadedFileInPath) {
                    File file = new File(downloadFileInfo.getFileDir(), downloadFileInfo.getFileName());
                    if (file != null) {
                        if (file.exists()) {
                            deleteResult = file.delete();
                        } else {
                            // has been deleted in file path
                        }
                    }
                }
                if (deleteResult) {
                    // 2.delete Success
                    OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFileSuccess(downloadFileInfo, mOnDeleteDownloadFileListener);
                    return;
                } else {
                    failReason = new OnDeleteDownloadFileFailReason("delete file in path failed!", OnDeleteDownloadFileFailReason.TYPE_UNKNOWN);
                }
            } else {
                failReason = new OnDeleteDownloadFileFailReason("delete file in record failed!", OnDeleteDownloadFileFailReason.TYPE_UNKNOWN);
            }
        } else {
            failReason = new OnDeleteDownloadFileFailReason("file record is not exist!", OnDeleteDownloadFileFailReason.TYPE_FILE_RECORD_IS_NOT_EXIST);
        }

        if (failReason != null) {
            // 2.delete failed
            OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFileFailed(downloadFileInfo, failReason, mOnDeleteDownloadFileListener);
        }
    }

}
