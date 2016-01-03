package org.wlf.filedownloader.util;

import android.text.TextUtils;

import java.net.URI;
import java.net.URLEncoder;

/**
 * URL Util
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class UrlUtil {

    private static final EncodeInfo[] SPECIAL_CHARACTER_ENCODER_MAP = new EncodeInfo[]{
            // % need first
            new EncodeInfo("%", URLEncoder.encode("%")),
            //
            new EncodeInfo(" ", "%20"),
            //
            new EncodeInfo("[", URLEncoder.encode("[")),
            //
            new EncodeInfo("]", URLEncoder.encode("]")),
            //
            new EncodeInfo("#", URLEncoder.encode("#"))
            //
    };

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

        String encodedUrl = getASCIIEncodedUrl(url);
        if (TextUtils.isEmpty(encodedUrl)) {
            return false;
        }

        return true;

        //        String regEx = "^(http|www|ftp|)?(://)?(\\w+(-\\w+)*)(\\.(\\w+(-\\w+)*))*((:\\d+)?)(/(\\w+(-\\w+)*)
        // )*(\\.?(\\w)*)(\\?)?(((\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*(\\w*%)*
        // (\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*)*(\\w*)*)$";
        //
        //        Pattern p = Pattern.compile(regEx, Pattern.CASE_INSENSITIVE);
        //        Matcher matcher = p.matcher(encodedUrl);
        //        return matcher.matches();
    }

    /**
     * get Encoded Url
     *
     * @param url file url
     * @return encoded url
     */
    public static String getASCIIEncodedUrl(String url) {

        // trim url
        if (url != null) {
            url = url.trim();
        }

        String encodedUrl = null;

        if (TextUtils.isEmpty(url)) {
            return null;
        }

        String replacedUrl = getReplacedUrl(url);

        try {
            URI uri = URI.create(replacedUrl);
            encodedUrl = uri.toASCIIString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (TextUtils.isEmpty(encodedUrl)) {
            if (!TextUtils.isEmpty(replacedUrl)) {
                encodedUrl = replacedUrl;
            } else {
                encodedUrl = url;
            }
        }

        // Log.e("wlf", "encodedUrl:" + encodedUrl);

        return encodedUrl;
    }

    /**
     * get File Name
     *
     * @param url file url
     * @return File Name
     */
    public static String getFileNameByUrl(String url) {

        String fileName = null;

        if (TextUtils.isEmpty(url)) {
            return null;
        }

        String replacedUrl = getReplacedUrl(url);

        try {
            URI uri = URI.create(replacedUrl);
            String path = uri.getPath();
            if (TextUtils.isEmpty(path)) {
                path = uri.getRawPath();
            }
            if (!TextUtils.isEmpty(path)) {
                fileName = path.substring(path.lastIndexOf('/') + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!TextUtils.isEmpty(fileName)) {
            return fileName;
        }

        return getUndoReplacedUrl(url);
    }

    private static String getReplacedUrl(String originalUrl) {

        if (originalUrl == null) {
            return null;
        }

        String replacedUrl = originalUrl;

        for (EncodeInfo encodeInfo : SPECIAL_CHARACTER_ENCODER_MAP) {
            if (encodeInfo == null) {
                continue;
            }
            if (replacedUrl.contains(encodeInfo.unEncode)) {
                // replace
                replacedUrl = replacedUrl.replace(encodeInfo.unEncode, encodeInfo.encoded);
            }
        }

        if (TextUtils.isEmpty(replacedUrl)) {
            replacedUrl = originalUrl;
        }

        return replacedUrl;
    }

    private static String getUndoReplacedUrl(String replacedUrl) {

        if (replacedUrl == null) {
            return null;
        }

        String originalUrl = replacedUrl;

        for (int i = SPECIAL_CHARACTER_ENCODER_MAP.length - 1; i > 0; i--) {
            EncodeInfo encodeInfo = SPECIAL_CHARACTER_ENCODER_MAP[i];
            if (encodeInfo == null) {
                continue;
            }
            if (originalUrl.contains(encodeInfo.encoded)) {
                // replace
                originalUrl = originalUrl.replace(encodeInfo.encoded, encodeInfo.unEncode);
            }
        }

        if (TextUtils.isEmpty(originalUrl)) {
            originalUrl = replacedUrl;
        }

        return originalUrl;
    }

    private static class EncodeInfo {

        public final String unEncode;
        public final String encoded;

        public EncodeInfo(String unEncode, String encoded) {
            this.unEncode = unEncode;
            this.encoded = encoded;
        }
    }

}
