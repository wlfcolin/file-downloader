package org.wlf.filedownloader.file_rename;

import android.text.TextUtils;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.file_download.db_recorder.DownloadFileDbRecorder;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener.RenameDownloadFileFailReason;
import org.wlf.filedownloader.util.CollectionUtil;

import java.io.File;
import java.util.List;

/**
 * RenameDownloadFileTask
 * <br/>
 * 重命名文件任务
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class RenameDownloadFileTask implements Runnable {

    private static final String TAG = RenameDownloadFileTask.class.getSimpleName();

    private String mUrl;
    private String mNewFileName;
    private boolean includedSuffix;
    private DownloadFileRenamer mDownloadFileRenamer;

    private OnRenameDownloadFileListener mOnRenameDownloadFileListener;

    public RenameDownloadFileTask(String url, String newFileName, boolean includedSuffix, DownloadFileRenamer 
            downloadFileRenamer) {
        super();
        this.mUrl = url;
        this.mNewFileName = newFileName;
        this.includedSuffix = includedSuffix;
        this.mDownloadFileRenamer = downloadFileRenamer;
    }

    public void setOnRenameDownloadFileListener(OnRenameDownloadFileListener onRenameDownloadFileListener) {
        this.mOnRenameDownloadFileListener = onRenameDownloadFileListener;
    }

    @Override
    public void run() {

        DownloadFileInfo downloadFileInfo = mDownloadFileRenamer.getDownloadFile(mUrl);

        // 1.prepared
        OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFilePrepared(downloadFileInfo, 
                mOnRenameDownloadFileListener);

        if (downloadFileInfo != null) {

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

            File file = new File(dirPath, oldFileName);
            File newFile = new File(dirPath, mNewFileName);

            switch (downloadFileInfo.getStatus()) {
                // completed,need to rename save file
                case Status.DOWNLOAD_STATUS_COMPLETED:
                    try {
                        if (!file.exists()) {
                            throw new RenameDownloadFileFailReason("the original file not exist!", 
                                    RenameDownloadFileFailReason.TYPE_ORIGINAL_FILE_NOT_EXIST);
                        }

                        if (TextUtils.isEmpty(mNewFileName)) {
                            throw new RenameDownloadFileFailReason("new file name is empty!", 
                                    RenameDownloadFileFailReason.TYPE_NEW_FILE_NAME_IS_EMPTY);
                        }

                        if (checkNewFileExist(newFile)) {
                            throw new RenameDownloadFileFailReason("the new file has been exist!", 
                                    RenameDownloadFileFailReason.TYPE_NEW_FILE_HAS_BEEN_EXIST);
                        }

                        boolean isSucceed = file.renameTo(newFile);

                        if (!isSucceed) {
                            throw new RenameDownloadFileFailReason("rename file failed!", 
                                    RenameDownloadFileFailReason.TYPE_UNKNOWN);
                        }

                        // success,write to db
                        //                        downloadFileInfo.setFileName(mNewFileName);
                        //                      boolean updateResult = mFileDownloadCacher.updateDownloadFile
                        // (downloadFileInfo);
                        boolean updateResult = false;
                        try {
                            mDownloadFileRenamer.renameDownloadFile(downloadFileInfo.getUrl(), mNewFileName);
                            updateResult = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (updateResult) {
                            // 2.rename success
                            OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileSuccess
                                    (downloadFileInfo, mOnRenameDownloadFileListener);
                            return;
                        } else {
                            throw new RenameDownloadFileFailReason("rename file failed!", 
                                    RenameDownloadFileFailReason.TYPE_UNKNOWN);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();

                        RenameDownloadFileFailReason failReason = null;

                        if (e instanceof RenameDownloadFileFailReason) {
                            failReason = (RenameDownloadFileFailReason) e;
                        } else {
                            failReason = new RenameDownloadFileFailReason(e);
                        }

                        // failed
                        OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileFailed(downloadFileInfo, 
                                failReason, mOnRenameDownloadFileListener);
                    }
                    break;
                // rename db record
                case Status.DOWNLOAD_STATUS_PAUSED:

                    if (checkNewFileExist(newFile)) {
                        // failed
                        OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileFailed(downloadFileInfo, new RenameDownloadFileFailReason("the new file has been exist!", RenameDownloadFileFailReason.TYPE_NEW_FILE_HAS_BEEN_EXIST), mOnRenameDownloadFileListener);
                        return;
                    }

                    // write to db
                    // downloadFileInfo.setFileName(mNewFileName);
                    // boolean updateResult = mFileDownloadCacher.updateDownloadFile(downloadFileInfo);
                    boolean updateResult = false;
                    try {
                        mDownloadFileRenamer.renameDownloadFile(downloadFileInfo.getUrl(), mNewFileName);
                        updateResult = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (updateResult) {
                        // 2.rename success
                        OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileSuccess(downloadFileInfo, 
                                mOnRenameDownloadFileListener);
                        return;
                    } else {
                        // failed
                        OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileFailed(downloadFileInfo, new RenameDownloadFileFailReason("rename file failed!", RenameDownloadFileFailReason.TYPE_UNKNOWN), mOnRenameDownloadFileListener);
                    }
                    break;
                default:
                    // status error
                    OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileFailed(downloadFileInfo, new RenameDownloadFileFailReason("DownloadFile status error!", RenameDownloadFileFailReason.TYPE_FILE_STATUS_ERROR), mOnRenameDownloadFileListener);
                    return;
            }
        } else {
            // failed
            OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileFailed(downloadFileInfo, new 
                    RenameDownloadFileFailReason("the download file is not exist!", RenameDownloadFileFailReason
                    .TYPE_FILE_RECORD_IS_NOT_EXIST), mOnRenameDownloadFileListener);
        }
    }

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

    /**
     * DownloadFile Renamer
     */
    public interface DownloadFileRenamer extends DownloadFileDbRecorder {

        /**
         * rename download file name
         *
         * @param url         download url
         * @param newFileName new file name
         * @throws Exception any exception during rename
         */
        void renameDownloadFile(String url, String newFileName) throws Exception;
    }

}
