package org.wlf.filedownloader_demo2;

import android.app.Application;
import android.os.Environment;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.wlf.filedownloader.FileDownloadConfiguration;
import org.wlf.filedownloader.FileDownloadConfiguration.Builder;
import org.wlf.filedownloader.FileDownloader;

import java.io.File;

/**
 * Demo2 Test Application
 * <br/>
 * 测试2应用的Application
 *
 * @author wlf(Andy)
 * @datetime 2015-12-05 10:12 GMT+8
 * @email 411086563@qq.com
 */
public class FileDownloadApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // init FileDownloader
        initFileDownloader();

        // init ImageLoader
        initImageLoader();
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
        // config the download path
        builder.configFileDownloadDir(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
                "FileDownloader2");
        // builder.configFileDownloadDir("/storage/sdcard1/FileDownloader2");

        // allow 3 download task at the same time
        builder.configDownloadTaskSize(3);

        // config retry download times when failed
        builder.configRetryDownloadTimes(5);

        // enable debug mode
        //builder.configDebugMode(true);

        // config connect timeout
        //builder.configConnectTimeout(25000); // 25s

        // 3.init FileDownloader with the configuration
        // build FileDownloadConfiguration with the builder
        FileDownloadConfiguration configuration = builder.build();
        FileDownloader.init(configuration);
    }

    // release FileDownloader
    private void releaseFileDownloader() {
        FileDownloader.release();
    }

    // init ImageLoader
    private void initImageLoader() {
        // Create global configuration and initialize ImageLoader with this config
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);
    }
}
