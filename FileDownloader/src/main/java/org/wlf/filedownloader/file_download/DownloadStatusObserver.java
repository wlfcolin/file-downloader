package org.wlf.filedownloader.file_download;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.DownloadStatusConfiguration;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.OnRetryableFileDownloadStatusListener;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.DownloadFileUtil;
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
     * @param downloadStatusConfiguration  Configuration for the OnFileDownloadStatusListener impl
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
                .getListenUrls())) ? downloadStatusConfiguration.getListenUrls().toString() : "all";

        Log.i(TAG, "file-downloader-listener 添加【文件下载状态监听器】成功，该listener监听的urls：" + urls);
    }

    /**
     * remove a OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener added OnFileDownloadStatusListener impl
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
                        .mDownloadStatusConfiguration.getListenUrls().toString() : "all";

                Log.i(TAG, "file-downloader-listener 移除【文件下载状态监听器】成功，该listener监听的urls：" + urls);
                break;
            }
        }
    }

    //    /**
    //     * remove all added OnFileDownloadStatusListeners of the url
    //     *
    //     * @param url         the url
    //     * @param forceRemove true means force remove, which will remove those the DownloadStatusConfiguration of 
    // matched
    //     *                    OnFileDownloadStatusListener has listen other urls, otherwise will remove those
    //     *                    OnFileDownloadStatusListener listen given url only
    //     */
    //    public void removeOnFileDownloadStatusListener(String url, boolean forceRemove) {
    //        if (!UrlUtil.isUrl(url)) {
    //            return;
    //        }
    //        Set<DownloadStatusListenerInfo> listenerNeedToRemove = new HashSet<DownloadStatusListenerInfo>();
    //        // find needed remove
    //        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {
    //            if (listenerInfo == null || listenerInfo.mDownloadStatusConfiguration == null) {
    //                // not match
    //                continue;
    //            }
    //
    //            Set<String> listenUrls = listenerInfo.mDownloadStatusConfiguration.getListenUrls();
    //            if (CollectionUtil.isEmpty(listenUrls)) {
    //                continue;
    //            }
    //
    //            for (String listenUrl : listenUrls) {
    //
    //                if (!UrlUtil.isUrl(listenUrl)) {
    //                    continue;
    //                }
    //
    //                if (!url.equals(listenUrl)) {
    //                    continue;
    //                }
    //
    //                // find one
    //                if (forceRemove) {
    //                    // need to move
    //                    listenerNeedToRemove.add(listenerInfo);
    //                    continue;
    //                }
    //
    //                boolean isFindNotMatch = false;
    //
    //                for (String lu : listenUrls) {
    //                    if (!UrlUtil.isUrl(lu)) {
    //                        continue;
    //                    }
    //                    if (!lu.equals(listenUrl)) {
    //                        // find one not match
    //                        isFindNotMatch = true;
    //                        break;
    //                    }
    //                }
    //
    //                if (!isFindNotMatch) {
    //                    // no match, need to move
    //                    listenerNeedToRemove.add(listenerInfo);
    //                }
    //            }
    //        }
    //
    //        // remove those needed to remove
    //        if (!CollectionUtil.isEmpty(listenerNeedToRemove)) {
    //            mDownloadStatusListenerInfos.removeAll(listenerNeedToRemove);
    //        }
    //    }

    // --------------------------------------notify caller--------------------------------------

    /**
     * notifyStatusWaiting
     */
    private void notifyStatusWaiting(DownloadFileInfo downloadFileInfo, OnFileDownloadStatusListener listener) {
        // main thread notify caller
        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusWaiting(downloadFileInfo, listener);

        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【文件下载状态为等待】，文件的url：" + url);
    }

    /**
     * notifyStatusRetrying
     */
    private void notifyStatusRetrying(DownloadFileInfo downloadFileInfo, int retryTimes, OnFileDownloadStatusListener
            listener) {
        if (listener instanceof OnRetryableFileDownloadStatusListener) {
            // main thread notify caller
            OnRetryableFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusRetrying(downloadFileInfo, 
                    retryTimes, (OnRetryableFileDownloadStatusListener) listener);

            String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

            Log.i(TAG, "file-downloader-listener 通知【文件下载状态为重试】，重试次数：" + retryTimes + "，文件的url：" + url);
        }
    }

    /**
     * notifyStatusPreparing
     */
    private void notifyStatusPreparing(DownloadFileInfo downloadFileInfo, OnFileDownloadStatusListener listener) {
        // main thread notify caller
        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPreparing(downloadFileInfo, listener);

        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【文件下载状态为准备中（正在连接）】，文件的url：" + url);
    }

    /**
     * notifyStatusPrepared
     */
    private void notifyStatusPrepared(DownloadFileInfo downloadFileInfo, OnFileDownloadStatusListener listener) {
        // main thread notify caller
        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPrepared(downloadFileInfo, listener);

        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【文件下载状态为已准备（已连接）】，文件的url：" + url);
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

        Log.i(TAG, "file-downloader-listener 通知【文件下载状态为正在下载】，文件的url：" + url);
    }

    /**
     * notifyStatusPaused
     */
    private void notifyStatusPaused(DownloadFileInfo downloadFileInfo, OnFileDownloadStatusListener listener) {
        // main thread notify caller
        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPaused(downloadFileInfo, listener);

        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【文件下载状态为暂停】，文件的url：" + url);
    }

    /**
     * notifyStatusCompleted
     */
    private void notifyStatusCompleted(DownloadFileInfo downloadFileInfo, OnFileDownloadStatusListener listener) {
        // main thread notify caller
        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusCompleted(downloadFileInfo, listener);

        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【文件下载状态为完成】，文件的url：" + url);
    }

    /**
     * notifyStatusFailed
     */
    private void notifyStatusFailed(String url, DownloadFileInfo downloadFileInfo, FileDownloadStatusFailReason 
            failReason, OnFileDownloadStatusListener listener) {
        // main thread notify caller
        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(url, downloadFileInfo, failReason, 
                listener);

        String downloadFileUrl = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";
        String failMsg = failReason != null ? failReason.getMessage() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【文件下载状态为失败】，文件的url：" + url + "，downloadFileUrl：" + downloadFileUrl +
                "，失败原因：" + failMsg);
    }

    @Override
    public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {

        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            // notify match url listeners
            if (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadStatusConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadStatusConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl) || url.trim().equals(listenUrl.trim())) {
                        // find match url, notify caller
                        notifyStatusWaiting(downloadFileInfo, listenerInfo.mListener);
                    }
                }
            }
            // Configuration or ListenUrls is null or empty, notify all
            else {
                // global register listener, notify all callers
                notifyStatusWaiting(downloadFileInfo, listenerInfo.mListener);
            }
        }
    }

    @Override
    public void onFileDownloadStatusRetrying(DownloadFileInfo downloadFileInfo, int retryTimes) {

        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this || !
                    (listenerInfo.mListener instanceof OnRetryableFileDownloadStatusListener)) {
                continue;
            }

            // notify match url listeners
            if (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadStatusConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadStatusConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl) || url.trim().equals(listenUrl.trim())) {
                        // find match url, notify caller
                        notifyStatusRetrying(downloadFileInfo, retryTimes, listenerInfo.mListener);
                    }
                }
            }
            // Configuration or ListenUrls is null or empty, notify all
            else {
                // global register listener, notify all callers
                notifyStatusRetrying(downloadFileInfo, retryTimes, listenerInfo.mListener);
            }
        }
    }


    @Override
    public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {

        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            // notify match url listeners
            if (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadStatusConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadStatusConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl) || url.trim().equals(listenUrl.trim())) {
                        // find match url, notify caller
                        notifyStatusPreparing(downloadFileInfo, listenerInfo.mListener);
                    }
                }
            }
            // Configuration or ListenUrls is null or empty, notify all
            else {
                // global register listener, notify all callers
                notifyStatusPreparing(downloadFileInfo, listenerInfo.mListener);
            }
        }
    }

    @Override
    public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {

        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            // notify match url listeners
            if (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadStatusConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadStatusConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl) || url.trim().equals(listenUrl.trim())) {
                        // find match url, notify caller
                        notifyStatusPrepared(downloadFileInfo, listenerInfo.mListener);
                    }
                }
            }
            // Configuration or ListenUrls is null or empty, notify all
            else {
                // global register listener, notify all callers
                notifyStatusPrepared(downloadFileInfo, listenerInfo.mListener);
            }
        }
    }

    @Override
    public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long 
            remainingTime) {

        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            // notify match url listeners
            if (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadStatusConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadStatusConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }

                    if (url.equals(listenUrl) || url.trim().equals(listenUrl.trim())) {
                        // find match url, notify caller
                        notifyStatusDownloading(downloadFileInfo, downloadSpeed, remainingTime, listenerInfo.mListener);
                    }
                }
            }
            // Configuration or ListenUrls is null or empty, notify all
            else {
                // global register listener, notify all callers
                notifyStatusDownloading(downloadFileInfo, downloadSpeed, remainingTime, listenerInfo.mListener);
            }
        }
    }

    @Override
    public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {

        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            // notify match url listeners
            if (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadStatusConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadStatusConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl) || url.trim().equals(listenUrl.trim())) {
                        // find match url, notify caller
                        notifyStatusPaused(downloadFileInfo, listenerInfo.mListener);
                        // remove the listener
                        if (listenerInfo.mDownloadStatusConfiguration.isAutoRelease()) {
                            mDownloadStatusListenerInfos.remove(listenerInfo);
                        }
                    }
                }
            }
            // Configuration or ListenUrls is null or empty, notify all
            else {
                // global register listener, notify all callers
                notifyStatusPaused(downloadFileInfo, listenerInfo.mListener);
            }
        }
    }

    @Override
    public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {

        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            // notify match url listeners
            if (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadStatusConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadStatusConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl) || url.trim().equals(listenUrl.trim())) {
                        // find match url, notify caller
                        notifyStatusCompleted(downloadFileInfo, listenerInfo.mListener);
                        // remove the listener
                        if (listenerInfo.mDownloadStatusConfiguration.isAutoRelease()) {
                            mDownloadStatusListenerInfos.remove(listenerInfo);
                        }
                    }
                }
            }
            // Configuration or ListenUrls is null or empty, notify all
            else {
                // global register listener, notify all callers
                notifyStatusCompleted(downloadFileInfo, listenerInfo.mListener);
            }
        }
    }

    @Override
    public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, 
                                           FileDownloadStatusFailReason failReason) {

        if (!UrlUtil.isUrl(url)) {
            return;
        }

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            // notify match url listeners
            if (listenerInfo.mDownloadStatusConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadStatusConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadStatusConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl) || url.trim().equals(listenUrl.trim())) {
                        // find match url, notify caller
                        notifyStatusFailed(url, downloadFileInfo, failReason, listenerInfo.mListener);
                        // remove the listener
                        if (listenerInfo.mDownloadStatusConfiguration.isAutoRelease()) {
                            mDownloadStatusListenerInfos.remove(listenerInfo);
                        }
                    }
                }
            }
            // Configuration or ListenUrls is null or empty, notify all
            else {
                // global register listener, notify all callers
                notifyStatusFailed(url, downloadFileInfo, failReason, listenerInfo.mListener);
            }
        }
    }

    /**
     * release
     */
    public void release() {
        mDownloadStatusListenerInfos.clear();
    }

    /**
     * DownloadStatusListenerInfo
     */
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