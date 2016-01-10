package org.wlf.filedownloader.util;

/**
 * MathUtil
 *
 * @author wlf(Andy)
 * @datetime 2015-12-11 21:40 GMT+8
 * @email 411086563@qq.com
 */
public class MathUtil {

    public static double formatNumber(double number) {
        return (Math.round(number * 100.00)) / 100.00;
    }
}
