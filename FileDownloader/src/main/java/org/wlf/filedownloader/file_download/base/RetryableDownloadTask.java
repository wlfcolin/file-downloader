package org.wlf.filedownloader.file_download.base;

/**
 * RetryableDownloadTask interface
 *
 * @author wlf(Andy)
 * @datetime 2016-01-10 00:17 GMT+8
 * @email 411086563@qq.com
 * @since 0.3.0
 */
public interface RetryableDownloadTask extends DownloadTask {

    /**
     * set RetryDownloadTimes
     *
     * @param retryDownloadTimes
     */
    void setRetryDownloadTimes(int retryDownloadTimes);
}
