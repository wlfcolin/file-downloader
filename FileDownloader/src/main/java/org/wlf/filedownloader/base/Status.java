package org.wlf.filedownloader.base;

/**
 * 状态
 * 
 * @author wlf
 * 
 */
public class Status {

	/** 未知状态 */
	public static final int DOWNLOAD_STATUS_UNKNOWN = 0;
	/** 等待下载（等待其它任务完成） */
	public static final int DOWNLOAD_STATUS_WAITING = 1;
	/** 准备下载（正在获取资源） */
	public static final int DOWNLOAD_STATUS_PREPARING = 2;
	/** 开始下载（已连接资源） */
	public static final int DOWNLOAD_STATUS_PREPARED = 3;
	/** 正在下载（下载中） */
	public static final int DOWNLOAD_STATUS_DOWNLOADING = 4;
	/** 下载完成（文件已完整保存好） */
	public static final int DOWNLOAD_STATUS_COMPLETED = 5;
	/** 暂停下载（已暂停下载） */
	public static final int DOWNLOAD_STATUS_PAUSED = 6;
	/** 下载出错（无法下载） */
	public static final int DOWNLOAD_STATUS_ERROR = 7;
	/** 已下载过，但文件不存在 */
	public static final int DOWNLOAD_STATUS_FILE_NOT_EXIST = 8;
}
