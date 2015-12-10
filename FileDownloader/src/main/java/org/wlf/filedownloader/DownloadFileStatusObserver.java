package org.wlf.filedownloader;

import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

/**
 * DownloadFileStatus Observer
 *
 * @author wlf(Andy)
 * @datetime 2015-12-09 17:55 GMT+8
 * @email 411086563@qq.com
 */
class DownloadFileStatusObserver implements OnFileDownloadStatusListener {

    private Set<WeakReference<OnFileDownloadStatusListener>> mWeakOnFileDownloadStatusListeners = new 
            HashSet<WeakReference<OnFileDownloadStatusListener>>();

    /**
     * add a OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener
     */
    void addOnFileDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
            if (weakReference == null) {
                continue;
            }
            OnFileDownloadStatusListener weakListener = weakReference.get();
            if (weakListener == null) {
                continue;
            }
            if (weakListener == onFileDownloadStatusListener) {
                return;// has been added
            }
        }
        WeakReference<OnFileDownloadStatusListener> weakReference = new WeakReference<OnFileDownloadStatusListener>
                (onFileDownloadStatusListener);
        // add listeners weakly
        mWeakOnFileDownloadStatusListeners.add(weakReference);
    }

    /**
     * remove a OnFileDownloadStatusListener
     *
     * @param onFileDownloadStatusListener OnFileDownloadStatusListener
     */
    void removeOnFileDownloadStatusListener(OnFileDownloadStatusListener onFileDownloadStatusListener) {
        for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
            if (weakReference == null) {
                continue;
            }
            OnFileDownloadStatusListener weakListener = weakReference.get();
            if (weakListener == null) {
                // not need to remove may has been removed
            } else {
                if (weakListener == onFileDownloadStatusListener) {
                    mWeakOnFileDownloadStatusListeners.remove(weakListener);
                    break;
                }
            }
        }
    }

    void release() {
        mWeakOnFileDownloadStatusListeners.clear();
    }

    @Override
    public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {
        // notify all registered listeners
        for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
            if (weakReference == null) {
                continue;
            }
            OnFileDownloadStatusListener weakListener = weakReference.get();
            if (weakListener == null || weakListener == this) {
                continue;
            }
            weakListener.onFileDownloadStatusWaiting(downloadFileInfo);
        }
    }

    @Override
    public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {
        // notify all registered listeners
        for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
            if (weakReference == null) {
                continue;
            }
            OnFileDownloadStatusListener weakListener = weakReference.get();
            if (weakListener == null || weakListener == this) {
                continue;
            }
            weakListener.onFileDownloadStatusPreparing(downloadFileInfo);
        }
    }

    @Override
    public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {
        // notify all registered listeners
        for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
            if (weakReference == null) {
                continue;
            }
            OnFileDownloadStatusListener weakListener = weakReference.get();
            if (weakListener == null || weakListener == this) {
                continue;
            }
            weakListener.onFileDownloadStatusPrepared(downloadFileInfo);
        }
    }

    @Override
    public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long 
            remainingTime) {
        // notify all registered listeners
        for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
            if (weakReference == null) {
                continue;
            }
            OnFileDownloadStatusListener weakListener = weakReference.get();
            if (weakListener == null || weakListener == this) {
                continue;
            }
            weakListener.onFileDownloadStatusDownloading(downloadFileInfo, downloadSpeed, remainingTime);
        }
    }

    @Override
    public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {
        // notify all registered listeners
        for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
            if (weakReference == null) {
                continue;
            }
            OnFileDownloadStatusListener weakListener = weakReference.get();
            if (weakListener == null || weakListener == this) {
                continue;
            }
            weakListener.onFileDownloadStatusPaused(downloadFileInfo);
        }
    }

    @Override
    public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {
        // notify all registered listeners
        for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
            if (weakReference == null) {
                continue;
            }
            OnFileDownloadStatusListener weakListener = weakReference.get();
            if (weakListener == null || weakListener == this) {
                continue;
            }
            weakListener.onFileDownloadStatusCompleted(downloadFileInfo);
        }
    }

    @Override
    public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, 
                                           OnFileDownloadStatusFailReason failReason) {
        // notify all registered listeners
        for (WeakReference<OnFileDownloadStatusListener> weakReference : mWeakOnFileDownloadStatusListeners) {
            if (weakReference == null) {
                continue;
            }
            OnFileDownloadStatusListener weakListener = weakReference.get();
            if (weakListener == null || weakListener == this) {
                continue;
            }
            weakListener.onFileDownloadStatusFailed(url, downloadFileInfo, failReason);
        }
    }
}