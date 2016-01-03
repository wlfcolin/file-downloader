package org.wlf.filedownloader.file_download.http_downloader;

import android.text.TextUtils;
import android.util.Log;

import org.wlf.filedownloader.base.FailException;
import org.wlf.filedownloader.file_download.HttpConnectionHelper;
import org.wlf.filedownloader.file_download.Range;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * http file download impl
 * <br/>
 * Http下载器
 *
 * @author wlf
 */
public class HttpDownloader implements Download {

    /**
     * LOG TAG
     */
    private static final String TAG = HttpDownloader.class.getSimpleName();

    private static final int MAX_REDIRECT_COUNT = 5;
    private static final int CONNECT_TIMEOUT = 15 * 1000;// 15s
    private static final String CHARSET = "UTF-8";

    private String mUrl;
    private Range mRange;// download data range
    private String mAcceptRangeType;// accept range type
    private String mETag;// http file eTag
    private int mConnectTimeout = CONNECT_TIMEOUT;

    private OnHttpDownloadListener mOnHttpDownloadListener;

    /**
     * constructor of HttpDownloader,the range is from the beginning to the end
     *
     * @param url url path
     */
    public HttpDownloader(String url) {
        this(url, null, null);
    }

    /**
     * constructor of HttpDownloader,will check range and acceptRangeType
     *
     * @param url             url path
     * @param range           data range
     * @param acceptRangeType accept range type
     */
    public HttpDownloader(String url, Range range, String acceptRangeType) {
        this(url, range, acceptRangeType, null);
    }

    /**
     * constructor of HttpDownloader,will check range,acceptRangeType and eTag
     *
     * @param url             url path
     * @param range           data range
     * @param acceptRangeType accept range type
     * @param eTag            file eTag
     */
    public HttpDownloader(String url, Range range, String acceptRangeType, String eTag) {
        this.mUrl = url;
        this.mRange = range;
        this.mAcceptRangeType = acceptRangeType;
        this.mETag = eTag;
    }

    /**
     * set HttpDownloadListener
     *
     * @param onHttpDownloadListener HttpDownloadListener
     */
    public void setOnHttpDownloadListener(OnHttpDownloadListener onHttpDownloadListener) {
        this.mOnHttpDownloadListener = onHttpDownloadListener;
    }

    @Override
    public void download() throws HttpDownloadException {

        String url = mUrl;// url

        HttpURLConnection conn = null;
        ContentLengthInputStream inputStream = null;

        try {
            conn = HttpConnectionHelper.createDownloadFileConnection(url, mConnectTimeout, CHARSET, mRange);

            int redirectCount = 0;
            while (conn != null && conn.getResponseCode() / 100 == 3 && redirectCount < MAX_REDIRECT_COUNT) {
                conn = HttpConnectionHelper.createDownloadFileConnection(conn.getHeaderField("Location"), 
                        mConnectTimeout, CHARSET, mRange);
                redirectCount++;
            }

            Log.d(TAG, "download 1、准备下载，重定向：" + redirectCount + "次" + "，最大重定向次数：" + MAX_REDIRECT_COUNT + "，url：" + url);

            if (redirectCount > MAX_REDIRECT_COUNT) {
                // error over max redirect
                throw new HttpDownloadException("over max redirect:" + MAX_REDIRECT_COUNT + "!", 
                        HttpDownloadException.TYPE_REDIRECT_COUNT_OVER_LIMITS);
            }

            // 1.check ResponseCode
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {

                // 2.check contentLength
                long contentLength = conn.getContentLength();

                if (contentLength <= 0) {
                    String contentLengthStr = conn.getHeaderField("Content-Length");
                    if (!TextUtils.isEmpty(contentLengthStr)) {
                        contentLength = Long.parseLong(contentLengthStr);
                    }
                }

                Log.d(TAG, "download 2、得到服务器返回的资源contentLength：" + contentLength + "，传入的range：" + mRange.toString() +
                        "，url：" + url);

                if (contentLength <= 0) {
                    // error content length illegal
                    throw new HttpDownloadException("content length illegal,get url file failed!", 
                            HttpDownloadException.TYPE_RESOURCES_SIZE_ILLEGAL);
                }

                // 3.check eTag(whether file is changed)
                if (!TextUtils.isEmpty(mETag)) {
                    String eTag = conn.getHeaderField("ETag");

                    Log.d(TAG, "download 3、得到服务器返回的资源eTag：" + eTag + "，传入的eTag：" + mETag + "，url：" + url);

                    if (TextUtils.isEmpty(eTag) || !mETag.equals(eTag)) {
                        // error eTag is not equal
                        throw new HttpDownloadException("eTag is not equal,please re-download!", 
                                HttpDownloadException.TYPE_ETAG_CHANGED);
                    }
                }

                // range is illegal,that means need download whole file from range 0 to file size
                if (!Range.isLegal(mRange) || (mRange != null && mRange.getLength() > contentLength)) {
                    mRange = new Range(0, contentLength);// FIXME whether need to notify range change?
                }
                // use range to continue download
                else {
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
                            throw new HttpDownloadException("contentRange validate failed!", HttpDownloadException
                                    .TYPE_CONTENT_RANGE_VALIDATE_FAIL);
                        }
                    }
                }

                // get server InputStream
                InputStream serverInputStream = conn.getInputStream();

                // wrap serverInputStream
                inputStream = new ContentLengthInputStream(serverInputStream, contentLength);

                Log.d(TAG, "download 4、准备处理数据，获取服务器返回的资源长度为：" + contentLength + "，获取服务器返回的输入流长度为：" + inputStream.getLength() + "，需要处理的区域为：" + mRange.toString() + "，url：" + url);

                // notifyDownloadConnected
                notifyDownloadConnected(inputStream, mRange.startPos);
            }
            // ResponseCode error
            else {
                // error ResponseCode error
                throw new HttpDownloadException("ResponseCode:" + responseCode + " error,can not read data!", 
                        HttpDownloadException.TYPE_RESPONSE_CODE_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // network timeout
            if (e instanceof SocketTimeoutException || e.getCause() instanceof SocketTimeoutException) {
                // error network timeout
                throw new HttpDownloadException("network timeout!", e, HttpDownloadException.TYPE_NETWORK_TIMEOUT);
            } else if (e instanceof ConnectException || e instanceof UnknownHostException) {
                // error network denied
                throw new HttpDownloadException("network denied!", e, HttpDownloadException.TYPE_NETWORK_DENIED);
            } else if (e instanceof HttpDownloadException) {
                // HttpDownloadException
                throw (HttpDownloadException) e;
            } else {
                // other Exception
                throw new HttpDownloadException(e);
            }
        } finally {
            // close inputStream
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // close connection
            if (conn != null) {
                conn.disconnect();
            }

            Log.d(TAG, "download 5、下载已结束" + "，url：" + url);
        }

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
    public static class HttpDownloadException extends FailException {

        private static final long serialVersionUID = -1264975040094495002L;

        /**
         * http redirect count over limits
         */
        public static final String TYPE_REDIRECT_COUNT_OVER_LIMITS = HttpDownloadException.class.getName() + 
                "_TYPE_REDIRECT_COUNT_OVER_LIMITS";
        /**
         * resources size illegal
         */
        public static final String TYPE_RESOURCES_SIZE_ILLEGAL = HttpDownloadException.class.getName() + 
                "_TYPE_RESOURCES_SIZE_ILLEGAL";
        /**
         * eTag is not equal
         */
        public static final String TYPE_ETAG_CHANGED = HttpDownloadException.class.getName() + "_TYPE_ETAG_CHANGED";
        /**
         * contentRange validate fail
         */
        public static final String TYPE_CONTENT_RANGE_VALIDATE_FAIL = HttpDownloadException.class.getName() + 
                "_TYPE_CONTENT_RANGE_VALIDATE_FAIL";
        /**
         * ResponseCode error,can not read data
         */
        public static final String TYPE_RESPONSE_CODE_ERROR = HttpDownloadException.class.getName() + 
                "_TYPE_RESPONSE_CODE_ERROR";
        /**
         * network timeout
         */
        public static final String TYPE_NETWORK_TIMEOUT = HttpDownloadException.class.getName() + 
                "_TYPE_NETWORK_TIMEOUT";
        /**
         * network denied
         */
        public static final String TYPE_NETWORK_DENIED = HttpDownloadException.class.getName() + "_TYPE_NETWORK_DENIED";

        public HttpDownloadException(String detailMessage, String type) {
            super(detailMessage, type);
        }

        public HttpDownloadException(String detailMessage, Throwable throwable, String type) {
            super(detailMessage, throwable, type);
        }

        public HttpDownloadException(Throwable throwable) {
            super(throwable);
        }

        @Override
        protected void onInitTypeWithThrowable(Throwable throwable) {
            super.onInitTypeWithThrowable(throwable);

            if (isTypeInit() || throwable == null) {
                return;
            }

            if (throwable instanceof SocketTimeoutException) {
                setType(TYPE_NETWORK_TIMEOUT);
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
}
