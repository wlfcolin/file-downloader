package org.wlf.filedownloader.listener;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.FailReason;

/**
 * OnRenameDownloadFileListener
 * <br/>
 * 重命名下载文件监听接口
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public interface OnRenameDownloadFileListener {

    /**
     * rename succeed
     *
     * @param downloadFileRenamed download files renamed
     */
    void onRenameDownloadFileSuccess(DownloadFileInfo downloadFileRenamed);

    /**
     * rename failed
     *
     * @param downloadFileInfo download files needed to rename
     * @param failReason       fail reason
     */
    void onRenameDownloadFileFailed(DownloadFileInfo downloadFileInfo, OnRenameDownloadFileFailReason failReason);

    /**
     * OnRenameDownloadFileFailReason
     */
    public static class OnRenameDownloadFileFailReason extends FailReason {

        private static final long serialVersionUID = 4959079784745889291L;

        public OnRenameDownloadFileFailReason(String detailMessage, String type) {
            super(detailMessage, type);
        }

        public OnRenameDownloadFileFailReason(Throwable throwable) {
            super(throwable);
        }

        @Override
        protected void onInitTypeWithThrowable(Throwable throwable) {
            super.onInitTypeWithThrowable(throwable);
            // TODO
        }
    }
}