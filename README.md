# file-downloader

this is a powerful http-file download tool, my goal is to make downloading http file easily.

**Usage:**
* 1.add in module build.gradle(Gradle:)
``` java
compile 'org.wlf:FileDownloader:0.1.0'
``` 
* 2.init FileDownloadManager in your application's onCreate() method
``` java
// 1.create FileDownloadConfiguration.Builder
Builder builder = new FileDownloadConfiguration.Builder(this);
// 2.config FileDownloadConfiguration.Builder
builder.configFileDownloadDir(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "FileDownloader");// config the download path
builder.configDownloadTaskSize(3);// allow 3 download task at the same time
FileDownloadConfiguration configuration = builder.build();// build FileDownloadConfiguration with the builder
// 3.init FileDownloadManager with the configuration
FileDownloadManager.getInstance(this).init(configuration);
```

* 3.create a new download
``` java
mFileDownloadManager.start(url, mOnFileDownloadStatusListener);
```

* 4.or create a custom new download
``` java
mFileDownloadManager.detect(url, new OnDetectUrlFileListener() {
    @Override
    public void onDetectNewDownloadFile(String url, String fileName, String saveDir, int fileSize) {
       // change fileName,saveDir if needed
        mFileDownloadManager.createAndStart(url, newFileDir, newFileName, mOnFileDownloadStatusListener);
    }
    @Override
    public void onDetectUrlFileExist(String url) {
        mFileDownloadManager.start(url, mOnFileDownloadStatusListener);
    }
    @Override
    public void onDetectUrlFileFailed(String url, DetectUrlFileFailReason failReason) {
        // error
    }
});
```

* 5.continue a paused download
``` java
mFileDownloadManager.start(url, mOnFileDownloadStatusListener);
```

* 6.move download files to new dir path
``` java
mFileDownloadManager.move(url, newDirPath, mOnMoveDownloadFileListener);// single file
mFileDownloadManager.move(urls, newDirPath, mOnMoveDownloadFilesListener);// multi files
```

* 7.delete download files
``` java
mFileDownloadManager.delete(url, true, mOnDeleteDownloadFileListener);// single file
mFileDownloadManager.delete(urls, true, mOnDeleteDownloadFilesListener);// multi files
```

* 8.rename a download file
``` java
mFileDownloadManager.rename(url, newName, true, mOnRenameDownloadFileListener);
```

------------------------------------------------------------------------
**Some captures:**

* create downloads
![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/device-2015-11-27-160200.png)

* create a download which the url with special character
![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/device-2015-11-27-160214.png)

* create multi downloads
![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/device-2015-11-27-160237.png)

* create a custom download which the url with complex character
![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/device-2015-11-27-160257.png)
![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/device-2015-11-27-160324.png)

* the download ui
![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/device-2015-11-27-160424.png)

* delete multi downloads
![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/device-2015-11-27-160450.png)

* move multi downloads
![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/device-2015-11-27-160508.png)

* rename a download
![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/device-2015-11-27-160538.png)
![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/device-2015-11-27-160545.png)

* start(continue) a paused download
![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/device-2015-11-27-160717.png)
![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/device-2015-11-27-160749.png)

* the download file dir
![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/device-2015-11-27-160808.png)

* the download file record
![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/device-2015-11-27-161739.png)
