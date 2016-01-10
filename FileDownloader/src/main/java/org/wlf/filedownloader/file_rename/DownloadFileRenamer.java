package org.wlf.filedownloader.file_rename;

import org.wlf.filedownloader.file_download.db_recorder.DownloadFileDbRecorder;

/**
 * DownloadFileRenamer
 * <br/>
 * 重命名下载文件
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 * @since 0.3.0
 */
public interface DownloadFileRenamer extends DownloadFileDbRecorder {

    /**
     * rename download file name
     *
     * @param url         download url
     * @param newFileName new file name
     * @throws Exception any exception during rename
     */
    void renameDownloadFile(String url, String newFileName) throws Exception;
}