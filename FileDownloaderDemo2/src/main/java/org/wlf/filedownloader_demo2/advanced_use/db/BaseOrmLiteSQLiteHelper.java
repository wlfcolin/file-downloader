package org.wlf.filedownloader_demo2.advanced_use.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * base database helper class
 * <br/>
 * 基本的数据库帮助类，使用了OrmLite框架进行管理
 *
 * @author wlf(Andy)
 * @datetime 2015-11-19 09:23 GMT+8
 * @email 411086563@qq.com
 */
public abstract class BaseOrmLiteSQLiteHelper extends OrmLiteSqliteOpenHelper {

    // 支持的数据库表，在创建的时候确定
    private List<Class<?>> supportTables = new ArrayList<Class<?>>();

    public BaseOrmLiteSQLiteHelper(Context context, String databaseName, CursorFactory factory, int databaseVersion) {
        super(context, databaseName, factory, databaseVersion);
        onConfigTables(supportTables);
    }

    /**
     * 子类配置数据库表，子类实现
     *
     * @param supportTables
     */
    protected abstract void onConfigTables(List<Class<?>> supportTables);

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        // 通过TableUtils这个类新建Module类对应的表
        try {
            // 通过TableUtils类创建所有的表
            for (Class<?> table : supportTables) {
                TableUtils.createTableIfNotExists(connectionSource, table);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public <D extends Dao<T, ?>, T> D getDao(Class<T> clazz) throws SQLException {
        for (Class<?> table : supportTables) {
            if (clazz == table) {
                return super.getDao(clazz);
            }
        }
        throw new SQLException("不支持的数据库表，请在" + this.getClass().getSimpleName() + "中配置通过OrmLite映射过的java bean class：" +
                clazz.getSimpleName());
    }

    /**
     * 关闭并释放资源。包括释放单一实例自己。
     * <p/>
     * <li>
     * 应用退出时务必手动调用进行关闭
     */
    @Override
    public void close() {
        super.close();
        supportTables.clear();
    }
}
