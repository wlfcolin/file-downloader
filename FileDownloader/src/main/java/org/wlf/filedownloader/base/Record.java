package org.wlf.filedownloader.base;

/**
 * 记录接口
 * 
 * @author wlf
 * 
 */
public interface Record {

	/**
	 * 记录状态
	 * 
	 * @param url
	 *            下载地址
	 * @param status
	 *            记录成哪个状态，参考{@link Status}
	 * @param increaseSize
	 *            新增大小
	 * @throws FailException
	 *             记录状态过程中遇到的任何异常
	 */
	void recordStatus(String url, int status, int increaseSize) throws FailException;
}
