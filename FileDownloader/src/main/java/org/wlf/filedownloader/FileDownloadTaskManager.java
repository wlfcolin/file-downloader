package org.wlf.filedownloader;

import org.wlf.filedownloader.FileDownloadTask.OnStopDownloadFileTaskFailReason;
import org.wlf.filedownloader.FileDownloadTask.OnStopFileDownloadTaskListener;

/**
 * file download task manager
 *
 * @author wlf(Andy)
 * @datetime 2015-12-09 18:14 GMT+8
 * @email 411086563@qq.com
 */
class FileDownloadTaskManager implements OnStopFileDownloadTaskListener {
    
    @Override
    public void onStopFileDownloadTaskSucceed(String url) {
       
    }

    @Override
    public void onStopFileDownloadTaskFailed(String url, OnStopDownloadFileTaskFailReason failReason) {

    }
}
