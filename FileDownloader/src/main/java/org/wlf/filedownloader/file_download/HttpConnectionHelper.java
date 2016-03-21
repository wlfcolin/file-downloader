package org.wlf.filedownloader.file_download;

import android.text.TextUtils;

import org.json.JSONObject;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.MapUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * HttpConnectionHelper
 * <br/>
 * Http连接帮助类
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class HttpConnectionHelper {

    private static final String TAG = HttpConnectionHelper.class.getSimpleName();

    //    private static final String[] IGNORE_HEADER_KEY = new String[]{
    //            // Accept-Encoding
    //            "Accept-Encoding",
    //    };

    /**
     * create Detect http file Connection
     */
    public static HttpURLConnection createDetectConnection(String url, int connectTimeout, String charset, String 
            requestMethod, Map<String, String> headers) throws Exception {

        RequestParam requestParam = new RequestParam(url, connectTimeout, charset);
        requestParam.setRequestMethod(requestMethod);
        requestParam.setHeaders(headers);

        return createHttpUrlConnection(requestParam);
    }

    /**
     * create download http file Connection
     */
    public static HttpURLConnection createDownloadFileConnection(RequestParam requestParam) throws Exception {
        return createHttpUrlConnection(requestParam);
    }

    /**
     * initTrustSSL
     */
    private static void initTrustSSL(HttpsURLConnection conn) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                // do nothing, let the check pass
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }}, new SecureRandom());
            // config https
            conn.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            conn.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    // always true, let the check pass
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        String encodedUrl = UrlUtil.getASCIIEncodedUrl(requestParam.mUrl, requestParam.mCharset);
        if (TextUtils.isEmpty(encodedUrl)) {
            throw new IllegalAccessException("URL Illegal !");
        }

        HttpURLConnection conn = null;

        URL url = new URL(encodedUrl);
        if (encodedUrl.toLowerCase().startsWith("https")) {
            // https
            HttpsURLConnection httpsConn = (HttpsURLConnection) url.openConnection();
            initTrustSSL(httpsConn);
            conn = httpsConn;
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }

        conn.setConnectTimeout(requestParam.mConnectTimeout);
        conn.setReadTimeout(requestParam.mConnectTimeout);// FIXME read timeout equals to connect timeout
        conn.setRequestMethod(requestParam.mRequestMethod);
        conn.setInstanceFollowRedirects(false);

        // -----------------------------headers-----------------------------------------

        if (!MapUtil.isEmpty(requestParam.mHeaders)) {
            // custom headers first
            Set<String> keys = requestParam.mHeaders.keySet();

            Log.i(TAG, "自定义头信息大小：" + keys.size());

            for (String key : keys) {
                if (TextUtils.isEmpty(key)) {
                    continue;
                }
                String value = requestParam.mHeaders.get(key);
                conn.setRequestProperty(key, value);

                Log.i(TAG, "添加自定义头信息，url：" + url + "，key：" + key + "，value：" + value);
            }
        }

        conn.setRequestProperty("Accept-Encoding", "identity");// FIXME now identity only
        // System.setProperty("http.keepAlive", "false");

        if (!TextUtils.isEmpty(requestParam.mCharset)) {
            conn.setRequestProperty("Charset", requestParam.mCharset);
        }

        // set range, Support HTTP 1.1 and above
        if (requestParam.mRangeStartPos > 0) {
            if (requestParam.mRangeEndPos > 0 && requestParam.mRangeEndPos > requestParam.mRangeStartPos) {
                conn.setRequestProperty("Range", "bytes=" + requestParam.mRangeStartPos + "-" + requestParam
                        .mRangeEndPos);
            } else {
                conn.setRequestProperty("Range", "bytes=" + requestParam.mRangeStartPos + "-");
            }
            // eTag first
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

    //    private static void enableHttpResponseCache(String cacheDirPath) {
    //        try {
    //            long httpCacheSize = 10 * 1024 * 1024;// 10M
    //            File httpCacheDir = new File(cacheDirPath, "http_cache");
    //            Class.forName("android.net.http.HttpResponseCache").getMethod("install", File.class, long.class)
    // .invoke
    //                    (null, httpCacheDir, httpCacheSize);
    //        } catch (Exception e) {
    //            e.printStackTrace();
    //        }
    //    }

    public static class RequestParam {

        private String mUrl;
        private int mConnectTimeout = 15000; // 15s
        private String mCharset;
        private long mRangeStartPos = -1;
        private long mRangeEndPos = -1;
        private String mETag;
        private String mLastModified;
        private String mRequestMethod = "GET";// get default
        private Map<String, String> mHeaders;

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

        public void setRequestMethod(String requestMethod) {
            if (TextUtils.isEmpty(requestMethod)) {
                return;
            }
            this.mRequestMethod = requestMethod;
        }

        public void setHeaders(Map<String, String> headers) {
            if (headers == null) {
                return;
            }
            mHeaders = headers;
        }

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
                    ", mRequestMethod='" + mRequestMethod + '\'' +
                    ", mHeaders=" + mHeaders +
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
