package org.wlf.filedownloader.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import org.wlf.filedownloader.util.CollectionUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * base SQLiteOpenHelper impl
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public abstract class BaseContentDbHelper extends SQLiteOpenHelper {

    /**
     * map of dao
     */
    private Map<String, ContentDbDao> mContentDbDaoMap = new HashMap<String, ContentDbDao>();

    public BaseContentDbHelper(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);
        initContentDbDaoMap();
    }

    /**
     * init dao map
     */
    private void initContentDbDaoMap() {

        List<ContentDbDao> contentDbDaos = new ArrayList<ContentDbDao>();

        // config daos
        onConfigContentDbDaos(contentDbDaos);

        if (CollectionUtil.isEmpty(contentDbDaos)) {
            return;
        }

        for (ContentDbDao contentDbDao : contentDbDaos) {
            if (contentDbDao == null) {
                continue;
            }
            String tableName = contentDbDao.getTableName();

            if (TextUtils.isEmpty(tableName)) {
                continue;
            }

            if (mContentDbDaoMap.containsKey(tableName)) {
                continue;
            }

            // put dao to map
            mContentDbDaoMap.put(tableName, contentDbDao);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        Collection<ContentDbDao> contentDbDaos = mContentDbDaoMap.values();

        if (CollectionUtil.isEmpty(contentDbDaos)) {
            return;
        }

        for (ContentDbDao contentDbDao : contentDbDaos) {
            if (contentDbDao == null) {
                continue;
            }
            // call dao's onCreate
            contentDbDao.onCreate(db);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Collection<ContentDbDao> contentDbDaos = mContentDbDaoMap.values();

        if (CollectionUtil.isEmpty(contentDbDaos)) {
            return;
        }

        for (ContentDbDao contentDbDao : contentDbDaos) {
            if (contentDbDao == null) {
                continue;
            }
            // call dao's onUpgrade
            contentDbDao.onUpgrade(db, oldVersion, newVersion);
        }
    }

    /**
     * config daos
     *
     * @param contentDbDaos current daos
     */
    protected abstract void onConfigContentDbDaos(List<ContentDbDao> contentDbDaos);

    /**
     * get dao by table name
     *
     * @param tableName table name
     * @return the dao for the given table name
     */
    public ContentDbDao getContentDbDao(String tableName) {
        if (!mContentDbDaoMap.containsKey(tableName)) {
            throw new RuntimeException("unregistered database table:" + tableName + " in " + this.getClass()
                    .getSimpleName());
        }
        return mContentDbDaoMap.get(tableName);
    }

}
