# file-downloader

这是文件安卓上轻量级Http文件下载框架，我的目标是让文件下载越简单越好，尽可能以最简洁明了的方式完成复杂需求。

**一、特点**
* 多任务下载、断线续传、自动重试、快速管理下载文件的生命周期（下载文件的增删改查）等。


**二、截图**
* ![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/simple_download_zh.gif)
* ![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/manager_download_zh.gif)


**三、快速上手使用**
* 第一步、在项目模块的build.gradle配置gradle
``` java
compile 'org.wlf:FileDownloader:0.3.0'
``` 
eclipse用户，可以在这里下载jar包：
**[FileDownloader-0.3.0.jar](https://github.com/wlfcolin/file-downloader/raw/master/download/release/FileDownloader-0.3.0.jar)**, 
**[FileDownloader-0.3.0-sources.jar](https://dl.bintray.com/wlfcolin/maven/org/wlf/FileDownloader/0.3.0/FileDownloader-0.3.0-sources.jar)**

* 第二步、在你的应用的onCreate()中初始化FileDownloader
``` java
// 1、创建Builder
Builder builder = new FileDownloadConfiguration.Builder(this);

// 2.配置Builder
// 配置下载文件保存的文件夹
builder.configFileDownloadDir(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
        "FileDownloader");
// 配置同时下载任务数量，默认为2
builder.configDownloadTaskSize(3);
// 配置失败时尝试重试的次数，默认为0不尝试
builder.configRetryDownloadTimes(5);
// 开启调试模式，方便查看日志，默认不开启
builder.configDebugMode(true);
// 配置连接网络超时时间，如果不配做默认15秒
builder.configConnectTimeout(25000);

// 3、使用配置文件初始化FileDownloader
FileDownloadConfiguration configuration = builder.build(); // build FileDownloadConfiguration with the builder
FileDownloader.init(configuration);
```

* 第三步、注册监听器（如果不需要监听，可以忽略）

-注册下载状态监听器(一搬在fragment或activity的onCreate方法中注册)
``` java
private OnFileDownloadStatusListener mOnFileDownloadStatusListener = new OnRetryableFileDownloadStatusListener() {
    @Override
    public void onFileDownloadStatusRetrying(DownloadFileInfo downloadFileInfo, int retryTimes) {
        // 正在重试下载（如果你配置了重试次数，当一旦下载失败时会尝试重试下载）
    }
    @Override
    public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {
        // 等待下载（等待其它任务执行完成，或者FileDownloader还没准备好下载）
    }
    @Override
    public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {
        // 准备中（即，正在连接资源）
    }
    @Override
    public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {
        // 已准备好（即，已经连接到了资源）
    }
    @Override
    public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long
            remainingTime) {
        // 正在下载
    }
    @Override
    public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {
        // 下载已被暂停
    }
    @Override
    public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {
        // 下载完成（整个文件已经全部下载完成）
    }
    @Override
    public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, FileDownloadStatusFailReason failReason) {
        // 下载失败了，详细查看失败原因failReason
        String failType = failReason.getType();
        if(FileDownloadStatusFailReason.TYPE_URL_ILLEGAL.equals(failType)){
            // url有错误
        }else if(FileDownloadStatusFailReason.TYPE_STORAGE_SPACE_IS_FULL.equals(failType)){
            // 本地存储空间不足
        }else if(FileDownloadStatusFailReason.TYPE_NETWORK_DENIED.equals(failType)){
            // 无法访问网络
        }else if(FileDownloadStatusFailReason.TYPE_NETWORK_TIMEOUT.equals(failType)){
            // 连接超时
        }else{
            // 更多错误....
        }
    }
};
FileDownloader.registerDownloadStatusListener(mOnFileDownloadStatusListener);
```

-注册文件数据变化监听器，监听比如删除，移动重命名，特殊状态改变等任何与文件数据变化相关都会收到通知
``` java
private OnDownloadFileChangeListener mOnDownloadFileChangeListener = new OnDownloadFileChangeListener() {
    @Override
    public void onDownloadFileCreated(DownloadFileInfo downloadFileInfo) {
        // 一个新下载文件被创建，也许你需要同步你自己的数据存储，比如在你的业务数据库中增加一条记录
    }
    @Override
    public void onDownloadFileUpdated(DownloadFileInfo downloadFileInfo, Type type) {
        // 一个下载文件被更新，也许你需要同步你自己的数据存储，比如在你的业务数据库中更新一条记录
    }
    @Override
    public void onDownloadFileDeleted(DownloadFileInfo downloadFileInfo) {
        // 一个下载文件被删除，也许你需要同步你自己的数据存储，比如在你的业务数据库中删除一条记录
    }
};
FileDownloader.registerDownloadFileChangeListener(mOnDownloadFileChangeListener);
```
下载状态监听器和文件数据变化监听器的主要区别在于，前者关心下载进度和错误（前端UI），后者关心文件数据变化（数据存储）

* 第四步、下载文件和管理文件

-创建一个新下载
``` java
FileDownloader.start(url);// 如果文件没被下载过，将创建并开启下载，否则继续下载，自动会断点续传
```

-创建一个自定义保存路径和文件名称的下载
``` java
FileDownloader.detect(url, new OnDetectBigUrlFileListener() {
    @Override
    public void onDetectNewDownloadFile(String url, String fileName, String saveDir, long fileSize) {
        // 如果有必要，可以改变文件名称fileName和下载保存的目录saveDir
        FileDownloader.createAndStart(url, newFileDir, newFileName);
    }
    @Override
    public void onDetectUrlFileExist(String url) {
        // 继续下载，自动会断点续传
        FileDownloader.start(url);
    }
    @Override
    public void onDetectUrlFileFailed(String url, DetectBigUrlFileFailReason failReason) {
        // 探测一个网络文件失败了，具体查看failReason
    }
});
```

-暂停下载
``` java
FileDownloader.pause(url);// 暂停单个下载任务
FileDownloader.pause(urls);// 暂停多个下载任务
FileDownloader.pauseAll();// 暂停所有下载任务
```

-继续下载
``` java
FileDownloader.start(url);// 继续下载，自动会断点续传
```

-移动下载文件
``` java
FileDownloader.move(url, newDirPath, mOnMoveDownloadFileListener);// 移动单个下载文件到新文件夹中
FileDownloader.move(urls, newDirPath, mOnMoveDownloadFilesListener);// 移动多个下载文件到新文件夹中
```

-删除下载文件
``` java
FileDownloader.delete(url, true, mOnDeleteDownloadFileListener);// 删除单个下载文件
FileDownloader.delete(urls, true, mOnDeleteDownloadFilesListener);// 删除多个下载文件
```

-重命名下载文件
``` java
FileDownloader.rename(url, newName, true, mOnRenameDownloadFileListener);// 重命名一个下载文件
```

* 第五步、取消注册的监听器

-取消注册下载状态监听器(一搬在fragment或activity的onDestroy方法中取消注册)
``` java
FileDownloader.unregisterDownloadStatusListener(mOnFileDownloadStatusListener);
```

-注册文件数据变化监听器
``` java
FileDownloader.unregisterDownloadFileChangeListener(mOnDownloadFileChangeListener);
```


**[四、详细API文档](http://htmlpreview.github.io/?https://raw.githubusercontent.com/wlfcolin/file-downloader/master/download/release/FileDownloader-0.3.0-javadoc/index.html)**


**[五、版本更新日志](https://github.com/wlfcolin/file-downloader/blob/master/CHANGELOG.md)**


**六、升级最新说明**

* 0.2.X --> 0.3.0

-建议替换用FileDownloader.detect(String, OnDetectBigUrlFileListener)替换掉FileDownloader.detect(String, OnDetectUrlFileListener)。

-建议使用FileDownloader.registerDownloadStatusListener(OnRetryableFileDownloadStatusListener)替换调用FileDownloader.registerDownloadStatusListener(OnFileDownloadStatusListener)以获得更好的体验。

-如果你注册了监听器，务必不要忘记在新版的合适时机取消注册unregisterDownloadStatusListener(OnFileDownloadStatusListener)和unregisterDownloadFileChangeListener(OnDownloadFileChangeListener)，以防止引起不必须得内存泄露。

* 0.1.X --> 0.3.0

-建议使用类FileDownloader替换掉类FileDownloadManager，同时对应的方法也替换掉。


**[七、设计](https://github.com/wlfcolin/file-downloader/blob/master/DESIGN.md)**


**八、LICENSE**
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
