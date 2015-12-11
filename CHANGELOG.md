# file-downloader Version Change Log

**0.2.0-preview**

* Fix re-download the downloaded size is bigger than 100%
* Fix preparing status never been called
* Fix multi deletes sometimes callback not sync with the status
* Fix delete exception in waiting status
* Fix re-download save dir exception when ever moved to other dir
* Fix can not start after paused in the status under waiting,preparing,prepared

* Introduced class FileDownloader instead of deprecated FileDownloadManager(FileDownloadManager also can use)
* Introduced interface OnDownloadFileChangeListener to listen DownloadFileChange(created,updated,deleted)
* New API in FileDownloadManager(also in FileDownloader): *
 * registerDownloadStatusListener(OnFileDownloadStatusListener)
 * unregisterDownloadStatusListener(OnFileDownloadStatusListener)
 * registerDownloadFileChangeListener(OnDownloadFileChangeListener)
 * unregisterDownloadFileChangeListener(OnDownloadFileChangeListener)
* Deprecated in FileDownloadManager: *
 * createAndStart(String, String, String,OnFileDownloadStatusListener)-->createAndStart(String, String, String) instead
 * start(String, OnFileDownloadStatusListener)-->start(String) instead
 * start(List<String>, OnFileDownloadStatusListener)-->start(List<String>) instead
 * reStart(String, OnFileDownloadStatusListener)-->reStart(String) instead
 * reStart(List<String>, OnFileDownloadStatusListener)-->reStart(List<String>) instead
 * getDownloadFileBySavePath(String, boolean)-->getDownloadFileByTempPath(String) instead
* Update in FileDownloadManager: *
 * Add return Control in move(List<String>, String, OnMoveDownloadFilesListener)
 * Add return Control in delete(List<String>, boolean, OnDeleteDownloadFilesListener)

**0.1.0**
* Initial version
