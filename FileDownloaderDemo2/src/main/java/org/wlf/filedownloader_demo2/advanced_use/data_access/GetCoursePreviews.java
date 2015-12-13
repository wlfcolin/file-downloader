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

            String[] coverUrls = new String[]{
                    //
                    "http://e.hiphotos.baidu.com/zhidao/pic/item/3801213fb80e7becf012edb72f2eb9389a506b98.jpg",
                    //
                    "http://e.hiphotos.baidu.com/zhidao/pic/item/f2deb48f8c5494eeac6d0f3a2ef5e0fe99257e41.jpg",
                    //
                    "http://a.hiphotos.baidu.com/zhidao/pic/item/8ad4b31c8701a18b9f38ddb99c2f07082938fe71.jpg",
                    //
                    "http://f.hiphotos.bdimg.com/album/pic/item/0e2442a7d933c8959c5136eed31373f082020073.jpg",
                    //
                    "http://d.hiphotos.baidu.com/zhidao/pic/item/faf2b2119313b07e6959a5560fd7912396dd8c8c.jpg",
                    //
                    "http://a.hiphotos.baidu.com/zhidao/pic/item/faedab64034f78f077b4dbb07b310a55b3191c31.jpg",
                    //
                    "http://d.hiphotos.baidu.com/zhidao/pic/item/f603918fa0ec08fa2a8d93df59ee3d6d55fbda3d.jpg",
                    //
                    "http://a.hiphotos.baidu.com/zhidao/pic/item/a686c9177f3e6709b9ec3d393bc79f3df9dc5503.jpg",
                    //
                    "http://a.hiphotos.baidu.com/zhidao/pic/item/c8177f3e6709c93df8710edd9e3df8dcd100541c.jpg",
                    //
                    "http://f.hiphotos.baidu.com/zhidao/pic/item/fd039245d688d43ff2eafcc07f1ed21b0ef43b44.jpg"

            };

            for (int i = 0; i < courseUrls.length; i++) {
                String courseName = courseUrls[i].substring(courseUrls[i].lastIndexOf("/") + 1, courseUrls[i].length());
                CoursePreviewInfo coursePreviewInfo = new CoursePreviewInfo("C" + i, courseUrls[i], coverUrls[i], 
                        courseName, CourseDbHelper.getInstance(context));
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
