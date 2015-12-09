package org.wlf.filedownloader_demo2.custom_model;

import android.content.Context;
import android.text.TextUtils;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloadManager;
import org.wlf.filedownloader.util.CollectionUtil;

import java.util.List;

/**
 * the custom model about the network resource(this example is an video play info)
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
    private void setDownloadFileInfo(DownloadFileInfo downloadFileInfo) {
        mDownloadFileInfo = downloadFileInfo;
    }

    /**
     * whether the field DownloadFile is init
     *
     * @return
     */
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

    /**
     * sync customVideoInfos with the given downloadFileInfo
     *
     * @param customVideoInfos
     * @param downloadFileInfo
     * @return
     */
    public static boolean syncCustomVideo(List<CustomVideoInfo> customVideoInfos, DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null || TextUtils.isEmpty(downloadFileInfo.getUrl())) {
            return false;
        }

        boolean hasSync = false;

        for (CustomVideoInfo customVideoInfo : customVideoInfos) {
            if (customVideoInfo == null) {
                continue;
            }
            if (downloadFileInfo.getUrl().equals(customVideoInfo.getUrl())) {
                // find
                if (!customVideoInfo.isInitDownloadFileInfo()) {
                    customVideoInfo.setDownloadFileInfo(downloadFileInfo);
                    hasSync = true;
                }
                break;
            }
        }

        return hasSync;
    }

    /**
     * sync customVideoInfo with the downloadFileInfo that the url is the same with customVideoInfo
     *
     * @param context
     * @param customVideoInfo
     * @return
     */
    public static boolean syncCustomVideo(Context context, CustomVideoInfo customVideoInfo) {
        if (customVideoInfo == null || TextUtils.isEmpty(customVideoInfo.getUrl()) || customVideoInfo
                .isInitDownloadFileInfo()) {
            return false;
        }

        DownloadFileInfo downloadFileInfo = FileDownloadManager.getInstance(context).getDownloadFileByUrl
                (customVideoInfo.getUrl());
        if (downloadFileInfo == null) {
            return false;
        }

        // init DownloadFileInfo,and need keep the CustomVideoInfo that contact with DownloadFileInfo
        customVideoInfo.setDownloadFileInfo(downloadFileInfo);
        return true;
    }

    /**
     * sync customVideoInfos with downloadFileInfos
     *
     * @param context
     * @param customVideoInfos
     * @return
     */
    public static boolean syncCustomVideos(Context context, List<CustomVideoInfo> customVideoInfos) {

        if (CollectionUtil.isEmpty(customVideoInfos)) {
            return false;
        }

        boolean hasSync = false;

        for (CustomVideoInfo customVideoInfo : customVideoInfos) {
            if (!hasSync) {
                hasSync = syncCustomVideo(context, customVideoInfo);
            } else {
                syncCustomVideo(context, customVideoInfo);
            }
        }

        return hasSync;
    }

}
