package org.wlf.filedownloader.file_download.db_recorder;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.wlf.filedownloader.DownloadFileInfo;
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
        // 1 to 2
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL(DownloadFileInfo.Table.getUpdateTableVersion1To2Sql());
        }
    }

}
