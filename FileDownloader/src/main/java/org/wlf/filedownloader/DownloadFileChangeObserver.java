package org.wlf.filedownloader;

import org.wlf.filedownloader.listener.OnDownloadFileChangeListener;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

/**
 * DownloadFileChange Observer
 *
 * @author wlf(Andy)
 * @datetime 2015-12-09 17:26 GMT+8
 * @email 411086563@qq.com
 */
class DownloadFileChangeObserver implements OnDownloadFileChangeListener {

    private Set<WeakReference<OnDownloadFileChangeListener>> mWeakOnDownloadFileChangeListeners = new 
            HashSet<WeakReference<OnDownloadFileChangeListener>>();

    /**
     * add a DownloadFileChangeListener
     *
     * @param onDownloadFileChangeListener DownloadFileChangeListener
     */
    void addOnDownloadFileChangeListener(OnDownloadFileChangeListener onDownloadFileChangeListener) {
        for (WeakReference<OnDownloadFileChangeListener> weakReference : mWeakOnDownloadFileChangeListeners) {
            if (weakReference == null) {
                continue;
            }
            OnDownloadFileChangeListener weakListener = weakReference.get();
            if (weakListener == null) {
                continue;
            }
            if (weakListener == onDownloadFileChangeListener) {
                return;// has been added
            }
        }
        WeakReference<OnDownloadFileChangeListener> weakReference = new WeakReference<OnDownloadFileChangeListener>
                (onDownloadFileChangeListener);
        // add listeners weakly
        mWeakOnDownloadFileChangeListeners.add(weakReference);
    }

    /**
     * remove a DownloadFileChangeListener
     *
     * @param onDownloadFileChangeListener DownloadFileChangeListener
     */
    void removeOnDownloadFileChangeListener(OnDownloadFileChangeListener onDownloadFileChangeListener) {
        for (WeakReference<OnDownloadFileChangeListener> weakReference : mWeakOnDownloadFileChangeListeners) {
            if (weakReference == null) {
                continue;
            }
            OnDownloadFileChangeListener weakListener = weakReference.get();
            if (weakListener == null) {
                // not need to remove may has been removed
            } else {
                if (weakListener == onDownloadFileChangeListener) {
                    mWeakOnDownloadFileChangeListeners.remove(weakListener);
                    break;
                }
            }
        }
    }

    void release() {
        mWeakOnDownloadFileChangeListeners.clear();
    }

    @Override
    public void onDownloadFileCreated(DownloadFileInfo downloadFileInfo) {
        // notify all registered listeners
        for (WeakReference<OnDownloadFileChangeListener> weakReference : mWeakOnDownloadFileChangeListeners) {
            if (weakReference == null) {
                continue;
            }
            OnDownloadFileChangeListener weakListener = weakReference.get();
            if (weakListener == null || weakListener == this) {
                continue;
            }
            weakListener.onDownloadFileCreated(downloadFileInfo);
        }
    }

    @Override
    public void onDownloadFileUpdated(DownloadFileInfo downloadFileInfo, Type type) {
        // notify all registered listeners
        for (WeakReference<OnDownloadFileChangeListener> weakReference : mWeakOnDownloadFileChangeListeners) {
            if (weakReference == null) {
                continue;
            }
            OnDownloadFileChangeListener weakListener = weakReference.get();
            if (weakListener == null || weakListener == this) {
                continue;
            }
            weakListener.onDownloadFileUpdated(downloadFileInfo, type);
        }
    }

    @Override
    public void onDownloadFileDeleted(DownloadFileInfo downloadFileInfo) {
        // notify all registered listeners
        for (WeakReference<OnDownloadFileChangeListener> weakReference : mWeakOnDownloadFileChangeListeners) {
            if (weakReference == null) {
                continue;
            }
            OnDownloadFileChangeListener weakListener = weakReference.get();
            if (weakListener == null || weakListener == this) {
                continue;
            }
            weakListener.onDownloadFileDeleted(downloadFileInfo);
        }
    }
}
