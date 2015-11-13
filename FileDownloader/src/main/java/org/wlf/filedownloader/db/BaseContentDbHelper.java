package org.wlf.filedownloader.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

/**
 * 支持ContentProvider操作数据库SQLiteOpenHelper
 * 
 * @author wlf
 * 
 */
public abstract class BaseContentDbHelper extends SQLiteOpenHelper {

	/** 所有Dao集合 */
	private Map<String, ContentDbDao> mContentDbDaoMap = new HashMap<String, ContentDbDao>();

	public BaseContentDbHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
		// 初始化Dao集合
		initContentDbDaoMap();
	}

	// 初始化Dao集合
	private void initContentDbDaoMap() {

		List<ContentDbDao> contentDbDaos = new ArrayList<ContentDbDao>();

		// 配置contentDbDaos
		onConfigContentDbDaos(contentDbDaos);

		if (contentDbDaos != null && !contentDbDaos.isEmpty()) {
			for (ContentDbDao contentDbDao : contentDbDaos) {
				if (contentDbDao == null) {
					continue;
				}
				String tableName = contentDbDao.getTableName();
				if (!TextUtils.isEmpty(tableName)) {
					if (!mContentDbDaoMap.containsKey(tableName)) {
						if (contentDbDao != null) {
							// 添加table跟Dao映射
							mContentDbDaoMap.put(tableName, contentDbDao);
						}
					}

				}
			}
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		Collection<ContentDbDao> contentDbDaos = mContentDbDaoMap.values();
		if (contentDbDaos != null && !contentDbDaos.isEmpty()) {
			for (ContentDbDao contentDbDao : contentDbDaos) {
				if (contentDbDao == null) {
					continue;
				}
				// 调用dao的onCreate
				contentDbDao.onCreate(db);
			}
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		Collection<ContentDbDao> contentDbDaos = mContentDbDaoMap.values();
		if (contentDbDaos != null && !contentDbDaos.isEmpty()) {
			for (ContentDbDao contentDbDao : contentDbDaos) {
				if (contentDbDao == null) {
					continue;
				}
				// 调用onUpgrade
				contentDbDao.onUpgrade(db, oldVersion, newVersion);
			}
		}
	}

	/**
	 * 配置操作数据的表的所有Dao集合
	 * 
	 * @param contentDbDaos
	 */
	protected abstract void onConfigContentDbDaos(List<ContentDbDao> contentDbDaos);

	/**
	 * 通过tableName获取操作的Dao
	 * 
	 * @param tableName
	 *            表名
	 * @return 操作对应表的Dao
	 */
	public ContentDbDao getContentDbDao(String tableName) {
		if (!mContentDbDaoMap.containsKey(tableName)) {
			throw new RuntimeException("未注册数据库表：" + tableName);
		}
		return mContentDbDaoMap.get(tableName);
	}

}
