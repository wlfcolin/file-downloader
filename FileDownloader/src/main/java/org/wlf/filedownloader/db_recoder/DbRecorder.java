package org.wlf.filedownloader.db_recoder;

import java.util.List;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.FailException;
import org.wlf.filedownloader.base.Record;

/**
 * 数据库记录器（记录下载文件状态）
 * 
 * @author wlf
 * 
 */
public abstract class DbRecorder implements Record {

	// 让子类去现实
	@Override
	public abstract void recordStatus(String url, int status, int increaseSize) throws FailException;

	/**
	 * 保存一个新下载文件
	 * 
	 * @param downloadFileInfo
	 * @return true表示添加成功
	 */
	public abstract boolean addDownloadFile(DownloadFileInfo downloadFileInfo);

	/**
	 * 更新一个已经存在的下载文件
	 * 
	 * @param downloadFileInfo
	 * @return true表示更新成功
	 */
	public abstract boolean updateDownloadFile(DownloadFileInfo downloadFileInfo);

	/**
	 * 删除一个已经存在的下载文件
	 * 
	 * @param downloadFileInfo
	 * @return true表示删除成功
	 */
	public abstract boolean deleteDownloadFile(DownloadFileInfo downloadFileInfo);

	/**
	 * 根据url获取一个下载文件
	 * 
	 * @param url
	 * @return 已存在下载记录的下载文件信息
	 */
	public abstract DownloadFileInfo getDownloadFile(String url);

	/**
	 * 获取所有下载文件
	 * 
	 * @return 所有已存在下载记录的下载文件信息
	 */
	public abstract List<DownloadFileInfo> getDownloadFiles();
}
