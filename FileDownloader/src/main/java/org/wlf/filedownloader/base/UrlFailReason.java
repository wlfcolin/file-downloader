package org.wlf.filedownloader.base;

/**
 * UrlFailReason
 *
 * @author wlf(Andy)
 * @datetime 2016-01-22 14:07 GMT+8
 * @email 411086563@qq.com
 */
public abstract class UrlFailReason extends FailReason {

    /**
     * fail url
     */
    private String mUrl;

    public UrlFailReason(String url, String type) {
        super(type);
        init(url);
    }

    public UrlFailReason(String url, String detailMessage, String type) {
        super(detailMessage, type);
        init(url);
    }

    public UrlFailReason(String url, String detailMessage, Throwable throwable, String type) {
        super(detailMessage, throwable, type);
        init(url);
    }

    public UrlFailReason(String url, Throwable throwable, String type) {
        super(throwable, type);
        init(url);
    }

    public UrlFailReason(String url, String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
        init(url);
    }

    public UrlFailReason(String url, Throwable throwable) {
        super(throwable);
        init(url);
    }

    private void init(String url) {
        this.mUrl = url;
    }

    // --------------------------------------getters & setters--------------------------------------

    /**
     * set url
     *
     * @param url the url
     */
    protected final void setUrl(String url) {
        mUrl = url;
    }

    /**
     * get url
     *
     * @return the url
     */
    public String getUrl() {
        return mUrl;
    }
}
