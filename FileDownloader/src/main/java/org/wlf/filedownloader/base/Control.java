package org.wlf.filedownloader.base;

/**
 * to control an operation
 *
 * @author wlf(Andy)
 * @datetime 2015-12-07 22:00 GMT+8
 * @email 411086563@qq.com
 */
public interface Control {

    /**
     * stop the operation
     */
    void stop();

    /**
     * whether is the operation stopped
     *
     * @return true means the operation has been stopped
     */
    boolean isStopped();
}
