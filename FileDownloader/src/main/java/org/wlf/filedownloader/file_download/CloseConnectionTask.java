package org.wlf.filedownloader.file_download;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * CloseConnection Task
 * <br/>
 * 连接关闭的任务
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class CloseConnectionTask implements Runnable {

    private HttpURLConnection mURLConnection = null;
    private InputStream mInputStream = null;

    public CloseConnectionTask(HttpURLConnection urlConnection, InputStream inputStream) {
        mURLConnection = urlConnection;
        mInputStream = inputStream;
    }

    @Override
    public void run() {
        // close inputStream
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // close connection
        if (mURLConnection != null) {
            mURLConnection.disconnect();
        }
    }
}