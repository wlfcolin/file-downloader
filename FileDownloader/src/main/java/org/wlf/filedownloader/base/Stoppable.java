package org.wlf.filedownloader.base;

/**
 * interface for those can stop classes
 * <br/>
 * 可停止接口
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public interface Stoppable {

    /**
     * stop
     */
    void stop();

    /**
     * whether is stopped
     *
     * @return true means stopped
     */
    boolean isStopped();

}
