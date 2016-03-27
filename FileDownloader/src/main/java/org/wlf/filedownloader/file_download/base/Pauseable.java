package org.wlf.filedownloader.file_download.base;

/**
 * Pauseable interface
 * <br/>
 * 可暂停的接口
 *
 * @author wlf(Andy)
 * @datetime 2016-01-10 00:44 GMT+8
 * @email 411086563@qq.com
 * @since 0.3.0
 */
public interface Pauseable {

    /**
     * whether is downloading
     *
     * @param url file url
     * @return true means the download task of the url is running
     */
    boolean isDownloading(String url);

    /**
     * pause a download
     *
     * @param url                            file url
     * @param onStopFileDownloadTaskListener OnStopFileDownloadTaskListener impl
     */
    void pause(String url, OnStopFileDownloadTaskListener onStopFileDownloadTaskListener);
}
