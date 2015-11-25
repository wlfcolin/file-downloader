package org.wlf.filedownloader.helper;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloadTask.FileDownloadTaskParam;

/**
 * {@link FileDownloadTaskParam} helper
 * <br/>
 * {@link FileDownloadTaskParam}帮助类
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class FileDownloadTaskParamHelper {

    /**
     * use {@link DownloadFileInfo} to create {@link FileDownloadTaskParam}
     */
    public static FileDownloadTaskParam createByDownloadFile(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return null;
        }

        return new FileDownloadTaskParam(downloadFileInfo.getUrl(), downloadFileInfo.getDownloadedSize(), downloadFileInfo.getFileSize(), downloadFileInfo.getETag(), downloadFileInfo.getAcceptRangeType(), downloadFileInfo.getTempFilePath(), downloadFileInfo.getFilePath());
    }
}
