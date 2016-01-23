package org.wlf.filedownloader_demo2.custom_model;

import android.util.Log;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloader;
import org.wlf.filedownloader.listener.OnDownloadFileChangeListener;

/**
 * the custom model about the network resource(this example is an video play info)
 *
 * @author wlf(Andy)
 * @datetime 2015-12-05 10:15 GMT+8
 * @email 411086563@qq.com
 */
public class CustomVideoInfo implements OnDownloadFileChangeListener {

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

        init();
    }

    /**
     * init resources
     */
    private void init() {
        // register DownloadFileChangeListener
        FileDownloader.registerDownloadFileChangeListener(this);
        // init DownloadFileInfo if has been downloaded
        mDownloadFileInfo = FileDownloader.getDownloadFile(mUrl);
    }

    /**
     * release resources
     */
    public void release() {
        // unregister
        FileDownloader.unregisterDownloadFileChangeListener(this);
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

    @Override
    public void onDownloadFileCreated(DownloadFileInfo downloadFileInfo) {
        if (downloadFileInfo != null && downloadFileInfo.getUrl() != null && downloadFileInfo.getUrl().equals(mUrl)) {
            this.mDownloadFileInfo = downloadFileInfo;
            Log.e("wlf", "onDownloadFileCreated,downloadFileInfo:" + downloadFileInfo);
        }
    }

    @Override
    public void onDownloadFileUpdated(DownloadFileInfo downloadFileInfo, Type type) {
        if (downloadFileInfo != null && downloadFileInfo.getUrl() != null && downloadFileInfo.getUrl().equals(mUrl)) {
            this.mDownloadFileInfo = downloadFileInfo;
            Log.e("wlf", "onDownloadFileUpdated,downloadFileInfo:" + downloadFileInfo);
        }
    }

    @Override
    public void onDownloadFileDeleted(DownloadFileInfo downloadFileInfo) {
        if (downloadFileInfo != null && downloadFileInfo.getUrl() != null && downloadFileInfo.getUrl().equals(mUrl)) {
            this.mDownloadFileInfo = null;
            Log.e("wlf", "onDownloadFileDeleted,downloadFileInfo:" + downloadFileInfo);
        }
    }
}
