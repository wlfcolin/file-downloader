package org.wlf.filedownloader.file_download.http_downloader;

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
     * @throws Exception any fail exception during download
     */
    void download() throws Exception;
}
