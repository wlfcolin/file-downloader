package org.wlf.filedownloader.listener.simple;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloadConfiguration;
import org.wlf.filedownloader.FileDownloader;
import org.wlf.filedownloader.listener.OnRetryableFileDownloadStatusListener;

/**
 * OnSimpleFileDownloadStatus Listener
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public abstract class OnSimpleFileDownloadStatusListener implements OnRetryableFileDownloadStatusListener {

    @Override
    public void onFileDownloadStatusRetrying(DownloadFileInfo downloadFileInfo, int retryTimes) {
    }

    @Override
    public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {
    }

    @Override
    public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {
    }

    @Override
    public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {
    }

    @Override
    public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long 
            remainingTime) {
    }

    @Override
    public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {
    }

    @Override
    public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {
        // child should override recommend, if not, it will make a toast below
        Context appContext = getAppContext();
        if (appContext != null) {
            String fileName = downloadFileInfo != null ? downloadFileInfo.getFileName() : null;
            if (!TextUtils.isEmpty(fileName)) {
                Toast.makeText(appContext, "Download  " + fileName + "  completed !", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, 
                                           FileDownloadStatusFailReason failReason) {
        // child should override recommend, if not, it will make a toast below
        Context appContext = getAppContext();
        if (appContext != null) {
            String fileName = downloadFileInfo != null ? downloadFileInfo.getFileName() : null;
            if (!TextUtils.isEmpty(fileName)) {
                Toast.makeText(appContext, "Download  " + fileName + "  error !", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * get AppContext
     */
    private Context getAppContext() {

        Context appContext = null;

        if (FileDownloader.isInit()) {
            FileDownloadConfiguration configuration = FileDownloader.getFileDownloadConfiguration();
            appContext = configuration != null ? configuration.getContext() : null;
        }
        return appContext;
    }

}
