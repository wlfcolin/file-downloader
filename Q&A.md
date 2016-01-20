# file-downloader Question and Answer

----------------------------------------------------------------------
Q: if i call the FileDownloader.start(url) twice with the same url, whether FileDownloader will download the url file twice ?

A: No, FileDownloader will never download the file that has been downloaded, in this cause, if FileDownloader is downloading the url file, the second call the same url will do nothing.

----------------------------------------------------------------------
Q: if FileDownloader can auto download a file that has been deleted without FileDownloader.delete() in local storage file system such as sdcard by calling FileDownloader.start(url) ?

A: No, FileDownloader will tell you the status DOWNLOAD_STATUS_FILE_NOT_EXIST through DownloadFileInfo.getStatus() or FileDownloadStatusFailReason.TYPE_SAVE_FILE_NOT_EXIST in OnFileDownloadStatusListener.onFileDownloadStatusFailed callback.

----------------------------------------------------------------------
Q: if the server url file change a new file, but the url is not changed, whether the FileDownloader will reStart download the url file automatic
A: No, if the server provided eTag, FileDownloader will tell you FileDownloadStatusFailReason.TYPE_DOWNLOAD_FILE_ERROR (since 0.3.1,the type will be FileDownloadStatusFailReason.TYPE_URL_FILE_CHANGED) in OnFileDownloadStatusListener.onFileDownloadStatusFailed callback.
