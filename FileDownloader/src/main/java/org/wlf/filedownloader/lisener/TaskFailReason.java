package org.wlf.filedownloader.lisener;

import org.wlf.filedownloader.base.FailReason;

/**
 * 任务失败原因
 * 
 * @author wlf
 * 
 */
public class TaskFailReason extends FailReason {

	private static final long serialVersionUID = -2387621301078094639L;

//	/** URL不合法 */
//	public static final String TYPE_URL_ILLEGAL = TaskFailReason.class.getName() + "_TYPE_URL_ILLEGAL";

	public TaskFailReason(String detailMessage, String type) {
		super(detailMessage, type);
	}

	public TaskFailReason(String detailMessage, Throwable throwable, String type) {
		super(detailMessage, throwable, type);
	}

	public TaskFailReason(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public TaskFailReason(String type) {
		super(type);
	}

	public TaskFailReason(Throwable throwable, String type) {
		super(throwable, type);
	}

	public TaskFailReason(Throwable throwable) {
		super(throwable);
	}

	@Override
	protected void onInitTypeWithThrowable(Throwable throwable) {
		super.onInitTypeWithThrowable(throwable);
		// TODO Auto-generated constructor stub
	}

}
