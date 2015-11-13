package org.wlf.filedownloader.http_downlaoder;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;

import org.wlf.filedownloader.base.Download;
import org.wlf.filedownloader.base.FailException;
import org.wlf.filedownloader.helper.HttpConnectionHelper;

import android.text.TextUtils;
import android.util.Log;

/**
 * Http下载器
 * 
 * @author wlf
 * 
 */
public class HttpDownloader implements Download {

	/** LOG TAG */
	private static final String TAG = HttpDownloader.class.getSimpleName();

	private static final int MAX_REDIRECT_COUNT = 5;// 重定向次数
	private static final int CONNECT_TIMEOUT = 10 * 1000;// 默认连接超时，10秒超时
	private static final String CHARSET = "UTF-8";// 请求编码

	private String mUrl;// 下载资源连接
	private Range mRange;// 下载的区域
	private String mAcceptRangeType;// 接收的分段下载范围类型
	private String mETag;// 下载资源对应的eTag
	private int mConnectTimeout = CONNECT_TIMEOUT;// 连接超时

	private OnHttpDownloadLisener mOnHttpDownloadLisener;// Http下载监听器

	/**
	 * 构建Http资源下载器，从头开始下载整个资源
	 * 
	 * @param url
	 *            下载地址
	 */
	public HttpDownloader(String url) {
		this(url, null, null);
	}

	/**
	 * 构建Http资源下载器，从指定range进行断点续传并严格检查acceptRangeType，若acceptRangeType不一致则无法下载
	 * 
	 * @param url
	 *            下载地址
	 * @param range
	 *            下载范围
	 * @param acceptRangeType
	 *            接收的分段下载范围类型
	 */
	public HttpDownloader(String url, Range range, String acceptRangeType) {
		this(url, range, acceptRangeType, null);
	}

	/**
	 * 构建Http资源下载器，从指定range进行断点续传并严格检查acceptRangeType和eTag，
	 * 若acceptRangeType或eTag不一致则无法下载
	 * 
	 * @param url
	 *            下载地址
	 * @param range
	 *            下载范围
	 * @param acceptRangeType
	 *            接收的分段下载范围类型
	 * @param eTag
	 *            资源eTag
	 */
	public HttpDownloader(String url, Range range, String acceptRangeType, String eTag) {
		this.mUrl = url;
		this.mRange = range;
		this.mAcceptRangeType = acceptRangeType;
		this.mETag = eTag;
	}

	/**
	 * 设置下载Http下载监听器
	 * 
	 * @param onHttpDownloadLisener
	 */
	public void setOnHttpDownloadLisener(OnHttpDownloadLisener onHttpDownloadLisener) {
		this.mOnHttpDownloadLisener = onHttpDownloadLisener;
	}

	@Override
	public void download() throws HttpDownloadException {

		String url = mUrl;// url

		HttpURLConnection conn = null;
		InputStream inputStream = null;

		try {
			// 4.0后可能需要加上，但加上后效率很不好
			// StrictMode.setThreadPolicy(new
			// StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites()
			// .detectNetwork().penaltyLog().build());

			conn = HttpConnectionHelper.createDownloadFileConnection(url, mConnectTimeout, CHARSET, mRange);

			int redirectCount = 0;
			while (conn != null && conn.getResponseCode() / 100 == 3 && redirectCount < MAX_REDIRECT_COUNT) {
				conn = HttpConnectionHelper.createDownloadFileConnection(conn.getHeaderField("Location"),
						mConnectTimeout, CHARSET, mRange);
				redirectCount++;
			}

			Log.d(TAG, "1、准备下载，重定向：" + redirectCount + "次" + "，最大重定向次数：" + MAX_REDIRECT_COUNT + "，url：" + url);

			if (redirectCount > MAX_REDIRECT_COUNT) {
				// error 重定向次数超过限制
				throw new HttpDownloadException("重定向次数已经超过设置的最大限制次数：" + MAX_REDIRECT_COUNT + "！",
						HttpDownloadException.TYPE_REDIRECT_COUNT_OVER_LIMITS);
			}

			// 1、检查响应ResponseCode
			int responseCode = conn.getResponseCode();

			if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {

				// 2、检查contentLength
				int contentLength = conn.getContentLength();

				Log.d(TAG, "2、得到服务器返回的资源contentLength：" + contentLength + "，传入的range：" + mRange.toString() + "，url："
						+ url);

				if (contentLength <= 0) {
					// error 资源大小不合法
					throw new HttpDownloadException("资源大小不合法，获取网络资源很可能失败！",
							HttpDownloadException.TYPE_RESOURCES_SIEZ_ILLEGAL);
				}

				// 3、检查eTag（验证资源是否变化）
				if (!TextUtils.isEmpty(mETag)) {
					String eTag = conn.getHeaderField("ETag");

					Log.d(TAG, "3、得到服务器返回的资源eTag：" + eTag + "，传入的eTag：" + mETag + "，url：" + url);

					if (TextUtils.isEmpty(eTag) || !mETag.equals(eTag)) {
						// error eTag验证不通过
						throw new HttpDownloadException("服务器资源已变化，无法支撑断点续传，请重新下载整个新资源！",
								HttpDownloadException.TYPE_ETAG_CHANGED);
					}
				}

				// 不期望断点续传，range为空或者不在合法的区域则下载整个资源（因为在创建连接的时候不会传递range给服务器）
				if (!Range.isLegal(mRange) || (mRange != null && mRange.getLength() > contentLength)) {
					mRange = new Range(0, contentLength);// FIXME 需要通知外部？
				}
				// 期望断点续传
				else {
					// 4、检查contentRange和acceptRangeType（若需要断点续传的话）
					if (mRange != null && !TextUtils.isEmpty(mAcceptRangeType)) {

						boolean isRangeValidateSucceed = false;

						String contentRange = conn.getHeaderField("Content-Range");
						// 解析ContentRange
						ContentRangeInfo contentRangeInfo = ContentRangeInfo.getContentRangeInfo(contentRange);
						if (contentRangeInfo != null) {
							Range serverResponseRange = new Range(contentRangeInfo.startPos, contentRangeInfo.endPos);
							if (mRange.equals(serverResponseRange)
									&& mAcceptRangeType.equals(contentRangeInfo.contentType)
									&& serverResponseRange.getLength() == contentLength) {
								// 验证通过
								isRangeValidateSucceed = true;
							}
						}

						if (!isRangeValidateSucceed) {
							// error contentRange验证不通过
							throw new HttpDownloadException("无法完成请求的断点续传范围，contentRange验证不通过！",
									HttpDownloadException.TYPE_CONTENT_RANGE_VALIDATE_FAIL);
						}
					}
				}

				// 获取服务器返回的输入流
				InputStream serverInputStream = conn.getInputStream();

				// 包装服务器返回的输入流
				inputStream = new ContentLengthInputStream(serverInputStream, contentLength);

				Log.d(TAG, "4、准备处理数据，获取服务器返回的资源长度为：" + contentLength + "，获取服务器返回的输入流长度为：" + inputStream.available()
						+ "，需要处理的区域为：" + mRange.toString() + "，url：" + url);

				// 通知已经下载
				notifyDownloadConnected(inputStream, mRange.startPos);
			}
			// ResponseCode验证不通过
			else {
				// error ResponseCode验证不通过
				throw new HttpDownloadException("服务器返回的ResponseCode：" + responseCode + "无法读取数据！",
						HttpDownloadException.TYPE_RESPONSE_CODE_ERROR);
			}
		} catch (Exception e) {
			// 请求网络超时
			if (e instanceof SocketTimeoutException) {
				// error 请求网络超时
				throw new HttpDownloadException("请求网络超时！", e, HttpDownloadException.TYPE_NETWORK_TIMEOUT);
			} else {
				// 将异常转抛出去
				throw new HttpDownloadException(e);
			}
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

			Log.d(TAG, "5、下载已结束" + "，url：" + url);
		}

	}

	// 通知下载已连接
	private void notifyDownloadConnected(InputStream inputStream, int startPosInTotal) {
		if (mOnHttpDownloadLisener != null) {
			mOnHttpDownloadLisener.onDownloadConnected(inputStream, mRange.startPos);
		}
	}

	/** Http下载异常 */
	public static class HttpDownloadException extends FailException {

		private static final long serialVersionUID = -1264975040094495002L;

		/** 重定向次数超过限制 */
		public static final String TYPE_REDIRECT_COUNT_OVER_LIMITS = HttpDownloadException.class.getName()
				+ "_TYPE_REDIRECT_COUNT_OVER_LIMITS";
		/** 资源大小不合法 */
		public static final String TYPE_RESOURCES_SIEZ_ILLEGAL = HttpDownloadException.class.getName()
				+ "_TYPE_RESOURCES_SIEZ_ILLEGAL";
		/** eTag验证不通过 */
		public static final String TYPE_ETAG_CHANGED = HttpDownloadException.class.getName() + "_TYPE_ETAG_CHANGED";
		/** contentRange验证不通过 */
		public static final String TYPE_CONTENT_RANGE_VALIDATE_FAIL = HttpDownloadException.class.getName()
				+ "_TYPE_CONTENT_RANGE_VALIDATE_FAIL";
		/** responseCode验证不通过 */
		public static final String TYPE_RESPONSE_CODE_ERROR = HttpDownloadException.class.getName()
				+ "_TYPE_RESPONSE_CODE_ERROR";
		/** 网络超时 */
		public static final String TYPE_NETWORK_TIMEOUT = HttpDownloadException.class.getName()
				+ "_TYPE_NETWORK_TIMEOUT";

		public HttpDownloadException(String detailMessage, String type) {
			super(detailMessage, type);
		}

		public HttpDownloadException(String detailMessage, Throwable throwable, String type) {
			super(detailMessage, throwable, type);
		}

		public HttpDownloadException(Throwable throwable) {
			super(throwable);
		}

		@Override
		protected void onInitTypeWithThrowable(Throwable throwable) {
			super.onInitTypeWithThrowable(throwable);

			if (isTypeInit() || throwable == null) {
				return;
			}

			if (throwable instanceof SocketTimeoutException) {
				setType(TYPE_NETWORK_TIMEOUT);
			}
		}
	}

	/** Http下载监听器 */
	public interface OnHttpDownloadLisener {

		/**
		 * 下载已连接
		 * 
		 * @param inputStream
		 *            下载输入流
		 * @param startPosInTotal
		 *            该参数是输入流位于总长度的起始点。
		 *            <p>
		 *            |(totalStart)----|(startPosInTotal,inputStream
		 *            start)-----|(inputStream end)------|(totalEnd)
		 */
		void onDownloadConnected(InputStream inputStream, int startPosInTotal);
	}
}
