package org.wlf.filedownloader.base;

import android.text.TextUtils;

/**
 * fail reason,which extends {@link Exception},it is need to care about type({@link #getType()})
 * and cause({@link #getCause()})
 * <br/>
 * 失败原因类，扩展自{@link Exception}类，使用时只需要关心type({@link #getType()})和cause(
 * {@link #getCause()})即可，可以满足各种同步和异步场合需求
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class FailReason extends Exception {

    private static final long serialVersionUID = -4866361177405722970L;

    /**
     * UNKNOWN
     */
    public static final String TYPE_UNKNOWN = FailReason.class.getName() + "_TYPE_UNKNOWN";
    /**
     * NULL_POINTER
     */
    public static final String TYPE_NULL_POINTER = FailReason.class.getName() + "_TYPE_NULL_POINTER";

    /**
     * fail type
     */
    private String mType = TYPE_UNKNOWN;

    /**
     * constructor of FailReason
     *
     * @param type fail type
     */
    public FailReason(String type) {
        this.mType = type;
        if (!isTypeInit()) {
            // init type with current object
            initType(this);
        }
    }

    /**
     * constructor of FailReason
     *
     * @param detailMessage fail description
     * @param type          fail type
     */
    public FailReason(String detailMessage, String type) {
        super(detailMessage);
        this.mType = type;
        if (!isTypeInit()) {
            // init type with current object
            initType(this);
        }
    }

    /**
     * constructor of FailReason
     *
     * @param detailMessage fail description
     * @param throwable     fail reason
     * @param type          fail type
     */
    public FailReason(String detailMessage, Throwable throwable, String type) {
        super(detailMessage, throwable);
        this.mType = type;
        if (!isTypeInit()) {
            // init type with throwable
            initType(throwable);
        }
    }

    /**
     * constructor of FailReason
     *
     * @param throwable fail reason
     * @param type      fail type
     */
    public FailReason(Throwable throwable, String type) {
        super(throwable);
        this.mType = type;
        if (!isTypeInit()) {
            // init type with throwable
            initType(throwable);
        }
    }

    /**
     * constructor of FailReason
     *
     * @param detailMessage fail description
     * @param throwable     fail reason
     */
    public FailReason(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
        // init type with throwable
        initType(throwable);
    }

    /**
     * constructor of FailReason
     *
     * @param throwable fail reason
     */
    public FailReason(Throwable throwable) {
        super(throwable);
        // init type with throwable
        initType(throwable);
    }

    // --------------------------------------------------------------

    /**
     * whether init fail type
     *
     * @return true means initialized
     */
    protected final boolean isTypeInit() {
        if (TYPE_UNKNOWN.equals(mType) || TextUtils.isEmpty(mType)) {
            return false;
        }
        return true;
    }

    /**
     * init fail type
     *
     * @param throwable fail reason
     */
    private void initType(Throwable throwable) {
        if (throwable == null) {// exit
            return;
        }
        // try init
        onInitTypeWithThrowable(throwable);

        if (throwable == throwable.getCause()) {
            return;// exit
        }

        if (!isTypeInit()) {// init type recursive
            initType(throwable.getCause());
        }
    }

    /**
     * init type by fail reason,for child to override
     *
     * @param throwable fail reason
     */
    protected void onInitTypeWithThrowable(Throwable throwable) {

        if (throwable == null) {
            return;
        }

        String throwableClassName = throwable.getClass().getName();

        if (TextUtils.isEmpty(throwableClassName)) {
            return;
        }

        if (throwableClassName.equals(NullPointerException.class.getName())) {
            mType = TYPE_NULL_POINTER;
        }
    }

    /**
     * set fail type
     *
     * @param type fail type
     */
    protected final void setType(String type) {
        this.mType = type;
    }

    /**
     * get fail type
     *
     * @return fail type
     */
    public String getType() {
        return mType;
    }


    /**
     * Returns the cause of original cause,the difference between {@link #getCause()} and {@link #getOriginalCause()}
     * is,
     * {@link #getCause()} may return the result including {@link FailReason} and it's child,{@link #getOriginalCause()}
     * will never return {@link FailReason} and it's child
     *
     * @return Throwable this {@code Throwable}'s cause.
     */
    public Throwable getOriginalCause() {
        return getOriginalCauseInternal(this);
    }

    // getOriginalCauseInternal
    private Throwable getOriginalCauseInternal(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Throwable cause = throwable.getCause();
        if (cause instanceof FailReason) {
            return getOriginalCauseInternal(cause);
        } else {
            return cause;
        }
    }
}
