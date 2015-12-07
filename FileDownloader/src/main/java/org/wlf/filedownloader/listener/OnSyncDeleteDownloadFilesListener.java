package org.wlf.filedownloader.listener;

import org.wlf.filedownloader.DownloadFileInfo;

import java.util.List;

/**
 * an interface that can sync with caller when deleting multi download files
 *
 * @author wlf(Andy)
 * @datetime 2015-12-01 17:56 GMT+8
 * @email 411086563@qq.com
 */
public interface OnSyncDeleteDownloadFilesListener extends OnDeleteDownloadFilesListener {

    /**
     * sync delete(for example,the caller will need to do it's own database record)
     *
     * @param downloadFilesNeedDelete download files needed to delete
     * @param downloadFilesDeleted    download files deleted
     * @return true means the caller hopes to continue the operation,otherwise the caller may get in trouble itself,
     * the file-downloader will rollback the operation
     */
    boolean onDoSyncDeleteDownloadFiles(List<DownloadFileInfo> downloadFilesNeedDelete, List<DownloadFileInfo> 
            downloadFilesDeleted);
}
