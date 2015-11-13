package org.wlf.filedownloader.lisener;

import org.wlf.filedownloader.DownloadFileInfo;

import java.util.List;

/**
 * 批量移动下载文件监听器
 * 
 * @author wlf
 * 
 */
public interface OnMoveDownloadFilesListener {

	/**
	 * 准备批量移动下载文件
	 * 
	 * @param downloadFilesNeedMove
	 *            需要批量移动的下载文件
	 */
	void onMoveDownloadFilesPrepared(List<DownloadFileInfo> downloadFilesNeedMove);

	/**
	 * 正在批量移动下载文件
	 * 
	 * @param downloadFilesNeedMove
	 *            需要批量移动的下载文件
	 * @param downloadFilesMoved
	 *            已跳过移动的下载文件
	 * @param downloadFilesSkip
	 *            已跳过移动的下载文件
	 * @param downloadFileMoving
	 */
	void onMovingDownloadFiles(List<DownloadFileInfo> downloadFilesNeedMove, List<DownloadFileInfo> downloadFilesMoved,
			List<DownloadFileInfo> downloadFilesSkip, DownloadFileInfo downloadFileMoving);

	/**
	 * 批量移动下载文件完成
	 * 
	 * @param downloadFilesNeedMove
	 *            需要批量移动的下载文件信息
	 * @param downloadFilesMoved
	 *            已成功移动的批量下载文件信息
	 */
	void onMoveDownloadFilesCompleted(List<DownloadFileInfo> downloadFilesNeedMove,
			List<DownloadFileInfo> downloadFilesMoved);

}
