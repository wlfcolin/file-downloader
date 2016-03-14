package org.wlf.filedownloader.util;

import android.text.TextUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.BitSet;

/**
 * URL Util
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class UrlUtil {

    private static final EncodeInfo[] SPECIAL_CHARACTER_ENCODER_MAP = new EncodeInfo[]{
            // % need first
            // new EncodeInfo("%", URLEncoder.encode("%")),
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
     * Emcode/escape a portion of a URL, to use with the query part ensure {@code plusAsBlank} is true.{@see
     * http://www.boyunjian.com/javasrc/org.apache.httpcomponents/httpclient/4.2
     * .2/_/org/apache/http/client/utils/URLEncodedUtils.java}
     *
     * @param content     the portion to decode
     * @param charset     the charset to use
     * @param blankAsPlus if {@code true}, then convert space to '+', otherwise leave as is.
     * @return
     */
    private static String urlEncode(String content, Charset charset, BitSet safeChars, boolean blankAsPlus) {
        if (content == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        ByteBuffer bb = charset.encode(content);
        while (bb.hasRemaining()) {
            int b = bb.get() & 0xff;
            if (safeChars.get(b)) {
                buf.append((char) b);
            } else if (blankAsPlus && b == ' ') {
                buf.append('+');
            } else {
                buf.append("%");
                char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
                char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
                buf.append(hex1);
                buf.append(hex2);
            }
        }
        return buf.toString();
    }

    /**
     * Decode/unescape a portion of a URL, to use with the query part ensure {@code plusAsBlank} is true.{@see
     * http://www.boyunjian.com/javasrc/org.apache.httpcomponents/httpclient/4.2
     * .2/_/org/apache/http/client/utils/URLEncodedUtils.java}
     *
     * @param content     the portion to decode
     * @param charset     the charset to use
     * @param plusAsBlank if {@code true}, then convert '+' to space, otherwise leave as is.
     * @return
     */
    private static String urlDecode(String content, Charset charset, boolean plusAsBlank) {
        if (content == null) {
            return null;
        }
        ByteBuffer bb = ByteBuffer.allocate(content.length());
        CharBuffer cb = CharBuffer.wrap(content);
        while (cb.hasRemaining()) {
            char c = cb.get();
            if (c == '%' && cb.remaining() >= 2) {
                char uc = cb.get();
                char lc = cb.get();
                int u = Character.digit(uc, 16);
                int l = Character.digit(lc, 16);
                if (u != -1 && l != -1) {
                    bb.put((byte) ((u << 4) + l));
                } else {
                    bb.put((byte) '%');
                    bb.put((byte) uc);
                    bb.put((byte) lc);
                }
            } else if (plusAsBlank && c == '+') {
                bb.put((byte) ' ');
            } else {
                bb.put((byte) c);
            }
        }
        bb.flip();
        return charset.decode(bb).toString();
    }

    /**
     * whether is the url encoded
     *
     * @param content the content
     * @return true means encoded
     */
    private boolean isUrlEncoded(String content, Charset charset, boolean plusAsBlank) {

        if (TextUtils.isEmpty(content)) {
            return false;
        }

        String decodedContent = urlDecode(content, charset, plusAsBlank);

        if (decodedContent != null && decodedContent.equalsIgnoreCase(content)) {
            return false;// the decodedContent is the same with the content,so it is not encoded
        }

        return true;
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