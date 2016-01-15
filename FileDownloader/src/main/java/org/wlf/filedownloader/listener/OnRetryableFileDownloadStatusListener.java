package org.wlf.filedownloader.listener;

import android.os.Handler;
import android.os.Looper;

import org.wlf.filedownloader.DownloadFileInfo;

/**
 * OnRetryableFileDownloadStatusListener
 * <br/>
 * 文件下载状态改变监听器
 *
 * @author wlf(Andy)
 * @datetime 2016-01-04 11:30 GMT+8
 * @email 411086563@qq.com
 */
public interface OnRetryableFileDownloadStatusListener extends OnFileDownloadStatusListener {

    /**
     * retry download
     *
     * @param downloadFileInfo download file info
     * @param retryTimes       the times to retry
     */
    void onFileDownloadStatusRetrying(DownloadFileInfo downloadFileInfo, int retryTimes);

    /**
     * Callback helper for main thread
     */
    public static class MainThreadHelper {
        /**
         * retry download
         *
         * @param downloadFileInfo download file info
         * @param retryTimes       the times to retry
         */
        public static void onFileDownloadStatusRetrying(final DownloadFileInfo downloadFileInfo, final int 
                retryTimes, final OnRetryableFileDownloadStatusListener onRetryableFileDownloadStatusListener) {
            if (onRetryableFileDownloadStatusListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onRetryableFileDownloadStatusListener == null) {
                        return;
                    }
                    onRetryableFileDownloadStatusListener.onFileDownloadStatusRetrying(downloadFileInfo, retryTimes);
                }
            });
        }
    }
}
