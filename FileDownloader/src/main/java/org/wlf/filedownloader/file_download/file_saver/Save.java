package org.wlf.filedownloader.file_download.file_saver;

import org.wlf.filedownloader.file_download.http_downloader.ContentLengthInputStream;

/**
 * save data
 * <br/>
 * 保存接口
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public interface Save {

    /**
     * save data
     *
     * @param inputStream     the inputStream data needed to save
     * @param startPosInTotal the start position of inputStream start to save in total data
     *                        <p/>
     *                        |(0,totalStart)----|(startPosInTotal,inputStream start)---
     *                        |(inputStream.length,inputStream end)----|(fileTotalSize,totalEnd)
     * @throws Exception any fail exception during saving data
     */
    void saveData(ContentLengthInputStream inputStream, long startPosInTotal) throws Exception;
}
