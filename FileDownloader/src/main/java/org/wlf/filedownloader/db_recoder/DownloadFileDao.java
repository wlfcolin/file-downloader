package org.wlf.filedownloader.db_recoder;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.db.BaseContentDbDao;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 操作下载文件的Dao
 * 
 * @author wlf
 * 
 */
public class DownloadFileDao extends BaseContentDbDao {

	public DownloadFileDao(SQLiteOpenHelper dbHelper) {
		super(dbHelper, DownloadFileInfo.Table.TABLE_NAME_OF_DOWNLOAD_FILE, DEFAULT_TABLE_ID_FIELD_NAME);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// 创建表
		db.execSQL(DownloadFileInfo.Table.getCreateTableSql());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// 暂时没有需要实现的
	}

}
