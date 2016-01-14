package org.wlf.filedownloader.listener;

import org.wlf.filedownloader.DownloadFileInfo;

/**
 * OnRetryableFileDownloadStatusListener
 * <br/>
 * 文件下载状态改变监听器
 *
 * @author wlf(Andy)
 * @datetime 2016-01-04 11:30 GMT+8
 * @email 411086563@qq.com
 */
public interface OnRetryableFileDownloadStatusListener extends OnFileDownloadStatusListener {

    /**
     * retry download
     *
     * @param downloadFileInfo download file info
     * @param retryTimes       the times to retry
     */
    void onFileDownloadStatusRetrying(DownloadFileInfo downloadFileInfo, int retryTimes);
}
