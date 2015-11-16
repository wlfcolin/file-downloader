package org.wlf.filedownloader;

import org.wlf.filedownloader.util.UrlUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Detect net File
 * <br/>
 * 探测文件缓存器
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DetectUrlFileCacher {

    private Map<String, DetectUrlFileInfo> mDetectUrlFileInfoMap = new HashMap<String, DetectUrlFileInfo>();// 探测的文件信息（内存缓存）

    private Object mModifyLock = new Object();// lock

    /**
     * update DetectUrlFile
     *
     * @param detectUrlFileInfo DetectUrlFile
     * @return true means update succeed
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
        synchronized (mModifyLock) {// lock
            if (urlFileInfo != null) {
                // update
                urlFileInfo.update(detectUrlFileInfo);
                return true;
            } else {
                // add
                mDetectUrlFileInfoMap.put(url, detectUrlFileInfo);
                return true;
            }
        }
    }

    /**
     * get DetectUrlFile by url
     *
     * @param url file url
     * @return DetectUrlFile
     */
    DetectUrlFileInfo getDetectUrlFile(String url) {
        return mDetectUrlFileInfoMap.get(url);
    }

    /**
     * release cache
     */
    void release() {
        synchronized (mModifyLock) {// 同步
            mDetectUrlFileInfoMap.clear();
        }
    }
}
