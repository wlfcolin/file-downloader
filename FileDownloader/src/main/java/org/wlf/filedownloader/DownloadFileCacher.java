package org.wlf.filedownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.wlf.filedownloader.base.FailException;
import org.wlf.filedownloader.db.ContentDbDao;
import org.wlf.filedownloader.db_recoder.DbRecorder;
import org.wlf.filedownloader.db_recoder.DownloadFileDbHelper;
import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader.util.UrlUtil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

/**
 * 下载文件缓存器
 * 
 * @author wlf
 * 
 */
public class DownloadFileCacher extends DbRecorder {

	private DownloadFileDbHelper mDbHelper;// 数据库操作帮助类

	private Map<String, DownloadFileInfo> mDownloadFileInfoMap = new HashMap<String, DownloadFileInfo>();// 下载文件信息（内存缓存）

	private Object mModifyLock = new Object();// 修改锁

	// 限制包内可访问
	DownloadFileCacher(Context context) {
		mDbHelper = new DownloadFileDbHelper(context);
		// 从数据库把已有下载数据读取出来
		initDownloadFileInfosFromDb();
	}

	/**
	 * 添加下载文件
	 * 
	 * @param downloadFileInfo
	 * @return
	 */
	public boolean addDownloadFile(DownloadFileInfo downloadFileInfo) {

		if (downloadFileInfo == null) {
			return false;
		}

		ContentDbDao dao = mDbHelper.getContentDbDao(DownloadFileInfo.Table.TABLE_NAME_OF_DOWNLOAD_FILE);
		if (dao == null) {
			return false;
		}

		String url = downloadFileInfo.getUrl();

		ContentValues values = downloadFileInfo.getContentValues();
		if (values != null && values.size() > 0) {
			synchronized (mModifyLock) {// 同步
				long result = dao.insert(values);
				if (result != -1 && result > 0) {
					// 成功，同步内存上的数据
					mDownloadFileInfoMap.put(url, downloadFileInfo);
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * 更新下载文件
	 * 
	 * @param downloadFileInfo
	 * @return
	 */
	public boolean updateDownloadFile(DownloadFileInfo downloadFileInfo) {

		if (downloadFileInfo == null) {
			return false;
		}

		ContentDbDao dao = mDbHelper.getContentDbDao(DownloadFileInfo.Table.TABLE_NAME_OF_DOWNLOAD_FILE);
		if (dao == null) {
			return false;
		}

		String url = downloadFileInfo.getUrl();

		ContentValues values = downloadFileInfo.getContentValues();
		if (values != null && values.size() > 0) {
			synchronized (mModifyLock) {// 同步
				int result = dao.update(values, DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_ID + "= ?",
						new String[] { downloadFileInfo.getId() + "" });
				if (result == 1) {// 成功，同步内存上的数据
					if (mDownloadFileInfoMap.containsKey(url)) {
						DownloadFileInfo downloadFileInfoInMap = mDownloadFileInfoMap.get(url);
						// 更新
						downloadFileInfoInMap.update(downloadFileInfo);
					} else {
						// 新增
						mDownloadFileInfoMap.put(url, downloadFileInfo);
					}
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 删除下载文件
	 * 
	 * @param downloadFileInfo
	 * @return
	 */
	public boolean deleteDownloadFile(DownloadFileInfo downloadFileInfo) {

		if (downloadFileInfo == null) {
			return false;
		}

		ContentDbDao dao = mDbHelper.getContentDbDao(DownloadFileInfo.Table.TABLE_NAME_OF_DOWNLOAD_FILE);
		if (dao == null) {
			return false;
		}

		String url = downloadFileInfo.getUrl();

		synchronized (mModifyLock) {// 同步
			int result = dao.delete(DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_ID + "= ?",
					new String[] { downloadFileInfo.getId() + "" });
			if (result > 0) {
				// 清除缓存
				mDownloadFileInfoMap.remove(url);
				// 成功
				return true;
			}
		}
		return false;
	}

	/**
	 * 根据url获取下载文件
	 * 
	 * @param url
	 * @return
	 */
	public DownloadFileInfo getDownloadFile(String url) {

		if (mDownloadFileInfoMap.containsKey(url) && mDownloadFileInfoMap.get(url) != null) {

			return mDownloadFileInfoMap.get(url);// 内存中有

		} else {

			ContentDbDao dao = mDbHelper.getContentDbDao(DownloadFileInfo.Table.TABLE_NAME_OF_DOWNLOAD_FILE);
			if (dao == null) {
				return null;
			}

			Cursor cursor = dao.query(null, DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_URL + "= ?",
					new String[] { url }, null);

			DownloadFileInfo downloadFileInfo = null;

			if (cursor != null && cursor.moveToFirst()) {
				downloadFileInfo = new DownloadFileInfo(cursor);
			}

			// 关闭cursor游标
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}

			if (downloadFileInfo != null) {
				String downloadUrl = downloadFileInfo.getUrl();
				if (UrlUtil.isUrl(downloadUrl)) {
					synchronized (mModifyLock) {// 同步
						// 缓存
						mDownloadFileInfoMap.put(downloadUrl, downloadFileInfo);
					}
				}
			}

			return mDownloadFileInfoMap.get(url);
		}
	}

	/**
	 * 根据保存的路径获取下载文件
	 * 
	 * @param savePath
	 * @return
	 */
	DownloadFileInfo getDownloadFileBySavePath(String savePath) {

		if (!FileUtil.isFilePath(savePath)) {
			return null;
		}

		DownloadFileInfo downloadFileInfo = null;

		// 从内存中寻找
		Set<Entry<String, DownloadFileInfo>> set = mDownloadFileInfoMap.entrySet();
		Iterator<Entry<String, DownloadFileInfo>> iterator = set.iterator();
		while (iterator.hasNext()) {
			Entry<String, DownloadFileInfo> entry = iterator.next();
			if (entry == null) {
				continue;
			}
			DownloadFileInfo info = entry.getValue();
			if (info == null) {
				continue;
			}
			String filePath = info.getFilePath();
			if (TextUtils.isEmpty(filePath)) {
				continue;
			}

			if (filePath.equals(savePath)) {
				downloadFileInfo = info;
				break;
			}
		}

		// 找到
		if (downloadFileInfo != null) {
			return downloadFileInfo;
		} else {
			ContentDbDao dao = mDbHelper.getContentDbDao(DownloadFileInfo.Table.TABLE_NAME_OF_DOWNLOAD_FILE);
			if (dao == null) {
				return null;
			}

			// 根据保存目录和保存名称找到保存的下载文件
			int separatorIndex = savePath.lastIndexOf(File.separator);
			if (separatorIndex != -1) {
				String fileSaveDir = savePath.substring(0, separatorIndex);
				String fileSaveName = savePath.substring(separatorIndex, savePath.length());
				Cursor cursor = dao.query(null, DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_FILE_DIR + "= ? AND "
						+ DownloadFileInfo.Table.COLUMN_NAME_OF_FIELD_FILE_NAME + "= ?", new String[] { fileSaveDir,
						fileSaveName }, null);

				DownloadFileInfo info = null;

				if (cursor != null && cursor.moveToFirst()) {
					info = new DownloadFileInfo(cursor);
				}

				// 关闭cursor游标
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}

				if (info != null) {
					String url = info.getUrl();
					if (UrlUtil.isUrl(url)) {
						synchronized (mModifyLock) {// 同步
							// 缓存
							mDownloadFileInfoMap.put(url, info);
						}
					}
					return mDownloadFileInfoMap.get(url);
				}
			}
		}
		return null;
	}

	/**
	 * 获取所有下载文件
	 * 
	 * @return
	 */
	public List<DownloadFileInfo> getDownloadFiles() {
		// 内存中没有，则先从数据库读取
		if (mDownloadFileInfoMap == null || mDownloadFileInfoMap.isEmpty()) {
			// 从数据库中初始化到内存
			initDownloadFileInfosFromDb();
		}
		// 从内存中提取下载文件
		if (mDownloadFileInfoMap != null && !mDownloadFileInfoMap.isEmpty()) {
			Collection<DownloadFileInfo> values = mDownloadFileInfoMap.values();
			return new ArrayList<DownloadFileInfo>(values);
		}
		return new ArrayList<DownloadFileInfo>();
	}

	/**
	 * 从数据库读取已下载文件填充到内存
	 */
	private void initDownloadFileInfosFromDb() {
		// 从数据库读取已下载文件
		ContentDbDao dao = mDbHelper.getContentDbDao(DownloadFileInfo.Table.TABLE_NAME_OF_DOWNLOAD_FILE);
		if (dao == null) {
			return;
		}
		Cursor cursor = dao.query(null, null, null, null);
		List<DownloadFileInfo> downloadFileInfos = getDownloadFileInfosFromCursor(cursor);
		// 关闭cursor游标
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		if (downloadFileInfos != null && !downloadFileInfos.isEmpty()) {
			// 填充到内存中
			for (DownloadFileInfo downloadFileInfo : downloadFileInfos) {
				synchronized (mModifyLock) {// 同步
					mDownloadFileInfoMap.put(downloadFileInfo.getUrl(), downloadFileInfo);
				}
			}
		}
	}

	/**
	 * 通过Cursor获取全部信息
	 * 
	 * @param cursor
	 * @return
	 */
	private List<DownloadFileInfo> getDownloadFileInfosFromCursor(Cursor cursor) {
		List<DownloadFileInfo> downloadFileInfos = new ArrayList<DownloadFileInfo>();
		while (cursor != null && cursor.moveToNext()) {
			DownloadFileInfo downloadFileInfo = new DownloadFileInfo(cursor);
			if (downloadFileInfo != null) {
				downloadFileInfos.add(downloadFileInfo);
			}
		}
		return downloadFileInfos;
	}

	@Override
	public void recordStatus(String url, int status, int increaseSize) throws DownloadStatusRecordException {
		DownloadFileInfo downloadFileInfo = getDownloadFile(url);
		if (downloadFileInfo != null) {
			synchronized (mModifyLock) {// 同步，FIXME 无法跟updateDownloadFile一起同步
				if (increaseSize > 0) {
					downloadFileInfo.setDownloadedSize(downloadFileInfo.getDownloadedSize() + increaseSize);
				}
				downloadFileInfo.setStatus(status);
			}
			updateDownloadFile(downloadFileInfo);
		}
	}

	/** 释放资源 */
	void release() {
		synchronized (mModifyLock) {// 同步
			mDownloadFileInfoMap.clear();
			if (mDbHelper != null) {
				mDbHelper.close();
			}
		}
	}

	/** 下载状态记录异常 */
	public static class DownloadStatusRecordException extends FailException {

		private static final long serialVersionUID = 2729490220280837606L;

		public DownloadStatusRecordException(String detailMessage, String type) {
			super(detailMessage, type);
		}

		public DownloadStatusRecordException(Throwable throwable) {
			super(throwable);
		}

		@Override
		protected void onInitTypeWithThrowable(Throwable throwable) {
			super.onInitTypeWithThrowable(throwable);
			// TODO Auto-generated constructor stub
		}

	}

}
