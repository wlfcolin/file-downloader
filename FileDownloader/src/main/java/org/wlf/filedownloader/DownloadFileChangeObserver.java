package org.wlf.filedownloader;

import android.util.Log;

import org.wlf.filedownloader.listener.OnDownloadFileChangeListener;
import org.wlf.filedownloader.util.CollectionUtil;
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
     * add a DownloadFileChangeListener
     *
     * @param onDownloadFileChangeListener    DownloadFileChangeListener
     * @param downloadFileChangeConfiguration the Configuration for the DownloadFileChangeListener
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
                .toString() : "";

        Log.i(TAG, "file-downloader-listener 添加【文件改变监听器】成功，监听的urls：" + urls);
    }

    /**
     * remove a DownloadFileChangeListener
     *
     * @param onDownloadFileChangeListener DownloadFileChangeListener
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
                        .mDownloadFileChangeConfiguration.getListenUrls().toString() : "";

                Log.i(TAG, "file-downloader-listener 添加【文件改变监听器】成功，监听的urls：" + urls);

                break;
            }
        }
    }

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
            // async notify caller
            OnDownloadFileChangeListener.MainThreadHelper.onDownloadFileCreated(downloadFileInfo, listener);
        }

        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【文件被创建】，被创建文件的url：" + url);
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
            // async notify caller
            OnDownloadFileChangeListener.MainThreadHelper.onDownloadFileUpdated(downloadFileInfo, type, listener);
        }

        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【文件被更新】，更新类型：" + type.toString() + "被更新文件的url：" + url);
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
            // async notify caller
            OnDownloadFileChangeListener.MainThreadHelper.onDownloadFileDeleted(downloadFileInfo, listener);
        }
        String url = downloadFileInfo != null ? downloadFileInfo.getUrl() : "unknown";

        Log.i(TAG, "file-downloader-listener 通知【文件被删除】，被删除文件的url：" + url);
    }


    @Override
    public void onDownloadFileCreated(DownloadFileInfo downloadFileInfo) {
        // notify all match registered listeners
        if (downloadFileInfo == null || !UrlUtil.isUrl(downloadFileInfo.getUrl())) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadFileChangeListenerInfo listenerInfo : mOnDownloadFileChangeListeners) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            if (listenerInfo.mDownloadFileChangeConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadFileChangeConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadFileChangeConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl)) {
                        // find match url, notify caller
                        notifyDownloadFileCreated(downloadFileInfo, listenerInfo.mListener, listenerInfo
                                .mDownloadFileChangeConfiguration.isSyncCallback());
                    }
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadFileChangeConfiguration == null || CollectionUtil.isEmpty(listenerInfo
                        .mDownloadFileChangeConfiguration.getListenUrls())) {
                    // notify caller
                    notifyDownloadFileCreated(downloadFileInfo, listenerInfo.mListener, listenerInfo
                            .mDownloadFileChangeConfiguration.isSyncCallback());
                } else {
                    // do not notify
                }
            }
        }
    }

    @Override
    public void onDownloadFileUpdated(DownloadFileInfo downloadFileInfo, Type type) {
        // notify all match registered listeners
        if (downloadFileInfo == null || !UrlUtil.isUrl(downloadFileInfo.getUrl())) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadFileChangeListenerInfo listenerInfo : mOnDownloadFileChangeListeners) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            if (listenerInfo.mDownloadFileChangeConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadFileChangeConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadFileChangeConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl)) {
                        // find match url, notify caller
                        notifyDownloadFileUpdated(downloadFileInfo, type, listenerInfo.mListener, listenerInfo
                                .mDownloadFileChangeConfiguration.isSyncCallback());
                    }
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadFileChangeConfiguration == null || CollectionUtil.isEmpty(listenerInfo
                        .mDownloadFileChangeConfiguration.getListenUrls())) {
                    // notify caller
                    notifyDownloadFileUpdated(downloadFileInfo, type, listenerInfo.mListener, listenerInfo
                            .mDownloadFileChangeConfiguration.isSyncCallback());
                } else {
                    // do not notify
                }
            }
        }
    }

    @Override
    public void onDownloadFileDeleted(DownloadFileInfo downloadFileInfo) {
        // notify all match registered listeners
        if (downloadFileInfo == null || !UrlUtil.isUrl(downloadFileInfo.getUrl())) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadFileChangeListenerInfo listenerInfo : mOnDownloadFileChangeListeners) {

            if (listenerInfo == null || listenerInfo.mListener == null || listenerInfo.mListener == this) {
                continue;
            }

            if (listenerInfo.mDownloadFileChangeConfiguration != null && !CollectionUtil.isEmpty(listenerInfo
                    .mDownloadFileChangeConfiguration.getListenUrls())) {
                for (String listenUrl : listenerInfo.mDownloadFileChangeConfiguration.getListenUrls()) {
                    if (!UrlUtil.isUrl(listenUrl)) {
                        continue;
                    }
                    if (url.equals(listenUrl)) {
                        // find match url, notify caller
                        notifyDownloadFileDeleted(downloadFileInfo, listenerInfo.mListener, listenerInfo
                                .mDownloadFileChangeConfiguration.isSyncCallback());
                    }
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadFileChangeConfiguration == null || CollectionUtil.isEmpty(listenerInfo
                        .mDownloadFileChangeConfiguration.getListenUrls())) {
                    // notify caller
                    notifyDownloadFileDeleted(downloadFileInfo, listenerInfo.mListener, listenerInfo
                            .mDownloadFileChangeConfiguration.isSyncCallback());
                } else {
                    // do not notify
                }
            }
        }
    }

    public void release() {
        mOnDownloadFileChangeListeners.clear();
    }

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
