package org.wlf.filedownloader.file_download.db_recorder;

import org.wlf.filedownloader.DownloadFileInfo;

import java.util.List;

/**
 * record status for download file
 * <br/>
 * 数据库记录器（记录下载文件状态）
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public interface DownloadFileDbRecorder extends Record {

    /**
     * get DownloadFile by url
     *
     * @param url the url
     * @return DownloadFile recorded
     */
    DownloadFileInfo getDownloadFile(String url);

    /**
     * get all DownloadFiles
     *
     * @return all DownloadFile recorded
     */
    List<DownloadFileInfo> getDownloadFiles();
}
