# file-downloader

FileDownloader是安卓上轻量级Http文件下载框架，我的目标是让文件下载越简单越好，尽可能以最简洁明了的方式完成复杂需求。

**一、特点**
* 多任务并行下载、自动断点续传、失败自动重试机制、支持大文件（超过2G）下载、强大方便的异常处理和异常恢复机制、轻松管理下载文件的生命周期（下载文件的增删改查）等


**二、非常适合的使用场景**
* 1、需要多页面同步下载进度和下载状态（比如商店APP，A页面开启点击下载，B页面和C页面也可以看到下载进度和暂停这个下载）
* 2、需要断点续传节约流量（比如做批量应用更新APP、单个应用自己更新下载新版本、做影视需要批量缓存视频的APP，做MP3歌曲下载的APP）
* 3、专门做下载的（比如迅雷之类的APP，需要下载任何服务器上的东西，并且兼顾下载效率和开发效率的）


**三、截图**
* ![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/simple_download_zh.gif)
* ![image](https://github.com/wlfcolin/file-downloader/blob/master/capture/manager_download_zh.gif)


**四、快速上手使用**
* 第一步、在项目模块的build.gradle配置gradle
``` java
compile 'org.wlf:FileDownloader:0.3.1'
``` 
eclipse用户，可以在这里下载jar包：
**[FileDownloader-0.3.1.jar](https://github.com/wlfcolin/file-downloader/raw/master/download/release/FileDownloader-0.3.1.jar)**,
**[FileDownloader-0.3.1-sources.jar](https://dl.bintray.com/wlfcolin/maven/org/wlf/FileDownloader/0.3.1/FileDownloader-0.3.1-sources.jar)**

* 第二步、在你的应用application的onCreate()中初始化FileDownloader
``` java
// 1、创建Builder
Builder builder = new FileDownloadConfiguration.Builder(this);

// 2.配置Builder
// 配置下载文件保存的文件夹
builder.configFileDownloadDir(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
        "FileDownloader");
// 配置同时下载任务数量，如果不配置默认为2
builder.configDownloadTaskSize(3);
// 配置失败时尝试重试的次数，如果不配置默认为0不尝试
builder.configRetryDownloadTimes(5);
// 开启调试模式，方便查看日志等调试相关，如果不配置默认不开启
builder.configDebugMode(true);
// 配置连接网络超时时间，如果不配置默认为15秒
builder.configConnectTimeout(25000);// 25秒

// 3、使用配置文件初始化FileDownloader
FileDownloadConfiguration configuration = builder.build();
FileDownloader.init(configuration);
```

* 第三步、注册监听器（如果不需要监听，可以忽略），监听器是基于观察者模式设计的全局监听器，可以设置多个，记得在不需要的时候取消注册

-注册下载状态监听器(一般在fragment或activity的onCreate方法中注册，如果你使用service，请查看[在Service中使用FileDownloader](https://github.com/wlfcolin/file-downloader/blob/master/USEINSERVICE-zh.md))
``` java
private OnFileDownloadStatusListener mOnFileDownloadStatusListener = new OnSimpleFileDownloadStatusListener() {
    @Override
    public void onFileDownloadStatusRetrying(DownloadFileInfo downloadFileInfo, int retryTimes) {
        // 正在重试下载（如果你配置了重试次数，当一旦下载失败时会尝试重试下载），retryTimes是当前第几次重试
    }
    @Override
    public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {
        // 等待下载（等待其它任务执行完成，或者FileDownloader在忙别的操作）
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
        // 正在下载，downloadSpeed为当前下载速度，单位KB/s，remainingTime为预估的剩余时间，单位秒
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
        // 下载失败了，详细查看失败原因failReason，有些失败原因你可能必须关心

        String failType = failReason.getType();
        String failUrl = failReason.getUrl();// 或：failUrl = url，url和failReason.getType()会是一样的

        if(FileDownloadStatusFailReason.TYPE_URL_ILLEGAL.equals(failType)){
            // 下载failUrl时出现url错误
        }else if(FileDownloadStatusFailReason.TYPE_STORAGE_SPACE_IS_FULL.equals(failType)){
            // 下载failUrl时出现本地存储空间不足
        }else if(FileDownloadStatusFailReason.TYPE_NETWORK_DENIED.equals(failType)){
            // 下载failUrl时出现无法访问网络
        }else if(FileDownloadStatusFailReason.TYPE_NETWORK_TIMEOUT.equals(failType)){
            // 下载failUrl时出现连接超时
        }else{
            // 更多错误....
        }

        // 查看详细异常信息
        Throwable failCause = failReason.getCause();// 或：failReason.getOriginalCause()

        // 查看异常描述信息
        String failMsg = failReason.getMessage();// 或：failReason.getOriginalCause().getMessage()
    }
};
FileDownloader.registerDownloadStatusListener(mOnFileDownloadStatusListener);
```

-注册文件数据变化监听器，监听比如文件不存在了，被删除了，状态变化了等任何与文件数据变化相关都会收到通知
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
FileDownloader.start(url);// 如果文件没被下载过，将创建并开启下载，否则继续下载，自动会断点续传（如果服务器无法支持断点续传将从头开始下载）
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
        // 继续下载，自动会断点续传（如果服务器无法支持断点续传将从头开始下载）
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
FileDownloader.start(url);// 继续下载，自动会断点续传（如果服务器无法支持断点续传将从头开始下载）
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

-取消注册下载状态监听器(一般在fragment或activity的onDestroy方法中取消注册)
``` java
FileDownloader.unregisterDownloadStatusListener(mOnFileDownloadStatusListener);
```

-取消注册文件数据变化监听器
``` java
FileDownloader.unregisterDownloadFileChangeListener(mOnDownloadFileChangeListener);
```


**[五、详细API文档](http://htmlpreview.github.io/?https://raw.githubusercontent.com/wlfcolin/file-downloader/master/download/release/FileDownloader-0.3.1-javadoc/index.html)**


**[六、版本更新日志](https://github.com/wlfcolin/file-downloader/blob/master/CHANGELOG.md)**


**七、旧版升级到最新版帮助说明**

* 0.2.X --> 0.3.X

-建议替换用FileDownloader.detect(String, OnDetectBigUrlFileListener)替换掉FileDownloader.detect(String, OnDetectUrlFileListener)，以支持超过2G的大文件下载。

-建议用DownloadFileInfo.getFileSizeLong()替换掉DownloadFileInfo.getFileSize()，用DownloadFileInfo.getDownloadedSizeLong()替换掉DownloadFileInfo.getDownloadedSize()，以便能正常显示超过2G文件的大小。

-建议用FileDownloader.registerDownloadStatusListener(OnSimpleFileDownloadStatusListener 或者 OnRetryableFileDownloadStatusListener)替换掉FileDownloader.registerDownloadStatusListener(OnFileDownloadStatusListener)，以获得更好的体验。

-如果你注册了监听器，务必不要忘记在新版的合适时机取消注册unregisterDownloadStatusListener(OnFileDownloadStatusListener)和unregisterDownloadFileChangeListener(OnDownloadFileChangeListener)，以防止不必要的内存泄露麻烦。

-所有以On开头的失败原因类（如：OnFileDownloadStatusFailReason）都替换成没有On开头的失败原因类（如：FileDownloadStatusFailReason）

* 0.1.X --> 0.3.X

-建议使用类FileDownloader替换掉类FileDownloadManager，同时对应的方法也替换掉。

-执行0.2.X --> 0.3.X中的说明。


**[八、框架设计](https://github.com/wlfcolin/file-downloader/blob/master/DESIGN.md)**


**九、编码规范，遵循[谷歌安卓官方编码规范](http://source.android.com/source/code-style.html)，同时要求参与的贡献者也遵循该规范**


**十、支持的系统版本：API 8及以上（安卓2.2及以上）**


**十一、[疑问解答](https://github.com/wlfcolin/file-downloader/blob/master/Q&A-zh.md)**

**十二、LICENSE**
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


**欢迎加入QQ讨论群：479868413**
