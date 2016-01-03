package org.wlf.filedownloader_demo2.custom_model;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloader;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader_demo2.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.tedyin.circleprogressbarlib.CircleProgressBar;

/**
 * Demo2 Test Download List Adapter
 * <br/>
 * 测试2下载列表适配器
 *
 * @author wlf(Andy)
 * @datetime 2015-12-05 10:11 GMT+8
 * @email 411086563@qq.com
 */
public class CustomDownloadFileListAdapter extends BaseAdapter implements OnFileDownloadStatusListener {

    private Context mContext;

    // model data
    private List<CustomVideoInfo> mCustomVideoInfos = new ArrayList<CustomVideoInfo>();
    // cached convert views
    private Map<String, WeakReference<View>> mConvertViews = new LinkedHashMap<String, WeakReference<View>>();

    private Toast mToast;

    public CustomDownloadFileListAdapter(Context context, List<CustomVideoInfo> customVideoInfos) {
        mContext = context;
        mCustomVideoInfos = customVideoInfos;
    }

    @Override
    public int getCount() {
        return mCustomVideoInfos.size();
    }

    @Override
    public CustomVideoInfo getItem(int position) {
        return mCustomVideoInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        CustomVideoInfo customVideoInfo = getItem(position);
        if (customVideoInfo == null || TextUtils.isEmpty(customVideoInfo.getUrl())) {
            return null;
        }

        Log.e("wlf", "getView,customVideoInfo.url:" + customVideoInfo.getUrl());

        String url = customVideoInfo.getUrl();
        if (TextUtils.isEmpty(url)) {
            mConvertViews.remove(url);
        }

        View cacheConvertView = null;

        WeakReference<View> weakCacheConvertView = mConvertViews.get(url);

        if (weakCacheConvertView == null) {
            // not exist
            cacheConvertView = View.inflate(parent.getContext(), R.layout.custom_model__item_download, null);
            mConvertViews.put(url, new WeakReference<View>(cacheConvertView));
        } else {
            cacheConvertView = weakCacheConvertView.get();
            if (cacheConvertView == null) {
                // not exist
                cacheConvertView = View.inflate(parent.getContext(), R.layout.custom_model__item_download, null);
                mConvertViews.put(url, new WeakReference<View>(cacheConvertView));
            }
        }

        Log.e("wlf", "getView,cacheConvertView:" + cacheConvertView);

        LinearLayout lnlyDownloadItem = (LinearLayout) cacheConvertView.findViewById(R.id.lnlyDownloadItem);
        registerClickListener(lnlyDownloadItem, customVideoInfo);

        CircleProgressBar barProgress = (CircleProgressBar) cacheConvertView.findViewById(R.id.barProgress);

        TextView tvText = (TextView) cacheConvertView.findViewById(R.id.tvText);
        tvText.setText(customVideoInfo.getUrl());

        // has been started download
        if (customVideoInfo.getDownloadFileInfo() != null) {

            DownloadFileInfo downloadFileInfo = customVideoInfo.getDownloadFileInfo();

            double percent = ((double) downloadFileInfo.getDownloadedSizeLong()) / ((double) downloadFileInfo
                    .getFileSizeLong());
            barProgress.setProgress((int) (percent * 100));

            switch (downloadFileInfo.getStatus()) {
                // download file status:unknown
                case Status.DOWNLOAD_STATUS_UNKNOWN:
                    break;
                // download file status:preparing
                case Status.DOWNLOAD_STATUS_PREPARING:
                    break;
                // download file status:prepared
                case Status.DOWNLOAD_STATUS_PREPARED:
                    break;
                // download file status:paused
                case Status.DOWNLOAD_STATUS_PAUSED:
                    break;
                // download file status:downloading
                case Status.DOWNLOAD_STATUS_DOWNLOADING:
                    break;
                // download file status:error
                case Status.DOWNLOAD_STATUS_ERROR:
                    break;
                // download file status:waiting
                case Status.DOWNLOAD_STATUS_WAITING:
                    break;
                // download file status:completed
                case Status.DOWNLOAD_STATUS_COMPLETED:
                    break;
                // download file status:file not exist
                case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:
                    break;
            }
        }
        // never download
        else {
            barProgress.setProgress(0);
        }

        return cacheConvertView;
    }

    private void registerClickListener(View lnlyDownloadItem, final CustomVideoInfo customVideoInfo) {

        Log.e("wlf", "registerClickListener,lnlyDownloadItem:" + lnlyDownloadItem + ",customVideoInfo:" +
                customVideoInfo.getUrl());

        if (lnlyDownloadItem == null || customVideoInfo == null) {
            return;
        }

        LinearLayout lnlyBarProgress = (LinearLayout) lnlyDownloadItem.findViewById(R.id.lnlyBarProgress);
        LinearLayout lnlyDelete = (LinearLayout) lnlyDownloadItem.findViewById(R.id.lnlyDelete);

        if (lnlyBarProgress != null) {
            lnlyBarProgress.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {

                    Log.e("wlf", "onClick:" + customVideoInfo.getUrl());

                    // has been started download
                    if (customVideoInfo.getDownloadFileInfo() != null) {

                        final DownloadFileInfo downloadFileInfo = customVideoInfo.getDownloadFileInfo();

                        switch (downloadFileInfo.getStatus()) {

                            // download file status:unknown
                            case Status.DOWNLOAD_STATUS_UNKNOWN:
                                showToast(mContext.getString(R.string.custom_model__can_not_download2) +
                                        downloadFileInfo.getFilePath() + mContext.getString(R.string
                                        .custom_model__re_download));
                                break;

                            // download file status:error & paused
                            case Status.DOWNLOAD_STATUS_ERROR:
                            case Status.DOWNLOAD_STATUS_PAUSED:
                                FileDownloader.start(downloadFileInfo.getUrl());
                                break;

                            // download file status:file not exist
                            case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:
                                // show dialog
                                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                                builder.setTitle(mContext.getString(R.string.custom_model__whether_re_download))
                                        .setNegativeButton(mContext.getString(R.string
                                                .custom_model__dialog_btn_cancel), null);
                                builder.setPositiveButton(mContext.getString(R.string
                                        .custom_model__dialog_btn_confirm), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // re-download
                                        FileDownloader.reStart(downloadFileInfo.getUrl());
                                    }
                                });
                                builder.show();
                                break;

                            // download file status:waiting & preparing & prepared & downloading
                            case Status.DOWNLOAD_STATUS_WAITING:
                            case Status.DOWNLOAD_STATUS_PREPARING:
                            case Status.DOWNLOAD_STATUS_PREPARED:
                            case Status.DOWNLOAD_STATUS_DOWNLOADING:

                                Log.e("wlf", "pause:" + downloadFileInfo.getUrl());
                                // pause
                                FileDownloader.pause(downloadFileInfo.getUrl());
                                break;

                            // download file status:completed
                            case Status.DOWNLOAD_STATUS_COMPLETED:
                                showToast(mContext.getString(R.string.custom_model__download_finish));
                                break;
                        }

                    }
                    // never download
                    else {
                        FileDownloader.start(customVideoInfo.getUrl());
                    }

                }
            });
        }

        if (lnlyDelete != null) {
            lnlyDelete.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    DownloadFileInfo downloadFileInfo = customVideoInfo.getDownloadFileInfo();
                    if (downloadFileInfo == null) {
                        return;
                    }
                    FileDownloader.delete(downloadFileInfo.getUrl(), true, new OnDeleteDownloadFileListener() {
                        @Override
                        public void onDeleteDownloadFilePrepared(DownloadFileInfo downloadFileNeedDelete) {

                        }

                        @Override
                        public void onDeleteDownloadFileSuccess(DownloadFileInfo downloadFileDeleted) {

                            showToast(mContext.getString(R.string.custom_model__delete_succeed));

                            mCustomVideoInfos.remove(customVideoInfo);
                            mConvertViews.remove(customVideoInfo.getUrl());
                            notifyDataSetChanged();
                        }

                        @Override
                        public void onDeleteDownloadFileFailed(DownloadFileInfo downloadFileInfo, DeleteDownloadFileFailReason failReason) {

                        }
                    });
                }
            });
        }

    }

    // show toast
    private void showToast(CharSequence text) {
        if (mToast == null) {
            mToast = Toast.makeText(mContext.getApplicationContext(), text, Toast.LENGTH_SHORT);
        } else {
            mToast.cancel();
            mToast = Toast.makeText(mContext.getApplicationContext(), text, Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    @Override
    public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return;
        }
        showToast(mContext.getString(R.string.custom_model__waiting) + ",url:" + downloadFileInfo.getUrl());
    }

    @Override
    public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return;
        }
        showToast(mContext.getString(R.string.custom_model__preparing) + ",url:" + downloadFileInfo.getUrl());
    }

    @Override
    public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return;
        }
        showToast(mContext.getString(R.string.custom_model__prepared) + ",url:" + downloadFileInfo.getUrl());
    }

    @Override
    public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long 
            remainingTime) {

        if (downloadFileInfo == null) {
            return;
        }

        WeakReference<View> weakCacheConvertView = mConvertViews.get(downloadFileInfo.getUrl());
        if (weakCacheConvertView == null || weakCacheConvertView.get() == null) {
            return;
        }

        View cacheConvertView = weakCacheConvertView.get();
        CircleProgressBar barProgress = (CircleProgressBar) cacheConvertView.findViewById(R.id.barProgress);
        if (barProgress == null) {
            return;
        }

        double percent = ((double) downloadFileInfo.getDownloadedSizeLong()) / ((double) downloadFileInfo
                .getFileSizeLong());
        barProgress.setProgress((int) (percent * 100));

        Log.i("wlf", "update barProgress:" + percent);
    }

    @Override
    public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return;
        }
        showToast(mContext.getString(R.string.custom_model__paused) + ",url:" + downloadFileInfo.getUrl());
    }

    @Override
    public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return;
        }
        showToast(mContext.getString(R.string.custom_model__download_finish) + ",url:" + downloadFileInfo.getUrl());
    }

    @Override
    public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, FileDownloadStatusFailReason failReason) {

        if (failReason != null) {

            String type = mContext.getString(R.string.custom_model__download_fail_type) + failReason.getType();
            String reason = failReason.getOriginalCause() == null ? "" : failReason.getOriginalCause().getClass()
                    .getSimpleName();

            showToast(mContext.getString(R.string.custom_model__download_failed) + type + "," + reason + ",url:" + url);
        } else {
            showToast(mContext.getString(R.string.custom_model__download_failed) + ",url:" + url);
        }

    }

    public void release() {
        for (CustomVideoInfo customVideoInfo : mCustomVideoInfos) {
            if (customVideoInfo == null) {
                continue;
            }
            customVideoInfo.release();
        }
    }
}
