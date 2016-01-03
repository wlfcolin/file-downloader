package org.wlf.filedownloader.file_delete;

import android.util.Log;

import org.wlf.filedownloader.DownloadCacher;
import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Control;
import org.wlf.filedownloader.base.Stoppable;
import org.wlf.filedownloader.file_download.DownloadTaskManager;
import org.wlf.filedownloader.file_download.FileDownloadTask.OnStopDownloadFileTaskFailReason;
import org.wlf.filedownloader.file_download.FileDownloadTask.OnStopFileDownloadTaskListener;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener.DeleteDownloadFileFailReason;
import org.wlf.filedownloader.listener.OnDeleteDownloadFilesListener;
import org.wlf.filedownloader.util.UrlUtil;

import java.util.ArrayList;
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
    private ExecutorService mSupportEngine;
    /**
     * DownloadFileCacher,to storage download files
     */
    private DownloadCacher mDownloadFileCacher;
    /**
     * DownloadTaskManager,to manage download tasks
     */
    private DownloadTaskManager mDownloadTaskManager;
    /**
     * the task for delete multi DownloadFiles,to delete download files
     */
    private DeleteDownloadFilesTask mDeleteDownloadFilesTask;

    public DownloadDeleteManager(ExecutorService supportEngine, DownloadCacher downloadFileCacher, 
                                 DownloadTaskManager downloadTaskManager) {
        mSupportEngine = supportEngine;
        mDownloadFileCacher = downloadFileCacher;
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
        return mDownloadFileCacher.getDownloadFile(url);
    }

    /**
     * delete download
     */
    private void deleteInternal(String url, boolean deleteDownloadedFileInPath, OnDeleteDownloadFileListener 
            onDeleteDownloadFileListener, boolean isSyncCallback) {
        // create delete download task
        DeleteDownloadFileTask deleteDownloadFileTask = new DeleteDownloadFileTask(url, deleteDownloadedFileInPath, 
                mDownloadFileCacher);
        if (isSyncCallback) {
            deleteDownloadFileTask.enableSyncCallback();
        }
        deleteDownloadFileTask.setOnDeleteDownloadFileListener(onDeleteDownloadFileListener);
        // run task
        addAndRunTask(deleteDownloadFileTask);
    }

    private void delete(String url, final boolean deleteDownloadedFileInPath, final OnDeleteDownloadFileListener 
            onDeleteDownloadFileListener, final boolean isSyncCallback) {

        final DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo == null) {

            Log.d(TAG, "delete 文件不存在,url:" + url);

            // the DownloadFile does not exist
            if (onDeleteDownloadFileListener != null) {
                onDeleteDownloadFileListener.onDeleteDownloadFileFailed(downloadFileInfo, new 
                        DeleteDownloadFileFailReason("the download file doest not exist!", 
                        DeleteDownloadFileFailReason.TYPE_FILE_RECORD_IS_NOT_EXIST));
            }
            return;
        }

        if (!mDownloadTaskManager.isInFileDownloadTaskMap(url)) {

            Log.d(TAG, "delete 直接删除,url:" + url);

            deleteInternal(url, deleteDownloadedFileInPath, onDeleteDownloadFileListener, isSyncCallback);
        } else {

            Log.d(TAG, "delete 需要先暂停后删除,url:" + url);

            // pause
            mDownloadTaskManager.pause(url, new OnStopFileDownloadTaskListener() {
                @Override
                public void onStopFileDownloadTaskSucceed(String url) {

                    Log.d(TAG, "delete 暂停成功，开始删除,url:" + url);

                    deleteInternal(url, deleteDownloadedFileInPath, onDeleteDownloadFileListener, isSyncCallback);
                }

                @Override
                public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {

                    Log.d(TAG, "delete 暂停失败，无法删除,url:" + url);

                    if (onDeleteDownloadFileListener != null) {
                        OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFileFailed(downloadFileInfo, 
                                new DeleteDownloadFileFailReason(failReason), onDeleteDownloadFileListener);
                    }
                }
            });
        }
    }

    /**
     * delete download
     *
     * @param url                          file url
     * @param deleteDownloadedFileInPath   whether delete file in path
     * @param onDeleteDownloadFileListener DeleteDownloadFileListener
     */
    public void delete(String url, boolean deleteDownloadedFileInPath, OnDeleteDownloadFileListener onDeleteDownloadFileListener) {
        delete(url, deleteDownloadedFileInPath, onDeleteDownloadFileListener, false);
    }

    /**
     * delete multi downloads
     *
     * @param urls                          file mUrls
     * @param deleteDownloadedFile          whether delete file in path
     * @param onDeleteDownloadFilesListener DeleteDownloadFilesListener
     * @return the control for the operation
     */
    public Control delete(List<String> urls, boolean deleteDownloadedFile, OnDeleteDownloadFilesListener onDeleteDownloadFilesListener) {

        if (mDeleteDownloadFilesTask != null && !mDeleteDownloadFilesTask.isStopped()) {
            // deleting
            return new DeleteControl(mDeleteDownloadFilesTask);
        }

        DeleteDownloadFilesTask deleteDownloadFilesTask = new DeleteDownloadFilesTask(urls, deleteDownloadedFile);
        deleteDownloadFilesTask.setOnDeleteDownloadFilesListener(onDeleteDownloadFilesListener);

        // start task
        addAndRunTask(deleteDownloadFilesTask);
        this.mDeleteDownloadFilesTask = deleteDownloadFilesTask;
        return new DeleteControl(this.mDeleteDownloadFilesTask);
    }


    /**
     * delete multi download task
     */
    private class DeleteDownloadFilesTask implements Runnable, Stoppable {

        private List<String> mUrls;
        private boolean mDeleteDownloadedFile;
        private OnDeleteDownloadFilesListener mOnDeleteDownloadFilesListener;

        private boolean mIsStop = false;
        private boolean mCompleted = false;

        private final List<DownloadFileInfo> mDownloadFilesNeedDelete = new ArrayList<DownloadFileInfo>();
        final List<DownloadFileInfo> mDownloadFilesDeleted = new ArrayList<DownloadFileInfo>();
        final List<DownloadFileInfo> mDownloadFilesSkip = new ArrayList<DownloadFileInfo>();

        public DeleteDownloadFilesTask(List<String> urls, boolean deleteDownloadedFile) {
            super();
            this.mUrls = urls;
            this.mDeleteDownloadedFile = deleteDownloadedFile;
        }

        public void setOnDeleteDownloadFilesListener(OnDeleteDownloadFilesListener onDeleteDownloadFilesListener) {
            this.mOnDeleteDownloadFilesListener = onDeleteDownloadFilesListener;
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

            for (String url : mUrls) {
                if (!UrlUtil.isUrl(url)) {
                    continue;
                }
                DownloadFileInfo downloadFileInfo = getDownloadFile(url);
                if (downloadFileInfo != null) {
                    mDownloadFilesNeedDelete.add(downloadFileInfo);
                }
            }

            // prepare to delete
            if (mOnDeleteDownloadFilesListener != null) {

                Log.d(TAG, "DeleteDownloadFilesTask.run 准备批量删除，大小：" + mDownloadFilesNeedDelete.size());

                OnDeleteDownloadFilesListener.MainThreadHelper.onDeleteDownloadFilePrepared(mDownloadFilesNeedDelete,
                        mOnDeleteDownloadFilesListener);
            }

            OnDeleteDownloadFileListener onDeleteDownloadFileListener = new OnDeleteDownloadFileListener() {

                private int deleteCount = 0;

                @Override
                public void onDeleteDownloadFilePrepared(DownloadFileInfo downloadFileNeedDelete) {

                    String url = null;
                    if (downloadFileNeedDelete != null) {
                        url = downloadFileNeedDelete.getUrl();
                    }

                    Log.d(TAG, "DeleteDownloadFilesTask.run 准备删除，url：" + url);

                    // start new delete
                    if (mOnDeleteDownloadFilesListener != null) {
                        OnDeleteDownloadFilesListener.MainThreadHelper.onDeletingDownloadFiles
                                (mDownloadFilesNeedDelete, mDownloadFilesDeleted, mDownloadFilesSkip, 
                                        downloadFileNeedDelete, mOnDeleteDownloadFilesListener);
                    }

                    deleteCount++;
                }

                @Override
                public void onDeleteDownloadFileSuccess(DownloadFileInfo downloadFileDeleted) {

                    String url = null;
                    if (downloadFileDeleted != null) {
                        url = downloadFileDeleted.getUrl();
                    }

                    Log.d(TAG, "DeleteDownloadFilesTask.run onDeleteDownloadFileSuccess,删除成功，deleteCount：" +
                            deleteCount + ",mDownloadFilesNeedDelete.size():" + mDownloadFilesNeedDelete.size() +
                            "，url：" + url);

                    // delete succeed
                    mDownloadFilesDeleted.add(downloadFileDeleted);

                    // if the last one to delete,notify finish the operation
                    if (deleteCount == mDownloadFilesNeedDelete.size() - mDownloadFilesSkip.size()) {

                        Log.d(TAG, "DeleteDownloadFilesTask.run onDeleteDownloadFileSuccess," + 
                                "删除成功，回调onDeleteDownloadFilesCompleted");

                        onDeleteDownloadFilesCompleted();
                    }
                }

                @Override
                public void onDeleteDownloadFileFailed(DownloadFileInfo downloadFileInfo, DeleteDownloadFileFailReason failReason) {

                    String url = null;
                    if (downloadFileInfo != null) {
                        url = downloadFileInfo.getUrl();
                    }

                    String type = null;
                    if (failReason != null) {
                        type = failReason.getType();
                    }

                    Log.d(TAG, "DeleteDownloadFilesTask.run onDeleteDownloadFileFailed,删除失败，deleteCount：" +
                            deleteCount + ",mDownloadFilesNeedDelete.size():" + mDownloadFilesNeedDelete.size() +
                            "，url：" + url + ",failReason:" + type);

                    // delete failed
                    mDownloadFilesSkip.add(downloadFileInfo);

                    // if the last one to delete,notify finish the operation
                    if (deleteCount == mDownloadFilesNeedDelete.size() - mDownloadFilesSkip.size()) {

                        Log.d(TAG, "DeleteDownloadFilesTask.run onDeleteDownloadFileFailed," + 
                                "删除失败，回调onDeleteDownloadFilesCompleted");

                        onDeleteDownloadFilesCompleted();
                    }
                }
            };

            // delete every single one
            for (int i = 0; i < mDownloadFilesNeedDelete.size(); i++) {

                DownloadFileInfo downloadFileInfo = mDownloadFilesNeedDelete.get(i);
                if (downloadFileInfo == null) {
                    continue;
                }

                String url = downloadFileInfo.getUrl();

                // if the task stopped,notify completed
                if (isStopped()) {

                    Log.d(TAG, "DeleteDownloadFilesTask.run task has been sopped," + 
                            "任务已经被取消，无法继续删除，回调onDeleteDownloadFilesCompleted");

                    onDeleteDownloadFilesCompleted();
                }

                // deleting
                delete(url, mDeleteDownloadedFile, onDeleteDownloadFileListener, true);
            }

            mIsStop = true;// because is sync delete,so at this time,the task is finish
        }

        // on delete finish
        private void onDeleteDownloadFilesCompleted() {
            if (mCompleted) {
                return;
            }
            if (mOnDeleteDownloadFilesListener != null) {
                OnDeleteDownloadFilesListener.MainThreadHelper.onDeleteDownloadFilesCompleted
                        (mDownloadFilesNeedDelete, mDownloadFilesDeleted, mOnDeleteDownloadFilesListener);
            }
            mCompleted = true;
            mIsStop = true;
        }
    }

    static class DeleteControl implements Control {

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
