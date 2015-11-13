package org.wlf.filedownloader.base;

/**
 * 下载接口
 * 
 * @author wlf
 * 
 */
public interface Download {

	/**
	 * 下载
	 * 
	 * @throws FailException
	 *             下载过程中遇到的任何异常
	 */
	void download() throws FailException;
}
