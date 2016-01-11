package org.wlf.filedownloader.file_move;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Control;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.file_download.OnStopFileDownloadTaskListener;
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

    /**
     * LOG TAG
     */
    private static final String TAG = DownloadMoveManager.class.getSimpleName();

    /**
     * task engine
     */
    private ExecutorService mTaskEngine;
    /**
     * DownloadFileMover,to move download files
     */
    private DownloadFileMover mDownloadFileMover;
    /**
     * Pauseable,to pause download tasks
     */
    private Pauseable mDownloadTaskPauseable;
    /**
     * move control
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
        return mDownloadFileMover.getDownloadFile(url);
    }

    /**
     * move download
     */
    private void moveInternal(String url, String newDirPath, OnMoveDownloadFileListener onMoveDownloadFileListener) {
        // create move download task
        MoveDownloadFileTask moveDownloadFileTask = new MoveDownloadFileTask(url, newDirPath, mDownloadFileMover);
        moveDownloadFileTask.setOnMoveDownloadFileListener(onMoveDownloadFileListener);
        // run task
        addAndRunTask(moveDownloadFileTask);
    }

    /**
     * move download
     *
     * @param url                        file url
     * @param newDirPath                 new dir path
     * @param onMoveDownloadFileListener MoveDownloadFileListener
     */
    public void move(String url, final String newDirPath, final OnMoveDownloadFileListener onMoveDownloadFileListener) {

        final String finalUrl = url;

        if (!mDownloadTaskPauseable.isDownloading(url)) {

            Log.d(TAG, "move 直接移动,url:" + url);

            moveInternal(url, newDirPath, onMoveDownloadFileListener);
        } else {

            Log.d(TAG, "move 需要先暂停后移动,url:" + url);

            // pause
            mDownloadTaskPauseable.pause(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {

                    Log.d(TAG, "move 暂停成功，开始移动,url:" + finalUrl);

                    moveInternal(finalUrl, newDirPath, onMoveDownloadFileListener);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason failReason) {

                    Log.d(TAG, "move 暂停失败，无法移动,url:" + finalUrl);

                    notifyMoveDownloadFileFailed(getDownloadFile(finalUrl), new OnMoveDownloadFileFailReason
                            (failReason), onMoveDownloadFileListener);
                }
            });
        }
    }

    /**
     * move multi downloads
     *
     * @param urls                        file mUrls
     * @param newDirPath                  new dir path
     * @param onMoveDownloadFilesListener MoveDownloadFilesListener
     * @return the control for the operation
     */
    public Control move(List<String> urls, String newDirPath, OnMoveDownloadFilesListener onMoveDownloadFilesListener) {

        if (mMoveControl != null && !mMoveControl.isStopped()) {
            // under moving, ignore
            return mMoveControl;
        }

        MoveDownloadFilesTask moveDownloadFilesTask = new MoveDownloadFilesTask(urls, newDirPath, mDownloadFileMover);
        moveDownloadFilesTask.setOnMoveDownloadFilesListener(onMoveDownloadFilesListener);

        // start task
        addAndRunTask(moveDownloadFilesTask);

        // record new move control  
        mMoveControl = new MoveControl(moveDownloadFilesTask);

        return mMoveControl;
    }

    // -----------------------notify caller-----------------------

    private void notifyMoveDownloadFileFailed(DownloadFileInfo downloadFileInfo, MoveDownloadFileFailReason 
            failReason, OnMoveDownloadFileListener onMoveDownloadFileListener) {
        if (onMoveDownloadFileListener == null) {
            return;
        }

        // notify caller
        OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo, failReason, 
                onMoveDownloadFileListener);
    }

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
