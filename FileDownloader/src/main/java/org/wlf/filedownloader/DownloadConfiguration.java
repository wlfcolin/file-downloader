package org.wlf.filedownloader;

import android.text.TextUtils;

import org.wlf.filedownloader.base.BaseDownloadConfigBuilder;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.MapUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Download Configuration
 * <br/>
 * 文件下载的配置类（单个）
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DownloadConfiguration {

    /**
     * Configuration Builder
     */
    public static class Builder extends InnerBuilder {

        @Override
        public Builder addHeader(String key, String value) {
            super.addHeader(key, value);
            return this;
        }

        @Override
        public Builder replaceHeader(String key, String value) {
            super.replaceHeader(key, value);
            return this;
        }

        @Override
        public Builder addHeaders(Map<String, String> headers) {
            super.addHeaders(headers);
            return this;
        }

        @Override
        public Builder configRetryDownloadTimes(int retryDownloadTimes) {
            super.configRetryDownloadTimes(retryDownloadTimes);
            return this;
        }

        @Override
        public Builder configConnectTimeout(int connectTimeout) {
            super.configConnectTimeout(connectTimeout);
            return this;
        }

        @Override
        public Builder configRequestMethod(String requestMethod) {
            super.configRequestMethod(requestMethod);
            return this;
        }
    }

    /**
     * Configuration MultiBuilder
     */
    public static class MultiBuilder extends InnerBuilder {

        @Override
        public MultiBuilder addHeader(String key, String value) {
            super.addHeader(key, value);
            return this;
        }

        @Override
        public MultiBuilder replaceHeader(String key, String value) {
            super.replaceHeader(key, value);
            return this;
        }

        @Override
        public MultiBuilder addHeaders(Map<String, String> headers) {
            super.addHeaders(headers);
            return this;
        }

        @Override
        public MultiBuilder configRetryDownloadTimes(int retryDownloadTimes) {
            super.configRetryDownloadTimes(retryDownloadTimes);
            return this;
        }

        @Override
        public MultiBuilder configConnectTimeout(int connectTimeout) {
            super.configConnectTimeout(connectTimeout);
            return this;
        }

        @Override
        public MultiBuilder configRequestMethod(String requestMethod) {
            super.configRequestMethod(requestMethod);
            return this;
        }

        // ------------multi------------

        @Override
        public MultiBuilder addHeaderWithUrl(String url, String key, String value) {
            super.addHeaderWithUrl(url, key, value);
            return this;
        }

        @Override
        public MultiBuilder replaceHeaderWithUrl(String url, String key, String value) {
            super.replaceHeaderWithUrl(url, key, value);
            return this;
        }

        @Override
        public MultiBuilder addHeadersWithUrl(String url, Map<String, String> headers) {
            super.addHeadersWithUrl(url, headers);
            return this;
        }

        @Override
        public MultiBuilder configRetryDownloadTimesWithUrl(String url, int retryDownloadTimes) {
            super.configRetryDownloadTimesWithUrl(url, retryDownloadTimes);
            return this;
        }

        @Override
        public MultiBuilder configConnectTimeoutWithUrl(String url, int connectTimeout) {
            super.configConnectTimeoutWithUrl(url, connectTimeout);
            return this;
        }

        @Override
        public MultiBuilder configRequestMethodWithUrl(String url, String requestMethod) {
            super.configRequestMethodWithUrl(url, requestMethod);
            return this;
        }
    }

    /**
     * Configuration Builder
     */
    private static class InnerBuilder extends BaseDownloadConfigBuilder {

        /**
         * all custom headers of urls, default is null which means no custom headers
         */
        private Map<String, Map<String, String>> mUrlHeaders = new HashMap<String, Map<String, String>>();

        /**
         * all retry download times of urls
         */
        private Map<String, Integer> mRetryDownloadTimes = new HashMap<String, Integer>();

        /**
         * all connect timeout of urls
         */
        private Map<String, Integer> mConnectTimeout = new HashMap<String, Integer>();

        /**
         * all request methods
         */
        private Map<String, String> mRequestMethod = new HashMap<String, String>();

        /**
         * add the custom header for download
         *
         * @param key   the key of the custom header
         * @param value the value of the custom header
         * @return the Builder
         */
        protected InnerBuilder addHeader(String key, String value) {
            String url = NULL_KEY_FOR_URL;
            addOrReplaceWithUrl(url, key, value, false);
            return this;
        }

        /**
         * add the custom header for download
         *
         * @param url   file url
         * @param key   the key of the custom header
         * @param value the value of the custom header
         * @return the Builder
         */
        protected InnerBuilder addHeaderWithUrl(String url, String key, String value) {
            addOrReplaceWithUrl(url, key, value, false);
            return this;
        }

        /**
         * replace the custom header for download
         *
         * @param key   the key of the custom header
         * @param value the value of the custom header
         * @return the Builder
         */
        protected InnerBuilder replaceHeader(String key, String value) {
            String url = NULL_KEY_FOR_URL;
            addOrReplaceWithUrl(url, key, value, true);
            return this;
        }

        /**
         * replace the custom header for download
         *
         * @param url   file url
         * @param key   the key of the custom header
         * @param value the value of the custom header
         * @return the Builder
         */
        protected InnerBuilder replaceHeaderWithUrl(String url, String key, String value) {
            addOrReplaceWithUrl(url, key, value, true);
            return this;
        }

        private void addOrReplaceWithUrl(String url, String key, String value, boolean replace) {
            if (UrlUtil.isUrl(url) && !TextUtils.isEmpty(key)) {
                Map<String, String> existHeaders = mUrlHeaders.get(url);
                if (existHeaders == null) {
                    existHeaders = new HashMap<String, String>();
                    mUrlHeaders.put(url, existHeaders);
                }
                if (replace) {
                    if (existHeaders.containsKey(key)) {
                        existHeaders.remove(key);
                        existHeaders.put(key, value);
                    }
                } else {
                    if (!existHeaders.containsKey(key)) {
                        existHeaders.put(key, value);
                    }
                }
            }
        }

        /**
         * add the custom headers for download
         *
         * @param headers all custom headers for download
         * @return the Builder
         */
        protected InnerBuilder addHeaders(Map<String, String> headers) {
            String url = NULL_KEY_FOR_URL;
            addHeadersWithUrl(url, headers);
            return this;
        }

        /**
         * add the custom headers for download
         *
         * @param url     file url
         * @param headers all custom headers for download
         * @return the Builder
         */
        protected InnerBuilder addHeadersWithUrl(String url, Map<String, String> headers) {

            if (UrlUtil.isUrl(url) && !MapUtil.isEmpty(headers)) {
                Map<String, String> existHeaders = mUrlHeaders.get(url);
                if (existHeaders == null) {
                    existHeaders = new HashMap<String, String>();
                    mUrlHeaders.put(url, existHeaders);
                }
                existHeaders.putAll(headers);
            }
            return this;
        }

        @Override
        public InnerBuilder configRetryDownloadTimes(int retryDownloadTimes) {
            String url = NULL_KEY_FOR_URL;
            configRetryDownloadTimesWithUrl(url, retryDownloadTimes);
            return this;
        }

        @Override
        public InnerBuilder configConnectTimeout(int connectTimeout) {
            String url = NULL_KEY_FOR_URL;
            configConnectTimeoutWithUrl(url, connectTimeout);
            return this;
        }

        /**
         * config RetryDownloadTimes
         *
         * @param url                file url
         * @param retryDownloadTimes DownloadTaskSize at the same time, please set 0 to {@link
         *                           #MAX_RETRY_DOWNLOAD_TIMES}, if not set, default is {@link
         *                           #DEFAULT_RETRY_DOWNLOAD_TIMES}, set 0 means not retry
         * @return the builder
         */
        protected InnerBuilder configRetryDownloadTimesWithUrl(String url, int retryDownloadTimes) {
            if (UrlUtil.isUrl(url)) {
                if (retryDownloadTimes >= 0 && retryDownloadTimes <= MAX_RETRY_DOWNLOAD_TIMES) {
                    mRetryDownloadTimes.put(url, retryDownloadTimes);
                } else if (retryDownloadTimes > MAX_RETRY_DOWNLOAD_TIMES) {
                    mRetryDownloadTimes.put(url, MAX_RETRY_DOWNLOAD_TIMES);
                } else if (retryDownloadTimes < 0) {
                    mRetryDownloadTimes.put(url, 0);
                } else {
                    Log.i(TAG, "configRetryDownloadTimes 配置下载失败重试次数失败，retryDownloadTimes：" + retryDownloadTimes);
                }
            } else {
                Log.i(TAG, "configRetryDownloadTimes 配置下载失败重试次数失败，retryDownloadTimes：" + retryDownloadTimes);
            }
            return this;
        }

        /**
         * config connect timeout
         *
         * @param url            file url
         * @param connectTimeout please set {@link#MIN_CONNECT_TIMEOUT} to {@link#MAX_CONNECT_TIMEOUT}, if not set,
         *                       default is {@link#DEFAULT_CONNECT_TIMEOUT}, millisecond
         * @return the builder
         */
        protected InnerBuilder configConnectTimeoutWithUrl(String url, int connectTimeout) {
            if (UrlUtil.isUrl(url)) {
                if (connectTimeout >= MIN_CONNECT_TIMEOUT && connectTimeout <= MAX_CONNECT_TIMEOUT) {
                    mConnectTimeout.put(url, connectTimeout);
                } else if (connectTimeout > MAX_CONNECT_TIMEOUT) {
                    mConnectTimeout.put(url, MAX_CONNECT_TIMEOUT);
                } else if (connectTimeout < MIN_CONNECT_TIMEOUT) {
                    mConnectTimeout.put(url, MIN_CONNECT_TIMEOUT);
                } else {
                    Log.i(TAG, "configConnectTimeout 配置连接超时时间失败，connectTimeout：" + connectTimeout);
                }
            } else {
                Log.i(TAG, "configConnectTimeout 配置连接超时时间失败，connectTimeout：" + connectTimeout);
            }
            return this;
        }

        /**
         * configRequestMethod
         *
         * @return the builder
         */
        protected InnerBuilder configRequestMethod(String requestMethod) {
            String url = NULL_KEY_FOR_URL;
            configRequestMethodWithUrl(url, requestMethod);
            return this;
        }

        /**
         * configRequestMethod
         *
         * @param url file url
         * @return the builder
         */
        protected InnerBuilder configRequestMethodWithUrl(String url, String requestMethod) {
            if (!TextUtils.isEmpty(requestMethod)) {
                mRequestMethod.put(url, requestMethod);
            } else {
                Log.i(TAG, "configRequestMethodWithUrl 配置请求方法失败，requestMethod：" + requestMethod);
            }
            return this;
        }

        /**
         * build DownloadConfiguration
         *
         * @return the DownloadConfiguration
         */
        public DownloadConfiguration build() {
            return new DownloadConfiguration(this);
        }
    }

    private static final String TAG = FileDownloadConfiguration.class.getSimpleName();

    /**
     * a temp key for those urls are null
     */
    private static final String NULL_KEY_FOR_URL = DownloadConfiguration.class + "_temp_key_for_null";

    /**
     * default request method
     */
    public static final String DEFAULT_REQUEST_METHOD = "GET";

    // builder
    private InnerBuilder mBuilder;

    private DownloadConfiguration(InnerBuilder builder) {
        mBuilder = builder;
    }

    /**
     * init null key for url
     */
    void initNullKeyForUrl(String url) {
        initNullKeyForUrlInternal(url, false);
    }

    private void initNullKeyForUrlInternal(String url, boolean replaceExistWithNullValue) {

        if (!UrlUtil.isUrl(url) || mBuilder == null) {
            return;
        }

        // init headers
        if (mBuilder.mUrlHeaders != null) {

            Map<String, String> nullHeaders = getHeaders(NULL_KEY_FOR_URL);
            Map<String, String> existUrlHeaders = getHeaders(url);
            Map<String, String> headers = new HashMap<String, String>();

            if (!MapUtil.isEmpty(nullHeaders)) {
                if (replaceExistWithNullValue) {
                    // replace
                    mBuilder.mUrlHeaders.remove(url);
                    headers.putAll(nullHeaders);
                } else {
                    // exist, add nullHeaders and existUrlHeaders
                    if (!MapUtil.isEmpty(existUrlHeaders)) {
                        mBuilder.mUrlHeaders.remove(url);// remove old ones
                        headers.putAll(nullHeaders);
                        headers.putAll(existUrlHeaders);
                    }
                    // add nullHeaders only
                    else {
                        headers.putAll(nullHeaders);
                    }
                }

                Log.e("wlf", "初始化headers：" + headers.size());

                // mBuilder.mUrlHeaders.remove(NULL_KEY_FOR_URL);
                mBuilder.mUrlHeaders.put(url, headers);
            }
        }

        // init retry download times
        if (mBuilder.mRetryDownloadTimes != null) {
            int existUrlRetryDownloadTimes = getRetryDownloadTimes(url);
            int retryDownloadTimes = getRetryDownloadTimes(NULL_KEY_FOR_URL);
            // replace
            if (replaceExistWithNullValue) {
                // exist, replace
                if (existUrlRetryDownloadTimes != InnerBuilder.DEFAULT_RETRY_DOWNLOAD_TIMES) {
                    mBuilder.mRetryDownloadTimes.remove(url);
                    // mBuilder.mRetryDownloadTimes.remove(NULL_KEY_FOR_URL);
                    mBuilder.mRetryDownloadTimes.put(url, retryDownloadTimes);
                }
                // add only
                else {
                    // mBuilder.mRetryDownloadTimes.remove(NULL_KEY_FOR_URL);
                    if (!mBuilder.mRetryDownloadTimes.containsKey(url)) {
                        mBuilder.mRetryDownloadTimes.put(url, retryDownloadTimes);
                    }
                }
            } else {
                // add only
                // mBuilder.mRetryDownloadTimes.remove(NULL_KEY_FOR_URL);
                if (!mBuilder.mRetryDownloadTimes.containsKey(url)) {
                    mBuilder.mRetryDownloadTimes.put(url, retryDownloadTimes);
                }
            }
        }

        // init retry download times
        if (mBuilder.mConnectTimeout != null) {
            int existUrlConnectTimeout = getConnectTimeout(url);
            int connectTimeout = getConnectTimeout(NULL_KEY_FOR_URL);
            // replace
            if (replaceExistWithNullValue) {
                // exist, replace
                if (existUrlConnectTimeout != InnerBuilder.DEFAULT_CONNECT_TIMEOUT) {
                    mBuilder.mConnectTimeout.remove(url);
                    // mBuilder.mConnectTimeout.remove(NULL_KEY_FOR_URL);
                    mBuilder.mConnectTimeout.put(url, connectTimeout);
                }
                // add only
                else {
                    // mBuilder.mConnectTimeout.remove(NULL_KEY_FOR_URL);
                    if (!mBuilder.mConnectTimeout.containsKey(url)) {
                        mBuilder.mConnectTimeout.put(url, connectTimeout);
                    }
                }
            } else {
                // add only
                // mBuilder.mConnectTimeout.remove(NULL_KEY_FOR_URL);
                if (!mBuilder.mConnectTimeout.containsKey(url)) {
                    mBuilder.mConnectTimeout.put(url, connectTimeout);
                }
            }
        }

        // init request method
        if (mBuilder.mRequestMethod != null) {
            String existUrlRequestMethod = getRequestMethod(url);
            String requestMethod = getRequestMethod(NULL_KEY_FOR_URL);
            // replace
            if (replaceExistWithNullValue) {
                // exist, replace
                if (!DEFAULT_REQUEST_METHOD.equalsIgnoreCase(existUrlRequestMethod)) {
                    mBuilder.mRequestMethod.remove(url);
                    mBuilder.mRequestMethod.put(url, requestMethod);
                }
                // add only
                else {
                    if (!mBuilder.mRequestMethod.containsKey(url)) {
                        mBuilder.mRequestMethod.put(url, requestMethod);
                    }
                }
            } else {
                // add only
                if (!mBuilder.mRequestMethod.containsKey(url)) {
                    mBuilder.mRequestMethod.put(url, requestMethod);
                }
            }
        }
    }

    /**
     * init null key for urls
     */
    void initNullKeyForUrls(List<String> urls) {

        if (CollectionUtil.isEmpty(urls) || mBuilder == null) {
            return;
        }

        Log.e("wlf", "初始化urls：" + urls.size());

        for (String url : urls) {
            if (!UrlUtil.isUrl(url)) {
                continue;
            }

            initNullKeyForUrlInternal(url, false);
        }
    }

    // getters

    /**
     * get custom headers
     *
     * @param url file url
     * @return custom headers for download
     */
    public Map<String, String> getHeaders(String url) {
        if (!UrlUtil.isUrl(url) || mBuilder == null || mBuilder.mUrlHeaders == null) {
            return null;
        }
        return mBuilder.mUrlHeaders.get(url);
    }

    /**
     * get RetryDownloadTimes
     *
     * @param url file url
     * @return retry download times
     */
    public int getRetryDownloadTimes(String url) {
        if (!UrlUtil.isUrl(url) || mBuilder == null || mBuilder.mRetryDownloadTimes == null) {
            return InnerBuilder.DEFAULT_RETRY_DOWNLOAD_TIMES;
        }

        Integer retryDownloadTimes = mBuilder.mRetryDownloadTimes.get(url);
        if (retryDownloadTimes == null) {
            return InnerBuilder.DEFAULT_RETRY_DOWNLOAD_TIMES;
        }
        return retryDownloadTimes;
    }

    /**
     * get connect timeout
     *
     * @param url file url
     * @return connect timeout
     */
    public int getConnectTimeout(String url) {
        if (!UrlUtil.isUrl(url) || mBuilder == null || mBuilder.mConnectTimeout == null) {
            return InnerBuilder.DEFAULT_CONNECT_TIMEOUT;
        }

        Integer connectTimeout = mBuilder.mConnectTimeout.get(url);
        if (connectTimeout == null) {
            return InnerBuilder.DEFAULT_CONNECT_TIMEOUT;
        }
        return connectTimeout;
    }

    /**
     * get request method
     *
     * @return request method
     */
    public String getRequestMethod(String url) {
        if (!UrlUtil.isUrl(url) || mBuilder == null || mBuilder.mRequestMethod == null) {
            return DEFAULT_REQUEST_METHOD;// default is get
        }

        String requestMethod = mBuilder.mRequestMethod.get(url);
        if (TextUtils.isEmpty(requestMethod)) {
            return DEFAULT_REQUEST_METHOD;// default is get
        }
        return requestMethod;
    }
}
