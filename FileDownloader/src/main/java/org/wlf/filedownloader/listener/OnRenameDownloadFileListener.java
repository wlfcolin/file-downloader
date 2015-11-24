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
    void onRenameDownloadFileFailed(DownloadFileInfo downloadFileInfo, OnRenameDownloadFileFailReason failReason);


    /**
     * Callback helper for main thread
     */
    public static class MainThreadHelper {

        /**
         * prepared rename
         *
         * @param downloadFileNeedRename download file needed to rename
         */
        public static void onRenameDownloadFilePrepared(final DownloadFileInfo downloadFileNeedRename, final OnRenameDownloadFileListener onRenameDownloadFileListener) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onRenameDownloadFileListener.onRenameDownloadFilePrepared(downloadFileNeedRename);
                    handler.removeCallbacksAndMessages(null);
                }
            });
        }

        /**
         * rename succeed
         *
         * @param downloadFileRenamed download files renamed
         */
        public static void onRenameDownloadFileSuccess(final DownloadFileInfo downloadFileRenamed, final OnRenameDownloadFileListener onRenameDownloadFileListener) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onRenameDownloadFileListener.onRenameDownloadFileSuccess(downloadFileRenamed);
                    handler.removeCallbacksAndMessages(null);
                }
            });
        }

        /**
         * rename failed
         *
         * @param downloadFileInfo download files needed to rename
         * @param failReason       fail reason
         */
        public static void onRenameDownloadFileFailed(final DownloadFileInfo downloadFileInfo, final OnRenameDownloadFileFailReason failReason, final OnRenameDownloadFileListener onRenameDownloadFileListener) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onRenameDownloadFileListener.onRenameDownloadFileFailed(downloadFileInfo, failReason);
                    handler.removeCallbacksAndMessages(null);
                }
            });
        }

    }

    /**
     * OnRenameDownloadFileFailReason
     */
    public static class OnRenameDownloadFileFailReason extends FailReason {

        private static final long serialVersionUID = 4959079784745889291L;


        /**
         * the download file is not exist
         */
        public static final String TYPE_FILE_RECORD_IS_NOT_EXIST = OnRenameDownloadFileFailReason.class.getName() + "_TYPE_FILE_RECORD_IS_NOT_EXIST";
        /**
         * original file not exist
         */
        public static final String TYPE_ORIGINAL_FILE_NOT_EXIST = OnRenameDownloadFileFailReason.class.getName() + "_TYPE_ORIGINAL_FILE_NOT_EXIST";
        /**
         * new file name is empty
         */
        public static final String TYPE_NEW_FILE_NAME_IS_EMPTY = OnRenameDownloadFileFailReason.class.getName() + "_TYPE_NEW_FILE_NAME_IS_EMPTY";
        /**
         * file does not complete download
         */
        public static final String TYPE_FILE_DOES_NOT_COMPLETE_DOWNLOAD = OnRenameDownloadFileFailReason.class.getName() + "_TYPE_FILE_DOES_NOT_COMPLETE_DOWNLOAD";


        public OnRenameDownloadFileFailReason(String detailMessage, String type) {
            super(detailMessage, type);
        }

        public OnRenameDownloadFileFailReason(Throwable throwable) {
            super(throwable);
        }

        @Override
        protected void onInitTypeWithThrowable(Throwable throwable) {
            super.onInitTypeWithThrowable(throwable);
            // TODO
        }
    }
}