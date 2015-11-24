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
import org.wlf.filedownloader.FileDownloadManager;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
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
public class DownloadFileListAdapter extends BaseAdapter implements OnFileDownloadStatusListener {

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
        this.mDownloadFileInfos = FileDownloadManager.getInstance(mActivity).getDownloadFiles();
        mConvertViews.clear();
        mSelectedDownloadFileInfos.clear();
        if(mOnItemSelectListener != null){
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
            cacheConvertView = View.inflate(parent.getContext(), R.layout.item_download, null);
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
            ivIcon.setImageResource(R.drawable.ic_apk);
        } else {
            ivIcon.setImageResource(R.drawable.ic_launcher);
        }

        // file name
        tvFileName.setText(downloadFileInfo.getFileName());

        // download progress
        pbProgress.setMax(downloadFileInfo.getFileSize());
        pbProgress.setProgress(downloadFileInfo.getDownloadedSize());

        // file size
        float downloadSize = downloadFileInfo.getDownloadedSize() / 1024f / 1024;
        float fileSize = downloadFileInfo.getFileSize() / 1024f / 1024;

        tvDownloadSize.setText(((float) (Math.round(downloadSize * 100)) / 100) + "M/");
        tvTotalSize.setText(((float) (Math.round(fileSize * 100)) / 100) + "M");

        // downloaded percent
        float percent = downloadSize / fileSize * 100;
        tvPercent.setText(((float) (Math.round(percent * 100)) / 100) + "%");

        final Context context = cacheConvertView.getContext();

        switch (downloadFileInfo.getStatus()) {
            // download file status:unknown
            case Status.DOWNLOAD_STATUS_UNKNOWN:
                tvText.setText(context.getString(R.string.main__can_not_download));
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
                tvText.setText(context.getString(R.string.main__downloading));
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
                        tvText.setText(context.getString(R.string.main__uninstall));
                    }
                } else {
                    tvText.setText(context.getString(R.string.main__download_completed));
                }
                break;
            // download file status:file not exist
            case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:
                tvDownloadSize.setText("");
                tvText.setText(context.getString(R.string.main__file_not_exsit));
                break;
        }

        cbSelect.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // fileDownloadManager
                final FileDownloadManager fileDownloadManager = FileDownloadManager.getInstance(buttonView.getContext());
                if (isChecked) {
                    mSelectedDownloadFileInfos.add(fileDownloadManager.getDownloadFileByUrl(url));
                    if (mOnItemSelectListener != null) {
                        // select a download file
                        mOnItemSelectListener.onSelected(mSelectedDownloadFileInfos);
                    }
                } else {
                    mSelectedDownloadFileInfos.remove(fileDownloadManager.getDownloadFileByUrl(url));
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

            // fileDownloadManager
            final FileDownloadManager fileDownloadManager = FileDownloadManager.getInstance(lnlyDownloadItem.getContext());
            // mOnFileDownloadStatusListener
            final OnFileDownloadStatusListener mOnFileDownloadStatusListener = DownloadFileListAdapter.this;

            @Override
            public void onClick(View v) {
                final Context context = v.getContext();
                if (curDownloadFileInfo != null) {
                    switch (curDownloadFileInfo.getStatus()) {
                        // download file status:unknown
                        case Status.DOWNLOAD_STATUS_UNKNOWN:
                            showToast(context.getString(R.string.main__can_not_download2) + curDownloadFileInfo.getFilePath() + context.getString(R.string.main__re_download));
                            break;
                        // download file status:error & paused
                        case Status.DOWNLOAD_STATUS_ERROR:
                        case Status.DOWNLOAD_STATUS_PAUSED:
                            fileDownloadManager.start(curDownloadFileInfo.getUrl(), mOnFileDownloadStatusListener);
                            showToast(context.getString(R.string.main__start_download) + curDownloadFileInfo.getFileName());
                            break;
                        // download file status:file not exist
                        case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:
                            // show dialog
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle(context.getString(R.string.main__whether_re_download)).setNegativeButton(context.getString(R.string.main__dialog_btn_cancel), null);
                            builder.setPositiveButton(context.getString(R.string.main__dialog_btn_confirm), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // re-download
                                    fileDownloadManager.reStart(curDownloadFileInfo.getUrl(), mOnFileDownloadStatusListener);
                                    showToast(context.getString(R.string.main__re_download2) + curDownloadFileInfo.getFileName());
                                }
                            });
                            builder.show();
                            break;
                        // download file status:waiting & preparing & prepared & downloading
                        case Status.DOWNLOAD_STATUS_WAITING:
                        case Status.DOWNLOAD_STATUS_PREPARING:
                        case Status.DOWNLOAD_STATUS_PREPARED:
                        case Status.DOWNLOAD_STATUS_DOWNLOADING:
                            // pause
                            fileDownloadManager.pause(curDownloadFileInfo.getUrl());

                            showToast(context.getString(R.string.main__paused_download) + curDownloadFileInfo.getFileName());

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

                            if ("apk".equalsIgnoreCase(FileUtil.getFileSuffix(curDownloadFileInfo.getFileName()))) {// apk

                                final String packageName = ApkUtil.getUnInstallApkPackageName(context, curDownloadFileInfo.getFilePath());
                                boolean isInstall = ApkUtil.checkAppInstalled(context, packageName);

                                if (isInstall) {
                                    if (tvText2 != null) {
                                        tvText2.setText(context.getString(R.string.main__open));
                                        try {
                                            Intent intent2 = mActivity.getPackageManager().getLaunchIntentForPackage(packageName);
                                            mActivity.startActivity(intent2);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            // show install dialog
                                            ApkUtil.installApk(context, curDownloadFileInfo.getFilePath());
                                            showToast(context.getString(R.string.main__not_install_apk) + curDownloadFileInfo.getFileName());
                                            tvText2.setText(context.getString(R.string.main__no_install));
                                        }
                                    }
                                } else {
                                    if (tvText2 != null) {
                                        tvText2.setText(context.getString(R.string.main__no_install));
                                    }
                                    ApkUtil.installApk(context, curDownloadFileInfo.getFilePath());
                                    showToast(context.getString(R.string.main__not_install_apk2) + curDownloadFileInfo.getFileName());
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

                Log.w(TAG, "onFileDownloadStatusWaiting，url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_WAITING + ")：" + downloadFileInfo.getStatus());
            } else {
                updateShow();
            }
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

            Log.w(TAG, "onFileDownloadStatusPreparing，url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_PREPARING + ")：" + downloadFileInfo.getStatus());
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

            Log.w(TAG, "onFileDownloadStatusPrepared，url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_PREPARED + ")：" + downloadFileInfo.getStatus());
        } else {
            updateShow();
        }
    }

    @Override
    public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long remainingTime) {

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
            // pbProgress.setMax(downloadFileInfo.getFileSize());
            pbProgress.setProgress(downloadFileInfo.getDownloadedSize());

            // download size
            float downloadSize = downloadFileInfo.getDownloadedSize() / 1024f / 1024f;
            float fileSize = downloadFileInfo.getFileSize() / 1024f / 1024f;

            tvDownloadSize.setText(((float) (Math.round(downloadSize * 100)) / 100) + "M/");
            tvTotalSize.setText(((float) (Math.round(fileSize * 100)) / 100) + "M");

            // download percent
            float percent = downloadSize / fileSize * 100;
            tvPercent.setText(((float) (Math.round(percent * 100)) / 100) + "%");

            // download speed and remain times
            tvText.setText(((float) (Math.round(downloadSpeed * 100)) / 100) + "KB/s" + "  " + TimeUtil.seconds2HH_mm_ss(remainingTime));

            setBackgroundOnClickListener(lnlyDownloadItem, downloadFileInfo);
        } else {
            updateShow();
        }

        Log.w(TAG, "onFileDownloadStatusDownloading，url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_DOWNLOADING + ")：" + downloadFileInfo.getStatus());
    }

    @Override
    public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return;
        }

        String url = downloadFileInfo.getUrl();
        View cacheConvertView = mConvertViews.get(url);
        if (cacheConvertView != null) {

            LinearLayout lnlyDownloadItem = (LinearLayout) cacheConvertView.findViewById(R.id.lnlyDownloadItem);
            TextView tvText = (TextView) cacheConvertView.findViewById(R.id.tvText);

            tvText.setText(cacheConvertView.getContext().getString(R.string.main__paused));

            setBackgroundOnClickListener(lnlyDownloadItem, downloadFileInfo);

            Log.w(TAG, "onFileDownloadStatusPaused，url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_PAUSED + ")：" + downloadFileInfo.getStatus());
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
                        tvText.setText(cacheConvertView.getContext().getString(R.string.main__uninstall));
                    }
                } else {
                    tvText.setText(cacheConvertView.getContext().getString(R.string.main__download_completed));
                }
            }

            setBackgroundOnClickListener(lnlyDownloadItem, downloadFileInfo);
        } else {
            updateShow();
        }

        Log.w(TAG, "onFileDownloadStatusCompleted，url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_COMPLETED + ")：" + downloadFileInfo.getStatus());
    }

    @Override
    public void onFileDownloadStatusFailed(String url,DownloadFileInfo downloadFileInfo, OnFileDownloadStatusFailReason failReason) {

        if (downloadFileInfo == null) {
            //
            return;
        }

         url = downloadFileInfo.getUrl();
        View cacheConvertView = mConvertViews.get(url);
        if (cacheConvertView != null) {

            LinearLayout lnlyDownloadItem = (LinearLayout) cacheConvertView.findViewById(R.id.lnlyDownloadItem);
            TextView tvText = (TextView) cacheConvertView.findViewById(R.id.tvText);

            String msg = cacheConvertView.getContext().getString(R.string.main__download_error);

            if (failReason != null) {
                if (OnFileDownloadStatusFailReason.TYPE_NETWORK_DENIED.equals(failReason.getType())) {
                    msg += cacheConvertView.getContext().getString(R.string.main__check_network);
                    tvText.setText(msg);
                } else if (OnFileDownloadStatusFailReason.TYPE_FILE_IS_DOWNLOADING.equals(failReason.getType())) {
                    msg = downloadFileInfo.getFileName() + cacheConvertView.getContext().getString(R.string.main__downloading);
                    showToast(msg);
                } else if (OnFileDownloadStatusFailReason.TYPE_URL_ILLEGAL.equals(failReason.getType())) {
                    msg = cacheConvertView.getContext().getString(R.string.main__url_illegal);
                    showToast(msg);
                }
            }

            setBackgroundOnClickListener(lnlyDownloadItem, downloadFileInfo);

            Log.w(TAG, "出错回调，onFileDownloadStatusFailed，url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_ERROR + ")：" + downloadFileInfo.getStatus());
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
