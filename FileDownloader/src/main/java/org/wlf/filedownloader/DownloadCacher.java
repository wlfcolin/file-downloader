package org.wlf.filedownloader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import org.wlf.filedownloader.DownloadFileInfo.Table;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.db.ContentDbDao;
import org.wlf.filedownloader.file_delete.DeleteDownloadFileTask.DownloadFileDeleter;
import org.wlf.filedownloader.file_download.DetectUrlFileInfo;
import org.wlf.filedownloader.file_download.DownloadTaskManager.DownloadRecorder;
import org.wlf.filedownloader.file_download.db_recorder.DownloadFileDbHelper;
import org.wlf.filedownloader.file_move.MoveDownloadFileTask.DownloadFileMover;
import org.wlf.filedownloader.file_rename.RenameDownloadFileTask.DownloadFileRenamer;
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
public class DownloadCacher implements DownloadRecorder, DownloadFileMover, DownloadFileDeleter, DownloadFileRenamer {

    private static final String TAG = DownloadCacher.class.getSimpleName();

    private DownloadFileDbHelper mDownloadFileDbHelper;

    // download file memory cache
    private Map<String, DownloadFileInfo> mDownloadFileInfoMap = new HashMap<String, DownloadFileInfo>();

    private Object mModifyLock = new Object();// lock

    // download file change observer
    private DownloadFileChangeObserver mDownloadFileChangeObserver;

    /**
     * constructor of DownloadFileCacher
     *
     * @param context Context
     */
    DownloadCacher(Context context) {
        mDownloadFileDbHelper = new DownloadFileDbHelper(context);
        mDownloadFileChangeObserver = new DownloadFileChangeObserver();
        initDownloadFileInfoMapFromDb();
    }

    /**
     * register a DownloadFileChangeListener
     *
     * @param onDownloadFileChangeListener    the OnDownloadFileChangeListener impl
     * @param downloadFileChangeConfiguration the Configuration for the DownloadFileChangeListener
     */
    public void registerDownloadFileChangeListener(OnDownloadFileChangeListener onDownloadFileChangeListener, 
                                                   DownloadFileChangeConfiguration downloadFileChangeConfiguration) {
        mDownloadFileChangeObserver.addOnDownloadFileChangeListener(onDownloadFileChangeListener, 
                downloadFileChangeConfiguration);
    }

    /**
     * unregister an OnDownloadFileChangeListener
     *
     * @param onDownloadFileChangeListener the OnDownloadFileChangeListener impl
     */
    public void unregisterDownloadFileChangeListener(OnDownloadFileChangeListener onDownloadFileChangeListener) {
        mDownloadFileChangeObserver.removeOnDownloadFileChangeListener(onDownloadFileChangeListener);
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
        List<DownloadFileInfo> downloadFileInfos = getDownloadFilesFromCursor(cursor);
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

    private boolean addDownloadFile(DownloadFileInfo downloadFileInfo) {

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
                // notify observer
                if (mDownloadFileChangeObserver != null) {
                    mDownloadFileChangeObserver.onDownloadFileCreated(downloadFileInfo);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * update an exist DownloadFile
     */
    private boolean updateDownloadFileInternal(DownloadFileInfo downloadFileInfo, boolean lockInternal, Type 
            notifyType) {

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
                    // notify observer
                    if (mDownloadFileChangeObserver != null) {
                        mDownloadFileChangeObserver.onDownloadFileUpdated(downloadFileInfo, notifyType);
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
                // notify observer
                if (mDownloadFileChangeObserver != null) {
                    mDownloadFileChangeObserver.onDownloadFileUpdated(downloadFileInfo, notifyType);
                }
                return true;
            }
        }
        return false;
    }

    private boolean deleteDownloadFile(DownloadFileInfo downloadFileInfo) {

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
                if (mDownloadFileChangeObserver != null) {
                    mDownloadFileChangeObserver.onDownloadFileDeleted(downloadFileInfo);
                }
                return true;
            } else {
                // try to delete by url
                result = dao.delete(Table.COLUMN_NAME_OF_FIELD_URL + "= ?", new String[]{url + ""});
                if (result == 1) {
                    // succeed,update memory cache
                    mDownloadFileInfoMap.remove(url);
                    // notify observer
                    if (mDownloadFileChangeObserver != null) {
                        mDownloadFileChangeObserver.onDownloadFileDeleted(downloadFileInfo);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * get DownloadFile by savePath
     *
     * @param savePath            the path of the file saved in
     * @param includeTempFilePath true means try use the savePath as temp file savePath if can not get DownloadFile
     *                            by savePath
     * @return DownloadFile
     */
    public DownloadFileInfo getDownloadFileBySavePath(String savePath, boolean includeTempFilePath) {

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

        checkDownloadFileStatus(downloadFileInfo);

        return downloadFileInfo;
    }

    /**
     * get DownloadFiles by database cursor
     *
     * @param cursor database cursor
     * @return DownloadFiles
     */
    private List<DownloadFileInfo> getDownloadFilesFromCursor(Cursor cursor) {
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
     * check the download file status
     */
    private void checkDownloadFileStatus(DownloadFileInfo downloadFileInfo) {
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
            if (!file.exists() && downloadFileInfo.getDownloadedSizeLong() > 0) {
                synchronized (mModifyLock) {
                    // file not exist
                    downloadFileInfo.setStatus(Status.DOWNLOAD_STATUS_FILE_NOT_EXIST);
                    updateDownloadFileInternal(downloadFileInfo, false, Type.DOWNLOAD_STATUS);
                }
            }
        }
    }

    /**
     * release the cacher
     */
    public void release() {
        synchronized (mModifyLock) {// lock
            // free memory cache
            mDownloadFileInfoMap.clear();
            mDownloadFileChangeObserver.release();
            // close the database
            if (mDownloadFileDbHelper != null) {
                mDownloadFileDbHelper.close();
            }
        }
    }

    // --------------------------- interface impl ---------------------------

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

        checkDownloadFileStatus(downloadFileInfo);

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
                    checkDownloadFileStatus(downloadFileInfo);
                }
            }
            return downloadFileInfos;
        }

        // otherwise return empty list
        return new ArrayList<DownloadFileInfo>();
    }

    @Override
    public DownloadFileInfo createDownloadFileInfo(DetectUrlFileInfo detectUrlFileInfo) {
        DownloadFileInfo downloadFileInfo = new DownloadFileInfo(detectUrlFileInfo);
        // add to cache
        addDownloadFile(downloadFileInfo);
        return downloadFileInfo;
    }

    @Override
    public void recordStatus(String url, int status, int increaseSize) throws Exception {

        Log.d(TAG, "recordStatus 记录状态：status：" + status + "，increaseSize：" + +increaseSize + "，url：" + url);

        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo != null) {
            synchronized (mModifyLock) {// lock
                boolean isStatusChange = (status != downloadFileInfo.getStatus());
                if (!isStatusChange && increaseSize <= 0) {
                    return;
                }
                Type type = Type.OTHER;
                if (increaseSize > 0) {
                    downloadFileInfo.setDownloadedSize(downloadFileInfo.getDownloadedSizeLong() + increaseSize);
                    type = Type.DOWNLOADED_SIZE;
                }
                downloadFileInfo.setStatus(status);
                if (isStatusChange) {
                    type = Type.DOWNLOAD_STATUS;
                }
                updateDownloadFileInternal(downloadFileInfo, false, type);
            }
        }
    }

    @Override
    public void resetDownloadFile(String url) throws Exception {
        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo == null) {
            return;
        }
        synchronized (mModifyLock) {// lock
            downloadFileInfo.setDownloadedSize(0);
            updateDownloadFileInternal(downloadFileInfo, false, Type.DOWNLOADED_SIZE);
        }
    }

    @Override
    public void moveDownloadFile(String url, String newDirPath) throws Exception {
        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo == null) {
            throw new Exception("file doest not exist!");
        }
        synchronized (mModifyLock) {// lock
            downloadFileInfo.setFileDir(newDirPath);
            updateDownloadFileInternal(downloadFileInfo, false, Type.SAVE_DIR);
        }
    }

    @Override
    public void deleteDownloadFile(String url) throws Exception {
        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo == null) {
            throw new Exception("file doest not exist!");
        }
        deleteDownloadFile(downloadFileInfo);
    }

    @Override
    public void renameDownloadFile(String url, String newFileName) throws Exception {
        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo == null) {
            throw new Exception("file doest not exist!");
        }
        synchronized (mModifyLock) {// lock
            downloadFileInfo.setFileName(newFileName);
            updateDownloadFileInternal(downloadFileInfo, false, Type.SAVE_FILE_NAME);
        }
    }

}
