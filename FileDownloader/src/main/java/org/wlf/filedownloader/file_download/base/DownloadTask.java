package org.wlf.filedownloader.file_download.base;

import org.wlf.filedownloader.base.Stoppable;

/**
 * DownloadTask interface
 *
 * @author wlf(Andy)
 * @datetime 2016-01-10 00:12 GMT+8
 * @email 411086563@qq.com
 * @since 0.3.0
 */
public interface DownloadTask extends Runnable, Stoppable {

    /**
     * get download url of the task
     *
     * @return download url
     */
    String getUrl();

    /**
     * set StopFileDownloadTaskListener
     *
     * @param onStopFileDownloadTaskListener OnStopFileDownloadTaskListener
     */
     void setOnStopFileDownloadTaskListener(OnStopFileDownloadTaskListener onStopFileDownloadTaskListener);

    /**
     * set TaskRunFinishListener
     *
     * @param onTaskRunFinishListener
     */
    void setOnTaskRunFinishListener(OnTaskRunFinishListener onTaskRunFinishListener);
}
