package org.wlf.filedownloader.file_download.db_recorder;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.db.BaseContentDbDao;

/**
 * DownloadFileDao
 * <br/>
 * 操作下载文件的Dao
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DownloadFileDao extends BaseContentDbDao {

    private static final String TAG = DownloadFileDao.class.getSimpleName();

    public DownloadFileDao(SQLiteOpenHelper dbHelper) {
        super(dbHelper, DownloadFileInfo.Table.TABLE_NAME_OF_DOWNLOAD_FILE, DownloadFileInfo.Table
                .COLUMN_NAME_OF_FIELD_ID);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // create table of DownloadFile
        db.execSQL(DownloadFileInfo.Table.getCreateTableSql());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Log.i(TAG, TAG + ".onUpgrade，oldVersion：" + oldVersion + "，oldVersion：" + newVersion);

        // upgrade to version 2
        if (newVersion == 2) {
            switch (oldVersion) {
                case 1:
                    // version 1 to 2
                    updateVersion1To2(db);
                    break;
            }
        }
        // upgrade to version 3
        else if (newVersion == 3) {
            switch (oldVersion) {
                case 1:
                    // version 1 to 3
                    updateVersion1To3(db);
                    break;
                case 2:
                    // version 2 to 3
                    updateVersion2To3(db);
                    break;
            }
        }
        // upgrade to version 4

    }

    // version 1 to 2
    private void updateVersion1To2(SQLiteDatabase db) {
        db.execSQL(DownloadFileInfo.Table.getUpdateTableVersion1To2Sql());
    }

    // version 2 to 3
    private void updateVersion2To3(SQLiteDatabase db) {
        db.execSQL(DownloadFileInfo.Table.getUpdateTableVersion2To3Sql());
    }

    // version 1 to 3
    private void updateVersion1To3(SQLiteDatabase db) {
        db.execSQL(DownloadFileInfo.Table.getUpdateTableVersion1To2Sql());
        db.execSQL(DownloadFileInfo.Table.getUpdateTableVersion2To3Sql());
    }

}
