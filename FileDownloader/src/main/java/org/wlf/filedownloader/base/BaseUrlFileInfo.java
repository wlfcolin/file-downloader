package org.wlf.filedownloader.base;

import java.io.File;

/**
 * BaseUrlFile Info
 *
 * @author wlf(Andy)
 * @datetime 2015-11-25 09:58 GMT+8
 * @email 411086563@qq.com
 */
public abstract class BaseUrlFileInfo {

    /**
     * support range of bytes
     */
    public static final String RANGE_TYPE_BYTES = "bytes";

    /**
     * file url
     */
    protected String mUrl;
    /**
     * file total size
     */
    protected long mFileSize;
    /**
     * file eTag
     */
    protected String mETag;
    /**
     * file last modified datetime(in server)
     */
    protected String mLastModified;
    /**
     * accept range type
     */
    protected String mAcceptRangeType;
    /**
     * save file dir
     */
    protected String mFileDir;
    /**
     * save file name
     */
    protected String mFileName;
    /**
     * create download datetime, yyyy-MM-dd HH:mm:ss
     */
    protected String mCreateDatetime;

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
     * @deprecated use {@link #getFileSizeLong} instead
     */
    @Deprecated
    public int getFileSize() {
        return (int) mFileSize;
    }

    /**
     * get file size
     *
     * @return file size
     */
    public long getFileSizeLong() {
        return mFileSize;
    }

    /**
     * get file eTag
     *
     * @return file eTag
     */
    public String getETag() {
        return mETag;
    }

    /**
     * get file last modified datetime(in server)
     *
     * @return file last modified datetime(in server)
     */
    public String getLastModified() {
        return mLastModified;
    }

    /**
     * get accept range type
     *
     * @return accept range type
     */
    public String getAcceptRangeType() {
        return mAcceptRangeType;
    }

    /**
     * get save file dir
     *
     * @return save file dir
     */
    public String getFileDir() {
        return mFileDir;
    }

    /**
     * get save file name
     *
     * @return save file name
     */
    public String getFileName() {
        return mFileName;
    }

    /**
     * get create download datetime
     *
     * @return create download datetime
     */
    public String getCreateDatetime() {
        return mCreateDatetime;
    }

    // other getters

    /**
     * get file path
     *
     * @return file path
     */
    public String getFilePath() {
        return getFileDir() + File.separator + mFileName;
    }

    @Override
    public String toString() {
        return "BaseUrlFileInfo{" +
                "mUrl='" + mUrl + '\'' +
                ", mFileSize=" + mFileSize +
                ", mETag='" + mETag + '\'' +
                ", mLastModified='" + mLastModified + '\'' +
                ", mAcceptRangeType='" + mAcceptRangeType + '\'' +
                ", mFileDir='" + mFileDir + '\'' +
                ", mFileName='" + mFileName + '\'' +
                ", mCreateDatetime='" + mCreateDatetime + '\'' +
                '}';
    }
}
