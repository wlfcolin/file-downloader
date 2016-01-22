package org.wlf.filedownloader.listener;

import android.os.Handler;
import android.os.Looper;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.FailReason;

/**
 * OnRenameDownloadFileListener
 * <br/>
 * 重命名下载文件监听接口
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public interface OnRenameDownloadFileListener {

    /**
     * prepared rename
     *
     * @param downloadFileNeedRename download file needed to rename
     */
    void onRenameDownloadFilePrepared(DownloadFileInfo downloadFileNeedRename);

    /**
     * rename succeed
     *
     * @param downloadFileRenamed download files renamed
     */
    void onRenameDownloadFileSuccess(DownloadFileInfo downloadFileRenamed);

    /**
     * rename failed
     *
     * @param downloadFileInfo download files needed to rename
     * @param failReason       fail reason
     */
    void onRenameDownloadFileFailed(DownloadFileInfo downloadFileInfo, RenameDownloadFileFailReason failReason);


    /**
     * Callback helper for main thread
     */
    public static class MainThreadHelper {

        /**
         * prepared rename
         *
         * @param downloadFileNeedRename download file needed to rename
         */
        public static void onRenameDownloadFilePrepared(final DownloadFileInfo downloadFileNeedRename, final 
        OnRenameDownloadFileListener onRenameDownloadFileListener) {
            if (onRenameDownloadFileListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onRenameDownloadFileListener == null) {
                        return;
                    }
                    onRenameDownloadFileListener.onRenameDownloadFilePrepared(downloadFileNeedRename);
                }
            });
        }

        /**
         * rename succeed
         *
         * @param downloadFileRenamed download files renamed
         */
        public static void onRenameDownloadFileSuccess(final DownloadFileInfo downloadFileRenamed, final 
        OnRenameDownloadFileListener onRenameDownloadFileListener) {
            if (onRenameDownloadFileListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onRenameDownloadFileListener == null) {
                        return;
                    }
                    onRenameDownloadFileListener.onRenameDownloadFileSuccess(downloadFileRenamed);
                }
            });
        }

        /**
         * rename failed
         *
         * @param downloadFileInfo download files needed to rename
         * @param failReason       fail reason
         */
        public static void onRenameDownloadFileFailed(final DownloadFileInfo downloadFileInfo, final 
        RenameDownloadFileFailReason failReason, final OnRenameDownloadFileListener onRenameDownloadFileListener) {
            if (onRenameDownloadFileListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onRenameDownloadFileListener == null) {
                        return;
                    }
                    onRenameDownloadFileListener.onRenameDownloadFileFailed(downloadFileInfo, failReason);
                }
            });
        }

    }

    /**
     * RenameDownloadFileFailReason
     *
     * @deprecated use {@link RenameDownloadFileFailReason} instead
     */
    @Deprecated
    public static class OnRenameDownloadFileFailReason extends RenameDownloadFileFailReason {

        public OnRenameDownloadFileFailReason(String url, String detailMessage, String type) {
            super(url, detailMessage, type);
        }

        public OnRenameDownloadFileFailReason(String url, Throwable throwable) {
            super(url, throwable);
        }
    }

    /**
     * RenameDownloadFileFailReason
     */
    public static class RenameDownloadFileFailReason extends FailReason {

        /**
         * the download file is not exist
         */
        public static final String TYPE_FILE_RECORD_IS_NOT_EXIST = RenameDownloadFileFailReason.class.getName() + 
                "_TYPE_FILE_RECORD_IS_NOT_EXIST";
        /**
         * original file not exist
         */
        public static final String TYPE_ORIGINAL_FILE_NOT_EXIST = RenameDownloadFileFailReason.class.getName() + 
                "_TYPE_ORIGINAL_FILE_NOT_EXIST";
        /**
         * new file name is empty
         */
        public static final String TYPE_NEW_FILE_NAME_IS_EMPTY = RenameDownloadFileFailReason.class.getName() + 
                "_TYPE_NEW_FILE_NAME_IS_EMPTY";
        /**
         * file status error,can not rename
         */
        public static final String TYPE_FILE_STATUS_ERROR = RenameDownloadFileFailReason.class.getName() + 
                "_TYPE_FILE_STATUS_ERROR";
        /**
         * the new file has been exist
         */
        public static final String TYPE_NEW_FILE_HAS_BEEN_EXIST = RenameDownloadFileFailReason.class.getName() + 
                "_TYPE_NEW_FILE_HAS_BEEN_EXIST";

        public RenameDownloadFileFailReason(String url, String detailMessage, String type) {
            super(detailMessage, type);
        }

        public RenameDownloadFileFailReason(String url, Throwable throwable) {
            super(url, throwable);
        }
    }
}