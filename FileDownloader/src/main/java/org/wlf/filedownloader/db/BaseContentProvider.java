package org.wlf.filedownloader.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * 基本的ContentProvider，对ContentProvider进行封装
 * 
 * @author wlf
 * @deprecated 暂时没有用到
 * 
 */
public abstract class BaseContentProvider extends ContentProvider {

	// 注册的内容
	private String mAuthority;// 在AndroidManifest.xml注册的一致

	// 与具体的数据库表相关
	private String mMimeTypeRow;// 例如：vnd.android.cursor.item/book
	private String mMimeTypeRows;// 例如：vnd.android.cursor.dir/books

	// 操作数据库的Dao
	private ContentDbDao mContentDbDaoImpl;

	// ContentProvider匹配规则
	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);// 默认的规则是不匹配的
	private static final int ROW = 1; // 操作方式，单行操作
	private static final int ROWS = 2; // 操作方式，多行操作

	/**
	 * 提供创建ContentProvider的Authority
	 * 
	 * @return Authority
	 */
	protected abstract String onCreateAuthority();

	/**
	 * 提供操作数据库的Dao
	 * 
	 * @return ContentDbDao
	 */
	protected abstract ContentDbDao onCreateContentDbDao();

	/**
	 * 根据authority和tableName创建唯一的ContentProviderUri
	 * 
	 * @param authority
	 * @param tableName
	 * @return ContentProviderUri
	 */
	public static final String createContentProviderUri(String authority, String tableName) {
		return "content://" + authority + "/" + tableName;
	}

	@Override
	public boolean onCreate() {

		mAuthority = onCreateAuthority();
		mContentDbDaoImpl = onCreateContentDbDao();

		String tableName = mContentDbDaoImpl.getTableName();

		mMimeTypeRow = "vnd.android.cursor.item/" + tableName;// 例如：vnd.android.cursor.item/book
		mMimeTypeRows = "vnd.android.cursor.dir/" + tableName + "s";// 例如：vnd.android.cursor.dir/books

		// 使用通配符#，匹配任意数字，表示数据库表的某一行操作
		URI_MATCHER.addURI(mAuthority, tableName + "/#", ROW);
		// 不使用通配符，匹配所有，表示数据库表的多行操作
		URI_MATCHER.addURI(mAuthority, tableName, ROWS);

		return !TextUtils.isEmpty(mAuthority) && !TextUtils.isEmpty(tableName)
				&& mContentDbDaoImpl.getTableIdFieldName() != null;
	}

	@Override
	public String getType(Uri uri) {
		// 当Uri匹配时，在任何地方可以调用ContentResolver.getType(Uri)
		int flag = URI_MATCHER.match(uri);
		switch (flag) {
		case ROW:
			return mMimeTypeRow; // 如果是单行操作，则为vnd.android.cursor.item/book
		case ROWS:
			return mMimeTypeRows; // 如果是多行操作，则为vnd.android.cursor.dir/books
		}
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// 当Uri匹配时，在任何地方可以调用ContentResolver.insert(Uri url, ContentValues
		// values)
		Uri resultUri = null;
		// 解析Uri返回的Code
		int flag = URI_MATCHER.match(uri);
		switch (flag) {
		case ROWS:
			// 调用SQLiteDatabase数据库的访问方法
			long id = mContentDbDaoImpl.insert(values); // 执行插入操作的方法，返回插入当前行的行号
			resultUri = ContentUris.withAppendedId(uri, id);

			Log.e("wlf", "插入数据，id：" + id + "，resultUri：" + resultUri);
			break;
		}
		return resultUri;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// 当Uri匹配时，在任何地方可以调用ContentResolver.delete(Uri, String, String[])
		int count = -1;
		try {
			int flag = URI_MATCHER.match(uri);
			switch (flag) {
			case ROW:
				// 单行操作，使用ContentUris工具类解析出结尾的Id
				long id = ContentUris.parseId(uri);
				String whereValue = mContentDbDaoImpl.getTableIdFieldName() + " = ?";// 例如："_id = ?"
				String[] args = { String.valueOf(id) };
				count = mContentDbDaoImpl.delete(whereValue, args);
				break;
			case ROWS:
				// 多行操作
				count = mContentDbDaoImpl.delete(selection, selectionArgs);
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		Log.e("wlf", "删除数据，删除了：" + count + "行");
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// 当Uri匹配时，在任何地方可以调用ContentResolver.update(Uri, ContentValues, String,
		// String[])
		int count = -1;
		try {
			int flag = URI_MATCHER.match(uri);
			switch (flag) {
			case ROW:
				long id = ContentUris.parseId(uri);
				String whereValue = mContentDbDaoImpl.getTableIdFieldName() + " = ?";// 例如："_id = ?"
				String[] args = { String.valueOf(id) };
				count = mContentDbDaoImpl.update(values, whereValue, args);
				break;
			case ROWS:
				count = mContentDbDaoImpl.update(values, selection, selectionArgs);
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		Log.e("wlf", "更新数据，更新了：" + count + "行");
		return count;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		// 当Uri匹配时，在任何地方可以调用ContentResolver.query(Uri, String[], String,
		// String[], String)
		Cursor cursor = null;
		try {
			int flag = URI_MATCHER.match(uri);
			switch (flag) {
			case ROW:
				long id = ContentUris.parseId(uri);
				String whereValue = mContentDbDaoImpl.getTableIdFieldName() + " = ?";// 例如："_id = ?"
				String[] args = { String.valueOf(id) };
				cursor = mContentDbDaoImpl.query(null, whereValue, args, null);
				break;
			case ROWS:
				cursor = mContentDbDaoImpl.query(projection, selection, selectionArgs, sortOrder);
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (cursor != null) {
			Log.e("wlf", "查询（重新查询）数据，查到了：" + cursor.getCount() + "行");
		}
		return cursor;
	}
}