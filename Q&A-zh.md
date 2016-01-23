# file-downloader 疑问解答

----------------------------------------------------------------------
Q: 如果针对同一的url连续调用FileDownloader.start(url)两次会导致这个url对对于的文件下载两次吗？

A: 不会，FileDownloader是通过url来区分一个文件的，如果对同一的url调用下载方法两次，那么第二次将会忽略。

----------------------------------------------------------------------
Q: 如果本地文件被手动或者别的软件删除了（前提是不是调用FileDownloader.delete()方法删除的），那么下次下载会自动重新开始下载吗?

A: 不会，FileDownloader回通过状态告诉你文件不存在了（DownloadFileInfo.getStatus() == DOWNLOAD_STATUS_FILE_NOT_EXIST）也会通过失败回调告诉你文件不存在了（OnFileDownloadStatusListener.onFileDownloadStatusFailed回调中的FileDownloadStatusFailReason.TYPE_SAVE_FILE_NOT_EXIST）.

----------------------------------------------------------------------
Q: 如果服务器文件被替换了，但是下载URL还是一样，请问会自动重新下载这个URL地址一样的新文件吗？
A: 不会，如果服务器支持eTag，FileDownloader会通过失败回调告诉你文件改变了（FileDownloadStatusFailReason.TYPE_DOWNLOAD_FILE_ERROR，0.3.1之后这种情况失败改为FileDownloadStatusFailReason.TYPE_URL_FILE_CHANGED）。
