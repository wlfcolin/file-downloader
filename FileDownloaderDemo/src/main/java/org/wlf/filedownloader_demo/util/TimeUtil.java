package org.wlf.filedownloader_demo.util;

import java.text.SimpleDateFormat;

/**
 * time util
 * <br/>
 * 时间工具类
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class TimeUtil {

    /**
     * format seconds to HH:mm:ss String
     *
     * @param seconds seconds
     * @return String of formatted in HH:mm:ss
     */
    public static String seconds2HH_mm_ss(long seconds) {
        long ms = seconds * 1000;//毫秒数  
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        return formatter.format(ms);
    }

    /**
     * format seconds to mm:ss String
     *
     * @param seconds seconds
     * @return String of formatted in mm:ss
     */
    public static String seconds2mm_ss(long seconds) {
        long ms = seconds * 1000;//毫秒数  
        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
        return formatter.format(ms);
    }
}
