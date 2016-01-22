package org.wlf.filedownloader.base;

/**
 * download file status
 * <br/>
 * 文件下载状态
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public final class Status {

    /**
     * UNKNOWN
     */
    public static final int DOWNLOAD_STATUS_UNKNOWN = 0;
    /**
     * WAITING(wait for other task finish,there is no more task place to hold this task)
     */
    public static final int DOWNLOAD_STATUS_WAITING = 1;
    /**
     * PREPARING(prepare to connect the url source)
     */
    public static final int DOWNLOAD_STATUS_PREPARING = 2;
    /**
     * PREPARED(the url source has been connected)
     */
    public static final int DOWNLOAD_STATUS_PREPARED = 3;
    /**
     * DOWNLOADING
     */
    public static final int DOWNLOAD_STATUS_DOWNLOADING = 4;
    /**
     * COMPLETED(the file has been fully downloaded without errors)
     */
    public static final int DOWNLOAD_STATUS_COMPLETED = 5;
    /**
     * PAUSED(the download is paused)
     */
    public static final int DOWNLOAD_STATUS_PAUSED = 6;
    /**
     * ERROR(the download encountered serious error and may not recovery)
     */
    public static final int DOWNLOAD_STATUS_ERROR = 7;
    /**
     * FILE_NOT_EXIST(the file has been started downloading ,but removed or delete unexpected)
     */
    public static final int DOWNLOAD_STATUS_FILE_NOT_EXIST = 8;
    /**
     * RETRYING(retry to re-connect the url source after failed)
     */
    public static final int DOWNLOAD_STATUS_RETRYING = 9;
}
