package org.wlf.filedownloader.listener;

import android.os.Handler;
import android.os.Looper;

import org.wlf.filedownloader.DownloadFileInfo;

import java.util.List;

/**
 * listener for moving multi download files
 * <br/>
 * 批量移动下载文件监听器
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public interface OnMoveDownloadFilesListener {

    /**
     * prepared multi move
     *
     * @param downloadFilesNeedMove download files needed to move
     */
    void onMoveDownloadFilesPrepared(List<DownloadFileInfo> downloadFilesNeedMove);

    /**
     * moving
     *
     * @param downloadFilesNeedMove download files needed to move
     * @param downloadFilesMoved    download files moved
     * @param downloadFilesSkip     download files skipped
     * @param downloadFileMoving    download file moving
     */
    void onMovingDownloadFiles(List<DownloadFileInfo> downloadFilesNeedMove, List<DownloadFileInfo> 
            downloadFilesMoved, List<DownloadFileInfo> downloadFilesSkip, DownloadFileInfo downloadFileMoving);

    /**
     * move multi completed
     *
     * @param downloadFilesNeedMove download files needed to move
     * @param downloadFilesMoved    download files moved
     */
    void onMoveDownloadFilesCompleted(List<DownloadFileInfo> downloadFilesNeedMove, List<DownloadFileInfo> 
            downloadFilesMoved);

    /**
     * Callback helper for main thread
     */
    public static class MainThreadHelper {

        /**
         * prepared multi move
         *
         * @param downloadFilesNeedMove download files needed to move
         */
        public static void onMoveDownloadFilesPrepared(final List<DownloadFileInfo> downloadFilesNeedMove, final 
        OnMoveDownloadFilesListener onMoveDownloadFilesListener) {
            if (onMoveDownloadFilesListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onMoveDownloadFilesListener == null) {
                        return;
                    }
                    onMoveDownloadFilesListener.onMoveDownloadFilesPrepared(downloadFilesNeedMove);
                }
            });
        }

        /**
         * moving
         *
         * @param downloadFilesNeedMove download files needed to move
         * @param downloadFilesMoved    download files moved
         * @param downloadFilesSkip     download files skipped
         * @param downloadFileMoving    download file moving
         */
        public static void onMovingDownloadFiles(final List<DownloadFileInfo> downloadFilesNeedMove, final 
        List<DownloadFileInfo> downloadFilesMoved, final List<DownloadFileInfo> downloadFilesSkip, final 
        DownloadFileInfo downloadFileMoving, final OnMoveDownloadFilesListener onMoveDownloadFilesListener) {
            if (onMoveDownloadFilesListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onMoveDownloadFilesListener == null) {
                        return;
                    }
                    onMoveDownloadFilesListener.onMovingDownloadFiles(downloadFilesNeedMove, downloadFilesMoved, 
                            downloadFilesSkip, downloadFileMoving);
                }
            });
        }

        /**
         * move multi completed
         *
         * @param downloadFilesNeedMove download files needed to move
         * @param downloadFilesMoved    download files moved
         */
        public static void onMoveDownloadFilesCompleted(final List<DownloadFileInfo> downloadFilesNeedMove, final 
        List<DownloadFileInfo> downloadFilesMoved, final OnMoveDownloadFilesListener onMoveDownloadFilesListener) {
            if (onMoveDownloadFilesListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onMoveDownloadFilesListener == null) {
                        return;
                    }
                    onMoveDownloadFilesListener.onMoveDownloadFilesCompleted(downloadFilesNeedMove, downloadFilesMoved);
                }
            });
        }
    }
}
