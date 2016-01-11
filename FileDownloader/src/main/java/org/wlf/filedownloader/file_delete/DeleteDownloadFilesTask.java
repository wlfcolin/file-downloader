package org.wlf.filedownloader.file_delete;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.base.Stoppable;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener;
import org.wlf.filedownloader.listener.OnDeleteDownloadFilesListener;
import org.wlf.filedownloader.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * DeleteDownloadFilesTask
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
class DeleteDownloadFilesTask implements Runnable, Stoppable {

    /**
     * LOG TAG
     */
    private static final String TAG = DeleteDownloadFilesTask.class.getSimpleName();

    private List<String> mUrls;
    private boolean mDeleteDownloadedFile;
    private DownloadFileDeleter mDownloadFileDeleter;

    private OnDeleteDownloadFilesListener mOnDeleteDownloadFilesListener;

    private boolean mIsStop = false;
    private boolean mIsNotifyFinish = false;

    private Object mLock = new Object();// lock

    private final List<DownloadFileInfo> mDownloadFilesNeedDelete = new ArrayList<DownloadFileInfo>();
    final List<DownloadFileInfo> mDownloadFilesDeleted = new ArrayList<DownloadFileInfo>();
    final List<DownloadFileInfo> mDownloadFilesSkip = new ArrayList<DownloadFileInfo>();

    public DeleteDownloadFilesTask(List<String> urls, boolean deleteDownloadedFile, DownloadFileDeleter 
            downloadFileDeleter) {
        super();
        this.mUrls = urls;
        this.mDeleteDownloadedFile = deleteDownloadedFile;
        this.mDownloadFileDeleter = downloadFileDeleter;
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
            OnDeleteDownloadFileListener onDeleteEverySingleDownloadFileListener = new 
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

                String url = downloadFileInfo.getUrl();

                // if the task stopped,notify completed
                if (isStopped()) {

                    Log.d(TAG, TAG + ".run 任务被取消，无法继续删除，任务即将结束");

                    // goto finally, notifyDeleteDownloadFilesCompleted()
                    return;
                }

                // init single delete task
                DeleteDownloadFileTask deleteSingleDownloadFileTask = new DeleteDownloadFileTask(url, 
                        mDeleteDownloadedFile, mDownloadFileDeleter);
                deleteSingleDownloadFileTask.enableSyncCallback();// sync callback
                deleteSingleDownloadFileTask.setOnDeleteDownloadFileListener(onDeleteEverySingleDownloadFileListener);

                // run single delete task
                deleteSingleDownloadFileTask.run();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // error, ignore
        } finally {
            // notify caller
            notifyDeleteDownloadFilesCompleted();

            Log.d(TAG, TAG + ".run 批量删除文件任务【已结束】");

            mIsStop = true;// the task is finish
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
        }
    }

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
        if (mIsNotifyFinish) {
            return;
        }

        int failedSize = mDownloadFilesNeedDelete.size() - mDownloadFilesDeleted.size();

        Log.d(TAG, TAG + ".run 批量删除完成，总共需要删除：" + mDownloadFilesNeedDelete.size() + "，已删除：" + mDownloadFilesDeleted
                .size() + "，失败：" + failedSize + "，跳过：" + mDownloadFilesSkip.size() + "，跳过数量是否等于失败数量：" + (failedSize 
                == mDownloadFilesSkip.size()));

        OnDeleteDownloadFilesListener.MainThreadHelper.onDeleteDownloadFilesCompleted(mDownloadFilesNeedDelete, 
                mDownloadFilesDeleted, mOnDeleteDownloadFilesListener);
        mIsNotifyFinish = true;
    }
}
