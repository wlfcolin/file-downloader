package org.wlf.filedownloader.file_download;

import android.text.TextUtils;
import android.util.Log;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.file_download.DownloadTaskManager.DownloadRecorder;
import org.wlf.filedownloader.listener.OnDetectUrlFileListener;
import org.wlf.filedownloader.listener.OnDetectUrlFileListener.DetectUrlFileFailReason;
import org.wlf.filedownloader.util.UrlUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * DetectUrlFile Task
 * <br/>
 * 探测网络文件任务
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
class DetectUrlFileTask implements Runnable {

    private static final String TAG = DetectUrlFileTask.class.getSimpleName();

    private static final int MAX_REDIRECT_COUNT = 5;
    private static final int CONNECT_TIMEOUT = 15 * 1000;// 15s
    private static final String CHARSET = "UTF-8";

    private String mUrl;
    private String mDownloadSaveDir;
    private DetectUrlFileCacher mDetectUrlFileCacher;
    private DownloadRecorder mDownloadRecorder;
    private OnDetectUrlFileListener mOnDetectUrlFileListener;

    public DetectUrlFileTask(String url, String downloadSaveDir, DetectUrlFileCacher detectUrlFileCacher, 
                             DownloadRecorder downloadRecorder) {
        super();
        this.mUrl = url;
        this.mDownloadSaveDir = downloadSaveDir;
        this.mDetectUrlFileCacher = detectUrlFileCacher;
        this.mDownloadRecorder = downloadRecorder;
    }

    /**
     * set DetectUrlFileListener
     *
     * @param onDetectUrlFileListener DetectUrlFileListener
     */
    public void setOnDetectUrlFileListener(OnDetectUrlFileListener onDetectUrlFileListener) {
        this.mOnDetectUrlFileListener = onDetectUrlFileListener;
    }

    @Override
    public void run() {

        HttpURLConnection conn = null;
        InputStream inputStream = null;

        DetectUrlFileFailReason failReason = null;

        try {
            // check URL
            if (!UrlUtil.isUrl(mUrl)) {
                // error url illegal
                failReason = new DetectUrlFileFailReason("url illegal!", DetectUrlFileFailReason.TYPE_URL_ILLEGAL);
            }
            // URL legal
            else {
                DownloadFileInfo downloadFileInfo = mDownloadRecorder.getDownloadFile(mUrl);
                if (downloadFileInfo != null) {
                    // 1.in the download file database,notify
                    if (mOnDetectUrlFileListener != null) {
                        OnDetectUrlFileListener.MainThreadHelper.onDetectUrlFileExist(mUrl, mOnDetectUrlFileListener);
                    }
                    return;
                }

                conn = HttpConnectionHelper.createDetectConnection(mUrl, CONNECT_TIMEOUT, CHARSET);

                int redirectCount = 0;
                while (conn.getResponseCode() / 100 == 3 && redirectCount < MAX_REDIRECT_COUNT) {
                    conn = HttpConnectionHelper.createDetectConnection(mUrl, CONNECT_TIMEOUT, CHARSET);
                    redirectCount++;
                }

                Log.d(TAG, "DetectUrlFileTask.run 探测文件，重定向：" + redirectCount + "次" + "，最大重定向次数：" + MAX_REDIRECT_COUNT
                        + "，url：" + mUrl);

                if (redirectCount > MAX_REDIRECT_COUNT) {
                    // error over max redirect
                    failReason = new DetectUrlFileFailReason("over max redirect:" + MAX_REDIRECT_COUNT + "!", 
                            DetectUrlFileFailReason.TYPE_URL_OVER_REDIRECT_COUNT);
                } else {
                    switch (conn.getResponseCode()) {
                        // http ok
                        case HttpURLConnection.HTTP_OK:
                            // get file name
                            String fileName = UrlUtil.getFileNameByUrl(mUrl);
                            // get file size
                            long fileSize = conn.getContentLength();
                            // get etag
                            String eTag = conn.getHeaderField("ETag");
                            // get acceptRangeType,bytes usually if supported range transmission
                            String acceptRangeType = conn.getHeaderField("Accept-Ranges");
                            if (fileSize <= 0) {
                                String contentLengthStr = conn.getHeaderField("Content-Length");
                                if (!TextUtils.isEmpty(contentLengthStr)) {
                                    fileSize = Long.parseLong(contentLengthStr);
                                }
                            }

                            DetectUrlFileInfo detectUrlFileInfo = new DetectUrlFileInfo(mUrl, fileSize, eTag, 
                                    acceptRangeType, mDownloadSaveDir, fileName);
                            // add to memory cache
                            mDetectUrlFileCacher.addOrUpdateDetectUrlFile(detectUrlFileInfo);

                            // 2.need to create download file
                            if (mOnDetectUrlFileListener != null) {
                                OnDetectUrlFileListener.MainThreadHelper.onDetectNewDownloadFile(mUrl, fileName, 
                                        mDownloadSaveDir, fileSize, mOnDetectUrlFileListener);
                            }
                            break;
                        // 404 not found
                        case HttpURLConnection.HTTP_NOT_FOUND:
                            // error url file does not exist
                            failReason = new DetectUrlFileFailReason("url file does not exist!", 
                                    DetectUrlFileFailReason.TYPE_HTTP_FILE_NOT_EXIST);
                            break;
                        // other,ResponseCode error
                        default:
                            // error ResponseCode error
                            failReason = new DetectUrlFileFailReason("ResponseCode:" + conn.getResponseCode() + " " +
                                    "error,can not read data!", DetectUrlFileFailReason.TYPE_BAD_HTTP_RESPONSE_CODE);
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            failReason = new DetectUrlFileFailReason(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
            // error occur
            if (failReason != null) {
                if (mOnDetectUrlFileListener != null) {
                    OnDetectUrlFileListener.MainThreadHelper.onDetectUrlFileFailed(mUrl, failReason, 
                            mOnDetectUrlFileListener);
                }
            }
        }
    }
}
