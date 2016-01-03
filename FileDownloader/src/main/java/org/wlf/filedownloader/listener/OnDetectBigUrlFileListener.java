package org.wlf.filedownloader.listener;

/**
 * OnDetectBigUrlFileListener
 * <br/>
 * 探测网络文件监听器（可以支持大文件监听）
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public abstract class OnDetectBigUrlFileListener implements OnDetectUrlFileListener {

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
     * @deprecated this callback method will not be called, please use {@link #onDetectNewDownloadFile(String,
     * String, String, long)} instead
     */
    @Deprecated
    public void onDetectNewDownloadFile(String url, String fileName, String saveDir, int fileSize) {
    }
}
