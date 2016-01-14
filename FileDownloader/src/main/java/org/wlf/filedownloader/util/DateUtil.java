package org.wlf.filedownloader.util;

import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Util for formatting {@link Date}
 *
 * @author wlf(Andy)
 * @datetime 2015-12-30 23:53 GMT+8
 * @email 411086563@qq.com
 */
public class DateUtil {

    /**
     * Thread Safe SimpleDateFormats
     */
    private static final Map<String, ThreadLocal<SimpleDateFormat>> DATE_FORMAT_MAP = new ConcurrentHashMap<String, 
            ThreadLocal<SimpleDateFormat>>();

    /**
     * get a safe SimpleDateFormat
     *
     * @param format date format in string
     * @return SimpleDateFormat
     */
    private static SimpleDateFormat getSimpleDateFormat(String format) {
        ThreadLocal<SimpleDateFormat> threadLocal = DATE_FORMAT_MAP.get(format);
        if (threadLocal == null) {
            threadLocal = new ThreadLocal<SimpleDateFormat>();
            DATE_FORMAT_MAP.put(format, threadLocal);
        }
        SimpleDateFormat sdf = threadLocal.get();
        if (sdf == null) {
            sdf = new SimpleDateFormat(format, Locale.getDefault());
            threadLocal.set(sdf);
        }
        return sdf;
    }

    /**
     * get datetime by date string which formatted with yyyy-MM-dd HH:mm:ss
     *
     * @param strDate formatted with yyyy-MM-dd HH:mm:ss
     * @return date object
     */
    public static Date string2Date_yyyy_MM_dd_HH_mm_ss(String strDate) {

        if (TextUtils.isEmpty(strDate)) {
            return null;
        }

        SimpleDateFormat sdf = getSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return sdf.parse(strDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * format date to yyyy-MM-dd HH:mm:ss string
     *
     * @param date the date
     * @return formatted string
     */
    public static String date2String_yyyy_MM_dd_HH_mm_ss(Date date) {
        return getSimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}
