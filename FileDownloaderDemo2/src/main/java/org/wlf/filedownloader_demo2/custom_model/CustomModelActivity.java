package org.wlf.filedownloader_demo2.custom_model;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

import org.wlf.filedownloader.FileDownloader;
import org.wlf.filedownloader_demo2.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo2 Test——CustomModelActivity
 * <br/>
 * 测试2自定义模型界面
 *
 * @author wlf(Andy)
 * @datetime 2015-12-05 10:10 GMT+8
 * @email 411086563@qq.com
 */
public class CustomModelActivity extends Activity {

    // adapter
    private CustomDownloadFileListAdapter mCustomDownloadFileListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.custom_model__activity_custom_model);

        // model data
        List<CustomVideoInfo> customVideoInfos = getModelData();

        // ListView
        ListView lvDownloadFileList = (ListView) findViewById(R.id.lvDownloadFileList);
        mCustomDownloadFileListAdapter = new CustomDownloadFileListAdapter(this, customVideoInfos);
        lvDownloadFileList.setAdapter(mCustomDownloadFileListAdapter);

        // registerDownloadStatusListener 
        FileDownloader.registerDownloadStatusListener(mCustomDownloadFileListAdapter);
    }

    // get model data(may come from server)
    private List<CustomVideoInfo> getModelData() {

        List<CustomVideoInfo> customVideoInfos = new ArrayList<CustomVideoInfo>();

        String url1 = "http://182.254.149.157/ftp/image/shop/product/儿童英语升华&￥.apk";
        CustomVideoInfo customVideoInfo1 = new CustomVideoInfo(1, url1, "2015-10-11 13:20:12", "2015-10-11 14:10:50");
        customVideoInfos.add(customVideoInfo1);

        String url2 = "http://dldir1.qq.com/weixin/android/weixin638android680.apk";
        CustomVideoInfo customVideoInfo2 = new CustomVideoInfo(2, url2, "2015-10-11 13:20:12", "2015-10-11 14:10:50");
        customVideoInfos.add(customVideoInfo2);

        String url3 = "http://dlsw.baidu.com/sw-search-sp/soft/e2/25726/iQIYIMedia_002_4.3.11.1447139777.dmg";
        CustomVideoInfo customVideoInfo3 = new CustomVideoInfo(1, url3, "2015-10-11 13:20:12", "2015-10-11 14:10:50");
        customVideoInfos.add(customVideoInfo3);

        return customVideoInfos;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //        // pause all downloads
        //        FileDownloader.pauseAll();

        // unregisterDownloadStatusListener
        FileDownloader.unregisterDownloadStatusListener(mCustomDownloadFileListAdapter);

        if (mCustomDownloadFileListAdapter != null) {
            mCustomDownloadFileListAdapter.release();
        }
    }
}
