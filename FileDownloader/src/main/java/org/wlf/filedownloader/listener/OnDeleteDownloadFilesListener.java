package org.wlf.filedownloader.listener;

import android.os.Handler;
import android.os.Looper;

import org.wlf.filedownloader.DownloadFileInfo;

import java.util.List;

/**
 * listener for deleting multi download files
 * <br/>
 * 批量删除下载文件监听器
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public interface OnDeleteDownloadFilesListener {

    /**
     * prepared multi delete
     *
     * @param downloadFilesNeedDelete download files needed to delete
     */
    void onDeleteDownloadFilesPrepared(List<DownloadFileInfo> downloadFilesNeedDelete);

    /**
     * deleting
     *
     * @param downloadFilesNeedDelete download files needed to delete
     * @param downloadFilesDeleted    download files deleted
     * @param downloadFilesSkip       download files skipped
     * @param downloadFileDeleting    download file deleting
     */
    void onDeletingDownloadFiles(List<DownloadFileInfo> downloadFilesNeedDelete, List<DownloadFileInfo> 
            downloadFilesDeleted, List<DownloadFileInfo> downloadFilesSkip, DownloadFileInfo downloadFileDeleting);

    /**
     * delete multi completed
     *
     * @param downloadFilesNeedDelete download files needed to delete
     * @param downloadFilesDeleted    download files deleted
     */
    void onDeleteDownloadFilesCompleted(List<DownloadFileInfo> downloadFilesNeedDelete, List<DownloadFileInfo> 
            downloadFilesDeleted);

    /**
     * Callback helper for main thread
     */
    public static class MainThreadHelper {

        /**
         * prepared multi delete
         *
         * @param downloadFilesNeedDelete download files needed to delete
         */
        public static void onDeleteDownloadFilesPrepared(final List<DownloadFileInfo> downloadFilesNeedDelete, final 
        OnDeleteDownloadFilesListener onDeleteDownloadFilesListener) {
            if (onDeleteDownloadFilesListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onDeleteDownloadFilesListener == null) {
                        return;
                    }
                    onDeleteDownloadFilesListener.onDeleteDownloadFilesPrepared(downloadFilesNeedDelete);
                }
            });
        }

        /**
         * deleting
         *
         * @param downloadFilesNeedDelete download files needed to delete
         * @param downloadFilesDeleted    download files deleted
         * @param downloadFilesSkip       download files skipped
         * @param downloadFileDeleting    download file deleting
         */
        public static void onDeletingDownloadFiles(final List<DownloadFileInfo> downloadFilesNeedDelete, final 
        List<DownloadFileInfo> downloadFilesDeleted, final List<DownloadFileInfo> downloadFilesSkip, final 
        DownloadFileInfo downloadFileDeleting, final OnDeleteDownloadFilesListener onDeleteDownloadFilesListener) {
            if (onDeleteDownloadFilesListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onDeleteDownloadFilesListener == null) {
                        return;
                    }
                    onDeleteDownloadFilesListener.onDeletingDownloadFiles(downloadFilesNeedDelete, 
                            downloadFilesDeleted, downloadFilesSkip, downloadFileDeleting);
                }
            });
        }

        /**
         * delete multi completed
         *
         * @param downloadFilesNeedDelete download files needed to delete
         * @param downloadFilesDeleted    download files deleted
         */
        public static void onDeleteDownloadFilesCompleted(final List<DownloadFileInfo> downloadFilesNeedDelete, final
        List<DownloadFileInfo> downloadFilesDeleted, final OnDeleteDownloadFilesListener 
                onDeleteDownloadFilesListener) {
            if (onDeleteDownloadFilesListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onDeleteDownloadFilesListener == null) {
                        return;
                    }
                    onDeleteDownloadFilesListener.onDeleteDownloadFilesCompleted(downloadFilesNeedDelete, 
                            downloadFilesDeleted);
                }
            });
        }
    }
}
