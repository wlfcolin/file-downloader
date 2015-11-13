package org.wlf.filedownloader.helper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.wlf.filedownloader.http_downlaoder.Range;

/**
 * Http连接帮助类
 * 
 * @author wlf
 * 
 */
public class HttpConnectionHelper {

	/** 创建探测网络文件的连接 */
	public static HttpURLConnection createDetectConnection(String url, int connectTimeout, String charset)
			throws Exception {
		return createHttpUrlConnection(url, connectTimeout, charset, -1, -1);
	}

	/** 创建下载网络文件的连接，如果range范围可用，则设置请求数据的范围 */
	public static HttpURLConnection createDownloadFileConnection(String url, int connectTimeout, String charset,
			Range range) throws Exception {

		int rangeStartPos = -1;
		int rangeEndPos = -1;

		if (Range.isLegal(range)) {
			rangeStartPos = range.startPos;
			rangeEndPos = range.endPos;
		}

		return createHttpUrlConnection(url, connectTimeout, charset, rangeStartPos, rangeEndPos);
	}

	/**
	 * 创建Http链接，如果[rangeStartPos,rangeEndPos]范围可用，则设置请求数据的范围
	 * 
	 * @param url
	 * @param connectTimeout
	 * @param charset
	 * @param rangeStartPos
	 * @param rangeEndPos
	 * @return Http链接
	 * @throws Exception
	 *             连接时产生的异常
	 */
	public static HttpURLConnection createHttpUrlConnection(String url, int connectTimeout, String charset,
			int rangeStartPos, int rangeEndPos) throws Exception {

		int start = url.lastIndexOf("/");
		if (start == -1) {
			throw new IllegalAccessException("URL不合法");
		}
		String needEncode = url.substring(start + 1, url.length());// 只对url的最后一个/之后的数据编码
		String needEncodeUrl = URLEncoder.encode(needEncode);// 使用URLEncoder进行编码
		if (needEncodeUrl != null) {
			needEncodeUrl = needEncodeUrl.replace("+", "%20");// 替换特殊编码字符
		}

		String encodedUrl = url.substring(0, start) + "/" + needEncodeUrl;

		HttpURLConnection conn = (HttpURLConnection) new URL(encodedUrl).openConnection();
		conn.setConnectTimeout(connectTimeout);
		conn.setRequestProperty("Charset", charset);
		conn.setRequestProperty("Accept-Encoding", "identity");
		// 设置获取实体数据的范围，设置断点续传范围（此时并不知道对方服务器是否支持断点续传）
		if (rangeStartPos > 0 && rangeEndPos > 0 && rangeEndPos > rangeStartPos) {
			conn.setRequestProperty("Range", "bytes=" + rangeStartPos + "-" + rangeEndPos);
		}
		conn.connect();
		return conn;
	}

}
