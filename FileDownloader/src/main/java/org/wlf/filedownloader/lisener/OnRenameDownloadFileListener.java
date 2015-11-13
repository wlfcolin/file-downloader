package org.wlf.filedownloader.lisener;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.FailReason;

/**
 * 重命名下载文件监听接口
 * 
 * @author wlf
 * 
 */
public interface OnRenameDownloadFileListener {

	/**
	 * 重命名下载文件成功
	 * 
	 * @param downloadFileRenamed
	 *            已经重命名过的下载文件信息
	 */
	void onRenameDownloadFileSuccess(DownloadFileInfo downloadFileRenamed);

	/**
	 * 重命名下载文件失败
	 * 
	 * @param downloadFileInfo
	 *            下载文件信息
	 * @param failReason
	 *            失败原因
	 */
	void onRenameDownloadFileFailed(DownloadFileInfo downloadFileInfo, FailReason failReason);
}