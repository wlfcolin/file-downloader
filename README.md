# file-downloader

this is a powerful http-file download tool, my goal is to make downloading http file easily.

**Usage:**
* 1.add in dependencies of your module's build.gradle(Gradle:)
``` java
compile 'org.wlf:FileDownloader:0.2.2'
``` 
for eclipse users,jars here:
**[FileDownloader-0.2.2.jar](https://github.com/wlfcolin/file-downloader/raw/master/download/release/FileDownloader-0.2.2.jar)**
, **[FileDownloader-0.2.2-sources.jar](https://dl.bintray.com/wlfcolin/maven/org/wlf/FileDownloader/0.2.2/FileDownloader-0.2.2-sources.jar)**

* 2.init FileDownloader in your application's onCreate() method
``` java
// 1.create FileDownloadConfiguration.Builder
Builder builder = new FileDownloadConfiguration.Builder(this);
// 2.config FileDownloadConfiguration.Builder
builder.configFileDownloadDir(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
        "FileDownloader");// config the download path
builder.configDownloadTaskSize(3);// allow 3 download task at the same time
FileDownloadConfiguration configuration = builder.build();// build FileDownloadConfiguration with the builder
// 3.init FileDownloader with the configuration
FileDownloader.init(configuration);
```
the deprecated class **FileDownloadManager** is also available，see [Change Log](https://github.com/wlfcolin/file-downloader/blob/master/CHANGELOG.md)

* 3.register a DownloadStatusListener
``` java
// registerDownloadStatusListener 
FileDownloader.registerDownloadStatusListener(mOnFileDownloadStatusListener);
```

* 4.create a new download
``` java
FileDownloader.start(url);
```

* 5.or create a custom new download
``` java
FileDownloader.detect(url, new OnDetectUrlFileListener() {
    @Override
    public void onDetectNewDownloadFile(String url, String fileName, String saveDir, int fileSize) {
       // change fileName,saveDir if needed
        FileDownloader.createAndStart(url, newFileDir, newFileName);
    }
    @Override
    public void onDetectUrlFileExist(String url) {
        FileDownloader.start(url);
    }
    @Override
    public void onDetectUrlFileFailed(String url, DetectUrlFileFailReason failReason) {
        // error
    }
});
```

* 6.pause a download
``` java
FileDownloader.pause(url);// single
FileDownloader.pause(urls);// multi
FileDownloader.pauseAll();// all
```

* 7.continue a paused download
``` java
FileDownloader.start(url);
```

* 8.move download files to new dir path
``` java
FileDownloader.move(url, newDirPath, mOnMoveDownloadFileListener);// single file
FileDownloader.move(urls, newDirPath, mOnMoveDownloadFilesListener);// multi files
```

* 9.delete download files
``` java
FileDownloader.delete(url, true, mOnDeleteDownloadFileListener);// single file
FileDownloader.delete(urls, true, mOnDeleteDownloadFilesListener);// multi files
```

* 10.rename a download file
``` java
FileDownloader.rename(url, newName, true, mOnRenameDownloadFileListener);
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
