package org.wlf.filedownloader.util;

import java.util.Map;

/**
 * Util for {@link Map}
 *
 * @author wlf(Andy)
 * @datetime 2015-11-14 18:12 GMT+8
 * @email 411086563@qq.com
 */
public class MapUtil {

    /**
     * Returns true if the collection is null or 0-length.
     *
     * @param map the map to be examined
     * @return true if str is null or zero length
     */
    public static boolean isEmpty(Map<?, ?> map) {
        if (map == null || map.isEmpty() || map.size() == 0) {
            return true;
        } else {
            return false;
        }
    }
}
