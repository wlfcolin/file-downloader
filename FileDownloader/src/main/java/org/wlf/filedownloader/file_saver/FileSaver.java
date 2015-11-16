package org.wlf.filedownloader.file_saver;

import android.util.Log;

import org.wlf.filedownloader.base.FailException;
import org.wlf.filedownloader.base.Save;
import org.wlf.filedownloader.base.Stoppable;
import org.wlf.filedownloader.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * file saver
 * <br/>
 * 文件保存器
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class FileSaver implements Save, Stoppable {

    /**
     * LOG TAG
     */
    private static final String TAG = FileSaver.class.getSimpleName();

    private static final int BUFFER_SIZE_WRITE_TO_FILE = 32 * 1024; // 32 KB write to file
    private static final int BUFFER_SIZE_FOR_CALLBACK_NOTIFY = BUFFER_SIZE_WRITE_TO_FILE; // cache size to notify caller

    private String mUrl;
    private String mTempFilePath;
    private String mSaveFilePath;
    private int mFileTotalSize;// file total size

    private int mBufferSizeWriteToFile = BUFFER_SIZE_WRITE_TO_FILE;
    private int mBufferSizeForCallbackNotify = BUFFER_SIZE_FOR_CALLBACK_NOTIFY;

    private boolean mIsStopped;// whether stopped
    private boolean mIsNotifyEnd;// whether notify end

    private OnFileSaveListener mOnFileSaveListener;

    /**
     * constructor of FileSaver
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

        mIsStopped = false;// reset
        mIsNotifyEnd = false;// reset
    }

    /**
     * set FileSaveListener
     *
     * @param onFileSaveListener FileSaveListener
     */
    public void setOnFileSaveListener(OnFileSaveListener onFileSaveListener) {
        this.mOnFileSaveListener = onFileSaveListener;
    }

    @Override
    public void saveData(InputStream inputStream, int startPosInTotal) throws FileSaveException {

        // check whether stopped,if stopped,will throw FileSaveException
        checkIsStop();

        // create parent dir if necessary
        FileUtil.createFileParentDir(mTempFilePath);
        FileUtil.createFileParentDir(mSaveFilePath);

        File tempFile = new File(mTempFilePath);// temp file
        File saveFile = new File(mSaveFilePath);// save file

        String url = mUrl;// url

        byte[] buffer = new byte[mBufferSizeWriteToFile];// buffer to write to file
        int start = 0;
        int offset = 0;

        RandomAccessFile randomAccessFile = null;

        int cachedIncreaseSizeForNotify = 0;// zero

        try {

            int handledSize = 0;// handed size
            int needHandleSize = inputStream.available();// need handle size
            int increaseSize = 0;// every time increaseSize

            // FIXME
            mBufferSizeForCallbackNotify = (int) (needHandleSize / 100f);

            randomAccessFile = new RandomAccessFile(tempFile, "rwd");// write to temp file
            randomAccessFile.seek(startPosInTotal);// start write pos

            // 1.prepare to write
            if (mOnFileSaveListener != null) {
                mOnFileSaveListener.onSaveDataStart();
            }

            Log.d(TAG, "1、准备写文件缓存，路径：" + tempFile.getAbsolutePath() + "，url：" + url);

            while (!mIsStopped && (offset = inputStream.read(buffer, start, mBufferSizeWriteToFile)) != -1) {
                // write file
                randomAccessFile.write(buffer, start, offset);
                // increaseSize
                increaseSize = offset - start;
                // handledSize
                handledSize += increaseSize;
                // needNotifySize
                cachedIncreaseSizeForNotify += increaseSize;

                // need notify callback
                if (cachedIncreaseSizeForNotify >= mBufferSizeForCallbackNotify) {
                    // 2.saving
                    Log.v(TAG, "2、正在写文件缓存，已处理：" + handledSize + "，总共需要处理：" + needHandleSize + "，完成（百分比）：" + ((float) handledSize / needHandleSize * 100 / 100) + "%" + "，url：" + url);

                    if (mOnFileSaveListener != null) {
                        mOnFileSaveListener.onSavingData(cachedIncreaseSizeForNotify, needHandleSize);
                        cachedIncreaseSizeForNotify = 0;// FIXME whether set zero out of if
                    }
                }
            }

            // the file has been written finish，notify remain cachedIncreaseSize to callback
            if (cachedIncreaseSizeForNotify > 0) {
                // 2、正在保存
                Log.v(TAG, "2、正在写文件缓存，已处理：" + handledSize + "，总共需要处理：" + needHandleSize + "，完成（百分比）：" + ((float) handledSize / needHandleSize * 100 / 100) + "%" + "，url：" + url);

                if (mOnFileSaveListener != null) {
                    mOnFileSaveListener.onSavingData(cachedIncreaseSizeForNotify, needHandleSize);
                    cachedIncreaseSizeForNotify = 0;// FIXME whether set zero out of if
                }
            }

            // finish the file's total size
            if (needHandleSize == handledSize && tempFile.length() == mFileTotalSize) {
                if (saveFile.exists()) {// delete the file if exist
                    saveFile.delete();
                }
                boolean isCompleted = tempFile.renameTo(saveFile);
                if (!isCompleted) {
                    throw new FileSaveException("rename temp file:" + tempFile.getAbsolutePath() + " failed!", FileSaveException.TYPE_RENAME_TEMP_FILE_ERROR);
                }

                Log.d(TAG, "3、文件保存完成，路径：" + saveFile.getAbsolutePath() + "，url：" + url);

                // 3.completed,notifyEnd
                notifyEnd(cachedIncreaseSizeForNotify, isCompleted);
            }
            // interrupted(paused or error)
            else {
                // force stopped
                if (mIsStopped) {
                    // 4.paused,notifyEnd
                    notifyEnd(cachedIncreaseSizeForNotify, false);
                } else {
                    // 5.error
                    throw new FileSaveException("saving data error!", FileSaveException.TYPE_UNKNOWN);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof FileSaveException) {
                throw (FileSaveException) e;
            } else {
                throw new FileSaveException(e);
            }
        } finally {
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // if notified,set cachedIncreaseSizeForNotify to zero
            if (mIsNotifyEnd) {
                cachedIncreaseSizeForNotify = 0;
            }

            // if not notified,notify
            if (!mIsNotifyEnd) {
                notifyEnd(cachedIncreaseSizeForNotify, false);
            }

            // stopped
            mIsStopped = true;
        }
    }

    /**
     * notifyEnd
     */
    private void notifyEnd(int increaseSize, boolean complete) {
        if (mIsNotifyEnd) {
            return;
        }
        if (mOnFileSaveListener != null) {
            mOnFileSaveListener.onSaveDataEnd(increaseSize, complete);
            mIsNotifyEnd = true;
        }

        // force stop
        if (mIsNotifyEnd && !isStopped()) {
            stop();
        }
    }

    /**
     * checkIsStop
     */
    private void checkIsStop() throws FileSaveException {
        // if stopped,throw FileSaveException
        if (isStopped()) {
            Log.d(TAG, "--已经处理完了/强制停止了，不能再处理数据！");
            throw new FileSaveException("the file saver is stopped,can not handle data any more!", FileSaveException.TYPE_IS_STOPPED);
        }
    }

    /**
     * stop save file
     */
    @Override
    public void stop() {
        this.mIsStopped = true;// caller stop
    }

    /**
     * whether is stopped to save file
     */
    @Override
    public boolean isStopped() {
        return mIsStopped;
    }

    /**
     * FileSaveException
     */
    public static class FileSaveException extends FailException {

        private static final long serialVersionUID = -4239369213699703830L;

        /**
         * rename temp file failed
         */
        public static final String TYPE_RENAME_TEMP_FILE_ERROR = FileSaveException.class.getName() + "_TYPE_RENAME_TEMP_FILE_ERROR";
        /**
         * the file saver is stopped
         */
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
            // TODO
        }

    }

    /**
     * OnFileSaveListener
     */
    public interface OnFileSaveListener {

        /**
         * start to save data
         */
        void onSaveDataStart();

        /**
         * saving data
         *
         * @param increaseSize increaseSize
         * @param totalSize    total size needed to save
         */
        void onSavingData(int increaseSize, int totalSize);

        /**
         * finish saving data
         *
         * @param increaseSize increaseSize
         * @param complete     whether the finish the file's total size
         */
        void onSaveDataEnd(int increaseSize, boolean complete);
    }

}
