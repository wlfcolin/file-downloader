package org.wlf.filedownloader.listener;

import org.wlf.filedownloader.DownloadFileInfo;

import java.util.List;

/**
 * @author wlf(Andy)
 * @datetime 2015-12-01 18:22 GMT+8
 * @email 411086563@qq.com
 */
public abstract class OnSycnMoveDownloadFilesListener implements OnMoveDownloadFilesListener {

    /**
     * sync move(for example,the caller will need to do it's own database record)
     *
     * @param downloadFilesNeedMove download files needed to move
     * @param downloadFilesMoved    download files moved
     * @return true means the caller hopes to continue the operation,otherwise the caller may get in trouble itself,the file-downloader will rollback the operation
     */
    public boolean onDoSyncMoveDownloadFiles(List<DownloadFileInfo> downloadFilesNeedMove, List<DownloadFileInfo> downloadFilesMoved) {
        return true;
    }
}
