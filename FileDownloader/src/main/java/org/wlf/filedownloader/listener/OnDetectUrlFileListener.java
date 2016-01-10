package org.wlf.filedownloader.listener;

import android.os.Handler;
import android.os.Looper;

import org.wlf.filedownloader.file_download.base.HttpFailReason;

/**
 * OnDetectUrlFileListener
 * <br/>
 * 探测网络文件监听器
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public interface OnDetectUrlFileListener {

    /**
     * the url file need to create(no database record for this url file)
     *
     * @param url      file url
     * @param fileName file name
     * @param saveDir  saveDir
     * @param fileSize fileSize
     */
    void onDetectNewDownloadFile(String url, String fileName, String saveDir, int fileSize);

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
    void onDetectUrlFileFailed(String url, DetectUrlFileFailReason failReason);

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
                                                   final long fileSize, final OnDetectUrlFileListener 
                                                           onDetectUrlFileListener) {
            if (onDetectUrlFileListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onDetectUrlFileListener == null) {
                        return;
                    }
                    if (onDetectUrlFileListener instanceof OnDetectBigUrlFileListener) {
                        ((OnDetectBigUrlFileListener) onDetectUrlFileListener).onDetectNewDownloadFile(url, fileName,
                                saveDir, fileSize);
                    } else {
                        onDetectUrlFileListener.onDetectNewDownloadFile(url, fileName, saveDir, (int) fileSize);
                    }
                }
            });
        }

        /**
         * the url file exist(it is in database record)
         *
         * @param url file url
         */
        public static void onDetectUrlFileExist(final String url, final OnDetectUrlFileListener 
                onDetectUrlFileListener) {
            if (onDetectUrlFileListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onDetectUrlFileListener == null) {
                        return;
                    }
                    onDetectUrlFileListener.onDetectUrlFileExist(url);
                }
            });
        }

        /**
         * DetectUrlFileFailed
         *
         * @param url        file url
         * @param failReason fail reason
         */
        public static void onDetectUrlFileFailed(final String url, final DetectUrlFileFailReason failReason, final 
        OnDetectUrlFileListener onDetectUrlFileListener) {
            if (onDetectUrlFileListener == null) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onDetectUrlFileListener == null) {
                        return;
                    }
                    onDetectUrlFileListener.onDetectUrlFileFailed(url, failReason);
                }
            });
        }

    }

    /**
     * DetectUrlFileFailReason
     */
    public static class DetectUrlFileFailReason extends HttpFailReason {
        /**
         * URL illegal
         */
        public static final String TYPE_URL_ILLEGAL = DetectUrlFileFailReason.class.getName() + "_TYPE_URL_ILLEGAL";
        /**
         * url over redirect count
         */
        public static final String TYPE_URL_OVER_REDIRECT_COUNT = DetectUrlFileFailReason.class.getName() + 
                "_TYPE_URL_OVER_REDIRECT_COUNT";
        /**
         * bad http response code, not 2XX
         */
        public static final String TYPE_BAD_HTTP_RESPONSE_CODE = DetectUrlFileFailReason.class.getName() + 
                "_TYPE_BAD_HTTP_RESPONSE_CODE";
        /**
         * the file need to download does not exist
         */
        public static final String TYPE_HTTP_FILE_NOT_EXIST = DetectUrlFileFailReason.class.getName() + 
                "_TYPE_HTTP_FILE_NOT_EXIST";

        public DetectUrlFileFailReason(String detailMessage, String type) {
            super(detailMessage, type);
        }

        public DetectUrlFileFailReason(Throwable throwable) {
            super(throwable);
        }
    }
}
