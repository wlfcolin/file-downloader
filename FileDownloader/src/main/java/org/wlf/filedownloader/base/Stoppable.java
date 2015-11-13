package org.wlf.filedownloader.base;

/**
 * 可停止接口
 * 
 * @author wlf
 * 
 */
public interface Stoppable {

	/** 停止 */
	void stop();

	/**
	 * 是否已经停止了
	 * 
	 * @return true表示已停止，false表示没有停止
	 */
	boolean isStopped();

}
