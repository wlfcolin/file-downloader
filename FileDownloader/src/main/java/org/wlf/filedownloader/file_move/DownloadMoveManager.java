package org.wlf.filedownloader.file_move;

import android.text.TextUtils;
import android.util.Log;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Control;
import org.wlf.filedownloader.base.Stoppable;
import org.wlf.filedownloader.file_download.DownloadTaskManager;
import org.wlf.filedownloader.file_download.FileDownloadTask.OnStopDownloadFileTaskFailReason;
import org.wlf.filedownloader.file_download.FileDownloadTask.OnStopFileDownloadTaskListener;
import org.wlf.filedownloader.file_move.MoveDownloadFileTask.DownloadFileMover;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener.MoveDownloadFileFailReason;
import org.wlf.filedownloader.listener.OnMoveDownloadFilesListener;
import org.wlf.filedownloader.util.UrlUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private ExecutorService mSupportEngine;
    /**
     * DownloadFileMover,to move download files
     */
    private DownloadFileMover mDownloadFileMover;
    /**
     * DownloadTaskManager,to manage download tasks
     */
    private DownloadTaskManager mDownloadTaskManager;
    /**
     * the task for move multi DownloadFiles,to move download files
     */
    private MoveDownloadFilesTask mMoveDownloadFilesTask;

    public DownloadMoveManager(ExecutorService supportEngine, DownloadFileMover downloadFileMover, 
                               DownloadTaskManager downloadTaskManager) {
        mSupportEngine = supportEngine;
        mDownloadFileMover = downloadFileMover;
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
        return mDownloadFileMover.getDownloadFile(url);
    }

    private void moveInternal(String url, String newDirPath, OnMoveDownloadFileListener onMoveDownloadFileListener, 
                              boolean isSyncCallback) {
        // create move download task
        MoveDownloadFileTask moveDownloadFileTask = new MoveDownloadFileTask(url, newDirPath, mDownloadFileMover);
        if (isSyncCallback) {
            moveDownloadFileTask.enableSyncCallback();
        }
        moveDownloadFileTask.setOnMoveDownloadFileListener(onMoveDownloadFileListener);
        // run task
        addAndRunTask(moveDownloadFileTask);
    }

    private void move(final String url, final String newDirPath, final OnMoveDownloadFileListener 
            onMoveDownloadFileListener, final boolean isSyncCallback) {

        final DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo == null) {

            Log.d(TAG, "move 文件不存在,url:" + url);

            // the DownloadFile does not exist
            if (onMoveDownloadFileListener != null) {
                onMoveDownloadFileListener.onMoveDownloadFileFailed(downloadFileInfo, new MoveDownloadFileFailReason
                        ("the download file doest not exist!", MoveDownloadFileFailReason
                                .TYPE_ORIGINAL_FILE_NOT_EXIST));
            }
            return;
        }

        if (!mDownloadTaskManager.isInFileDownloadTaskMap(url)) {

            Log.d(TAG, "move 直接移动,url:" + url);

            moveInternal(url, newDirPath, onMoveDownloadFileListener, isSyncCallback);
        } else {

            Log.d(TAG, "move 需要先暂停后移动,url:" + url);

            // pause
            mDownloadTaskManager.pause(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {

                    Log.d(TAG, "move 暂停成功，开始移动,url:" + url);

                    moveInternal(url, newDirPath, onMoveDownloadFileListener, isSyncCallback);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {

                    Log.d(TAG, "move 暂停失败，无法移动,url:" + url);

                    if (onMoveDownloadFileListener != null) {
                        OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo, new 
                                MoveDownloadFileFailReason(failReason), onMoveDownloadFileListener);
                    }
                }
            });
        }
    }

    /**
     * move download
     *
     * @param url                        file url
     * @param newDirPath                 new dir path
     * @param onMoveDownloadFileListener MoveDownloadFileListener
     */
    public void move(String url, String newDirPath, OnMoveDownloadFileListener onMoveDownloadFileListener) {
        move(url, newDirPath, onMoveDownloadFileListener, false);
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

        if (mMoveDownloadFilesTask != null && !mMoveDownloadFilesTask.isStopped()) {
            // moving
            return new MoveControl(mMoveDownloadFilesTask);
        }

        MoveDownloadFilesTask moveDownloadFilesTask = new MoveDownloadFilesTask(urls, newDirPath);
        moveDownloadFilesTask.setOnMoveDownloadFilesListener(onMoveDownloadFilesListener);

        // start task
        addAndRunTask(moveDownloadFilesTask);
        this.mMoveDownloadFilesTask = moveDownloadFilesTask;
        return new MoveControl(this.mMoveDownloadFilesTask);
    }

    /**
     * move multi download task
     */
    private class MoveDownloadFilesTask implements Runnable, Stoppable {

        private List<String> mUrls;
        private String mNewDirPath;
        private Map<String, String> mOldFileDir = new HashMap<String, String>();
        private OnMoveDownloadFilesListener mOnMoveDownloadFilesListener;

        private boolean mIsStop = false;
        private boolean mCompleted = false;

        final List<DownloadFileInfo> mDownloadFilesNeedMove = new ArrayList<DownloadFileInfo>();
        final List<DownloadFileInfo> mDownloadFilesMoved = new ArrayList<DownloadFileInfo>();
        final List<DownloadFileInfo> mDownloadFilesSkip = new ArrayList<DownloadFileInfo>();

        public MoveDownloadFilesTask(List<String> urls, String newDirPath) {
            super();
            this.mUrls = urls;
            this.mNewDirPath = newDirPath;
        }

        public void setOnMoveDownloadFilesListener(OnMoveDownloadFilesListener onMoveDownloadFilesListener) {
            this.mOnMoveDownloadFilesListener = onMoveDownloadFilesListener;
        }

        public void stop() {
            this.mIsStop = true;
        }

        public boolean isStopped() {
            return mIsStop;
        }

        @Override
        public void run() {

            for (String url : mUrls) {
                if (!UrlUtil.isUrl(url)) {
                    continue;
                }
                DownloadFileInfo downloadFileInfo = getDownloadFile(url);
                if (downloadFileInfo != null) {
                    mDownloadFilesNeedMove.add(downloadFileInfo);
                    if (!TextUtils.isEmpty(downloadFileInfo.getUrl())) {
                        mOldFileDir.put(downloadFileInfo.getUrl(), downloadFileInfo.getFileDir());
                    }
                }
            }

            // prepare to delete
            if (mOnMoveDownloadFilesListener != null) {

                Log.d(TAG, "MoveDownloadFilesTask.run 准备批量移动，大小：" + mDownloadFilesNeedMove.size());

                OnMoveDownloadFilesListener.MainThreadHelper.onMoveDownloadFilesPrepared(mDownloadFilesNeedMove, 
                        mOnMoveDownloadFilesListener);
            }

            OnMoveDownloadFileListener onMoveDownloadFileListener = new OnMoveDownloadFileListener() {

                private int deleteCount = 0;

                @Override
                public void onMoveDownloadFilePrepared(DownloadFileInfo downloadFileNeedToMove) {

                    String url = null;
                    if (downloadFileNeedToMove != null) {
                        url = downloadFileNeedToMove.getUrl();
                    }

                    Log.d(TAG, "MoveDownloadFilesTask.run 准备删除，url：" + url);

                    // start new move
                    if (mOnMoveDownloadFilesListener != null) {
                        OnMoveDownloadFilesListener.MainThreadHelper.onMovingDownloadFiles(mDownloadFilesNeedMove, 
                                mDownloadFilesMoved, mDownloadFilesSkip, downloadFileNeedToMove, 
                                mOnMoveDownloadFilesListener);
                    }

                    deleteCount++;
                }

                @Override
                public void onMoveDownloadFileSuccess(DownloadFileInfo downloadFileMoved) {

                    String url = null;
                    if (downloadFileMoved != null) {
                        url = downloadFileMoved.getUrl();
                    }

                    Log.d(TAG, "MoveDownloadFilesTask.run onMoveDownloadFileSuccess,移动成功，moveCount：" + deleteCount +
                            ",mDownloadFilesNeedMove.size():" + mDownloadFilesNeedMove.size() + "，url：" + url);

                    // move succeed
                    mDownloadFilesMoved.add(downloadFileMoved);

                    // if the last one to move,notify finish the operation
                    if (deleteCount == mDownloadFilesNeedMove.size() - mDownloadFilesSkip.size()) {

                        Log.d(TAG, "MoveDownloadFilesTask.run onMoveDownloadFileSuccess," + 
                                "移动成功，回调onMoveDownloadFilesCompleted");

                        onMoveDownloadFilesCompleted();
                    }
                }

                @Override
                public void onMoveDownloadFileFailed(DownloadFileInfo downloadFileInfo, MoveDownloadFileFailReason failReason) {

                    String url = null;
                    if (downloadFileInfo != null) {
                        url = downloadFileInfo.getUrl();
                    }

                    String type = null;
                    if (failReason != null) {
                        type = failReason.getType();
                    }

                    Log.d(TAG, "MoveDownloadFilesTask.run onMoveDownloadFileFailed,移动失败，moveCount：" + deleteCount +
                            ",mDownloadFilesNeedMove.size():" + mDownloadFilesNeedMove.size() + "，url：" + url + "," +
                            "failReason:" + type);

                    // move failed
                    mDownloadFilesSkip.add(downloadFileInfo);

                    // if the last one to move,notify finish the operation
                    if (deleteCount == mDownloadFilesNeedMove.size() - mDownloadFilesSkip.size()) {

                        Log.d(TAG, "MoveDownloadFilesTask.run onMoveDownloadFileFailed," + 
                                "移动失败，回调onMoveDownloadFilesCompleted");

                        onMoveDownloadFilesCompleted();
                    }
                }
            };

            // move every single one
            for (int i = 0; i < mDownloadFilesNeedMove.size(); i++) {

                DownloadFileInfo downloadFileInfo = mDownloadFilesNeedMove.get(i);
                if (downloadFileInfo == null) {
                    continue;
                }

                String url = downloadFileInfo.getUrl();

                // if the task stopped,notify completed
                if (isStopped()) {

                    Log.d(TAG, "MoveDownloadFilesTask.run task has been sopped," + 
                            "任务已经被取消，无法继续移动，回调onMoveDownloadFilesCompleted");

                    onMoveDownloadFilesCompleted();
                } else {
                    // moving
                    move(url, mNewDirPath, onMoveDownloadFileListener, true);
                }
            }

            mIsStop = true;// because is sync delete,so at this time,the task is finish
        }

        // move finish
        private void onMoveDownloadFilesCompleted() {
            if (mCompleted) {
                return;
            }

            if (mOnMoveDownloadFilesListener != null) {
                OnMoveDownloadFilesListener.MainThreadHelper.onMoveDownloadFilesCompleted(mDownloadFilesNeedMove, 
                        mDownloadFilesMoved, mOnMoveDownloadFilesListener);
            }
            mCompleted = true;
            mIsStop = true;
        }
    }

    static class MoveControl implements Control {

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
