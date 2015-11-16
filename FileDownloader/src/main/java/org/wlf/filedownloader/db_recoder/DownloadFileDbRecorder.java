package org.wlf.filedownloader.db_recoder;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.FailException;
import org.wlf.filedownloader.base.Record;

import java.util.List;

/**
 * record status for download file
 * <br/>
 * 数据库记录器（记录下载文件状态）
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public abstract class DownloadFileDbRecorder implements Record {

    // for child to impl
    @Override
    public abstract void recordStatus(String url, int status, int increaseSize) throws FailException;

    /**
     * add new DownloadFile
     *
     * @param downloadFileInfo new DownloadFile
     * @return true for succeeded
     */
    public abstract boolean addDownloadFile(DownloadFileInfo downloadFileInfo);

    /**
     * update an exist DownloadFile
     *
     * @param downloadFileInfo DownloadFile needed to update
     * @return true for succeeded
     */
    public abstract boolean updateDownloadFile(DownloadFileInfo downloadFileInfo);

    /**
     * delete an exist DownloadFile
     *
     * @param downloadFileInfo DownloadFile needed to delete
     * @return true for succeeded
     */
    public abstract boolean deleteDownloadFile(DownloadFileInfo downloadFileInfo);

    /**
     * get DownloadFile by url
     *
     * @param url the url
     * @return DownloadFile recorded
     */
    public abstract DownloadFileInfo getDownloadFile(String url);

    /**
     * get all DownloadFiles
     *
     * @return all DownloadFile recorded
     */
    public abstract List<DownloadFileInfo> getDownloadFiles();
}
