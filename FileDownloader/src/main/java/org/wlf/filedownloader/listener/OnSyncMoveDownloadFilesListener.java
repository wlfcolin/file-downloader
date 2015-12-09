package org.wlf.filedownloader.listener;

import org.wlf.filedownloader.DownloadFileInfo;

import java.util.List;

/**
 * an interface that can sync with caller when moving multi download files
 *
 * @author wlf(Andy)
 * @datetime 2015-12-01 18:22 GMT+8
 * @email 411086563@qq.com
 */
public interface OnSyncMoveDownloadFilesListener extends OnMoveDownloadFilesListener {

    /**
     * sync move(for example,the caller will need to do it's own database record)
     *
     * @param downloadFilesNeedMove download files needed to move
     * @param downloadFilesMoved    download files moved
     * @return if the return result is equals to the downloadFilesMoved,that means the caller hopes to continue the
     * operation,otherwise the caller may get in trouble itself,
     * the file-downloader will rollback some of the operations that the return result not in the downloadFilesMoved
     */
    List<DownloadFileInfo> onDoSyncMoveDownloadFiles(List<DownloadFileInfo> downloadFilesNeedMove, 
                                                     List<DownloadFileInfo> downloadFilesMoved);
}
