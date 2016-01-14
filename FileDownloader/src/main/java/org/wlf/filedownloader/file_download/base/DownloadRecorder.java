package org.wlf.filedownloader.file_download.base;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.file_download.DetectUrlFileInfo;
import org.wlf.filedownloader.file_download.db_recorder.DownloadFileDbRecorder;

/**
 * DownloadRecorder
 * <br/>
 * 删除下载文件任务
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 * @since 0.3.0
 */
public interface DownloadRecorder extends DownloadFileDbRecorder {

    /**
     * record download file
     *
     * @param url        file url
     * @param deleteMode true means delete all resource
     * @throws Exception any exception during record
     */
    void resetDownloadFile(String url, boolean deleteMode) throws Exception;

    DownloadFileInfo createDownloadFileInfo(DetectUrlFileInfo detectUrlFileInfo);
}