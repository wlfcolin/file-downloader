package org.wlf.filedownloader.file_rename;

import android.util.Log;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.file_download.DownloadTaskManager;
import org.wlf.filedownloader.file_download.FileDownloadTask.OnStopDownloadFileTaskFailReason;
import org.wlf.filedownloader.file_download.FileDownloadTask.OnStopFileDownloadTaskListener;
import org.wlf.filedownloader.file_rename.RenameDownloadFileTask.DownloadFileRenamer;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener;
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
    private ExecutorService mSupportEngine;
    /**
     * DownloadFileRenamer,to rename download files
     */
    private DownloadFileRenamer mDownloadFileRenamer;
    /**
     * DownloadTaskManager,to manage download tasks
     */
    private DownloadTaskManager mDownloadTaskManager;

    public DownloadRenameManager(ExecutorService supportEngine, DownloadFileRenamer downloadFileRenamer, 
                                 DownloadTaskManager downloadTaskManager) {
        mSupportEngine = supportEngine;
        mDownloadFileRenamer = downloadFileRenamer;
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
        return mDownloadFileRenamer.getDownloadFile(url);
    }

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
    public void rename(String url, final String newFileName, final boolean includedSuffix, final OnRenameDownloadFileListener onRenameDownloadFileListener) {

        final DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo == null) {

            Log.d(TAG, "rename 文件不存在,url:" + url);

            // the DownloadFile does not exist
            if (onRenameDownloadFileListener != null) {
                onRenameDownloadFileListener.onRenameDownloadFileFailed(downloadFileInfo, new 
                        RenameDownloadFileFailReason("the download file is not exist!", RenameDownloadFileFailReason
                        .TYPE_FILE_RECORD_IS_NOT_EXIST));
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
                                RenameDownloadFileFailReason(failReason));
                    }
                }
            });
        }
    }

}
