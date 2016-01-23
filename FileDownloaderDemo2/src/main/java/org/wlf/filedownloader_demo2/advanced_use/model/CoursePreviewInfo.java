package org.wlf.filedownloader_demo2.advanced_use.model;

import android.text.TextUtils;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.Dao.CreateOrUpdateStatus;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.table.DatabaseTable;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloader;
import org.wlf.filedownloader.listener.OnDownloadFileChangeListener;
import org.wlf.filedownloader_demo2.advanced_use.db.CourseDbHelper;

import java.sql.SQLException;

/**
 * @author wlf(Andy)
 * @datetime 2015-12-11 22:04 GMT+8
 * @email 411086563@qq.com
 */
@DatabaseTable(tableName = "tb_course")
public class CoursePreviewInfo implements OnDownloadFileChangeListener {

    public static final String COLUMN_NAME_OF_FIELD_COURSE_URL = "course_url";

    @DatabaseField(generatedId = true, columnName = "_id")
    private Integer mId;//the id of the table

    @DatabaseField(columnName = "course_id", unique = true, canBeNull = false)
    private String mCourseId;//the id of the course

    @DatabaseField(columnName = COLUMN_NAME_OF_FIELD_COURSE_URL, unique = true, canBeNull = false)
    private String mCourseUrl;//the url of the course

    @DatabaseField(columnName = "course_cover_url")
    private String mCourseCoverUrl;//the cover_url of the course

    @DatabaseField(columnName = "course_name")
    private String mCourseName;//the name of the course

    private DownloadFileInfo mDownloadFileInfo;//DownloadFileInfo
    private CourseDbHelper mCourseDbHelper;//the DbOpenHelper

    private CoursePreviewInfo() {
        init();
    }

    public CoursePreviewInfo(String courseId, String courseUrl, String courseCoverUrl, String courseName,
                             CourseDbHelper courseDbHelper) {
        mCourseId = courseId;
        mCourseUrl = courseUrl;
        mCourseCoverUrl = courseCoverUrl;
        mCourseName = courseName;
        mCourseDbHelper = courseDbHelper;

        init();
    }

    /**
     * init resources
     */
    public void init() {
        // register DownloadFileChangeListener
        FileDownloader.registerDownloadFileChangeListener(this);
        // init DownloadFileInfo if has been downloaded
        if (!TextUtils.isEmpty(mCourseUrl)) {
            mDownloadFileInfo = FileDownloader.getDownloadFile(mCourseUrl);
        }
    }

    /**
     * release resources
     */
    public void release() {
        // unregister
        FileDownloader.unregisterDownloadFileChangeListener(this);
    }

    // getters
    public Integer getId() {
        return mId;
    }

    public String getCourseId() {
        return mCourseId;
    }

    public String getCourseUrl() {
        return mCourseUrl;
    }

    public String getCourseCoverUrl() {
        return mCourseCoverUrl;
    }

    public String getCourseName() {
        return mCourseName;
    }

    public DownloadFileInfo getDownloadFileInfo() {
        return mDownloadFileInfo;
    }

    @Override
    public void onDownloadFileCreated(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo != null && downloadFileInfo.getUrl() != null && downloadFileInfo.getUrl().equals
                (mCourseUrl)) {

            // add this CoursePreviewInfo in database download record
            // 
            // the reason why to save this CoursePreviewInfo in course database is 
            // that when the user enter the CourseDownloadFragment,the fragment need to show the course title to the 
            // user,however the DownloadFileInfo in FileDownloader can not provide the course title,also the fragment 
            // need to know how many course items need to show,so it is depends on the size of the course database, 
            // because FileDownloader is just a tool to download, record and manage all the files which were 
            // downloaded from the internet
            try {
                if (mCourseDbHelper == null) {
                    return;
                }
                Dao<CoursePreviewInfo, Integer> dao = mCourseDbHelper.getDao(CoursePreviewInfo.class);
                CreateOrUpdateStatus status = dao.createOrUpdate(this);
                if (status.isCreated() || status.isUpdated()) {
                    this.mDownloadFileInfo = downloadFileInfo;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onDownloadFileUpdated(DownloadFileInfo downloadFileInfo, Type type) {

        if (downloadFileInfo != null && downloadFileInfo.getUrl() != null && downloadFileInfo.getUrl().equals
                (mCourseUrl)) {
            if (this.mDownloadFileInfo == null) {
                try {
                    if (mCourseDbHelper == null) {
                        return;
                    }
                    UpdateBuilder builder = mCourseDbHelper.getDao(CoursePreviewInfo.class).updateBuilder();
                    builder.where().eq(CoursePreviewInfo.COLUMN_NAME_OF_FIELD_COURSE_URL, downloadFileInfo.getUrl());
                    int result = builder.update();
                    if (result == 1) {
                        this.mDownloadFileInfo = downloadFileInfo;
                    } else {
                        Dao<CoursePreviewInfo, Integer> dao = mCourseDbHelper.getDao(CoursePreviewInfo.class);
                        CreateOrUpdateStatus status = dao.createOrUpdate(this);
                        if (status.isCreated() || status.isUpdated()) {
                            this.mDownloadFileInfo = downloadFileInfo;
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public void onDownloadFileDeleted(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo != null) {
            Log.e("wlf", "onDownloadFileDeleted,downloadFileInfo:" + downloadFileInfo.getUrl());
        } else {
            Log.e("wlf", "onDownloadFileDeleted,downloadFileInfo:" + downloadFileInfo);
        }

        if (downloadFileInfo != null && downloadFileInfo.getUrl() != null && downloadFileInfo.getUrl().equals
                (mCourseUrl)) {
            // delete this course preview in database download record
            try {
                if (mCourseDbHelper == null) {
                    return;
                }
                DeleteBuilder builder = mCourseDbHelper.getDao(CoursePreviewInfo.class).deleteBuilder();
                builder.where().eq(CoursePreviewInfo.COLUMN_NAME_OF_FIELD_COURSE_URL, mCourseUrl);
                int result = builder.delete();
                if (result == 1) {
                    this.mDownloadFileInfo = null;
                    Log.w("wlf", "onDownloadFileDeleted,downloadFileInfo,delete succeed:" + downloadFileInfo.getUrl());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
    }
}
