package org.wlf.filedownloader.file_move;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.base.Stoppable;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener;
import org.wlf.filedownloader.listener.OnMoveDownloadFilesListener;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MoveDownloadFilesTask
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
class MoveDownloadFilesTask implements Runnable, Stoppable {

    /**
     * LOG TAG
     */
    private static final String TAG = MoveDownloadFilesTask.class.getSimpleName();

    private List<String> mUrls;
    private String mNewDirPath;
    private DownloadFileMover mDownloadFileMover;

    private Map<String, String> mOldFileDir = new HashMap<String, String>();

    private OnMoveDownloadFilesListener mOnMoveDownloadFilesListener;

    private boolean mIsStop = false;
    private boolean mIsNotifyFinish = false;

    private Object mLock = new Object();// lock

    final List<DownloadFileInfo> mDownloadFilesNeedMove = new ArrayList<DownloadFileInfo>();
    final List<DownloadFileInfo> mDownloadFilesMoved = new ArrayList<DownloadFileInfo>();
    final List<DownloadFileInfo> mDownloadFilesSkip = new ArrayList<DownloadFileInfo>();

    public MoveDownloadFilesTask(List<String> urls, String newDirPath, DownloadFileMover downloadFileMover) {
        super();
        this.mUrls = urls;
        this.mNewDirPath = newDirPath;
        this.mDownloadFileMover = downloadFileMover;
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
            OnMoveDownloadFileListener onMoveEverySingleDownloadFileListener = new OnMoveSingleDownloadFileListener();

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

                String url = downloadFileInfo.getUrl();

                // if the task stopped,notify completed
                if (isStopped()) {

                    Log.d(TAG, TAG + ".run 任务被取消，无法继续移动，任务即将结束");

                    // goto finally, notifyMoveDownloadFilesCompleted()
                    return;
                }

                // init single move task
                MoveDownloadFileTask moveSingleDownloadFileTask = new MoveDownloadFileTask(url, mNewDirPath, 
                        mDownloadFileMover);
                moveSingleDownloadFileTask.enableSyncCallback();// sync callback
                moveSingleDownloadFileTask.setOnMoveDownloadFileListener(onMoveEverySingleDownloadFileListener);

                // run single move task
                moveSingleDownloadFileTask.run();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // error, ignore
        } finally {

            // rollback move failed in db
            checkRollback();

            // notify caller
            notifyMoveDownloadFilesCompleted();

            Log.d(TAG, TAG + ".run 批量移动文件任务【已结束】");

            mIsStop = true;// the task is finish
        }
    }

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
            if (!FileUtil.isFilePath(oldDirPath)) {
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
        }
    }

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
        if (mIsNotifyFinish) {
            return;
        }

        int failedSize = mDownloadFilesNeedMove.size() - mDownloadFilesMoved.size();

        Log.d(TAG, TAG + ".run 批量移动完成，总共需要移动：" + mDownloadFilesNeedMove.size() + "，已移动：" + mDownloadFilesMoved.size()
                + "，失败：" + failedSize + "，跳过：" + mDownloadFilesSkip.size() + "，跳过数量是否等于失败数量：" + (failedSize == 
                mDownloadFilesSkip.size()));

        OnMoveDownloadFilesListener.MainThreadHelper.onMoveDownloadFilesCompleted(mDownloadFilesNeedMove, 
                mDownloadFilesMoved, mOnMoveDownloadFilesListener);
        mIsNotifyFinish = true;
    }
}