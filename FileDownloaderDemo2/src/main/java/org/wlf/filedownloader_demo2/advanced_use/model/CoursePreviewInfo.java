package org.wlf.filedownloader_demo2.advanced_use.model;

import android.text.TextUtils;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
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

    @DatabaseField(generatedId = true, columnName = "_id")
    private Integer mId;//the id of the table

    @DatabaseField(columnName = "course_id", unique = true, canBeNull = false)
    private String mCourseId;//the id of the course

    @DatabaseField(columnName = "course_url", unique = true, canBeNull = false)
    private String mCourseUrl;//the url of the course

    @DatabaseField(columnName = "course_name")
    private String mCourseName;//the name of the course

    private DownloadFileInfo mDownloadFileInfo;//DownloadFileInfo
    private CourseDbHelper mCourseDbHelper;//the DbOpenHelper

    private CoursePreviewInfo() {

        init();
    }

    public CoursePreviewInfo(String courseId, String courseUrl, String courseName, CourseDbHelper courseDbHelper) {
        mCourseId = courseId;
        mCourseUrl = courseUrl;
        mCourseName = courseName;
        mCourseDbHelper = courseDbHelper;

        init();
    }

    public void init() {
        // register DownloadFileChangeListener, you may not care about to unregister the reference,because it is a 
        // WeakReference
        FileDownloader.registerDownloadFileChangeListener(this);

        // init DownloadFileInfo if has been downloaded
        if (!TextUtils.isEmpty(mCourseUrl)) {
            mDownloadFileInfo = FileDownloader.getDownloadFileByUrl(mCourseUrl);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        // unregister, optional,not require
        FileDownloader.unregisterDownloadFileChangeListener(this);
        super.finalize();
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
            this.mDownloadFileInfo = downloadFileInfo;

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
                dao.createOrUpdate(this);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onDownloadFileUpdated(DownloadFileInfo downloadFileInfo, Type type) {

        if (downloadFileInfo != null && downloadFileInfo.getUrl() != null && downloadFileInfo.getUrl().equals
                (mCourseUrl)) {
            this.mDownloadFileInfo = downloadFileInfo;

            // for this example there is nothing need to do in database when the DownloadFile updated
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
            this.mDownloadFileInfo = null;

            // delete this course preview in database download record
            try {
                if (mCourseDbHelper == null) {
                    return;
                }
                Dao<CoursePreviewInfo, Integer> dao = mCourseDbHelper.getDao(CoursePreviewInfo.class);
                dao.delete(this);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CoursePreviewInfo) {
            CoursePreviewInfo other = (CoursePreviewInfo) o;
            if (!TextUtils.isEmpty(other.mCourseUrl)) {
                return other.mCourseUrl.equals(mCourseUrl);
            }
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        if (!TextUtils.isEmpty(mCourseUrl)) {
            return mCourseName.hashCode();
        }
        return super.hashCode();
    }
}
