package org.wlf.filedownloader_demo2.advanced_use.course_download;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloader;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.listener.OnRetryableFileDownloadStatusListener;
import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader.util.MathUtil;
import org.wlf.filedownloader_demo2.R;
import org.wlf.filedownloader_demo2.ToastUtil;
import org.wlf.filedownloader_demo2.advanced_use.course_download.CourseDownloadAdapter.CourseDownloadViewHolder;
import org.wlf.filedownloader_demo2.advanced_use.model.CoursePreviewInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wlf(Andy)
 * @datetime 2015-12-11 22:08 GMT+8
 * @email 411086563@qq.com
 */
public class CourseDownloadAdapter extends RecyclerView.Adapter<CourseDownloadViewHolder> implements OnRetryableFileDownloadStatusListener {

    private List<CoursePreviewInfo> mCoursePreviewInfos = new ArrayList<CoursePreviewInfo>();
    private List<CoursePreviewInfo> mSelectCoursePreviewInfos = new ArrayList<CoursePreviewInfo>();

    private OnItemSelectListener mOnItemSelectListener;
    private Context mContext;

    public CourseDownloadAdapter(Context context, List<CoursePreviewInfo> coursePreviewInfos) {
        this.mContext = context;
        update(coursePreviewInfos, true);
    }

    public void update(List<CoursePreviewInfo> coursePreviewInfos, boolean clearSelects) {
        if (coursePreviewInfos == null) {
            return;
        }
        mCoursePreviewInfos = coursePreviewInfos;

        if (clearSelects) {
            mSelectCoursePreviewInfos.clear();
            if (mOnItemSelectListener != null) {
                mOnItemSelectListener.onNoneSelect();
            }
        }

        notifyDataSetChanged();
    }

    public void setOnItemSelectListener(OnItemSelectListener onItemSelectListener) {
        mOnItemSelectListener = onItemSelectListener;
    }

    @Override
    public int getItemViewType(int position) {
        return position;// make viewType == position
    }

    @Override
    public CourseDownloadViewHolder onCreateViewHolder(ViewGroup parent, final int viewType) {

        Log.e("wlf", "onCreateViewHolder,viewType(==position):" + viewType);

        if (parent == null) {
            return null;
        }

        View itemView = View.inflate(parent.getContext(), R.layout.advanced_use__item_course_download, null);

        // set item layout param
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup
                .LayoutParams.WRAP_CONTENT);
        itemView.setLayoutParams(lp);

        CourseDownloadViewHolder holder = new CourseDownloadViewHolder(itemView);

        return holder;
    }

    @Override
    public void onBindViewHolder(final CourseDownloadViewHolder holder, final int position, List<Object> payloads) {

        Log.e("wlf", "position:" + position + ",payloads:" + payloads.size() + ",payloads.toString:" + payloads
                .toString());

        if (holder == null) {
            return;
        }

        if (position >= mCoursePreviewInfos.size()) {
            return;
        }

        Payload payload = null;
        for (int i = payloads.size() - 1; i >= 0; i--) {
            try {
                payload = (Payload) payloads.get(i);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (payload != null) {
                break;
            }
        }

        final CoursePreviewInfo coursePreviewInfo = mCoursePreviewInfos.get(position);
        if (coursePreviewInfo == null) {
            return;
        }

        final Context context = holder.itemView.getContext();

        // course name
        holder.mTvFileName.setText(coursePreviewInfo.getCourseName());

        DownloadFileInfo downloadFileInfo = coursePreviewInfo.getDownloadFileInfo();

        Log.e("wlf", "onBindViewHolder,position:" + position + ",downloadFileInfo:" + downloadFileInfo);

        if (downloadFileInfo == null) {
            holder.mIvIcon.setImageDrawable(null);
            holder.mPbProgress.setMax(100);
            holder.mPbProgress.setProgress(0);
            holder.mTvDownloadSize.setText("00.00M/");
            holder.mTvTotalSize.setText("00.00M");
            holder.mTvPercent.setText("00.00%");
            holder.mTvText.setText(context.getString(R.string.advanced_use__course_download_not_start));
        } else {
            // course icon
            if ("mp4".equalsIgnoreCase(FileUtil.getFileSuffix(downloadFileInfo.getFileName()))) {//mp4
                holder.mIvIcon.setImageResource(R.mipmap.ic_launcher);
            } else if ("flv".equalsIgnoreCase(FileUtil.getFileSuffix(downloadFileInfo.getFileName()))) {//flv
                holder.mIvIcon.setImageResource(R.mipmap.ic_launcher);
            } else {//other
                //more
            }

            // download progress
            int totalSize = (int) downloadFileInfo.getFileSizeLong();
            int downloaded = (int) downloadFileInfo.getDownloadedSizeLong();
            double rate = (double) totalSize / Integer.MAX_VALUE;
            if (rate > 1.0) {
                totalSize = Integer.MAX_VALUE;
                downloaded = (int) (downloaded / rate);
            }

            holder.mPbProgress.setMax(totalSize);
            holder.mPbProgress.setProgress(downloaded);

            // downloaded size & file size
            double downloadSize = downloadFileInfo.getDownloadedSizeLong() / 1024f / 1024;
            double fileSize = downloadFileInfo.getFileSizeLong() / 1024f / 1024;
            holder.mTvDownloadSize.setText(MathUtil.formatNumber(downloadSize) + "M/");
            holder.mTvTotalSize.setText(MathUtil.formatNumber(fileSize) + "M");

            // downloaded percent
            double percent = downloadSize / fileSize * 100;
            holder.mTvPercent.setText(MathUtil.formatNumber(percent) + "%");

            final TextView tvText = holder.mTvText;
            // status
            switch (downloadFileInfo.getStatus()) {
                // download file status:unknown
                case Status.DOWNLOAD_STATUS_UNKNOWN:
                    tvText.setText(context.getString(R.string.advanced_use__can_not_download));
                    break;
                // download file status:waiting
                case Status.DOWNLOAD_STATUS_WAITING:
                    tvText.setText(context.getString(R.string.advanced_use__waiting));
                    break;
                // download file status:waiting
                case Status.DOWNLOAD_STATUS_RETRYING:
                    String retryTimesStr = "";
                    if (payload != null) {
                        retryTimesStr = "(" + payload.mRetryTimes + ")";
                    }
                    tvText.setText(context.getString(R.string.advanced_use__retrying_connect_resource) + retryTimesStr);
                    break;
                // download file status:preparing
                case Status.DOWNLOAD_STATUS_PREPARING:
                    tvText.setText(context.getString(R.string.advanced_use__getting_resource));
                    break;
                // download file status:prepared
                case Status.DOWNLOAD_STATUS_PREPARED:
                    tvText.setText(context.getString(R.string.advanced_use__connected_resource));
                    break;
                // download file status:downloading
                case Status.DOWNLOAD_STATUS_DOWNLOADING:
                    if (payload != null && payload.mDownloadSpeed > 0 && payload.mRemainingTime > 0) {
                        tvText.setText(MathUtil.formatNumber(payload.mDownloadSpeed) + "KB/s   " + TimeUtil
                                .seconds2HH_mm_ss(payload.mRemainingTime));
                    } else {
                        tvText.setText(context.getString(R.string.advanced_use__downloading));
                    }
                    break;
                // download file status:paused
                case Status.DOWNLOAD_STATUS_PAUSED:
                    tvText.setText(context.getString(R.string.advanced_use__paused));
                    break;
                // download file status:error
                case Status.DOWNLOAD_STATUS_ERROR:

                    String msg = context.getString(R.string.advanced_use__download_error);

                    if (payload != null && payload.mFailReason != null) {
                        FileDownloadStatusFailReason failReason = payload.mFailReason;
                        if (FileDownloadStatusFailReason.TYPE_NETWORK_DENIED.equals(failReason.getType())) {
                            msg += context.getString(R.string.advanced_use__check_network);
                        } else if (FileDownloadStatusFailReason.TYPE_URL_ILLEGAL.equals(failReason.getType())) {
                            msg += context.getString(R.string.advanced_use__url_illegal);
                        } else if (FileDownloadStatusFailReason.TYPE_NETWORK_TIMEOUT.equals(failReason.getType())) {
                            msg += context.getString(R.string.advanced_use__network_timeout);
                        }
                    }

                    tvText.setText(msg);

                    tvText.setText(context.getString(R.string.advanced_use__download_error));
                    break;
                // download file status:completed
                case Status.DOWNLOAD_STATUS_COMPLETED:
                    holder.mTvDownloadSize.setText("");
                    //mp4
                    if ("mp4".equalsIgnoreCase(FileUtil.getFileSuffix(downloadFileInfo.getFileName()))) {
                        tvText.setText(context.getString(R.string.advanced_use__course_download_play_mp4));
                    }
                    //flv
                    else if ("flv".equalsIgnoreCase(FileUtil.getFileSuffix(downloadFileInfo.getFileName()))) {
                        tvText.setText(context.getString(R.string.advanced_use__course_download_play_flv));
                    }
                    //other
                    else {
                        //more
                        tvText.setText(context.getString(R.string.advanced_use__download_completed));
                    }
                    break;
                // download file status:file not exist
                case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:
                    holder.mTvDownloadSize.setText("");
                    tvText.setText(context.getString(R.string.advanced_use__file_not_exist));
                    break;
            }
        }

        // check box
        holder.mCbSelect.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSelectCoursePreviewInfos.add(coursePreviewInfo);

                    Log.e("wlf", "isChecked=true mSelectCoursePreviewInfos.size:" + mSelectCoursePreviewInfos.size() 
                            + ",position:" + position);

                    if (mOnItemSelectListener != null) {
                        // select a download file
                        mOnItemSelectListener.onSelected(mSelectCoursePreviewInfos);
                    }
                } else {
                    mSelectCoursePreviewInfos.remove(coursePreviewInfo);

                    Log.e("wlf", "isChecked=false mSelectCoursePreviewInfos.size:" + mSelectCoursePreviewInfos.size()
                            + ",position:" + position);

                    if (mSelectCoursePreviewInfos.isEmpty()) {
                        if (mOnItemSelectListener != null) {
                            // select none
                            mOnItemSelectListener.onNoneSelect();
                        }
                    } else {
                        if (mOnItemSelectListener != null) {
                            // select a download file
                            mOnItemSelectListener.onSelected(mSelectCoursePreviewInfos);
                        }
                    }
                }
            }
        });

        // set check status
        boolean isChecked = false;
        for (CoursePreviewInfo selectCoursePreviewInfo : mSelectCoursePreviewInfos) {
            if (selectCoursePreviewInfo == coursePreviewInfo) {
                isChecked = true;
                break;
            }
        }
        holder.mCbSelect.setChecked(isChecked);

        // set background on click listener
        setBackgroundOnClickListener(holder, coursePreviewInfo);

    }

    // set background on click listener
    private void setBackgroundOnClickListener(final CourseDownloadViewHolder holder, final CoursePreviewInfo 
            coursePreviewInfo) {

        if (holder == null || holder.itemView == null) {
            return;
        }

        holder.itemView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if (coursePreviewInfo == null) {
                    return;
                }

                final DownloadFileInfo downloadFileInfo = coursePreviewInfo.getDownloadFileInfo();

                final String courseName = coursePreviewInfo.getCourseName();// or downloadFileInfo.getFileName()
                final String url = coursePreviewInfo.getCourseUrl();// or downloadFileInfo.getUrl()

                final Context context = v.getContext();
                if (downloadFileInfo != null) {
                    switch (downloadFileInfo.getStatus()) {
                        // download file status:unknown
                        case Status.DOWNLOAD_STATUS_UNKNOWN:

                            showToast(context, context.getString(R.string.advanced_use__can_not_download2) +
                                    downloadFileInfo.getFilePath() + context.getString(R.string
                                    .advanced_use__re_download));

                            break;
                        // download file status:error & paused
                        case Status.DOWNLOAD_STATUS_ERROR:
                        case Status.DOWNLOAD_STATUS_PAUSED:

                            // start
                            FileDownloader.start(url);

                            showToast(context, context.getString(R.string.advanced_use__start_download) + courseName);
                            break;
                        // download file status:file not exist
                        case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:

                            // show dialog
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle(context.getString(R.string.advanced_use__whether_re_download))
                                    .setNegativeButton(context.getString(R.string.advanced_use__dialog_btn_cancel), 
                                            null);
                            builder.setPositiveButton(context.getString(R.string.advanced_use__dialog_btn_confirm), 
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {

                                            // re-download
                                            FileDownloader.reStart(url);

                                            showToast(context, context.getString(R.string.advanced_use__re_download2)
                                                    + courseName);
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
                            FileDownloader.pause(url);

                            showToast(context, context.getString(R.string.advanced_use__paused_download) + courseName);

                            if (holder.mTvText != null) {
                                holder.mTvText.setText(context.getString(R.string.advanced_use__paused));
                            }
                            break;
                        // download file status:completed
                        case Status.DOWNLOAD_STATUS_COMPLETED:

                            if (holder.mTvDownloadSize != null) {
                                holder.mTvDownloadSize.setText("");
                            }

                            TextView tvText = holder.mTvText;
                            if (tvText != null) {
                                //mp4
                                if ("mp4".equalsIgnoreCase(FileUtil.getFileSuffix(downloadFileInfo.getFileName()))) {
                                    tvText.setText(context.getString(R.string.advanced_use__course_download_play_mp4));
                                }
                                //flv
                                else if ("flv".equalsIgnoreCase(FileUtil.getFileSuffix(downloadFileInfo.getFileName()
                                ))) {
                                    tvText.setText(context.getString(R.string.advanced_use__course_download_play_flv));
                                }
                                //other
                                else {
                                    //more
                                    tvText.setText(context.getString(R.string.advanced_use__download_completed));
                                }
                            }
                            break;
                    }
                } else {
                    // start
                    FileDownloader.start(url);
                }
            }
        });
    }

    @Override
    public void onBindViewHolder(CourseDownloadViewHolder holder, int position) {
    }

    // show toast
    private void showToast(Context context, String text) {
        ToastUtil.showToast(context, text);
    }

    @Override
    public int getItemCount() {
        return mCoursePreviewInfos.size();
    }

    @Override
    public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {
        int position = findPosition(downloadFileInfo);
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, new Payload(downloadFileInfo.getStatus(), downloadFileInfo.getUrl(), -1, -1, -1, null));
        }
    }

    @Override
    public void onFileDownloadStatusRetrying(DownloadFileInfo downloadFileInfo, int retryTimes) {
        int position = findPosition(downloadFileInfo);
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, new Payload(downloadFileInfo.getStatus(), downloadFileInfo.getUrl(), -1, -1, 
                    retryTimes, null));
        }
    }

    @Override
    public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {
        int position = findPosition(downloadFileInfo);
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, new Payload(downloadFileInfo.getStatus(), downloadFileInfo.getUrl(), -1, -1, -1, null));
        }
    }

    @Override
    public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {
        int position = findPosition(downloadFileInfo);
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, new Payload(downloadFileInfo.getStatus(), downloadFileInfo.getUrl(), -1, -1, -1, null));
        }
    }

    @Override
    public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long 
            remainingTime) {
        int position = findPosition(downloadFileInfo);
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, new Payload(downloadFileInfo.getStatus(), downloadFileInfo.getUrl(), downloadSpeed, remainingTime, -1, null));
        }
    }

    @Override
    public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {
        int position = findPosition(downloadFileInfo);
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, new Payload(downloadFileInfo.getStatus(), downloadFileInfo.getUrl(), -1, -1, -1, null));
        }
    }

    @Override
    public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {
        int position = findPosition(downloadFileInfo);
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, new Payload(downloadFileInfo.getStatus(), downloadFileInfo.getUrl(), -1, -1, -1, null));
        }
    }

    @Override
    public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, FileDownloadStatusFailReason failReason) {
        int position = findPosition(downloadFileInfo);
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, new Payload(downloadFileInfo.getStatus(), downloadFileInfo.getUrl(), -1, -1, -1, failReason));
        }


        if (mContext != null) {
            String msg = mContext.getString(R.string.advanced_use__download_error);

            if (failReason != null) {
                if (FileDownloadStatusFailReason.TYPE_NETWORK_DENIED.equals(failReason.getType())) {
                    msg += mContext.getString(R.string.advanced_use__check_network);
                } else if (FileDownloadStatusFailReason.TYPE_URL_ILLEGAL.equals(failReason.getType())) {
                    msg += mContext.getString(R.string.advanced_use__url_illegal);
                } else if (FileDownloadStatusFailReason.TYPE_NETWORK_TIMEOUT.equals(failReason.getType())) {
                    msg += mContext.getString(R.string.advanced_use__network_timeout);
                } else if (FileDownloadStatusFailReason.TYPE_STORAGE_SPACE_IS_FULL.equals(failReason.getType())) {
                    msg += mContext.getString(R.string.advanced_use__storage_space_is_full);
                } else if (FileDownloadStatusFailReason.TYPE_STORAGE_SPACE_CAN_NOT_WRITE.equals(failReason.getType())) {
                    msg += mContext.getString(R.string.advanced_use__storage_space_can_not_write);
                } else if (FileDownloadStatusFailReason.TYPE_FILE_NOT_DETECT.equals(failReason.getType())) {
                    msg += mContext.getString(R.string.advanced_use__file_not_detect);
                } else if (FileDownloadStatusFailReason.TYPE_BAD_HTTP_RESPONSE_CODE.equals(failReason.getType())) {
                    msg += mContext.getString(R.string.advanced_use__http_bad_response_code);
                } else if (FileDownloadStatusFailReason.TYPE_HTTP_FILE_NOT_EXIST.equals(failReason.getType())) {
                    msg += mContext.getString(R.string.advanced_use__http_file_not_exist);
                } else if (FileDownloadStatusFailReason.TYPE_SAVE_FILE_NOT_EXIST.equals(failReason.getType())) {
                    msg += mContext.getString(R.string.advanced_use__save_file_not_exist);
                }
            }


            showToast(mContext, msg + "，url：" + url);
        }
    }

    public static final class CourseDownloadViewHolder extends RecyclerView.ViewHolder {

        private ImageView mIvIcon;
        private TextView mTvFileName;
        private ProgressBar mPbProgress;
        private TextView mTvDownloadSize;
        private TextView mTvTotalSize;
        private TextView mTvPercent;
        private TextView mTvText;
        private CheckBox mCbSelect;


        public CourseDownloadViewHolder(View itemView) {
            super(itemView);

            mIvIcon = (ImageView) itemView.findViewById(R.id.ivIcon);
            mTvFileName = (TextView) itemView.findViewById(R.id.tvFileName);
            mPbProgress = (ProgressBar) itemView.findViewById(R.id.pbProgress);
            mTvDownloadSize = (TextView) itemView.findViewById(R.id.tvDownloadSize);
            mTvTotalSize = (TextView) itemView.findViewById(R.id.tvTotalSize);
            mTvPercent = (TextView) itemView.findViewById(R.id.tvPercent);
            mTvText = (TextView) itemView.findViewById(R.id.tvText);
            mCbSelect = (CheckBox) itemView.findViewById(R.id.cbSelect);
        }
    }

    private int findPosition(DownloadFileInfo downloadFileInfo) {
        if (downloadFileInfo == null) {
            return -1;
        }
        for (int i = 0; i < mCoursePreviewInfos.size(); i++) {
            CoursePreviewInfo coursePreviewInfo = mCoursePreviewInfos.get(i);
            if (coursePreviewInfo == null || TextUtils.isEmpty(coursePreviewInfo.getCourseUrl())) {
                continue;
            }
            if (coursePreviewInfo.getCourseUrl().equals(downloadFileInfo.getUrl())) {
                // find
                return i;
            }
        }
        return -1;
    }

    private static class Payload {

        private int mStatus = Status.DOWNLOAD_STATUS_UNKNOWN;

        private String mUrl;
        private float mDownloadSpeed;
        private long mRemainingTime;
        private int mRetryTimes;
        private FileDownloadStatusFailReason mFailReason;

        public Payload(int status, String url, float downloadSpeed, long remainingTime, int retryTimes, FileDownloadStatusFailReason failReason) {
            this.mStatus = status;
            this.mUrl = url;
            this.mDownloadSpeed = downloadSpeed;
            this.mRemainingTime = remainingTime;
            this.mRetryTimes = retryTimes;
            this.mFailReason = failReason;
        }

        @Override
        public String toString() {
            return "Payload{" +
                    "mStatus=" + mStatus +
                    ", mUrl='" + mUrl + '\'' +
                    ", mDownloadSpeed=" + mDownloadSpeed +
                    ", mRemainingTime=" + mRemainingTime +
                    ", mRetryTimes=" + mRetryTimes +
                    ", mFailReason=" + mFailReason +
                    '}';
        }
    }

    /**
     * OnItemSelectListener
     */
    public interface OnItemSelectListener {

        void onSelected(List<CoursePreviewInfo> selectCoursePreviewInfos);

        void onNoneSelect();
    }

    public void release() {
        for (CoursePreviewInfo coursePreviewInfo : mCoursePreviewInfos) {
            if (coursePreviewInfo == null) {
                continue;
            }
            coursePreviewInfo.release();
        }
    }

}
