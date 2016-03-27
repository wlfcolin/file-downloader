package org.wlf.filedownloader.file_rename;

import android.text.TextUtils;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener.OnRenameDownloadFileFailReason;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener.RenameDownloadFileFailReason;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.DownloadFileUtil;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RenameDownloadFileTask
 * <br/>
 * 重命名文件任务
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
class RenameDownloadFileTask implements Runnable {

    private static final String TAG = RenameDownloadFileTask.class.getSimpleName();

    private String mUrl;
    private String mNewFileName;
    private boolean includedSuffix;
    private DownloadFileRenamer mDownloadFileRenamer;

    private AtomicBoolean mIsNotifyFinish = new AtomicBoolean(false);

    private OnRenameDownloadFileListener mOnRenameDownloadFileListener;

    public RenameDownloadFileTask(String url, String newFileName, boolean includedSuffix, DownloadFileRenamer
            downloadFileRenamer) {
        super();
        this.mUrl = url;
        this.mNewFileName = newFileName;
        this.includedSuffix = includedSuffix;
        this.mDownloadFileRenamer = downloadFileRenamer;
    }

    /**
     * set OnRenameDownloadFileListener
     *
     * @param onRenameDownloadFileListener OnRenameDownloadFileListener
     */
    public void setOnRenameDownloadFileListener(OnRenameDownloadFileListener onRenameDownloadFileListener) {
        this.mOnRenameDownloadFileListener = onRenameDownloadFileListener;
    }

    // --------------------------------------run the task--------------------------------------

    @Override
    public void run() {

        /**
         * rename logic
         *
         * 1.if downloading, pause(check by rename manager), make sure the download status is right
         * 2.check illegal conditions such as file system not mount, download file not exist and so on
         * 3.backup save file name
         * 4.rename save file name in database
         * 5.check and rename save file in the file system
         * 6.if step 5 succeed, finish, otherwise, rollback the temp file name and save file name in database that 
         * backup in step 3
         */

        DownloadFileInfo downloadFileInfo = null;
        RenameDownloadFileFailReason failReason = null;

        try {

            downloadFileInfo = mDownloadFileRenamer.getDownloadFile(mUrl);

            // ------------start checking conditions------------

            if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
                failReason = new OnRenameDownloadFileFailReason(mUrl, "the download file is not exist !",
                        OnRenameDownloadFileFailReason.TYPE_FILE_RECORD_IS_NOT_EXIST);
                // goto finally, notifyFailed()
                return;
            }

            // 1.prepared
            notifyPrepared(downloadFileInfo);

            // check status
            if (!DownloadFileUtil.canRename(downloadFileInfo)) {

                failReason = new OnRenameDownloadFileFailReason(mUrl, "the download file status error !",
                        OnRenameDownloadFileFailReason.TYPE_FILE_STATUS_ERROR);
                // goto finally, notifyFailed()
                return;
            }

            String dirPath = downloadFileInfo.getFileDir();
            String oldFileName = downloadFileInfo.getFileName();

            String suffix = "";
            if (oldFileName != null && oldFileName.contains(".")) {
                int index = oldFileName.lastIndexOf(".");
                if (index != -1) {
                    suffix = oldFileName.substring(index, oldFileName.length());
                }
            }

            if (!includedSuffix) {
                mNewFileName = mNewFileName + suffix;
            }


            File newFile = new File(dirPath, mNewFileName);

            if (TextUtils.isEmpty(mNewFileName)) {
                failReason = new OnRenameDownloadFileFailReason(mUrl, "new file name is empty !",
                        OnRenameDownloadFileFailReason.TYPE_NEW_FILE_NAME_IS_EMPTY);
                // goto finally, notifyFailed()
                return;
            }

            if (checkNewFileExist(newFile)) {
                failReason = new OnRenameDownloadFileFailReason(mUrl, "the new file has been exist !",
                        OnRenameDownloadFileFailReason.TYPE_NEW_FILE_HAS_BEEN_EXIST);
                // goto finally, notifyFailed()
                return;
            }
            // ------------end checking conditions------------

            boolean renameResult = false;

            // write to db
            try {
                mDownloadFileRenamer.renameDownloadFile(downloadFileInfo.getUrl(), mNewFileName);
                renameResult = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!renameResult) {
                failReason = new OnRenameDownloadFileFailReason(mUrl, "rename file in db failed !",
                        OnRenameDownloadFileFailReason.TYPE_UNKNOWN);
                // goto finally, notifyFailed()
                return;
            }

            // rename db record succeed

            // need rename save file
            if (DownloadFileUtil.isCompleted(downloadFileInfo)) {
                File oldSaveFile = new File(dirPath, oldFileName);
                // finished download and file exists
                if (oldSaveFile.exists()) {
                    // success, rename save file
                    renameResult = oldSaveFile.renameTo(newFile);
                } else {
                    renameResult = false;
                    failReason = new OnRenameDownloadFileFailReason(mUrl, "the original file not exist !",
                            OnRenameDownloadFileFailReason.TYPE_ORIGINAL_FILE_NOT_EXIST);
                }

                if (!renameResult) {
                    // rollback in db
                    try {
                        mDownloadFileRenamer.renameDownloadFile(downloadFileInfo.getUrl(), oldFileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        // try again
                        try {
                            mDownloadFileRenamer.renameDownloadFile(downloadFileInfo.getUrl(), oldFileName);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                            // ignore   
                        }
                    }

                    if (failReason == null) {
                        failReason = new OnRenameDownloadFileFailReason(mUrl, "rename file in file system failed !",
                                OnRenameDownloadFileFailReason.TYPE_UNKNOWN);
                    }
                    // goto finally, notifyFailed()
                    return;
                }

                // rename save file succeed
            }

            // rename succeed
        } catch (Exception e) {
            e.printStackTrace();
            failReason = new OnRenameDownloadFileFailReason(mUrl, e);
        } finally {
            // ------------start notifying caller------------
            {
                // rename succeed
                if (failReason == null) {
                    // 2.rename success
                    notifySuccess(downloadFileInfo);

                    Log.d(TAG, TAG + ".run 重命名成功，url：" + mUrl);
                } else {
                    // 2.rename failed
                    notifyFailed(downloadFileInfo, failReason);

                    Log.d(TAG, TAG + ".run 重命名失败，url：" + mUrl + ",failReason:" + failReason.getType());
                }
            }
            // ------------end notifying caller------------

            Log.d(TAG, TAG + ".run 重命名任务【已结束】，是否有异常：" + (failReason == null) + "，url：" + mUrl);
        }
    }

    /**
     * check new file whether exist
     */
    private boolean checkNewFileExist(File newFile) {
        if (newFile != null && newFile.exists()) {// the file has been exist
            return true;
        }

        List<DownloadFileInfo> downloadFileInfos = mDownloadFileRenamer.getDownloadFiles();
        if (!CollectionUtil.isEmpty(downloadFileInfos)) {
            for (DownloadFileInfo info : downloadFileInfos) {
                if (info == null) {
                    continue;
                }
                String path = info.getFilePath();
                if (TextUtils.isEmpty(path)) {
                    continue;
                }
                if (path.equals(newFile.getAbsolutePath())) {// the file has been exist
                    return true;
                }
            }
        }
        return false;
    }

    // --------------------------------------notify caller--------------------------------------

    /**
     * notifyPrepared
     */
    private void notifyPrepared(DownloadFileInfo downloadFileInfo) {
        if (mOnRenameDownloadFileListener == null) {
            return;
        }
        OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFilePrepared(downloadFileInfo,
                mOnRenameDownloadFileListener);
    }

    /**
     * notifySuccess
     */
    private void notifySuccess(DownloadFileInfo downloadFileInfo) {
        if (mIsNotifyFinish.get()) {
            return;
        }
        if (mIsNotifyFinish.compareAndSet(false, true)) {
            if (mOnRenameDownloadFileListener != null) {
                OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileSuccess(downloadFileInfo,
                        mOnRenameDownloadFileListener);
            }
        }
    }

    /**
     * notifyFailed
     */
    private void notifyFailed(DownloadFileInfo downloadFileInfo, RenameDownloadFileFailReason failReason) {
        if (mIsNotifyFinish.get()) {
            return;
        }
        if (mIsNotifyFinish.compareAndSet(false, true)) {
            if (mOnRenameDownloadFileListener != null) {
                OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileFailed(downloadFileInfo,
                        failReason, mOnRenameDownloadFileListener);
            }
        }
    }

}
