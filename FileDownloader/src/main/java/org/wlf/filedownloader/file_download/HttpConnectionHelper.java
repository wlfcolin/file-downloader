package org.wlf.filedownloader.file_download;

import android.text.TextUtils;

import org.json.JSONObject;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.MapUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
     * LOG TAG
     */
    private static final String TAG = HttpConnectionHelper.class.getSimpleName();

    /**
     * create Detect http file Connection
     */
    public static HttpURLConnection createDetectConnection(String url, int connectTimeout, String charset) throws 
            Exception {
        return createHttpUrlConnection(new RequestParam(url, connectTimeout, charset));
    }

    /**
     * create download http file Connection
     */
    public static HttpURLConnection createDownloadFileConnection(RequestParam requestParam) throws Exception {
        return createHttpUrlConnection(requestParam);
    }

    /**
     * create http file Connection,use [rangeStartPos,rangeEndPos] for request range
     *
     * @param requestParam
     * @return HttpURLConnection
     * @throws Exception any exception during connect
     */
    private static HttpURLConnection createHttpUrlConnection(RequestParam requestParam) throws Exception {

        // up 4.0 can use if necessary
        // StrictMode.setThreadPolicy(new
        // StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites()
        // .detectNetwork().penaltyLog().build());

        if (requestParam == null) {
            return null;
        }

        Log.d(TAG, "headBuffer，createHttpUrlConnection，发送的请求参数：" + requestParam.toString());

        String encodedUrl = UrlUtil.getASCIIEncodedUrl(requestParam.mUrl);
        if (TextUtils.isEmpty(encodedUrl)) {
            throw new IllegalAccessException("URL Illegal !");
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(encodedUrl).openConnection();

        conn.setConnectTimeout(requestParam.mConnectTimeout);
        conn.setReadTimeout(requestParam.mConnectTimeout);// FIXME read timeout equals to connect timeout

        conn.setRequestProperty("Accept-Encoding", "identity");// FIXME now identity only
        System.setProperty("http.keepAlive", "false");// FIXME now force do not keep alive

        if (!TextUtils.isEmpty(requestParam.mCharset)) {
            conn.setRequestProperty("Charset", requestParam.mCharset);
        }

        // set range, Support HTTP 1.1 and above only
        if (requestParam.mRangeStartPos > 0) {
            if (requestParam.mRangeEndPos > 0 && requestParam.mRangeEndPos > requestParam.mRangeStartPos) {
                conn.setRequestProperty("Range", "bytes=" + requestParam.mRangeStartPos + "-" + requestParam
                        .mRangeEndPos);
            } else {
                conn.setRequestProperty("Range", "bytes=" + requestParam.mRangeStartPos + "-");
            }
            if (!TextUtils.isEmpty(requestParam.mETag)) {
                conn.setRequestProperty("If-Range", requestParam.mETag);
            } else {
                if (!TextUtils.isEmpty(requestParam.mLastModified)) {
                    conn.setRequestProperty("If-Range", requestParam.mLastModified);
                }
            }
        }

        conn.connect();

        return conn;
    }

    // FIXME not use now
    private static void enableHttpResponseCache(String cacheDirPath) {
        try {
            long httpCacheSize = 10 * 1024 * 1024;// 10M
            File httpCacheDir = new File(cacheDirPath, "http_cache");
            Class.forName("android.net.http.HttpResponseCache").getMethod("install", File.class, long.class).invoke
                    (null, httpCacheDir, httpCacheSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class RequestParam {

        private String mUrl;
        private int mConnectTimeout = 15000; // 15s
        private String mCharset;
        private long mRangeStartPos = -1;
        private long mRangeEndPos = -1;
        private String mETag;
        private String mLastModified;

        public RequestParam(String url, int connectTimeout, String charset) {
            mUrl = url;
            mConnectTimeout = connectTimeout;
            mCharset = charset;
        }

        public RequestParam(String url, int connectTimeout, String charset, long rangeStartPos, long rangeEndPos, 
                            String ETag, String lastModified) {
            mUrl = url;
            mConnectTimeout = connectTimeout;
            mCharset = charset;
            mRangeStartPos = rangeStartPos;
            mRangeEndPos = rangeEndPos;
            mETag = ETag;
            mLastModified = lastModified;
        }

        public void setUrl(String url) {
            mUrl = url;
        }

        //        public void setConnectTimeout(int connectTimeout) {
        //            mConnectTimeout = connectTimeout;
        //        }
        //
        //        public void setCharset(String charset) {
        //            mCharset = charset;
        //        }
        //
        //        public void setRangeStartPos(long rangeStartPos) {
        //            mRangeStartPos = rangeStartPos;
        //        }
        //
        //        public void setRangeEndPos(long rangeEndPos) {
        //            mRangeEndPos = rangeEndPos;
        //        }
        //
        //        public void setETag(String ETag) {
        //            mETag = ETag;
        //        }
        //
        //        public void setLastModified(String lastModified) {
        //            mLastModified = lastModified;
        //        }

        @Override
        public String toString() {
            return "RequestParam{" +
                    "mUrl='" + mUrl + '\'' +
                    ", mConnectTimeout=" + mConnectTimeout +
                    ", mCharset='" + mCharset + '\'' +
                    ", mRangeStartPos=" + mRangeStartPos +
                    ", mRangeEndPos=" + mRangeEndPos +
                    ", mETag='" + mETag + '\'' +
                    ", mLastModified='" + mLastModified + '\'' +
                    '}';
        }
    }

    /**
     * get file size form response header
     *
     * @param responseHeaderMap response header map
     * @return file size
     */
    public static long getFileSizeFromResponseHeader(Map<String, List<String>> responseHeaderMap) {

        if (MapUtil.isEmpty(responseHeaderMap)) {
            return -1;
        }

        // common server
        long fileSize = getFileSizeFromCommonServerResponseHeader(responseHeaderMap);

        if (fileSize <= 0) {
            // php server
            fileSize = getFileSizeFromPhpServerResponseHeader(responseHeaderMap);
        }

        return fileSize;
    }

    private static long getFileSizeFromCommonServerResponseHeader(Map<String, List<String>> responseHeaderMap) {

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

    /**
     * get file name from response header
     *
     * @param responseHeaderMap response header map
     * @return file name
     */
    public static String getFileNameFromResponseHeader(Map<String, List<String>> responseHeaderMap) {

        if (MapUtil.isEmpty(responseHeaderMap)) {
            return null;
        }

        // common server
        String fileName = getFileNameFromCommonServerResponseHeader(responseHeaderMap);

        if (!TextUtils.isEmpty(fileName)) {
            return fileName;
        }

        // php server
        fileName = getFileNameFromPhpServerResponseHeader(responseHeaderMap);

        return fileName;
    }

    private static String getFileNameFromCommonServerResponseHeader(Map<String, List<String>> responseHeaderMap) {

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

    /**
     * get last modified datetime from response header
     *
     * @param responseHeaderMap response header map
     * @return file name
     */
    public static String getLastModifiedFromResponseHeader(Map<String, List<String>> responseHeaderMap) {

        if (MapUtil.isEmpty(responseHeaderMap)) {
            return null;
        }

        List<String> contentLengths = responseHeaderMap.get("Last-Modified");

        if (CollectionUtil.isEmpty(contentLengths)) {
            return null;
        }

        String lastModified = contentLengths.get(0);

        return lastModified;
    }

    public static String getStringHeaders(Map<String, List<String>> headers) {

        try {
            if (MapUtil.isEmpty(headers)) {
                return null;
            }

            Map<String, ArrayList<String>> copyMap = getWritableMap(headers);
            JSONObject jsonObject = new JSONObject(copyMap);
            return jsonObject.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * get a writable map
     *
     * @param readOnlyMap a read only map
     * @return a writable map
     */
    private static Map<String, ArrayList<String>> getWritableMap(final Map<String, ? extends List<String>> 
                                                                         readOnlyMap) {

        Map<String, ArrayList<String>> readAndWriteMap = new TreeMap<String, ArrayList<String>>();

        Set<String> keys = readOnlyMap.keySet();

        Iterator<String> iter = keys.iterator();
        while (iter.hasNext()) {

            String key = iter.next();
            if (key != null) {

                ArrayList<String> readAndWriteList = new ArrayList<String>();// 可读写的map
                List<String> readOnlyValues = readOnlyMap.get(key);

                Iterator<String> it = readOnlyValues.iterator();
                while (it.hasNext()) {
                    String value = it.next();
                    readAndWriteList.add(value);
                }
                readAndWriteMap.put(key, readAndWriteList);
            }
        }
        return readAndWriteMap;
    }

}
