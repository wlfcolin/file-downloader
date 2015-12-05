package org.wlf.filedownloader_demo2;

import android.text.TextUtils;

import org.wlf.filedownloader.DownloadFileInfo;

/**
 * the custom model about the network resource
 *
 * @author wlf(Andy)
 * @datetime 2015-12-05 10:15 GMT+8
 * @email 411086563@qq.com
 */
public class CustomVideoInfo {

    private Integer mId;//the id of the video
    private String mUrl;//the url of the video
    private String mStartTime;//the start time of the video,yyyy-MM-dd HH:mm:ss
    private String mEndTime;//the end time of the video,yyyy-MM-dd HH:mm:ss

    private DownloadFileInfo mDownloadFileInfo;

    public CustomVideoInfo(Integer id, String url, String startTime, String endTime) {
        mId = id;
        mUrl = url;
        mStartTime = startTime;
        mEndTime = endTime;
    }

    public boolean isInitDownloadFileInfo() {
        if (mDownloadFileInfo == null) {
            return false;
        }

        if (TextUtils.isEmpty(mDownloadFileInfo.getUrl())) {
            return false;
        }

        if (!mDownloadFileInfo.getUrl().equals(mUrl)) {
            return false;
        }

        return true;
    }

    // getters
    public Integer getId() {
        return mId;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getStartTime() {
        return mStartTime;
    }

    public String getEndTime() {
        return mEndTime;
    }

    public DownloadFileInfo getDownloadFileInfo() {
        return mDownloadFileInfo;
    }

    // setters
    public void setDownloadFileInfo(DownloadFileInfo downloadFileInfo) {
        mDownloadFileInfo = downloadFileInfo;
    }
}
