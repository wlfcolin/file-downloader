package org.wlf.filedownloader.file_delete;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Control;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.file_download.base.OnStopFileDownloadTaskListener;
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

    private static final String TAG = DownloadDeleteManager.class.getSimpleName();

    /**
     * task engine
     */
    private ExecutorService mTaskEngine;
    /**
     * DownloadFileDeleter, which to delete download files in record
     */
    private DownloadFileDeleter mDownloadFileDeleter;
    /**
     * Pauseable, which to pause download tasks
     */
    private Pauseable mDownloadTaskPauseable;
    /**
     * multi delete control
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
        return mDownloadFileDeleter.getDownloadFile(url);
    }

    /**
     * delete a download
     */
    private void singleDeleteInternal(String url, boolean deleteDownloadedFileInPath, OnDeleteDownloadFileListener 
            onDeleteDownloadFileListener) {
        // create a delete download task
        DeleteDownloadFileTask deleteDownloadFileTask = new DeleteDownloadFileTask(url, deleteDownloadedFileInPath, 
                mDownloadFileDeleter);
        deleteDownloadFileTask.setOnDeleteDownloadFileListener(onDeleteDownloadFileListener);
        // run the task
        addAndRunTask(deleteDownloadFileTask);
    }

    /**
     * delete a download file
     *
     * @param url                          file url
     * @param deleteDownloadedFileInPath   whether delete file in path
     * @param onDeleteDownloadFileListener OnDeleteDownloadFileListener impl
     */
    public void delete(final String url, final boolean deleteDownloadedFileInPath, final OnDeleteDownloadFileListener
            onDeleteDownloadFileListener) {

        final String finalUrl = url;

        // the download task has been stopped
        if (!mDownloadTaskPauseable.isDownloading(url)) {

            Log.d(TAG, TAG + ".delete 下载任务已经暂停，可以直接删除，url:" + url);

            singleDeleteInternal(url, deleteDownloadedFileInPath, onDeleteDownloadFileListener);
        } else {

            Log.d(TAG, TAG + ".delete 需要先暂停下载任务后删除,url:" + url);

            // pause
            mDownloadTaskPauseable.pause(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {

                    Log.d(TAG, TAG + ".delete 暂停下载任务成功，开始删除，url:" + finalUrl);

                    singleDeleteInternal(finalUrl, deleteDownloadedFileInPath, onDeleteDownloadFileListener);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason failReason) {

                    if (failReason != null) {
                        if (StopDownloadFileTaskFailReason.TYPE_TASK_HAS_BEEN_STOPPED.equals(failReason.getType())) {
                            // has been stopped, so can restart normally
                            singleDeleteInternal(finalUrl, deleteDownloadedFileInPath, onDeleteDownloadFileListener);
                            return;
                        }
                    }

                    Log.d(TAG, TAG + ".delete 暂停下载任务失败，无法删除，url:" + finalUrl);

                    // otherwise error occur, notify caller
                    notifyDeleteDownloadFileFailed(getDownloadFile(finalUrl), new OnDeleteDownloadFileFailReason
                            (finalUrl, failReason), onDeleteDownloadFileListener);
                }
            });
        }
    }

    /**
     * delete multi download files
     *
     * @param urls                          file urls
     * @param deleteDownloadedFile          whether delete file in path
     * @param onDeleteDownloadFilesListener OnDeleteDownloadFilesListener impl
     * @return a control for the operation
     */
    public Control delete(List<String> urls, boolean deleteDownloadedFile, OnDeleteDownloadFilesListener 
            onDeleteDownloadFilesListener) {

        if (mDeleteControl != null && !mDeleteControl.isStopped()) {
            // under deleting, ignore
            return mDeleteControl;// FIXME whether need to notify caller by onMoveDownloadFilesListener ?
        }

        // create a multi delete task
        DeleteDownloadFilesTask deleteDownloadFilesTask = new DeleteDownloadFilesTask(urls, deleteDownloadedFile, 
                mTaskEngine, mDownloadFileDeleter, mDownloadTaskPauseable);
        deleteDownloadFilesTask.setOnDeleteDownloadFilesListener(onDeleteDownloadFilesListener);

        // start the task
        addAndRunTask(deleteDownloadFilesTask);

        // record new delete control  
        mDeleteControl = new DeleteControl(deleteDownloadFilesTask);

        return mDeleteControl;
    }

    // --------------------------------------notify caller--------------------------------------

    /**
     * notifyDeleteDownloadFileFailed
     */
    private void notifyDeleteDownloadFileFailed(DownloadFileInfo downloadFileInfo, DeleteDownloadFileFailReason 
            failReason, OnDeleteDownloadFileListener onDeleteDownloadFileListener) {
        // main thread notify caller
        OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFileFailed(downloadFileInfo, failReason, 
                onDeleteDownloadFileListener);
    }

    // --------------------------------------internal classes--------------------------------------

    /**
     * MoveControl for multi delete
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
