package org.wlf.filedownloader.listener;

import android.os.Handler;
import android.os.Looper;

import org.wlf.filedownloader.file_download.base.HttpFailReason;

/**
 * OnDetectBigUrlFileListener
 * <br/>
 * 探测网络文件监听器（可以支持大文件监听）
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public interface OnDetectBigUrlFileListener {

    /**
     * the url file need to create(no database record for this url file)
     *
     * @param url      file url
     * @param fileName file name
     * @param saveDir  saveDir
     * @param fileSize fileSize
     */
    public abstract void onDetectNewDownloadFile(String url, String fileName, String saveDir, long fileSize);

    /**
     * the url file exist(it is in database record)
     *
     * @param url file url
     */
    void onDetectUrlFileExist(String url);

    /**
     * DetectUrlFileFailed
     *
     * @param url        file url
     * @param failReason fail reason
     */
    void onDetectUrlFileFailed(String url, DetectBigUrlFileFailReason failReason);

    /**
     * Callback helper for main thread
     */
    public static class MainThreadHelper {

        /**
         * the url file need to create(no database record for this url file)
         *
         * @param url      file url
         * @param fileName file name
         * @param saveDir  saveDir
         * @param fileSize fileSize
         */
        public static void onDetectNewDownloadFile(final String url, final String fileName, final String saveDir, 
                                                   final long fileSize, final OnDetectBigUrlFileListener 
                                                           nnDetectBigUrlFileListener) {
            if (nnDetectBigUrlFileListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (nnDetectBigUrlFileListener == null) {
                        return;
                    }
                    nnDetectBigUrlFileListener.onDetectNewDownloadFile(url, fileName, saveDir, fileSize);
                }
            });
        }

        /**
         * the url file exist(it is in database record)
         *
         * @param url file url
         */
        public static void onDetectUrlFileExist(final String url, final OnDetectBigUrlFileListener 
                nnDetectBigUrlFileListener) {
            if (nnDetectBigUrlFileListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (nnDetectBigUrlFileListener == null) {
                        return;
                    }
                    nnDetectBigUrlFileListener.onDetectUrlFileExist(url);
                }
            });
        }

        /**
         * DetectUrlFileFailed
         *
         * @param url        file url
         * @param failReason fail reason
         */
        public static void onDetectUrlFileFailed(final String url, final DetectBigUrlFileFailReason failReason, final
        OnDetectBigUrlFileListener nnDetectBigUrlFileListener) {
            if (nnDetectBigUrlFileListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (nnDetectBigUrlFileListener == null) {
                        return;
                    }
                    nnDetectBigUrlFileListener.onDetectUrlFileFailed(url, failReason);
                }
            });
        }
    }

    /**
     * DetectUrlFileFailReason
     */
    public static class DetectBigUrlFileFailReason extends HttpFailReason {
        /**
         * URL illegal
         */
        public static final String TYPE_URL_ILLEGAL = DetectBigUrlFileFailReason.class.getName() + "_TYPE_URL_ILLEGAL";
        /**
         * url over redirect count
         */
        public static final String TYPE_URL_OVER_REDIRECT_COUNT = DetectBigUrlFileFailReason.class.getName() + 
                "_TYPE_URL_OVER_REDIRECT_COUNT";
        /**
         * bad http response code, not 2XX
         */
        public static final String TYPE_BAD_HTTP_RESPONSE_CODE = DetectBigUrlFileFailReason.class.getName() + 
                "_TYPE_BAD_HTTP_RESPONSE_CODE";
        /**
         * the file need to download does not exist
         */
        public static final String TYPE_HTTP_FILE_NOT_EXIST = DetectBigUrlFileFailReason.class.getName() + 
                "_TYPE_HTTP_FILE_NOT_EXIST";

        public DetectBigUrlFileFailReason(String url, String detailMessage, String type) {
            super(url, detailMessage, type);
        }

        public DetectBigUrlFileFailReason(String url, Throwable throwable) {
            super(url, throwable);
        }
    }

}
