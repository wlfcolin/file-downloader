package org.wlf.filedownloader.file_download.http_downloader;

import android.text.TextUtils;
import android.util.Log;

import org.wlf.filedownloader.base.FailReason;
import org.wlf.filedownloader.file_download.CloseConnectionTask;
import org.wlf.filedownloader.file_download.HttpConnectionHelper;
import org.wlf.filedownloader.file_download.base.HttpFailReason;
import org.wlf.filedownloader.file_download.file_saver.FileSaver.FileSaveException;

import java.io.InputStream;
import java.net.HttpURLConnection;
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

    /**
     * LOG TAG
     */
    private static final String TAG = HttpDownloader.class.getSimpleName();

    private static final int MAX_REDIRECT_TIMES = 5;
    private static final int CONNECT_TIMEOUT = 15 * 1000;// 15s default
    private static final String DEFAULT_CHARSET = "UTF-8";

    private String mUrl;// download url
    private Range mRange;// download data range
    private String mAcceptRangeType;// accept range type
    private String mETag;// http file eTag
    private int mConnectTimeout = CONNECT_TIMEOUT;// connect time out, millisecond
    private String mCharset = DEFAULT_CHARSET;// FIXME now UTF-8 only

    private ExecutorService mCloseConnectionEngine;// engine use for closing the http connection

    private OnHttpDownloadListener mOnHttpDownloadListener;

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
     */
    public HttpDownloader(String url, Range range, String acceptRangeType, String eTag) {
        this.mUrl = url;
        this.mRange = range;
        this.mAcceptRangeType = acceptRangeType;
        this.mETag = eTag;
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
     * set CloseConnectionEngine
     *
     * @param closeConnectionEngine CloseConnectionEngine
     */
    public void setCloseConnectionEngine(ExecutorService closeConnectionEngine) {
        mCloseConnectionEngine = closeConnectionEngine;
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
            conn = HttpConnectionHelper.createDownloadFileConnection(url, mConnectTimeout, mCharset, mRange);

            int redirectTimes = 0;
            while (conn != null && conn.getResponseCode() / 100 == 3 && redirectTimes < MAX_REDIRECT_TIMES) {// redirect
                conn = HttpConnectionHelper.createDownloadFileConnection(conn.getHeaderField("Location"), 
                        mConnectTimeout, mCharset, mRange);
                redirectTimes++;
            }

            Log.d(TAG, TAG + ".download 1、准备下载，重定向：" + redirectTimes + "次" + "，最大重定向次数：" + MAX_REDIRECT_TIMES +
                    "，url：" + url);

            if (redirectTimes > MAX_REDIRECT_TIMES) {
                // error over max redirect times
                throw new HttpDownloadException("over max redirect:" + MAX_REDIRECT_TIMES + "!", 
                        HttpDownloadException.TYPE_REDIRECT_COUNT_OVER_LIMITS);
            }

            if (conn == null) {
                throw new HttpDownloadException("the connection is null:" + MAX_REDIRECT_TIMES + "!", 
                        HttpDownloadException.TYPE_NULL_POINTER);
            }

            // 1.check ResponseCode
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {

                // 2.check contentLength
                long contentLength = conn.getContentLength();

                if (contentLength <= 0) {
                    // get contentLength by header
                    String contentLengthStr = conn.getHeaderField("Content-Length");
                    if (!TextUtils.isEmpty(contentLengthStr)) {
                        contentLength = Long.parseLong(contentLengthStr);
                    }
                }

                Log.d(TAG, TAG + ".download 2、得到服务器返回的资源contentLength：" + contentLength + "，传入的range：" + mRange
                        .toString() +
                        "，url：" + url);

                if (contentLength <= 0) {
                    // error content length illegal
                    throw new HttpDownloadException("content length illegal,get url file failed!", 
                            HttpDownloadException.TYPE_RESOURCES_SIZE_ILLEGAL);
                }

                // 3.check eTag(whether file is changed)
                if (!TextUtils.isEmpty(mETag)) {
                    String eTag = conn.getHeaderField("ETag");

                    Log.d(TAG, TAG + ".download 3、得到服务器返回的资源eTag：" + eTag + "，传入的eTag：" + mETag + "，url：" + url);

                    if (TextUtils.isEmpty(eTag) || !mETag.equals(eTag)) {
                        // error eTag is not equal
                        throw new HttpDownloadException("eTag is not equal,please delete the old one then " + 
                                "re-download!", HttpDownloadException.TYPE_ETAG_CHANGED);
                    }
                }

                // range is illegal,that means it need download whole file from range 0 to file size
                if (!Range.isLegal(mRange) || (mRange != null && mRange.getLength() > contentLength)) {
                    mRange = new Range(0, contentLength);// FIXME whether need to notify caller the range change?
                }
                // use custom range to continue download
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
                throw new HttpDownloadException("ResponseCode:" + responseCode + " error,can not read server data!", 
                        HttpDownloadException.TYPE_RESPONSE_CODE_ERROR);
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
                throw new HttpDownloadException(e);
            }
        } finally {
            // close inputStream & url connection
            CloseConnectionTask closeConnectionTask = new CloseConnectionTask(conn, inputStream);
            if (mCloseConnectionEngine != null) {
                mCloseConnectionEngine.execute(closeConnectionTask);
            } else {
                closeConnectionTask.run();
            }

            Log.d(TAG, TAG + ".download 5、文件下载【已结束】，是否有异常：" + hasException + "，url：" + url);
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

        public HttpDownloadException(String detailMessage, String type) {
            super(detailMessage, type);
        }

        public HttpDownloadException(Throwable throwable) {
            super(throwable);
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
}
