package org.wlf.filedownloader.task;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.task.FileDownloadTask.FileDownloadTaskParam;

/**
 * {@link FileDownloadTaskParam} 帮助类
 * 
 * @author wlf
 * 
 */
public class FileDownloadTaskParamHelper {

	/** 使用{@link DownloadFileInfo}进行创建 */
	public static FileDownloadTaskParam createByDownloadFile(DownloadFileInfo downloadFileInfo) {

		if (downloadFileInfo == null) {
			return null;
		}

		return new FileDownloadTaskParam(downloadFileInfo.getUrl(), downloadFileInfo.getDownloadedSize(),
				downloadFileInfo.getFileSize(), downloadFileInfo.geteTag(), downloadFileInfo.getAcceptRangeType(),
				downloadFileInfo.getTempFilePath(), downloadFileInfo.getFilePath());
	}
}
