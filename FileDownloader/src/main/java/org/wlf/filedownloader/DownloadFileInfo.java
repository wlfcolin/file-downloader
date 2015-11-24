package org.wlf.filedownloader;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.io.File;

/**
 * download file Model,synchronous with database table
 * <br/>
 * 下载文件信息模型，跟数据库一致
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DownloadFileInfo {

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
         * the sql of create table
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
                    + COLUMN_NAME_OF_FIELD_STATUS + " INTEGER" + ")";//

            return createTableSql;
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
     * file url
     */
    private String mUrl;
    /**
     * downloadedSize
     */
    private int mDownloadedSize;
    /**
     * File Size
     */
    private int mFileSize;
    /**
     * file eTag
     */
    private String mETag;
    /**
     * AcceptRangeType
     */
    private String mAcceptRangeType;
    /**
     * SaveFileDir
     */
    private String mFileDir;
    /**
     * TempFileName
     */
    private String mTempFileName;
    /**
     * SaveFileName
     */
    private String mFileName;
    /**
     * download status，ref{@link Status}
     */
    private int mStatus = Status.DOWNLOAD_STATUS_UNKNOWN;

    @SuppressWarnings("unused")
    private DownloadFileInfo() {
    }

    /**
     * constructor of HttpDownloader,use DetectUrlFileInfo to create
     *
     * @param detectUrlFileInfo DetectUrlFileInfo
     */
    public DownloadFileInfo(DetectUrlFileInfo detectUrlFileInfo) {
        this.mUrl = detectUrlFileInfo.getUrl();
        this.mFileName = detectUrlFileInfo.getFileName();
        this.mFileSize = detectUrlFileInfo.getFileSize();
        this.mAcceptRangeType = detectUrlFileInfo.getAcceptRangeType();
        this.mFileDir = detectUrlFileInfo.getFileDir();
        this.mTempFileName = mFileName + "." + TEMP_FILE_SUFFIX;
        // this.mStatus = Status.DOWNLOAD_STATUS_WAITING;// download status
    }

    /**
     * constructor of HttpDownloader,use {@link Cursor} to create
     *
     * @param cursor database cursor
     */
    public DownloadFileInfo(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            int id = -1;
            String url = null;
            int downloadedSize = 0;
            int fileSize = 0;
            String eTag = null;
            String acceptRangeType = null;
            String fileDir = null;
            String tempFileName = null;
            String fileName = null;
            int status = Status.DOWNLOAD_STATUS_UNKNOWN;

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
                downloadedSize = cursor.getInt(columnIndex);
            }
            columnIndex = cursor.getColumnIndex(Table.COLUMN_NAME_OF_FIELD_FILE_SIZE);
            if (columnIndex != -1) {
                fileSize = cursor.getInt(columnIndex);
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
            } else {
                throw new IllegalArgumentException("id and url illegal!");
            }
        } else {
            throw new NullPointerException("cursor illegal!");
        }
    }

    /**
     * update DownloadFileInfo
     *
     * @param downloadFileInfo DownloadFileInfo
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
    }

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

    // setters

    /**
     * 设置id
     *
     * @param id
     */
    void setId(Integer id) {
        mId = id;
    }

    /**
     * set save file dir
     */
    void setFileDir(String fileDir) {
        this.mFileDir = fileDir;
    }

    /**
     * set file name
     */
    void setFileName(String fileName) {
        this.mFileName = fileName;
    }

    /**
     * set download size
     */
    void setDownloadedSize(int downloadedSize) {
        this.mDownloadedSize = downloadedSize;
    }

    /**
     * set download status
     */
    void setStatus(int status) {
        this.mStatus = status;
    }

    // getters

    /**
     * get Is
     *
     * @return return null means id illegal
     */
    public Integer getId() {
        return mId;
    }

    /**
     * get file url
     *
     * @return file url
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     * get Downloaded Size
     *
     * @return Downloaded Size
     */
    public int getDownloadedSize() {
        return mDownloadedSize;
    }

    /**
     * get file size
     *
     * @return file size
     */
    public int getFileSize() {
        return mFileSize;
    }

    /**
     * get file ETag
     *
     * @return file ETag
     */
    public String getETag() {
        return mETag;
    }

    /**
     * get AcceptRangeType
     *
     * @return AcceptRangeType
     */
    public String getAcceptRangeType() {
        return mAcceptRangeType;
    }

    /**
     * get SaveFileDir
     *
     * @return SaveFileDir
     */
    public String getFileDir() {
        return mFileDir;
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
     * get SaveFileName
     *
     * @return SaveFileName
     */
    public String getFileName() {
        return mFileName;
    }

    /**
     * get download status
     *
     * @return download status,ref{@link Status}
     */
    public int getStatus() {
        return mStatus;
    }

    // other getter

    /**
     * get TempFilePath
     *
     * @return TempFilePath
     */
    public String getTempFilePath() {
        return getFileDir() + File.separator + mTempFileName;
    }

    /**
     * get FilePath
     *
     * @return FilePath
     */
    public String getFilePath() {
        //        return getFileDir() + File.separator + mFileName;
        return getFilePath(false);
    }

    /**
     * get FilePath
     *
     * @param includeTempFilePath whether return the TempFilePath if Necessary
     * @return
     */
    public String getFilePath(boolean includeTempFilePath) {

        String filePath = getFileDir() + File.separator + mFileName;

        if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
            if (includeTempFilePath) {
                filePath = getTempFilePath();
            }
        }

        return filePath;
    }

}
