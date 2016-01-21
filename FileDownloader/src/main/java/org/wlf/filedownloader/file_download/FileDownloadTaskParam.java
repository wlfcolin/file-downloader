package org.wlf.filedownloader.file_download;

import org.wlf.filedownloader.DownloadFileInfo;

/**
 * FileDownloadTaskParam
 *
 * @author wlf(Andy)
 * @datetime 2016-01-08 18:50 GMT+8
 * @email 411086563@qq.com
 */
class FileDownloadTaskParam {
    /**
     * file url
     */
    public final String url;
    /**
     * the position of this time to start
     */
    public final long startPosInTotal;
    /**
     * file total size
     */
    public final long fileTotalSize;
    /**
     * file eTag
     */
    public final String eTag;
    /**
     * file last modified datetime(in server)
     */
    public final String lastModified;
    /**
     * AcceptRangeType
     */
    public final String acceptRangeType;
    /**
     * TempFilePath
     */
    public final String tempFilePath;
    /**
     * SaveFilePath
     */
    public final String filePath;

    public FileDownloadTaskParam(String url, long startPosInTotal, long fileTotalSize, String eTag, String lastModified, String acceptRangeType, String tempFilePath, String filePath) {
        super();
        this.url = url;
        this.startPosInTotal = startPosInTotal;
        this.fileTotalSize = fileTotalSize;
        this.eTag = eTag;
        this.lastModified = lastModified;
        this.acceptRangeType = acceptRangeType;
        this.tempFilePath = tempFilePath;
        this.filePath = filePath;
    }

    public static FileDownloadTaskParam createByDownloadFile(DownloadFileInfo downloadFileInfo) {
        if (downloadFileInfo == null) {
            return null;
        }
        return new FileDownloadTaskParam(downloadFileInfo.getUrl(), downloadFileInfo.getDownloadedSizeLong(), downloadFileInfo.getFileSizeLong(), downloadFileInfo.getETag(), downloadFileInfo.getLastModified(), downloadFileInfo.getAcceptRangeType(), downloadFileInfo.getTempFilePath(), downloadFileInfo.getFilePath());
    }
}
