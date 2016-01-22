package org.wlf.filedownloader.listener;

import android.os.Handler;
import android.os.Looper;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.UrlFailReason;

/**
 * listener for deleting download file
 * <br/>
 * 删除下载文件监听器
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public interface OnDeleteDownloadFileListener {

    /**
     * prepared delete
     *
     * @param downloadFileNeedDelete download file needed to delete
     */
    void onDeleteDownloadFilePrepared(DownloadFileInfo downloadFileNeedDelete);

    /**
     * delete succeed
     *
     * @param downloadFileDeleted download file deleted
     */
    void onDeleteDownloadFileSuccess(DownloadFileInfo downloadFileDeleted);

    /**
     * delete failed
     *
     * @param downloadFileInfo download file needed to delete,may be null
     * @param failReason       fail reason
     */
    void onDeleteDownloadFileFailed(DownloadFileInfo downloadFileInfo, DeleteDownloadFileFailReason failReason);

    /**
     * Callback helper for main thread
     */
    public static class MainThreadHelper {

        /**
         * prepared delete
         *
         * @param downloadFileNeedDelete download file needed to delete
         */
        public static void onDeleteDownloadFilePrepared(final DownloadFileInfo downloadFileNeedDelete, final 
        OnDeleteDownloadFileListener onDeleteDownloadFileListener) {
            if (onDeleteDownloadFileListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onDeleteDownloadFileListener == null) {
                        return;
                    }
                    onDeleteDownloadFileListener.onDeleteDownloadFilePrepared(downloadFileNeedDelete);
                }
            });
        }

        /**
         * delete succeed
         *
         * @param downloadFileDeleted download file deleted
         */
        public static void onDeleteDownloadFileSuccess(final DownloadFileInfo downloadFileDeleted, final 
        OnDeleteDownloadFileListener onDeleteDownloadFileListener) {
            if (onDeleteDownloadFileListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onDeleteDownloadFileListener == null) {
                        return;
                    }
                    onDeleteDownloadFileListener.onDeleteDownloadFileSuccess(downloadFileDeleted);
                }
            });
        }

        /**
         * delete failed
         *
         * @param downloadFileInfo download file needed to delete,may be null
         * @param failReason       fail reason
         */
        public static void onDeleteDownloadFileFailed(final DownloadFileInfo downloadFileInfo, final 
        DeleteDownloadFileFailReason failReason, final OnDeleteDownloadFileListener onDeleteDownloadFileListener) {
            if (onDeleteDownloadFileListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onDeleteDownloadFileListener == null) {
                        return;
                    }
                    onDeleteDownloadFileListener.onDeleteDownloadFileFailed(downloadFileInfo, failReason);
                }
            });
        }
    }

    /**
     * DeleteDownloadFileFailReason
     *
     * @deprecated use {@link DeleteDownloadFileFailReason} instead
     */
    @Deprecated
    public static class OnDeleteDownloadFileFailReason extends DeleteDownloadFileFailReason {

        public OnDeleteDownloadFileFailReason(String url, String detailMessage, String type) {
            super(url, detailMessage, type);
        }

        public OnDeleteDownloadFileFailReason(String url, Throwable throwable) {
            super(url, throwable);
        }
    }

    /**
     * DeleteDownloadFileFailReason
     */
    public static class DeleteDownloadFileFailReason extends UrlFailReason {
        /**
         * the download file record doest not exist
         */
        public static final String TYPE_FILE_RECORD_IS_NOT_EXIST = DeleteDownloadFileFailReason.class.getName() + 
                "_TYPE_FILE_RECORD_IS_NOT_EXIST";

        /**
         * the download file status error
         */
        public static final String TYPE_FILE_STATUS_ERROR = DeleteDownloadFileFailReason.class.getName() + 
                "_TYPE_RECORD_FILE_STATUS_ERROR";

        public DeleteDownloadFileFailReason(String url, String detailMessage, String type) {
            super(url, detailMessage, type);
        }

        public DeleteDownloadFileFailReason(String url, Throwable throwable) {
            super(url, throwable);
        }

        // StopDownloadFileTaskFailReason
    }
}
