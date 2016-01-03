package org.wlf.filedownloader.file_download;

/**
 * DownloadStatus Configuration
 * <br/>
 * 下载文件缓存器
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DownloadStatusConfiguration {

    String mUrl;
    boolean mAutoRelease;

    public DownloadStatusConfiguration(String url, boolean autoRelease) {
        mUrl = url;
        mAutoRelease = autoRelease;
    }
}
