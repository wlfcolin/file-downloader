package org.wlf.filedownloader.util;

import android.text.TextUtils;

import org.wlf.filedownloader.base.Log;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * URL Util
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class UrlUtil {

    private static final EncodeInfo FRAGMENT_SPLIT = new EncodeInfo("#", "%23");

    private static final EncodeInfo SPACE_SPLIT = new EncodeInfo("+", "%20");

    private static final EncodeInfo[] SPECIAL_CHARACTER_ENCODER_MAP = new EncodeInfo[]{
            //
            SPACE_SPLIT,
            //
            FRAGMENT_SPLIT,
            //
            // new EncodeInfo("+", "%2B"), // FIXME
            //
            new EncodeInfo("/", "%2F"),
            //
            new EncodeInfo("?", "%3F"),
            //
            new EncodeInfo("%", "%25"),
            //
            new EncodeInfo("&", "%26"),
            //
            new EncodeInfo("=", "%3D"),
            //
    };

    private static String getEncodedHost(String host, String userInfo, String charset, EncodeInfo[] specials) {

        if (TextUtils.isEmpty(host)) {
            return null;
        }

        StringBuffer bufferEncodedHost = new StringBuffer();

        try {

            // ----------------------host----------------------

            String[] splitHost = host.split(".");

            // included '.'
            if (!ArrayUtil.isEmpty(splitHost)) {
                for (int i = 0; i < splitHost.length; i++) {
                    String hostSegment = splitHost[i];
                    if (i != 0) {
                        bufferEncodedHost.append(".");
                    }
                    if (TextUtils.isEmpty(hostSegment)) {
                        continue;
                    }
                    // not encode
                    if (!isEncoded(hostSegment, charset)) {
                        // encode
                        String encodedHostSegment = URLEncoder.encode(hostSegment, charset);
                        if (TextUtils.isEmpty(encodedHostSegment)) {
                            continue;
                        }
                        // check specials
                        if (!ArrayUtil.isEmpty(specials)) {
                            for (EncodeInfo encodeInfo : specials) {
                                if (encodeInfo == null || TextUtils.isEmpty(encodeInfo.needEncode) ||
                                        TextUtils.isEmpty(encodeInfo.encoded)) {
                                    continue;
                                }
                                if (!TextUtils.isEmpty(encodedHostSegment) && encodedHostSegment.contains(encodeInfo
                                        .encoded)) {
                                    // replace means undo encoded
                                    encodedHostSegment = encodedHostSegment.replace(encodeInfo.encoded, encodeInfo
                                            .needEncode);
                                }
                            }
                        }
                        bufferEncodedHost.append(encodedHostSegment);

                    }
                    // encoded
                    else {
                        bufferEncodedHost.append(hostSegment);
                    }

                }
            }
            // do not included '.'
            else {
                // not encode
                if (!isEncoded(host, charset)) {
                    // encode
                    String encodedHost = URLEncoder.encode(host, charset);
                    if (!TextUtils.isEmpty(encodedHost)) {
                        // check specials
                        if (!ArrayUtil.isEmpty(specials)) {
                            for (EncodeInfo encodeInfo : specials) {
                                if (encodeInfo == null || TextUtils.isEmpty(encodeInfo.needEncode) ||
                                        TextUtils.isEmpty(encodeInfo.encoded)) {
                                    continue;
                                }
                                if (!TextUtils.isEmpty(encodedHost) && encodedHost.contains(encodeInfo.encoded)) {
                                    // replace means undo encoded
                                    encodedHost = encodedHost.replace(encodeInfo.encoded, encodeInfo.needEncode);
                                }
                            }
                        }
                        bufferEncodedHost.append(encodedHost);
                    }
                }
                // encoded
                else {
                    bufferEncodedHost.append(host);
                }
            }

            // ----------------------userInfo----------------------

            if (!TextUtils.isEmpty(userInfo)) {
                bufferEncodedHost.append("@");
                bufferEncodedHost.append(userInfo);// FIXME here not encode
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return bufferEncodedHost.toString();
    }

    private static String getEncodedFile(String path, String query, String ref, String charset, EncodeInfo[] specials) {

        if (TextUtils.isEmpty(path)) {
            return null;
        }

        StringBuffer bufferEncodedFile = new StringBuffer();

        try {

            // ----------------------path----------------------

            String[] splitPath = path.split("/");

            // included '/'
            if (!ArrayUtil.isEmpty(splitPath)) {
                for (int i = 0; i < splitPath.length; i++) {
                    String pathSegment = splitPath[i];
                    if (i != 0) {
                        bufferEncodedFile.append("/");
                    }
                    if (TextUtils.isEmpty(pathSegment)) {
                        continue;
                    }
                    // not encode
                    if (!isEncoded(pathSegment, charset)) {
                        // encode
                        String encodedPathSegment = URLEncoder.encode(pathSegment, charset);
                        if (TextUtils.isEmpty(encodedPathSegment)) {
                            continue;
                        }
                        // check specials
                        if (!ArrayUtil.isEmpty(specials)) {
                            for (EncodeInfo encodeInfo : specials) {
                                if (encodeInfo == null || TextUtils.isEmpty(encodeInfo.needEncode) ||
                                        TextUtils.isEmpty(encodeInfo.encoded)) {
                                    continue;
                                }
                                if (!TextUtils.isEmpty(encodedPathSegment) && encodedPathSegment.contains(encodeInfo
                                        .encoded)) {
                                    // replace means undo encoded
                                    encodedPathSegment = encodedPathSegment.replace(encodeInfo.encoded, encodeInfo
                                            .needEncode);
                                }
                            }
                        }
                        bufferEncodedFile.append(encodedPathSegment);
                    }
                    // encoded
                    else {
                        bufferEncodedFile.append(pathSegment);
                    }
                }
            }
            // do not included '/'
            else {
                // not encode
                if (!isEncoded(path, charset)) {
                    // encode
                    String encodedPath = URLEncoder.encode(path, charset);
                    if (!TextUtils.isEmpty(encodedPath)) {
                        // check specials
                        if (!ArrayUtil.isEmpty(specials)) {
                            for (EncodeInfo encodeInfo : specials) {
                                if (encodeInfo == null || TextUtils.isEmpty(encodeInfo.needEncode) ||
                                        TextUtils.isEmpty(encodeInfo.encoded)) {
                                    continue;
                                }
                                if (!TextUtils.isEmpty(encodedPath) && encodedPath.contains(encodeInfo.encoded)) {
                                    // replace means undo encoded
                                    encodedPath = encodedPath.replace(encodeInfo.encoded, encodeInfo.needEncode);
                                }
                            }
                        }
                        bufferEncodedFile.append(encodedPath);
                    }
                }
                // encoded
                else {
                    bufferEncodedFile.append(path);
                }
            }

            // ----------------------query----------------------

            if (!TextUtils.isEmpty(query)) {

                if (query.contains("\\?") || query.contains("&")) {
                    bufferEncodedFile.append("?");
                }

                String[] splitQuery = query.split("&");

                // included '&'
                if (!ArrayUtil.isEmpty(splitQuery)) {
                    for (int i = 0; i < splitQuery.length; i++) {
                        String querySegment = splitQuery[i];
                        if (i != 0) {
                            bufferEncodedFile.append("&");
                        }
                        if (TextUtils.isEmpty(querySegment)) {
                            continue;
                        }
                        // check '='
                        String[] keyValue = querySegment.split("=");
                        // included '='
                        if (!ArrayUtil.isEmpty(keyValue) && keyValue.length >= 2) {
                            String key = keyValue[0];
                            String value = keyValue[1];
                            if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
                                continue;
                            }
                            // not encode
                            if (!isEncoded(key, charset)) {
                                // encode
                                key = URLEncoder.encode(key, charset);
                                // check specials
                                if (!ArrayUtil.isEmpty(specials)) {
                                    for (EncodeInfo encodeInfo : specials) {
                                        if (encodeInfo == null || TextUtils.isEmpty(encodeInfo.needEncode) ||
                                                TextUtils.isEmpty(encodeInfo.encoded)) {
                                            continue;
                                        }
                                        if (!TextUtils.isEmpty(key) && key.contains(encodeInfo.encoded)) {
                                            // replace means undo encoded
                                            key = key.replace(encodeInfo.encoded, encodeInfo.needEncode);
                                        }
                                    }
                                }
                            }
                            // encoded
                            else {
                            }

                            // not encode
                            if (!isEncoded(value, charset)) {
                                // encode
                                value = URLEncoder.encode(value, charset);
                                // check specials
                                if (!ArrayUtil.isEmpty(specials)) {
                                    for (EncodeInfo encodeInfo : specials) {
                                        if (encodeInfo == null || TextUtils.isEmpty(encodeInfo.needEncode) ||
                                                TextUtils.isEmpty(encodeInfo.encoded)) {
                                            continue;
                                        }
                                        if (!TextUtils.isEmpty(value) && value.contains(encodeInfo.encoded)) {
                                            // replace means undo encoded
                                            value = value.replace(encodeInfo.encoded, encodeInfo.needEncode);
                                        }
                                    }
                                }
                            }
                            // encoded
                            else {
                            }

                            bufferEncodedFile.append(key);
                            bufferEncodedFile.append("=");
                            bufferEncodedFile.append(value);
                        }
                        // do not included '='
                        else {
                            // not encode
                            if (!isEncoded(querySegment, charset)) {
                                // encode
                                String encodedQuerySegment = URLEncoder.encode(querySegment, charset);
                                if (!TextUtils.isEmpty(encodedQuerySegment)) {
                                    // check specials
                                    if (!ArrayUtil.isEmpty(specials)) {
                                        for (EncodeInfo encodeInfo : specials) {
                                            if (encodeInfo == null || TextUtils.isEmpty(encodeInfo.needEncode) ||
                                                    TextUtils.isEmpty(encodeInfo.encoded)) {
                                                continue;
                                            }
                                            if (!TextUtils.isEmpty(encodedQuerySegment) && encodedQuerySegment
                                                    .contains(encodeInfo.encoded)) {
                                                // replace means undo encoded
                                                encodedQuerySegment = encodedQuerySegment.replace(encodeInfo.encoded,
                                                        encodeInfo.needEncode);
                                            }
                                        }
                                    }
                                    bufferEncodedFile.append(encodedQuerySegment);
                                }
                            }
                            // encoded
                            else {
                                bufferEncodedFile.append(querySegment);
                            }
                        }
                    }
                }
                // do not included '&'
                else {
                    // not encode
                    if (!isEncoded(query, charset)) {
                        // encode
                        String encodedQuery = URLEncoder.encode(query, charset);
                        if (!TextUtils.isEmpty(encodedQuery)) {
                            // check specials
                            if (!ArrayUtil.isEmpty(specials)) {
                                for (EncodeInfo encodeInfo : specials) {
                                    if (encodeInfo == null || TextUtils.isEmpty(encodeInfo.needEncode) ||
                                            TextUtils.isEmpty(encodeInfo.encoded)) {
                                        continue;
                                    }
                                    if (!TextUtils.isEmpty(encodedQuery) && encodedQuery.contains(encodeInfo.encoded)) {
                                        // replace means undo encoded
                                        encodedQuery = encodedQuery.replace(encodeInfo.encoded, encodeInfo.needEncode);
                                    }
                                }
                            }
                            bufferEncodedFile.append(encodedQuery);
                        }
                    }
                    // encoded
                    else {
                        bufferEncodedFile.append(query);
                    }
                }
            }

            // ----------------------ref----------------------

            if (!TextUtils.isEmpty(ref)) {
                // not encode
                if (!isEncoded(query, charset)) {
                    // encode
                    String encodedRef = URLEncoder.encode(ref, charset);
                    if (!TextUtils.isEmpty(encodedRef)) {
                        bufferEncodedFile.append("#");
                        bufferEncodedFile.append(encodedRef);
                    }
                }
                // encoded
                else {
                    bufferEncodedFile.append("#");
                    bufferEncodedFile.append(ref);
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return bufferEncodedFile.toString();
    }

    public static boolean isEncoded(String content, String charset) {

        if (TextUtils.isEmpty(content)) {
            return false;
        }

        // if it is included special character encoded, that means it is has been encoded
        // if this time use the follow codes to check will throws java.lang.IllegalArgumentException: Invalid XXX
        // sequence by URLDecoder.decode

        Log.d("wlf", "isEncoded，check content：" + content);

        for (EncodeInfo info : SPECIAL_CHARACTER_ENCODER_MAP) {
            if (info == null) {
                continue;
            }
            if (content.contains(info.needEncode)) {
                return false;
            }
        }

        String decodedContent = null;
        try {
            decodedContent = URLDecoder.decode(content, charset);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (decodedContent != null && decodedContent.equalsIgnoreCase(content)) {
            return false;// the decodedContent is the same with the content,so it is not encoded
        }

        return true;
    }

    public static String decode(String content, String charset) {
        try {
            String decodedContent = URLDecoder.decode(content, charset);
            return decodedContent;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getEncodedUrl(String url, String charset, boolean useFragment, boolean blankAsPlus) {

        Log.d("wlf", "getEncodedUrl，--------------------");

        Log.d("wlf", "getEncodedUrl，准备编码URL，url：" + url);

        if (TextUtils.isEmpty(url)) {
            return null;
        }

        // trim url
        if (url != null) {
            url = url.trim();
        }

        String encodedUrl = null;

        try {

            // encoded url

            URL unEncodeUrl = new URL(url);

            Log.d("wlf", "getEncodedUrl，开始编码URL，unEncodeUrl：" + unEncodeUrl);

            Log.d("wlf", "getEncodedUrl，开始编码URL，getProtocol：" + unEncodeUrl.getProtocol());

            Log.d("wlf", "getEncodedUrl，开始编码URL，getHost：" + unEncodeUrl.getHost());
            Log.d("wlf", "getEncodedUrl，开始编码URL，getUserInfo：" + unEncodeUrl.getUserInfo());

            Log.d("wlf", "getEncodedUrl，开始编码URL，getPort：" + unEncodeUrl.getPort());

            Log.d("wlf", "getEncodedUrl，开始编码URL，getFile：" + unEncodeUrl.getFile());
            Log.d("wlf", "getEncodedUrl，开始编码URL，getPath：" + unEncodeUrl.getPath());
            Log.d("wlf", "getEncodedUrl，开始编码URL，getQuery：" + unEncodeUrl.getQuery());
            Log.d("wlf", "getEncodedUrl，开始编码URL，getRef：" + unEncodeUrl.getRef());

            //  protocol  ://  host  @  userInfo  :  port  /  path  ?  query  #  ref
            //|-protocol-|    |-------host------|  |-port-|   |--------file--------|
            // new URL(String protocol, String host, int port, String file);

            String protocol = unEncodeUrl.getProtocol();

            String host = unEncodeUrl.getHost();// may need encode
            String userInfo = unEncodeUrl.getUserInfo();// may need encode

            host = getEncodedHost(host, userInfo, charset, SPECIAL_CHARACTER_ENCODER_MAP);

            int port = unEncodeUrl.getPort();

            String file = unEncodeUrl.getFile();// may need encode
            String path = unEncodeUrl.getPath();// may need encode
            String query = unEncodeUrl.getQuery();// may need encode
            String ref = unEncodeUrl.getRef();// may need encode

            file = getEncodedFile(path, query, ref, charset, SPECIAL_CHARACTER_ENCODER_MAP);

            URL tempEncodedUrl = new URL(protocol, host, port, file);

            encodedUrl = tempEncodedUrl.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // check space
        if (encodedUrl != null && encodedUrl.contains(SPACE_SPLIT.needEncode) && !blankAsPlus) {
            encodedUrl = encodedUrl.replaceAll("\\" + SPACE_SPLIT.needEncode, SPACE_SPLIT.encoded);
        }

        // check fragment
        if (encodedUrl != null && encodedUrl.contains(FRAGMENT_SPLIT.needEncode) && !useFragment) {
            encodedUrl = encodedUrl.replaceAll(FRAGMENT_SPLIT.needEncode, FRAGMENT_SPLIT.encoded);
        }

        Log.e("wlf", "getEncodedUrl，编码后URL，encodedUrl：" + encodedUrl);

        return encodedUrl;
    }

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

        String encodedUrl = getASCIIEncodedUrl(url, "UTF-8");
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
    public static String getASCIIEncodedUrl(String url, String charset) {
        return getEncodedUrl(url, charset, false, false);
    }

    /**
     * get File Name
     *
     * @param url file url
     * @return File Name
     */
    public static String getFileNameByUrl(String url, String charset) {

        String fileName = null;

        if (TextUtils.isEmpty(url)) {
            return null;
        }

        // trim url
        if (url != null) {
            url = url.trim();
        }

        try {
            if (url.contains("/")) {
                fileName = url.substring(url.lastIndexOf('/') + 1);
                if (fileName != null && fileName.contains("?")) {
                    fileName = fileName.substring(0, fileName.indexOf('?'));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!TextUtils.isEmpty(fileName)) {
            if (isEncoded(fileName, charset)) {
                // decode
                String decodedContent = decode(fileName, charset);
                if (!TextUtils.isEmpty(decodedContent)) {
                    fileName = decodedContent;
                }
            }
        }

        if (!TextUtils.isEmpty(fileName)) {
            return fileName;
        }

        return url;
    }

    private static class EncodeInfo {

        public final String needEncode;
        public final String encoded;

        public EncodeInfo(String needEncode, String encoded) {
            this.needEncode = needEncode;
            this.encoded = encoded;
        }
    }

}