package org.wlf.filedownloader.file_download;

import android.text.TextUtils;

import org.wlf.filedownloader.file_download.http_downloader.Range;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.MapUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

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

        long rangeStartPos = -1;
        long rangeEndPos = -1;

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
    public static HttpURLConnection createHttpUrlConnection(String url, int connectTimeout, String charset, long
            rangeStartPos, long rangeEndPos) throws Exception {
        return createHttpUrlConnectionInternal(url, connectTimeout, charset, rangeStartPos, rangeEndPos);
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
    private static HttpURLConnection createHttpUrlConnectionInternal(String url, int connectTimeout, String charset,
                                                                     long rangeStartPos, long rangeEndPos) throws
            Exception {

        // up 4.0 can use if necessary
        // StrictMode.setThreadPolicy(new
        // StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites()
        // .detectNetwork().penaltyLog().build());

        String encodedUrl = UrlUtil.getASCIIEncodedUrl(url);
        if (TextUtils.isEmpty(encodedUrl)) {
            throw new IllegalAccessException("URL Illegal !");
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(encodedUrl).openConnection();
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(connectTimeout);// FIXME read timeout equals to connect timeout
        conn.setRequestProperty("Charset", charset);
        conn.setRequestProperty("Accept-Encoding", "identity");// FIXME now identity only
        // set range
        if (rangeStartPos > 0 && rangeEndPos > 0 && rangeEndPos > rangeStartPos) {
            conn.setRequestProperty("Range", "bytes=" + rangeStartPos + "-" + rangeEndPos);
        }
        conn.connect();
        return conn;
    }


    public static long getFileSizeFromResponseHeader(Map<String, List<String>> responseHeaderMap) {

        if (MapUtil.isEmpty(responseHeaderMap)) {
            return -1;
        }

        // java server
        long fileSize = getFileSizeFromJavaServerResponseHeader(responseHeaderMap);

        if (fileSize <= 0) {
            // php server
            fileSize = getFileSizeFromPhpServerResponseHeader(responseHeaderMap);
        }


        return fileSize;
    }


    private static long getFileSizeFromJavaServerResponseHeader(Map<String, List<String>> responseHeaderMap) {

        if (MapUtil.isEmpty(responseHeaderMap)) {
            return -1;
        }

        List<String> contentLengths = responseHeaderMap.get("Content-Length");

        if (CollectionUtil.isEmpty(contentLengths)) {
            return -1;
        }

        String contentLengthStr = contentLengths.get(0);

        if (!TextUtils.isEmpty(contentLengthStr)) {
            long fileSize = -1;
            try {
                fileSize = Long.parseLong(contentLengthStr);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            return fileSize;
        }

        return -1;
    }

    private static long getFileSizeFromPhpServerResponseHeader(Map<String, List<String>> responseHeaderMap) {

        if (MapUtil.isEmpty(responseHeaderMap)) {
            return -1;
        }

        List<String> contentLengths = responseHeaderMap.get("Accept-Length");

        if (CollectionUtil.isEmpty(contentLengths)) {
            return -1;
        }

        String contentLengthStr = contentLengths.get(0);

        if (!TextUtils.isEmpty(contentLengthStr)) {
            long fileSize = -1;
            try {
                fileSize = Long.parseLong(contentLengthStr);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            return fileSize;
        }


        return -1;
    }


    public static String getFileNameFromResponseHeader(Map<String, List<String>> responseHeaderMap) {

        if (MapUtil.isEmpty(responseHeaderMap)) {
            return null;
        }

        // java server
        String fileName = getFileNameFromJavaServerResponseHeader(responseHeaderMap);

        if (!TextUtils.isEmpty(fileName)) {
            return fileName;
        }

        // php server
        fileName = getFileNameFromPhpServerResponseHeader(responseHeaderMap);

        return fileName;
    }

    private static String getFileNameFromJavaServerResponseHeader(Map<String, List<String>> responseHeaderMap) {

        if (MapUtil.isEmpty(responseHeaderMap)) {
            return null;
        }

        // nothing to do

        return null;
    }

    private static String getFileNameFromPhpServerResponseHeader(Map<String, List<String>> responseHeaderMap) {

        if (MapUtil.isEmpty(responseHeaderMap)) {
            return null;
        }

        List<String> contentDispositions = responseHeaderMap.get("Content-Disposition");

        if (CollectionUtil.isEmpty(contentDispositions)) {
            return null;
        }

        for (String contentDisposition : contentDispositions) {
            if (TextUtils.isEmpty(contentDisposition)) {
                continue;
            }
            if (contentDisposition.contains("filename=")) {
                // find
                int start = contentDisposition.lastIndexOf("=");
                if (start != -1) {
                    String fileName = contentDisposition.substring(start + 1, contentDisposition.length());
                    return fileName;
                }
            }
        }

        return null;
    }


}
