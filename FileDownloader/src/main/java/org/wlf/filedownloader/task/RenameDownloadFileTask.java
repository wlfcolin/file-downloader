package org.wlf.filedownloader.task;

import android.text.TextUtils;

import org.wlf.filedownloader.DownloadFileCacher;
import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener.OnRenameDownloadFileFailReason;

import java.io.File;

/**
 * RenameDownloadFileTask
 * <br/>
 * 重命名文件任务
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class RenameDownloadFileTask implements Runnable {

    private static final String TAG = RenameDownloadFileTask.class.getSimpleName();

    private String url;
    private String newFileName;
    private DownloadFileCacher fileDownloadCacher;

    private OnRenameDownloadFileListener onRenameDownloadFileListener;

    public RenameDownloadFileTask(String url, String newFileName, DownloadFileCacher fileDownloadCacher) {
        super();
        this.url = url;
        this.newFileName = newFileName;
        this.fileDownloadCacher = fileDownloadCacher;
    }

    public void setOnRenameDownloadFileListener(OnRenameDownloadFileListener onRenameDownloadFileListener) {
        this.onRenameDownloadFileListener = onRenameDownloadFileListener;
    }

    @Override
    public void run() {

        DownloadFileInfo downloadFileInfo = fileDownloadCacher.getDownloadFile(url);

        // 1.Prepared
        OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFilePrepared(downloadFileInfo, onRenameDownloadFileListener);

        if (downloadFileInfo != null) {

            if (downloadFileInfo.getStatus() != Status.DOWNLOAD_STATUS_COMPLETED) {
                // not complete download
                OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileFailed(downloadFileInfo, new OnRenameDownloadFileFailReason("the file does not complete download!", OnRenameDownloadFileFailReason.TYPE_FILE_DOES_NOT_COMPLETE_DOWNLOAD), onRenameDownloadFileListener);
                return;
            }

            String dirPath = downloadFileInfo.getFileDir();
            String oldFileName = downloadFileInfo.getFileName();

            try {
                File file = new File(dirPath, oldFileName);
                if (!file.exists()) {
                    throw new OnRenameDownloadFileFailReason("the original file not exist!", OnRenameDownloadFileFailReason.TYPE_ORIGINAL_FILE_NOT_EXIST);
                }

                if (TextUtils.isEmpty(newFileName)) {
                    throw new OnRenameDownloadFileFailReason("new file name is empty!", OnRenameDownloadFileFailReason.TYPE_NEW_FILE_NAME_IS_EMPTY);
                }

                File newFile = new File(dirPath, newFileName);
                boolean isSucceed = file.renameTo(newFile);

                if (!isSucceed) {
                    throw new OnRenameDownloadFileFailReason("rename file failed!", OnRenameDownloadFileFailReason.TYPE_UNKNOWN);
                }

                // success,write to db
                // downloadFileInfo.// TODO
                boolean updateResult = fileDownloadCacher.updateDownloadFile(downloadFileInfo);
                if (updateResult) {
                    // 2.rename success
                    OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileSuccess(downloadFileInfo, onRenameDownloadFileListener);
                    return;
                } else {
                    throw new OnRenameDownloadFileFailReason("rename file failed!", OnRenameDownloadFileFailReason.TYPE_UNKNOWN);
                }
            } catch (Exception e) {
                e.printStackTrace();

                OnRenameDownloadFileFailReason failReason = null;

                if (e instanceof OnRenameDownloadFileFailReason) {
                    failReason = (OnRenameDownloadFileFailReason) e;
                } else {
                    failReason = new OnRenameDownloadFileFailReason(e);
                }

                // failed
                OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileFailed(downloadFileInfo, failReason, onRenameDownloadFileListener);
            }
        } else {
            // failed
            OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileFailed(downloadFileInfo, new OnRenameDownloadFileFailReason("the download file is not exist!", OnRenameDownloadFileFailReason.TYPE_FILE_RECORD_IS_NOT_EXIST), onRenameDownloadFileListener);
        }
    }

}
