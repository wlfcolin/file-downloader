package org.wlf.filedownloader_demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloader;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.listener.OnRetryableFileDownloadStatusListener;
import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader_demo.util.ApkUtil;
import org.wlf.filedownloader_demo.util.TimeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo Test Download List Adapter
 * <br/>
 * 测试下载列表适配器
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DownloadFileListAdapter extends BaseAdapter implements OnRetryableFileDownloadStatusListener {

    /**
     * LOG TAG
     */
    private static final String TAG = DownloadFileListAdapter.class.getSimpleName();

    // all download infos
    private List<DownloadFileInfo> mDownloadFileInfos = Collections.synchronizedList(new ArrayList<DownloadFileInfo>());
    // cached convert views
    private Map<String, View> mConvertViews = new LinkedHashMap<String, View>();
    // select download file infos
    private List<DownloadFileInfo> mSelectedDownloadFileInfos = new ArrayList<DownloadFileInfo>();

    private Activity mActivity;

    private Toast mToast;

    private OnItemSelectListener mOnItemSelectListener;

    public DownloadFileListAdapter(Activity activity) {
        super();
        this.mActivity = activity;
        initDownloadFileInfos();
    }

    // init DownloadFileInfos
    private void initDownloadFileInfos() {
        this.mDownloadFileInfos = FileDownloader.getDownloadFiles();
        mConvertViews.clear();
        mSelectedDownloadFileInfos.clear();
        if (mOnItemSelectListener != null) {
            mOnItemSelectListener.onNoneSelect();
        }
    }

    /**
     * update show
     */
    public void updateShow() {
        initDownloadFileInfos();
        notifyDataSetChanged();
    }

    public void setOnItemSelectListener(OnItemSelectListener onItemSelectListener) {
        this.mOnItemSelectListener = onItemSelectListener;
    }

    @Override
    public int getCount() {
        return mDownloadFileInfos.size();
    }

    @Override
    public DownloadFileInfo getItem(int position) {
        return mDownloadFileInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        DownloadFileInfo downloadFileInfo = getItem(position);

        if (downloadFileInfo == null) {
            return null;
        }

        final String url = downloadFileInfo.getUrl();

        if (TextUtils.isEmpty(url)) {
            mConvertViews.remove(url);
            return null;
        }

        View cacheConvertView = mConvertViews.get(url);

        if (cacheConvertView == null) {
            cacheConvertView = View.inflate(parent.getContext(), R.layout.main__item_download, null);
            mConvertViews.put(url, cacheConvertView);
        }

        LinearLayout lnlyDownloadItem = (LinearLayout) cacheConvertView.findViewById(R.id.lnlyDownloadItem);
        ImageView ivIcon = (ImageView) cacheConvertView.findViewById(R.id.ivIcon);
        TextView tvFileName = (TextView) cacheConvertView.findViewById(R.id.tvFileName);
        ProgressBar pbProgress = (ProgressBar) cacheConvertView.findViewById(R.id.pbProgress);
        TextView tvDownloadSize = (TextView) cacheConvertView.findViewById(R.id.tvDownloadSize);
        TextView tvTotalSize = (TextView) cacheConvertView.findViewById(R.id.tvTotalSize);
        TextView tvPercent = (TextView) cacheConvertView.findViewById(R.id.tvPercent);
        TextView tvText = (TextView) cacheConvertView.findViewById(R.id.tvText);
        CheckBox cbSelect = (CheckBox) cacheConvertView.findViewById(R.id.cbSelect);

        if ("apk".equalsIgnoreCase(FileUtil.getFileSuffix(downloadFileInfo.getFileName()))) {// apk
            ivIcon.setImageResource(R.mipmap.main__ic_apk);
        } else {
            ivIcon.setImageResource(R.mipmap.ic_launcher);
        }

        // file name
        tvFileName.setText(downloadFileInfo.getFileName());

        // download progress
        int totalSize = (int) downloadFileInfo.getFileSizeLong();
        int downloaded = (int) downloadFileInfo.getDownloadedSizeLong();
        double rate = (double) totalSize / Integer.MAX_VALUE;
        if (rate > 1.0) {
            totalSize = Integer.MAX_VALUE;
            downloaded = (int) (downloaded / rate);
        }

        pbProgress.setMax(totalSize);
        pbProgress.setProgress(downloaded);

        // file size
        double downloadSize = downloadFileInfo.getDownloadedSizeLong() / 1024f / 1024;
        double fileSize = downloadFileInfo.getFileSizeLong() / 1024f / 1024;

        tvDownloadSize.setText(((float) (Math.round(downloadSize * 100)) / 100) + "M/");
        tvTotalSize.setText(((float) (Math.round(fileSize * 100)) / 100) + "M");

        // downloaded percent
        double percent = downloadSize / fileSize * 100;
        tvPercent.setText(((float) (Math.round(percent * 100)) / 100) + "%");

        final Context context = cacheConvertView.getContext();

        switch (downloadFileInfo.getStatus()) {
            // download file status:unknown
            case Status.DOWNLOAD_STATUS_UNKNOWN:
                tvText.setText(context.getString(R.string.main__can_not_download));
                break;
            // download file status:retrying
            case Status.DOWNLOAD_STATUS_RETRYING:
                tvText.setText(context.getString(R.string.main__retrying_connect_resource));
                break;
            // download file status:preparing
            case Status.DOWNLOAD_STATUS_PREPARING:
                tvText.setText(context.getString(R.string.main__getting_resource));
                break;
            // download file status:prepared
            case Status.DOWNLOAD_STATUS_PREPARED:
                tvText.setText(context.getString(R.string.main__connected_resource));
                break;
            // download file status:paused
            case Status.DOWNLOAD_STATUS_PAUSED:
                tvText.setText(context.getString(R.string.main__paused));
                break;
            // download file status:downloading
            case Status.DOWNLOAD_STATUS_DOWNLOADING:
                if (tvText.getTag() != null) {
                    tvText.setText((String) tvText.getTag());
                } else {
                    tvText.setText(context.getString(R.string.main__downloading));
                }
                break;
            // download file status:error
            case Status.DOWNLOAD_STATUS_ERROR:
                tvText.setText(context.getString(R.string.main__download_error));
                break;
            // download file status:waiting
            case Status.DOWNLOAD_STATUS_WAITING:
                tvText.setText(context.getString(R.string.main__waiting));
                break;
            // download file status:completed
            case Status.DOWNLOAD_STATUS_COMPLETED:
                tvDownloadSize.setText("");
                if ("apk".equalsIgnoreCase(FileUtil.getFileSuffix(downloadFileInfo.getFileName()))) {// apk
                    String packageName = ApkUtil.getUnInstallApkPackageName(mActivity, downloadFileInfo.getFilePath());
                    boolean isInstall = ApkUtil.checkAppInstalled(mActivity, packageName);
                    if (isInstall) {
                        tvText.setText(context.getString(R.string.main__open));
                    } else {
                        tvText.setText(context.getString(R.string.main__not_install));
                    }
                } else {
                    tvText.setText(context.getString(R.string.main__download_completed));
                }
                break;
            // download file status:file not exist
            case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:
                tvDownloadSize.setText("");
                tvText.setText(context.getString(R.string.main__file_not_exist));
                break;
        }

        cbSelect.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSelectedDownloadFileInfos.add(FileDownloader.getDownloadFile(url));
                    if (mOnItemSelectListener != null) {
                        // select a download file
                        mOnItemSelectListener.onSelected(mSelectedDownloadFileInfos);
                    }
                } else {
                    mSelectedDownloadFileInfos.remove(FileDownloader.getDownloadFile(url));
                    if (mSelectedDownloadFileInfos.isEmpty()) {
                        if (mOnItemSelectListener != null) {
                            // select none
                            mOnItemSelectListener.onNoneSelect();
                        }
                    } else {
                        if (mOnItemSelectListener != null) {
                            // select a download file
                            mOnItemSelectListener.onSelected(mSelectedDownloadFileInfos);
                        }
                    }
                }
            }
        });

        // set convertView click
        setBackgroundOnClickListener(lnlyDownloadItem, downloadFileInfo);
        return cacheConvertView;
    }

    // set convertView click
    private void setBackgroundOnClickListener(final View lnlyDownloadItem, final DownloadFileInfo curDownloadFileInfo) {

        lnlyDownloadItem.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final Context context = v.getContext();
                if (curDownloadFileInfo != null) {
                    switch (curDownloadFileInfo.getStatus()) {
                        // download file status:unknown
                        case Status.DOWNLOAD_STATUS_UNKNOWN:
                            showToast(context.getString(R.string.main__can_not_download2) + curDownloadFileInfo
                                    .getFilePath() + context.getString(R.string.main__re_download));
                            break;
                        // download file status:error & paused
                        case Status.DOWNLOAD_STATUS_ERROR:
                        case Status.DOWNLOAD_STATUS_PAUSED:
                            FileDownloader.start(curDownloadFileInfo.getUrl());
                            showToast(context.getString(R.string.main__start_download) + curDownloadFileInfo
                                    .getFileName());
                            break;
                        // download file status:file not exist
                        case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:
                            // show dialog
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle(context.getString(R.string.main__whether_re_download)).setNegativeButton
                                    (context.getString(R.string.main__dialog_btn_cancel), null);
                            builder.setPositiveButton(context.getString(R.string.main__dialog_btn_confirm), new 
                                    DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // re-download
                                    FileDownloader.reStart(curDownloadFileInfo.getUrl());
                                    showToast(context.getString(R.string.main__re_download2) + curDownloadFileInfo
                                            .getFileName());
                                }
                            });
                            builder.show();
                            break;
                        // download file status:retrying & waiting & preparing & prepared & downloading
                        case Status.DOWNLOAD_STATUS_RETRYING:
                        case Status.DOWNLOAD_STATUS_WAITING:
                        case Status.DOWNLOAD_STATUS_PREPARING:
                        case Status.DOWNLOAD_STATUS_PREPARED:
                        case Status.DOWNLOAD_STATUS_DOWNLOADING:
                            // pause
                            FileDownloader.pause(curDownloadFileInfo.getUrl());

                            showToast(context.getString(R.string.main__paused_download) + curDownloadFileInfo
                                    .getFileName());

                            TextView tvText = (TextView) lnlyDownloadItem.findViewById(R.id.tvText);
                            if (tvText != null) {
                                tvText.setText(context.getString(R.string.main__paused));
                            }
                            break;
                        // download file status:completed
                        case Status.DOWNLOAD_STATUS_COMPLETED:

                            TextView tvDownloadSize = (TextView) lnlyDownloadItem.findViewById(R.id.tvDownloadSize);
                            if (tvDownloadSize != null) {
                                tvDownloadSize.setText("");
                            }

                            final TextView tvText2 = (TextView) lnlyDownloadItem.findViewById(R.id.tvText);

                            if ("apk".equalsIgnoreCase(FileUtil.getFileSuffix(curDownloadFileInfo.getFileName()))) 
                            {// apk

                                final String packageName = ApkUtil.getUnInstallApkPackageName(context, 
                                        curDownloadFileInfo.getFilePath());
                                boolean isInstall = ApkUtil.checkAppInstalled(context, packageName);

                                if (isInstall) {
                                    if (tvText2 != null) {
                                        tvText2.setText(context.getString(R.string.main__open));
                                        try {
                                            Intent intent2 = mActivity.getPackageManager().getLaunchIntentForPackage
                                                    (packageName);
                                            mActivity.startActivity(intent2);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            // show install dialog
                                            ApkUtil.installApk(context, curDownloadFileInfo.getFilePath());
                                            showToast(context.getString(R.string.main__not_install_apk) + 
                                                    curDownloadFileInfo.getFileName());
                                            tvText2.setText(context.getString(R.string.main__no_install));
                                        }
                                    }
                                } else {
                                    if (tvText2 != null) {
                                        tvText2.setText(context.getString(R.string.main__no_install));
                                    }
                                    ApkUtil.installApk(context, curDownloadFileInfo.getFilePath());
                                    showToast(context.getString(R.string.main__not_install_apk2) + 
                                            curDownloadFileInfo.getFileName());
                                }
                            } else {
                                tvText2.setText(context.getString(R.string.main__download_completed));
                            }
                            break;
                    }
                }
            }
        });
    }

    // show toast
    private void showToast(CharSequence text) {
        if (mToast == null) {
            mToast = Toast.makeText(mActivity, text, Toast.LENGTH_SHORT);
        } else {
            mToast.cancel();
            mToast = Toast.makeText(mActivity, text, Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    /**
     * add new download file info
     */
    public boolean addNewDownloadFileInfo(DownloadFileInfo downloadFileInfo) {
        if (downloadFileInfo != null) {
            if (!mDownloadFileInfos.contains(downloadFileInfo)) {
                boolean isFind = false;
                for (DownloadFileInfo info : mDownloadFileInfos) {
                    if (info != null && info.getUrl().equals(downloadFileInfo.getUrl())) {
                        isFind = true;
                        break;
                    }
                }
                if (isFind) {
                    return false;
                }
                mDownloadFileInfos.add(downloadFileInfo);
                notifyDataSetChanged();
                return true;
            }
        }
        return false;
    }

    // ///////////////////////////////////////////////////////////

    // ----------------------download status callback----------------------
    @Override
    public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return;
        }

        // add
        if (addNewDownloadFileInfo(downloadFileInfo)) {
            // add succeed
            notifyDataSetChanged();
        } else {
            String url = downloadFileInfo.getUrl();
            View cacheConvertView = mConvertViews.get(url);
            if (cacheConvertView != null) {
                TextView tvText = (TextView) cacheConvertView.findViewById(R.id.tvText);
                tvText.setText(cacheConvertView.getContext().getString(R.string.main__waiting));

                Log.d(TAG, "onFileDownloadStatusWaiting url：" + url + "，status(正常应该是" + Status
                        .DOWNLOAD_STATUS_WAITING + ")：" + downloadFileInfo.getStatus());
            } else {
                updateShow();
            }
        }
    }

    @Override
    public void onFileDownloadStatusRetrying(DownloadFileInfo downloadFileInfo, int retryTimes) {

        if (downloadFileInfo == null) {
            return;
        }

        String url = downloadFileInfo.getUrl();
        View cacheConvertView = mConvertViews.get(url);
        if (cacheConvertView != null) {
            TextView tvText = (TextView) cacheConvertView.findViewById(R.id.tvText);
            tvText.setText(cacheConvertView.getContext().getString(R.string.main__retrying_connect_resource) + "(" +
                    retryTimes + ")");

            Log.d(TAG, "onFileDownloadStatusRetrying url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_RETRYING 
                    + ")：" + downloadFileInfo.getStatus());
        } else {
            updateShow();
        }
    }

    @Override
    public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return;
        }

        String url = downloadFileInfo.getUrl();
        View cacheConvertView = mConvertViews.get(url);
        if (cacheConvertView != null) {
            TextView tvText = (TextView) cacheConvertView.findViewById(R.id.tvText);
            tvText.setText(cacheConvertView.getContext().getString(R.string.main__getting_resource));

            Log.d(TAG, "onFileDownloadStatusPreparing url：" + url + "，status(正常应该是" + Status
                    .DOWNLOAD_STATUS_PREPARING + ")：" + downloadFileInfo.getStatus());
        } else {
            updateShow();
        }
    }

    @Override
    public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return;
        }

        String url = downloadFileInfo.getUrl();
        View cacheConvertView = mConvertViews.get(url);
        if (cacheConvertView != null) {
            TextView tvText = (TextView) cacheConvertView.findViewById(R.id.tvText);
            tvText.setText(cacheConvertView.getContext().getString(R.string.main__connected_resource));

            Log.d(TAG, "onFileDownloadStatusPrepared url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_PREPARED 
                    + ")：" + downloadFileInfo.getStatus());
        } else {
            updateShow();
        }
    }

    @Override
    public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long 
            remainingTime) {

        if (downloadFileInfo == null) {
            return;
        }

        String url = downloadFileInfo.getUrl();
        View cacheConvertView = mConvertViews.get(url);
        if (cacheConvertView != null) {

            LinearLayout lnlyDownloadItem = (LinearLayout) cacheConvertView.findViewById(R.id.lnlyDownloadItem);
            ProgressBar pbProgress = (ProgressBar) cacheConvertView.findViewById(R.id.pbProgress);
            TextView tvDownloadSize = (TextView) cacheConvertView.findViewById(R.id.tvDownloadSize);
            TextView tvTotalSize = (TextView) cacheConvertView.findViewById(R.id.tvTotalSize);
            TextView tvPercent = (TextView) cacheConvertView.findViewById(R.id.tvPercent);
            TextView tvText = (TextView) cacheConvertView.findViewById(R.id.tvText);

            // download progress
            int totalSize = (int) downloadFileInfo.getFileSizeLong();
            int downloaded = (int) downloadFileInfo.getDownloadedSizeLong();
            double rate = (double) totalSize / Integer.MAX_VALUE;
            if (rate > 1.0) {
                totalSize = Integer.MAX_VALUE;
                downloaded = (int) (downloaded / rate);
            }
            // pbProgress.setMax(totalSize);
            pbProgress.setProgress(downloaded);

            // download size
            double downloadSize = downloadFileInfo.getDownloadedSizeLong() / 1024f / 1024f;
            double fileSize = downloadFileInfo.getFileSizeLong() / 1024f / 1024f;

            tvDownloadSize.setText(((float) (Math.round(downloadSize * 100)) / 100) + "M/");
            tvTotalSize.setText(((float) (Math.round(fileSize * 100)) / 100) + "M");

            // download percent
            double percent = downloadSize / fileSize * 100;
            tvPercent.setText(((float) (Math.round(percent * 100)) / 100) + "%");

            // download speed and remain times
            String speed = ((float) (Math.round(downloadSpeed * 100)) / 100) + "KB/s" + "  " + TimeUtil
                    .seconds2HH_mm_ss(remainingTime);
            tvText.setText(speed);
            tvText.setTag(speed);

            setBackgroundOnClickListener(lnlyDownloadItem, downloadFileInfo);
        } else {
            updateShow();
        }

        Log.d(TAG, "onFileDownloadStatusDownloading url：" + url + "，status(正常应该是" + Status
                .DOWNLOAD_STATUS_DOWNLOADING + ")：" + downloadFileInfo.getStatus());
    }

    @Override
    public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return;
        }

        String url = downloadFileInfo.getUrl();

        Log.d(TAG, "onFileDownloadStatusPaused url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_PAUSED + ")：" +
                downloadFileInfo.getStatus());

        View cacheConvertView = mConvertViews.get(url);
        if (cacheConvertView != null) {

            LinearLayout lnlyDownloadItem = (LinearLayout) cacheConvertView.findViewById(R.id.lnlyDownloadItem);
            TextView tvText = (TextView) cacheConvertView.findViewById(R.id.tvText);

            tvText.setText(cacheConvertView.getContext().getString(R.string.main__paused));

            setBackgroundOnClickListener(lnlyDownloadItem, downloadFileInfo);
        } else {
            updateShow();
        }

    }

    @Override
    public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return;
        }

        String url = downloadFileInfo.getUrl();
        View cacheConvertView = mConvertViews.get(url);
        if (cacheConvertView != null) {

            LinearLayout lnlyDownloadItem = (LinearLayout) cacheConvertView.findViewById(R.id.lnlyDownloadItem);
            TextView tvDownloadSize = (TextView) cacheConvertView.findViewById(R.id.tvDownloadSize);
            TextView tvPercent = (TextView) cacheConvertView.findViewById(R.id.tvPercent);
            TextView tvText = (TextView) cacheConvertView.findViewById(R.id.tvText);

            tvDownloadSize.setText("");

            // download percent
            float percent = 1;
            tvPercent.setText(((float) (Math.round(percent * 100)) / 100 * 100) + "%");

            if (downloadFileInfo.getStatus() == Status.DOWNLOAD_STATUS_COMPLETED) {

                if ("apk".equalsIgnoreCase(FileUtil.getFileSuffix(downloadFileInfo.getFileName()))) {// apk
                    String packageName = ApkUtil.getUnInstallApkPackageName(mActivity, downloadFileInfo.getFilePath());
                    boolean isInstall = ApkUtil.checkAppInstalled(mActivity, packageName);

                    if (isInstall) {
                        tvText.setText(cacheConvertView.getContext().getString(R.string.main__open));
                    } else {
                        tvText.setText(cacheConvertView.getContext().getString(R.string.main__not_install));
                    }
                } else {
                    tvText.setText(cacheConvertView.getContext().getString(R.string.main__download_completed));
                }
            }

            setBackgroundOnClickListener(lnlyDownloadItem, downloadFileInfo);
        } else {
            updateShow();
        }

        Log.d(TAG, "onFileDownloadStatusCompleted url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_COMPLETED +
                ")：" + downloadFileInfo.getStatus());
    }

    @Override
    public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, 
                                           FileDownloadStatusFailReason failReason) {

        String msg = mActivity.getString(R.string.main__download_error);

        if (failReason != null) {
            if (FileDownloadStatusFailReason.TYPE_NETWORK_DENIED.equals(failReason.getType())) {
                msg += mActivity.getString(R.string.main__check_network);
            } else if (FileDownloadStatusFailReason.TYPE_URL_ILLEGAL.equals(failReason.getType())) {
                msg += mActivity.getString(R.string.main__url_illegal);
            } else if (FileDownloadStatusFailReason.TYPE_NETWORK_TIMEOUT.equals(failReason.getType())) {
                msg += mActivity.getString(R.string.main__network_timeout);
            } else if (FileDownloadStatusFailReason.TYPE_STORAGE_SPACE_IS_FULL.equals(failReason.getType())) {
                msg += mActivity.getString(R.string.main__storage_space_is_full);
            } else if (FileDownloadStatusFailReason.TYPE_STORAGE_SPACE_CAN_NOT_WRITE.equals(failReason.getType())) {
                msg += mActivity.getString(R.string.main__storage_space_can_not_write);
            } else if (FileDownloadStatusFailReason.TYPE_FILE_NOT_DETECT.equals(failReason.getType())) {
                msg += mActivity.getString(R.string.main__file_not_detect);
            } else if (FileDownloadStatusFailReason.TYPE_BAD_HTTP_RESPONSE_CODE.equals(failReason.getType())) {
                msg += mActivity.getString(R.string.main__http_bad_response_code);
            } else if (FileDownloadStatusFailReason.TYPE_HTTP_FILE_NOT_EXIST.equals(failReason.getType())) {
                msg += mActivity.getString(R.string.main__http_file_not_exist);
            } else if (FileDownloadStatusFailReason.TYPE_SAVE_FILE_NOT_EXIST.equals(failReason.getType())) {
                msg += mActivity.getString(R.string.main__save_file_not_exist);
            }
        }

        if (downloadFileInfo == null) {
            showToast(msg + "，url：" + url);
            return;
        }

        url = downloadFileInfo.getUrl();
        View cacheConvertView = mConvertViews.get(url);
        if (cacheConvertView != null) {

            LinearLayout lnlyDownloadItem = (LinearLayout) cacheConvertView.findViewById(R.id.lnlyDownloadItem);
            TextView tvText = (TextView) cacheConvertView.findViewById(R.id.tvText);

            tvText.setText(msg);
            showToast(msg + "，url：" + url);

            setBackgroundOnClickListener(lnlyDownloadItem, downloadFileInfo);

            Log.d(TAG, "onFileDownloadStatusFailed 出错回调，url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_ERROR 
                    + ")：" + downloadFileInfo.getStatus());
        } else {
            updateShow();
        }
    }

    /**
     * OnItemSelectListener
     */
    public interface OnItemSelectListener {

        void onSelected(List<DownloadFileInfo> selectDownloadFileInfos);

        void onNoneSelect();
    }

}
