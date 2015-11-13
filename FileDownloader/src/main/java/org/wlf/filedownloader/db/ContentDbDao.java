package org.wlf.filedownloader.db;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * 通过ContentProvider操作数据库的Dao接口
 * 
 * @author wlf
 * 
 */
public interface ContentDbDao extends DatabaseCallback {

	// 增删改查
	/**
	 * 插入
	 * 
	 * @param values
	 * @return 返回-1表示插入失败，否则返回插入的行id
	 */
	long insert(ContentValues values);

	/**
	 * 删除
	 * 
	 * @param selection
	 * @param selectionArgs
	 * @return 返回删除的行数
	 */
	int delete(String selection, String[] selectionArgs);

	/**
	 * 更新
	 * 
	 * @param values
	 * @param selection
	 * @param selectionArgs
	 * @return 返回更新的行数
	 */
	int update(ContentValues values, String selection, String[] selectionArgs);

	/**
	 * 查询
	 * 
	 * @param projection
	 * @param selection
	 * @param selectionArgs
	 * @param sortOrder
	 * @return 结果游标集
	 */
	Cursor query(String[] projection, String selection, String[] selectionArgs, String sortOrder);

	// 其它
	/**
	 * 获取表名
	 * 
	 * @return 表名
	 */
	String getTableName();

	/**
	 * 获取表id字段名
	 * 
	 * @return
	 */
	String getTableIdFieldName();
}
