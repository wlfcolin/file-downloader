package org.wlf.filedownloader.base;

/**
 * record status
 * <br/>
 * 记录状态接口
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public interface Record {

    /**
     * record status
     *
     * @param url          download url
     * @param status       record status，ref{@link Status}
     * @param increaseSize increased size since last record
     * @throws FailException any fail exception during recording status
     */
    void recordStatus(String url, int status, int increaseSize) throws FailException;
}
