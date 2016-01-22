package org.wlf.filedownloader.base;

/**
 * fail exception, use for sync method call
 * <br/>
 * 失败异常，用于同步调用抛出的异常
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public abstract class FailException extends FailReason {

    public FailException(String type) {
        super(type);
    }

    public FailException(String detailMessage, String type) {
        super(detailMessage, type);
    }

    public FailException(String detailMessage, Throwable throwable, String type) {
        super(detailMessage, throwable, type);
    }

    public FailException(Throwable throwable, String type) {
        super(throwable, type);
    }

    public FailException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public FailException(Throwable throwable) {
        super(throwable);
    }
}
