package org.wlf.filedownloader;

import java.io.File;

import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader.util.UrlUtil;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

/**
 * 下载文件信息模型，跟数据库一致
 * 
 * @author wlf
 * 
 */
public class DownloadFileInfo {

	/** {@link DownloadFileInfo}数据库表相关信息 */
	public static final class Table {

		/** 表DownloadFileInfo在数据库的名字 */
		public static final String TABLE_NAME_OF_DOWNLOAD_FILE = "tb_download_file";

		/** 字段id在数据库的名字 */
		public static final String COLUMN_NAME_OF_FIELD_ID = "_id";
		/** 字段url在数据库的名字 */
		public static final String COLUMN_NAME_OF_FIELD_URL = "url";
		/** 字段downloadedSize在数据库的名字 */
		public static final String COLUMN_NAME_OF_FIELD_DOWNLOADED_SIZE = "downloaded_size";
		/** 字段fileSize在数据库的名字 */
		public static final String COLUMN_NAME_OF_FIELD_FILE_SIZE = "file_size";
		/** 字段eTag在数据库的名字 */
		public static final String COLUMN_NAME_OF_FIELD_E_TAG = "e_tag";
		/** 字段acceptRangeType在数据库的名字 */
		public static final String COLUMN_NAME_OF_FIELD_ACCEPT_RANGE_TYPE = "accept_range_type";
		/** 字段fileSize在数据库的名字 */
		public static final String COLUMN_NAME_OF_FIELD_FILE_DIR = "file_dir";
		/** 字段tempFileName在数据库的名字 */
		public static final String COLUMN_NAME_OF_FIELD_TEMP_FILE_NAME = "temp_file_name";
		/** 字段fileName在数据库的名字 */
		public static final String COLUMN_NAME_OF_FIELD_FILE_NAME = "file_name";
		/** 字段status在数据库的名字 */
		public static final String COLUMN_NAME_OF_FIELD_STATUS = "status";

		/** 获取创建表SQL语句 */
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

	/** 临时文件后缀名 */
	private static final String TEMP_FILE_SUFFIX = "temp";

	/** 对应数据库id */
	private Integer mId;
	/** 下载路径 */
	private String mUrl;
	/** 已下载大小 */
	private int mDownloadedSize;
	/** 文件大小 */
	private int mFileSize;
	/** 文件对应的eTag */
	private String mETag;
	/** 接收的范围类型 */
	private String mAcceptRangeType;
	/** 文件保存目录 */
	private String mFileDir;
	/** 临时文件保存名称，跟最终保存的文件在同一个目录 */
	private String mTempFileName;
	/** 文件保存名称 */
	private String mFileName;
	/** 状态，参考{@link Status} */
	private int mStatus = Status.DOWNLOAD_STATUS_UNKNOWN;

	// 某些情况下给反射调用
	@SuppressWarnings("unused")
	private DownloadFileInfo() {
	}

	/**
	 * 构造下载文件信息，需要网络文件信息
	 * 
	 * @param detectUrlFileInfo
	 *            网络文件信息
	 */
	public DownloadFileInfo(DetectUrlFileInfo detectUrlFileInfo) {
		this.mUrl = detectUrlFileInfo.getUrl();
		this.mFileName = detectUrlFileInfo.getFileName();
		this.mFileSize = detectUrlFileInfo.getFileSize();
		this.mAcceptRangeType = detectUrlFileInfo.getAcceptRangeType();
		this.mFileDir = detectUrlFileInfo.getFileDir();
		this.mTempFileName = mFileName + "." + TEMP_FILE_SUFFIX;
		// this.mStatus = Status.DOWNLOAD_STATUS_WAITING;// 刚创建为等待状态
	}

	// 用于从数据库构造对象
	/**
	 * 使用{@link Cursor}构建对象
	 * 
	 * @param cursor
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
				// 初始化当前类的字段
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
				throw new IllegalArgumentException("请传入正确的id和url！");
			}
		} else {
			throw new NullPointerException("cursor不可用！");
		}
	}

	// 限制包内可访问
	/**
	 * 更新多个字段值
	 * 
	 * @param downloadFileInfo
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

	/** 获取字段对应的ContentValues */
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

	// setters，全部限制包内可访问
	/** 设置文件保存目录 */
	void setFileDir(String fileDir) {
		this.mFileDir = fileDir;
	}

	/** 设置文件保存名称 */
	void setFileName(String fileName) {
		this.mFileName = fileName;
	}

	/** 设置已下载大小 */
	void setDownloadedSize(int downloadedSize) {
		this.mDownloadedSize = downloadedSize;
	}

	/** 设置状态 */
	void setStatus(int status) {
		this.mStatus = status;
	}

	// getters
	/**
	 * 获取数据库Id
	 * 
	 * @return 若返回null则未能正确获取
	 */
	public Integer getId() {
		return mId;
	}

	/**
	 * 获取下载路径
	 * 
	 * @return 下载路径
	 */
	public String getUrl() {
		return mUrl;
	}

	/**
	 * 获取已下载大小
	 * 
	 * @return 已下载大小
	 */
	public int getDownloadedSize() {
		return mDownloadedSize;
	}

	/**
	 * 获取文件大小
	 * 
	 * @return 文件大小
	 */
	public int getFileSize() {
		return mFileSize;
	}

	/**
	 * 获取文件对应的eTag
	 * 
	 * @return 文件对应的eTag
	 */
	public String geteTag() {
		return mETag;
	}

	/**
	 * 获取接收的范围类型
	 * 
	 * @return 接收的范围类型
	 */
	public String getAcceptRangeType() {
		return mAcceptRangeType;
	}

	/**
	 * 获取文件保存目录
	 * 
	 * @return 文件保存目录
	 */
	public String getFileDir() {
		return mFileDir;
	}

	/**
	 * 获取临时文件保存名称
	 * 
	 * @return 临时文件保存名称
	 */
	public String getTempFileName() {
		return mTempFileName;
	}

	/**
	 * 获取文件保存名称
	 * 
	 * @return 文件保存名称
	 */
	public String getFileName() {
		return mFileName;
	}

	/**
	 * 获取状态
	 * 
	 * @return 状态，参考{@link Status}
	 */
	public int getStatus() {
		return mStatus;
	}

	// 特殊getter
	/**
	 * 获取临时下载文件路径
	 * 
	 * @return 临时下载文件路径
	 */
	public String getTempFilePath() {
		return getFileDir() + File.separator + mTempFileName;
	}

	/**
	 * 获取下载文件路径
	 * 
	 * @return 下载文件路径
	 */
	public String getFilePath() {
		return getFileDir() + File.separator + mFileName;
	}

}
