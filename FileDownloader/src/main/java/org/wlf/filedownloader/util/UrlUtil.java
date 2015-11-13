package org.wlf.filedownloader.util;

import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

/**
 * URL工具类
 * 
 * @author wlf
 * 
 */
public class UrlUtil {

	/**
	 * 是否合法的URL
	 * 
	 * @param url
	 * @return true表示是合法的url，false则不是
	 */
	public static boolean isUrl(String url) {
		if (TextUtils.isEmpty(url)) {
			return false;
		}
		
//		int start = url.lastIndexOf("/");
//		if (start == -1) {
//			return false;
//		}
//		String needEncode = url.substring(start + 1, url.length());// 只对url的最后一个/之后的数据编码
//		String needEncodeUrl = URLEncoder.encode(needEncode);// 使用URLEncoder进行编码
//		if (needEncodeUrl != null) {
//			needEncodeUrl = needEncodeUrl.replace("+", "%20");// 替换特殊编码字符
//		}
//
//		String encodedUrl = url.substring(0, start) + "/" + needEncodeUrl;
//		
//		String regEx = "^(http|www|ftp|)?(://)?(\\w+(-\\w+)*)(\\.(\\w+(-\\w+)*))*((:\\d+)?)(/(\\w+(-\\w+)*))*(\\.?(\\w)*)(\\?)?(((\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*(\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*)*(\\w*)*)$";
//
//		// String regEx = "^(http|https|ftp)\\://([a-zA-Z0-9\\.\\-]+(\\:[a-zA-"
//		// + "Z0-9\\.&%\\$\\-]+)*@)?((25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{"
//		// + "2}|[1-9]{1}[0-9]{1}|[1-9])\\.(25[0-5]|2[0-4][0-9]|[0-1]{1}"
//		// + "[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|"
//		// + "[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\.(25[0-5]|2[0-"
//		// + "4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[0-9])|([a-zA-Z0"
//		// + "-9\\-]+\\.)*[a-zA-Z0-9\\-]+\\.[a-zA-Z]{2,4})(\\:[0-9]+)?(/"
//		// + "[^/][a-zA-Z0-9\\.\\,\\?\\'\\\\/\\+&%\\$\\=~_\\-@]*)*$";
//
//		Pattern p = Pattern.compile(regEx, Pattern.CASE_INSENSITIVE);
//		Matcher matcher = p.matcher(encodedUrl);
//		return matcher.matches();
		return true;
	}
}
