package org.wlf.filedownloader.listener;

import android.os.Handler;
import android.os.Looper;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.FailReason;

/**
 * OnFileDownloadStatusListener
 * <br/>
 * 文件下载状态改变监听器
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public interface OnFileDownloadStatusListener {

    /**
     * waiting download
     *
     * @param downloadFileInfo download file info
     */
    void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo);

    /**
     * preparing
     *
     * @param downloadFileInfo download file info
     */
    void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo);

    /**
     * prepared(connected)
     *
     * @param downloadFileInfo download file info
     */
    void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo);

    /**
     * downloading
     *
     * @param downloadFileInfo download file info
     * @param downloadSpeed    download speed,KB/s
     * @param remainingTime    remain time,seconds
     */
    void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long remainingTime);

    /**
     * download paused
     *
     * @param downloadFileInfo download file info
     */
    void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo);

    /**
     * download completed
     *
     * @param downloadFileInfo download file info
     */
    void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo);

    /**
     * download error
     *
     * @param url              file url
     * @param downloadFileInfo download file info,may null
     * @param failReason       fail reason
     */
    void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, OnFileDownloadStatusFailReason failReason);

    /**
     * Callback helper for main thread
     */
    public static class MainThreadHelper {

        /**
         * waiting download
         *
         * @param downloadFileInfo download file info
         */
        public static void onFileDownloadStatusWaiting(final DownloadFileInfo downloadFileInfo, final OnFileDownloadStatusListener onFileDownloadStatusListener) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onFileDownloadStatusListener.onFileDownloadStatusWaiting(downloadFileInfo);
                    handler.removeCallbacksAndMessages(null);
                }
            });
        }

        /**
         * preparing
         *
         * @param downloadFileInfo download file info
         */
        public static void onFileDownloadStatusPreparing(final DownloadFileInfo downloadFileInfo, final OnFileDownloadStatusListener onFileDownloadStatusListener) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onFileDownloadStatusListener.onFileDownloadStatusPreparing(downloadFileInfo);
                    handler.removeCallbacksAndMessages(null);
                }
            });
        }

        /**
         * prepared(connected)
         *
         * @param downloadFileInfo download file info
         */
        public static void onFileDownloadStatusPrepared(final DownloadFileInfo downloadFileInfo, final OnFileDownloadStatusListener onFileDownloadStatusListener) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onFileDownloadStatusListener.onFileDownloadStatusPrepared(downloadFileInfo);
                    handler.removeCallbacksAndMessages(null);
                }
            });
        }

        /**
         * downloading
         *
         * @param downloadFileInfo download file info
         * @param downloadSpeed    download speed,KB/s
         * @param remainingTime    remain time,seconds
         */
        public static void onFileDownloadStatusDownloading(final DownloadFileInfo downloadFileInfo, final float downloadSpeed, final long remainingTime, final OnFileDownloadStatusListener onFileDownloadStatusListener) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onFileDownloadStatusListener.onFileDownloadStatusDownloading(downloadFileInfo, downloadSpeed, remainingTime);
                    handler.removeCallbacksAndMessages(null);
                }
            });
        }

        /**
         * download paused
         *
         * @param downloadFileInfo download file info
         */
        public static void onFileDownloadStatusPaused(final DownloadFileInfo downloadFileInfo, final OnFileDownloadStatusListener onFileDownloadStatusListener) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onFileDownloadStatusListener.onFileDownloadStatusPaused(downloadFileInfo);
                    handler.removeCallbacksAndMessages(null);
                }
            });
        }

        /**
         * download completed
         *
         * @param downloadFileInfo download file info
         */
        public static void onFileDownloadStatusCompleted(final DownloadFileInfo downloadFileInfo, final OnFileDownloadStatusListener onFileDownloadStatusListener) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onFileDownloadStatusListener.onFileDownloadStatusCompleted(downloadFileInfo);
                    handler.removeCallbacksAndMessages(null);
                }
            });
        }

        /**
         * download error
         *
         * @param url              file url
         * @param downloadFileInfo download file info
         * @param failReason       fail reason
         */
        public static void onFileDownloadStatusFailed(final String url, final DownloadFileInfo downloadFileInfo, final OnFileDownloadStatusFailReason failReason, final OnFileDownloadStatusListener onFileDownloadStatusListener) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onFileDownloadStatusListener.onFileDownloadStatusFailed(url, downloadFileInfo, failReason);
                    handler.removeCallbacksAndMessages(null);
                }
            });
        }
    }

    /**
     * OnFileDownloadStatusFailReason
     */
    public static class OnFileDownloadStatusFailReason extends FailReason {

        private static final long serialVersionUID = -8178297554707996481L;

        /**
         * network denied
         */
        public static final String TYPE_NETWORK_DENIED = OnFileDownloadStatusFailReason.class.getName() + "_TYPE_NETWORK_DENIED";

        // in task
        /**
         * URL illegal
         */
        public static final String TYPE_URL_ILLEGAL = OnFileDownloadStatusFailReason.class.getName() + "_TYPE_URL_ILLEGAL";
        /**
         * file save path illegal
         */
        public static final String TYPE_FILE_SAVE_PATH_ILLEGAL = OnFileDownloadStatusFailReason.class.getName() + "_TYPE_FILE_SAVE_PATH_ILLEGAL";
        /**
         * storage space can not write
         */
        public static final String TYPE_STORAGE_SPACE_CAN_NOT_WRITE = OnFileDownloadStatusFailReason.class.getName() + "_TYPE_STORAGE_SPACE_CAN_NOT_WRITE";
        /**
         * storage space is full
         */
        public static final String TYPE_STORAGE_SPACE_IS_FULL = OnFileDownloadStatusFailReason.class.getName() + "_TYPE_STORAGE_SPACE_IS_FULL";

        // inf file_download_manager
        /**
         * url file does not detect
         */
        public static final String TYPE_FILE_NOT_DETECT = OnFileDownloadStatusFailReason.class.getName() + "_TYPE_FILE_NOT_DETECT";
        /**
         * file is downloading
         */
        public static final String TYPE_FILE_IS_DOWNLOADING = OnFileDownloadStatusFailReason.class.getName() + "_TYPE_FILE_IS_DOWNLOADING";
        /**
         * download file error
         */
        public static final String TYPE_DOWNLOAD_FILE_ERROR = OnFileDownloadStatusFailReason.class.getName() + "_TYPE_DOWNLOAD_FILE_ERROR";

        public OnFileDownloadStatusFailReason(String detailMessage, String type) {
            super(detailMessage, type);
        }

        public OnFileDownloadStatusFailReason(Throwable throwable) {
            super(throwable);
        }

        @Override
        protected void onInitTypeWithThrowable(Throwable throwable) {
            super.onInitTypeWithThrowable(throwable);
            // TODO
        }

    }
}
