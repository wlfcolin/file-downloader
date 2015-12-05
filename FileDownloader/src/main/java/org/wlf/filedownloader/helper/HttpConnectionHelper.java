package org.wlf.filedownloader.helper;

import android.text.TextUtils;

import org.wlf.filedownloader.http_downlaoder.Range;
import org.wlf.filedownloader.util.UrlUtil;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HttpConnectionHelper
 * <br/>
 * Http连接帮助类
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class HttpConnectionHelper {

    /**
     * create Detect http file Connection
     */
    public static HttpURLConnection createDetectConnection(String url, int connectTimeout, String charset) throws 
            Exception {
        return createHttpUrlConnection(url, connectTimeout, charset, -1, -1);
    }

    /**
     * create download http file Connection
     */
    public static HttpURLConnection createDownloadFileConnection(String url, int connectTimeout, String charset, 
                                                                 Range range) throws Exception {

        int rangeStartPos = -1;
        int rangeEndPos = -1;

        if (Range.isLegal(range)) {
            rangeStartPos = range.startPos;
            rangeEndPos = range.endPos;
        }

        return createHttpUrlConnection(url, connectTimeout, charset, rangeStartPos, rangeEndPos);
    }

    /**
     * create http file Connection,use [rangeStartPos,rangeEndPos] for request range
     *
     * @param url
     * @param connectTimeout
     * @param charset
     * @param rangeStartPos
     * @param rangeEndPos
     * @return HttpURLConnection
     * @throws Exception any exception during connect
     */
    public static HttpURLConnection createHttpUrlConnection(String url, int connectTimeout, String charset, int 
            rangeStartPos, int rangeEndPos) throws Exception {

        // up 4.0 can use if necessary
        // StrictMode.setThreadPolicy(new
        // StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites()
        // .detectNetwork().penaltyLog().build());

        String encodedUrl = UrlUtil.getEncoderUrl(url, charset);
        if (TextUtils.isEmpty(encodedUrl)) {
            throw new IllegalAccessException("URL Illegal");
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(encodedUrl).openConnection();
        conn.setConnectTimeout(connectTimeout);
        conn.setRequestProperty("Charset", charset);
        conn.setRequestProperty("Accept-Encoding", "identity");// FIXME now identity only
        // set range
        if (rangeStartPos > 0 && rangeEndPos > 0 && rangeEndPos > rangeStartPos) {
            conn.setRequestProperty("Range", "bytes=" + rangeStartPos + "-" + rangeEndPos);
        }
        conn.connect();
        return conn;
    }

}
