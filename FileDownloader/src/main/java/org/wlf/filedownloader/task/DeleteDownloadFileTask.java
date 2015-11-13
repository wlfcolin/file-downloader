package org.wlf.filedownloader.task;

import java.io.File;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.DownloadFileCacher;
import org.wlf.filedownloader.lisener.OnDeleteDownloadFileListener;

/**
 * 删除下载文件任务
 * 
 * @author wlf
 * 
 */
public class DeleteDownloadFileTask implements Runnable {

	private static final String TAG = DeleteDownloadFileTask.class.getSimpleName();

	private String url;
	private boolean deleteDownloadedFile;
	private DownloadFileCacher fileDownloadCacher;

	private OnDeleteDownloadFileListener mOnDeleteDownloadFileListener;

	public DeleteDownloadFileTask(String url, boolean deleteDownloadedFile, DownloadFileCacher fileDownloadCacher) {
		super();
		this.url = url;
		this.deleteDownloadedFile = deleteDownloadedFile;
		this.fileDownloadCacher = fileDownloadCacher;
	}

	public void setOnDeleteDownloadFileListener(OnDeleteDownloadFileListener onDeleteDownloadFileListener) {
		this.mOnDeleteDownloadFileListener = onDeleteDownloadFileListener;
	}

	@Override
	public void run() {

		DownloadFileInfo downloadFileInfo = fileDownloadCacher.getDownloadFile(url);

		OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFilePrepared(downloadFileInfo,
				mOnDeleteDownloadFileListener);

		// 删除
		boolean deleteResult = fileDownloadCacher.deleteDownloadFile(downloadFileInfo);
		if (deleteResult) {
			if (deleteDownloadedFile) {
				File file = new File(downloadFileInfo.getFileDir(), downloadFileInfo.getFileName());
				if (file != null && file.exists()) {
					deleteResult = file.delete();
				}
			}
		}

		if (deleteResult) {
			// 删除成功
			OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFileSuccess(downloadFileInfo,
					mOnDeleteDownloadFileListener);
		} else {
			// 删除失败
			OnDeleteDownloadFileListener.MainThreadHelper.onDeleteDownloadFileFailed(downloadFileInfo, null,
					mOnDeleteDownloadFileListener);
		}
	}

}
