package org.wlf.filedownloader.file_download.file_saver;

/**
 * the Strategy use for notify download progress
 * <br/>
 * 更新进度的策略
 *
 * @author wlf(Andy)
 * @datetime 2015-11-25 11:40 GMT+8
 * @email 411086563@qq.com
 */
public enum DownloadNoticeStrategy {

    NOTICE_AUTO(-1),// notice auto
    NOTICE_BY_SIZE(1024 * 1024),// notice by size, 1M(bytes for mValue)
    NOTICE_BY_TIME(1000 * 2);// notice by time interval, 2s(milliseconds for mValue)

    private long mValue;

    private DownloadNoticeStrategy(long value) {
        this.mValue = value;
    }

    public long getValue() {
        return mValue;
    }
}
