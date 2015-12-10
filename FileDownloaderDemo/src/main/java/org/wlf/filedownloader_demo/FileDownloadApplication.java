package org.wlf.filedownloader_demo;

import android.app.Application;
import android.os.Environment;

import org.wlf.filedownloader.FileDownloadConfiguration;
import org.wlf.filedownloader.FileDownloadConfiguration.Builder;
import org.wlf.filedownloader.FileDownloader;

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

        // init FileDownloader
        initFileDownloader();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // release FileDownloader
        releaseFileDownloader();
    }

    // init FileDownloader
    private void initFileDownloader() {
        // 1.create FileDownloadConfiguration.Builder
        Builder builder = new FileDownloadConfiguration.Builder(this);
        // 2.config FileDownloadConfiguration.Builder
        builder.configFileDownloadDir(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
                "FileDownloader");// config the download path
        builder.configDownloadTaskSize(3);// allow 3 download task at the same time
        FileDownloadConfiguration configuration = builder.build();// build FileDownloadConfiguration with the builder
        // 3.init FileDownloader with the configuration
        FileDownloader.init(configuration);
    }

    // release FileDownloader
    private void releaseFileDownloader() {
        FileDownloader.release();
    }
}
