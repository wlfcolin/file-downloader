package org.wlf.filedownloader.base;

import android.text.TextUtils;

/**
 * 失败原因类，扩展自{@link Exception}类，使用时只需要关心type({@link #getType()})和cause(
 * {@link #getCause()})即可， 可以满足各种同步和异步场合需求
 * 
 * @author wlf
 */
public class FailReason extends Exception {

	private static final long serialVersionUID = -4866361177405722970L;

	/**
	 * 未知
	 */
	public static final String TYPE_UNKNOWN = FailReason.class.getName() + "_TYPE_UNKNOWN";
	/**
	 * 空指针
	 */
	public static final String TYPE_NULL_POINTER = FailReason.class.getName() + "_TYPE_NULL_POINTER";

	/**
	 * 失败类型
	 */
	private String mType = TYPE_UNKNOWN;

	/**
	 * 构造失败原因
	 * 
	 * @param type
	 *            失败类型
	 */
	public FailReason(String type) {
		this.mType = type;
		if (!isTypeInit()) {
			// 使用自身来初始化type
			initType(this);
		}
	}

	/**
	 * 构造失败原因
	 * 
	 * @param detailMessage
	 *            失败描述
	 * @param type
	 *            失败类型
	 */
	public FailReason(String detailMessage, String type) {
		super(detailMessage);
		this.mType = type;
		if (!isTypeInit()) {
			// 使用自身来初始化type
			initType(this);
		}
	}

	/**
	 * 构造失败原因
	 * 
	 * @param detailMessage
	 *            失败描述
	 * @param throwable
	 *            导致失败的原因
	 * @param type
	 *            失败类型
	 */
	public FailReason(String detailMessage, Throwable throwable, String type) {
		super(detailMessage, throwable);
		this.mType = type;
		if (!isTypeInit()) {
			// 使用throwable来初始化type
			initType(throwable);
		}
	}

	/**
	 * 构造失败原因
	 * 
	 * @param throwable
	 *            导致失败的原因
	 * @param type
	 *            失败类型
	 */
	public FailReason(Throwable throwable, String type) {
		super(throwable);
		this.mType = type;
		if (!isTypeInit()) {
			// 使用throwable来初始化type
			initType(throwable);
		}
	}

	/**
	 * 构造失败原因
	 * 
	 * @param detailMessage
	 *            失败描述
	 * @param throwable
	 *            导致失败的原因
	 */
	public FailReason(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
		// 使用throwable来初始化type
		initType(throwable);
	}

	/**
	 * 构造失败原因
	 * 
	 * @param throwable
	 *            导致失败的原因
	 */
	public FailReason(Throwable throwable) {
		super(throwable);
		// 使用throwable来初始化type
		initType(throwable);
	}

	// --------------------------------------------------------------

	/**
	 * 是否已经初始化
	 * 
	 * @return true表示已经初始化
	 */
	protected final boolean isTypeInit() {
		if (TYPE_UNKNOWN.equals(mType) || TextUtils.isEmpty(mType)) {
			return false;
		}
		return true;
	}

	/**
	 * 使用Throwable递归初始化
	 * 
	 * @param throwable
	 *            导致失败的原因
	 */
	private void initType(Throwable throwable) {
		if (throwable == null) {// 退出递归
			return;
		}
		// 尝试初始化
		onInitTypeWithThrowable(throwable);

		if (throwable == throwable.getCause()) {
			return;// 退出递归
		}

		if (!isTypeInit()) {// 如果是未初始化。则递归
			initType(throwable.getCause());
		}
	}

	/**
	 * 使用Throwable初始化异常类型，子类可以复写此方法
	 * 
	 * @param throwable
	 *            导致失败的原因
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
	 * 设置失败类型
	 * 
	 * @param type
	 *            失败类型
	 */
	protected final void setType(String type) {
		this.mType = type;
	}

	/**
	 * 获取当前失败类型
	 * 
	 * @return 当前失败类型
	 */
	public String getType() {
		return mType;
	}

}
