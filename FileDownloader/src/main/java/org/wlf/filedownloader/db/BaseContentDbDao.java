package org.wlf.filedownloader.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * base dao impl
 * <br/>
 * 基本的ContentDbDao实现封装
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public abstract class BaseContentDbDao implements ContentDbDao, DatabaseCallback {

    /**
     * default id field name
     */
    public static final String DEFAULT_TABLE_ID_FIELD_NAME = "_id";

    private SQLiteOpenHelper mDbHelper;
    private String mTableName;
    private String mTableIdFieldName = DEFAULT_TABLE_ID_FIELD_NAME;

    public BaseContentDbDao(SQLiteOpenHelper dbHelper, String tableName, String tableIdFieldName) {
        super();
        this.mDbHelper = dbHelper;
        this.mTableName = tableName;
        this.mTableIdFieldName = tableIdFieldName;
    }

    @Override
    public long insert(ContentValues values) {
        long id = -1;
        SQLiteDatabase database = null;
        try {
            database = mDbHelper.getWritableDatabase();
            id = database.insert(mTableName, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //            if (database != null) {
            //                database.close();
            //            }
        }
        return id;
    }

    @Override
    public int delete(String selection, String[] selectionArgs) {
        int count = -1;
        SQLiteDatabase database = null;
        try {
            database = mDbHelper.getWritableDatabase();
            count = database.delete(mTableName, selection, selectionArgs);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //            if (database != null) {
            //                database.close();
            //            }
        }
        return count;
    }

    @Override
    public int update(ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase database = null;
        int count = -1;
        try {
            database = mDbHelper.getWritableDatabase();
            count = database.update(mTableName, values, selection, selectionArgs);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //            if (database != null) {
            //                database.close();
            //            }
        }
        return count;
    }

    @Override
    public Cursor query(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = null;
        Cursor cursor = null;
        try {
            database = mDbHelper.getReadableDatabase();
            cursor = database.query(true, mTableName, null, selection, selectionArgs, null, null, sortOrder, null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //            if (database != null) {
            //                database.close();
            //            }
        }
        return cursor;
    }

    @Override
    public String getTableName() {
        return mTableName;
    }

    @Override
    public String getTableIdFieldName() {
        return mTableIdFieldName;
    }
}
