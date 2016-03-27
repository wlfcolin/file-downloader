package org.wlf.filedownloader.file_download;

import android.text.TextUtils;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.file_download.base.DownloadRecorder;
import org.wlf.filedownloader.listener.OnDetectBigUrlFileListener;
import org.wlf.filedownloader.listener.OnDetectBigUrlFileListener.DetectBigUrlFileFailReason;
import org.wlf.filedownloader.listener.OnDetectUrlFileListener.DetectUrlFileFailReason;
import org.wlf.filedownloader.util.UrlUtil;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.ExecutorService;

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

    private static final int MAX_REDIRECT_TIMES = 5;
    private static final int DEFAULT_CONNECT_TIMEOUT = 15 * 1000;// 15s
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String DEFAULT_REQUEST_METHOD = "GET";// GET default

    private String mUrl;
    private String mDownloadSaveDir;
    private int mConnectTimeout = DEFAULT_CONNECT_TIMEOUT;// connect time out, millisecond
    private String mCharset = DEFAULT_CHARSET;// FIXME now UTF-8 only
    private String mRequestMethod = DEFAULT_REQUEST_METHOD;
    private Map<String, String> mHeaders;//custom  headers

    private DetectUrlFileCacher mDetectUrlFileCacher;
    private DownloadRecorder mDownloadRecorder;
    private OnDetectBigUrlFileListener mOnDetectBigUrlFileListener;

    // if it is true, that means force detect without check whether the file is downloaded
    private boolean mIsForceDetect = false;

    private ExecutorService mCloseConnectionEngine;// engine use for closing the http connection

    public DetectUrlFileTask(String url, String downloadSaveDir, DetectUrlFileCacher detectUrlFileCacher, 
                             DownloadRecorder downloadRecorder) {
        super();
        this.mUrl = url;
        this.mDownloadSaveDir = downloadSaveDir;
        this.mDetectUrlFileCacher = detectUrlFileCacher;
        this.mDownloadRecorder = downloadRecorder;
    }

    /**
     * set OnDetectBigUrlFileListener
     *
     * @param onDetectBigUrlFileListener OnDetectBigUrlFileListener impl
     */
    public void setOnDetectBigUrlFileListener(OnDetectBigUrlFileListener onDetectBigUrlFileListener) {
        this.mOnDetectBigUrlFileListener = onDetectBigUrlFileListener;
    }

    /**
     * set CloseConnectionEngine
     *
     * @param closeConnectionEngine CloseConnectionEngine
     */
    public void setCloseConnectionEngine(ExecutorService closeConnectionEngine) {
        mCloseConnectionEngine = closeConnectionEngine;
    }

    /**
     * set connect timeout
     *
     * @param connectTimeout connect timeout
     */
    public void setConnectTimeout(int connectTimeout) {
        mConnectTimeout = connectTimeout;
    }

    /**
     * set request method
     *
     * @param requestMethod request method
     */
    public void setRequestMethod(String requestMethod) {
        this.mRequestMethod = requestMethod;
    }

    /**
     * set custom headers
     *
     * @param headers custom headers
     */
    public void setHeaders(Map<String, String> headers) {
        mHeaders = headers;
    }

    /**
     * enable force detect
     */
    public void enableForceDetect() {
        mIsForceDetect = true;
    }

    @Override
    public void run() {

        /**
         * detect logic
         *
         * 1.check illegal conditions such as url illegal, network error and so on
         * 2.if url file detected and not force detect flag, ignore, make sure do not detect duplicate
         * 3.check illegal response status such as response code is not 20X
         * 4.rename save file name in database
         * 5.create DetectUrlFileInfo
         *
         * note: DetectUrlFileFailReason extends from DetectBigUrlFileFailReason, so they are compatible
         */

        HttpURLConnection conn = null;
        InputStream inputStream = null;

        DownloadFileInfo downloadFileInfo = null;
        DetectUrlFileInfo detectUrlFileInfo = null;
        DetectUrlFileFailReason failReason = null;

        try {
            // ------------start checking conditions------------
            {
                // check URL
                if (!UrlUtil.isUrl(mUrl)) {
                    // error, url illegal
                    failReason = new DetectUrlFileFailReason(mUrl, "url illegal !", DetectUrlFileFailReason
                            .TYPE_URL_ILLEGAL);
                    // goto finally, url error
                    return;
                }

                // network check by caller

                downloadFileInfo = mDownloadRecorder.getDownloadFile(mUrl);
                if (downloadFileInfo != null && !mIsForceDetect) {
                    // goto finally, download file exist
                    return;
                }
            }
            // ------------end checking conditions------------

            String connectUrl = mUrl;

            conn = HttpConnectionHelper.createDetectConnection(connectUrl, mConnectTimeout, mCharset, mRequestMethod,
                    mHeaders);

            int redirectCount = 0;
            while (conn.getResponseCode() / 100 == 3 && redirectCount < MAX_REDIRECT_TIMES) {
                connectUrl = conn.getHeaderField("Location");
                conn = HttpConnectionHelper.createDetectConnection(connectUrl, mConnectTimeout, mCharset, mRequestMethod, mHeaders);
                redirectCount++;
            }

            Log.d(TAG, TAG + ".run 探测文件，重定向：" + redirectCount + "次" + "，最大重定向次数：" + MAX_REDIRECT_TIMES +
                    "，url：" + mUrl);

            if (redirectCount > MAX_REDIRECT_TIMES) {
                // error over max redirect
                failReason = new DetectUrlFileFailReason(mUrl, "over max redirect:" + MAX_REDIRECT_TIMES + "!", 
                        DetectUrlFileFailReason.TYPE_URL_OVER_REDIRECT_COUNT);
                // goto finally, over redirect limit error
                return;
            }

            Log.i(TAG, TAG + ".run Response Headers:" + HttpConnectionHelper.getStringHeaders(conn.getHeaderFields()));

            switch (conn.getResponseCode()) {
                // http ok
                case HttpURLConnection.HTTP_OK:

                    // get file name
                    String fileName = HttpConnectionHelper.getFileNameFromResponseHeader(conn.getHeaderFields());
                    if (TextUtils.isEmpty(fileName)) {
                        // get from url
                        fileName = UrlUtil.getFileNameByUrl(connectUrl, mCharset);
                    } else {
                        // check is decoded
                        if (UrlUtil.isEncoded(fileName, mCharset)) {
                            fileName = UrlUtil.decode(fileName, mCharset);
                        }
                    }

                    // get eTag
                    String eTag = conn.getHeaderField("ETag");

                    // get acceptRangeType,bytes usually if supported range transmission
                    String acceptRangeType = conn.getHeaderField("Accept-Ranges");

                    // String lastModified = conn.getLastModified() + "";
                    String lastModified = HttpConnectionHelper.getLastModifiedFromResponseHeader(conn.getHeaderFields
                            ());

                    // get file size
                    long fileSize = conn.getContentLength();
                    if (fileSize <= 0) {
                        fileSize = HttpConnectionHelper.getFileSizeFromResponseHeader(conn.getHeaderFields());
                    }

                    if (fileSize > 0) {
                        detectUrlFileInfo = new DetectUrlFileInfo(mUrl, fileSize, eTag, lastModified, 
                                acceptRangeType, mDownloadSaveDir, fileName);
                        // add or update to memory cache
                        mDetectUrlFileCacher.addOrUpdateDetectUrlFile(detectUrlFileInfo);
                        // goto finally, the detectUrlFileInfo created
                        return;
                    }
                    break;
                // 404 not found
                case HttpURLConnection.HTTP_NOT_FOUND:
                    // error url file does not exist
                    failReason = new DetectUrlFileFailReason(mUrl, "url file does not exist !", 
                            DetectUrlFileFailReason.TYPE_HTTP_FILE_NOT_EXIST);
                    break;
                // other, ResponseCode error
                default:
                    // error ResponseCode error
                    failReason = new DetectUrlFileFailReason(mUrl, "ResponseCode:" + conn.getResponseCode() + " " +
                            "error, can not read data !", DetectUrlFileFailReason.TYPE_BAD_HTTP_RESPONSE_CODE);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            // cast Exception to DetectUrlFileFailReason
            failReason = new DetectUrlFileFailReason(mUrl, e);
        } finally {
            // close inputStream & url connection
            CloseConnectionTask closeConnectionTask = new CloseConnectionTask(conn, inputStream);
            if (mCloseConnectionEngine != null) {
                mCloseConnectionEngine.execute(closeConnectionTask);
            } else {
                closeConnectionTask.run();
            }

            boolean isNotify = false;

            // ------------start notifying caller------------
            {
                // notify file exist
                if (downloadFileInfo != null && !mIsForceDetect) {
                    // in the download file database, notify
                    isNotify = notifyDetectUrlFileExist();
                }

                // detect new one
                if (!isNotify && detectUrlFileInfo != null) {
                    // delete the old one and tell the caller that need to create new download file
                    try {
                        mDownloadRecorder.resetDownloadFile(mUrl, true);
                        isNotify = notifyDetectNewDownloadFile(detectUrlFileInfo.getFileName(), detectUrlFileInfo
                                .getFileDir(), detectUrlFileInfo.getFileSizeLong());
                    } catch (Exception e) {
                        e.printStackTrace();
                        failReason = new DetectUrlFileFailReason(mUrl, e);
                    }
                }

                if (!isNotify) {
                    if (failReason == null) {
                        failReason = new DetectUrlFileFailReason(mUrl, "the file need to download may not access !", 
                                DetectUrlFileFailReason.TYPE_UNKNOWN);
                    }
                    // error occur
                    isNotify = notifyDetectUrlFileFailed(failReason);
                }
            }
            // ------------end notifying caller------------

            boolean hasException = failReason == null ? true : false;

            Log.d(TAG, TAG + ".run 探测文件任务【已结束】，是否有异常：" + hasException + "，是否强制探测模式：" + mIsForceDetect +
                    "，是否成功通知了调用者（如果没有设置监听者认为没有通知成功）：" + isNotify + "，url：" + mUrl);
        }
    }

    // --------------------------------------notify caller--------------------------------------

    /**
     * notifyDetectUrlFileExist
     */
    private boolean notifyDetectUrlFileExist() {

        Log.d(TAG, "探测文件已存在，url：" + mUrl);

        if (mOnDetectBigUrlFileListener != null) {
            // main thread notify caller
            OnDetectBigUrlFileListener.MainThreadHelper.onDetectUrlFileExist(mUrl, mOnDetectBigUrlFileListener);
            // return true;
        }
        // force return true
        return true;
    }

    /**
     * notifyDetectNewDownloadFile
     */
    private boolean notifyDetectNewDownloadFile(String fileName, String saveDir, long fileSize) {

        Log.d(TAG, "探测文件不存在，需要创建，url：" + mUrl);

        if (mOnDetectBigUrlFileListener != null) {
            // main thread notify caller
            OnDetectBigUrlFileListener.MainThreadHelper.onDetectNewDownloadFile(mUrl, fileName, saveDir, fileSize, 
                    mOnDetectBigUrlFileListener);
            // return true;
        }
        // force return true
        return true;
    }

    /**
     * notifyDetectUrlFileFailed
     */
    private boolean notifyDetectUrlFileFailed(DetectBigUrlFileFailReason failReason) {

        Log.d(TAG, "探测文件失败，url：" + mUrl);

        if (mOnDetectBigUrlFileListener != null) {
            // main thread notify caller
            OnDetectBigUrlFileListener.MainThreadHelper.onDetectUrlFileFailed(mUrl, failReason, 
                    mOnDetectBigUrlFileListener);
            // return true;
        }
        // force return true
        return true;
    }
}
