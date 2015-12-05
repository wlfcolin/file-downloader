package org.wlf.filedownloader.listener;

import android.os.Handler;
import android.os.Looper;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.FailReason;

/**
 * OnMoveDownloadFileListener
 * <br/>
 * 移动下载文件监听接口
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public interface OnMoveDownloadFileListener {

    /**
     * prepared move
     *
     * @param downloadFileNeedToMove download file needed to move
     */
    void onMoveDownloadFilePrepared(DownloadFileInfo downloadFileNeedToMove);

    /**
     * move succeed
     *
     * @param downloadFileMoved download file moved
     */
    void onMoveDownloadFileSuccess(DownloadFileInfo downloadFileMoved);

    /**
     * move failed
     *
     * @param downloadFileInfo download file needed to move,may be null
     * @param failReason       fail reason
     */
    void onMoveDownloadFileFailed(DownloadFileInfo downloadFileInfo, OnMoveDownloadFileFailReason failReason);

    /**
     * OnMoveDownloadFileFailReason
     */
    public static class OnMoveDownloadFileFailReason extends FailReason {

        private static final long serialVersionUID = -5988401984979760118L;

        /**
         * target file exist
         */
        public static final String TYPE_TARGET_FILE_EXIST = OnMoveDownloadFileFailReason.class.getName() + 
                "_TYPE_TARGET_FILE_EXIST";
        /**
         * original file not exist
         */
        public static final String TYPE_ORIGINAL_FILE_NOT_EXIST = OnMoveDownloadFileFailReason.class.getName() + 
                "_TYPE_ORIGINAL_FILE_NOT_EXIST";
        /**
         * update record error
         */
        public static final String TYPE_UPDATE_RECORD_ERROR = OnMoveDownloadFileFailReason.class.getName() + 
                "_TYPE_UPDATE_RECORD_ERROR";
        /**
         * file status error
         */
        public static final String TYPE_FILE_STATUS_ERROR = OnMoveDownloadFileFailReason.class.getName() + 
                "_TYPE_FILE_STATUS_ERROR";

        public OnMoveDownloadFileFailReason(String detailMessage, String type) {
            super(detailMessage, type);
        }

        public OnMoveDownloadFileFailReason(Throwable throwable) {
            super(throwable);
        }

        @Override
        protected void onInitTypeWithThrowable(Throwable throwable) {
            super.onInitTypeWithThrowable(throwable);
            // TODO
        }
    }

    /**
     * Callback helper for main thread
     */
    public static class MainThreadHelper {

        /**
         * prepared move
         *
         * @param downloadFileNeedToMove download file needed to move
         */
        public static void onMoveDownloadFilePrepared(final DownloadFileInfo downloadFileNeedToMove, final 
        OnMoveDownloadFileListener onMoveDownloadFileListener) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onMoveDownloadFileListener.onMoveDownloadFilePrepared(downloadFileNeedToMove);
                    handler.removeCallbacksAndMessages(null);
                }
            });
        }

        /**
         * move succeed
         *
         * @param downloadFileMoved download file moved
         */
        public static void onMoveDownloadFileSuccess(final DownloadFileInfo downloadFileMoved, final 
        OnMoveDownloadFileListener onMoveDownloadFileListener) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onMoveDownloadFileListener.onMoveDownloadFileSuccess(downloadFileMoved);
                    handler.removeCallbacksAndMessages(null);
                }
            });
        }

        /**
         * move failed
         *
         * @param downloadFileInfo download file needed to move,may be null
         * @param failReason       fail reason
         */
        public static void onMoveDownloadFileFailed(final DownloadFileInfo downloadFileInfo, final 
        OnMoveDownloadFileFailReason failReason, final OnMoveDownloadFileListener onMoveDownloadFileListener) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onMoveDownloadFileListener.onMoveDownloadFileFailed(downloadFileInfo, failReason);
                    handler.removeCallbacksAndMessages(null);
                }
            });
        }
    }
}