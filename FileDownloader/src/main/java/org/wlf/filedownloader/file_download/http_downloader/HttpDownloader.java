package org.wlf.filedownloader.file_download.http_downloader;

import android.text.TextUtils;

import org.wlf.filedownloader.base.FailReason;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.file_download.CloseConnectionTask;
import org.wlf.filedownloader.file_download.HttpConnectionHelper;
import org.wlf.filedownloader.file_download.HttpConnectionHelper.RequestParam;
import org.wlf.filedownloader.file_download.base.HttpFailReason;
import org.wlf.filedownloader.file_download.file_saver.FileSaver.FileSaveException;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * http file download impl
 * <br/>
 * Http下载器
 *
 * @author wlf
 * @email 411086563@qq.com
 */
public class HttpDownloader implements Download {

    private static final String TAG = HttpDownloader.class.getSimpleName();

    private static final int MAX_REDIRECT_TIMES = 5;
    private static final int CONNECT_TIMEOUT = 15 * 1000;// 15s default
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String DEFAULT_REQUEST_METHOD = "GET";// GET default

    private String mUrl;// download url
    private Range mRange;// download data range
    private String mAcceptRangeType;// accept range type
    private String mETag;// http file eTag
    private String mLastModified;// file last modified time
    private int mConnectTimeout = CONNECT_TIMEOUT;// connect time out, millisecond
    private String mCharset = DEFAULT_CHARSET;// FIXME now UTF-8 only
    private String mRequestMethod = DEFAULT_REQUEST_METHOD;
    private Map<String, String> mHeaders;//custom  headers

    private ExecutorService mCloseConnectionEngine;// engine use for closing the http connection

    private OnHttpDownloadListener mOnHttpDownloadListener;
    private OnRangeChangeListener mOnRangeChangeListener;

    //    /**
    //     * constructor of HttpDownloader,the range is from the beginning to the end of the url file
    //     *
    //     * @param url url path
    //     */
    //    public HttpDownloader(String url) {
    //        this(url, null, null);
    //    }

    //    /**
    //     * constructor of HttpDownloader,will check range and acceptRangeType
    //     *
    //     * @param url             url path
    //     * @param range           data range
    //     * @param acceptRangeType accept range type
    //     */
    //    public HttpDownloader(String url, Range range, String acceptRangeType) {
    //        this(url, range, acceptRangeType, null);
    //    }

    /**
     * constructor of HttpDownloader,will check range,acceptRangeType and eTag
     *
     * @param url             url path
     * @param range           data range
     * @param acceptRangeType accept range type
     * @param eTag            file eTag
     * @param lastModified    last modified datetime(in server)
     */
    public HttpDownloader(String url, Range range, String acceptRangeType, String eTag, String lastModified) {
        this.mUrl = url;
        this.mRange = range;
        this.mAcceptRangeType = acceptRangeType;
        this.mETag = eTag;
        this.mLastModified = lastModified;
    }

    /**
     * set OnHttpDownloadListener
     *
     * @param onHttpDownloadListener OnHttpDownloadListener
     */
    public void setOnHttpDownloadListener(OnHttpDownloadListener onHttpDownloadListener) {
        this.mOnHttpDownloadListener = onHttpDownloadListener;
    }

    /**
     * set OnRangeChangeListener
     *
     * @param onRangeChangeListener OnRangeChangeListener
     */
    public void setOnRangeChangeListener(OnRangeChangeListener onRangeChangeListener) {
        mOnRangeChangeListener = onRangeChangeListener;
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
     * set connect timeout
     *
     * @param connectTimeout connect timeout
     */
    public void setConnectTimeout(int connectTimeout) {
        mConnectTimeout = connectTimeout;
    }

    /**
     * if it throw HttpDownloadException,that means download data failed(error occur)
     */
    @Override
    public void download() throws HttpDownloadException {

        boolean hasException = true;

        String url = mUrl;// url

        HttpURLConnection conn = null;
        ContentLengthInputStream inputStream = null;

        try {
            RequestParam requestParam = new RequestParam(url, mConnectTimeout, mCharset, mRange.startPos, mRange
                    .endPos, mETag, mLastModified);
            requestParam.setRequestMethod(mRequestMethod);
            requestParam.setHeaders(mHeaders);

            conn = HttpConnectionHelper.createDownloadFileConnection(requestParam);

            int redirectTimes = 0;
            while (conn != null && conn.getResponseCode() / 100 == 3 && redirectTimes < MAX_REDIRECT_TIMES) {// redirect
                requestParam.setUrl(conn.getHeaderField("Location"));
                conn = HttpConnectionHelper.createDownloadFileConnection(requestParam);
                redirectTimes++;
            }

            Log.d(TAG, TAG + ".download 1、准备下载，重定向：" + redirectTimes + "次" + "，最大重定向次数：" + MAX_REDIRECT_TIMES +
                    "，url：" + url);

            if (redirectTimes > MAX_REDIRECT_TIMES) {
                // error over max redirect times
                throw new HttpDownloadException(url, "over max redirect:" + MAX_REDIRECT_TIMES + "!", 
                        HttpDownloadException.TYPE_REDIRECT_COUNT_OVER_LIMITS);
            }

            if (conn == null) {
                throw new HttpDownloadException(url, "the connection is null:" + MAX_REDIRECT_TIMES + "!", 
                        HttpDownloadException.TYPE_NULL_POINTER);
            }

            Log.i(TAG, TAG + ".download Response Headers:" + HttpConnectionHelper.getStringHeaders(conn
                    .getHeaderFields()));

            // 1.check ResponseCode
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {

                // 2.check contentLength
                long contentLength = conn.getContentLength();

                if (contentLength <= 0) {
                    // get contentLength by header
                    contentLength = HttpConnectionHelper.getFileSizeFromResponseHeader(conn.getHeaderFields());
                }

                Log.d(TAG, TAG + ".download 2、得到服务器返回的资源contentLength：" + contentLength + "，传入的range：" + mRange
                        .toString() + "，url：" + url);

                if (contentLength <= 0) {
                    // error content length illegal
                    throw new HttpDownloadException(url, "content length illegal,get url file failed!", 
                            HttpDownloadException.TYPE_RESOURCES_SIZE_ILLEGAL);
                }

                // not partial range, that means the whole data
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // handle whole data
                    if (!Range.isLegal(mRange) || (mRange != null && (mRange.startPos != 0 || mRange.getLength() != 
                            contentLength))) {
                        Range newRange = new Range(0, contentLength);
                        // need notify caller
                        boolean continueDownload = notifyRangeChanged(mRange, newRange);
                        if (continueDownload) {
                            mRange = new Range(newRange.startPos, newRange.endPos);
                        } else {
                            throw new HttpDownloadException(url, "contentRange validate failed!", 
                                    HttpDownloadException.TYPE_CONTENT_RANGE_VALIDATE_FAIL);
                        }
                    } else {
                        // do not need notify caller
                        mRange = new Range(0, contentLength);
                    }
                }
                // partial range, that means range of the data
                else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                    // 3.check eTag(whether file is changed)
                    if (!TextUtils.isEmpty(mETag)) {
                        String eTag = conn.getHeaderField("ETag");

                        Log.d(TAG, TAG + ".download 3、得到服务器返回的资源eTag：" + eTag + "，传入的eTag：" + mETag + "，url：" + url);

                        if (TextUtils.isEmpty(eTag) || !mETag.equals(eTag)) {
                            // error eTag is not equal
                            throw new HttpDownloadException(url, "eTag is not equal,please delete the old one then " 
                                    + "re-download!", HttpDownloadException.TYPE_ETAG_CHANGED);
                        }
                    }
                    if (!Range.isLegal(mRange) || (mRange != null && mRange.getLength() > contentLength)) {
                        Range newRange = new Range(0, contentLength);
                        // need notify caller
                        boolean continueDownload = notifyRangeChanged(mRange, newRange);
                        if (continueDownload) {
                            mRange = new Range(newRange.startPos, newRange.endPos);
                        } else {
                            throw new HttpDownloadException(url, "contentRange validate failed!", 
                                    HttpDownloadException.TYPE_CONTENT_RANGE_VALIDATE_FAIL);
                        }
                    }
                    // 4.check contentRange and acceptRangeType
                    if (mRange != null && !TextUtils.isEmpty(mAcceptRangeType)) {
                        boolean isRangeValidateSucceed = false;
                        String contentRange = conn.getHeaderField("Content-Range");
                        // get ContentRange
                        ContentRangeInfo contentRangeInfo = ContentRangeInfo.getContentRangeInfo(contentRange);
                        if (contentRangeInfo != null) {
                            Range serverResponseRange = new Range(contentRangeInfo.startPos, contentRangeInfo.endPos);
                            if (mRange.equals(serverResponseRange) && mAcceptRangeType.equals(contentRangeInfo
                                    .contentType) && serverResponseRange.getLength() == contentLength) {
                                // range validate pass
                                isRangeValidateSucceed = true;
                            }
                        }

                        if (!isRangeValidateSucceed) {
                            // error contentRange validate failed
                            throw new HttpDownloadException(url, "contentRange validate failed!", 
                                    HttpDownloadException.TYPE_CONTENT_RANGE_VALIDATE_FAIL);
                        }
                    }
                }

                // 3.check eTag(whether file is changed)
                if (!TextUtils.isEmpty(mETag)) {
                    String eTag = conn.getHeaderField("ETag");

                    Log.d(TAG, TAG + ".download 3、得到服务器返回的资源eTag：" + eTag + "，传入的eTag：" + mETag + "，url：" + url);

                    if (TextUtils.isEmpty(eTag) || !mETag.equals(eTag)) {
                        // error eTag is not equal
                        throw new HttpDownloadException(url, "eTag is not equal,please delete the old one then " + 
                                "re-download!", HttpDownloadException.TYPE_ETAG_CHANGED);
                    }
                }

                // get server InputStream
                InputStream serverInputStream = conn.getInputStream();
                // wrap serverInputStream by ContentLengthInputStream
                inputStream = new ContentLengthInputStream(serverInputStream, contentLength);

                Log.d(TAG, TAG + ".download 4、准备处理数据，获取服务器返回的资源长度为：" + contentLength + "，获取服务器返回的输入流长度为：" +
                        inputStream.available() + "，需要处理的区域为：" + mRange.toString() + "，url：" + url);

                // notifyDownloadConnected
                notifyDownloadConnected(inputStream, mRange.startPos);
            }
            // ResponseCode error
            else {
                // error ResponseCode error
                throw new HttpDownloadException(url, "ResponseCode:" + responseCode + " error,can not read server " +
                        "data!", HttpDownloadException.TYPE_RESPONSE_CODE_ERROR);
            }
            hasException = false;
        } catch (Exception e) {
            e.printStackTrace();
            hasException = true;
            if (e instanceof HttpDownloadException) {
                // HttpDownloadException
                throw (HttpDownloadException) e;
            } else {
                // other Exception
                throw new HttpDownloadException(url, e);
            }
        } finally {
            // close inputStream & url connection
            CloseConnectionTask closeConnectionTask = new CloseConnectionTask(conn, inputStream);
            if (mCloseConnectionEngine != null) {
                mCloseConnectionEngine.execute(closeConnectionTask);
            } else {
                closeConnectionTask.run();
            }

            Log.d(TAG, TAG + ".download 5、Http文件下载【已结束】，是否有异常：" + hasException + "，url：" + url);
        }

    }

    // --------------------------------------notify caller--------------------------------------

    // notifyRangeChanged
    private boolean notifyRangeChanged(Range oldRange, Range newRange) {
        if (mOnRangeChangeListener != null) {
            return mOnRangeChangeListener.onRangeChanged(oldRange, newRange);
        }
        return true;
    }

    // notifyDownloadConnected
    private void notifyDownloadConnected(ContentLengthInputStream inputStream, long startPosInTotal) {
        if (mOnHttpDownloadListener != null) {
            mOnHttpDownloadListener.onDownloadConnected(inputStream, startPosInTotal);
        }
    }

    /**
     * HttpDownloadException
     */
    public static class HttpDownloadException extends HttpFailReason {
        /**
         * http redirect times over limits
         */
        public static final String TYPE_REDIRECT_COUNT_OVER_LIMITS = HttpDownloadException.class.getName() + 
                "_TYPE_REDIRECT_COUNT_OVER_LIMITS";
        /**
         * resources size illegal
         */
        public static final String TYPE_RESOURCES_SIZE_ILLEGAL = HttpDownloadException.class.getName() + 
                "_TYPE_RESOURCES_SIZE_ILLEGAL";
        /**
         * eTag changed
         */
        public static final String TYPE_ETAG_CHANGED = HttpDownloadException.class.getName() + "_TYPE_ETAG_CHANGED";
        /**
         * contentRange validate fail
         */
        public static final String TYPE_CONTENT_RANGE_VALIDATE_FAIL = HttpDownloadException.class.getName() + 
                "_TYPE_CONTENT_RANGE_VALIDATE_FAIL";
        /**
         * ResponseCode error,can not read server data
         */
        public static final String TYPE_RESPONSE_CODE_ERROR = HttpDownloadException.class.getName() + 
                "_TYPE_RESPONSE_CODE_ERROR";

        public HttpDownloadException(String url, String detailMessage, String type) {
            super(url, detailMessage, type);
        }

        public HttpDownloadException(String url, Throwable throwable) {
            super(url, throwable);
        }

        @Override
        protected void onInitTypeWithFailReason(FailReason failReason) {
            super.onInitTypeWithFailReason(failReason);

            if (failReason == null) {
                return;
            }

            // other FailReason exceptions that need cast to HttpDownloadException

            // cast FileSaveException
            if (failReason instanceof FileSaveException) {

                FileSaveException fileSaveException = (FileSaveException) failReason;
                String type = fileSaveException.getType();

                if (FileSaveException.TYPE_FILE_CAN_NOT_STORAGE.equals(type)) {
                    // ignore
                } else if (FileSaveException.TYPE_RENAME_TEMP_FILE_ERROR.equals(type)) {
                    // ignore
                } else if (FileSaveException.TYPE_SAVER_HAS_BEEN_STOPPED.equals(type)) {
                    // ignore
                } else if (FileSaveException.TYPE_TEMP_FILE_DOES_NOT_EXIST.equals(type)) {
                    // ignore
                }
            }
        }
    }

    /**
     * OnHttpDownloadListener
     */
    public interface OnHttpDownloadListener {

        /**
         * the download connected
         *
         * @param inputStream     download inputStream
         * @param startPosInTotal the start position of inputStream start to save in total data
         *                        <p/>
         *                        |(0,totalStart)----|(startPosInTotal,inputStream start)---
         *                        |(inputStream.length,inputStream end)----|(fileTotalSize,totalEnd)
         */
        void onDownloadConnected(ContentLengthInputStream inputStream, long startPosInTotal);
    }

    /**
     * OnRangeChangeListener
     */
    public interface OnRangeChangeListener {

        /**
         * the range has been changed
         *
         * @param oldRange
         * @param newRange
         * @return true means continue download, otherwise stop download
         */
        boolean onRangeChanged(Range oldRange, Range newRange);
    }
}
