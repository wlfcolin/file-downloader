package org.wlf.filedownloader;

import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.listener.OnDownloadFileChangeListener;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.DownloadFileUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * DownloadFileChange Observer
 *
 * @author wlf(Andy)
 * @datetime 2015-12-09 17:26 GMT+8
 * @email 411086563@qq.com
 */
class DownloadFileChangeObserver implements OnDownloadFileChangeListener {

    private static final String TAG = DownloadFileChangeObserver.class.getSimpleName();

    private Set<DownloadFileChangeListenerInfo> mOnDownloadFileChangeListeners = new 
            CopyOnWriteArraySet<DownloadFileChangeListenerInfo>();

    /**
     * add a OnDownloadFileChangeListener
     *
     * @param onDownloadFileChangeListener    OnDownloadFileChangeListener impl
     * @param downloadFileChangeConfiguration Configuration for the OnDownloadFileChangeListener impl
     */
    public void addOnDownloadFileChangeListener(OnDownloadFileChangeListener onDownloadFileChangeListener, 
                                                DownloadFileChangeConfiguration downloadFileChangeConfiguration) {
        if (onDownloadFileChangeListener == null) {
            return;
        }
        // find whether is added 
        for (DownloadFileChangeListenerInfo listenerInfo : mOnDownloadFileChangeListeners) {
            if (listenerInfo == null) {
                return;
            }

            if (listenerInfo.mListener == onDownloadFileChangeListener) {
                return;// has been added
            }
        }

        // need add
        DownloadFileChangeListenerInfo listenerInfo = new DownloadFileChangeListenerInfo
                (downloadFileChangeConfiguration, onDownloadFileChangeListener);
        mOnDownloadFileChangeListeners.add(listenerInfo);

        String urls = (downloadFileChangeConfiguration != null && !CollectionUtil.isEmpty
                (downloadFileChangeConfiguration.getListenUrls())) ? downloadFileChangeConfiguration.getListenUrls()
                .toString() : "all";

        Log.i(TAG, "file-downloader-listener 添加【下载文件改变监听器】成功，该listener监听的urls：" + urls);
    }

    /**
     * remove a OnDownloadFileChangeListener
     *
     * @param onDownloadFileChangeListener added OnDownloadFileChangeListener impl
     */
    public void removeOnDownloadFileChangeListener(OnDownloadFileChangeListener onDownloadFileChangeListener) {
        if (onDownloadFileChangeListener == null) {
            return;
        }
        // find and remove
        for (DownloadFileChangeListenerInfo listenerInfo : mOnDownloadFileChangeListeners) {
            if (listenerInfo == null) {
                continue;
            }
            if (listenerInfo.mListener == onDownloadFileChangeListener) {
                // find, remove
                mOnDownloadFileChangeListeners.remove(listenerInfo);

                String urls = (listenerInfo.mDownloadFileChangeConfiguration != null && !CollectionUtil.isEmpty
                        (listenerInfo.mDownloadFileChangeConfiguration.getListenUrls())) ? listenerInfo
                        .mDownloadFileChangeConfiguration.getListenUrls().toString() : "all";

                Log.i(TAG, "file-downloader-listener 移除【下载文件改变监听器】成功，该listener监听的urls：" + urls);

                break;
            }
        }
    }

    // --------------------------------------notify caller--------------------------------------

    /**
     * notifyDownloadFileCreated
     */
    private void notifyDownloadFileCreated(DownloadFileInfo downloadFileInfo, OnDownloadFileChangeListener listener, 
                                           boolean sync) {
        // notify caller
        if (sync) {
            // sync notify caller
            try {
                if (listener != null) {
                    listener.onDownloadFileCreated(downloadFileInfo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // main thread notify caller
            OnDownloadFileChangeListener.MainThreadHelper.onDownloadFileCreated(downloadFileInfo, listener);
        }

        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【下载文件被创建】，被创建文件的url：" + url);
    }

    /**
     * notifyDownloadFileUpdated
     */
    private void notifyDownloadFileUpdated(DownloadFileInfo downloadFileInfo, Type type, OnDownloadFileChangeListener
            listener, boolean sync) {
        // notify caller
        if (sync) {
            // sync notify caller
            try {
                if (listener != null) {
                    listener.onDownloadFileUpdated(downloadFileInfo, type);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // main thread notify caller
            OnDownloadFileChangeListener.MainThreadHelper.onDownloadFileUpdated(downloadFileInfo, type, listener);
        }

        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";
        String typeName = type != null ? type.name() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【下载文件被更新】，更新类型：" + typeName + "，被更新文件的url：" + url);
    }

    /**
     * notifyDownloadFileDeleted
     */
    private void notifyDownloadFileDeleted(DownloadFileInfo downloadFileInfo, OnDownloadFileChangeListener listener, 
                                           boolean sync) {
        // notify caller
        if (sync) {
            // sync notify caller
            try {
                if (listener != null) {
                    listener.onDownloadFileDeleted(downloadFileInfo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // main thread caller
            OnDownloadFileChangeListener.MainThreadHelper.onDownloadFileDeleted(downloadFileInfo, listener);
        }
        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【下载文件被删除】，被删除文件的url：" + url);
    }

    // --------------interface methods impl--------------

    @Override
    public void onDownloadFileCreated(DownloadFileInfo downloadFileInfo) {

        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadFileChangeListenerInfo listenerInfo : mOnDownloadFileChangeListeners) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            // notify match url listeners
            if (listenerInfo.mDownloadFileChangeConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadFileChangeConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadFileChangeConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl) || url.trim().equals(listenUrl.trim())) {
                        // find match url, notify match caller
                        notifyDownloadFileCreated(downloadFileInfo, listenerInfo.mListener, listenerInfo
                                .mDownloadFileChangeConfiguration.isTreadCallback());
                    }
                }
            }
            // Configuration or ListenUrls is null or empty, notify all
            else {
                // global register listener, notify all callers
                boolean isSyncCallback = false;
                if (listenerInfo.mDownloadFileChangeConfiguration != null) {
                    isSyncCallback = listenerInfo.mDownloadFileChangeConfiguration.isTreadCallback();
                }
                notifyDownloadFileCreated(downloadFileInfo, listenerInfo.mListener, isSyncCallback);
            }
        }
    }

    @Override
    public void onDownloadFileUpdated(DownloadFileInfo downloadFileInfo, Type type) {

        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadFileChangeListenerInfo listenerInfo : mOnDownloadFileChangeListeners) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            // notify match url listeners
            if (listenerInfo.mDownloadFileChangeConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadFileChangeConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadFileChangeConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl) || url.trim().equals(listenUrl.trim())) {
                        // find match url, notify caller
                        notifyDownloadFileUpdated(downloadFileInfo, type, listenerInfo.mListener, listenerInfo
                                .mDownloadFileChangeConfiguration.isTreadCallback());
                    }
                }
            }
            // Configuration or ListenUrls is null or empty, notify all
            else {
                // global register listener, notify all callers
                boolean isSyncCallback = false;
                if (listenerInfo.mDownloadFileChangeConfiguration != null) {
                    isSyncCallback = listenerInfo.mDownloadFileChangeConfiguration.isTreadCallback();
                }
                notifyDownloadFileUpdated(downloadFileInfo, type, listenerInfo.mListener, isSyncCallback);
            }
        }
    }

    @Override
    public void onDownloadFileDeleted(DownloadFileInfo downloadFileInfo) {

        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadFileChangeListenerInfo listenerInfo : mOnDownloadFileChangeListeners) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            // notify match url listeners
            if (listenerInfo.mDownloadFileChangeConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadFileChangeConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadFileChangeConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl) || url.trim().equals(listenUrl.trim())) {
                        // find match url, notify caller
                        notifyDownloadFileDeleted(downloadFileInfo, listenerInfo.mListener, listenerInfo
                                .mDownloadFileChangeConfiguration.isTreadCallback());
                    }
                }
            }
            // Configuration or ListenUrls is null or empty, notify all
            else {
                // global register listener, notify all callers
                boolean isSyncCallback = false;
                if (listenerInfo.mDownloadFileChangeConfiguration != null) {
                    isSyncCallback = listenerInfo.mDownloadFileChangeConfiguration.isTreadCallback();
                }
                notifyDownloadFileDeleted(downloadFileInfo, listenerInfo.mListener, isSyncCallback);
            }
        }
    }

    /**
     * release
     */
    public void release() {
        mOnDownloadFileChangeListeners.clear();
    }

    /**
     * DownloadFileChangeListenerInfo
     */
    private class DownloadFileChangeListenerInfo {

        private DownloadFileChangeConfiguration mDownloadFileChangeConfiguration;
        private OnDownloadFileChangeListener mListener;

        public DownloadFileChangeListenerInfo(DownloadFileChangeConfiguration downloadFileChangeConfiguration, 
                                              OnDownloadFileChangeListener listener) {
            mDownloadFileChangeConfiguration = downloadFileChangeConfiguration;
            mListener = listener;
        }
    }
}
