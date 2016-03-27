package org.wlf.filedownloader.file_rename;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.file_download.base.OnStopFileDownloadTaskListener;
import org.wlf.filedownloader.file_download.base.Pauseable;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener.OnRenameDownloadFileFailReason;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener.RenameDownloadFileFailReason;

import java.util.concurrent.ExecutorService;

/**
 * DownloadRenameManager
 *
 * @author wlf(Andy)
 * @datetime 2015-12-10 11:31 GMT+8
 * @email 411086563@qq.com
 * @since 0.2.0
 */
public class DownloadRenameManager {

    private static final String TAG = DownloadRenameManager.class.getSimpleName();

    /**
     * task engine
     */
    private ExecutorService mTaskEngine;
    /**
     * DownloadFileRenamer, which to rename download files in record
     */
    private DownloadFileRenamer mDownloadFileRenamer;
    /**
     * Pauseable, which to pause download tasks
     */
    private Pauseable mDownloadTaskPauseable;

    public DownloadRenameManager(ExecutorService taskEngine, DownloadFileRenamer downloadFileRenamer, Pauseable 
            downloadTaskPauseable) {
        mTaskEngine = taskEngine;
        mDownloadFileRenamer = downloadFileRenamer;
        mDownloadTaskPauseable = downloadTaskPauseable;
    }

    /**
     * start a task
     */
    private void addAndRunTask(Runnable task) {
        // exec the task
        mTaskEngine.execute(task);
    }

    /**
     * get DownloadFile by file url
     *
     * @param url file url
     * @return DownloadFile
     */
    private DownloadFileInfo getDownloadFile(String url) {
        return mDownloadFileRenamer.getDownloadFile(url);
    }

    /**
     * rename a download
     */
    private void renameInternal(String url, String newFileName, boolean includedSuffix, OnRenameDownloadFileListener 
            onRenameDownloadFileListener) {
        // create a rename download task
        RenameDownloadFileTask task = new RenameDownloadFileTask(url, newFileName, includedSuffix, 
                mDownloadFileRenamer);
        task.setOnRenameDownloadFileListener(onRenameDownloadFileListener);
        // run the task
        addAndRunTask(task);
    }

    /**
     * rename a download file
     *
     * @param url                          file url
     * @param newFileName                  new file name
     * @param includedSuffix               true means the newFileName has been included the suffix, otherwise the
     *                                     newFileName not include the suffix
     * @param onRenameDownloadFileListener OnRenameDownloadFileListener impl
     */
    public void rename(String url, final String newFileName, final boolean includedSuffix, final 
    OnRenameDownloadFileListener onRenameDownloadFileListener) {

        final String finalUrl = url;

        if (!mDownloadTaskPauseable.isDownloading(url)) {

            Log.d(TAG, TAG + ".rename 下载任务已经暂停，可以直接重命名，url:" + url);

            renameInternal(url, newFileName, includedSuffix, onRenameDownloadFileListener);
        } else {

            Log.d(TAG, TAG + ".rename 需要先暂停下载任务后重命名,url:" + url);

            // pause
            mDownloadTaskPauseable.pause(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {

                    Log.d(TAG, TAG + ".rename 暂停下载任务成功，开始重命名，url:" + finalUrl);

                    renameInternal(finalUrl, newFileName, includedSuffix, onRenameDownloadFileListener);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason failReason) {

                    if (failReason != null) {
                        if (StopDownloadFileTaskFailReason.TYPE_TASK_HAS_BEEN_STOPPED.equals(failReason.getType())) {
                            // has been stopped, so can restart normally
                            renameInternal(finalUrl, newFileName, includedSuffix, onRenameDownloadFileListener);
                            return;
                        }
                    }

                    Log.d(TAG, TAG + ".rename 暂停下载任务失败，无法重命名，url:" + finalUrl);

                    // otherwise error occur, notify caller
                    notifyRenameDownloadFileFailed(getDownloadFile(finalUrl), new OnRenameDownloadFileFailReason
                            (finalUrl, failReason), onRenameDownloadFileListener);
                }
            });
        }
    }

    // --------------------------------------notify caller--------------------------------------

    /**
     * notifyRenameDownloadFileFailed
     */
    private void notifyRenameDownloadFileFailed(DownloadFileInfo downloadFileInfo, RenameDownloadFileFailReason 
            failReason, OnRenameDownloadFileListener onRenameDownloadFileListener) {
        // main thread notify caller
        OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileFailed(downloadFileInfo, failReason, 
                onRenameDownloadFileListener);
    }
}
