package org.wlf.filedownloader_demo;

import android.app.Application;
import android.os.Environment;

import org.wlf.filedownloader.FileDownloadConfiguration;
import org.wlf.filedownloader.FileDownloadConfiguration.Builder;
import org.wlf.filedownloader.FileDownloadManager;

import java.io.File;

/**
 * Demo Test Application
 * <br/>
 * 测试应用的Application
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class FileDownloadApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // init FileDownloadManager
        initFileDownloadManager();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // release FileDownloadManager
        releaseFileDownloadManager();
    }

    // init FileDownloadManager
    private void initFileDownloadManager() {
        // 1.init FileDownloadConfiguration.Builder
        Builder config = new FileDownloadConfiguration.Builder(this);
        // 2.config FileDownloadConfiguration.Builder
        config.configFileDownloadDir(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "FileDownloader");// config download path
        config.configDownloadTaskSize(3);// allow 3 download task at the same time
        FileDownloadConfiguration configuration = config.build();// build FileDownloadConfiguration
        // 3.init FileDownloadManager
        FileDownloadManager.getInstance(this).init(configuration);
    }

    // release FileDownloadManager
    private void releaseFileDownloadManager() {
        FileDownloadManager.getInstance(this).release();
    }
}
