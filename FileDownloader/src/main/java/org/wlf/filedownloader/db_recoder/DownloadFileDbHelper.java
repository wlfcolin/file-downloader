package org.wlf.filedownloader.db_recoder;

import java.util.List;

import org.wlf.filedownloader.db.BaseContentDbHelper;
import org.wlf.filedownloader.db.ContentDbDao;

import android.content.Context;

/**
 * 下载文件数据库操作类
 * 
 * @author wlf
 * 
 */
public class DownloadFileDbHelper extends BaseContentDbHelper {

	private static final String DB_NAME = "download_file.db";
	private static final int DB_VERSION = 1;

	public DownloadFileDbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	protected void onConfigContentDbDaos(List<ContentDbDao> contentDbDaos) {

		DownloadFileDao downloadFileDao = new DownloadFileDao(this);
		// 增加操作下载文件的Dao
		contentDbDaos.add(downloadFileDao);
	}

}
