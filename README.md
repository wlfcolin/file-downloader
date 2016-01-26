# file-downloader

FileDownloader, a powerful http/https file download tool, I am to make downloading http/https file easily on Android.

[中文说明文档](https://github.com/wlfcolin/file-downloader/blob/master/README-zh.md)

**Features**
* Multi task at same time, Broken-point download, Auto retry, Support download large file bigger than 2G, Powerful to deal with exceptions, download files CRUD and so on.

----------------------------------------------------------------------
**Captures**
* ![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/simple_download.gif)
* ![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/manager_download.gif)

----------------------------------------------------------------------
**Quick start**
* step 1. add in dependencies in your module's build.gradle
``` java
compile 'org.wlf:FileDownloader:0.3.1'
``` 
for eclipse users, jars here:
**[FileDownloader-0.3.1.jar](https://github.com/wlfcolin/file-downloader/raw/master/download/release/FileDownloader-0.3.1.jar)**,
**[FileDownloader-0.3.1-sources.jar](https://dl.bintray.com/wlfcolin/maven/org/wlf/FileDownloader/0.3.1/FileDownloader-0.3.1-sources.jar)**

* step 2. init FileDownloader in your application's onCreate()
``` java
// 1.create FileDownloadConfiguration.Builder
Builder builder = new FileDownloadConfiguration.Builder(this);

// 2.config FileDownloadConfiguration.Builder
builder.configFileDownloadDir(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
        "FileDownloader"); // config the download path
// allow 3 download tasks at the same time, if not config, default is 2
builder.configDownloadTaskSize(3);
// config retry download times when failed, if not config, default is 0
builder.configRetryDownloadTimes(5);
// enable debug mode, if not config, default is false
builder.configDebugMode(true);
// config connect timeout, if not config, default is 15s
builder.configConnectTimeout(25000); // 25s

// 3.init FileDownloader with the configuration
FileDownloadConfiguration configuration = builder.build(); // build FileDownloadConfiguration with the builder
FileDownloader.init(configuration);
```

* step 3. register listeners

-register a DownloadStatusListener(may be at the time the fragment or activity's onCreate called, 
you can ignore this if your app do not care the download progress, if you want to use in android service,
see [Use FileDownloader in Service](https://github.com/wlfcolin/file-downloader/blob/master/USEINSERVICE.md))
``` java
private OnFileDownloadStatusListener mOnFileDownloadStatusListener = new OnSimpleFileDownloadStatusListener() {
    @Override
    public void onFileDownloadStatusRetrying(DownloadFileInfo downloadFileInfo, int retryTimes) {
        // retrying download when failed once, the retryTimes is the current trying times
    }
    @Override
    public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {
        // waiting for download(wait for other task paused, or FileDownloader is busy for other operations)
    }
    @Override
    public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {
        // preparing(connecting)
    }
    @Override
    public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {
        // prepared(connected)
    }
    @Override
    public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long
            remainingTime) {
        // downloading, the downloadSpeed with KB/s unit, the remainingTime with seconds unit
    }
    @Override
    public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {
        // download paused
    }
    @Override
    public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {
        // download completed(the url file has been finished)
    }
    @Override
    public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, FileDownloadStatusFailReason failReason) {
        // error occur, see failReason for details, some of the failReason you must concern

        String failType = failReason.getType();
        String failUrl = failReason.getUrl();// or failUrl = url, both url and failReason.getType() are the same

        if(FileDownloadStatusFailReason.TYPE_URL_ILLEGAL.equals(failType)){
            // the url error when downloading file with failUrl
        }else if(FileDownloadStatusFailReason.TYPE_STORAGE_SPACE_IS_FULL.equals(failType)){
            // storage space is full when downloading file with failUrl
        }else if(FileDownloadStatusFailReason.TYPE_NETWORK_DENIED.equals(failType)){
            // network access denied when downloading file with failUrl
        }else if(FileDownloadStatusFailReason.TYPE_NETWORK_TIMEOUT.equals(failType)){
            // connect timeout when downloading file with failUrl
        }else{
            // more....
        }

        // exception details
        Throwable failCause = failReason.getCause();// or failReason.getOriginalCause()

        // also you can see the exception message
        String failMsg = failReason.getMessage();// or failReason.getOriginalCause().getMessage()
    }
};
FileDownloader.registerDownloadStatusListener(mOnFileDownloadStatusListener);
```

-register a DownloadFileChangeListener if you care any change of the download file in db
``` java
private OnDownloadFileChangeListener mOnDownloadFileChangeListener = new OnDownloadFileChangeListener() {
    @Override
    public void onDownloadFileCreated(DownloadFileInfo downloadFileInfo) {
        // a new download file created, may be you need to sync your own data storage status, such as add a record in your own database
    }
    @Override
    public void onDownloadFileUpdated(DownloadFileInfo downloadFileInfo, Type type) {
        // a download file updated, may be you need to sync your own data storage status, such as update a record in your own database
    }
    @Override
    public void onDownloadFileDeleted(DownloadFileInfo downloadFileInfo) {
        // a download file deleted, may be you need to sync your own data storage status, such as delete a record in your own database
    }
};
FileDownloader.registerDownloadFileChangeListener(mOnDownloadFileChangeListener);
```
the difference between DownloadStatusListener and DownloadFileChangeListener is, 
DownloadStatusListener concerns the download progress, and DownloadFileChangeListener concerns the data change 

* step 4. start to download and operate the url files

-create a new download
``` java
FileDownloader.start(url);// start a new download if not download yet, or continue download if it start download before, it will auto Broken-point if the server supported
```

-or create a custom new download
``` java
FileDownloader.detect(url, new OnDetectBigUrlFileListener() {
    @Override
    public void onDetectNewDownloadFile(String url, String fileName, String saveDir, long fileSize) {
        // here to change to custom fileName, saveDir if needed
        FileDownloader.createAndStart(url, newFileDir, newFileName);
    }
    @Override
    public void onDetectUrlFileExist(String url) {
        // continue to download
        FileDownloader.start(url);
    }
    @Override
    public void onDetectUrlFileFailed(String url, DetectBigUrlFileFailReason failReason) {
        // error occur, see failReason for details
    }
});
```

-pause downloads
``` java
FileDownloader.pause(url);// pause single
FileDownloader.pause(urls);// pause multi
FileDownloader.pauseAll();// pause all
```

-continue a paused download
``` java
FileDownloader.start(url);// continue a paused download, it will auto Broken-point if the server supported
```

-move download files to new dir path
``` java
FileDownloader.move(url, newDirPath, mOnMoveDownloadFileListener);// move single file
FileDownloader.move(urls, newDirPath, mOnMoveDownloadFilesListener);// move multi files
```

-delete download files
``` java
FileDownloader.delete(url, true, mOnDeleteDownloadFileListener);// delete single file
FileDownloader.delete(urls, true, mOnDeleteDownloadFilesListener);// delete multi files
```

-rename a download file
``` java
FileDownloader.rename(url, newName, true, mOnRenameDownloadFileListener);
```

* step 5. unregister listeners

-unregister the DownloadStatusListener(may be at the time the fragment or activity's onDestroy called)
``` java
FileDownloader.unregisterDownloadStatusListener(mOnFileDownloadStatusListener);
```

-unregister the DownloadFileChangeListener
``` java
FileDownloader.unregisterDownloadFileChangeListener(mOnDownloadFileChangeListener);
```

----------------------------------------------------------------------
**[API docs](http://htmlpreview.github.io/?https://raw.githubusercontent.com/wlfcolin/file-downloader/master/download/release/FileDownloader-0.3.1-javadoc/index.html)**

----------------------------------------------------------------------
**[Version change log](https://github.com/wlfcolin/file-downloader/blob/master/CHANGELOG.md)**

----------------------------------------------------------------------
**Upgrade to latest version help**

* 0.2.X --> 0.3.X

-replace FileDownloader.detect(String, OnDetectUrlFileListener) with FileDownloader.detect(String, OnDetectBigUrlFileListener) for supporting large file bigger than 2G to detect recommended.

-replace DownloadFileInfo.getFileSize() with DownloadFileInfo.getFileSizeLong(), and also replace DownloadFileInfo.getDownloadedSize() with DownloadFileInfo.getDownloadedSizeLong() for supporting large file bigger than 2G to detect recommended.

-replace FileDownloader.registerDownloadStatusListener(OnFileDownloadStatusListener) with FileDownloader.registerDownloadStatusListener(OnSimpleFileDownloadStatusListener or OnRetryableFileDownloadStatusListener) for better experience.

-do not forget to do unregisterDownloadStatusListener(OnFileDownloadStatusListener) and unregisterDownloadFileChangeListener(OnDownloadFileChangeListener) if your registered then, because the listeners may cause memory overflow if you never unregister then.

-replace all FailReason classes started with On by those ones started with non-On, eg. OnFileDownloadStatusFailReason -->FileDownloadStatusFailReason.

* 0.1.X --> 0.3.X

-replace class FileDownloadManager with new class FileDownloader, also replace all methods in the FileDownloadManager with new ones recommended.

-do steps in 0.2.X --> 0.3.X

----------------------------------------------------------------------
**[Design of FileDownloader](https://github.com/wlfcolin/file-downloader/blob/master/DESIGN.md)**

----------------------------------------------------------------------
**Code Style, follow [Google Android Code Style for Contributors](http://source.android.com/source/code-style.html), contributors are required to follow the rules**

----------------------------------------------------------------------
**Supported system version: API 8 and above(Android 2.2+)**

----------------------------------------------------------------------
**[FAQ](https://github.com/wlfcolin/file-downloader/blob/master/Q&A.md)**

----------------------------------------------------------------------
**LICENSE**
```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
