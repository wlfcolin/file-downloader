package org.wlf.filedownloader.file_download;

import org.wlf.filedownloader.util.UrlUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * DetectUrlFile Cacher
 * <br/>
 * 探测文件缓存器
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
class DetectUrlFileCacher {

    // detect file memory cache
    private Map<String, DetectUrlFileInfo> mDetectUrlFileInfoMap = new HashMap<String, DetectUrlFileInfo>();

    private Object mModifyLock = new Object();// modify lock

    /**
     * update DetectUrlFile
     *
     * @param detectUrlFileInfo DetectUrlFile
     * @return true means update succeed
     */
    boolean addOrUpdateDetectUrlFile(DetectUrlFileInfo detectUrlFileInfo) {

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
