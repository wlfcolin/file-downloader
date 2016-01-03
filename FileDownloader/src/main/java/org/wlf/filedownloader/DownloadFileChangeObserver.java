package org.wlf.filedownloader;

import android.text.TextUtils;

import org.wlf.filedownloader.listener.OnDownloadFileChangeListener;
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

    private Set<DownloadFileChangeListenerInfo> mOnDownloadFileChangeListeners = new 
            CopyOnWriteArraySet<DownloadFileChangeListenerInfo>();

    /**
     * add a DownloadFileChangeListener
     *
     * @param onDownloadFileChangeListener DownloadFileChangeListener
     * @param downloadFileChangeConfiguration          the Configuration for the DownloadFileChangeListener
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
                break;
            }
        }
    }

    @Override
    public void onDownloadFileCreated(DownloadFileInfo downloadFileInfo) {
        // notify all match registered listeners
        if (downloadFileInfo == null || !UrlUtil.isUrl(downloadFileInfo.getUrl())) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadFileChangeListenerInfo listenerInfo : mOnDownloadFileChangeListeners) {
            if (listenerInfo == null) {
                continue;
            }
            if (listenerInfo.mDownloadFileChangeConfiguration != null && url.equals(listenerInfo
                    .mDownloadFileChangeConfiguration.mUrl)) {
                // find match url,notify
                if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                    OnDownloadFileChangeListener.MainThreadHelper.onDownloadFileCreated(downloadFileInfo, 
                            listenerInfo.mListener);
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadFileChangeConfiguration == null || TextUtils.isEmpty(listenerInfo
                        .mDownloadFileChangeConfiguration.mUrl)) {
                    if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                        OnDownloadFileChangeListener.MainThreadHelper.onDownloadFileCreated(downloadFileInfo, 
                                listenerInfo.mListener);
                    }
                } else {
                    // do not notify because not match
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
            if (listenerInfo == null) {
                continue;
            }
            if (listenerInfo.mDownloadFileChangeConfiguration != null && url.equals(listenerInfo
                    .mDownloadFileChangeConfiguration.mUrl)) {
                // find match url,notify
                if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                    OnDownloadFileChangeListener.MainThreadHelper.onDownloadFileUpdated(downloadFileInfo, type, 
                            listenerInfo.mListener);
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadFileChangeConfiguration == null || TextUtils.isEmpty(listenerInfo
                        .mDownloadFileChangeConfiguration.mUrl)) {
                    if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                        OnDownloadFileChangeListener.MainThreadHelper.onDownloadFileUpdated(downloadFileInfo, type, 
                                listenerInfo.mListener);
                    }
                } else {
                    // do not notify because not match
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
            if (listenerInfo == null) {
                continue;
            }
            if (listenerInfo.mDownloadFileChangeConfiguration != null && url.equals(listenerInfo
                    .mDownloadFileChangeConfiguration.mUrl)) {
                // find match url,notify
                if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                    OnDownloadFileChangeListener.MainThreadHelper.onDownloadFileDeleted(downloadFileInfo, 
                            listenerInfo.mListener);
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadFileChangeConfiguration == null || TextUtils.isEmpty(listenerInfo
                        .mDownloadFileChangeConfiguration.mUrl)) {
                    if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                        OnDownloadFileChangeListener.MainThreadHelper.onDownloadFileDeleted(downloadFileInfo, 
                                listenerInfo.mListener);
                    }
                } else {
                    // do not notify because not match
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
