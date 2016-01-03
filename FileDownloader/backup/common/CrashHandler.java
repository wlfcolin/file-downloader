package org.wlf.common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * UncaughtException处理实现类，当程序发生Uncaught异常的时候，用该类来记录崩溃日志
 * <p/>
 * 使用该类前需要确保满足以下条件：
 * <p/>
 * <li>
 * 可能需要android.permission.WRITE_EXTERNAL_STORAGE和android.permission.
 * MOUNT_UNMOUNT_FILESYSTEMS权限，因为要写日志
 * <p/>
 * <li>在应用的Application的onCreate方法中需要调用 {@link #init(Context, String)}
 * 把当前应用的ApplicationContext和崩溃日志写入位置设置进来
 */
public class CrashHandler implements UncaughtExceptionHandler {

    /**
     * 当传入路径为空时默认崩溃文件夹名
     */
    private static final String DEFAULT_CRASH_DIR = "crash";

    /**
     * 崩溃日志保存路径
     */
    private String crashLogDirPath;

    /**
     * 崩溃日志文件名称的日期格式化器
     */
    private static final SimpleDateFormat LOG_FILE_NAME_DATE_FORMAT = new SimpleDateFormat("yyyy" + "-MM-dd-HH-mm-ss", Locale.getDefault());

    /**
     * 系统默认的UncaughtException处理类
     */
    private UncaughtExceptionHandler defaultHandler;
    /**
     * ApplicationContext上下文
     */
    private Context context;
    /**
     * 用来存储设备信息
     */
    private Map<String, String> deviceInfos = new HashMap<String, String>();
    /**
     * 用来存储应用信息
     */
    private Map<String, String> appInfos = new HashMap<String, String>();

    /**
     * 获取到了异常
     */
    private OnCaughtExceptionListener onCaughtExceptionListener;

    public void setOnCaughtExceptionListener(OnCaughtExceptionListener onCaughtExceptionListener) {
        this.onCaughtExceptionListener = onCaughtExceptionListener;
    }

    /**
     * 构造方法私有化，使用{@link #init(Context, String)}进行实例化一个对象
     *
     * @param appContext      当前应用的ApplicationContext
     * @param crashLogDirPath 日志存储目录
     */
    private CrashHandler(Context appContext, String crashLogDirPath) {
        this.context = appContext.getApplicationContext();
        // 获取系统默认的UncaughtException处理器
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);

        this.crashLogDirPath = crashLogDirPath;
        if (TextUtils.isEmpty(this.crashLogDirPath)) {
            this.crashLogDirPath = this.context.getCacheDir().getAbsolutePath() + File.separator + DEFAULT_CRASH_DIR;
        }
    }

    /**
     * 初始化，在哪个线程调用则会捕获哪个线程的未处理异常，通常在Application的onCreate方法中注册
     *
     * @param appContext      当前应用的ApplicationContext
     * @param crashLogDirPath 日志存储目录，若为空，则使用应用的缓存目录
     * @return 当前CrashHandler实例
     */
    public static final CrashHandler init(Context appContext, String crashLogDirPath) {
        return new CrashHandler(appContext, crashLogDirPath);
    }

    // 当UncaughtException发生时会转入该函数来处理
    @Override
    public final void uncaughtException(Thread thread, final Throwable ex) {

        boolean isHandleByCustom = false;

        // 自己处理
        if (defaultHandler == null || handleException(ex)) {

            // 必须终止掉应用
            if (defaultHandler == null || ex instanceof RuntimeException) {

                final String crashNote = "Sorry, the application has encountered a serious mistake, will exit!";

                // 使用Toast来显示崩溃提示
                new Thread() {
                    @Override
                    public void run() {
                        // 使用Looper效率不高
                        Looper.prepare();
                        Toast.makeText(context.getApplicationContext(), crashNote, Toast.LENGTH_LONG).show();
                        Looper.loop();
                    }
                }.start();

                isHandleByCustom = true;

                if (onCaughtExceptionListener != null) {
                    onCaughtExceptionListener.onCaughtException(thread, ex, isHandleByCustom);
                }

                // 将异常信息写入日志
                if (ex != null) {
                    Log.e(Log.TAG_EROOR, crashNote + "，e：" + ex.getMessage());
                }
                // 打印异常信息到控制台
                ex.printStackTrace();

                // 睡眠2秒后退出App，FIXME 睡眠是否考虑用子线程操作？
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 退出程序
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
            // 让系统默认的异常处理器来处理
            else {
                defaultHandler.uncaughtException(thread, ex);
                if (onCaughtExceptionListener != null) {
                    onCaughtExceptionListener.onCaughtException(thread, ex, isHandleByCustom);
                }
            }
        }
        // 让系统默认的异常处理器来处理
        else {
            defaultHandler.uncaughtException(thread, ex);
            if (onCaughtExceptionListener != null) {
                onCaughtExceptionListener.onCaughtException(thread, ex, isHandleByCustom);
            }
        }

    }

    /**
     * 自定义错误处理，收集错误信息，发送错误报告等操作均在此完成
     *
     * @param ex 异常信息
     * @return true表示处理了该异常信息，否则返回false
     */
    protected synchronized boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        // 收集崩溃信息
        collectCrashInfo();
        // 保存崩溃日志文件
        String crashLog = saveCrashLog(ex);

        // 清除
        deviceInfos.clear();
        appInfos.clear();

        return crashLog == null ? false : true;
    }

    /**
     * 收集崩溃信息
     */
    private void collectCrashInfo() {
        if (context == null) {
            return;
        }
        // 收集设备信息
        try {
            Field[] fields = Build.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                deviceInfos.put(field.getName(), field.get(null).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 收集应用信息
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                appInfos.put("packageName", pi.packageName);
                appInfos.put("versionName", pi.versionName);
                appInfos.put("versionCode", pi.versionCode + "");
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存错误信息到文件中
     *
     * @param ex 异常信息
     * @return 返回文件路径
     */
    private String saveCrashLog(Throwable ex) {
        StringBuffer sb = new StringBuffer();
        // 写设备信息
        sb.append("-----设备信息-----\n");
        for (Entry<String, String> entry : deviceInfos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }
        // 写应用信息
        sb.append("\n\n-----应用信息-----\n");
        for (Entry<String, String> entry : appInfos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        // 写崩溃信息
        sb.append("\n\n-----崩溃信息-----\n");
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);

        String crashFilePath = null;
        FileOutputStream fos = null;

        // 保存崩溃日志到文件
        try {
            String time = LOG_FILE_NAME_DATE_FORMAT.format(new Date());
            long timestamp = System.currentTimeMillis();
            // 崩溃日志名称保存格式为以“crash_”开头，崩溃时间和时间戳，以“.log”结尾
            String crashFileName = "crash_" + time + "_" + timestamp + ".log";

            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File dir = new File(crashLogDirPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                // 检查日志空间大小，并将空间降到合理区别
                LogHelper.checkLogFileDirSize(dir);

                // 保存文件路径
                crashFilePath = crashLogDirPath + File.separator + crashFileName;

                fos = new FileOutputStream(crashFilePath);
                fos.write(sb.toString().getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return crashFilePath;
    }

    public interface OnCaughtExceptionListener {
        void onCaughtException(Thread thread, final Throwable throwable, boolean isHandleByCustom);
    }

}
