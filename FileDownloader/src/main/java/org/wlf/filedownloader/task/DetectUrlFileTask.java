package org.wlf.filedownloader.task;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.wlf.filedownloader.DetectUrlFileCacher;
import org.wlf.filedownloader.DetectUrlFileInfo;
import org.wlf.filedownloader.DownloadFileCacher;
import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.helper.HttpConnectionHelper;
import org.wlf.filedownloader.lisener.OnDetectUrlFileListener;
import org.wlf.filedownloader.lisener.OnDetectUrlFileListener.DetectUrlFileFailReason;
import org.wlf.filedownloader.util.UrlUtil;

import android.util.Log;

/**
 * 探测网络文件任务
 * 
 * @author wlf
 * 
 */
public class DetectUrlFileTask implements Runnable {

	private static final String TAG = DetectUrlFileTask.class.getSimpleName();

	private static final int MAX_REDIRECT_COUNT = 5;// 最大重定向次数
	private static final int CONNECT_TIMEOUT = 10 * 1000;// 默认连接超时，10秒超时
	private static final String CHARSET = "UTF-8";// 请求编码

	private String mUrl;
	private String mDonwloadSaveDir;// 保存地址
	private DetectUrlFileCacher mDetectUrlFileCacher;// 探测缓存器
	private DownloadFileCacher mDownloadFileCacher;
	private OnDetectUrlFileListener mOnDetectUrlFileListener;// 探测网络文件监听器

	public DetectUrlFileTask(String url, String donwloadSaveDir, DetectUrlFileCacher detectUrlFileCacher,
			DownloadFileCacher downloadFileCacher) {
		super();
		this.mUrl = url;
		this.mDonwloadSaveDir = donwloadSaveDir;
		this.mDetectUrlFileCacher = detectUrlFileCacher;
		this.mDownloadFileCacher = downloadFileCacher;
	}

	public void setOnDetectUrlFileListener(OnDetectUrlFileListener onDetectUrlFileListener) {
		this.mOnDetectUrlFileListener = onDetectUrlFileListener;
	}

	@Override
	public void run() {

		HttpURLConnection conn = null;
		InputStream inputStream = null;

		DetectUrlFileFailReason failReason = null;

		try {
			// URL不合法
			if (!UrlUtil.isUrl(mUrl)) {
				// error url不合法
				failReason = new DetectUrlFileFailReason("url不合法！", DetectUrlFileFailReason.TYPE_URL_ILLEGAL);
			}
			// URL合法
			else {
				DownloadFileInfo downloadFileInfo = mDownloadFileCacher.getDownloadFile(mUrl);
				if (downloadFileInfo != null) {
					// 1、已存在，直接返回
					if (mOnDetectUrlFileListener != null) {
						OnDetectUrlFileListener.MainThreadHelper.onDetectUrlFileExist(mUrl, mOnDetectUrlFileListener);
					}
					return;
				}

				conn = HttpConnectionHelper.createDetectConnection(mUrl, CONNECT_TIMEOUT, CHARSET);

				int redirectCount = 0;
				while (conn.getResponseCode() / 100 == 3 && redirectCount < MAX_REDIRECT_COUNT) {
					conn = HttpConnectionHelper.createDetectConnection(mUrl, CONNECT_TIMEOUT, CHARSET);
					redirectCount++;
				}

				Log.d(TAG, "探测文件，重定向：" + redirectCount + "次" + "，最大重定向次数：" + MAX_REDIRECT_COUNT + "，url：" + mUrl);

				if (redirectCount > MAX_REDIRECT_COUNT) {
					// error 重定向次数超过限制
					failReason = new DetectUrlFileFailReason("重定向次数已经超过设置的最大限制次数：" + MAX_REDIRECT_COUNT + "！",
							DetectUrlFileFailReason.TYPE_URL_OVER_REDIRECT_COUNT);
				} else {
					switch (conn.getResponseCode()) {
					// 成功响应
					case HttpURLConnection.HTTP_OK:
						// 文件名
						String fileName = mUrl.substring(mUrl.lastIndexOf('/') + 1);
						// 文件大小
						int fileSize = conn.getContentLength();
						// 文件标识
						String eTag = conn.getHeaderField("ETag");
						// 可以分段接受文件的单位，一般是bytes
						String acceptRangeType = conn.getHeaderField("Accept-Ranges");

						// 新建网络探测文件信息
						DetectUrlFileInfo detectUrlFileInfo = new DetectUrlFileInfo(mUrl, fileSize, eTag,
								acceptRangeType, mDonwloadSaveDir, fileName);

						// 添加网络探测文件
						mDetectUrlFileCacher.addOrUpdateDetectUrlFile(detectUrlFileInfo);

						// 2、需要创建新下载文件
						if (mOnDetectUrlFileListener != null) {
							OnDetectUrlFileListener.MainThreadHelper.onDetectNewDownloadFile(mUrl, fileName,
									mDonwloadSaveDir, fileSize, mOnDetectUrlFileListener);
						}
						break;

					// 文件不存在
					case HttpURLConnection.HTTP_NOT_FOUND:
						// error Http响应错误
						failReason = new DetectUrlFileFailReason("要下载的文件不存在！",
								DetectUrlFileFailReason.TYPE_HTTP_FILE_NOT_EXIST);
						break;
					default:
						// error Http响应错误
						failReason = new DetectUrlFileFailReason("Http响应错误！",
								DetectUrlFileFailReason.TYPE_BAD_HTTP_RESPONSE_CODE);
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// 捕获到异常
			failReason = new DetectUrlFileFailReason(e);
		} finally {
			// 关闭输入流
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// 关闭联网
			if (conn != null) {
				conn.disconnect();
			}
			// 有异常
			if (failReason != null) {
				// 3、出现异常
				if (mOnDetectUrlFileListener != null) {
					OnDetectUrlFileListener.MainThreadHelper.onDetectUrlFileFailed(mUrl, failReason,
							mOnDetectUrlFileListener);
				}
			}
		}
	}
}
