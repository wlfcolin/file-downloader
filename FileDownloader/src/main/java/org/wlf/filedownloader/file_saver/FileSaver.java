package org.wlf.filedownloader.file_saver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import org.wlf.filedownloader.base.FailException;
import org.wlf.filedownloader.base.Save;
import org.wlf.filedownloader.base.Stoppable;
import org.wlf.filedownloader.util.FileUtil;

import android.util.Log;

/**
 * 文件保存器
 * 
 * @author wlf
 * 
 */
public class FileSaver implements Save, Stoppable {

	/** LOG TAG */
	private static final String TAG = FileSaver.class.getSimpleName();

	/** 写文件默认缓冲区大小 */
	private static final int BUFFER_SIZE_WRITE_TO_FILE = 32 * 1024; // 32 KB
	/** 回调通知默认缓冲区大小 */
	private static final int BUFFER_SIZE_FOR_CALLBACK_NOTIFY = BUFFER_SIZE_WRITE_TO_FILE; // 跟写文件默认缓冲区大小一样

	private String mUrl;
	private String mTempFilePath;// 缓存的文件路径
	private String mSaveFilePath;// 保存的文件路径
	private int mFileTotalSize;// 文件总大小

	private int mBufferSizeWriteToFile = BUFFER_SIZE_WRITE_TO_FILE;// 写文件缓冲区大小
	private int mBufferSizeForCallbackNotify = BUFFER_SIZE_FOR_CALLBACK_NOTIFY;// 更新回调通知缓冲区大小

	private boolean mIsStopped;// 是否已停止处理
	private boolean mIsNotifyEnd;// 是否已经通知外部处理结束

	private OnFileSaveLisener mOnFileSaveLisener;// 文件保存监听器

	/**
	 * 构造文件保存器
	 * 
	 * @param url
	 * @param tempFilePath
	 * @param saveFilePath
	 * @param fileTotalSize
	 */
	public FileSaver(String url, String tempFilePath, String saveFilePath, int fileTotalSize) {
		super();
		this.mUrl = url;
		this.mTempFilePath = tempFilePath;
		this.mSaveFilePath = saveFilePath;
		this.mFileTotalSize = fileTotalSize;

		mIsStopped = false;// 初始状态
		mIsNotifyEnd = false;// 初始状态
	}

	/**
	 * 设置文件保存监听器
	 * 
	 * @param onFileSaveLisener
	 */
	public void setOnFileSaveLisener(OnFileSaveLisener onFileSaveLisener) {
		this.mOnFileSaveLisener = onFileSaveLisener;
	}

	@Override
	public void saveData(InputStream inputStream, int startPosInTotal) throws FileSaveException {

		// 检查是否停止处理了
		checkIsStop();

		// 创建父文件夹，防止写入错误
		FileUtil.createFileParentDir(mTempFilePath);
		FileUtil.createFileParentDir(mSaveFilePath);

		File tempFile = new File(mTempFilePath);// 临时文件
		File saveFile = new File(mSaveFilePath);// 保存文件

		String url = mUrl;// url

		byte[] buffer = new byte[mBufferSizeWriteToFile];// 写文件缓冲区大小
		int start = 0;
		int offset = 0;

		RandomAccessFile randomAccessFile = null;

		int cachedIncreaseSizeForNotify = 0;// 还未回调通知外部的大小

		try {

			int handledSize = 0;// 已处理的大小
			int needHandleSize = inputStream.available();// 总大小
			int increaseSize = 0;// 当次写入的实际大小

			// FIXME 处理了百分之一回调通知外部一次（限制不宜像写文件一样太频繁通知外部）
			mBufferSizeForCallbackNotify = (int) (needHandleSize / 100f);

			randomAccessFile = new RandomAccessFile(tempFile, "rwd");// 写到缓存文件中
			randomAccessFile.seek(startPosInTotal);// 定位到要开始写的位置

			// 1、准备开始保存
			if (mOnFileSaveLisener != null) {
				mOnFileSaveLisener.onSaveDataStart();
			}

			Log.d(TAG, "1、准备写文件缓存，路径：" + tempFile.getAbsolutePath() + "，url：" + url);

			while (!mIsStopped && (offset = inputStream.read(buffer, start, mBufferSizeWriteToFile)) != -1) {
				// 写文件
				randomAccessFile.write(buffer, start, offset);
				// 新增大小
				increaseSize = offset - start;
				// 已处理的大小
				handledSize += increaseSize;
				// 还未回调通知外部的大小
				cachedIncreaseSizeForNotify += increaseSize;

				// 需要通知外部
				if (cachedIncreaseSizeForNotify >= mBufferSizeForCallbackNotify) {
					// 2、正在保存
					Log.v(TAG, "2、正在写文件缓存，已处理：" + handledSize + "，总共需要处理：" + needHandleSize + "，完成（百分比）："
							+ ((float) handledSize / needHandleSize * 100 / 100) + "%" + "，url：" + url);

					if (mOnFileSaveLisener != null) {
						mOnFileSaveLisener.onSavingData(cachedIncreaseSizeForNotify, needHandleSize);
						cachedIncreaseSizeForNotify = 0;// FIXME 是否需要放到外面置0
					}
				}
			}

			// 循坏处理完了，强制通知外部一次
			if (cachedIncreaseSizeForNotify > 0) {
				// 2、正在保存
				Log.v(TAG, "2、正在写文件缓存，已处理：" + handledSize + "，总共需要处理：" + needHandleSize + "，完成（百分比）："
						+ ((float) handledSize / needHandleSize * 100 / 100) + "%" + "，url：" + url);

				if (mOnFileSaveLisener != null) {
					mOnFileSaveLisener.onSavingData(cachedIncreaseSizeForNotify, needHandleSize);
					cachedIncreaseSizeForNotify = 0;// FIXME 是否需要放到外面置0
				}
			}

			// 将缓存文件重命名为保存文件
			boolean isCompleted = false;
			// 全部数据已经处理完成，才重命名缓存文件
			if (needHandleSize == handledSize && tempFile.length() == mFileTotalSize) {
				if (saveFile.exists()) {// 如果文件已经存在则删掉
					saveFile.delete();
				}
				isCompleted = tempFile.renameTo(saveFile);
				if (!isCompleted) {
					throw new FileSaveException("重命名缓存文件：" + tempFile.getAbsolutePath() + "没有成功！",
							FileSaveException.TYPE_RENAME_TEMP_FILE_ERROR);
				}

				// 3、保存完成
				Log.d(TAG, "3、文件保存完成，路径：" + saveFile.getAbsolutePath() + "，url：" + url);

				// 通知外部结束
				notifyEnd(cachedIncreaseSizeForNotify, isCompleted);
			}
			// 被中断（暂停或者出错了）
			else {
				// 被强制停止
				if (mIsStopped) {
					// 4、保存暂停
					// 通知外部结束
					notifyEnd(cachedIncreaseSizeForNotify, false);
				} else {
					// 5、出错了，遇到了异常
					throw new FileSaveException("保存数据遇到未知错误，异常停止！", FileSaveException.TYPE_UNKNOWN);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// 将异常转抛出去
			if (e instanceof FileSaveException) {
				throw (FileSaveException) e;
			} else {
				throw new FileSaveException(e);
			}
		} finally {
			// 关闭文件写入流
			if (randomAccessFile != null) {
				try {
					randomAccessFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// 通知过外部，则cachedIncreaseSizeForNotify置为0
			if (mIsNotifyEnd) {
				cachedIncreaseSizeForNotify = 0;
			}

			// 还未通知外部，强制通知外部
			if (!mIsNotifyEnd) {
				notifyEnd(cachedIncreaseSizeForNotify, false);
			}

			// 已停止处理
			mIsStopped = true;
		}
	}

	/** 通知外部已经结束 */
	private void notifyEnd(int increaseSize, boolean complete) {
		if (mIsNotifyEnd) {
			return;
		}
		if (mOnFileSaveLisener != null) {
			mOnFileSaveLisener.onSaveDataEnd(increaseSize, complete);
			mIsNotifyEnd = true;
		}

		// 必须强行停止
		if (mIsNotifyEnd && !isStopped()) {
			stop();
		}
	}

	/** 检查是否已经停止 */
	private void checkIsStop() throws FileSaveException {
		// 如果已经要求停止处理了，则不做任何处理了
		if (isStopped()) {
			Log.d(TAG, "--已经处理完了/强制停止了，不能再处理数据！");
			throw new FileSaveException("文件保存器已经处理完了/强制停止了，不能再处理数据！", FileSaveException.TYPE_IS_STOPPED);
		}
	}

	/** 停止保存 */
	@Override
	public void stop() {
		this.mIsStopped = true;// 属于外部关闭
	}

	/**
	 * 是否已经停止保存文件
	 */
	@Override
	public boolean isStopped() {
		return mIsStopped;
	}

	/** 文件保存异常 */
	public static class FileSaveException extends FailException {

		private static final long serialVersionUID = -4239369213699703830L;

		/** 重命名缓存文件失败 */
		public static final String TYPE_RENAME_TEMP_FILE_ERROR = FileSaveException.class.getName()
				+ "_TYPE_RENAME_TEMP_FILE_ERROR";
		/** 文件大小不合法 */
		public static final String TYPE_FILE_SIEZ_ILLEGAL = FileSaveException.class.getName()
				+ "_TYPE_FILE_SIEZ_ILLEGAL";
		/** 文件保存器已经停止了 */
		public static final String TYPE_IS_STOPPED = FileSaveException.class.getName() + "_TYPE_IS_STOPPED";

		public FileSaveException(String detailMessage, String type) {
			super(detailMessage, type);
		}

		public FileSaveException(Throwable throwable) {
			super(throwable);
		}

		@Override
		protected void onInitTypeWithThrowable(Throwable throwable) {
			super.onInitTypeWithThrowable(throwable);
			// TODO Auto-generated constructor stub
		}

	}

	/** 文件数据保存监听器 */
	public interface OnFileSaveLisener {

		/** 开始保存数据 */
		void onSaveDataStart();

		/**
		 * 正在保存数据
		 * 
		 * @param increaseSize
		 *            新增大小
		 * @param totalSize
		 *            总需要处理大小
		 */
		void onSavingData(int increaseSize, int totalSize);

		/**
		 * 结束保存数据
		 * 
		 * @param increaseSize
		 *            新增大小
		 * @param complete
		 *            是否整个文件处理完成（文件完整的保存）
		 */
		void onSaveDataEnd(int increaseSize, boolean complete);
	}

}
