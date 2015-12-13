package org.wlf.filedownloader_demo2.advanced_use.data_access;

import android.content.Context;

import org.wlf.filedownloader_demo2.advanced_use.db.CourseDbHelper;
import org.wlf.filedownloader_demo2.advanced_use.model.CoursePreviewInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class GetCoursePreviews {

    public void getCoursePreviews(Context context, OnGetCoursePreviewsListener onGetCoursePreviewsListener) {

        // getFromCode
        List<CoursePreviewInfo> coursePreviewInfos = getFromCode(context);
        if (onGetCoursePreviewsListener != null) {
            if (coursePreviewInfos != null) {
                onGetCoursePreviewsListener.onGetCoursePreviewsSucceed(coursePreviewInfos);
            } else {
                onGetCoursePreviewsListener.onGetCoursePreviewsFailed();
            }
        }

        //getFromInternet

    }

    private List<CoursePreviewInfo> getFromCode(Context context) {

        try {

            List<CoursePreviewInfo> coursePreviewInfos = new ArrayList<CoursePreviewInfo>();

            String[] courseUrls = new String[]{
                    //
                    "http://sqdd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk",
                    //
                    "http://down.sandai.net/thunder7/Thunder_dl_7.9.41.5020.exe",
                    //
                    "http://mp4.28mtv.com/mp41/1862-刘德华-余生一起过[68mtv.com].mp4",
                    //
                    "http://182.254.149.157/ftp/image/shop/product/儿童英语拓展篇HD_air.com.congcongbb.yingyue.mi_1000000.apk",
                    //
                    "http://dlsw.baidu.com/sw-search-sp/soft/c6/25790/WeChatzhCN1.0.0.6.1428545414.dmg",
                    //
                    "http://dlsw.baidu.com/sw-search-sp/soft/0c/25762/KugouMusicForMac.1395978517.dmg",
                    //
                    "http://dlsw.baidu.com/sw-search-sp/soft/40/12856/QIYImedia_1_06_4.2.1.7.1446201936.exe",
                    //
                    "http://dlsw.baidu.com/sw-search-sp/soft/2a/25677/QQ_V4.0.5.1446465388.dmg",
                    //
                    "http://dlsw.baidu.com/sw-search-sp/soft/3a/12350/QQ_7.9.16638.0_setup.1449542695.exe",
                    //
                    "http://dlsw.baidu.com/sw-search-sp/soft/51/11843/Firefox_42.0.0.5780_setup.1446619646.exe"
                    
            };

            for (int i = 0; i < courseUrls.length; i++) {
                String courseName = courseUrls[i].substring(courseUrls[i].lastIndexOf("/") + 1, courseUrls[i].length());
                CoursePreviewInfo coursePreviewInfo = new CoursePreviewInfo("C" + i, courseUrls[i], courseName, 
                        CourseDbHelper.getInstance(context));
                coursePreviewInfos.add(coursePreviewInfo);
            }

            return coursePreviewInfos;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public interface OnGetCoursePreviewsListener {

        void onGetCoursePreviewsSucceed(List<CoursePreviewInfo> coursePreviewInfos);

        void onGetCoursePreviewsFailed();
    }
}
