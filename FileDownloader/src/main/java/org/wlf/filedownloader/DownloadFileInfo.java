package org.wlf.filedownloader;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import org.wlf.filedownloader.base.BaseUrlFileInfo;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.file_download.DetectUrlFileInfo;
import org.wlf.filedownloader.util.DateUtil;
import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.io.File;
import java.util.Date;

/**
 * download file Model,synchronous with database table
 * <br/>
 * 下载文件信息模型，跟数据库一致
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DownloadFileInfo extends BaseUrlFileInfo {

    /**
     * {@link DownloadFileInfo} database table info
     */
    public static final class Table {

        /**
         * table name
         */
        public static final String TABLE_NAME_OF_DOWNLOAD_FILE = "tb_download_file";

        /**
         * id field name
         */
        public static final String COLUMN_NAME_OF_FIELD_ID = "_id";
        /**
         * url field name
         */
        public static final String COLUMN_NAME_OF_FIELD_URL = "url";
        /**
         * downloadedSize field name
         */
        public static final String COLUMN_NAME_OF_FIELD_DOWNLOADED_SIZE = "downloaded_size";
        /**
         * fileSize field name
         */
        public static final String COLUMN_NAME_OF_FIELD_FILE_SIZE = "file_size";
        /**
         * eTag field name
         */
        public static final String COLUMN_NAME_OF_FIELD_E_TAG = "e_tag";
        /**
         * acceptRangeType field name
         */
        public static final String COLUMN_NAME_OF_FIELD_ACCEPT_RANGE_TYPE = "accept_range_type";
        /**
         * fileSize field name
         */
        public static final String COLUMN_NAME_OF_FIELD_FILE_DIR = "file_dir";
        /**
         * tempFileName field name
         */
        public static final String COLUMN_NAME_OF_FIELD_TEMP_FILE_NAME = "temp_file_name";
        /**
         * fileName field name
         */
        public static final String COLUMN_NAME_OF_FIELD_FILE_NAME = "file_name";
        /**
         * status field name
         */
        public static final String COLUMN_NAME_OF_FIELD_STATUS = "status";
        /**
         * create download datetime
         */
        public static final String COLUMN_NAME_OF_FIELD_CREATE_DATETIME = "create_datetime";

        /**
         * the sql to create table
         */
        public static final String getCreateTableSql() {

            String createTableSql = "CREATE TABLE IF NOT EXISTS " //
                    + TABLE_NAME_OF_DOWNLOAD_FILE //

                    + "(" + COLUMN_NAME_OF_FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"//
                    + COLUMN_NAME_OF_FIELD_URL + " TEXT UNIQUE,"//
                    + COLUMN_NAME_OF_FIELD_DOWNLOADED_SIZE + " INTEGER,"//
                    + COLUMN_NAME_OF_FIELD_FILE_SIZE + " INTEGER,"//
                    + COLUMN_NAME_OF_FIELD_E_TAG + " TEXT,"//
                    + COLUMN_NAME_OF_FIELD_ACCEPT_RANGE_TYPE + " TEXT,"//
                    + COLUMN_NAME_OF_FIELD_FILE_DIR + " TEXT,"//
                    + COLUMN_NAME_OF_FIELD_TEMP_FILE_NAME + " TEXT,"//
                    + COLUMN_NAME_OF_FIELD_FILE_NAME + " TEXT,"//
                    + COLUMN_NAME_OF_FIELD_STATUS + " INTEGER,"//
                    + COLUMN_NAME_OF_FIELD_CREATE_DATETIME + " TEXT" + ")";//            

            return createTableSql;
        }

        /**
         * the sql to update table when db version is 1 to 2
         */
        public static final String getUpdateTableVersion1To2Sql() {

            String updateTableVersion1To2Sql = "ALTER TABLE " //
                    + TABLE_NAME_OF_DOWNLOAD_FILE //
                    + " ADD " //
                    + COLUMN_NAME_OF_FIELD_CREATE_DATETIME + " TEXT"; //

            return updateTableVersion1To2Sql;
        }
    }

    /**
     * temp file suffix
     */
    private static final String TEMP_FILE_SUFFIX = "temp";

    /**
     * id
     */
    private Integer mId;
    /**
     * downloadedSize
     */
    private long mDownloadedSize;
    /**
     * TempFileName
     */
    private String mTempFileName;
    /**
     * download status，ref{@link Status}
     */
    private int mStatus = Status.DOWNLOAD_STATUS_UNKNOWN;
    /**
     * create download datetime, yyyy-MM-dd HH:mm:ss
     */
    private String mCreateDatetime;

    @SuppressWarnings("unused")
    private DownloadFileInfo() {
    }

    // package use only

    /**
     * constructor of HttpDownloader,use DetectUrlFileInfo to create
     *
     * @param detectUrlFileInfo DetectUrlFileInfo
     */
    DownloadFileInfo(DetectUrlFileInfo detectUrlFileInfo) {
        this.mUrl = detectUrlFileInfo.getUrl();
        this.mFileName = detectUrlFileInfo.getFileName();
        this.mFileSize = detectUrlFileInfo.getFileSizeLong();
        this.mAcceptRangeType = detectUrlFileInfo.getAcceptRangeType();
        this.mFileDir = detectUrlFileInfo.getFileDir();
        this.mTempFileName = mFileName + "." + TEMP_FILE_SUFFIX;
        // this.mStatus = Status.DOWNLOAD_STATUS_WAITING;// download status
        this.mCreateDatetime = DateUtil.date2String_yyyy_MM_dd_HH_mm_ss(new Date());
    }

    // package use only

    /**
     * constructor of HttpDownloader,use {@link Cursor} to create
     *
     * @param cursor database cursor
     */
    DownloadFileInfo(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            int id = -1;
            String url = null;
            long downloadedSize = 0;
            long fileSize = 0;
            String eTag = null;
            String acceptRangeType = null;
            String fileDir = null;
            String tempFileName = null;
            String fileName = null;
            int status = Status.DOWNLOAD_STATUS_UNKNOWN;
            String createDatetime = null;

            int columnIndex = -1;
            columnIndex = cursor.getColumnIndex(Table.COLUMN_NAME_OF_FIELD_ID);
            if (columnIndex != -1) {
                id = cursor.getInt(columnIndex);
            }
            columnIndex = cursor.getColumnIndex(Table.COLUMN_NAME_OF_FIELD_URL);
            if (columnIndex != -1) {
                url = cursor.getString(columnIndex);
            }
            columnIndex = cursor.getColumnIndex(Table.COLUMN_NAME_OF_FIELD_DOWNLOADED_SIZE);
            if (columnIndex != -1) {
                downloadedSize = cursor.getLong(columnIndex);
            }
            columnIndex = cursor.getColumnIndex(Table.COLUMN_NAME_OF_FIELD_FILE_SIZE);
            if (columnIndex != -1) {
                fileSize = cursor.getLong(columnIndex);
            }
            columnIndex = cursor.getColumnIndex(Table.COLUMN_NAME_OF_FIELD_E_TAG);
            if (columnIndex != -1) {
                eTag = cursor.getString(columnIndex);
            }
            columnIndex = cursor.getColumnIndex(Table.COLUMN_NAME_OF_FIELD_ACCEPT_RANGE_TYPE);
            if (columnIndex != -1) {
                acceptRangeType = cursor.getString(columnIndex);
            }
            columnIndex = cursor.getColumnIndex(Table.COLUMN_NAME_OF_FIELD_FILE_DIR);
            if (columnIndex != -1) {
                fileDir = cursor.getString(columnIndex);
            }
            columnIndex = cursor.getColumnIndex(Table.COLUMN_NAME_OF_FIELD_TEMP_FILE_NAME);
            if (columnIndex != -1) {
                tempFileName = cursor.getString(columnIndex);
            }
            columnIndex = cursor.getColumnIndex(Table.COLUMN_NAME_OF_FIELD_FILE_NAME);
            if (columnIndex != -1) {
                fileName = cursor.getString(columnIndex);
            }
            columnIndex = cursor.getColumnIndex(Table.COLUMN_NAME_OF_FIELD_STATUS);
            if (columnIndex != -1) {
                status = cursor.getInt(columnIndex);
            }
            columnIndex = cursor.getColumnIndex(Table.COLUMN_NAME_OF_FIELD_CREATE_DATETIME);
            if (columnIndex != -1) {
                createDatetime = cursor.getString(columnIndex);
            }
            if (id > 0 && !TextUtils.isEmpty(url)) {
                // init fields
                this.mId = id;
                this.mUrl = url;
                this.mDownloadedSize = downloadedSize;
                this.mFileSize = fileSize;
                this.mETag = eTag;
                this.mAcceptRangeType = acceptRangeType;
                this.mFileDir = fileDir;
                this.mTempFileName = tempFileName;
                this.mFileName = fileName;
                this.mStatus = status;
                this.mCreateDatetime = createDatetime;
            } else {
                throw new IllegalArgumentException("id and url from cursor illegal!");
            }
        } else {
            throw new NullPointerException("cursor illegal!");
        }
    }

    // package use only

    /**
     * update DownloadFileInfo with new DownloadFileInfo
     *
     * @param downloadFileInfo new DownloadFileInfo
     */
    void update(DownloadFileInfo downloadFileInfo) {
        if (downloadFileInfo.mId != null && downloadFileInfo.mId > 0) {
            this.mId = downloadFileInfo.mId;
        }
        if (UrlUtil.isUrl(downloadFileInfo.mUrl)) {
            this.mUrl = downloadFileInfo.mUrl;
        }
        if (downloadFileInfo.mDownloadedSize > 0 && downloadFileInfo.mDownloadedSize != this.mDownloadedSize) {
            this.mDownloadedSize = downloadFileInfo.mDownloadedSize;
        }
        if (downloadFileInfo.mFileSize > 0 && downloadFileInfo.mFileSize != this.mFileSize) {
            this.mFileSize = downloadFileInfo.mFileSize;
        }
        if (!TextUtils.isEmpty(downloadFileInfo.mETag)) {
            this.mETag = downloadFileInfo.mETag;
        }
        if (!TextUtils.isEmpty(downloadFileInfo.mAcceptRangeType)) {
            this.mAcceptRangeType = downloadFileInfo.mAcceptRangeType;
        }
        if (FileUtil.isFilePath(downloadFileInfo.mFileDir)) {
            this.mFileDir = downloadFileInfo.mFileDir;
        }
        if (!TextUtils.isEmpty(downloadFileInfo.mTempFileName)) {
            this.mTempFileName = downloadFileInfo.mTempFileName;
        }
        if (!TextUtils.isEmpty(downloadFileInfo.mFileName)) {
            this.mFileName = downloadFileInfo.mFileName;
        }
        if (downloadFileInfo.mStatus != this.mStatus) {
            this.mStatus = downloadFileInfo.mStatus;
        }
        if (!TextUtils.isEmpty(downloadFileInfo.mCreateDatetime)) {
            this.mCreateDatetime = downloadFileInfo.mCreateDatetime;
        }
    }

    // package use only

    /**
     * get ContentValues for all fields
     */
    ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        values.put(Table.COLUMN_NAME_OF_FIELD_URL, mUrl);
        values.put(Table.COLUMN_NAME_OF_FIELD_DOWNLOADED_SIZE, mDownloadedSize);
        values.put(Table.COLUMN_NAME_OF_FIELD_FILE_SIZE, mFileSize);
        values.put(Table.COLUMN_NAME_OF_FIELD_E_TAG, mETag);
        values.put(Table.COLUMN_NAME_OF_FIELD_ACCEPT_RANGE_TYPE, mAcceptRangeType);
        values.put(Table.COLUMN_NAME_OF_FIELD_FILE_DIR, mFileDir);
        values.put(Table.COLUMN_NAME_OF_FIELD_TEMP_FILE_NAME, mTempFileName);
        values.put(Table.COLUMN_NAME_OF_FIELD_FILE_NAME, mFileName);
        values.put(Table.COLUMN_NAME_OF_FIELD_STATUS, mStatus);
        values.put(Table.COLUMN_NAME_OF_FIELD_CREATE_DATETIME, mCreateDatetime);
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (!TextUtils.isEmpty(mUrl)) {
            if (o instanceof DownloadFileInfo) {
                DownloadFileInfo other = (DownloadFileInfo) o;
                return mUrl.equals(other.mUrl);
            }
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        if (!TextUtils.isEmpty(mUrl)) {
            return mUrl.hashCode();
        }
        return super.hashCode();
    }

    // setters,package use only

    /**
     * set id
     *
     * @param id
     */
    void setId(Integer id) {
        mId = id;
    }

    /**
     * set download size
     */
    void setDownloadedSize(long downloadedSize) {
        this.mDownloadedSize = downloadedSize;
    }

    /**
     * set download status
     */
    void setStatus(int status) {
        this.mStatus = status;
    }

    /**
     * set create download datetime
     */
    void setCreateDatetime(String createDatetime) {
        this.mCreateDatetime = createDatetime;
    }

    /**
     * set save file dir
     */
    protected void setFileDir(String fileDir) {
        setFileDir(fileDir);
    }

    /**
     * set file name
     */
    protected void setFileName(String fileName) {
        setFileName(fileName);
    }


    // getters

    /**
     * get id
     *
     * @return return null means id illegal
     */
    public Integer getId() {
        return mId;
    }

    /**
     * get Downloaded Size
     *
     * @return Downloaded Size
     * @deprecated use {@link #getDownloadedSizeLong} instead
     */
    @Deprecated
    public int getDownloadedSize() {
        return (int) mDownloadedSize;
    }

    /**
     * get Downloaded Size
     *
     * @return Downloaded Size
     */
    public long getDownloadedSizeLong() {
        return mDownloadedSize;
    }

    /**
     * get TempFileName
     *
     * @return TempFileName
     */
    public String getTempFileName() {
        return mTempFileName;
    }

    /**
     * get download status
     *
     * @return download status,ref{@link Status}
     */
    public int getStatus() {
        return mStatus;
    }

    /**
     * get create download datetime
     *
     * @return create download datetime
     */
    public String getCreateDatetime() {
        return mCreateDatetime;
    }

    // other getters

    /**
     * get TempFilePath
     *
     * @return TempFilePath
     */
    public String getTempFilePath() {
        return getFileDir() + File.separator + mTempFileName;
    }
}
