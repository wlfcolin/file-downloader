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

        switch (downloadFileInfo.getStatus()) {
            // download file status:unknown
            case Status.DOWNLOAD_STATUS_UNKNOWN:
                tvText.setText("无法下载，请删除后重新下载");
                break;
            // download file status:preparing
            case Status.DOWNLOAD_STATUS_PREPARING:
                tvText.setText("正在获取资源");
                break;
            // download file status:prepared
            case Status.DOWNLOAD_STATUS_PREPARED:
                tvText.setText("已连接资源");
                break;
            // download file status:paused
            case Status.DOWNLOAD_STATUS_PAUSED:
                tvText.setText("已暂停");
                break;
            // download file status:downloading
            case Status.DOWNLOAD_STATUS_DOWNLOADING:
                tvText.setText("正在下载");
                break;
            // download file status:error
            case Status.DOWNLOAD_STATUS_ERROR:
                tvText.setText("下载出错");
                break;
            // download file status:waiting
            case Status.DOWNLOAD_STATUS_WAITING:
                tvText.setText("等待下载");
                break;
            // download file status:completed
            case Status.DOWNLOAD_STATUS_COMPLETED:
                tvDownloadSize.setText("");
                if ("apk".equalsIgnoreCase(FileUtil.getFileSuffix(downloadFileInfo.getFileName()))) {// apk
                    String packageName = ApkUtil.getUnInstallApkPckageName(mActivity, downloadFileInfo.getFilePath());
                    boolean isInstall = ApkUtil.checkAppInstalled(mActivity, packageName);
                    if (isInstall) {
                        tvText.setText("打开");
                    } else {
                        tvText.setText("未安装");
                    }
                } else {
                    tvText.setText("已下载完成");
                }
                break;
            // download file status:file not exist
            case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:
                tvDownloadSize.setText("");
                tvText.setText("文件不存在");
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
                            showToast("无法下载，请删除：" + curDownloadFileInfo.getFilePath() + " 后重新下载");
                            break;
                        // download file status:error & paused
                        case Status.DOWNLOAD_STATUS_ERROR:
                        case Status.DOWNLOAD_STATUS_PAUSED:
                            fileDownloadManager.start(curDownloadFileInfo.getUrl(), mOnFileDownloadStatusListener);
                            showToast("开始/继续下载：" + curDownloadFileInfo.getFileName());
                            break;
                        // download file status:file not exist
                        case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:
                            // show dialog
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle("是否重新下载").setNegativeButton("取消", null);
                            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // re-download
                                    fileDownloadManager.reStart(curDownloadFileInfo.getUrl(), mOnFileDownloadStatusListener);
                                    showToast("重新下载下载：" + curDownloadFileInfo.getFileName());
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

                            showToast("暂停下载：" + curDownloadFileInfo.getFileName());

                            TextView tvText = (TextView) lnlyDownloadItem.findViewById(R.id.tvText);
                            if (tvText != null) {
                                tvText.setText("已暂停");
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

                                final String packageName = ApkUtil.getUnInstallApkPckageName(context, curDownloadFileInfo.getFilePath());
                                boolean isInstall = ApkUtil.checkAppInstalled(context, packageName);

                                if (isInstall) {
                                    if (tvText2 != null) {
                                        tvText2.setText("打开");
                                        try {
                                            Intent intent2 = mActivity.getPackageManager().getLaunchIntentForPackage(packageName);
                                            mActivity.startActivity(intent2);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            // show install dialog
                                            ApkUtil.installApk(context, curDownloadFileInfo.getFilePath());
                                            showToast("（无法启动）请安装Apk：" + curDownloadFileInfo.getFileName());
                                            tvText2.setText("未安装");
                                        }
                                    }
                                } else {
                                    if (tvText2 != null) {
                                        tvText2.setText("未安装");
                                    }
                                    ApkUtil.installApk(context, curDownloadFileInfo.getFilePath());
                                    showToast("（未安装）请安装Apk：" + curDownloadFileInfo.getFileName());
                                }
                            } else {
                                tvText2.setText("已下载完成");
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
                tvText.setText("等待下载");

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
            tvText.setText("正在获取资源");

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
            tvText.setText("已连接资源");

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

            tvText.setText("已暂停");

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
                    String packageName = ApkUtil.getUnInstallApkPckageName(mActivity, downloadFileInfo.getFilePath());
                    boolean isInstall = ApkUtil.checkAppInstalled(mActivity, packageName);

                    if (isInstall) {
                        tvText.setText("打开");
                    } else {
                        tvText.setText("未安装");
                    }
                } else {
                    tvText.setText("已下载完成");
                }
            }

            setBackgroundOnClickListener(lnlyDownloadItem, downloadFileInfo);
        } else {
            updateShow();
        }

        Log.w(TAG, "onFileDownloadStatusCompleted，url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_COMPLETED + ")：" + downloadFileInfo.getStatus());
    }

    @Override
    public void onFileDownloadStatusFailed(DownloadFileInfo downloadFileInfo, OnFileDownloadStatusFailReason failReason) {

        if (downloadFileInfo == null) {
            return;
        }

        String url = downloadFileInfo.getUrl();
        View cacheConvertView = mConvertViews.get(url);
        if (cacheConvertView != null) {

            LinearLayout lnlyDownloadItem = (LinearLayout) cacheConvertView.findViewById(R.id.lnlyDownloadItem);
            TextView tvText = (TextView) cacheConvertView.findViewById(R.id.tvText);

            String msg = "下载出错";

            if (failReason != null) {
                if (OnFileDownloadStatusFailReason.TYPE_NETWORK_DENIED.equals(failReason.getType())) {
                    msg += "(请检查网络)";
                    tvText.setText(msg);
                } else if (OnFileDownloadStatusFailReason.TYPE_FILE_IS_DOWNLOADING.equals(failReason.getType())) {
                    msg = downloadFileInfo.getFileName() + " 正在下载";
                    showToast(msg);
                } else if (OnFileDownloadStatusFailReason.TYPE_URL_ILLEGAL.equals(failReason.getType())) {
                    msg = "Url不合法";
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
