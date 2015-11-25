package org.wlf.filedownloader;

import java.io.File;

/**
 * base url file info
 *
 * @author wlf(Andy)
 * @datetime 2015-11-25 09:58 GMT+8
 * @email 411086563@qq.com
 */
public abstract class BaseUrlFileInfo {

    /**
     * Support Range of bytes
     */
    public static final String RANGE_TYPE_BYTES = "bytes";

    /**
     * file url
     */
    protected String mUrl;
    /**
     * file total size
     */
    protected int mFileSize;
    /**
     * file eTag
     */
    protected String mETag;
    /**
     * AcceptRangeType
     */
    protected String mAcceptRangeType;
    /**
     * SaveFileDir
     */
    protected String mFileDir;
    /**
     * SaveFileName
     */
    protected String mFileName;

    // setters,package use only

    /**
     * set save file dir
     */
    void setFileDir(String fileDir) {
        this.mFileDir = fileDir;
    }

    /**
     * set file name
     */
    void setFileName(String fileName) {
        this.mFileName = fileName;
    }

    // getters

    /**
     * get file url
     *
     * @return file url
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     * get file size
     *
     * @return file size
     */
    public int getFileSize() {
        return mFileSize;
    }

    /**
     * get file ETag
     *
     * @return file ETag
     */
    public String getETag() {
        return mETag;
    }

    /**
     * get AcceptRangeType
     *
     * @return AcceptRangeType
     */
    public String getAcceptRangeType() {
        return mAcceptRangeType;
    }

    /**
     * get SaveFileDir
     *
     * @return SaveFileDir
     */
    public String getFileDir() {
        return mFileDir;
    }

    /**
     * get SaveFileName
     *
     * @return SaveFileName
     */
    public String getFileName() {
        return mFileName;
    }

    /**
     * get FilePath
     *
     * @return FilePath
     */
    public String getFilePath() {
        return getFileDir() + File.separator + mFileName;
    }
}
