package org.wlf.filedownloader.task;

import org.wlf.filedownloader.DownloadFileCacher;
import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener.OnMoveDownloadFileFailReason;

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

    private String url;
    private String newDirPath;
    private DownloadFileCacher fileDownloadCacher;

    private OnMoveDownloadFileListener mOnMoveDownloadFileListener;

    public MoveDownloadFileTask(String url, String newDirPath, DownloadFileCacher fileDownloadCacher) {
        super();
        this.url = url;
        this.newDirPath = newDirPath;
        this.fileDownloadCacher = fileDownloadCacher;
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

        DownloadFileInfo downloadFileInfo = fileDownloadCacher.getDownloadFile(url);

        OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFilePrepared(downloadFileInfo, mOnMoveDownloadFileListener);

        File oldFile = new File(downloadFileInfo.getFileDir(), downloadFileInfo.getFileName());
        File newFile = new File(newDirPath, downloadFileInfo.getFileName());

        if (oldFile == null || !oldFile.exists()) {
            OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo, new OnMoveDownloadFileFailReason("the original fie does not exist!", OnMoveDownloadFileFailReason.TYPE_ORIGINAL_FILE_NOT_EXIST), mOnMoveDownloadFileListener);
            return;
        }

        if (newFile != null && newFile.exists()) {
            OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo, new OnMoveDownloadFileFailReason("the target fie exist!", OnMoveDownloadFileFailReason.TYPE_TARGET_FILE_EXIST), mOnMoveDownloadFileListener);
            return;
        }

        boolean moveResult = false;

        if (oldFile != null && newFile != null) {
            moveResult = oldFile.renameTo(newFile);
        }

        if (moveResult) {
            // TODO record in database
            // move success
            OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileSuccess(downloadFileInfo, mOnMoveDownloadFileListener);
        } else {
            // move failed
            OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo, null, mOnMoveDownloadFileListener);
        }

    }

}
