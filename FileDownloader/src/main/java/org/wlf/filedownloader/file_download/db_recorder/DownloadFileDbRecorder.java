package org.wlf.filedownloader.file_download.db_recorder;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.file_download.Record;

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
     * add new DownloadFile
     *
     * @param downloadFileInfo new DownloadFile
     * @return true for succeeded
    boolean addDownloadFile(DownloadFileInfo downloadFileInfo);
     */
    /**
     * update an exist DownloadFile
     *
     * @param downloadFileInfo DownloadFile needed to update
     * @return true for succeeded

    boolean updateDownloadFile(DownloadFileInfo downloadFileInfo);
     */
    /**
     * delete an exist DownloadFile
     *
     * @param downloadFileInfo DownloadFile needed to delete
     * @return true for succeeded
    boolean deleteDownloadFile(DownloadFileInfo downloadFileInfo);
     */

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
