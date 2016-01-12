package org.wlf.filedownloader.util;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.BaseUrlFileInfo;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.file_download.db_recorder.Record;

/**
 * Util for {@link Status}
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DownloadFileUtil {

    /**
     * whether the download file can delete
     *
     * @param downloadFileInfo
     * @return
     */
    public static boolean canDelete(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return false;
        }

        switch (downloadFileInfo.getStatus()) {
            // only the status below can NOT be deleted
            case Status.DOWNLOAD_STATUS_WAITING:
            case Status.DOWNLOAD_STATUS_RETRYING:
            case Status.DOWNLOAD_STATUS_PREPARING:
            case Status.DOWNLOAD_STATUS_PREPARED:
            case Status.DOWNLOAD_STATUS_DOWNLOADING:
                return false;
        }

        return true;
    }

    /**
     * whether the download file can move
     *
     * @param downloadFileInfo
     * @return
     */
    public static boolean canMove(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return false;
        }

        switch (downloadFileInfo.getStatus()) {
            // only the status below can NOT be moved
            case Status.DOWNLOAD_STATUS_WAITING:
            case Status.DOWNLOAD_STATUS_RETRYING:
            case Status.DOWNLOAD_STATUS_PREPARING:
            case Status.DOWNLOAD_STATUS_PREPARED:
            case Status.DOWNLOAD_STATUS_DOWNLOADING:
                return false;
        }

        return true;
    }

    /**
     * whether the download file download completed
     *
     * @param downloadFileInfo
     * @return
     */
    public static boolean isCompleted(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return false;
        }

        switch (downloadFileInfo.getStatus()) {
            case Status.DOWNLOAD_STATUS_COMPLETED:
                return true;
        }

        return false;
    }

    /**
     * whether the download file can rename
     *
     * @param downloadFileInfo
     * @return
     */
    public static boolean canRename(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return false;
        }

        switch (downloadFileInfo.getStatus()) {
            // only the status below can NOT be renamed
            case Status.DOWNLOAD_STATUS_WAITING:
            case Status.DOWNLOAD_STATUS_RETRYING:
            case Status.DOWNLOAD_STATUS_PREPARING:
            case Status.DOWNLOAD_STATUS_PREPARED:
            case Status.DOWNLOAD_STATUS_DOWNLOADING:
                return false;
        }

        return true;
    }

    public static boolean isLegal(BaseUrlFileInfo baseUrlFileInfo) {

        if (baseUrlFileInfo == null || !UrlUtil.isUrl(baseUrlFileInfo.getUrl())) {
            return false;
        }

        return true;
    }

    public static void recoveryExceptionStatus(Record record, DownloadFileInfo downloadFileInfo) {

        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        // check whether there is an exception status
        switch (downloadFileInfo.getStatus()) {
            // 1.exception status: downloading
            case Status.DOWNLOAD_STATUS_WAITING:
            case Status.DOWNLOAD_STATUS_PREPARING:
            case Status.DOWNLOAD_STATUS_PREPARED:
            case Status.DOWNLOAD_STATUS_DOWNLOADING:
                // recover paused
                try {
                    record.recordStatus(url, Status.DOWNLOAD_STATUS_PAUSED, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            // 2.finished status, ignore
            case Status.DOWNLOAD_STATUS_COMPLETED:
            case Status.DOWNLOAD_STATUS_PAUSED:
            case Status.DOWNLOAD_STATUS_ERROR:
                // ignore
                break;
            // 3.exception status: error but not finished
            case Status.DOWNLOAD_STATUS_UNKNOWN:
            case Status.DOWNLOAD_STATUS_RETRYING:
            default:
                // recover error
                try {
                    record.recordStatus(url, Status.DOWNLOAD_STATUS_ERROR, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public static boolean isDownloadingStatus(DownloadFileInfo downloadFileInfo) {

        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            return false;
        }

        // only the status below is downloading
        switch (downloadFileInfo.getStatus()) {
            case Status.DOWNLOAD_STATUS_WAITING:
            case Status.DOWNLOAD_STATUS_RETRYING:
            case Status.DOWNLOAD_STATUS_PREPARING:
            case Status.DOWNLOAD_STATUS_PREPARED:
            case Status.DOWNLOAD_STATUS_DOWNLOADING:
                return true;
        }
        return false;
    }
}
