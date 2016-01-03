package org.wlf.filedownloader;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * the Configuration of FileDownload
 * <br/>
 * 文件下载的配置类
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class FileDownloadConfiguration {

    /**
     * LOG TAG
     */
    private static final String TAG = FileDownloadConfiguration.class.getSimpleName();

    private Context mContext;
    /**
     * download dir
     */
    private String mFileDownloadDir;
    /**
     * engine use for downloading file
     */
    private ExecutorService mFileDownloadEngine;
    /**
     * engine use for detecting url file
     */
    private ExecutorService mFileDetectEngine;
    /**
     * engine use for operate downloaded file
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
        this.mContext = builder.mContext;
        this.mFileDownloadDir = builder.mFileDownloadDir;
        this.mFileDownloadEngine = Executors.newFixedThreadPool(builder.mDownloadTaskSize);
        this.mFileDetectEngine = Executors.newCachedThreadPool(); // no limit
        this.mFileOperationEngine = Executors.newFixedThreadPool(3); // limit 3, for operations: move,delete,rename
    }

    // getters

    /**
     * get Context
     *
     * @return Context
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * get FileDownloadDir
     *
     * @return FileDownloadDir
     */
    public String getFileDownloadDir() {
        return mFileDownloadDir;
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

    /**
     * Configuration Builder
     */
    public static class Builder {

        /**
         * max download task at the same time,max 5
         */
        public static final int MAX_DOWNLOAD_TASK_SIZE = 5;
        /**
         * default download task at the same time,default 2
         */
        public static final int DEFAULT_DOWNLOAD_TASK_SIZE = 2;

        private Context mContext;
        private String mFileDownloadDir;
        private int mDownloadTaskSize = -1;

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
        }

        /**
         * configFileDownloadDir
         *
         * @param fileDownloadDir FileDownloadDir,if use sdcard,please add permission:  android.permission
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
            if (downloadTaskSize > 0 && downloadTaskSize <= MAX_DOWNLOAD_TASK_SIZE) {
                this.mDownloadTaskSize = downloadTaskSize;
            } else if (downloadTaskSize > MAX_DOWNLOAD_TASK_SIZE) {
                this.mDownloadTaskSize = MAX_DOWNLOAD_TASK_SIZE;
            } else {
                Log.i(TAG, "configDownloadTaskSize 配置同时下载任务的数量失败，downloadTaskSize：" + downloadTaskSize);
            }
            return this;
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
}
