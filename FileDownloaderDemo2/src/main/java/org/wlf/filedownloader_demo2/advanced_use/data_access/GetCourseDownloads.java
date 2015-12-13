package org.wlf.filedownloader_demo2.advanced_use.data_access;

import android.content.Context;

import com.j256.ormlite.dao.Dao;

import org.wlf.filedownloader_demo2.advanced_use.db.CourseDbHelper;
import org.wlf.filedownloader_demo2.advanced_use.model.CoursePreviewInfo;

import java.sql.SQLException;
import java.util.List;

/**
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class GetCourseDownloads {

    public void getCourseDownloads(Context context, OnGetCourseDownloadsListener onGetCourseDownloadsListener) {

        List<CoursePreviewInfo> coursePreviewInfos = null;

        try {
            Dao<CoursePreviewInfo, Integer> dao = CourseDbHelper.getInstance(context).getDao(CoursePreviewInfo.class);
            coursePreviewInfos = dao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (coursePreviewInfos != null) {

                // init DownloadFiles
                for (CoursePreviewInfo coursePreviewInfo : coursePreviewInfos) {
                    if (coursePreviewInfo == null) {
                        continue;
                    }
                    coursePreviewInfo.init();
                }

                onGetCourseDownloadsListener.onGetCourseDownloadsSucceed(coursePreviewInfos);
            } else {
                onGetCourseDownloadsListener.onGetCourseDownloadsFailed();
            }
        }

    }

    public interface OnGetCourseDownloadsListener {

        void onGetCourseDownloadsSucceed(List<CoursePreviewInfo> coursePreviewInfos);

        void onGetCourseDownloadsFailed();
    }
}
