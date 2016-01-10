package org.wlf.filedownloader.file_delete;

import android.util.Log;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Control;
import org.wlf.filedownloader.file_download.OnStopFileDownloadTaskListener;
import org.wlf.filedownloader.file_download.base.Pauseable;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener.DeleteDownloadFileFailReason;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener.OnDeleteDownloadFileFailReason;
import org.wlf.filedownloader.listener.OnDeleteDownloadFilesListener;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * DownloadDeleteManager
 *
 * @author wlf(Andy)
 * @datetime 2015-12-10 10:49 GMT+8
 * @email 411086563@qq.com
 * @since 0.2.0
 */
public class DownloadDeleteManager {

    /**
     * LOG TAG
     */
    private static final String TAG = DownloadDeleteManager.class.getSimpleName();

    /**
     * task engine
     */
    private ExecutorService mTaskEngine;
    /**
     * DownloadFileDeleter,to manage download files
     */
    private DownloadFileDeleter mDownloadFileDeleter;
    /**
     * Pauseable,to pause download tasks
     */
    private Pauseable mDownloadTaskPauseable;
    /**
     * delete control
     */
    private DeleteControl mDeleteControl;

    public DownloadDeleteManager(ExecutorService taskEngine, DownloadFileDeleter downloadFileDeleter, Pauseable 
            downloadTaskPauseable) {
        mTaskEngine = taskEngine;
        mDownloadFileDeleter = downloadFileDeleter;
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
        return mDownloadFileDeleter.getDownloadFile(url);
    }

    /**
     * delete download
     */
    private void deleteInternal(String url, boolean deleteDownloadedFileInPath, OnDeleteDownloadFileListener 
            onDeleteDownloadFileListener) {
        // create delete download task
        DeleteDownloadFileTask deleteDownloadFileTask = new DeleteDownloadFileTask(url, deleteDownloadedFileInPath, 
                mDownloadFileDeleter);
        deleteDownloadFileTask.setOnDeleteDownloadFileListener(onDeleteDownloadFileListener);
        // run task
        addAndRunTask(deleteDownloadFileTask);
    }

    /**
     * delete download
     *
     * @param url                          file url
     * @param deleteDownloadedFileInPath   whether delete file in path
     * @param onDeleteDownloadFileListener DeleteDownloadFileListener
     */
    public void delete(final String url, final boolean deleteDownloadedFileInPath, final OnDeleteDownloadFileListener
            onDeleteDownloadFileListener) {

        final String finalUrl = url;

        // not downloading, delete direct
        if (!mDownloadTaskPauseable.isDownloading(url)) {

            Log.d(TAG, "delete 直接删除,url:" + url);

            deleteInternal(url, deleteDownloadedFileInPath, onDeleteDownloadFileListener);
        } else {

            Log.d(TAG, "delete 需要先暂停后删除,url:" + url);

            // pause
            mDownloadTaskPauseable.pause(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {

                    Log.d(TAG, "delete 暂停成功，开始删除,url:" + finalUrl);

                    deleteInternal(finalUrl, deleteDownloadedFileInPath, onDeleteDownloadFileListener);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason failReason) {

                    Log.d(TAG, "delete 暂停失败，无法删除,url:" + finalUrl);

                    // notify caller
                    notifyDeleteDownloadFileFailed(getDownloadFile(finalUrl), new OnDeleteDownloadFileFailReason
                            (failReason), onDeleteDownloadFileListener);
                }
            });
        }
    }

    /**
     * delete multi downloads
     *
     * @param urls                          file mUrls
     * @param deleteDownloadedFile          whether delete file in path
     * @param onDeleteDownloadFilesListener DeleteDownloadFilesListener
     * @return the control for the operation
     */
    public Control delete(List<String> urls, boolean deleteDownloadedFile, OnDeleteDownloadFilesListener 
            onDeleteDownloadFilesListener) {

        if (mDeleteControl != null && !mDeleteControl.isStopped()) {
            // under deleting, ignore
            return mDeleteControl;
        }

        DeleteDownloadFilesTask deleteDownloadFilesTask = new DeleteDownloadFilesTask(urls, deleteDownloadedFile, 
                mDownloadFileDeleter);
        deleteDownloadFilesTask.setOnDeleteDownloadFilesListener(onDeleteDownloadFilesListener);

        // start task
        addAndRunTask(deleteDownloadFilesTask);

        // record new delete control  
        mDeleteControl = new DeleteControl(deleteDownloadFilesTask);

        return mDeleteControl;
    }

    // -----------------------notify caller-----------------------

    private void notifyDeleteDownloadFileFailed(DownloadFileInfo downloadFileInfo, DeleteDownloadFileFailReason 
            failReason, OnDeleteDownloadFileListener onDeleteDownloadFileListener) {
        if (onDeleteDownloadFileListener == null) {
            return;
        }

        // notify caller
        OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFileFailed(downloadFileInfo, failReason, 
                onDeleteDownloadFileListener);
    }

    /**
     * DeleteControl
     */
    private class DeleteControl implements Control {

        private DeleteDownloadFilesTask mDeleteDownloadFilesTask;

        private DeleteControl(DeleteDownloadFilesTask deleteDownloadFilesTask) {
            mDeleteDownloadFilesTask = deleteDownloadFilesTask;
        }

        @Override
        public void stop() {
            if (mDeleteDownloadFilesTask != null) {
                mDeleteDownloadFilesTask.stop();
            }
        }

        @Override
        public boolean isStopped() {
            if (mDeleteDownloadFilesTask == null) {
                return true;
            }
            return mDeleteDownloadFilesTask.isStopped();
        }
    }
}
