package org.wlf.filedownloader.file_delete;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.base.Stoppable;
import org.wlf.filedownloader.file_download.base.OnStopFileDownloadTaskListener;
import org.wlf.filedownloader.file_download.base.Pauseable;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener;
import org.wlf.filedownloader.listener.OnDeleteDownloadFilesListener;
import org.wlf.filedownloader.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DeleteDownloadFilesTask
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
class DeleteDownloadFilesTask implements Runnable, Stoppable {

    private static final String TAG = DeleteDownloadFilesTask.class.getSimpleName();

    private List<String> mUrls;
    private boolean mDeleteDownloadedFile;
    // task engine
    private ExecutorService mTaskEngine;
    //DownloadFileDeleter, which to delete download files in record
    private DownloadFileDeleter mDownloadFileDeleter;
    // Pauseable, which to pause download tasks
    private Pauseable mDownloadTaskPauseable;

    private OnDeleteDownloadFilesListener mOnDeleteDownloadFilesListener;

    private boolean mIsStop = false;
    private AtomicBoolean mIsNotifyFinish = new AtomicBoolean(false);

    private Object mLock = new Object();// lock

    private final List<DownloadFileInfo> mDownloadFilesNeedDelete = new ArrayList<DownloadFileInfo>();
    private final List<DownloadFileInfo> mDownloadFilesDeleted = new ArrayList<DownloadFileInfo>();
    private final List<DownloadFileInfo> mDownloadFilesSkip = new ArrayList<DownloadFileInfo>();

    public DeleteDownloadFilesTask(List<String> urls, boolean deleteDownloadedFile, ExecutorService taskEngine,
                                   DownloadFileDeleter downloadFileDeleter, Pauseable downloadTaskPauseable) {
        super();
        this.mUrls = urls;
        this.mTaskEngine = taskEngine;
        this.mDeleteDownloadedFile = deleteDownloadedFile;
        this.mDownloadFileDeleter = downloadFileDeleter;
        this.mDownloadTaskPauseable = downloadTaskPauseable;
    }

    /**
     * set OnDeleteDownloadFilesListener
     *
     * @param onDeleteDownloadFilesListener OnDeleteDownloadFilesListener
     */
    public void setOnDeleteDownloadFilesListener(OnDeleteDownloadFilesListener onDeleteDownloadFilesListener) {
        this.mOnDeleteDownloadFilesListener = onDeleteDownloadFilesListener;
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
            mDownloadFilesNeedDelete.clear();
            mDownloadFilesDeleted.clear();
            mDownloadFilesSkip.clear();

            for (String url : mUrls) {
                if (!UrlUtil.isUrl(url)) {
                    continue;
                }
                DownloadFileInfo downloadFileInfo = getDownloadFile(url);
                if (downloadFileInfo != null) {
                    //                    synchronized (mLock) {//lock
                    mDownloadFilesNeedDelete.add(downloadFileInfo);
                    //                    }
                }
            }

            // prepare to delete
            notifyDeleteDownloadFilesPrepared();

            // delete every single download file listener
            final OnDeleteDownloadFileListener onDeleteEverySingleDownloadFileListener = new
                    OnDeleteSingleDownloadFileListener();

            // delete every single one
            for (int i = 0; i < mDownloadFilesNeedDelete.size(); i++) {

                DownloadFileInfo downloadFileInfo = mDownloadFilesNeedDelete.get(i);
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

                    Log.d(TAG, TAG + ".run 批量删除任务被取消，无法继续删除，任务即将结束");

                    // goto finally, notifyDeleteDownloadFilesCompleted()
                    return;
                }

                // downloading
                if (mDownloadTaskPauseable.isDownloading(finalUrl)) {

                    Log.d(TAG, TAG + ".run 需要先暂停单个下载任务后删除，url:" + finalUrl);

                    // pause first
                    mDownloadTaskPauseable.pause(finalUrl, new OnStopFileDownloadTaskListener() {
                        @Override
                        public void onStopFileDownloadTaskSucceed(String url) {

                            Log.d(TAG, TAG + ".run 暂停单个下载任务成功，开始删除，url:" + finalUrl);

                            // if the task stopped,notify completed
                            if (isStopped()) {

                                Log.d(TAG, TAG + ".run 批量删除任务被取消，无法继续删除，任务即将结束");

                                // notify caller
                                notifyDeleteDownloadFilesCompleted();
                                return;
                            }

                            // stopped, continue delete
                            runSingleDeleteTask(finalUrl, onDeleteEverySingleDownloadFileListener, false);
                        }

                        @Override
                        public void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason
                                failReason) {

                            // if the task stopped, notify completed
                            if (isStopped()) {

                                Log.d(TAG, TAG + ".run 批量删除任务被取消，无法继续删除，任务即将结束");

                                // notify caller
                                notifyDeleteDownloadFilesCompleted();
                                return;
                            }

                            if (failReason != null) {
                                if (StopDownloadFileTaskFailReason.TYPE_TASK_HAS_BEEN_STOPPED.equals(failReason
                                        .getType())) {
                                    // stopped, continue delete
                                    runSingleDeleteTask(finalUrl, onDeleteEverySingleDownloadFileListener, false);
                                    return;
                                }
                            }

                            Log.d(TAG, TAG + ".run 暂停单个下载任务失败，无法删除，url:" + finalUrl);

                            // failed
                            synchronized (mLock) {// lock
                                mDownloadFilesSkip.add(getDownloadFile(finalUrl));
                            }
                        }
                    });
                } else {
                    // run delete task directly
                    runSingleDeleteTask(finalUrl, onDeleteEverySingleDownloadFileListener, true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // error, ignore
        } finally {

            if (isStopped()) {
                // notify caller
                notifyDeleteDownloadFilesCompleted();
            }

            Log.d(TAG, TAG + ".run 批量删除文件主任务【已结束】，但是通过暂停下载中的文件删除任务可能还没有结束");
        }
    }

    /**
     * run delete single file task
     */
    private void runSingleDeleteTask(String url, OnDeleteDownloadFileListener
            onDeleteEverySingleDownloadFileListener, boolean sync) {

        // init single delete task
        DeleteDownloadFileTask deleteSingleDownloadFileTask = new DeleteDownloadFileTask(url, mDeleteDownloadedFile,
                mDownloadFileDeleter);
        deleteSingleDownloadFileTask.enableSyncCallback();// sync callback
        deleteSingleDownloadFileTask.setOnDeleteDownloadFileListener(onDeleteEverySingleDownloadFileListener);

        if (sync) {
            // run single delete task
            deleteSingleDownloadFileTask.run();
        } else {
            // run async
            mTaskEngine.execute(deleteSingleDownloadFileTask);
        }
    }

    /**
     * single delete callback
     */
    private class OnDeleteSingleDownloadFileListener implements OnDeleteDownloadFileListener {

        @Override
        public void onDeleteDownloadFilePrepared(DownloadFileInfo downloadFileNeedDelete) {

            // notify callback
            notifyDeletingDownloadFiles(downloadFileNeedDelete);
        }

        @Override
        public void onDeleteDownloadFileSuccess(DownloadFileInfo downloadFileDeleted) {

            String url = null;
            if (downloadFileDeleted != null) {
                url = downloadFileDeleted.getUrl();
            }

            synchronized (mLock) {// lock
                // delete succeed
                mDownloadFilesDeleted.add(downloadFileDeleted);
            }

            Log.d(TAG, TAG + ".run 删除单个成功，已删除数量：" + mDownloadFilesDeleted.size() + "，总共需要删除数量" +
                    mDownloadFilesNeedDelete.size() + "，url：" + url);

            if (mDownloadFilesDeleted.size() + mDownloadFilesSkip.size() == mDownloadFilesNeedDelete.size()) {
                // complete, notify caller
                notifyDeleteDownloadFilesCompleted();
            }
        }

        @Override
        public void onDeleteDownloadFileFailed(DownloadFileInfo downloadFileInfo, DeleteDownloadFileFailReason
                failReason) {

            String url = null;
            if (downloadFileInfo != null) {
                url = downloadFileInfo.getUrl();
            }

            String msg = null;
            if (failReason != null) {
                msg = failReason.getMessage();
            }

            Log.d(TAG, TAG + ".run 删除单个成功，已删除数量：" + mDownloadFilesDeleted.size() + "，总共需要删除数量" +
                    mDownloadFilesNeedDelete.size() + "，失败原因：" + msg + "，url：" + url);

            synchronized (mLock) {// lock
                // delete failed
                mDownloadFilesSkip.add(downloadFileInfo);
            }

            if (mDownloadFilesDeleted.size() + mDownloadFilesSkip.size() == mDownloadFilesNeedDelete.size()) {
                // complete, notify caller
                notifyDeleteDownloadFilesCompleted();
            }
        }
    }

    // --------------------------------------notify caller--------------------------------------

    /**
     * notifyDeleteDownloadFilesPrepared
     */
    private void notifyDeleteDownloadFilesPrepared() {

        Log.d(TAG, TAG + ".run 准备批量删除，大小：" + mDownloadFilesNeedDelete.size());

        OnDeleteDownloadFilesListener.MainThreadHelper.onDeleteDownloadFilesPrepared(mDownloadFilesNeedDelete,
                mOnDeleteDownloadFilesListener);
    }

    /**
     * notifyDeletingDownloadFiles
     */
    private void notifyDeletingDownloadFiles(DownloadFileInfo downloadFileInfo) {

        String url = null;
        if (downloadFileInfo != null) {
            url = downloadFileInfo.getUrl();
        }

        Log.d(TAG, TAG + ".run 准备删除单个，url：" + url);

        OnDeleteDownloadFilesListener.MainThreadHelper.onDeletingDownloadFiles(mDownloadFilesNeedDelete,
                mDownloadFilesDeleted, mDownloadFilesSkip, downloadFileInfo, mOnDeleteDownloadFilesListener);
    }

    /**
     * notifyDeleteDownloadFilesCompleted
     */
    private void notifyDeleteDownloadFilesCompleted() {
        if (mIsNotifyFinish.get()) {
            return;
        }

        if (mIsNotifyFinish.compareAndSet(false, true)) {

            OnDeleteDownloadFilesListener.MainThreadHelper.onDeleteDownloadFilesCompleted(mDownloadFilesNeedDelete,
                    mDownloadFilesDeleted, mOnDeleteDownloadFilesListener);

            mIsStop = true;// the task is finish

            int failedSize = mDownloadFilesNeedDelete.size() - mDownloadFilesDeleted.size();

            Log.d(TAG, TAG + ".run 批量删除文件主任务和其它相关任务全部【已结束】，总共需要删除：" + mDownloadFilesNeedDelete.size() + "，已删除：" +
                    mDownloadFilesDeleted.size() + "，失败：" + failedSize + "，跳过：" + mDownloadFilesSkip.size() +
                    "，跳过数量是否等于失败数量：" + (failedSize == mDownloadFilesSkip.size()));
        }
    }
}
