package org.wlf.filedownloader.file_move;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.base.Stoppable;
import org.wlf.filedownloader.file_download.base.OnStopFileDownloadTaskListener;
import org.wlf.filedownloader.file_download.base.Pauseable;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener;
import org.wlf.filedownloader.listener.OnMoveDownloadFilesListener;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MoveDownloadFilesTask
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
class MoveDownloadFilesTask implements Runnable, Stoppable {

    private static final String TAG = MoveDownloadFilesTask.class.getSimpleName();

    private List<String> mUrls;
    private String mNewDirPath;
    // task engine
    private ExecutorService mTaskEngine;
    //DownloadFileMover, which to move download files in record
    private DownloadFileMover mDownloadFileMover;
    // Pauseable, which to pause download tasks
    private Pauseable mDownloadTaskPauseable;

    private Map<String, String> mOldFileDir = new HashMap<String, String>();

    private OnMoveDownloadFilesListener mOnMoveDownloadFilesListener;

    private boolean mIsStop = false;
    private AtomicBoolean mIsNotifyFinish = new AtomicBoolean(false);

    private Object mLock = new Object();// lock

    private final List<DownloadFileInfo> mDownloadFilesNeedMove = new ArrayList<DownloadFileInfo>();
    private final List<DownloadFileInfo> mDownloadFilesMoved = new ArrayList<DownloadFileInfo>();
    private final List<DownloadFileInfo> mDownloadFilesSkip = new ArrayList<DownloadFileInfo>();

    public MoveDownloadFilesTask(List<String> urls, String newDirPath, ExecutorService taskEngine, DownloadFileMover 
            downloadFileMover, Pauseable downloadTaskPauseable) {
        super();
        this.mUrls = urls;
        this.mNewDirPath = newDirPath;
        this.mTaskEngine = taskEngine;
        this.mDownloadFileMover = downloadFileMover;
        this.mDownloadTaskPauseable = downloadTaskPauseable;
    }

    /**
     * set OnMoveDownloadFilesListener
     *
     * @param onMoveDownloadFilesListener OnMoveDownloadFilesListener
     */
    public void setOnMoveDownloadFilesListener(OnMoveDownloadFilesListener onMoveDownloadFilesListener) {
        this.mOnMoveDownloadFilesListener = onMoveDownloadFilesListener;
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

    @Override
    public void stop() {
        this.mIsStop = true;
    }

    @Override
    public boolean isStopped() {
        return mIsStop;
    }

    // --------------------------------------run the task--------------------------------------

    @Override
    public void run() {

        try {
            mDownloadFilesNeedMove.clear();
            mDownloadFilesMoved.clear();
            mDownloadFilesSkip.clear();

            for (String url : mUrls) {
                if (!UrlUtil.isUrl(url)) {
                    continue;
                }
                DownloadFileInfo downloadFileInfo = getDownloadFile(url);
                if (downloadFileInfo != null) {
                    //                    synchronized (mLock) {//lock
                    mDownloadFilesNeedMove.add(downloadFileInfo);
                    // backup the file dir
                    mOldFileDir.put(downloadFileInfo.getUrl(), downloadFileInfo.getFileDir());
                    //                    }
                }
            }

            // prepare to move
            notifyMoveDownloadFilesPrepared();

            // move every single download file listener
            final OnMoveDownloadFileListener onMoveEverySingleDownloadFileListener = new 
                    OnMoveSingleDownloadFileListener();

            // move every single one
            for (int i = 0; i < mDownloadFilesNeedMove.size(); i++) {

                DownloadFileInfo downloadFileInfo = mDownloadFilesNeedMove.get(i);
                if (downloadFileInfo == null) {
                    // skip one
                    synchronized (mLock) {//lock
                        mDownloadFilesSkip.add(downloadFileInfo);
                    }
                    continue;
                }

                final String finalUrl = downloadFileInfo.getUrl();

                // if the task stopped,notify completed
                if (isStopped()) {

                    Log.d(TAG, TAG + ".run 批量移动任务被取消，无法继续移动，任务即将结束");

                    // goto finally, notifyMoveDownloadFilesCompleted()
                    return;
                }

                // downloading
                if (mDownloadTaskPauseable.isDownloading(finalUrl)) {

                    Log.d(TAG, TAG + ".run 需要先暂停单个下载任务后移动，url:" + finalUrl);

                    // pause first
                    mDownloadTaskPauseable.pause(finalUrl, new OnStopFileDownloadTaskListener() {
                        @Override
                        public void onStopFileDownloadTaskSucceed(String url) {

                            Log.d(TAG, TAG + ".run 暂停单个下载任务成功，开始移动，url:" + finalUrl);

                            // if the task stopped, notify completed
                            if (isStopped()) {

                                Log.d(TAG, TAG + ".run 批量移动任务被取消，无法继续移动，任务即将结束");

                                // notify caller
                                notifyMoveDownloadFilesCompleted();
                                return;
                            }

                            // stopped, continue move
                            runSingleMoveTask(url, onMoveEverySingleDownloadFileListener, false);
                        }

                        @Override
                        public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason 
                                failReason) {

                            // if the task stopped, notify completed
                            if (isStopped()) {

                                Log.d(TAG, TAG + ".run 批量移动任务被取消，无法继续移动，任务即将结束");

                                // notify caller
                                notifyMoveDownloadFilesCompleted();
                                return;
                            }

                            if (failReason != null) {
                                if (StopDownloadFileTaskFailReason.TYPE_TASK_HAS_BEEN_STOPPED.equals(failReason
                                        .getType())) {
                                    // stopped, continue move
                                    runSingleMoveTask(url, onMoveEverySingleDownloadFileListener, false);
                                    return;
                                }
                            }

                            Log.d(TAG, TAG + ".run 暂停单个下载任务失败，无法移动，url:" + finalUrl);

                            // failed
                            synchronized (mLock) {// lock
                                mDownloadFilesSkip.add(getDownloadFile(finalUrl));
                            }
                        }
                    });
                } else {
                    // run move task directly
                    runSingleMoveTask(finalUrl, onMoveEverySingleDownloadFileListener, true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // error, ignore
        } finally {

            if (isStopped()) {
                // notify caller
                notifyMoveDownloadFilesCompleted();
            }

            Log.d(TAG, TAG + ".run 批量移动文件主任务【已结束】，但是通过暂停下载中的文件移动任务可能还没有结束");
        }
    }

    /**
     * run move single file task
     */
    private void runSingleMoveTask(String url, OnMoveDownloadFileListener onMoveEverySingleDownloadFileListener, 
                                   boolean sync) {

        // init single move task
        MoveDownloadFileTask moveSingleDownloadFileTask = new MoveDownloadFileTask(url, mNewDirPath, 
                mDownloadFileMover);
        moveSingleDownloadFileTask.enableSyncCallback();// sync callback
        moveSingleDownloadFileTask.setOnMoveDownloadFileListener(onMoveEverySingleDownloadFileListener);

        if (sync) {
            // run single move task
            moveSingleDownloadFileTask.run();
        } else {
            // run async
            mTaskEngine.execute(moveSingleDownloadFileTask);
        }
    }

    /**
     * single move callback
     */
    private class OnMoveSingleDownloadFileListener implements OnMoveDownloadFileListener {

        @Override
        public void onMoveDownloadFilePrepared(DownloadFileInfo downloadFileNeedToMove) {

            // notify callback
            notifyMovingDownloadFiles(downloadFileNeedToMove);
        }

        @Override
        public void onMoveDownloadFileSuccess(DownloadFileInfo downloadFileMoved) {

            String url = null;
            if (downloadFileMoved != null) {
                url = downloadFileMoved.getUrl();
            }

            synchronized (mLock) {// lock
                // move succeed
                mDownloadFilesMoved.add(downloadFileMoved);
            }

            Log.d(TAG, TAG + ".run 移动单个成功，已移动数量：" + mDownloadFilesMoved.size() + "，总共需要移动数量" +
                    mDownloadFilesNeedMove.size() + "，url：" + url);

            if (mDownloadFilesMoved.size() + mDownloadFilesSkip.size() == mDownloadFilesNeedMove.size()) {
                // complete, notify caller
                notifyMoveDownloadFilesCompleted();
            }
        }

        @Override
        public void onMoveDownloadFileFailed(DownloadFileInfo downloadFileInfo, MoveDownloadFileFailReason failReason) {

            String url = null;
            if (downloadFileInfo != null) {
                url = downloadFileInfo.getUrl();
            }

            String msg = null;
            if (failReason != null) {
                msg = failReason.getMessage();
            }

            Log.d(TAG, TAG + ".run 移动单个成功，已移动数量：" + mDownloadFilesMoved.size() + "，总共需要移动数量" +
                    mDownloadFilesNeedMove.size() + "，失败原因：" + msg + "，url：" + url);

            synchronized (mLock) {// lock
                // move failed
                mDownloadFilesSkip.add(downloadFileInfo);
            }

            if (mDownloadFilesMoved.size() + mDownloadFilesSkip.size() == mDownloadFilesNeedMove.size()) {
                // complete, notify caller
                notifyMoveDownloadFilesCompleted();
            }
        }
    }

    // --------------------------------------notify caller--------------------------------------

    /**
     * notifyMoveDownloadFilesPrepared
     */
    private void notifyMoveDownloadFilesPrepared() {

        Log.d(TAG, TAG + ".run 准备批量移动，大小：" + mDownloadFilesNeedMove.size());

        OnMoveDownloadFilesListener.MainThreadHelper.onMoveDownloadFilesPrepared(mDownloadFilesNeedMove,
                mOnMoveDownloadFilesListener);
    }

    /**
     * notifyMovingDownloadFiles
     */
    private void notifyMovingDownloadFiles(DownloadFileInfo downloadFileInfo) {

        String url = null;
        if (downloadFileInfo != null) {
            url = downloadFileInfo.getUrl();
        }

        Log.d(TAG, TAG + ".run 准备移动单个，url：" + url);

        OnMoveDownloadFilesListener.MainThreadHelper.onMovingDownloadFiles(mDownloadFilesNeedMove,
                mDownloadFilesMoved, mDownloadFilesSkip, downloadFileInfo, mOnMoveDownloadFilesListener);
    }

    /**
     * notifyMoveDownloadFilesCompleted
     */
    private void notifyMoveDownloadFilesCompleted() {
        if (mIsNotifyFinish.get()) {
            return;
        }

        if (mIsNotifyFinish.compareAndSet(false, true)) {

            checkRollback();// rollback check

            OnMoveDownloadFilesListener.MainThreadHelper.onMoveDownloadFilesCompleted(mDownloadFilesNeedMove,
                    mDownloadFilesMoved, mOnMoveDownloadFilesListener);

            mIsStop = true;// the task is finish

            int failedSize = mDownloadFilesNeedMove.size() - mDownloadFilesMoved.size();

            Log.d(TAG, TAG + ".run，批量移动文件主任务和其它相关任务全部【已结束】，总共需要移动：" + mDownloadFilesNeedMove.size() + "，已移动：" +
                    mDownloadFilesMoved.size() + "，失败：" + failedSize + "，跳过：" + mDownloadFilesSkip.size() +
                    "，跳过数量是否等于失败数量：" + (failedSize == mDownloadFilesSkip.size()));
        }
    }

    /**
     * check and rollback those failed
     */
    private void checkRollback() {
        if (CollectionUtil.isEmpty(mDownloadFilesSkip)) {
            return;
        }
        // rollback move failed in db
        for (DownloadFileInfo downloadFileInfo : mDownloadFilesSkip) {
            if (downloadFileInfo == null) {
                continue;
            }

            String oldDirPath = mOldFileDir.get(downloadFileInfo.getUrl());
            if (!FileUtil.isFilePath(oldDirPath) || oldDirPath.equals(downloadFileInfo.getFileDir())) {
                continue;
            }

            // rollback in db
            try {
                mDownloadFileMover.moveDownloadFile(downloadFileInfo.getUrl(), oldDirPath);
            } catch (Exception e) {
                e.printStackTrace();
                // try again
                try {
                    mDownloadFileMover.moveDownloadFile(downloadFileInfo.getUrl(), oldDirPath);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    // ignore   
                }
            }
        }
    }
}