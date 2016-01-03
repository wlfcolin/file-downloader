package org.wlf.filedownloader.listener;

import org.wlf.filedownloader.base.FailReason;
import org.wlf.filedownloader.file_download.http_downloader.HttpDownloader.HttpDownloadException;
import org.wlf.filedownloader.listener.OnDetectUrlFileListener.DetectUrlFileFailReason;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * HttpFailReason
 *
 * @author wlf(Andy)
 * @datetime 2015-11-27 11:55 GMT+8
 * @email 411086563@qq.com
 */
class HttpFailReason extends FailReason {

    private static final long serialVersionUID = 6959079784746888591L;

    /**
     * network denied
     */
    public static final String TYPE_NETWORK_DENIED = HttpFailReason.class.getName() + "_TYPE_NETWORK_DENIED";
    /**
     * network timeout
     */
    public static final String TYPE_NETWORK_TIMEOUT = HttpFailReason.class.getName() + "_TYPE_NETWORK_TIMEOUT";

    /**
     * URL illegal
     */
    public static final String TYPE_URL_ILLEGAL = HttpFailReason.class.getName() + "_TYPE_URL_ILLEGAL";
    /**
     * url over redirect count
     */
    public static final String TYPE_URL_OVER_REDIRECT_COUNT = HttpFailReason.class.getName() + 
            "_TYPE_URL_OVER_REDIRECT_COUNT";
    /**
     * bad http response code,not 2XX
     */
    public static final String TYPE_BAD_HTTP_RESPONSE_CODE = HttpFailReason.class.getName() + 
            "_TYPE_BAD_HTTP_RESPONSE_CODE";
    /**
     * the file need to download does not exist
     */
    public static final String TYPE_HTTP_FILE_NOT_EXIST = HttpFailReason.class.getName() + "_TYPE_HTTP_FILE_NOT_EXIST";

    public HttpFailReason(String detailMessage, String type) {
        super(detailMessage, type);
    }

    public HttpFailReason(Throwable throwable) {
        super(throwable);
    }

    @Override
    protected void onInitTypeWithThrowable(Throwable throwable) {
        super.onInitTypeWithThrowable(throwable);
        if (isTypeInit()) {
            return;
        }

        if (throwable instanceof FailReason) {
            FailReason failReason = (FailReason) throwable;
            setTypeByOriginalClassInstanceType(failReason.getOriginalCause());
            if (isTypeInit()) {
                return;
            }

            // HttpDownloadException
            if (throwable instanceof HttpDownloadException) {
                HttpDownloadException httpDownloadException = (HttpDownloadException) throwable;
                String type = httpDownloadException.getType();
                if (HttpDownloadException.TYPE_NETWORK_TIMEOUT.equals(type)) {
                    setType(TYPE_NETWORK_TIMEOUT);
                } else if (HttpDownloadException.TYPE_NETWORK_DENIED.equals(type)) {
                    setType(TYPE_NETWORK_DENIED);
                } else {
                    //....
                }
            }
            // DetectUrlFileFailReason
            else if (throwable instanceof DetectUrlFileFailReason) {
                DetectUrlFileFailReason detectUrlFileFailReason = (DetectUrlFileFailReason) throwable;
                String type = detectUrlFileFailReason.getType();
                if (DetectUrlFileFailReason.TYPE_NETWORK_DENIED.equals(type)) {
                    setType(TYPE_NETWORK_DENIED);
                } else if (DetectUrlFileFailReason.TYPE_NETWORK_TIMEOUT.equals(type)) {
                    setType(TYPE_NETWORK_TIMEOUT);
                } else {
                    //....
                }
            }

            // FileSaveException

            // DownloadStatusRecordException
        } else {
            setTypeByOriginalClassInstanceType(throwable);
        }
    }

    private void setTypeByOriginalClassInstanceType(Throwable throwable) {
        if (throwable instanceof SocketTimeoutException) {
            setType(TYPE_NETWORK_TIMEOUT);
        } else if (throwable instanceof ConnectException || throwable instanceof UnknownHostException) {
            setType(TYPE_NETWORK_DENIED);
        }
    }
}
