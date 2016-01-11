package org.wlf.filedownloader.file_rename;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.file_download.OnStopFileDownloadTaskListener;
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

    /**
     * LOG TAG
     */
    private static final String TAG = DownloadRenameManager.class.getSimpleName();

    /**
     * task engine
     */
    private ExecutorService mTaskEngine;
    /**
     * DownloadFileRenamer,to rename download files
     */
    private DownloadFileRenamer mDownloadFileRenamer;
    /**
     * Pauseable,to pause download tasks
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
        // exec a task
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
     * rename download
     */
    private void renameInternal(String url, String newFileName, boolean includedSuffix, OnRenameDownloadFileListener 
            onRenameDownloadFileListener) {
        // create rename download task
        RenameDownloadFileTask task = new RenameDownloadFileTask(url, newFileName, includedSuffix, 
                mDownloadFileRenamer);
        task.setOnRenameDownloadFileListener(onRenameDownloadFileListener);
        // run task
        addAndRunTask(task);
    }

    /**
     * rename download
     *
     * @param url                          file url
     * @param newFileName                  new file name
     * @param includedSuffix               true means the newFileName include the suffix
     * @param onRenameDownloadFileListener RenameDownloadFileListener
     */
    public void rename(String url, final String newFileName, final boolean includedSuffix, final 
    OnRenameDownloadFileListener onRenameDownloadFileListener) {

        final String finalUrl = url;

        if (!mDownloadTaskPauseable.isDownloading(url)) {

            Log.d(TAG, "rename 直接重命名,url:" + url);

            renameInternal(url, newFileName, includedSuffix, onRenameDownloadFileListener);
        } else {

            Log.d(TAG, "rename 需要先暂停后重命名,url:" + url);

            // pause
            mDownloadTaskPauseable.pause(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {

                    Log.d(TAG, "rename 暂停成功，开始重命名,url:" + finalUrl);

                    renameInternal(finalUrl, newFileName, includedSuffix, onRenameDownloadFileListener);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason failReason) {

                    Log.d(TAG, "rename 暂停失败，无法重命名,url:" + finalUrl);

                    // error
                    notifyRenameDownloadFileFailed(getDownloadFile(finalUrl), new OnRenameDownloadFileFailReason
                            (failReason), onRenameDownloadFileListener);
                }
            });
        }
    }

    // -----------------------notify caller-----------------------

    private void notifyRenameDownloadFileFailed(DownloadFileInfo downloadFileInfo, RenameDownloadFileFailReason 
            failReason, OnRenameDownloadFileListener onRenameDownloadFileListener) {
        if (onRenameDownloadFileListener == null) {
            return;
        }

        // notify caller
        OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileFailed(downloadFileInfo, failReason, 
                onRenameDownloadFileListener);
    }
}
