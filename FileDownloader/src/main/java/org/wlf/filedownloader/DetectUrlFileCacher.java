package org.wlf.filedownloader;

import java.util.HashMap;
import java.util.Map;

import org.wlf.filedownloader.util.UrlUtil;

/**
 * 探测文件缓存器
 * 
 * @author wlf
 * 
 */
public class DetectUrlFileCacher {

	private Map<String, DetectUrlFileInfo> mDetectUrlFileInfoMap = new HashMap<String, DetectUrlFileInfo>();// 探测的文件信息（内存缓存）

	private Object mModifyLock = new Object();// 修改锁

	/**
	 * 更新探测文件信息
	 * 
	 * @param detectUrlFileInfo
	 * @return true表示更新成功
	 */
	public boolean addOrUpdateDetectUrlFile(DetectUrlFileInfo detectUrlFileInfo) {

		if (detectUrlFileInfo == null) {
			return false;
		}

		String url = detectUrlFileInfo.getUrl();

		if (!UrlUtil.isUrl(url)) {
			return false;
		}

		DetectUrlFileInfo urlFileInfo = mDetectUrlFileInfoMap.get(url);
		synchronized (mModifyLock) {// 同步
			if (urlFileInfo != null) {
				// 更新
				urlFileInfo.update(detectUrlFileInfo);
				return true;
			} else {
				// 添加
				mDetectUrlFileInfoMap.put(url, detectUrlFileInfo);
				return true;
			}
		}
	}

	/**
	 * 根据url获取缓存的探测文件
	 * 
	 * @param url
	 * @return 获取探测文件
	 */
	DetectUrlFileInfo getDetectUrlFile(String url) {
		return mDetectUrlFileInfoMap.get(url);
	}

	/** 释放资源 */
	void release() {
		synchronized (mModifyLock) {// 同步
			mDetectUrlFileInfoMap.clear();
		}
	}
}
