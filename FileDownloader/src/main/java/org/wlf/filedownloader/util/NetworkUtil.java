package org.wlf.filedownloader.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * NetworkUtil
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class NetworkUtil {

    /**
     * is network available
     *
     * @param context Context
     * @return true means network is available
     */
    public static boolean isNetworkAvailable(Context context) {
        boolean isNetwork = false;
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return isNetwork;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        isNetwork = true;
                        break;
                    }
                }
            }
            return isNetwork;
        }
    }
}
