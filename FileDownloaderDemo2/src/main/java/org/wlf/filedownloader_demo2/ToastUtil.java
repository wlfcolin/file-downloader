package org.wlf.filedownloader_demo2;

import android.content.Context;
import android.widget.Toast;

/**
 * show toast util
 *
 * @author wlf
 * @email 411086563@qq.com
 */
public class ToastUtil {

    private static Toast cacheToast = null;

    /**
     * show toast
     *
     * @param context
     * @param msg
     */
    public static final void showToast(Context context, String msg) {
        if (cacheToast != null) {
            cacheToast.cancel();
        }
        cacheToast = Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT);
        cacheToast.show();
        
    }
}
