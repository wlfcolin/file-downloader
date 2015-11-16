package org.wlf.filedownloader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import org.wlf.filedownloader.base.FailException;
import org.wlf.filedownloader.db.ContentDbDao;
import org.wlf.filedownloader.db_recoder.DownloadFileDbHelper;
import org.wlf.filedownloader.db_recoder.DownloadFileDbRecorder;
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

    private DownloadFileDbHelper mDownloadFileDbHelper;

    private Map<String, DownloadFileInfo> mDownloadFileInfoMap = new HashMap<String, DownloadFileInfo>();// memory cache

    private Object mModifyLock = new Object();// lock

    DownloadFileCacher(Context context) {
        mDownloadFileDbHelper = new DownloadFileDbHelper(context);
        initDownloadFileInfoMapFromDb();
    }

    @Override
    public void recordStatus(String url, int status, int increaseSize) throws DownloadStatusRecordException {
        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo != null) {
            synchronized (mModifyLock) {// lock，FIXME 无法跟updateDownloadFile一起同步
                if (increaseSize > 0) {
                    downloadFileInfo.setDownloadedSize(downloadFileInfo.getDownloadedSize() + increaseSize);
                }
                downloadFileInfo.setStatus(status);
            }
            updateDownloadFile(downloadFileInfo);
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
            long result = dao.insert(values);
            if (result == 1) {
                // succeed,update memory cache
                mDownloadFileInfoMap.put(url, downloadFileInfo);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean updateDownloadFile(DownloadFileInfo downloadFileInfo) {

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
            int result = dao.update(values, DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_ID + "= ?", new String[]{downloadFileInfo.getId() + ""});
            if (result == 1) {
                // succeed,update memory cache
                if (mDownloadFileInfoMap.containsKey(url)) {
                    DownloadFileInfo downloadFileInfoInMap = mDownloadFileInfoMap.get(url);
                    downloadFileInfoInMap.update(downloadFileInfo);
                } else {
                    mDownloadFileInfoMap.put(url, downloadFileInfo);
                }
                return true;
            }
        }

        return false;
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
            int result = dao.delete(DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_ID + "= ?", new String[]{downloadFileInfo.getId() + ""});
            if (result == 1) {
                // succeed,update memory cache
                mDownloadFileInfoMap.remove(url);
                return true;
            } else {
                // try delete by url
                //TODO
            }
        }

        return false;
    }

    @Override
    public DownloadFileInfo getDownloadFile(String url) {

        if (mDownloadFileInfoMap.get(url) != null) {
            // return memory cache
            return mDownloadFileInfoMap.get(url);
        } else {
            // find in database
            ContentDbDao dao = mDownloadFileDbHelper.getContentDbDao(DownloadFileInfo.Table.TABLE_NAME_OF_DOWNLOAD_FILE);
            if (dao == null) {
                return null;
            }

            DownloadFileInfo downloadFileInfo = null;

            Cursor cursor = dao.query(null, DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_URL + "= ?", new String[]{url}, null);
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
                    return mDownloadFileInfoMap.get(url);
                }
            }

            return mDownloadFileInfoMap.get(url);
        }
    }

    @Override
    public List<DownloadFileInfo> getDownloadFiles() {

        // if memory cache is empty,then init from database
        if (MapUtil.isEmpty(mDownloadFileInfoMap)) {
            initDownloadFileInfoMapFromDb();
        }

        // if this time the memory cache is not empty,return the cache
        if (!MapUtil.isEmpty(mDownloadFileInfoMap)) {
            return new ArrayList<DownloadFileInfo>(mDownloadFileInfoMap.values());
        }

        // otherwise return empty list
        return new ArrayList<DownloadFileInfo>();
    }

    /**
     * get DownloadFile by savePath
     *
     * @param savePath the path of the file saved in
     * @return DownloadFile
     */
    DownloadFileInfo getDownloadFileBySavePath(String savePath) {

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
            return downloadFileInfo;
        } else {
            // find in database
            ContentDbDao dao = mDownloadFileDbHelper.getContentDbDao(DownloadFileInfo.Table.TABLE_NAME_OF_DOWNLOAD_FILE);
            if (dao == null) {
                return null;
            }

            int separatorIndex = savePath.lastIndexOf(File.separator);
            if (separatorIndex == -1) {// not a file path
                return null;
            }

            String fileSaveDir = savePath.substring(0, separatorIndex);
            String fileSaveName = savePath.substring(separatorIndex, savePath.length());

            Cursor cursor = dao.query(null, DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_FILE_DIR + "= ? AND " + DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_FILE_NAME + "= ?", new String[]{fileSaveDir, fileSaveName}, null);
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

            String url = downloadFileInfo.getUrl();
            if (UrlUtil.isUrl(url)) {
                synchronized (mModifyLock) {// lock
                    // cache in memory
                    mDownloadFileInfoMap.put(url, downloadFileInfo);
                    return mDownloadFileInfoMap.get(url);
                }
            }
        }

        return null;
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
     * change database cursor to DownloadFiles
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
