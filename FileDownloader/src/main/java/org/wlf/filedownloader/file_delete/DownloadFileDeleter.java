package org.wlf.filedownloader.file_delete;

import org.wlf.filedownloader.file_download.db_recorder.DownloadFileDbRecorder;

/**
 * DownloadFileDeleter
 * <br/>
 * 删除下载文件
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 * @since 0.3.0
 */
public interface DownloadFileDeleter extends DownloadFileDbRecorder {

    /**
     * delete download file
     *
     * @param url download url
     * @throws Exception any exception during delete
     */
    void deleteDownloadFile(String url) throws Exception;
}