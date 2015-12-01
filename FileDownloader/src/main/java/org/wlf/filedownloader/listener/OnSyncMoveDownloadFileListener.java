package org.wlf.filedownloader.listener;

import org.wlf.filedownloader.DownloadFileInfo;

/**
 * @author wlf(Andy)
 * @datetime 2015-12-01 18:21 GMT+8
 * @email 411086563@qq.com
 */
public abstract class OnSyncMoveDownloadFileListener implements OnMoveDownloadFileListener {

    /**
     * sync move(for example,the caller will need to do it's own database record)
     *
     * @param downloadFileMoved download file moved
     * @return true means the caller hopes to continue the operation,otherwise the caller may get in trouble itself,the file-downloader will rollback the operation
     */
    boolean onDoSyncMoveDownloadFile(DownloadFileInfo downloadFileMoved) {
        return true;
    }
}
