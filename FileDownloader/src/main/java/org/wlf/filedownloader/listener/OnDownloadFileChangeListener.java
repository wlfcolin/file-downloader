package org.wlf.filedownloader.listener;

import org.wlf.filedownloader.DownloadFileInfo;

/**
 * the listener for listening the DownloadFile change
 *
 * @author wlf(Andy)
 * @datetime 2015-12-09 16:45 GMT+8
 * @email 411086563@qq.com
 */
public interface OnDownloadFileChangeListener {

    /**
     * an new DownloadFile created
     *
     * @param downloadFileInfo new DownloadFile created
     */
    void onDownloadFileCreated(DownloadFileInfo downloadFileInfo);

    /**
     * an DownloadFile updated
     *
     * @param downloadFileInfo DownloadFile updated
     * @param type             the update type
     */
    void onDownloadFileUpdated(DownloadFileInfo downloadFileInfo, Type type);

    /**
     * an DownloadFile deleted
     *
     * @param downloadFileInfo DownloadFile deleted
     */
    void onDownloadFileDeleted(DownloadFileInfo downloadFileInfo);

    /**
     * DownloadFile Update Type
     */
    public static enum Type {
        /**
         * save dir changed
         */
        SAVE_DIR,
        /**
         * save file name changed
         */
        SAVE_FILE_NAME,
        /**
         * other，may be all
         */
        OTHER
    }
}
