package org.wlf.filedownloader_demo2.advanced_use.course_download;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloader;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.listener.OnDeleteDownloadFilesListener;
import org.wlf.filedownloader.listener.OnDownloadFileChangeListener;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader_demo2.R;
import org.wlf.filedownloader_demo2.ToastUtil;
import org.wlf.filedownloader_demo2.advanced_use.course_download.CourseDownloadAdapter.OnItemSelectListener;
import org.wlf.filedownloader_demo2.advanced_use.data_access.GetCourseDownloads;
import org.wlf.filedownloader_demo2.advanced_use.data_access.GetCourseDownloads.OnGetCourseDownloadsListener;
import org.wlf.filedownloader_demo2.advanced_use.model.CoursePreviewInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Course Download Fragment
 * <br/>
 * 课程下载界面
 *
 * @author wlf(Andy)
 * @datetime 2015-12-11 21:40 GMT+8
 * @email 411086563@qq.com
 */
public class CourseDownloadFragment extends Fragment implements OnItemSelectListener, OnDownloadFileChangeListener {

    public static CourseDownloadFragment newInstance() {
        CourseDownloadFragment frag = new CourseDownloadFragment();
        //        Bundle args = new Bundle();
        //        frag.setArguments(args);
        return frag;
    }

    private RecyclerView mRvCourseDownload;
    private CourseDownloadAdapter mCourseDownloadAdapter;

    private LinearLayout mLnlyOperation;
    private Button mBtnPause;
    private Button mBtnStartOrContinue;
    private Button mBtnDelete;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = getView();

        if (rootView == null) {

            rootView = inflater.inflate(R.layout.advanced_use__fragment_course_download, null);

            mRvCourseDownload = (RecyclerView) rootView.findViewById(R.id.rvCourseDownload);

            // create LinearLayoutManager
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            // vertical layout
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            // set layoutManager
            mRvCourseDownload.setLayoutManager(layoutManager);

            if (mCourseDownloadAdapter != null) {
                mCourseDownloadAdapter.release();
            }
            mCourseDownloadAdapter = new CourseDownloadAdapter(getActivity(), null);
            mRvCourseDownload.setAdapter(mCourseDownloadAdapter);

            mRvCourseDownload.setItemAnimator(null);
            mCourseDownloadAdapter.setOnItemSelectListener(this);

            mLnlyOperation = (LinearLayout) rootView.findViewById(R.id.lnlyOperation);
            mBtnPause = (Button) rootView.findViewById(R.id.btnPause);
            mBtnStartOrContinue = (Button) rootView.findViewById(R.id.btnStartOrContinue);
            mBtnDelete = (Button) rootView.findViewById(R.id.btnDelete);

            initCourseDownloadData(true);

            FileDownloader.registerDownloadStatusListener(mCourseDownloadAdapter);
            FileDownloader.registerDownloadFileChangeListener(this);
        }

        return rootView;
    }

    private void initCourseDownloadData(final boolean clearSelects) {
        GetCourseDownloads getCourseDownloads = new GetCourseDownloads();
        getCourseDownloads.getCourseDownloads(getActivity(), new OnGetCourseDownloadsListener() {
            @Override
            public void onGetCourseDownloadsSucceed(List<CoursePreviewInfo> coursePreviewInfos) {
                mCourseDownloadAdapter.update(coursePreviewInfos, clearSelects);
            }

            @Override
            public void onGetCourseDownloadsFailed() {
                ToastUtil.showToast(getActivity(), getActivity().getString(R.string.common__get_data_failed));
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FileDownloader.unregisterDownloadStatusListener(mCourseDownloadAdapter);
        FileDownloader.unregisterDownloadFileChangeListener(this);
        if (mCourseDownloadAdapter != null) {
            mCourseDownloadAdapter.release();
        }   
    }

    @Override
    public void onSelected(final List<CoursePreviewInfo> selectCoursePreviewInfos) {

        mLnlyOperation.setVisibility(View.VISIBLE);

        mBtnStartOrContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectCoursePreviewInfos != null && !selectCoursePreviewInfos.isEmpty()) {
                    List<String> stoppedUrls = new ArrayList<String>();
                    for (CoursePreviewInfo info : selectCoursePreviewInfos) {
                        if (info == null || info.getDownloadFileInfo() == null) {
                            continue;
                        }
                        switch (info.getDownloadFileInfo().getStatus()) {
                            case Status.DOWNLOAD_STATUS_PAUSED:
                            case Status.DOWNLOAD_STATUS_ERROR:
                                stoppedUrls.add(info.getDownloadFileInfo().getUrl());
                                break;
                        }
                    }

                    if (!CollectionUtil.isEmpty(stoppedUrls)) {
                        // start/continue download
                        FileDownloader.start(stoppedUrls);
                    } else {
                        // do nothing
                    }
                }
            }
        });

        mBtnPause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectCoursePreviewInfos != null && !selectCoursePreviewInfos.isEmpty()) {
                    List<String> downloadingUrls = new ArrayList<String>();
                    for (CoursePreviewInfo info : selectCoursePreviewInfos) {
                        if (info == null || info.getDownloadFileInfo() == null) {
                            continue;
                        }
                        switch (info.getDownloadFileInfo().getStatus()) {
                            case Status.DOWNLOAD_STATUS_WAITING:
                            case Status.DOWNLOAD_STATUS_DOWNLOADING:
                            case Status.DOWNLOAD_STATUS_PREPARING:
                            case Status.DOWNLOAD_STATUS_PREPARED:
                                downloadingUrls.add(info.getDownloadFileInfo().getUrl());
                                break;
                        }
                    }

                    if (!CollectionUtil.isEmpty(downloadingUrls)) {
                        FileDownloader.pause(downloadingUrls);
                    } else {
                        // do nothing
                    }
                }
            }
        });

        mBtnDelete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!CollectionUtil.isEmpty(selectCoursePreviewInfos)) {
                    final List<String> needDeleteUrls = new ArrayList<String>();
                    for (CoursePreviewInfo info : selectCoursePreviewInfos) {
                        if (info == null) {
                            continue;
                        }
                        if (info.getDownloadFileInfo() == null) {
                            if (info.getCourseUrl() != null) {
                                needDeleteUrls.add(info.getCourseUrl());
                            }
                        } else {
                            needDeleteUrls.add(info.getDownloadFileInfo().getUrl());
                        }
                    }


                    if (!CollectionUtil.isEmpty(needDeleteUrls)) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        String note = getString(R.string.course_center__course_cache_delete_confirm);
                        note = String.format(note, needDeleteUrls.size());
                        builder.setTitle(note);
                        builder.setNegativeButton(getString(R.string.custom_model__dialog_btn_cancel), null);
                        builder.setPositiveButton(getString(R.string.custom_model__dialog_btn_confirm), new 
                                DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                FileDownloader.delete(needDeleteUrls, true, new OnDeleteDownloadFilesListener() {
                                    @Override
                                    public void onDeletingDownloadFiles(List<DownloadFileInfo> 
                                                                                downloadFilesNeedDelete, 
                                                                        List<DownloadFileInfo> downloadFilesDeleted, 
                                                                        List<DownloadFileInfo> downloadFilesSkip, 
                                                                        DownloadFileInfo downloadFileDeleting) {
                                        Log.e("wlf", "批量删除中，downloadFilesNeedDelete：" + downloadFilesNeedDelete.size
                                                () + ",downloadFilesDeleted:" + downloadFilesDeleted.size());
                                        if (downloadFileDeleting != null && downloadFilesSkip != null) {
                                            showToast(getString(R.string.advanced_use__deleting) +
                                                    downloadFileDeleting.getFileName() +
                                                    getString(R.string.advanced_use__progress) +
                                                    (downloadFilesDeleted.size() + downloadFilesSkip.size()) +
                                                    getString(R.string.advanced_use__failed2) +
                                                    downloadFilesSkip.size() + getString(R.string
                                                    .advanced_use__skip_and_total_delete_division) +

                                                    downloadFilesNeedDelete.size());
                                        }
                                    }

                                    @Override
                                    public void onDeleteDownloadFilesPrepared(List<DownloadFileInfo> 
                                                                                      downloadFilesNeedDelete) {
                                        Log.e("wlf", "开始批量删除，downloadFilesNeedDelete：" + downloadFilesNeedDelete.size
                                                ());
                                        showToast(getString(R.string.advanced_use__need_delete) + 
                                                downloadFilesNeedDelete.size());
                                    }

                                    @Override
                                    public void onDeleteDownloadFilesCompleted(List<DownloadFileInfo> 
                                                                                       downloadFilesNeedDelete, 
                                                                               List<DownloadFileInfo> 
                                                                                       downloadFilesDeleted) {
                                        Log.e("wlf", "批量删除完成，downloadFilesNeedDelete：" + downloadFilesNeedDelete.size
                                                () + ",downloadFilesDeleted:" + downloadFilesDeleted.size());
                                        showToast(getString(R.string.advanced_use__delete_finish) +
                                                downloadFilesDeleted.size() +
                                                getString(R.string.advanced_use__failed3) + (downloadFilesNeedDelete
                                                .size() - downloadFilesDeleted.size()));
                                    }
                                });
                                // 
                                dialog.dismiss();
                            }
                        });
                        builder.show();
                    } else {
                        showToast(getString(R.string.advanced_use__delete_failed));
                        // can not do it
                    }
                }
            }
        });
    }

    private void showToast(String msg) {
        ToastUtil.showToast(getActivity(), msg);
    }

    @Override
    public void onNoneSelect() {
        mLnlyOperation.setVisibility(View.GONE);
    }

    @Override
    public void onDownloadFileCreated(DownloadFileInfo downloadFileInfo) {
        Log.e("wlf", "onDownloadFileCreated---" + downloadFileInfo.getUrl() + ",thread:" + Thread.currentThread());
        initCourseDownloadData(false);
    }

    @Override
    public void onDownloadFileUpdated(DownloadFileInfo downloadFileInfo, Type type) {
    }

    @Override
    public void onDownloadFileDeleted(DownloadFileInfo downloadFileInfo) {
        initCourseDownloadData(true);
    }

}
