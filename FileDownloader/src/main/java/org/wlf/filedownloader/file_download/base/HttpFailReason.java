package org.wlf.filedownloader.file_download.base;

import org.wlf.filedownloader.base.UrlFailReason;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * HttpFailReason
 *
 * @author wlf(Andy)
 * @datetime 2015-11-27 11:55 GMT+8
 * @email 411086563@qq.com
 */
public class HttpFailReason extends UrlFailReason {

    /**
     * network denied
     */
    public static final String TYPE_NETWORK_DENIED = HttpFailReason.class.getName() + "_TYPE_NETWORK_DENIED";
    /**
     * network timeout
     */
    public static final String TYPE_NETWORK_TIMEOUT = HttpFailReason.class.getName() + "_TYPE_NETWORK_TIMEOUT";

    public HttpFailReason(String url, String detailMessage, String type) {
        super(url, detailMessage, type);
    }

    public HttpFailReason(String url, Throwable throwable) {
        super(url, throwable);
    }

    @Override
    protected void onInitTypeWithOriginalThrowable(Throwable throwable) {
        super.onInitTypeWithOriginalThrowable(throwable);

        if (throwable == null) {
            return;
        }

        if (throwable instanceof SocketTimeoutException) {
            setType(TYPE_NETWORK_TIMEOUT);
        } else if (throwable instanceof ConnectException || throwable instanceof UnknownHostException) {
            setType(TYPE_NETWORK_DENIED);
        } else if (throwable instanceof SocketException) {
            setType(TYPE_NETWORK_DENIED);
        }
    }
}
