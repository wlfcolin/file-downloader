# file-downloader Version Change Log

**0.3.2**
* Fix the bug that can not download the URL with encoded chinese
* Fix the bug that will call OnFileDownloadStatusListener.onFileDownloadStatusCompleted twice sometimes
* Fix the bug that delete/move/rename will both call fail callback and success
* Fix the bug that can not detect file by redirecting over one time
* Fix the bug that can not auto detect file name when the URL included params
* Fix the bug that it is will overwrite the file that the exist one with the file name
* Fix the bug that it may case the file error or the download status/size in a exception when using pause after the start operation in a short time

* New API in FileDownloader
 * detect(String, OnDetectBigUrlFileListener, DownloadConfiguration)
 * createAndStart(String, String, String, DownloadConfiguration)
 * start(String, DownloadConfiguration)
 * start(List<String>, DownloadConfiguration)
 * continueAll(boolean, DownloadConfiguration)
 * reStart(String, final DownloadConfiguration)
 * reStart(List<String>, DownloadConfiguration)

* New Features
 * add https URL support
 * add DownloadConfiguration to support download config such as http heads,timeout and so on

**0.3.1**
* Fix the bug that can not download file from some php servers
* Fix the bug that the method FileDownloader.registerDownloadFileChangeListener(OnDownloadFileChangeListener, DownloadFileChangeConfiguration) is not static
* Fix the bug that calling FileDownloader.start will case the download file size error when the file does not exist in file system

* Introduced abstract class OnSimpleFileDownloadStatusListener to implements OnRetryableFileDownloadStatusListener for easy use

* New API in those classes extends from UrlFailReason such as FileDownloadStatusFailReason, DeleteDownloadFileFailReason, MoveDownloadFileFailReason, RenameDownloadFileFailReason:
 * String getUrl()

* New API in FileDownloader:
 * continueAll(boolean)


**0.3.0**
* Fix can not call the listener's method some times because of the weak reference of the listener
* Fix the wrong status that delete the file in file system when downloading
* Fix reStart will not access the url file correct after a long time
* Fix the detect file cache can not auto release bug
* Fix other known bugs

* Introduced interface OnRetryableFileDownloadStatusListener to support retry download
* Introduced interface OnDetectBigUrlFileListener to support detect big url file which file size more than 2G

* New API in FileDownloadConfiguration.Builder:
 * configRetryDownloadTimes(int)
 * configConnectTimeout(int)
 * configDebugMode(boolean)

* New API in FileDownloader:
 * registerDownloadFileChangeListener(OnDownloadFileChangeListener, DownloadFileChangeConfiguration)
 * registerDownloadStatusListener(OnFileDownloadStatusListener, DownloadStatusConfiguration)

* Deprecated in DownloadFileInfo:
 * getFileSize()-->getFileSizeLong() instead
 * getDownloadedSize()-->getDownloadedSizeLong() instead

* Deprecated in FileDownloader:
 * detect(String,OnDetectUrlFileListener)-->detect(String,OnDetectBigUrlFileListener) instead

* Deleted in DownloadFileInfo
 * getFilePath(boolean)


**0.2.2**
* Fix the url bug which includes params
* Fix crash when some callback listeners are null 


**0.2.1**
* Fix can not multi operation in detect url file, delete file, move file and rename file
* Fix the callback not correct when network error


**0.2.0**

* Fix re-download the downloaded size is bigger than 100%
* Fix waiting status never been called
* Fix multi deletes sometimes callback not sync with the status
* Fix delete exception in waiting status
* Fix re-download save dir exception when ever moved to other dir
* Fix can not start after paused in the status under waiting,preparing,prepared

* Introduced class FileDownloader instead of deprecated FileDownloadManager(FileDownloadManager is also available)
* Introduced interface OnDownloadFileChangeListener to listen DownloadFileChange(created,updated,deleted)

* New API in FileDownloadManager(also in FileDownloader):
 * registerDownloadStatusListener(OnFileDownloadStatusListener)
 * unregisterDownloadStatusListener(OnFileDownloadStatusListener)
 * registerDownloadFileChangeListener(OnDownloadFileChangeListener)
 * unregisterDownloadFileChangeListener(OnDownloadFileChangeListener)
 
* Deprecated in FileDownloadManager:
 * createAndStart(String, String, String,OnFileDownloadStatusListener)-->createAndStart(String, String, String) instead
 * start(String, OnFileDownloadStatusListener)-->start(String) instead
 * start(List<String>, OnFileDownloadStatusListener)-->start(List<String>) instead
 * reStart(String, OnFileDownloadStatusListener)-->reStart(String) instead
 * reStart(List<String>, OnFileDownloadStatusListener)-->reStart(List<String>) instead
 * getDownloadFileBySavePath(String, boolean)-->getDownloadFileByTempPath(String) instead
 * getDownloadFileByUrl(String)-->getDownloadFile(String) instead
 
* Update in FileDownloadManager(also in FileDownloader):
 * Add return Control in move(List<String>, String, OnMoveDownloadFilesListener)
 * Add return Control in delete(List<String>, boolean, OnDeleteDownloadFilesListener)


**0.1.0**
* Initial version
