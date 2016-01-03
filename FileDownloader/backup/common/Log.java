package org.wlf.common;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * the wrapper of {@link android.util.Log},to use {@link #addDebugTag(String)}, {@link #addReleaseTag(String)} and
 * {@link #addIgnoreTag(String)} can do some advanced operations that {@link android.util.Log} can not do
 * <br/>
 * before to use the class, make sure you have do the rules below:
 * <br/>
 * <li>
 * 1.add permission: android.permission.WRITE_EXTERNAL_STORAGE and android.permission.MOUNT_UNMOUNT_FILESYSTEMS,because
 * the class need access file system
 * <br/>
 * <li>
 * 2.init the class with {@link #init(Context, String, boolean)} in Application's onCreate() method
 * <p/>
 * 写日志的类，对{@link android.util.Log}的进一步封装，将一些特殊标签（见{@link #addDebugTag(String)}和{@link #addReleaseTag(String)
 * }）日志记录到到文件中，可以根据tag标签进行日志过滤（见{@link #addIgnoreTag(String)}）
 * <br/>
 * 使用该类前需要确保满足以下条件：
 * <br/>
 * <li>
 * 1、可能需要android.permission.WRITE_EXTERNAL_STORAGE和android.permission.MOUNT_UNMOUNT_FILESYSTEMS权限，因为要写日志
 * <br/>
 * <li>
 * 2、在应用的Application的onCreate方法中需要调用{@link #init(Context, String, boolean)}把当前应用的ApplicationContext和日志写入位置等设置进来。
 *
 * @author wlf
 * @email 411086563@qq.com
 */
public final class Log {

    /**
     * TAG
     */
    private static final String TAG = Log.class.getName();

    /**
     * the log file prefix, the final log file name will be like: log_20131010_101010111.log
     */
    private static final String LOG_FILE_NAME_PREFIX = "log_";
    /**
     * the log file suffix, the final log file name will be like: log_20131010_101010111.log
     */
    private static final String LOG_FILE_NAME_SUFFIX = ".log";
    /**
     * log mode: follow the system og, will not write log to file
     */
    private static final int LOG_MODE_SYSTEM = -1;
    /**
     * log mode: follow the system log, will not write log to file
     */
    private static final int LOG_MODE_DEBUG = 0;
    /**
     * log mode: release, will write log with tag in release tag list to file
     */
    private static final int LOG_MODE_RELEASE = 1;

    /**
     * single log file size
     */
    private static final long MAX_LOG_FILE_SIZE = 1024 * 512;
    /**
     * save datetime format
     */
    private static final SimpleDateFormat LOG_DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", 
            Locale.getDefault());
    /**
     * 判断日期大小格式字符串
     */
    private static final String DATE_FORMAT_STR = "yyyyMMdd";
    /**
     * 判断时间大小格式字符串
     */
    private static final String TIME_FORMAT_STR = "HHmmssSSS";
    /**
     * 判断日期大小格式化器
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STR, Locale.getDefault());
    /**
     * 判断时间大小格式化器
     */
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat(TIME_FORMAT_STR, Locale.getDefault());
    /**
     * 解析日志文件名字格式化器
     */
    private static final SimpleDateFormat LOG_FILE_NAME_DATE_TIME_FORMAT = new SimpleDateFormat(DATE_FORMAT_STR + 
            TIME_FORMAT_STR, Locale.getDefault());
    /**
     * 最后创建日志文件的日期
     */
    private static String sCreateLogFileDate;
    /**
     * 最后创建日志文件的时间
     */
    private static String sCreateLogFileTime;
    /**
     * 日志文件
     */
    private static File sLogFile;
    /**
     * 日志输出流
     */
    private static FileOutputStream sLogOutputStream;
    /**
     * 是否能写日志到存储中
     */
    private static boolean sIsCanWriteLog = true;

    /**
     * 调试（debug）时要记录到日志文件的Tag标签集合
     */
    private static final Set<String> DEBUG_LOG_TAG_LIST = new HashSet<String>();
    /**
     * 发布（release）时要记录到日志文件的Tag标签集合
     */
    private static final Set<String> RELEASE_LOG_TAG_LIST = new HashSet<String>();
    /**
     * 直接忽略的日志Tag标签集合，即：不会写日志文件也不会输出到控制台
     */
    private static final Set<String> IGNORE_LOG_TAG_LIST = new HashSet<String>();
    /**
     * 记录日志的根目录
     */
    private static String sLogDirPath;
    /**
     * 日志模式默认为调试模式
     */
    private static int sLogMode = LOG_MODE_SYSTEM;

    /**
     * 调试标签，调试时默认会写入日志的tag标签，也可以通过修改{@link #addDebugTag(String)}增加新标签
     */
    public static final String TAG_DEBUG = "_debug";
    /**
     * 发布标签，发布时默认会写入日志的tag标签，也可以通过修改{@link #addReleaseTag(String)}增加新标签
     */
    public static final String TAG_RELEASE = "_release";
    /**
     * 错误标签，不管调试模式还是发布模式都会写入日志的tag标签，改标签用于记录非常严重的错误或者异常
     */
    public static final String TAG_EROOR = "_error";

    /**
     * 当传入路径为空时默认日志文件夹名
     */
    private static final String DEFAULT_LOG_DIR = "log";

    /**
     * 线程池，专门写日志，提高UI流畅度
     */
    private static ExecutorService sSingleThreadPool;

    /**
     * 初始化，在Application的onCreate方法中注册
     *
     * @param appContext 当前应用的ApplicationContext
     * @param logDirPath 写日志目录，若为空，则使用应用的缓存目录
     * @param isDebug    是否是调试模式，true为调试模式，false为发布模式
     */
    public static final void init(Context appContext, String logDirPath, boolean isDebug) {

        sLogDirPath = logDirPath;
        if (TextUtils.isEmpty(sLogDirPath)) {
            sLogDirPath = appContext.getApplicationContext().getCacheDir().getAbsolutePath() +
                    File.separator + DEFAULT_LOG_DIR;
        }

        sLogMode = isDebug ? LOG_MODE_DEBUG : LOG_MODE_RELEASE;

        if (sSingleThreadPool == null || sSingleThreadPool.isShutdown()) {
            sSingleThreadPool = Executors.newSingleThreadExecutor();//
            // 因为可能一个日志文件可能会同时写，所以做成单一线程池，即该线程池内的唯一个线程是专门负责写日志而生的
        }

        // 固定会写日志的标签
        DEBUG_LOG_TAG_LIST.add(TAG_DEBUG);
        RELEASE_LOG_TAG_LIST.add(TAG_RELEASE);
        DEBUG_LOG_TAG_LIST.add(TAG_EROOR);
        RELEASE_LOG_TAG_LIST.add(TAG_EROOR);
    }

    /**
     * 释放资源，并不是必须的，最好这样做
     */
    public static final void relese() {

        DEBUG_LOG_TAG_LIST.clear();
        RELEASE_LOG_TAG_LIST.clear();
        IGNORE_LOG_TAG_LIST.clear();

        sLogDirPath = null;

        if (sSingleThreadPool != null && !sSingleThreadPool.isShutdown()) {
            sSingleThreadPool.shutdown();
        }
    }

    /**
     * 增加调试tag。该tag标签的日志会写入文件中
     *
     * @param tag log的tag标签
     */
    public static final void addDebugTag(String tag) {
        if (TextUtils.isEmpty(tag)) {
            return;
        }
        DEBUG_LOG_TAG_LIST.add(tag);
    }

    /**
     * 增加发布tag。该tag标签的日志会写入文件中
     *
     * @param tag log的tag标签
     */
    public static final void addReleaseTag(String tag) {
        if (TextUtils.isEmpty(tag)) {
            return;
        }
        RELEASE_LOG_TAG_LIST.add(tag);
    }

    /**
     * 增加发布忽略tag。该tag标签的Log不会写日志文件也不会输出到控制台
     *
     * @param tag log的tag标签
     */
    public static final void addIgnoreTag(String tag) {
        if (TextUtils.isEmpty(tag)) {
            return;
        }
        IGNORE_LOG_TAG_LIST.add(tag);
    }

    /**
     * 检查是否需要写日志
     */
    private static void checkNeedWriteLog(String tag, String msg) {
        switch (sLogMode) {
            case LOG_MODE_SYSTEM:// 跟随系统
                break;
            case LOG_MODE_DEBUG:
                if (DEBUG_LOG_TAG_LIST.contains(tag)) {
                    write(msg);// 写入日志到文件
                }
                break;
            case LOG_MODE_RELEASE:
                if (RELEASE_LOG_TAG_LIST.contains(tag)) {
                    write(msg);// 写入日志到文件
                }
                break;
        }
    }

    /**
     * Send a {@link android.util.Log#VERBOSE} log message.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int v(String tag, String msg) {
        // 忽略
        if (IGNORE_LOG_TAG_LIST.contains(tag)) {
            return -1;
        }
        // 检查是否需要写日志
        checkNeedWriteLog(tag, msg);
        // 系统日志正常打印
        return android.util.Log.v(tag, msg == null ? "" : msg);
    }

    /**
     * Send a {@link android.util.Log#VERBOSE} log message and log the
     * exception.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int v(String tag, String msg, Throwable tr) {
        // 忽略
        if (IGNORE_LOG_TAG_LIST.contains(tag)) {
            return -1;
        }
        // 检查是否需要写日志
        checkNeedWriteLog(tag, msg + "\n" + android.util.Log.getStackTraceString(tr));
        // 系统日志正常打印
        return android.util.Log.v(tag, msg == null ? "" : msg, tr);
    }

    /**
     * Send a {@link android.util.Log#DEBUG} log message.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int d(String tag, String msg) {
        // 忽略
        if (IGNORE_LOG_TAG_LIST.contains(tag)) {
            return -1;
        }
        // 检查是否需要写日志
        checkNeedWriteLog(tag, msg);
        // 系统日志正常打印
        return android.util.Log.d(tag, msg == null ? "" : msg);
    }

    /**
     * Send a {@link android.util.Log#DEBUG} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int d(String tag, String msg, Throwable tr) {
        // 忽略
        if (IGNORE_LOG_TAG_LIST.contains(tag)) {
            return -1;
        }
        // 检查是否需要写日志
        checkNeedWriteLog(tag, msg + "\n" + android.util.Log.getStackTraceString(tr));
        // 系统日志正常打印
        return android.util.Log.d(tag, msg == null ? "" : msg, tr);
    }

    /**
     * Send an {@link android.util.Log#INFO} log message.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int i(String tag, String msg) {
        // 忽略
        if (IGNORE_LOG_TAG_LIST.contains(tag)) {
            return -1;
        }
        // 检查是否需要写日志
        checkNeedWriteLog(tag, msg);
        // 系统日志正常打印
        return android.util.Log.i(tag, msg == null ? "" : msg);
    }

    /**
     * Send a {@link android.util.Log#INFO} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int i(String tag, String msg, Throwable tr) {
        // 忽略
        if (IGNORE_LOG_TAG_LIST.contains(tag)) {
            return -1;
        }
        // 检查是否需要写日志
        checkNeedWriteLog(tag, msg + "\n" + android.util.Log.getStackTraceString(tr));
        // 系统日志正常打印
        return android.util.Log.i(tag, msg == null ? "" : msg, tr);
    }

    /**
     * Send a {@link android.util.Log#WARN} log message.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int w(String tag, String msg) {
        // 忽略
        if (IGNORE_LOG_TAG_LIST.contains(tag)) {
            return -1;
        }
        // 检查是否需要写日志
        checkNeedWriteLog(tag, msg);
        // 系统日志正常打印
        return android.util.Log.w(tag, msg == null ? "" : msg);
    }

    /**
     * Send a {@link android.util.Log#WARN} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int w(String tag, String msg, Throwable tr) {
        // 忽略
        if (IGNORE_LOG_TAG_LIST.contains(tag)) {
            return -1;
        }
        // 检查是否需要写日志
        checkNeedWriteLog(tag, msg + "\n" + android.util.Log.getStackTraceString(tr));
        // 系统日志正常打印
        return android.util.Log.w(tag, msg == null ? "" : msg, tr);
    }

    /**
     * Send a {@link android.util.Log#WARN} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param tr  An exception to log
     */
    public static int w(String tag, Throwable tr) {
        // 忽略
        if (IGNORE_LOG_TAG_LIST.contains(tag)) {
            return -1;
        }
        // 检查是否需要写日志
        checkNeedWriteLog(tag, android.util.Log.getStackTraceString(tr));
        // 系统日志正常打印
        return android.util.Log.w(tag, tr);
    }

    /**
     * Send an {@link android.util.Log#ERROR} log message.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int e(String tag, String msg) {
        // 忽略
        if (IGNORE_LOG_TAG_LIST.contains(tag)) {
            return -1;
        }
        // 检查是否需要写日志
        checkNeedWriteLog(tag, msg);
        // 系统日志正常打印
        return android.util.Log.e(tag, msg == null ? "" : msg);
    }

    /**
     * Send a {@link android.util.Log#ERROR} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int e(String tag, String msg, Throwable tr) {
        // 忽略
        if (IGNORE_LOG_TAG_LIST.contains(tag)) {
            return -1;
        }
        // 检查是否需要写日志
        checkNeedWriteLog(tag, msg + "\n" + android.util.Log.getStackTraceString(tr));
        // 系统日志正常打印
        return android.util.Log.e(tag, msg == null ? "" : msg, tr);
    }

    /**
     * 将日志写到文件，当文件达到限制大小，或已经跨天时，创建新文件
     *
     * @param logMsg 日志内容
     */
    private static void write(final String logMsg) {
        sSingleThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String msg = logMsg;

                    // 能往SD卡写日志，流未创建，正在写的文件被删除，则创建文件
                    if (sIsCanWriteLog) {
                        if (sLogOutputStream == null || (sLogFile != null && !sLogFile.exists())) {
                            checkAndCreateLogFile();
                        }
                    }
                    // 流正常创建
                    if (sLogOutputStream != null) {
                        msg = LOG_DATE_TIME_FORMAT.format(new Date()) + "  " + msg + "\r\n";
                        sLogOutputStream.write(msg.getBytes());
                        sLogOutputStream.flush();

                        // 文件达到限制大小，或已经跨天时，创建新文件
                        if (sLogFile.length() >= MAX_LOG_FILE_SIZE || !DATE_FORMAT.format(new Date()).equals
                                (sCreateLogFileDate)) {
                            sLogOutputStream.close();
                            sLogOutputStream = null;
                        }
                    } else {
                        // 日志文件流未创建错误！
                        android.util.Log.e(TAG, "日志文件流未创建错误！");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    android.util.Log.e(TAG, e.getMessage(), e);
                }
            }
        });
    }

    /**
     * 检查写文件状态并创建日志文件
     */
    private static void checkAndCreateLogFile() {
        try {

            // 检查日志空间大小，并将空间降到合理区别
            LogHelper.checkLogFileDirSize(new File(sLogDirPath));

            // 需要往SD卡中写日志文件
            if (!TextUtils.isEmpty(sLogDirPath) && sLogDirPath.contains(Environment.getExternalStorageDirectory()
                    .getAbsolutePath())) {
                String sdStatus = Environment.getExternalStorageState();
                if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) { // 无SD卡则不写
                    sIsCanWriteLog = false;
                    // 创建日志文件失败，没有内存卡或者内存卡不可用！
                    android.util.Log.e(TAG, "创建日志文件失败，没有内存卡或者内存卡不可用！");
                } else { // 有SD卡，写文件
                    sIsCanWriteLog = true;
                }
            }
            // 往非SD卡写日志，尝试是否可以写
            else {
                File file = new File(sLogDirPath);
                if (!file.isDirectory()) {
                    if (!file.exists()) {
                        sIsCanWriteLog = file.mkdirs();
                    } else {
                        sIsCanWriteLog = file.canWrite();
                    }
                } else {
                    sIsCanWriteLog = file.canWrite();
                }
                if (!sIsCanWriteLog) {
                    // 创建日志文件失败，不能写入
                    android.util.Log.e(TAG, "创建日志文件失败，不能写入：" + sLogDirPath + "！");
                }
            }

            // 写日志
            if (sIsCanWriteLog) {
                createLogFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
            sIsCanWriteLog = false;
            android.util.Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * 创建日志文件
     */
    private static void createLogFile() throws Exception {

        File logDir = new File(sLogDirPath);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        // 是否需要创建新文件
        boolean isNeedCreateNewFile = true;
        // 判断当天的最后一个文件是否已写满，若未写满则继续写
        isNeedCreateNewFile = checkNeedCreateNewFile(logDir.listFiles());

        // 创建新文件
        if (isNeedCreateNewFile) {
            Date date = new Date();
            sCreateLogFileDate = DATE_FORMAT.format(date);
            sCreateLogFileTime = TIME_FORMAT.format(date);
            // 日志文件名称，如log_20131010_101010111.log格式
            String logFileName = LOG_FILE_NAME_PREFIX + sCreateLogFileDate + "_" +
                    sCreateLogFileTime + LOG_FILE_NAME_SUFFIX;

            File dirFile = new File(sLogDirPath);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
            }
            sLogFile = new File(dirFile, logFileName);
            // 不存在则创建
            if (!sLogFile.exists()) {
                sLogFile.createNewFile();
            }
            // 创建日志文件成功
            sLogOutputStream = new FileOutputStream(sLogFile);
            android.util.Log.i(TAG, "创建日志文件：" + sLogFile.getName() + " 成功！");
        }
        // 往旧文件写
        else {
            // 打开日志文件成功
            sLogOutputStream = new FileOutputStream(sLogFile, true);
            android.util.Log.i(TAG, "打开日志文件：" + sLogFile.getName() + " 成功！");
        }
    }

    /**
     * 检查是否需要创建新的日志文件
     *
     * @param fileList 检查目录下的文件列表
     * @return true：需要，false：不需要
     */
    private static boolean checkNeedCreateNewFile(File[] fileList) {
        boolean isNeedCreateNewFile = true;
        try {
            if (fileList != null && fileList.length > 0) {
                File newestFile = null; // 最新的日志文件
                Date newestData = null; // 最新的日志文件的创建时间

                // 遍历找出最新的日志文件
                for (int i = 0; i < fileList.length; i++) {
                    Date date = getLogFileCreateTime(fileList[i].getName());
                    if (date != null) {
                        if (newestData == null || date.getTime() > newestData.getTime()) {
                            newestData = (Date) date.clone();
                            newestFile = fileList[i];
                        }
                    }
                }

                if (newestFile != null && newestData != null) {
                    Date today = new Date();
                    if (newestData.getYear() == today.getYear() && newestData.getMonth() == today.getMonth() &&
                            newestData.getDate() == today.getDate() && newestFile.length() < MAX_LOG_FILE_SIZE) {
                        // 若最新的日志文件是今天创建，并且未写满
                        isNeedCreateNewFile = false;
                        sLogFile = newestFile;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e(TAG, e.getMessage(), e);
        }
        return isNeedCreateNewFile;
    }

    /**
     * 根据日志文件名称解析日志文件的创建日期
     *
     * @param fileName 日志文件名。格式：log_20131010_101010111.log
     * @return null：解析失败，不是日志文件。否则返回日志文件的创建日期
     */
    private static Date getLogFileCreateTime(String fileName) {
        Date date = null;
        try {
            if (fileName != null) {
                String startStr = LOG_FILE_NAME_PREFIX;
                String endStr = LOG_FILE_NAME_SUFFIX;
                if (fileName.startsWith(startStr) && fileName.endsWith(endStr)) {
                    // 如log_20131010_101010111.log格式
                    String dateStr = fileName.substring(startStr.length(), fileName.indexOf(endStr));
                    dateStr = dateStr.replace("_", "");// 如20131010_101010111格式的去掉"_"
                    date = LOG_FILE_NAME_DATE_TIME_FORMAT.parse(dateStr);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e(TAG, e.getMessage(), e);
        }
        return date;
    }
}
