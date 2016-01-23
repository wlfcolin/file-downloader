# 在Service中使用FileDownloader，下面是一个简单的示例

**第一步、初始化FileDownloader**
参考**[中文说明文档](https://github.com/wlfcolin/file-downloader/blob/master/README-zh.md)**
中的：**第二步、在你的应用application的onCreate()中初始化FileDownloader**


**第二步、让你的service实现OnRetryableFileDownloadStatusListener接口，并且在service的onCreate方法中注册当前service为FileDownloader的下载状态监听器，
并在service的onCreate方法中取消注册当前service为FileDownloader的下载状态监听器。**
``` java
public class YourService extends Service implements OnRetryableFileDownloadStatusListener {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        // 将当前service注册为FileDownloader下载状态监听器
        FileDownloader.registerDownloadStatusListener(this);
        // 如果希望service启动就开始下载所有未完成的任务，则开启以下实现
        FileDownloader.continueAll(true);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 将当前service取消注册为FileDownloader下载状态监听器
        FileDownloader.unregisterDownloadStatusListener(this);
       // 如果希望service停止就停止所有下载任务，则开启以下实现
        FileDownloader.pauseAll();// 暂停所有下载任务
    }

    @Override
    public void onFileDownloadStatusRetrying(DownloadFileInfo downloadFileInfo, int retryTimes) {
        // 发送通知或者广播
    }
    @Override
    public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {
        // 发送通知或者广播
    }
    @Override
    public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {
        // 发送通知或者广播
    }
    @Override
    public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {
        // 发送通知或者广播
    }
    @Override
    public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long
            remainingTime) {
        // 发送通知或者广播
    }
    @Override
    public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {
        // 发送通知或者广播
    }
    @Override
    public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {
        // 发送通知或者广播
    }
    @Override
    public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, FileDownloadStatusFailReason failReason) {
        // 发送通知或者广播
    }
}

```


**第三步、在其它地方（比如应用application的onCreate方法，或者MainActivity的onCreate方法）开启service**
``` java
Intent intent = new Intent(this, YourService.class);
startService(intent);
```


**第四步、在其它地方（比如在activity或者fragment中）使用FileDownloader下载API下载文件**
``` java
FileDownloader.detect(url, mOnDetectBigUrlFileListener);
FileDownloader.createAndStart(url, newFileDir, newFileName);// create a custom new download after FileDownloader.detect(url, mOnDetectBigUrlFileListener)
FileDownloader.start(url);// 如果文件没被下载过，将创建并开启下载，否则继续下载，自动会断点续传（如果服务器无法支持断点续传将从头开始下载）
FileDownloader.pause(url);// 暂停单个下载任务
FileDownloader.pause(urls);// 暂停多个下载任务
FileDownloader.pauseAll();// 暂停所有下载任务
```

**第五步、在service实现的OnRetryableFileDownloadStatusListener接口方法中发送通知或者广播出去**
``` java
public class YourService extends Service implements OnRetryableFileDownloadStatusListener {
    @Override
    public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {
        // 如果有必要，发送通知或者广播
    }
    @Override
    public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, FileDownloadStatusFailReason failReason) {
        // 如果有必要，发送通知或者广播
    }
    
    // 省略更多方法....
}
```

**第六步、在其它地方（比如应用application的onTerminate方法，或者MainActivity的onDestroy方法）关闭service**
``` java
Intent intent = new Intent(this, YourService.class);
stopService(intent);
```