package org.wlf.filedownloader.util;

import java.util.Collection;

/**
 * Util for {@link Collection}
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class CollectionUtil {

    /**
     * Returns true if the collection is null or 0-length.
     *
     * @param collection the collection to be examined
     * @return true if str collection null or zero length
     */
    public static boolean isEmpty(Collection<?> collection) {
        if (collection == null || collection.isEmpty() || collection.size() == 0) {
            return true;
        } else {
            return false;
        }
    }
}
