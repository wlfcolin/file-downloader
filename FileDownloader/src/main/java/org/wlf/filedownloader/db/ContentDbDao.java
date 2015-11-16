package org.wlf.filedownloader.db;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * the base dao that can use for ContentProvider and SQLiteOpenHelper
 * <br/>
 * 可以通过ContentProvider和SQLiteOpenHelper操作数据库的Dao接口
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public interface ContentDbDao extends DatabaseCallback {

    // CRUD

    /**
     * insert
     *
     * @param values A set of column_name/value pairs to add to the database.
     * @return -1 means failed,otherwise return the row id
     */
    long insert(ContentValues values);

    /**
     * delete
     *
     * @param selection     An optional restriction to apply to rows when deleting.
     * @param selectionArgs You may include ?s in selection, which will be replaced by
     *                      the values from selectionArgs, in order that they appear in the selection.
     *                      The values will be bound as Strings.
     * @return -1 means failed,The number of rows affected.
     */
    int delete(String selection, String[] selectionArgs);

    /**
     * update
     *
     * @param values        A Bundle mapping from column names to new column values (NULL is a valid value).
     * @param selection     An optional filter to match rows to update.
     * @param selectionArgs You may include ?s in selection, which will be replaced by
     *                      the values from selectionArgs, in order that they appear in the selection.
     *                      The values will be bound as Strings.
     * @return -1 means failed,the number of rows affected.
     */
    int update(ContentValues values, String selection, String[] selectionArgs);

    /**
     * query
     *
     * @param projection    The list of columns to put into the cursor. If
     *                      null all columns are included.
     * @param selection     A selection criteria to apply when filtering rows.
     *                      If null then all rows are included.
     * @param selectionArgs You may include ?s in selection, which will be replaced by
     *                      the values from selectionArgs, in order that they appear in the selection.
     *                      The values will be bound as Strings.
     * @param sortOrder     How the rows in the cursor should be sorted.
     *                      If null then the provider is free to define the sort order.
     * @return a Cursor or null.
     */
    Cursor query(String[] projection, String selection, String[] selectionArgs, String sortOrder);

    // other

    /**
     * get table name
     *
     * @return table name
     */
    String getTableName();

    /**
     * get id field name
     *
     * @return id field name
     */
    String getTableIdFieldName();
}
