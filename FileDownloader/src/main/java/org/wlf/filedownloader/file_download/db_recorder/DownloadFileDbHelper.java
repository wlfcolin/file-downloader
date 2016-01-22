package org.wlf.filedownloader.file_download.db_recorder;

import android.content.Context;

import org.wlf.filedownloader.db.BaseContentDbHelper;
import org.wlf.filedownloader.db.ContentDbDao;

import java.util.List;

/**
 * DownloadFile DbHelper
 * <br/>
 * 下载文件数据库操作类
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DownloadFileDbHelper extends BaseContentDbHelper {

    private static final String DB_NAME = "download_file.db";
    private static final int DB_VERSION = 3;

    public DownloadFileDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    protected void onConfigContentDbDaos(List<ContentDbDao> contentDbDaos) {
        DownloadFileDao downloadFileDao = new DownloadFileDao(this);
        // config DownloadFileDao dao
        contentDbDaos.add(downloadFileDao);
    }

}
