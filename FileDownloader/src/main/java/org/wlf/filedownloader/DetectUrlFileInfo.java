package org.wlf.filedownloader;

import android.text.TextUtils;

import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.io.File;

/**
 * DetectUrlFileInfo
 * <br/>
 * 探测到的网络文件信息
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DetectUrlFileInfo {

    /**
     * Support Range of bytes
     */
    public static final String RANGE_TYPE_BYTES = "bytes";

    /**
     * file url
     */
    private String mUrl;
    /**
     * file total size
     */
    private int mfFileSize;
    /**
     * file eTag
     */
    private String mETag;
    /**
     * AcceptRangeType
     */
    private String mAcceptRangeType;
    /**
     * SaveFileDir
     */
    private String mFileDir;
    /**
     * SaveFileName
     */
    private String mFileName;
    /**
     * TempFileName
     */
    private String mTempFileName;

    public DetectUrlFileInfo(String url, int fileSize, String eTag, String acceptRangeType, String fileDir, String fileName) {
        super();
        this.mUrl = url;
        this.mfFileSize = fileSize;
        this.mETag = eTag;
        this.mAcceptRangeType = acceptRangeType;
        this.mFileDir = fileDir;
        this.mFileName = fileName;
    }

    // 限制包内可访问

    /**
     * 更新多个字段值
     *
     * @param detectUrlFileInfo
     */
    void update(DetectUrlFileInfo detectUrlFileInfo) {
        if (UrlUtil.isUrl(detectUrlFileInfo.mUrl)) {
            this.mUrl = detectUrlFileInfo.mUrl;
        }
        if (detectUrlFileInfo.mfFileSize > 0 && detectUrlFileInfo.mfFileSize != this.mfFileSize) {
            this.mfFileSize = detectUrlFileInfo.mfFileSize;
        }
        if (!TextUtils.isEmpty(detectUrlFileInfo.mETag)) {
            this.mETag = detectUrlFileInfo.mETag;
        }
        if (!TextUtils.isEmpty(detectUrlFileInfo.mAcceptRangeType)) {
            this.mAcceptRangeType = detectUrlFileInfo.mAcceptRangeType;
        }
        if (FileUtil.isFilePath(detectUrlFileInfo.mFileDir)) {
            this.mFileDir = detectUrlFileInfo.mFileDir;
        }
        if (!TextUtils.isEmpty(detectUrlFileInfo.mFileName)) {
            this.mFileName = detectUrlFileInfo.mFileName;
        }
        if (!TextUtils.isEmpty(detectUrlFileInfo.mTempFileName)) {
            this.mTempFileName = detectUrlFileInfo.mTempFileName;
        }
    }

    // setters，全部限制包内可访问
    void setFileDir(String fileDir) {
        this.mFileDir = fileDir;
    }

    void setFileName(String fileName) {
        this.mFileName = fileName;
    }

    // getters
    public String getUrl() {
        return mUrl;
    }

    public int getFileSize() {
        return mfFileSize;
    }

    public String geteTag() {
        return mETag;
    }

    public String getAcceptRangeType() {
        return mAcceptRangeType;
    }

    public String getFileDir() {
        return mFileDir;
    }

    public String getFileName() {
        return mFileName;
    }

    // 特殊getter

    /**
     * 获取临时下载文件路径
     *
     * @return 临时下载文件路径
     */
    public String getTempFilePath() {
        return getFileDir() + File.separator + mTempFileName;
    }

    /**
     * 获取下载文件路径
     *
     * @return 下载文件路径
     */
    public String getFilePath() {
        return getFileDir() + File.separator + mFileName;
    }
}
