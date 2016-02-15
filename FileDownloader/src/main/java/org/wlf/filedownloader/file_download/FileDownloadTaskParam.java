package org.wlf.filedownloader.file_download;

import org.wlf.filedownloader.DownloadFileInfo;

import java.util.Map;

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
    private String mUrl;
    /**
     * the position of this time to start
     */
    private long mStartPosInTotal;
    /**
     * file total size
     */
    private long mFileTotalSize;
    /**
     * file eTag
     */
    private String mETag;
    /**
     * file last modified datetime(in server)
     */
    private String mLastModified;
    /**
     * AcceptRangeType
     */
    private String mAcceptRangeType;
    /**
     * TempFilePath
     */
    private String mTempFilePath;
    /**
     * SaveFilePath
     */
    private String mFilePath;

    private String mRequestMethod = "GET";

    private Map<String, String> mHeaders;// custom  headers

    public FileDownloadTaskParam(String url, long startPosInTotal, long fileTotalSize, String ETag, String 
            lastModified, String acceptRangeType, String tempFilePath, String filePath) {
        mUrl = url;
        mStartPosInTotal = startPosInTotal;
        mFileTotalSize = fileTotalSize;
        mETag = ETag;
        mLastModified = lastModified;
        mAcceptRangeType = acceptRangeType;
        mTempFilePath = tempFilePath;
        mFilePath = filePath;
    }

    public static FileDownloadTaskParam createByDownloadFile(DownloadFileInfo downloadFileInfo, String requestMethod,
                                                             Map<String, String> headers) {

        if (downloadFileInfo == null) {
            return null;
        }

        FileDownloadTaskParam fileDownloadTaskParam = new FileDownloadTaskParam(downloadFileInfo.getUrl(), 
                downloadFileInfo.getDownloadedSizeLong(), downloadFileInfo.getFileSizeLong(), downloadFileInfo
                .getETag(), downloadFileInfo.getLastModified(), downloadFileInfo.getAcceptRangeType(), 
                downloadFileInfo.getTempFilePath(), downloadFileInfo.getFilePath());
        fileDownloadTaskParam.mRequestMethod = requestMethod;
        fileDownloadTaskParam.mHeaders = headers;

        return fileDownloadTaskParam;
    }

    // --------------------------------------setters--------------------------------------

    public void setRequestMethod(String requestMethod) {
        mRequestMethod = requestMethod;
    }

    public void setHeaders(Map<String, String> headers) {
        mHeaders = headers;
    }

    // --------------------------------------getters--------------------------------------

    public String getUrl() {
        return mUrl;
    }

    public long getStartPosInTotal() {
        return mStartPosInTotal;
    }

    public long getFileTotalSize() {
        return mFileTotalSize;
    }

    public String getETag() {
        return mETag;
    }

    public String getLastModified() {
        return mLastModified;
    }

    public String getAcceptRangeType() {
        return mAcceptRangeType;
    }

    public String getTempFilePath() {
        return mTempFilePath;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public String getRequestMethod() {
        return mRequestMethod;
    }

    public Map<String, String> getHeaders() {
        return mHeaders;
    }
}
