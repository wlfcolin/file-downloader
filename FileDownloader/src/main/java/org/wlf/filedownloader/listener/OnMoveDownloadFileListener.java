package org.wlf.filedownloader.listener;

import android.os.Handler;
import android.os.Looper;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.UrlFailReason;

/**
 * listener for moving download file
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
    void onMoveDownloadFileFailed(DownloadFileInfo downloadFileInfo, MoveDownloadFileFailReason failReason);

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
            if (onMoveDownloadFileListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onMoveDownloadFileListener == null) {
                        return;
                    }
                    onMoveDownloadFileListener.onMoveDownloadFilePrepared(downloadFileNeedToMove);
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
            if (onMoveDownloadFileListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onMoveDownloadFileListener == null) {
                        return;
                    }
                    onMoveDownloadFileListener.onMoveDownloadFileSuccess(downloadFileMoved);
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
        MoveDownloadFileFailReason failReason, final OnMoveDownloadFileListener onMoveDownloadFileListener) {
            if (onMoveDownloadFileListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onMoveDownloadFileListener == null) {
                        return;
                    }
                    onMoveDownloadFileListener.onMoveDownloadFileFailed(downloadFileInfo, failReason);
                }
            });
        }
    }

    /**
     * MoveDownloadFileFailReason
     *
     * @deprecated use {@link MoveDownloadFileFailReason} instead
     */
    @Deprecated
    public static class OnMoveDownloadFileFailReason extends MoveDownloadFileFailReason {

        public OnMoveDownloadFileFailReason(String url, String detailMessage, String type) {
            super(url, detailMessage, type);
        }

        public OnMoveDownloadFileFailReason(String url, Throwable throwable) {
            super(url, throwable);
        }
    }

    /**
     * MoveDownloadFileFailReason
     */
    public static class MoveDownloadFileFailReason extends UrlFailReason {

        /**
         * target file exist
         */
        public static final String TYPE_TARGET_FILE_EXIST = MoveDownloadFileFailReason.class.getName() + 
                "_TYPE_TARGET_FILE_EXIST";
        /**
         * original file not exist
         */
        public static final String TYPE_ORIGINAL_FILE_NOT_EXIST = MoveDownloadFileFailReason.class.getName() + 
                "_TYPE_ORIGINAL_FILE_NOT_EXIST";
        /**
         * update record error
         */
        public static final String TYPE_UPDATE_RECORD_ERROR = MoveDownloadFileFailReason.class.getName() + 
                "_TYPE_UPDATE_RECORD_ERROR";
        /**
         * file status error,can not move
         */
        public static final String TYPE_FILE_STATUS_ERROR = MoveDownloadFileFailReason.class.getName() + 
                "_TYPE_FILE_STATUS_ERROR";


        public MoveDownloadFileFailReason(String url, String detailMessage, String type) {
            super(url, detailMessage, type);
        }

        public MoveDownloadFileFailReason(String url, Throwable throwable) {
            super(url, throwable);
        }

    }
}