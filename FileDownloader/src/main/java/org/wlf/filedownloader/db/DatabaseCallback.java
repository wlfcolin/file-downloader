package org.wlf.filedownloader.db;

import android.database.sqlite.SQLiteDatabase;

/**
 * database callback
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public interface DatabaseCallback {

    /**
     * the database has been created
     *
     * @param db SQLiteDatabase
     */
    void onCreate(SQLiteDatabase db);

    /**
     * the database has been upgraded
     *
     * @param db         SQLiteDatabase
     * @param oldVersion oldVersion
     * @param newVersion newVersion
     */
    void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
}
