package org.wlf.filedownloader.file_move;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Control;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.file_download.base.OnStopFileDownloadTaskListener;
import org.wlf.filedownloader.file_download.base.Pauseable;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener.MoveDownloadFileFailReason;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener.OnMoveDownloadFileFailReason;
import org.wlf.filedownloader.listener.OnMoveDownloadFilesListener;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * DownloadMoveManager
 *
 * @author wlf(Andy)
 * @datetime 2015-12-10 10:49 GMT+8
 * @email 411086563@qq.com
 * @since 0.2.0
 */
public class DownloadMoveManager {

    private static final String TAG = DownloadMoveManager.class.getSimpleName();

    /**
     * task engine
     */
    private ExecutorService mTaskEngine;
    /**
     * DownloadFileMover, which to move download files in record
     */
    private DownloadFileMover mDownloadFileMover;
    /**
     * Pauseable, which to pause download tasks
     */
    private Pauseable mDownloadTaskPauseable;
    /**
     * multi move control
     */
    private MoveControl mMoveControl;

    public DownloadMoveManager(ExecutorService taskEngine, DownloadFileMover downloadFileMover, Pauseable 
            downloadTaskPauseable) {
        mTaskEngine = taskEngine;
        mDownloadFileMover = downloadFileMover;
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
        return mDownloadFileMover.getDownloadFile(url);
    }

    /**
     * move a download
     */
    private void singleMoveInternal(String url, String newDirPath, OnMoveDownloadFileListener 
            onMoveDownloadFileListener) {
        // create a move download task
        MoveDownloadFileTask moveDownloadFileTask = new MoveDownloadFileTask(url, newDirPath, mDownloadFileMover);
        moveDownloadFileTask.setOnMoveDownloadFileListener(onMoveDownloadFileListener);
        // run the task
        addAndRunTask(moveDownloadFileTask);
    }

    /**
     * move a download
     *
     * @param url                        file url
     * @param newDirPath                 new dir path
     * @param onMoveDownloadFileListener OnMoveDownloadFilesListener impl
     */
    public void move(String url, final String newDirPath, final OnMoveDownloadFileListener onMoveDownloadFileListener) {

        final String finalUrl = url;

        // the download task has been stopped
        if (!mDownloadTaskPauseable.isDownloading(url)) {

            Log.d(TAG, TAG + ".move 下载任务已经暂停，可以直接移动，url:" + url);

            singleMoveInternal(url, newDirPath, onMoveDownloadFileListener);
        } else {

            Log.d(TAG, TAG + ".move 需要先暂停下载任务后移动，url:" + url);

            // pause first
            mDownloadTaskPauseable.pause(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {

                    Log.d(TAG, TAG + ".move 暂停下载任务成功，开始移动，url:" + finalUrl);

                    singleMoveInternal(finalUrl, newDirPath, onMoveDownloadFileListener);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason failReason) {

                    if (failReason != null) {
                        if (StopDownloadFileTaskFailReason.TYPE_TASK_HAS_BEEN_STOPPED.equals(failReason.getType())) {
                            // has been stopped, so can restart normally
                            singleMoveInternal(finalUrl, newDirPath, onMoveDownloadFileListener);
                            return;
                        }
                    }

                    Log.d(TAG, TAG + ".move 暂停下载任务失败，无法移动，url:" + finalUrl);

                    // otherwise error occur, notify caller
                    notifyMoveDownloadFileFailed(getDownloadFile(finalUrl), new OnMoveDownloadFileFailReason
                            (finalUrl, failReason), onMoveDownloadFileListener);
                }
            });
        }
    }

    /**
     * move multi download files
     *
     * @param urls                        file urls
     * @param newDirPath                  new dir path
     * @param onMoveDownloadFilesListener OnMoveDownloadFilesListener impl
     * @return a control for the operation
     */
    public Control move(List<String> urls, String newDirPath, OnMoveDownloadFilesListener onMoveDownloadFilesListener) {

        if (mMoveControl != null && !mMoveControl.isStopped()) {
            // under moving, ignore
            return mMoveControl;// FIXME whether need to notify caller by onMoveDownloadFilesListener ?
        }

        // create a multi move task
        MoveDownloadFilesTask moveDownloadFilesTask = new MoveDownloadFilesTask(urls, newDirPath, mTaskEngine, 
                mDownloadFileMover, mDownloadTaskPauseable);
        moveDownloadFilesTask.setOnMoveDownloadFilesListener(onMoveDownloadFilesListener);

        // start the task
        addAndRunTask(moveDownloadFilesTask);

        // record new move control  
        mMoveControl = new MoveControl(moveDownloadFilesTask);

        return mMoveControl;
    }

    // --------------------------------------notify caller--------------------------------------

    /**
     * notifyMoveDownloadFileFailed
     */
    private void notifyMoveDownloadFileFailed(DownloadFileInfo downloadFileInfo, MoveDownloadFileFailReason 
            failReason, OnMoveDownloadFileListener onMoveDownloadFileListener) {
        // main thread notify caller
        OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo, failReason, 
                onMoveDownloadFileListener);
    }

    // --------------------------------------internal classes--------------------------------------

    /**
     * MoveControl for multi move
     */
    private static class MoveControl implements Control {

        private MoveDownloadFilesTask mMoveDownloadFilesTask;

        private MoveControl(MoveDownloadFilesTask moveDownloadFilesTask) {
            mMoveDownloadFilesTask = moveDownloadFilesTask;
        }

        @Override
        public void stop() {
            if (mMoveDownloadFilesTask != null) {
                mMoveDownloadFilesTask.stop();
            }
        }

        @Override
        public boolean isStopped() {
            if (mMoveDownloadFilesTask == null) {
                return true;
            }
            return mMoveDownloadFilesTask.isStopped();
        }
    }
}
