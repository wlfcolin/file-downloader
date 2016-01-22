package org.wlf.filedownloader.listener;

import org.wlf.filedownloader.listener.OnDetectBigUrlFileListener.DetectBigUrlFileFailReason;

/**
 * OnDetectUrlFileListener
 * <br/>
 * 探测网络文件监听器
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 * @deprecated this callback can not detect the url file which bigger than 2G, use {@link
 * OnDetectBigUrlFileListener} instead
 */
@Deprecated
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
     * DetectUrlFileFailReason
     *
     * @deprecated use {@link OnDetectBigUrlFileListener} and  {@link
     * OnDetectBigUrlFileListener#onDetectUrlFileFailed(String, DetectBigUrlFileFailReason)} instead
     */
    @Deprecated
    public static class DetectUrlFileFailReason extends OnDetectBigUrlFileListener.DetectBigUrlFileFailReason {

        public DetectUrlFileFailReason(String url, String detailMessage, String type) {
            super(url, detailMessage, type);
        }

        public DetectUrlFileFailReason(String url, Throwable throwable) {
            super(url, throwable);
        }
    }
}
