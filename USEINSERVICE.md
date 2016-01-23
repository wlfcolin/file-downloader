# Use FileDownloader in Service, this is a simple use below

**step 1. init FileDownloader**
see step 2. init FileDownloader in your application's onCreate() in **[README](https://github.com/wlfcolin/file-downloader/blob/master/README.md)**


**step 2. to make your service implements OnRetryableFileDownloadStatusListener
and registerDownloadStatusListener(OnFileDownloadStatusListener) in your service's onCreate()
and unregisterDownloadStatusListener(OnFileDownloadStatusListener) in your service's onDestroy()**
``` java
public class YourService extends Service implements OnRetryableFileDownloadStatusListener {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        // registerDownloadStatusListener
        FileDownloader.registerDownloadStatusListener(this);
        // continue all the download task if you hope that the service started, the download auto start too
        FileDownloader.continueAll(true);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        // unregisterDownloadStatusListener
        FileDownloader.unregisterDownloadStatusListener(this);
        // pause all the download task if you hope that the service stopped, the download auto stop too
        FileDownloader.pauseAll();// pause all
    }

    @Override
    public void onFileDownloadStatusRetrying(DownloadFileInfo downloadFileInfo, int retryTimes) {
        // send a notification or a broadcast if needed
    }
    @Override
    public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {
        // send a notification or a broadcast if needed
    }
    @Override
    public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {
        // send a notification or a broadcast if needed
    }
    @Override
    public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {
        // send a notification or a broadcast if needed
    }
    @Override
    public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long
            remainingTime) {
        // send a notification or a broadcast if needed
    }
    @Override
    public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {
        // send a notification or a broadcast if needed
    }
    @Override
    public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {
        // send a notification or a broadcast if needed
    }
    @Override
    public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, FileDownloadStatusFailReason failReason) {
        // send a notification or a broadcast if needed
    }
}

```


**step 3. start service in other place such as in your application's onCreate() or your MainActivity's onCreate(), also you can use bind service**
``` java
Intent intent = new Intent(this, YourService.class);
startService(intent);
```


**step 4. use FileDownloader download API in other place such as in a activity or a fragment**
``` java
FileDownloader.detect(url, new OnDetectBigUrlFileListener() {// create a custom new download
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
FileDownloader.start(url);// start a new download or continue a paused download, it will auto Broken-point if the server supported
FileDownloader.pause(url);// pause single
FileDownloader.pause(urls);// pause multi
FileDownloader.pauseAll();// pause all
```

**step 5. send a notification or a broadcast in the method that the service implements form OnRetryableFileDownloadStatusListener**
``` java
public class YourService extends Service implements OnRetryableFileDownloadStatusListener {
    @Override
    public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {
        // send a notification or a broadcast if needed
    }
    @Override
    public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, FileDownloadStatusFailReason failReason) {
        // send a notification or a broadcast if needed
    }

    // other methods....
}
```

**step 6. stop the service in other place such as in your application's onTerminate() or your MainActivity's onDestroy()**
``` java
Intent intent = new Intent(this, YourService.class);
stopService(intent);
```