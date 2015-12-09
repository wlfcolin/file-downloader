package org.wlf.filedownloader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import org.wlf.filedownloader.DownloadFileInfo.Table;
import org.wlf.filedownloader.base.FailException;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.db.ContentDbDao;
import org.wlf.filedownloader.db_recoder.DownloadFileDbHelper;
import org.wlf.filedownloader.db_recoder.DownloadFileDbRecorder;
import org.wlf.filedownloader.listener.OnDownloadFileChangeListener;
import org.wlf.filedownloader.listener.OnDownloadFileChangeListener.Type;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.ContentValuesUtil;
import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader.util.MapUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * a class to manage DownloadFile Cache
 * <br/>
 * 下载文件缓存器
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DownloadFileCacher extends DownloadFileDbRecorder {

    private static final String TAG = DownloadFileCacher.class.getSimpleName();

    private DownloadFileDbHelper mDownloadFileDbHelper;

    // download file memory cache
    private Map<String, DownloadFileInfo> mDownloadFileInfoMap = new HashMap<String, DownloadFileInfo>();

    private Object mModifyLock = new Object();// lock

    private OnDownloadFileChangeListener mOnDownloadFileChangeListener;

    // package use only

    /**
     * constructor of DownloadFileCacher
     *
     * @param context                      Context
     * @param onDownloadFileChangeListener
     */
    DownloadFileCacher(Context context, OnDownloadFileChangeListener onDownloadFileChangeListener) {
        mDownloadFileDbHelper = new DownloadFileDbHelper(context);
        this.mOnDownloadFileChangeListener = onDownloadFileChangeListener;
        initDownloadFileInfoMapFromDb();
    }

    @Override
    public void recordStatus(String url, int status, int increaseSize) throws DownloadStatusRecordException {

        Log.d(TAG, "recordStatus 记录状态：status：" + status + "，increaseSize：" + +increaseSize + "，url：" + url);

        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo != null) {
            synchronized (mModifyLock) {// lock
                if (increaseSize > 0) {
                    downloadFileInfo.setDownloadedSize(downloadFileInfo.getDownloadedSize() + increaseSize);
                }
                downloadFileInfo.setStatus(status);
                updateDownloadFileInternal(downloadFileInfo, false);
            }
        }
    }

    @Override
    public boolean addDownloadFile(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return false;
        }

        ContentDbDao dao = mDownloadFileDbHelper.getContentDbDao(DownloadFileInfo.Table.TABLE_NAME_OF_DOWNLOAD_FILE);
        if (dao == null) {
            return false;
        }

        ContentValues values = downloadFileInfo.getContentValues();
        if (ContentValuesUtil.isEmpty(values)) {
            return false;
        }

        String url = downloadFileInfo.getUrl();

        synchronized (mModifyLock) {// lock
            long id = dao.insert(values);
            if (id != -1) {
                // succeed,update memory cache
                downloadFileInfo.setId(new Integer((int) id));
                mDownloadFileInfoMap.put(url, downloadFileInfo);
                // notify listener
                if (mOnDownloadFileChangeListener != null) {
                    mOnDownloadFileChangeListener.onDownloadFileCreated(downloadFileInfo);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * update an exist DownloadFile
     */
    private boolean updateDownloadFileInternal(DownloadFileInfo downloadFileInfo, boolean lockInternal) {

        if (downloadFileInfo == null) {
            return false;
        }

        ContentDbDao dao = mDownloadFileDbHelper.getContentDbDao(DownloadFileInfo.Table.TABLE_NAME_OF_DOWNLOAD_FILE);
        if (dao == null) {
            return false;
        }

        ContentValues values = downloadFileInfo.getContentValues();
        if (ContentValuesUtil.isEmpty(values)) {
            return false;
        }

        String url = downloadFileInfo.getUrl();

        if (lockInternal) {// need internal lock
            synchronized (mModifyLock) {// lock
                int result = dao.update(values, DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_ID + "= ?", new 
                        String[]{downloadFileInfo.getId() + ""});
                if (result == 1) {
                    // succeed,update memory cache
                    if (mDownloadFileInfoMap.containsKey(url)) {
                        DownloadFileInfo downloadFileInfoInMap = mDownloadFileInfoMap.get(url);
                        downloadFileInfoInMap.update(downloadFileInfo);
                    } else {
                        mDownloadFileInfoMap.put(url, downloadFileInfo);
                    }
                    // notify listener
                    if (mOnDownloadFileChangeListener != null) {
                        mOnDownloadFileChangeListener.onDownloadFileUpdated(downloadFileInfo, Type.OTHER);// FIXME 
                        // now notify all
                    }
                    return true;
                }
            }
        } else {// not lock
            int result = dao.update(values, DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_ID + "= ?", new 
                    String[]{downloadFileInfo.getId() + ""});
            if (result == 1) {
                // succeed,update memory cache
                if (mDownloadFileInfoMap.containsKey(url)) {
                    DownloadFileInfo downloadFileInfoInMap = mDownloadFileInfoMap.get(url);
                    downloadFileInfoInMap.update(downloadFileInfo);
                } else {
                    mDownloadFileInfoMap.put(url, downloadFileInfo);
                }
                // notify listener
                if (mOnDownloadFileChangeListener != null) {
                    mOnDownloadFileChangeListener.onDownloadFileUpdated(downloadFileInfo, Type.OTHER);// FIXME now 
                    // notify all
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean updateDownloadFile(DownloadFileInfo downloadFileInfo) {
        return updateDownloadFileInternal(downloadFileInfo, true);
    }

    @Override
    public boolean deleteDownloadFile(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return false;
        }

        ContentDbDao dao = mDownloadFileDbHelper.getContentDbDao(DownloadFileInfo.Table.TABLE_NAME_OF_DOWNLOAD_FILE);
        if (dao == null) {
            return false;
        }

        String url = downloadFileInfo.getUrl();

        synchronized (mModifyLock) {// lock
            int result = dao.delete(DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_ID + "= ?", new 
                    String[]{downloadFileInfo.getId() + ""});
            if (result == 1) {
                // succeed,update memory cache
                mDownloadFileInfoMap.remove(url);
                // notify observer
                if (mOnDownloadFileChangeListener != null) {
                    mOnDownloadFileChangeListener.onDownloadFileDeleted(downloadFileInfo);
                }
                return true;
            } else {
                // try to delete by url
                result = dao.delete(Table.COLUMN_NAME_OF_FIELD_URL + "= ?", new String[]{url + ""});
                if (result == 1) {
                    // succeed,update memory cache
                    mDownloadFileInfoMap.remove(url);
                    // notify listener
                    if (mOnDownloadFileChangeListener != null) {
                        mOnDownloadFileChangeListener.onDownloadFileDeleted(downloadFileInfo);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public DownloadFileInfo getDownloadFile(String url) {

        DownloadFileInfo downloadFileInfo = null;

        if (mDownloadFileInfoMap.get(url) != null) {
            // return memory cache
            downloadFileInfo = mDownloadFileInfoMap.get(url);
        } else {
            // find in database
            ContentDbDao dao = mDownloadFileDbHelper.getContentDbDao(DownloadFileInfo.Table
                    .TABLE_NAME_OF_DOWNLOAD_FILE);
            if (dao == null) {
                return null;
            }

            Cursor cursor = dao.query(null, DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_URL + "= ?", new 
                    String[]{url}, null);
            if (cursor != null && cursor.moveToFirst()) {
                downloadFileInfo = new DownloadFileInfo(cursor);
            }

            // close the cursor
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }

            if (downloadFileInfo == null) {
                return null;
            }

            String downloadUrl = downloadFileInfo.getUrl();
            if (UrlUtil.isUrl(downloadUrl)) {
                synchronized (mModifyLock) {// lock
                    // cache in memory
                    mDownloadFileInfoMap.put(downloadUrl, downloadFileInfo);
                    downloadFileInfo = mDownloadFileInfoMap.get(url);
                }
            }
        }

        checkFileStatus(downloadFileInfo);

        return downloadFileInfo;
    }

    @Override
    public List<DownloadFileInfo> getDownloadFiles() {

        // if memory cache is empty,then init from database
        if (MapUtil.isEmpty(mDownloadFileInfoMap)) {
            initDownloadFileInfoMapFromDb();
        }

        // if this time the memory cache is not empty,return the cache
        if (!MapUtil.isEmpty(mDownloadFileInfoMap)) {
            List<DownloadFileInfo> downloadFileInfos = new ArrayList<DownloadFileInfo>(mDownloadFileInfoMap.values());

            // check status
            if (!CollectionUtil.isEmpty(downloadFileInfos)) {
                for (DownloadFileInfo downloadFileInfo : downloadFileInfos) {
                    checkFileStatus(downloadFileInfo);
                }
            }
            return downloadFileInfos;
        }

        // otherwise return empty list
        return new ArrayList<DownloadFileInfo>();
    }

    // package use only

    /**
     * get DownloadFile by savePath
     *
     * @param savePath            the path of the file saved in
     * @param includeTempFilePath true means try use the savePath as temp file savePath if can not get DownloadFile
     *                            by savePath
     * @return DownloadFile
     */
    DownloadFileInfo getDownloadFileBySavePath(String savePath, boolean includeTempFilePath) {

        if (!FileUtil.isFilePath(savePath)) {
            return null;
        }

        DownloadFileInfo downloadFileInfo = null;

        // look up the memory cache
        Set<Entry<String, DownloadFileInfo>> set = mDownloadFileInfoMap.entrySet();
        Iterator<Entry<String, DownloadFileInfo>> iterator = set.iterator();
        while (iterator.hasNext()) {
            Entry<String, DownloadFileInfo> entry = iterator.next();
            if (entry == null) {
                continue;
            }
            DownloadFileInfo info = entry.getValue();
            if (info == null) {
                continue;
            }
            String filePath = info.getFilePath();
            if (TextUtils.isEmpty(filePath)) {
                continue;
            }

            if (filePath.equals(savePath)) {
                downloadFileInfo = info;// find in memory cache
                break;
            }
        }

        // find in memory cache
        if (downloadFileInfo != null) {

        } else {
            // try to find in database
            ContentDbDao dao = mDownloadFileDbHelper.getContentDbDao(DownloadFileInfo.Table
                    .TABLE_NAME_OF_DOWNLOAD_FILE);
            if (dao == null) {
                return null;
            }

            int separatorIndex = savePath.lastIndexOf(File.separator);
            if (separatorIndex == -1) {// not a file path
                return null;
            }

            String fileSaveDir = savePath.substring(0, separatorIndex);
            String fileSaveName = savePath.substring(separatorIndex + 1, savePath.length());

            Cursor cursor = dao.query(null, DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_FILE_DIR + "= ? AND " +
                    DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_FILE_NAME + "= ?", new String[]{fileSaveDir, 
                    fileSaveName}, null);
            if (cursor != null && cursor.moveToFirst()) {
                downloadFileInfo = new DownloadFileInfo(cursor);
            }

            // close the cursor
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }

            if (downloadFileInfo == null) {
                if (includeTempFilePath) {
                    // try use temp file to query
                    cursor = dao.query(null, DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_FILE_DIR + "= ? AND " +
                            Table.COLUMN_NAME_OF_FIELD_TEMP_FILE_NAME + "= ?", new String[]{fileSaveDir, 
                            fileSaveName}, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        downloadFileInfo = new DownloadFileInfo(cursor);
                    }

                    // close the cursor
                    if (cursor != null && !cursor.isClosed()) {
                        cursor.close();
                    }
                }
            }

            if (downloadFileInfo == null) {
                return null;
            }

            String url = downloadFileInfo.getUrl();
            if (UrlUtil.isUrl(url)) {
                synchronized (mModifyLock) {// lock
                    // cache in memory
                    mDownloadFileInfoMap.put(url, downloadFileInfo);
                    downloadFileInfo = mDownloadFileInfoMap.get(url);
                }
            }
        }

        checkFileStatus(downloadFileInfo);

        return downloadFileInfo;
    }

    /**
     * read all DownloadFiles from database
     */
    private void initDownloadFileInfoMapFromDb() {
        // read from database
        ContentDbDao dao = mDownloadFileDbHelper.getContentDbDao(DownloadFileInfo.Table.TABLE_NAME_OF_DOWNLOAD_FILE);
        if (dao == null) {
            return;
        }

        Cursor cursor = dao.query(null, null, null, null);
        List<DownloadFileInfo> downloadFileInfos = getDownloadFileInfosFromCursor(cursor);
        // close the cursor
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        if (CollectionUtil.isEmpty(downloadFileInfos)) {
            return;
        }

        // cache in memory
        for (DownloadFileInfo downloadFileInfo : downloadFileInfos) {
            if (downloadFileInfo == null) {
                continue;
            }
            synchronized (mModifyLock) {// lock
                mDownloadFileInfoMap.put(downloadFileInfo.getUrl(), downloadFileInfo);
            }
        }
    }

    /**
     * get DownloadFiles by database cursor
     *
     * @param cursor database cursor
     * @return DownloadFiles
     */
    private List<DownloadFileInfo> getDownloadFileInfosFromCursor(Cursor cursor) {
        List<DownloadFileInfo> downloadFileInfos = new ArrayList<DownloadFileInfo>();
        while (cursor != null && cursor.moveToNext()) {
            DownloadFileInfo downloadFileInfo = new DownloadFileInfo(cursor);
            if (downloadFileInfo == null) {
                continue;
            }
            downloadFileInfos.add(downloadFileInfo);
        }
        return downloadFileInfos;
    }

    // package use only

    /**
     * release cacher
     */
    void release() {
        synchronized (mModifyLock) {// lock
            // free memory cache
            mDownloadFileInfoMap.clear();
            // close the database
            if (mDownloadFileDbHelper != null) {
                mDownloadFileDbHelper.close();
            }
        }
    }

    private void checkFileStatus(DownloadFileInfo downloadFileInfo) {
        if (downloadFileInfo == null) {
            return;
        }

        // check whether file exist
        String filePath = null;
        switch (downloadFileInfo.getStatus()) {
            case Status.DOWNLOAD_STATUS_COMPLETED:
                filePath = downloadFileInfo.getFilePath();
                break;
            case Status.DOWNLOAD_STATUS_PAUSED:
                filePath = downloadFileInfo.getTempFilePath();
                break;
        }

        if (!TextUtils.isEmpty(filePath)) {
            File file = new File(filePath);
            if (!file.exists() && downloadFileInfo.getDownloadedSize() > 0) {
                synchronized (mModifyLock) {
                    // file not exist
                    downloadFileInfo.setStatus(Status.DOWNLOAD_STATUS_FILE_NOT_EXIST);
                    updateDownloadFileInternal(downloadFileInfo, false);
                }
            }
        }
    }

    /**
     * DownloadStatusRecordException
     */
    public static class DownloadStatusRecordException extends FailException {

        private static final long serialVersionUID = 2729490220280837606L;

        public DownloadStatusRecordException(String detailMessage, String type) {
            super(detailMessage, type);
        }

        public DownloadStatusRecordException(Throwable throwable) {
            super(throwable);
        }

        @Override
        protected void onInitTypeWithThrowable(Throwable throwable) {
            super.onInitTypeWithThrowable(throwable);
            // TODO
        }

    }

}
