package org.wlf.filedownloader;

import android.content.Context;
import android.text.TextUtils;

import org.wlf.filedownloader.base.BaseDownloadConfigBuilder;
import org.wlf.filedownloader.base.Log;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * the Configuration of FileDownloader
 * <br/>
 * 文件下载的配置类（全局）
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class FileDownloadConfiguration {

    /**
     * Configuration Builder
     */
    public static class Builder extends BaseDownloadConfigBuilder {

        /**
         * max download task at the same time, max is 10
         */
        public static final int MAX_DOWNLOAD_TASK_SIZE = 10;
        /**
         * default download task at the same time, default is 2
         */
        public static final int DEFAULT_DOWNLOAD_TASK_SIZE = 2;

        private Context mContext;
        private String mFileDownloadDir;
        private int mDownloadTaskSize;
        private boolean mIsDebugMode = false;

        public Builder(Context context) {
            super();
            this.mContext = context.getApplicationContext();
            // default: /sdcard/Android/data/{package_name}/files/file_downloader
            try {
                mFileDownloadDir = this.mContext.getExternalFilesDir(null).getAbsolutePath() + File.separator +
                        "file_downloader";
            } catch (Exception e) {
                // if there is not sdcard,use /data/data/{package_name}/files/file_downloader for the default
                e.printStackTrace();
                mFileDownloadDir = this.mContext.getFilesDir().getAbsolutePath() + File.separator + "file_downloader";
            }
            mDownloadTaskSize = DEFAULT_DOWNLOAD_TASK_SIZE;

            // set log mode
            Log.setDebugMode(mIsDebugMode);
        }

        /**
         * configFileDownloadDir
         *
         * @param fileDownloadDir FileDownloadDir, if use sdcard, please add permission:  android.permission
         *                        .WRITE_EXTERNAL_STORAGE
         * @return the builder
         */
        public Builder configFileDownloadDir(String fileDownloadDir) {
            if (!TextUtils.isEmpty(fileDownloadDir)) {
                File file = new File(fileDownloadDir);
                if (!file.exists()) {

                    Log.i(TAG, "configFileDownloadDir 要设置的文件下载保存目录：" + fileDownloadDir + " 还不存在，需要创建！");

                    boolean isCreateSuccess = file.mkdirs();

                    if (isCreateSuccess) {
                        Log.i(TAG, "configFileDownloadDir 要设置的文件下载保存目录：" + fileDownloadDir + " 创建成功！");
                    } else {
                        Log.i(TAG, "configFileDownloadDir 要设置的文件下载保存目录：" + fileDownloadDir + " 创建失败！");
                    }

                } else {
                    Log.i(TAG, "configFileDownloadDir 要设置的文件下载保存目录：" + fileDownloadDir + " 已存在，不需要创建！");
                }
                this.mFileDownloadDir = fileDownloadDir;
            }
            return this;
        }

        /**
         * config DownloadTaskSize at the same time
         *
         * @param downloadTaskSize DownloadTaskSize at the same time,please set 1 to {@link #MAX_DOWNLOAD_TASK_SIZE},
         *                         if not set,default is {@link #DEFAULT_DOWNLOAD_TASK_SIZE}
         * @return the builder
         */
        public Builder configDownloadTaskSize(int downloadTaskSize) {
            if (downloadTaskSize >= 1 && downloadTaskSize <= MAX_DOWNLOAD_TASK_SIZE) {
                this.mDownloadTaskSize = downloadTaskSize;
            } else if (downloadTaskSize > MAX_DOWNLOAD_TASK_SIZE) {
                this.mDownloadTaskSize = MAX_DOWNLOAD_TASK_SIZE;
            } else if (downloadTaskSize < 1) {
                this.mDownloadTaskSize = 1;
            } else {
                Log.i(TAG, "configDownloadTaskSize 配置同时下载任务的数量失败，downloadTaskSize：" + downloadTaskSize);
            }
            return this;
        }

        /**
         * config whether is debug, debug mode can print log and some debug operations
         *
         * @param isDebugMode true means debug mode
         * @return the builder
         */
        public Builder configDebugMode(boolean isDebugMode) {
            this.mIsDebugMode = isDebugMode;
            // set log mode
            Log.setDebugMode(mIsDebugMode);
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

        // ---------------------------getters---------------------------

        private int getRetryDownloadTimes() {
            return mRetryDownloadTimes;
        }

        private int getConnectTimeout() {
            return mConnectTimeout;
        }

        /**
         * build FileDownloadConfiguration
         *
         * @return FileDownloadConfiguration instance
         */
        public FileDownloadConfiguration build() {
            return new FileDownloadConfiguration(this);
        }
    }

    private static final String TAG = FileDownloadConfiguration.class.getSimpleName();

    /**
     * the builder
     */
    private Builder mBuilder;

    /**
     * engine use for downloading file
     */
    private ExecutorService mFileDownloadEngine;
    /**
     * engine use for detecting url file
     */
    private ExecutorService mFileDetectEngine;
    /**
     * engine use for operate downloaded file such as delete, move, rename and other async operations
     */
    private ExecutorService mFileOperationEngine;

    /**
     * create default configuration,use {@link Builder#build()} to create recommended
     *
     * @param context Context
     * @return default configuration
     */
    public static FileDownloadConfiguration createDefault(Context context) {
        return new Builder(context).build();
    }

    /**
     * constructor of FileDownloadConfiguration
     *
     * @param builder FileDownloadConfiguration builder
     */
    private FileDownloadConfiguration(Builder builder) {
        if (builder == null) {
            throw new NullPointerException("builder can not be empty!");
        }
        this.mBuilder = builder;
        this.mFileDownloadEngine = Executors.newFixedThreadPool(builder.mDownloadTaskSize);
        this.mFileDetectEngine = Executors.newCachedThreadPool(); // no limit
        this.mFileOperationEngine = Executors.newCachedThreadPool(); // no limit
    }

    // getters

    /**
     * get Context
     *
     * @return Context
     */
    public Context getContext() {
        return mBuilder.mContext;
    }

    /**
     * get FileDownloadDir
     *
     * @return FileDownloadDir
     */
    public String getFileDownloadDir() {
        return mBuilder.mFileDownloadDir;
    }

    /**
     * whether is debug mode, debug mode can print log and some debug operations
     *
     * @return true means debug mode
     */
    public boolean isDebugMode() {
        return mBuilder.mIsDebugMode;
    }

    /**
     * get RetryDownloadTimes
     *
     * @return retry download times
     */
    public int getRetryDownloadTimes() {
        return mBuilder.getRetryDownloadTimes();
    }

    /**
     * get connect timeout
     *
     * @return connect timeout
     */
    public int getConnectTimeout() {
        return mBuilder.getConnectTimeout();
    }

    /**
     * get FileDownloadEngine
     */
    public ExecutorService getFileDownloadEngine() {
        return mFileDownloadEngine;
    }

    /**
     * get FileDetectEngine
     */
    public ExecutorService getFileDetectEngine() {
        return mFileDetectEngine;
    }

    /**
     * get FileOperationEngine
     */
    public ExecutorService getFileOperationEngine() {
        return mFileOperationEngine;
    }
}
