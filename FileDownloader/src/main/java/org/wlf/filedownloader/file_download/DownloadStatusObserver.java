package org.wlf.filedownloader.file_download;

import android.util.Log;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.OnRetryableFileDownloadStatusListener;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * FileDownloadStatus Observer
 *
 * @author wlf(Andy)
 * @datetime 2015-12-09 17:55 GMT+8
 * @email 411086563@qq.com
 */
class DownloadStatusObserver implements OnRetryableFileDownloadStatusListener {

    private static final String TAG = DownloadStatusObserver.class.getSimpleName();

    // listeners
    private Set<DownloadStatusListenerInfo> mDownloadStatusListenerInfos = new 
            CopyOnWriteArraySet<DownloadStatusListenerInfo>();

    /**
     * add a OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener
     * @param downloadStatusConfiguration
     */
    public void addOnFileDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener, 
                                                DownloadStatusConfiguration downloadStatusConfiguration) {
        if (onFileDownloadStatusListener == null) {
            return;
        }
        // find whether is added 
        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {
            if (listenerInfo == null) {
                return;
            }

            if (listenerInfo.mListener == onFileDownloadStatusListener) {
                return;// has been added
            }
        }

        // need add
        DownloadStatusListenerInfo listenerInfo = new DownloadStatusListenerInfo(downloadStatusConfiguration, 
                onFileDownloadStatusListener);
        mDownloadStatusListenerInfos.add(listenerInfo);

        String urls = (downloadStatusConfiguration != null && !CollectionUtil.isEmpty(downloadStatusConfiguration
                .getListenUrls())) ? downloadStatusConfiguration.getListenUrls().toString() : "";

        Log.i(TAG, "file-downloader-listener 添加【下载状态监听器】成功，监听的urls：" + urls);
    }

    /**
     * remove a OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener
     */
    public void removeOnFileDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        if (onFileDownloadStatusListener == null) {
            return;
        }
        // find and remove
        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {
            if (listenerInfo == null) {
                // not need to remove, may has been removed
                continue;
            }
            if (listenerInfo.mListener == onFileDownloadStatusListener) {
                // find, remove
                mDownloadStatusListenerInfos.remove(listenerInfo);

                String urls = (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty
                        (listenerInfo.mDownloadStatusConfiguration.getListenUrls())) ? listenerInfo
                        .mDownloadStatusConfiguration.getListenUrls().toString() : "";

                Log.i(TAG, "file-downloader-listener 移除【下载状态监听器】成功，包含监听的urls：" + urls);
                break;
            }
        }
    }

    //    /**
    //     * remove all OnFileDownloadStatusListener of the url
    //     *
    //     * @param url
    //     */
    //    public void removeOnFileDownloadStatusListener(String url) {
    //        if (!UrlUtil.isUrl(url)) {
    //            return;
    //        }
    //        Set<DownloadStatusListenerInfo> listenerNeedToRemove = new HashSet<DownloadStatusListenerInfo>();
    //        // find needed remove
    //        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {
    //            if (listenerInfo == null || listenerInfo.mDownloadStatusConfiguration == null) {
    //                // not need to remove, may has been removed
    //                continue;
    //            }
    //
    //            Set<String> listenUrls = listenerInfo.mDownloadStatusConfiguration.getListenUrls();
    //            if (CollectionUtil.isEmpty(listenUrls)) {
    //                continue;
    //            }
    //            for (String listenUrl : listenUrls) {
    //                if (!UrlUtil.isUrl(listenUrl)) {
    //                    continue;
    //                }
    //                if (url.equals(listenUrl)) {
    //                    // find one
    //                    listenerNeedToRemove.add(listenerInfo);
    //                }
    //           }
    //        }
    //
    //        // remove
    //        if (!CollectionUtil.isEmpty(listenerNeedToRemove)) {
    //            mDownloadStatusListenerInfos.removeAll(listenerNeedToRemove);
    //        }
    //    }

    /**
     * notifyStatusWaiting
     */
    private void notifyStatusWaiting(DownloadFileInfo downloadFileInfo, OnFileDownloadStatusListener listener) {
        // notify caller
        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusWaiting(downloadFileInfo, listener);

        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【下载状态为等待】，文件的url：" + url);
    }

    /**
     * notifyStatusRetrying
     */
    private void notifyStatusRetrying(DownloadFileInfo downloadFileInfo, int retryTimes, OnFileDownloadStatusListener
            listener) {
        if (listener instanceof OnRetryableFileDownloadStatusListener) {
            // notify caller
            OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusRetrying(downloadFileInfo, retryTimes, 
                    (OnRetryableFileDownloadStatusListener) listener);

            String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

            Log.i(TAG, "file-downloader-listener 通知【下载状态为重试】，重试次数：" + retryTimes + "，文件的url：" + url);
        }
    }

    /**
     * notifyStatusPreparing
     */
    private void notifyStatusPreparing(DownloadFileInfo downloadFileInfo, OnFileDownloadStatusListener listener) {
        // notify caller
        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPreparing(downloadFileInfo, listener);

        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【下载状态为准备中（正在连接）】，文件的url：" + url);
    }

    /**
     * notifyStatusPrepared
     */
    private void notifyStatusPrepared(DownloadFileInfo downloadFileInfo, OnFileDownloadStatusListener listener) {
        // notify caller
        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPrepared(downloadFileInfo, listener);

        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【下载状态为已准备（已连接）】，文件的url：" + url);
    }

    /**
     * notifyStatusDownloading
     */
    private void notifyStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long remainingTime, 
                                         OnFileDownloadStatusListener listener) {
        // notify caller
        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusDownloading(downloadFileInfo, 
                downloadSpeed, remainingTime, listener);

        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【下载状态为正在下载】，文件的url：" + url);
    }

    /**
     * notifyStatusPaused
     */
    private void notifyStatusPaused(DownloadFileInfo downloadFileInfo, OnFileDownloadStatusListener listener) {
        // notify caller
        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPaused(downloadFileInfo, listener);

        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【下载状态为暂停】，文件的url：" + url);
    }

    /**
     * notifyStatusCompleted
     */
    private void notifyStatusCompleted(DownloadFileInfo downloadFileInfo, OnFileDownloadStatusListener listener) {
        // notify caller
        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusCompleted(downloadFileInfo, listener);

        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【下载状态为完成】，文件的url：" + url);
    }

    /**
     * notifyStatusFailed
     */
    private void notifyStatusFailed(String url, DownloadFileInfo downloadFileInfo, FileDownloadStatusFailReason 
            failReason, OnFileDownloadStatusListener listener) {
        // notify caller
        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(url, downloadFileInfo, failReason, 
                listener);

        String downloadFileUrl = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【下载状态为失败】，文件的url：" + url + "，downloadFileUrl：" + downloadFileUrl);
    }

    @Override
    public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {
        // notify all match registered listeners
        if (downloadFileInfo == null || !UrlUtil.isUrl(downloadFileInfo.getUrl())) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            if (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadStatusConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadStatusConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl)) {
                        // find match url,notify caller
                        notifyStatusWaiting(downloadFileInfo, listenerInfo.mListener);
                    }
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadStatusConfiguration == null || CollectionUtil.isEmpty(listenerInfo
                        .mDownloadStatusConfiguration.getListenUrls())) {
                    // notify caller
                    notifyStatusWaiting(downloadFileInfo, listenerInfo.mListener);
                } else {
                    // do not notify
                }
            }
        }
    }

    @Override
    public void onFileDownloadStatusRetrying(DownloadFileInfo downloadFileInfo, int retryTimes) {
        // notify all match registered listeners
        if (downloadFileInfo == null || !UrlUtil.isUrl(downloadFileInfo.getUrl())) {
            return;
        }
        String url = downloadFileInfo.getUrl();

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this || !
                    (listenerInfo.mListener instanceof OnRetryableFileDownloadStatusListener)) {
                continue;
            }

            if (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadStatusConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadStatusConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl)) {
                        // find match url,notify caller
                        notifyStatusRetrying(downloadFileInfo, retryTimes, listenerInfo.mListener);
                    }
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadStatusConfiguration == null || CollectionUtil.isEmpty(listenerInfo
                        .mDownloadStatusConfiguration.getListenUrls())) {
                    // notify caller
                    notifyStatusRetrying(downloadFileInfo, retryTimes, listenerInfo.mListener);
                } else {
                    // do not notify
                }
            }
        }
    }


    @Override
    public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {
        // notify all match registered listeners
        if (downloadFileInfo == null || !UrlUtil.isUrl(downloadFileInfo.getUrl())) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            if (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadStatusConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadStatusConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl)) {
                        // find match url,notify caller
                        notifyStatusPreparing(downloadFileInfo, listenerInfo.mListener);
                    }
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadStatusConfiguration == null || CollectionUtil.isEmpty(listenerInfo
                        .mDownloadStatusConfiguration.getListenUrls())) {
                    // notify caller
                    notifyStatusPreparing(downloadFileInfo, listenerInfo.mListener);
                } else {
                    // do not notify
                }
            }
        }
    }

    @Override
    public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {
        // notify all match registered listeners
        if (downloadFileInfo == null || !UrlUtil.isUrl(downloadFileInfo.getUrl())) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            if (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadStatusConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadStatusConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl)) {
                        // find match url,notify caller
                        notifyStatusPrepared(downloadFileInfo, listenerInfo.mListener);
                    }
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadStatusConfiguration == null || CollectionUtil.isEmpty(listenerInfo
                        .mDownloadStatusConfiguration.getListenUrls())) {
                    // notify caller
                    notifyStatusPrepared(downloadFileInfo, listenerInfo.mListener);
                } else {
                    // do not notify
                }
            }
        }
    }

    @Override
    public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long 
            remainingTime) {
        // notify all match registered listeners
        if (downloadFileInfo == null || !UrlUtil.isUrl(downloadFileInfo.getUrl())) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            if (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadStatusConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadStatusConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl)) {
                        // find match url,notify caller
                        notifyStatusDownloading(downloadFileInfo, downloadSpeed, remainingTime, listenerInfo.mListener);
                    }
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadStatusConfiguration == null || CollectionUtil.isEmpty(listenerInfo
                        .mDownloadStatusConfiguration.getListenUrls())) {
                    // notify caller
                    notifyStatusDownloading(downloadFileInfo, downloadSpeed, remainingTime, listenerInfo.mListener);
                } else {
                    // do not notify
                }
            }
        }
    }

    @Override
    public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {
        // notify all match registered listeners
        if (downloadFileInfo == null || !UrlUtil.isUrl(downloadFileInfo.getUrl())) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            if (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadStatusConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadStatusConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl)) {
                        // find match url,notify caller
                        notifyStatusPaused(downloadFileInfo, listenerInfo.mListener);
                        // remove the listener
                        if (listenerInfo.mDownloadStatusConfiguration.isAutoRelease()) {
                            mDownloadStatusListenerInfos.remove(listenerInfo);
                        }
                    }
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadStatusConfiguration == null || CollectionUtil.isEmpty(listenerInfo
                        .mDownloadStatusConfiguration.getListenUrls())) {
                    // notify caller
                    notifyStatusPaused(downloadFileInfo, listenerInfo.mListener);
                } else {
                    // do not notify
                }
            }
        }
    }

    @Override
    public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {
        // notify all match registered listeners
        if (downloadFileInfo == null || !UrlUtil.isUrl(downloadFileInfo.getUrl())) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            if (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadStatusConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadStatusConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl)) {
                        // find match url,notify caller
                        notifyStatusCompleted(downloadFileInfo, listenerInfo.mListener);
                        // remove the listener
                        if (listenerInfo.mDownloadStatusConfiguration.isAutoRelease()) {
                            mDownloadStatusListenerInfos.remove(listenerInfo);
                        }
                    }
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadStatusConfiguration == null || CollectionUtil.isEmpty(listenerInfo
                        .mDownloadStatusConfiguration.getListenUrls())) {
                    // notify caller
                    notifyStatusCompleted(downloadFileInfo, listenerInfo.mListener);
                } else {
                    // do not notify
                }
            }
        }
    }

    @Override
    public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, 
                                           FileDownloadStatusFailReason failReason) {
        // notify all match registered listeners
        if (!UrlUtil.isUrl(url)) {
            return;
        }

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            if (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadStatusConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadStatusConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl)) {
                        // find match url,notify caller
                        notifyStatusFailed(url, downloadFileInfo, failReason, listenerInfo.mListener);
                        // remove the listener
                        if (listenerInfo.mDownloadStatusConfiguration.isAutoRelease()) {
                            mDownloadStatusListenerInfos.remove(listenerInfo);
                        }
                    }
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadStatusConfiguration == null || CollectionUtil.isEmpty(listenerInfo
                        .mDownloadStatusConfiguration.getListenUrls())) {
                    // notify caller
                    notifyStatusFailed(url, downloadFileInfo, failReason, listenerInfo.mListener);
                } else {
                    // do not notify
                }
            }
        }
    }

    public void release() {
        mDownloadStatusListenerInfos.clear();
    }

    private static class DownloadStatusListenerInfo {

        private DownloadStatusConfiguration mDownloadStatusConfiguration;
        private OnFileDownloadStatusListener mListener;

        public DownloadStatusListenerInfo(DownloadStatusConfiguration downloadStatusConfiguration, 
                                          OnFileDownloadStatusListener listener) {
            mDownloadStatusConfiguration = downloadStatusConfiguration;
            mListener = listener;
        }
    }

}