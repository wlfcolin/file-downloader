package org.wlf.filedownloader.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

/**
 * base ContentProvider,use {@link ContentDbDao}
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 * @deprecated not use
 */
@Deprecated
public abstract class BaseContentProvider extends ContentProvider {

    // ContentProvider Authority
    private String mAuthority;// the ContentProvider Authority in AndroidManifest.xml

    // MimeType
    private String mMimeTypeRow;// eg.  vnd.android.cursor.item/book
    private String mMimeTypeRows;// eg. vnd.android.cursor.dir/books

    private ContentDbDao mContentDbDaoImpl;

    // MimeType match rules
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);// no match
    private static final int ROW = 1; // single row
    private static final int ROWS = 2; // multi rows

    /**
     * for child to provider Authority
     *
     * @return Authority
     */
    protected abstract String onCreateAuthority();

    /**
     * for child to provider ContentDbDao
     *
     * @return ContentDbDao
     */
    protected abstract ContentDbDao onCreateContentDbDao();

    /**
     * using authority and tableName to create ContentProvider Uri
     *
     * @param authority
     * @param tableName
     * @return ContentProvider Uri
     */
    private static final String createContentProviderUri(String authority, String tableName) {
        return "content://" + authority + "/" + tableName;
    }

    @Override
    public boolean onCreate() {

        mAuthority = onCreateAuthority();
        mContentDbDaoImpl = onCreateContentDbDao();

        String tableName = mContentDbDaoImpl.getTableName();

        mMimeTypeRow = "vnd.android.cursor.item/" + tableName;// eg.    vnd.android.cursor.item/book
        mMimeTypeRows = "vnd.android.cursor.dir/" + tableName + "s";// eg.  vnd.android.cursor.dir/books

        // use /# for database row match
        URI_MATCHER.addURI(mAuthority, tableName + "/#", ROW);
        // use none for multi database row match
        URI_MATCHER.addURI(mAuthority, tableName, ROWS);

        return !TextUtils.isEmpty(mAuthority) && !TextUtils.isEmpty(tableName) && mContentDbDaoImpl
                .getTableIdFieldName() != null;
    }

    @Override
    public String getType(Uri uri) {
        // get MineType by Uri using ContentResolver.getType(Uri) in any component such as Activity,Service
        int flag = URI_MATCHER.match(uri);
        switch (flag) {
            case ROW:
                return mMimeTypeRow; // single row operation
            case ROWS:
                return mMimeTypeRows; // multi row operation
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // insert by Uri using ContentResolver.insert(Uri url, ContentValues values) in any component such as 
        // Activity,Service
        Uri resultUri = null;
        try {
            int flag = URI_MATCHER.match(uri);
            switch (flag) {
                case ROW://TODO
                case ROWS:
                    long id = mContentDbDaoImpl.insert(values);
                    resultUri = ContentUris.withAppendedId(uri, id);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // delete by Uri using ContentResolver.delete(Uri, String, String[]) in any component such as Activity,Service
        int count = -1;
        try {
            int flag = URI_MATCHER.match(uri);
            switch (flag) {
                case ROW:
                    long id = ContentUris.parseId(uri);
                    String whereValue = mContentDbDaoImpl.getTableIdFieldName() + " = ?";// eg. "_id = ?"
                    String[] args = {String.valueOf(id)};
                    count = mContentDbDaoImpl.delete(whereValue, args);
                    break;
                case ROWS:
                    count = mContentDbDaoImpl.delete(selection, selectionArgs);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // update by Uri using ContentResolver.update(Uri, ContentValues, String, String[]) in any component such as 
        // Activity,Service
        int count = -1;
        try {
            int flag = URI_MATCHER.match(uri);
            switch (flag) {
                case ROW:
                    long id = ContentUris.parseId(uri);
                    String whereValue = mContentDbDaoImpl.getTableIdFieldName() + " = ?";// eg. "_id = ?"
                    String[] args = {String.valueOf(id)};
                    count = mContentDbDaoImpl.update(values, whereValue, args);
                    break;
                case ROWS:
                    count = mContentDbDaoImpl.update(values, selection, selectionArgs);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // update by Uri using ContentResolver.query(Uri, String[], String, String[], String) in any component such 
        // as Activity,Service
        Cursor cursor = null;
        try {
            int flag = URI_MATCHER.match(uri);
            switch (flag) {
                case ROW:
                    long id = ContentUris.parseId(uri);
                    String whereValue = mContentDbDaoImpl.getTableIdFieldName() + " = ?";// eg. "_id = ?"
                    String[] args = {String.valueOf(id)};
                    cursor = mContentDbDaoImpl.query(null, whereValue, args, null);
                    break;
                case ROWS:
                    cursor = mContentDbDaoImpl.query(projection, selection, selectionArgs, sortOrder);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cursor;
    }
}