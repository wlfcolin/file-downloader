package org.wlf.filedownloader;

import android.util.Log;

import org.wlf.filedownloader.FileDownloadTask.OnStopDownloadFileTaskFailReason;
import org.wlf.filedownloader.FileDownloadTask.OnStopFileDownloadTaskListener;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener.OnRenameDownloadFileFailReason;

import java.util.concurrent.ExecutorService;

/**
 * DownloadRenameManager
 *
 * @author wlf(Andy)
 * @datetime 2015-12-10 11:31 GMT+8
 * @email 411086563@qq.com
 * @since 0.2.0
 */
class DownloadRenameManager {

    /**
     * LOG TAG
     */
    private static final String TAG = DownloadRenameManager.class.getSimpleName();

    /**
     * task engine
     */
    private ExecutorService mSupportEngine;
    /**
     * DownloadFileCacher,to storage download files
     */
    private DownloadFileCacher mDownloadFileCacher;
    /**
     * DownloadTaskManager,to manage download tasks
     */
    private DownloadTaskManager mDownloadTaskManager;

    DownloadRenameManager(ExecutorService supportEngine, DownloadFileCacher downloadFileCacher, DownloadTaskManager 
            downloadTaskManager) {
        mSupportEngine = supportEngine;
        mDownloadFileCacher = downloadFileCacher;
        mDownloadTaskManager = downloadTaskManager;
    }

    /**
     * start a support task
     */
    private void addAndRunTask(Runnable task) {
        // exec a support task
        mSupportEngine.execute(task);
    }

    /**
     * get DownloadFile by file url
     *
     * @param url file url
     * @return DownloadFile
     */
    private DownloadFileInfo getDownloadFile(String url) {
        return mDownloadFileCacher.getDownloadFile(url);
    }

    private void renameInternal(String url, String newFileName, boolean includedSuffix, OnRenameDownloadFileListener 
            onRenameDownloadFileListener) {
        // create rename download task
        RenameDownloadFileTask task = new RenameDownloadFileTask(url, newFileName, includedSuffix, mDownloadFileCacher);
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
    void rename(String url, final String newFileName, final boolean includedSuffix, final 
    OnRenameDownloadFileListener onRenameDownloadFileListener) {

        final DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo == null) {

            Log.d(TAG, "rename 文件不存在,url:" + url);

            // the DownloadFile does not exist
            if (onRenameDownloadFileListener != null) {
                onRenameDownloadFileListener.onRenameDownloadFileFailed(downloadFileInfo, new 
                        OnRenameDownloadFileFailReason("the download file is not exist!", 
                        OnRenameDownloadFileFailReason.TYPE_FILE_RECORD_IS_NOT_EXIST));
            }
            return;
        }

        if (!mDownloadTaskManager.isInFileDownloadTaskMap(url)) {

            Log.d(TAG, "rename 直接重命名,url:" + url);

            renameInternal(url, newFileName, includedSuffix, onRenameDownloadFileListener);
        } else {

            Log.d(TAG, "rename 需要先暂停后重命名,url:" + url);

            // pause
            mDownloadTaskManager.pause(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {

                    Log.d(TAG, "rename 暂停成功，开始重命名,url:" + url);

                    renameInternal(url, newFileName, includedSuffix, onRenameDownloadFileListener);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {

                    Log.d(TAG, "rename 暂停失败，无法重命名,url:" + url);

                    // error
                    if (onRenameDownloadFileListener != null) {
                        onRenameDownloadFileListener.onRenameDownloadFileFailed(downloadFileInfo, new 
                                OnRenameDownloadFileFailReason(failReason));
                    }
                }
            });
        }
    }

}
