package org.wlf.filedownloader.file_download;

import android.text.TextUtils;

import org.wlf.filedownloader.util.DateUtil;
import org.wlf.filedownloader.util.DownloadFileUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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

    /**
     * remove DetectUrlFile
     *
     * @param url file url
     */
    public void removeDetectUrlFile(String url) {
        synchronized (mModifyLock) {// lock
            mDetectUrlFileInfoMap.remove(url);
        }
    }

    /**
     * get DetectUrlFile by url
     *
     * @param url file url
     * @return DetectUrlFile
     */
    public DetectUrlFileInfo getDetectUrlFile(String url) {
        DetectUrlFileInfo detectUrlFileInfo = mDetectUrlFileInfoMap.get(url);
        // check and remove
        if (DownloadFileUtil.isLegal(detectUrlFileInfo)) {
            String createDatetime = detectUrlFileInfo.getCreateDatetime();
            if (!TextUtils.isEmpty(detectUrlFileInfo.getCreateDatetime())) {
                // check whether is longer than 24 hours(one day)
                Date createDate = DateUtil.string2Date_yyyy_MM_dd_HH_mm_ss(createDatetime);
                if (createDate != null) {
                    GregorianCalendar createDateCalendar = new GregorianCalendar();
                    createDateCalendar.setTime(createDate);

                    GregorianCalendar curDateCalendar = new GregorianCalendar();
                    curDateCalendar.setTime(new Date());

                    createDateCalendar.add(Calendar.DAY_OF_YEAR, 1);// one day, 24 hours
                    if (curDateCalendar.after(createDateCalendar)) {
                        // remove the cache
                        removeDetectUrlFile(detectUrlFileInfo.getUrl());
                        detectUrlFileInfo = null;
                    }
                }
            }
        }
        return detectUrlFileInfo;
    }

    /**
     * release cache
     */
    public void release() {
        synchronized (mModifyLock) {// lock
            mDetectUrlFileInfoMap.clear();
        }
    }
}
