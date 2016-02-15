package org.wlf.filedownloader.base;

/**
 * BaseDownloadConfigBuilder
 *
 * @author wlf(Andy)
 * @datetime 2016-01-27 15:31 GMT+8
 * @email 411086563@qq.com
 */
public class BaseDownloadConfigBuilder {

    /**
     * max retry download times, max is 10
     */
    public static final int MAX_RETRY_DOWNLOAD_TIMES = 10;
    /**
     * default retry download times, default is 0
     */
    public static final int DEFAULT_RETRY_DOWNLOAD_TIMES = 0;
    /**
     * default connect timeout, default is 15s
     */
    public static final int DEFAULT_CONNECT_TIMEOUT = 15 * 1000;// 15s
    /**
     * default connect timeout, default is 5s
     */
    public static final int MIN_CONNECT_TIMEOUT = 5 * 1000;// 5s
    /**
     * default connect timeout, default is 2min
     */
    public static final int MAX_CONNECT_TIMEOUT = 120 * 1000;// 120s

    protected int mRetryDownloadTimes;
    protected int mConnectTimeout;

    public BaseDownloadConfigBuilder() {
        mRetryDownloadTimes = DEFAULT_RETRY_DOWNLOAD_TIMES;
        mConnectTimeout = DEFAULT_CONNECT_TIMEOUT;
    }

    /**
     * config RetryDownloadTimes
     *
     * @param retryDownloadTimes DownloadTaskSize at the same time, please set 0 to {@link
     *                           #MAX_RETRY_DOWNLOAD_TIMES}, if not set, default is {@link
     *                           #DEFAULT_RETRY_DOWNLOAD_TIMES}, set 0 means not retry
     * @return the builder
     */
    public BaseDownloadConfigBuilder configRetryDownloadTimes(int retryDownloadTimes) {
        if (retryDownloadTimes >= 0 && retryDownloadTimes <= MAX_RETRY_DOWNLOAD_TIMES) {
            this.mRetryDownloadTimes = retryDownloadTimes;
        } else if (retryDownloadTimes > MAX_RETRY_DOWNLOAD_TIMES) {
            this.mRetryDownloadTimes = MAX_RETRY_DOWNLOAD_TIMES;
        } else if (retryDownloadTimes < 0) {
            this.mRetryDownloadTimes = 0;
        } else {
            Log.i(getClass().getSimpleName(), "configRetryDownloadTimes 配置下载失败重试次数失败，retryDownloadTimes：" + 
                    retryDownloadTimes);
        }
        return this;
    }


    /**
     * config connect timeout
     *
     * @param connectTimeout please set {@link#MIN_CONNECT_TIMEOUT} to {@link#MAX_CONNECT_TIMEOUT}, if not set,
     *                       default is {@link#DEFAULT_CONNECT_TIMEOUT}, millisecond
     * @return the builder
     */
    public BaseDownloadConfigBuilder configConnectTimeout(int connectTimeout) {
        if (connectTimeout >= MIN_CONNECT_TIMEOUT && connectTimeout <= MAX_CONNECT_TIMEOUT) {
            mConnectTimeout = connectTimeout;
        } else if (connectTimeout > MAX_CONNECT_TIMEOUT) {
            mConnectTimeout = MAX_CONNECT_TIMEOUT;
        } else if (connectTimeout < MIN_CONNECT_TIMEOUT) {
            mConnectTimeout = MIN_CONNECT_TIMEOUT;
        } else {
            Log.i(getClass().getSimpleName(), "configConnectTimeout 配置连接超时时间失败，connectTimeout：" + connectTimeout);
        }

        return this;
    }
}
