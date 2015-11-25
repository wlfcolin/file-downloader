package org.wlf.filedownloader;

import org.wlf.filedownloader.util.UrlUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * detect url file
 * <br/>
 * 探测文件缓存器
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DetectUrlFileCacher {

    private Map<String, DetectUrlFileInfo> mDetectUrlFileInfoMap = new HashMap<String, DetectUrlFileInfo>();// detect file memory cache

    private Object mModifyLock = new Object();// modify lock

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
                // update memory cache
                urlFileInfo.update(detectUrlFileInfo);
                return true;
            } else {
                // add in memory cache
                mDetectUrlFileInfoMap.put(url, detectUrlFileInfo);
                return true;
            }
        }
    }

    // package use only

    /**
     * get DetectUrlFile by url
     *
     * @param url file url
     * @return DetectUrlFile
     */
    DetectUrlFileInfo getDetectUrlFile(String url) {
        return mDetectUrlFileInfoMap.get(url);
    }

    // package use only

    /**
     * release cache
     */
    void release() {
        synchronized (mModifyLock) {// lock
            mDetectUrlFileInfoMap.clear();
        }
    }
}
