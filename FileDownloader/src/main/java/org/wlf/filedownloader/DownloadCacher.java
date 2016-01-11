package org.wlf.filedownloader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import org.wlf.filedownloader.DownloadFileInfo.Table;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.db.ContentDbDao;
import org.wlf.filedownloader.file_delete.DownloadFileDeleter;
import org.wlf.filedownloader.file_download.DetectUrlFileInfo;
import org.wlf.filedownloader.file_download.base.DownloadRecorder;
import org.wlf.filedownloader.file_download.db_recorder.DownloadFileDbHelper;
import org.wlf.filedownloader.file_move.DownloadFileMover;
import org.wlf.filedownloader.file_rename.DownloadFileRenamer;
import org.wlf.filedownloader.listener.OnDownloadFileChangeListener;
import org.wlf.filedownloader.listener.OnDownloadFileChangeListener.Type;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.ContentValuesUtil;
import org.wlf.filedownloader.util.DownloadFileUtil;
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
     * @since 0.3.0
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
            if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
                continue;
            }
            synchronized (mModifyLock) {// lock
                mDownloadFileInfoMap.put(downloadFileInfo.getUrl(), downloadFileInfo);
            }
        }
    }

    private boolean addDownloadFile(DownloadFileInfo downloadFileInfo) {

        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
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

        DownloadFileInfo downloadFileInfoExist = getDownloadFile(url);
        if (DownloadFileUtil.isLegal(downloadFileInfoExist)) {
            // exist the download file, update
            synchronized (mModifyLock) {// lock
                Type type = Type.OTHER;
                int changeCount = 0;

                if (downloadFileInfoExist.getStatus() != downloadFileInfo.getStatus()) {
                    changeCount++;
                    type = Type.DOWNLOAD_STATUS;
                }
                if (downloadFileInfoExist.getDownloadedSizeLong() != downloadFileInfo.getDownloadedSizeLong()) {
                    changeCount++;
                    type = Type.DOWNLOADED_SIZE;
                }
                if (downloadFileInfoExist.getFileDir() != null && !downloadFileInfoExist.getFileDir().equals
                        (downloadFileInfo.getFileDir())) {
                    changeCount++;
                    type = Type.SAVE_DIR;
                }
                if (downloadFileInfoExist.getFileName() != null && !downloadFileInfoExist.getFileName().equals
                        (downloadFileInfo.getFileName())) {
                    changeCount++;
                    type = Type.SAVE_FILE_NAME;
                }

                if (changeCount > 1) {
                    type = Type.OTHER;
                }

                downloadFileInfoExist.update(downloadFileInfo);

                boolean isSucceed = updateDownloadFileInternal(downloadFileInfoExist, false, type);

                if (!isSucceed) {
                    // not concern
                }
                return true;
            }
        }

        // insert new one
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

        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
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

        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
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

        try {
            if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
                return;
            }

            // must has been downloaded
            if (downloadFileInfo.getDownloadedSizeLong() <= 0) {
                return;
            }

            // check whether file exist
            String saveFilePath = downloadFileInfo.getFilePath();
            String tempFilePath = downloadFileInfo.getTempFilePath();

            File saveFile = null;
            File tempFile = null;

            if (FileUtil.isFilePath(saveFilePath)) {
                saveFile = new File(saveFilePath);
            }
            if (FileUtil.isFilePath(tempFilePath)) {
                tempFile = new File(tempFilePath);
            }

            // if download status,may now is available
            if (downloadFileInfo.getStatus() == Status.DOWNLOAD_STATUS_FILE_NOT_EXIST) {
                boolean handled = false;
                // try to recovery complete status
                if (saveFile != null && saveFile.length() == downloadFileInfo.getDownloadedSizeLong() &&
                        downloadFileInfo.getDownloadedSizeLong() == downloadFileInfo.getFileSizeLong()) {
                    // file completed

                    Log.d(TAG, "checkDownloadFileStatus，文件已下载完，当前状态：DOWNLOAD_STATUS_FILE_NOT_EXIST" +
                            "，更新状态为：DOWNLOAD_STATUS_COMPLETED，url:" + downloadFileInfo.getUrl());

                    handled = updateStatus(downloadFileInfo, Status.DOWNLOAD_STATUS_COMPLETED);
                }

                if (!handled) {
                    // try to recovery error status, not recovery pause status because current status is 
                    // DOWNLOAD_STATUS_FILE_NOT_EXIST
                    if (tempFile != null && tempFile.exists() && tempFile.length() > 0) {
                        // file error

                        Log.d(TAG, "checkDownloadFileStatus，文件未下载完，当前状态：DOWNLOAD_STATUS_FILE_NOT_EXIST" +
                                "，更新状态为：DOWNLOAD_STATUS_ERROR，url:" + downloadFileInfo.getUrl());

                        handled = updateStatus(downloadFileInfo, Status.DOWNLOAD_STATUS_ERROR);
                    }
                }
            } else {

                boolean fileExist = true;

                // completed status
                if (DownloadFileUtil.isCompleted(downloadFileInfo)) {
                    // must has been downloaded
                    if (saveFile != null) {
                        // save file not exists
                        if (!saveFile.exists()) {
                            // check tempFile size equals total file size
                            if (tempFile != null && tempFile.exists() && tempFile.length() == downloadFileInfo
                                    .getDownloadedSizeLong() && downloadFileInfo.getDownloadedSizeLong() == 
                                    downloadFileInfo.getFileSizeLong()) {
                                // however the temp file is finished, so ignore
                                // FIXME whether need to move to save file
                            } else {

                                Log.d(TAG, "checkDownloadFileStatus，下载完成，文件不存在，更新状态为：DOWNLOAD_STATUS_FILE_NOT_EXIST" +
                                        "，url:" + downloadFileInfo.getUrl());

                                fileExist = false;
                            }
                        }
                    }
                }
                // other status
                else {
                    if (tempFile != null && tempFile.exists() && tempFile.length() > 0) {
                        // temp file exist, so ignore
                    } else {

                        Log.d(TAG, "checkDownloadFileStatus，没有下载完成，文件不存在，更新状态为：DOWNLOAD_STATUS_FILE_NOT_EXIST，url:" +
                                downloadFileInfo.getUrl());

                        fileExist = false;
                    }
                }

                if (!fileExist) {
                    // file not exist
                    updateStatus(downloadFileInfo, Status.DOWNLOAD_STATUS_FILE_NOT_EXIST);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // can ignore
        }
    }

    private boolean updateStatus(DownloadFileInfo downloadFileInfo, int newStatus) {
        synchronized (mModifyLock) {// lock
            int oldStatus = downloadFileInfo.getStatus();
            downloadFileInfo.setStatus(newStatus);
            boolean isSucceed = updateDownloadFileInternal(downloadFileInfo, false, Type.DOWNLOAD_STATUS);
            if (!isSucceed) {
                // rollback
                downloadFileInfo.setStatus(oldStatus);
            } else {
                return true;
            }
            return false;
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
        if (!DownloadFileUtil.isLegal(detectUrlFileInfo)) {
            return null;
        }
        DownloadFileInfo downloadFileInfo = new DownloadFileInfo(detectUrlFileInfo);
        // add to cache
        boolean isSucceed = addDownloadFile(downloadFileInfo);
        if (isSucceed) {
            return downloadFileInfo;
        }
        return null;
    }

    @Override
    public void recordStatus(String url, int status, int increaseSize) throws Exception {

        Log.i(TAG, "recordStatus 记录状态：status：" + status + "，increaseSize：" + increaseSize + "，url：" + url);

        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            return;
        }

        synchronized (mModifyLock) {// lock

            int oldStatus = downloadFileInfo.getStatus();
            long oldDownloadedSize = downloadFileInfo.getDownloadedSizeLong();

            boolean isStatusChange = (status != downloadFileInfo.getStatus());
            if (!isStatusChange && increaseSize <= 0) {
                return;
            }

            Type type = Type.OTHER;
            int changeCount = 0;

            if (isStatusChange) {
                downloadFileInfo.setStatus(status);
                changeCount++;
                type = Type.DOWNLOAD_STATUS;
            }
            if (increaseSize > 0) {
                downloadFileInfo.setDownloadedSize(downloadFileInfo.getDownloadedSizeLong() + increaseSize);
                changeCount++;
                type = Type.DOWNLOADED_SIZE;
            }
            if (changeCount > 1) {
                type = Type.OTHER;
            }
            boolean isSucceed = updateDownloadFileInternal(downloadFileInfo, false, type);
            if (!isSucceed) {
                downloadFileInfo.setStatus(oldStatus);
                downloadFileInfo.setDownloadedSize(oldDownloadedSize);

                throw new Exception("record failed!");
            }
        }
    }

    @Override
    public void resetDownloadFile(String url) throws Exception {
        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            return;
        }
        synchronized (mModifyLock) {// lock
            long oldDownloadedSize = downloadFileInfo.getDownloadedSizeLong();
            downloadFileInfo.setDownloadedSize(0);
            boolean isSucceed = updateDownloadFileInternal(downloadFileInfo, false, Type.DOWNLOADED_SIZE);
            if (!isSucceed) {
                // rollback
                downloadFileInfo.setDownloadedSize(oldDownloadedSize);
                throw new Exception("reset failed!");
            }
        }
    }

    @Override
    public void moveDownloadFile(String url, String newDirPath) throws Exception {
        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            throw new Exception("download file doest not exist or illegal!");
        }
        synchronized (mModifyLock) {// lock
            String oldFileDir = downloadFileInfo.getFileDir();
            downloadFileInfo.setFileDir(newDirPath);
            boolean isSucceed = updateDownloadFileInternal(downloadFileInfo, false, Type.SAVE_DIR);
            if (!isSucceed) {
                // rollback
                downloadFileInfo.setFileDir(oldFileDir);
                throw new Exception("move failed!");
            }
        }
    }

    @Override
    public void deleteDownloadFile(String url) throws Exception {
        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (!DownloadFileUtil.isLegal(downloadFileInfo)) {
            throw new Exception("download file doest not exist or illegal!");
        }
        boolean isSucceed = deleteDownloadFile(downloadFileInfo);
        if (!isSucceed) {
            throw new Exception("delete failed!");
        }
    }

    @Override
    public void renameDownloadFile(String url, String newFileName) throws Exception {
        DownloadFileInfo downloadFileInfo = getDownloadFile(url);
        if (downloadFileInfo == null) {
            throw new Exception("download file doest not exist or illegal!");
        }
        synchronized (mModifyLock) {// lock
            String oldFileName = downloadFileInfo.getFileName();
            downloadFileInfo.setFileName(newFileName);
            boolean isSucceed = updateDownloadFileInternal(downloadFileInfo, false, Type.SAVE_FILE_NAME);
            if (!isSucceed) {
                // rollback
                downloadFileInfo.setFileName(oldFileName);
                throw new Exception("rename failed!");
            }
        }
    }
}
