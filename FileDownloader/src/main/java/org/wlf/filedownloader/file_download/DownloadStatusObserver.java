package org.wlf.filedownloader.file_download;

import android.text.TextUtils;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener2;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * FileDownloadStatus Observer
 *
 * @author wlf(Andy)
 * @datetime 2015-12-09 17:55 GMT+8
 * @email 411086563@qq.com
 */
class DownloadStatusObserver implements OnFileDownloadStatusListener2 {

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
                break;
            }
        }
    }

    /**
     * remove all OnFileDownloadStatusListener of the url
     *
     * @param url
     */
    public void removeOnFileDownloadStatusListener(String url) {
        if (!UrlUtil.isUrl(url)) {
            return;
        }
        Set<DownloadStatusListenerInfo> listenerNeedToRemove = new HashSet<DownloadStatusListenerInfo>();
        // find needed remove
        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {
            if (listenerInfo == null || listenerInfo.mDownloadStatusConfiguration == null) {
                // not need to remove, may has been removed
                continue;
            }
            if (url.equals(listenerInfo.mDownloadStatusConfiguration.mUrl)) {
                // find one
                listenerNeedToRemove.add(listenerInfo);
            }
        }

        // remove
        if (!CollectionUtil.isEmpty(listenerNeedToRemove)) {
            mDownloadStatusListenerInfos.removeAll(listenerNeedToRemove);
        }
    }

    @Override
    public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {
        // notify all match registered listeners
        if (downloadFileInfo == null || !UrlUtil.isUrl(downloadFileInfo.getUrl())) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        for (DownloadStatusListenerInfo listenerInfo : mDownloadStatusListenerInfos) {
            if (listenerInfo == null) {
                continue;
            }
            if (listenerInfo.mDownloadStatusConfiguration != null && url.equals(listenerInfo
                    .mDownloadStatusConfiguration.mUrl)) {
                // find match url,notify
                if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                    OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusWaiting(downloadFileInfo, 
                            listenerInfo.mListener);
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadStatusConfiguration == null || TextUtils.isEmpty(listenerInfo
                        .mDownloadStatusConfiguration.mUrl)) {
                    if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusWaiting(downloadFileInfo, 
                                listenerInfo.mListener);
                    }
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
            if (listenerInfo == null) {
                continue;
            }
            if (listenerInfo.mDownloadStatusConfiguration != null && url.equals(listenerInfo
                    .mDownloadStatusConfiguration.mUrl)) {
                // find match url,notify
                if (listenerInfo.mListener != null && listenerInfo.mListener != this && listenerInfo.mListener 
                        instanceof OnFileDownloadStatusListener2) {
                    OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusRetrying(downloadFileInfo, 
                            retryTimes, (OnFileDownloadStatusListener2) listenerInfo.mListener);
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadStatusConfiguration == null || TextUtils.isEmpty(listenerInfo
                        .mDownloadStatusConfiguration.mUrl)) {
                    if (listenerInfo.mListener != null && listenerInfo.mListener != this && listenerInfo.mListener 
                            instanceof OnFileDownloadStatusListener2) {
                        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusRetrying(downloadFileInfo, 
                                retryTimes, (OnFileDownloadStatusListener2) listenerInfo.mListener);
                    }
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
            if (listenerInfo == null) {
                continue;
            }
            if (listenerInfo.mDownloadStatusConfiguration != null && url.equals(listenerInfo
                    .mDownloadStatusConfiguration.mUrl)) {
                // find match url,notify
                if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                    OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPreparing(downloadFileInfo, 
                            listenerInfo.mListener);
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadStatusConfiguration == null || TextUtils.isEmpty(listenerInfo
                        .mDownloadStatusConfiguration.mUrl)) {
                    if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPreparing(downloadFileInfo,
                                listenerInfo.mListener);
                    }
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
            if (listenerInfo == null) {
                continue;
            }
            if (listenerInfo.mDownloadStatusConfiguration != null && url.equals(listenerInfo
                    .mDownloadStatusConfiguration.mUrl)) {
                // find match url,notify
                if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                    OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPrepared(downloadFileInfo, 
                            listenerInfo.mListener);
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadStatusConfiguration == null || TextUtils.isEmpty(listenerInfo
                        .mDownloadStatusConfiguration.mUrl)) {
                    if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPrepared(downloadFileInfo, 
                                listenerInfo.mListener);
                    }
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
            if (listenerInfo == null) {
                continue;
            }
            if (listenerInfo.mDownloadStatusConfiguration != null && url.equals(listenerInfo
                    .mDownloadStatusConfiguration.mUrl)) {
                // find match url,notify
                if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                    OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusDownloading(downloadFileInfo, 
                            downloadSpeed, remainingTime, listenerInfo.mListener);
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadStatusConfiguration == null || TextUtils.isEmpty(listenerInfo
                        .mDownloadStatusConfiguration.mUrl)) {
                    if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusDownloading
                                (downloadFileInfo, downloadSpeed, remainingTime, listenerInfo.mListener);
                    }
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
            if (listenerInfo == null) {
                continue;
            }
            if (listenerInfo.mDownloadStatusConfiguration != null && url.equals(listenerInfo
                    .mDownloadStatusConfiguration.mUrl)) {
                // find match url,notify
                if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                    OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPaused(downloadFileInfo, 
                            listenerInfo.mListener);
                }
                // remove the listener
                if (listenerInfo.mDownloadStatusConfiguration.mAutoRelease) {
                    mDownloadStatusListenerInfos.remove(listenerInfo);
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadStatusConfiguration == null || TextUtils.isEmpty(listenerInfo
                        .mDownloadStatusConfiguration.mUrl)) {
                    if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusPaused(downloadFileInfo, 
                                listenerInfo.mListener);
                    }
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
            if (listenerInfo == null) {
                continue;
            }
            if (listenerInfo.mDownloadStatusConfiguration != null && url.equals(listenerInfo
                    .mDownloadStatusConfiguration.mUrl)) {
                // find match url,notify
                if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                    OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusCompleted(downloadFileInfo, 
                            listenerInfo.mListener);
                }
                // remove the listener
                if (listenerInfo.mDownloadStatusConfiguration.mAutoRelease) {
                    mDownloadStatusListenerInfos.remove(listenerInfo);
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadStatusConfiguration == null || TextUtils.isEmpty(listenerInfo
                        .mDownloadStatusConfiguration.mUrl)) {
                    if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusCompleted(downloadFileInfo,
                                listenerInfo.mListener);
                    }
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
            if (listenerInfo == null) {
                continue;
            }
            if (listenerInfo.mDownloadStatusConfiguration != null && url.equals(listenerInfo
                    .mDownloadStatusConfiguration.mUrl)) {
                // find match url,notify
                if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                    OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(url, downloadFileInfo, 
                            failReason, listenerInfo.mListener);
                }
                // remove the listener
                if (listenerInfo.mDownloadStatusConfiguration.mAutoRelease) {
                    mDownloadStatusListenerInfos.remove(listenerInfo);
                }
            } else {
                // others
                // global register listener,notify
                if (listenerInfo.mDownloadStatusConfiguration == null || TextUtils.isEmpty(listenerInfo
                        .mDownloadStatusConfiguration.mUrl)) {
                    if (listenerInfo.mListener != null && listenerInfo.mListener != this) {
                        OnFileDownloadStatusListener.MainThreadHelper.onFileDownloadStatusFailed(url, 
                                downloadFileInfo, failReason, listenerInfo.mListener);
                    }
                } else {
                    // do not notify
                }
            }
        }
    }

    public void release() {
        mDownloadStatusListenerInfos.clear();
    }

    private class DownloadStatusListenerInfo {

        private DownloadStatusConfiguration mDownloadStatusConfiguration;
        private OnFileDownloadStatusListener mListener;

        public DownloadStatusListenerInfo(DownloadStatusConfiguration downloadStatusConfiguration, 
                                          OnFileDownloadStatusListener listener) {
            mDownloadStatusConfiguration = downloadStatusConfiguration;
            mListener = listener;
        }
    }

}