package org.wlf.filedownloader.base;

/**
 * download
 * <br/>
 * 下载接口
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public interface Download {

    /**
     * download
     *
     * @throws FailException any fail exception during download
     */
    void download() throws FailException;
}
