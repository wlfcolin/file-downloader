package org.wlf.filedownloader.file_download.file_saver;

import android.os.SystemClock;

import org.wlf.filedownloader.base.FailException;
import org.wlf.filedownloader.base.FailReason;
import org.wlf.filedownloader.base.Log;
import org.wlf.filedownloader.base.Stoppable;
import org.wlf.filedownloader.file_download.http_downloader.ContentLengthInputStream;
import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader.util.MathUtil;

import java.io.File;
import java.io.IOException;
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

    private static final String TAG = FileSaver.class.getSimpleName();

    private static final int BUFFER_SIZE_WRITE_TO_FILE = 32 * 1024; // 32 KB write to file

    private String mUrl;
    private String mTempFilePath;
    private String mSaveFilePath;
    private long mFileTotalSize;// file total size

    private int mBufferSizeWriteToFile = BUFFER_SIZE_WRITE_TO_FILE;

    private DownloadNoticeStrategy mDownloadNoticeStrategy = DownloadNoticeStrategy.NOTICE_AUTO;// default is auto

    private boolean mIsStopped;// whether stopped
    private boolean mIsNotifyEnd;// whether notify end

    private OnFileSaveListener mOnFileSaveListener;

    /**
     * constructor of FileSaver
     *
     * @param url           the file url
     * @param tempFilePath  the temp file path
     * @param saveFilePath  the save file path
     * @param fileTotalSize the file total size
     */
    public FileSaver(String url, String tempFilePath, String saveFilePath, long fileTotalSize) {
        super();
        this.mUrl = url;
        this.mTempFilePath = tempFilePath;
        this.mSaveFilePath = saveFilePath;
        this.mFileTotalSize = fileTotalSize;

        mIsStopped = false;// reset
        mIsNotifyEnd = false;// reset
    }

    /**
     * set OnFileSaveListener
     *
     * @param onFileSaveListener OnFileSaveListener
     */
    public void setOnFileSaveListener(OnFileSaveListener onFileSaveListener) {
        this.mOnFileSaveListener = onFileSaveListener;
    }

    /**
     * if it throw FileSaveException,that means saving data failed(error occur)
     */
    @Override
    public void saveData(ContentLengthInputStream inputStream, long startPosInTotal) throws FileSaveException {

        boolean hasException = true;
        String filePath = null;

        boolean isCompleted = false;

        String url = mUrl;// url

        byte[] buffer = new byte[mBufferSizeWriteToFile];// the buffer to write to file
        int startIndex = 0;// the start index of the buffer to read in
        int readSize;// the size of the buffer has been readed

        RandomAccessFile randomAccessFile = null;

        // for calculating size to notify caller
        int needNotifySize = 0;// init with zero
        // for calculating time to notify caller
        long lastNotifyTime;

        try {
            // check whether stopped,if stopped,will throw FileSaveException
            checkIsStop();

            // create parent dir for saving file if necessary
            FileUtil.createFileParentDir(mTempFilePath);
            FileUtil.createFileParentDir(mSaveFilePath);

            // init temp file and save file
            File tempFile = new File(mTempFilePath);// temp file
            File saveFile = new File(mSaveFilePath);// save file

            long handledFileSize = 0;// handed file size
            long needHandleSize = inputStream.getLength();// this time need handle size
            int increaseSize;// the increaseSize of every buffer

            randomAccessFile = new RandomAccessFile(tempFile, "rwd");// write to temp file
            randomAccessFile.seek(startPosInTotal);// set start write pos

            filePath = tempFile.getAbsolutePath();

            // 1.notify caller,prepare to write
            notifyStart();

            Log.d(TAG, TAG + ".saveData 1、准备写文件缓存，路径：" + tempFile.getAbsolutePath() + "，url：" + url);

            lastNotifyTime = SystemClock.elapsedRealtime();
            long curTime = SystemClock.elapsedRealtime();

            while (!mIsStopped && (readSize = inputStream.read(buffer, startIndex, mBufferSizeWriteToFile)) != -1) {
                // temp not exist exception occur
                if (!tempFile.exists()) {
                    throw new FileSaveException("temp file not exist!", FileSaveException
                            .TYPE_TEMP_FILE_DOES_NOT_EXIST);
                }
                // write file
                randomAccessFile.write(buffer, startIndex, readSize);
                // increaseSize
                increaseSize = readSize - startIndex;
                // handledFileSize
                handledFileSize += increaseSize;
                // needNotifySize
                needNotifySize += increaseSize;

                curTime = SystemClock.elapsedRealtime();
                long dTime = curTime - lastNotifyTime;

                // check whether notify caller
                switch (mDownloadNoticeStrategy) {
                    case NOTICE_AUTO:
                        long maxNotifySize = (long) (needHandleSize * 0.5);// handed more than 50%
                        // need notify caller,time first
                        if (dTime >= DownloadNoticeStrategy.NOTICE_BY_TIME.getValue()) {
                            // 2.saving
                            Log.d(TAG, TAG + ".saveData 2、正在写文件缓存，已处理：" + handledFileSize + "，总共需要处理：" +
                                    needHandleSize + "，完成（百分比）：" + (MathUtil.formatNumber(((double) handledFileSize /
                                    needHandleSize) * 100)) + "%，url：" + url);

                            if (notifySaving(needNotifySize, needHandleSize)) {
                                needNotifySize = 0;
                                lastNotifyTime = curTime;
                            }
                        } else {
                            // need notify caller,file size secondly
                            if (needNotifySize >= maxNotifySize) {
                                // 2.saving
                                Log.d(TAG, TAG + ".saveData 2、正在写文件缓存，已处理：" + handledFileSize + "，总共需要处理：" +
                                        needHandleSize + "，完成（百分比）：" + (MathUtil.formatNumber(((double) 
                                        handledFileSize / needHandleSize) * 100)) + "%，url：" + url);

                                if (notifySaving(needNotifySize, needHandleSize)) {
                                    needNotifySize = 0;
                                    lastNotifyTime = curTime;
                                }
                            }
                        }
                        break;
                    case NOTICE_BY_SIZE:
                        // need notify caller
                        if (needNotifySize >= mDownloadNoticeStrategy.getValue()) {
                            // 2.saving
                            Log.d(TAG, TAG + ".saveData 2、正在写文件缓存，已处理：" + handledFileSize + "，总共需要处理：" +
                                    needHandleSize + "，完成（百分比）：" + (MathUtil.formatNumber(((double) handledFileSize /
                                    needHandleSize) * 100)) + "%，url：" + url);

                            if (notifySaving(needNotifySize, needHandleSize)) {
                                needNotifySize = 0;
                                lastNotifyTime = curTime;
                            }
                        }
                        break;
                    case NOTICE_BY_TIME:
                        // need notify caller
                        if (dTime >= mDownloadNoticeStrategy.getValue()) {
                            // 2.saving
                            Log.d(TAG, TAG + ".saveData 2、正在写文件缓存，已处理：" + handledFileSize + "，总共需要处理：" +
                                    needHandleSize + "，完成（百分比）：" + (MathUtil.formatNumber(((double) handledFileSize /
                                    needHandleSize) * 100)) + "%，url：" + url);

                            if (notifySaving(needNotifySize, needHandleSize)) {
                                needNotifySize = 0;
                                lastNotifyTime = curTime;
                            }
                        }
                        break;
                }
            }

            // the file has been written finish，notify remain needNotifySize to caller
            if (needNotifySize > 0) {
                // 2、saving
                Log.d(TAG, TAG + ".saveData 2、正在写文件缓存，已处理：" + handledFileSize + "，总共需要处理：" + needHandleSize +
                        "，完成（百分比）：" + (MathUtil.formatNumber(((double) handledFileSize / needHandleSize) * 100)) +
                        "%，url：" + url);

                if (notifySaving(needNotifySize, needHandleSize)) {
                    needNotifySize = 0;
                    lastNotifyTime = curTime;
                }
            }

            // has been finished the file's total size
            if (needHandleSize == handledFileSize && tempFile.length() == mFileTotalSize) {
                if (saveFile.exists()) {// delete the file if exist
                    boolean deleteResult = saveFile.delete();
                    if (!deleteResult) {
                        throw new FileSaveException("delete old file:" + saveFile.getAbsolutePath() + " failed!", 
                                FileSaveException.TYPE_FILE_CAN_NOT_STORAGE);
                    }
                }
                isCompleted = tempFile.renameTo(saveFile);
                // rename temp file failed,may be the caller is using the temp file,however,try copy the temp file
                if (!isCompleted) {
                    // try copy the temp file to save file
                    isCompleted = FileUtil.copyFile(tempFile, saveFile, true);
                }
                // failed
                if (!isCompleted) {
                    // FIXME whether need throw exception ?
                    throw new FileSaveException("rename temp file:" + tempFile.getAbsolutePath() + " to save " +
                            saveFile.getAbsolutePath() + " failed!", FileSaveException.TYPE_RENAME_TEMP_FILE_ERROR);
                }

                filePath = saveFile.getAbsolutePath();

                Log.d(TAG, TAG + ".saveData 3、文件保存完成，路径：" + saveFile.getAbsolutePath() + "，url：" + url);
            }
            // interrupted(paused or error)
            else {
                // caller stopped
                if (!mIsStopped) {
                    // 5.error
                    throw new FileSaveException("saving data error!", FileSaveException.TYPE_UNKNOWN);
                }
            }

            hasException = false;// no exception
        } catch (Exception e) {
            e.printStackTrace();
            hasException = false;
            if (e instanceof FileSaveException) {
                throw (FileSaveException) e;
            } else {
                throw new FileSaveException(e);
            }
        } finally {
            // close the randomAccessFile if necessary
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // make sure to notify caller
            if (!hasException) {// if it has exception,has been thrown,no need to notify caller
                notifyEnd(needNotifySize, isCompleted);
            }

            // stopped
            mIsStopped = true;

            Log.d(TAG, TAG + ".saveData 3、文件保存【已结束】，是否有异常：" + hasException + "，保存路径：" + filePath + "，url：" + url);
        }
    }

    /**
     * notifyStart
     */
    private void notifyStart() {
        if (mOnFileSaveListener != null) {
            mOnFileSaveListener.onSaveDataStart();
        }

        Log.i(TAG, "file-downloader-save 准备开始保存数据");
    }

    /**
     * notifySaving
     */
    private boolean notifySaving(int needNotifySize, long needHandleSize) {
        if (mOnFileSaveListener != null) {
            mOnFileSaveListener.onSavingData(needNotifySize, needHandleSize);

            Log.i(TAG, "file-downloader-save 正在保存数据，needNotifySize：" + needNotifySize + "，needHandleSize：" +
                    needHandleSize);

            return true;// FIXME whether return true when mOnFileSaveListener is not null ?
        }
        return false;
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
        }

        mIsNotifyEnd = true;

        Log.i(TAG, "file-downloader-save 保存数据完成，是否整个文件全部下载完成：" + complete);
    }

    /**
     * checkIsStop
     */
    private void checkIsStop() throws FileSaveException {
        // if stopped,throw FileSaveException
        if (isStopped()) {

            Log.e(TAG, TAG + ".checkIsStop --已经处理完了/强制停止了，不能再处理数据！");

            throw new FileSaveException("the file saver has been stopped,it can not handle data any more!", 
                    FileSaveException.TYPE_SAVER_HAS_BEEN_STOPPED);
        }
    }

    /**
     * stop save file
     */
    @Override
    public void stop() {
        this.mIsStopped = true;// caller call to stop
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
        /**
         * rename temp file failed
         */
        public static final String TYPE_RENAME_TEMP_FILE_ERROR = FileSaveException.class.getName() + 
                "_TYPE_RENAME_TEMP_FILE_ERROR";
        /**
         * the file saver has been stopped
         */
        public static final String TYPE_SAVER_HAS_BEEN_STOPPED = FileSaveException.class.getName() + 
                "_TYPE_SAVER_HAS_BEEN_STOPPED";
        /**
         * the temp file does not exist
         */
        public static final String TYPE_TEMP_FILE_DOES_NOT_EXIST = FileSaveException.class.getName() + 
                "_TYPE_TEMP_FILE_DOES_NOT_EXIST";
        /**
         * file can not storage
         */
        public static final String TYPE_FILE_CAN_NOT_STORAGE = FileSaveException.class.getName() + 
                "_TYPE_FILE_CAN_NOT_STORAGE";

        public FileSaveException(String detailMessage, String type) {
            super(detailMessage, type);
        }

        public FileSaveException(Throwable throwable) {
            super(throwable);
        }

        @Override
        protected void onInitTypeWithThrowable(Throwable throwable) {
            super.onInitTypeWithThrowable(throwable);

            if (isTypeInit() || throwable == null) {
                return;
            }

            if (throwable instanceof FailReason) {
                FailReason failReason = (FailReason) throwable;
                setTypeByOriginalClassInstanceType(failReason.getOriginalCause());
                if (isTypeInit()) {
                    return;
                }
                // other FailReason exceptions that need cast to FileSaveException

            } else {
                setTypeByOriginalClassInstanceType(throwable);
            }
        }

        private void setTypeByOriginalClassInstanceType(Throwable throwable) {
            if (throwable == null) {
                return;
            }
            if (throwable instanceof IOException) {
                // setType(TYPE_FILE_CAN_NOT_STORAGE);
            }
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
        void onSavingData(int increaseSize, long totalSize);

        /**
         * finish saving data
         *
         * @param increaseSize increaseSize
         * @param complete     whether complete the file's total size
         */
        void onSaveDataEnd(int increaseSize, boolean complete);
    }
}
