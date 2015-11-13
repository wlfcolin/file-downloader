package org.wlf.filedownloader.base;

import java.io.InputStream;

/**
 * 保存接口
 * 
 * @author wlf
 * 
 */
public interface Save {

	/**
	 * 保存数据
	 * 
	 * @param inputStream
	 *            需要保存的输入流
	 * @param startPosInTotal
	 *            该参数是输入流位于总长度的起始点。
	 *            <p>
	 *            |(totalStart)----|(startPosInTotal,inputStream
	 *            start)-----|(inputStream end)------|(totalEnd)
	 * @throws FailException
	 *             保存数据过程中遇到的任何异常
	 */
	void saveData(InputStream inputStream, int startPosInTotal) throws FailException;
}
