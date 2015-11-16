package org.wlf.filedownloader.util;

import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * URL Util
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class UrlUtil {

    /**
     * whether the file url is http url
     *
     * @param url file url
     * @return true means the file url is http url
     */
    public static boolean isUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        String encodedUrl = getEncoderUrl(url, "UTF-8");
        if (TextUtils.isEmpty(encodedUrl)) {
            return false;
        }

        return true;

        //        String regEx = "^(http|www|ftp|)?(://)?(\\w+(-\\w+)*)(\\.(\\w+(-\\w+)*))*((:\\d+)?)(/(\\w+(-\\w+)*))*(\\.?(\\w)*)(\\?)?(((\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*(\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*)*(\\w*)*)$";
        //
        //        Pattern p = Pattern.compile(regEx, Pattern.CASE_INSENSITIVE);
        //        Matcher matcher = p.matcher(encodedUrl);
        //        return matcher.matches();
    }

    /**
     * get Encoder Url
     *
     * @param url     file url
     * @param charset encode charset
     * @return encoded url
     */
    public static String getEncoderUrl(String url, String charset) {

        int start = url.lastIndexOf("/");
        if (start == -1) {
            return null;
        }
        String needEncode = url.substring(start + 1, url.length());// FIXME just encode file name,not file path
        String needEncodeUrl = null;// URL Encoder
        try {
            needEncodeUrl = URLEncoder.encode(needEncode, charset);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (needEncodeUrl != null) {
            needEncodeUrl = needEncodeUrl.replace("+", "%20");// replace space
        }

        String encodedUrl = url.substring(0, start) + "/" + needEncodeUrl;
        return encodedUrl;
    }
}
