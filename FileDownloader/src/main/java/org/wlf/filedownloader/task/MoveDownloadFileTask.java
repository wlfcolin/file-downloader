package org.wlf.filedownloader.task;

import java.io.File;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.DownloadFileCacher;
import org.wlf.filedownloader.lisener.OnMoveDownloadFileListener;
import org.wlf.filedownloader.lisener.OnMoveDownloadFileListener.OnMoveDownloadFileFailReason;

/**
 * 移动下载文件任务
 * 
 * @author wlf
 * 
 */
public class MoveDownloadFileTask implements Runnable {

	private static final String TAG = MoveDownloadFileTask.class.getSimpleName();

	private String url;
	private String newDirPath;
	private DownloadFileCacher fileDownloadCacher;

	private OnMoveDownloadFileListener mOnMoveDownloadFileListener;

	public MoveDownloadFileTask(String url, String newDirPath, DownloadFileCacher fileDownloadCacher) {
		super();
		this.url = url;
		this.newDirPath = newDirPath;
		this.fileDownloadCacher = fileDownloadCacher;
	}

	public void setOnMoveDownloadFileListener(OnMoveDownloadFileListener onMoveDownloadFileListener) {
		this.mOnMoveDownloadFileListener = onMoveDownloadFileListener;
	}

	@Override
	public void run() {

		DownloadFileInfo downloadFileInfo = fileDownloadCacher.getDownloadFile(url);

		OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFilePrepared(downloadFileInfo,
				mOnMoveDownloadFileListener);

		File oldFile = new File(downloadFileInfo.getFileDir(), downloadFileInfo.getFileName());
		File newFile = new File(newDirPath, downloadFileInfo.getFileName());

		if (oldFile == null || !oldFile.exists()) {
			OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo,
					new OnMoveDownloadFileFailReason("原始文件不存在",
							OnMoveDownloadFileFailReason.TYPE_ORIGINAL_FILE_NOT_EXIST), mOnMoveDownloadFileListener);
			return;
		}

		if (newFile != null && newFile.exists()) {
			OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo,
					new OnMoveDownloadFileFailReason("目标文件已存在", OnMoveDownloadFileFailReason.TYPE_TARGET_FILE_EXIST),
					mOnMoveDownloadFileListener);
			return;
		}

		boolean moveResult = false;

		if (oldFile != null && newFile != null) {
			moveResult = oldFile.renameTo(newFile);
		}

		if (moveResult) {
			// TODO 修改状态
			// 删除成功
			OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileSuccess(downloadFileInfo,
					mOnMoveDownloadFileListener);
		} else {
			// 删除失败
			OnMoveDownloadFileListener.MainThreadHelper.onMoveDownloadFileFailed(downloadFileInfo, null,
					mOnMoveDownloadFileListener);
		}

	}

}
