package org.wlf.filedownloader.db;

import android.database.sqlite.SQLiteDatabase;

/**
 * 数据库回调
 * 
 * @author wlf
 * 
 */
public interface DatabaseCallback {

	/**
	 * 数据库创建
	 * 
	 * @param db
	 */
	void onCreate(SQLiteDatabase db);

	/**
	 * 数据库更新
	 * 
	 * @param db
	 * @param oldVersion
	 * @param newVersion
	 */
	void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
}
