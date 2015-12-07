package org.wlf.filedownloader.listener;

import org.wlf.filedownloader.DownloadFileInfo;

/**
 * an interface that can sync with caller when deleting download file
 *
 * @author wlf(Andy)
 * @datetime 2015-12-01 17:40 GMT+8
 * @email 411086563@qq.com
 */
public interface OnSyncDeleteDownloadFileListener extends OnDeleteDownloadFileListener {

    /**
     * sync delete(for example,the caller will need to do it's own database record)
     *
     * @param downloadFileDeleted download file deleted
     * @return true means the caller hopes to continue the operation,otherwise the caller may get in trouble itself,
     * the file-downloader will rollback the operation
     */
    boolean onDoSyncDeleteDownloadFile(DownloadFileInfo downloadFileDeleted);
}
