package org.wlf.filedownloader.base;

/**
 * Custom Log class, a wrap of {@link android.util.Log}
 *
 * @author wlf(Andy)
 * @datetime 2016-01-11 17:12 GMT+8
 * @email 411086563@qq.com
 */
public final class Log {

    /**
     * debug mode flag, default is true
     */
    private static boolean sIsDebugMode = true;

    /**
     * set whether is debug mode
     *
     * @param isDebugMode whether is debug mode, default is true
     */
    public static void setDebugMode(boolean isDebugMode) {
        Log.sIsDebugMode = isDebugMode;
    }

    /**
     * Send a {@link android.util.Log#VERBOSE} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int v(String tag, String msg) {
        if (sIsDebugMode) {
            return android.util.Log.v(tag, msg);
        }
        return -1;
    }

    /**
     * Send a {@link android.util.Log#VERBOSE} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int v(String tag, String msg, Throwable tr) {
        if (sIsDebugMode) {
            return android.util.Log.v(tag, msg, tr);
        }
        return -1;
    }

    /**
     * Send a {@link android.util.Log#DEBUG} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int d(String tag, String msg) {
        if (sIsDebugMode) {
            return android.util.Log.d(tag, msg);
        }
        return -1;
    }

    /**
     * Send a {@link android.util.Log#DEBUG} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int d(String tag, String msg, Throwable tr) {
        if (sIsDebugMode) {
            return android.util.Log.d(tag, msg, tr);
        }
        return -1;
    }

    /**
     * Send an {@link android.util.Log#INFO} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int i(String tag, String msg) {
        if (sIsDebugMode) {
            return android.util.Log.i(tag, msg);
        }
        return -1;
    }

    /**
     * Send a {@link android.util.Log#INFO} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int i(String tag, String msg, Throwable tr) {
        if (sIsDebugMode) {
            return android.util.Log.i(tag, msg, tr);
        }
        return -1;
    }

    /**
     * Send a {@link android.util.Log#WARN} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int w(String tag, String msg) {
        if (sIsDebugMode) {
            return android.util.Log.w(tag, msg);
        }
        return -1;
    }

    /**
     * Send a {@link android.util.Log#WARN} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int w(String tag, String msg, Throwable tr) {
        if (sIsDebugMode) {
            return android.util.Log.w(tag, msg, tr);
        }
        return -1;
    }

    /**
     * Send a {@link android.util.Log#WARN} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param tr  An exception to log
     */
    public static int w(String tag, Throwable tr) {
        if (sIsDebugMode) {
            return android.util.Log.w(tag, tr);
        }
        return -1;
    }

    /**
     * Send an {@link android.util.Log#ERROR} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int e(String tag, String msg) {
        if (sIsDebugMode) {
            return android.util.Log.e(tag, msg);
        }
        return -1;
    }

    /**
     * Send a {@link android.util.Log#ERROR} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int e(String tag, String msg, Throwable tr) {
        if (sIsDebugMode) {
            return android.util.Log.e(tag, msg, tr);
        }
        return -1;
    }
}
