package org.wlf.filedownloader.file_download.base;

import org.wlf.filedownloader.base.FailReason;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener.FileDownloadStatusFailReason;

/**
 * @author wlf(Andy)
 * @datetime 2016-01-08 19:05 GMT+8
 * @email 411086563@qq.com
 */
public interface OnStopFileDownloadTaskListener {

    /**
     * StopFileDownloadTaskSucceed
     *
     * @param url file url
     */
    void onStopFileDownloadTaskSucceed(String url);

    /**
     * StopFileDownloadTaskFailed
     *
     * @param url        file url
     * @param failReason fail reason
     */
    void onStopFileDownloadTaskFailed(String url, StopDownloadFileTaskFailReason failReason);

    /**
     * StopDownloadFileTaskFailReason
     */
    public static class StopDownloadFileTaskFailReason extends FileDownloadStatusFailReason {

        /**
         * the task has been stopped
         */
        public static final String TYPE_TASK_HAS_BEEN_STOPPED = StopDownloadFileTaskFailReason.class.getName() + 
                "_TYPE_TASK_HAS_BEEN_STOPPED";

        public StopDownloadFileTaskFailReason(String url, String detailMessage, String type) {
            super(url, detailMessage, type);
        }

        @Override
        protected void onInitTypeWithFailReason(FailReason failReason) {
            super.onInitTypeWithFailReason(failReason);

            if (failReason == null) {
                return;
            }

            // other FailReason exceptions that need cast to StopDownloadFileTaskFailReason

            // cast FileDownloadStatusFailReason
            if (failReason instanceof FileDownloadStatusFailReason) {
                FileDownloadStatusFailReason fileDownloadStatusFailReason = (FileDownloadStatusFailReason) failReason;
                setType(fileDownloadStatusFailReason.getType());// init type
            }
        }
    }
}
