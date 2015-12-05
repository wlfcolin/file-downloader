package org.wlf.filedownloader.listener;

import org.wlf.filedownloader.DownloadFileInfo;

/**
 * @author wlf(Andy)
 * @datetime 2015-12-01 17:40 GMT+8
 * @email 411086563@qq.com
 */
public abstract class OnSyncDeleteDownloadFileListener implements OnDeleteDownloadFileListener {

    /**
     * sync delete(for example,the caller will need to do it's own database record)
     *
     * @param downloadFileDeleted download file deleted
     * @return true means the caller hopes to continue the operation,otherwise the caller may get in trouble itself,
     * the file-downloader will rollback the operation
     */
    boolean onDoSyncDeleteDownloadFile(DownloadFileInfo downloadFileDeleted) {
        return true;
    }
}
