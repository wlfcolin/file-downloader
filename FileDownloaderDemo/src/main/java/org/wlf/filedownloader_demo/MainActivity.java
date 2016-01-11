package org.wlf.filedownloader_demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloader;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener;
import org.wlf.filedownloader.listener.OnDeleteDownloadFilesListener;
import org.wlf.filedownloader.listener.OnDetectBigUrlFileListener;
import org.wlf.filedownloader.listener.OnDetectUrlFileListener;
import org.wlf.filedownloader.listener.OnMoveDownloadFileListener;
import org.wlf.filedownloader.listener.OnMoveDownloadFilesListener;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener;
import org.wlf.filedownloader_demo.DownloadFileListAdapter.OnItemSelectListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo Test MainActivity
 * <br/>
 * 测试主界面
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class MainActivity extends Activity implements OnItemSelectListener {

    // adapter
    private DownloadFileListAdapter mDownloadFileListAdapter;

    // toast
    private Toast mToast;

    private LinearLayout mLnlyOperation;
    private Button mBtnDelete;
    private Button mBtnMove;
    private Button mBtnRename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main__activity_main);

        mLnlyOperation = (LinearLayout) findViewById(R.id.lnlyOperation);
        mBtnDelete = (Button) findViewById(R.id.btnDelete);
        mBtnMove = (Button) findViewById(R.id.btnMove);
        mBtnRename = (Button) findViewById(R.id.btnRename);

        // ListView
        ListView lvDownloadFileList = (ListView) findViewById(R.id.lvDownloadFileList);
        mDownloadFileListAdapter = new DownloadFileListAdapter(this);
        lvDownloadFileList.setAdapter(mDownloadFileListAdapter);

        mDownloadFileListAdapter.setOnItemSelectListener(this);

        // registerDownloadStatusListener 
        FileDownloader.registerDownloadStatusListener(mDownloadFileListAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mDownloadFileListAdapter != null) {
            mDownloadFileListAdapter.updateShow();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options, menu);// init OptionsMenu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {// handle OptionsMenu
        switch (item.getItemId()) {
            // new download
            case R.id.optionsNew:
                // show new download dialog
                showNewDownloadDialog();
                return true;
            // new multi download
            case R.id.optionsNews:
                // show new multi download dialog
                showMultiNewDownloadDialog();
                return true;
            // new download(custom)
            case R.id.optionsNewWithDetect:
                // show new download(custom) dialog
                showCustomNewDownloadDialog();
                return true;
            // new big file download
            case R.id.optionsNewBigFileWithDetect:
                showNewBigDownloadDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // show new download dialog
    private void showNewDownloadDialog() {

        final EditText etUrl = new EditText(this);
        // apk file, the url with special character
        etUrl.setText("  http://182.254.149.157/ftp/image/shop/product/Kids Addition & Subtraction 1.0.apk ");
        etUrl.setFocusable(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.main__please_input_download_file)).setView(etUrl).setNegativeButton
                (getString(R.string.main__dialog_btn_cancel), null);
        builder.setPositiveButton(getString(R.string.main__dialog_btn_confirm), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // file url
                String url = etUrl.getText().toString().trim();
                FileDownloader.start(url);
            }
        });
        builder.show();
    }

    // show new multi download dialog
    private void showMultiNewDownloadDialog() {

        final EditText etUrl1 = new EditText(this);
        etUrl1.setText("http://img13.360buyimg.com/n1/g14/M01/1B/1F/rBEhVlM03iwIAAAAAAFJnWsj5UAAAK8_gKFgkMAAUm1950" +
                ".jpg");// web image file,jpg
        etUrl1.setFocusable(true);

        final EditText etUrl2 = new EditText(this);
        etUrl2.setText("http://sqdd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk");// apk file,tencent qq
        etUrl2.setFocusable(true);

        final EditText etUrl3 = new EditText(this);
        etUrl3.setText("http://down.sandai.net/thunder7/Thunder_dl_7.9.41.5020.exe");// exe file,thunder
        etUrl3.setFocusable(true);

        final EditText etUrl4 = new EditText(this);
        etUrl4.setText("http://mp4.28mtv.com/mp41/1862-刘德华-余生一起过[68mtv.com].mp4");// mp4 file,mv
        etUrl4.setFocusable(true);

        final EditText etUrl5 = new EditText(this);
        etUrl5.setText("http://182.254.149.157/ftp/image/shop/product/@#_% &.apk");// apk file, with special characters
        etUrl5.setFocusable(true);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT);
        linearLayout.addView(etUrl1, params);
        linearLayout.addView(etUrl2, params);
        linearLayout.addView(etUrl3, params);
        linearLayout.addView(etUrl4, params);
        linearLayout.addView(etUrl5, params);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.main__please_input_multi_download_files)).setView(linearLayout)
                .setNegativeButton(getString(R.string.main__dialog_btn_cancel), null);
        builder.setPositiveButton(getString(R.string.main__dialog_btn_confirm), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // file urls
                String url1 = etUrl1.getText().toString().trim();
                String url2 = etUrl2.getText().toString().trim();
                String url3 = etUrl3.getText().toString().trim();
                String url4 = etUrl4.getText().toString().trim();
                String url5 = etUrl5.getText().toString().trim();

                List<String> urls = new ArrayList<String>();
                urls.add(url1);
                urls.add(url2);
                urls.add(url3);
                urls.add(url4);
                urls.add(url5);

                FileDownloader.start(urls);
            }
        });
        builder.show();
    }

    // show new download(custom) dialog
    private void showCustomNewDownloadDialog() {

        final EditText etUrlCustom = new EditText(this);
        // mp4 file, the url with params
        etUrlCustom.setText("http://183.57.151" +
                ".208/download/videos/47CDA700A098E497/2015/12/17/1_1449832690_1449833760" +
                ".mp4?a=836e48885e3a571404b85948aadb4797a4f6dec200407c1f48710c1a16fca32b&u" +
                "=2819e7dec4dd32a780d6713df83b1b9df0c5bc193b52c5c1cacf932893b42327");
        etUrlCustom.setFocusable(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.main__please_input_download_file)).setView(etUrlCustom).setNegativeButton
                (getString(R.string.main__dialog_btn_cancel), null);
        builder.setPositiveButton(getString(R.string.main__dialog_btn_confirm), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // file url
                String url = etUrlCustom.getText().toString().trim();
                FileDownloader.detect(url, new OnDetectUrlFileListener() {
                    // ----------------------detect url file callback----------------------
                    @Override
                    public void onDetectNewDownloadFile(final String url, String fileName, final String saveDir, int 
                            fileSize) {
                        final TextView tvFileDir = new TextView(MainActivity.this);
                        tvFileDir.setText(getString(R.string.main__save_path));

                        final EditText etFileDir = new EditText(MainActivity.this);
                        etFileDir.setText(saveDir);
                        etFileDir.setFocusable(true);

                        final TextView tvFileName = new TextView(MainActivity.this);
                        tvFileName.setText(getString(R.string.main__save_file_name));

                        final EditText etFileName = new EditText(MainActivity.this);
                        etFileName.setText(fileName);
                        etFileName.setFocusable(true);

                        final TextView tvFileSize = new TextView(MainActivity.this);
                        float size = fileSize / 1024f / 1024f;
                        tvFileSize.setText(getString(R.string.main__file_size) + ((float) (Math.round(size * 100)) / 
                                100) + "M");

                        LinearLayout linearLayout = new LinearLayout(MainActivity.this);
                        linearLayout.setOrientation(LinearLayout.VERTICAL);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams
                                .MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        linearLayout.addView(tvFileDir, params);
                        linearLayout.addView(etFileDir, params);
                        linearLayout.addView(tvFileName, params);
                        linearLayout.addView(etFileName, params);
                        linearLayout.addView(tvFileSize, params);

                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(getString(R.string.main__confirm_save_path_and_name)).setView(linearLayout)
                                .setNegativeButton(getString(R.string.main__dialog_btn_cancel), null);
                        builder.setPositiveButton(getString(R.string.main__dialog_btn_confirm), new DialogInterface
                                .OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // save file dir
                                String newFileDir = etFileDir.getText().toString().trim();
                                // save file name
                                String newFileName = etFileName.getText().toString().trim();
                                // create download
                                showToast(getString(R.string.main__new_download) + url);
                                Log.e("wlf", "探测文件，新建下载：" + url);
                                FileDownloader.createAndStart(url, newFileDir, newFileName);
                            }
                        });
                        builder.show();
                    }

                    @Override
                    public void onDetectUrlFileExist(String url) {
                        showToast(getString(R.string.main__continue_download) + url);
                        Log.e("wlf", "探测文件，继续下载：" + url);
                        // continue download
                        FileDownloader.start(url);
                    }

                    @Override
                    public void onDetectUrlFileFailed(String url, DetectUrlFileFailReason failReason) {
                        String msg = null;
                        if (failReason != null) {
                            msg = failReason.getMessage();
                            if (TextUtils.isEmpty(msg)) {
                                Throwable t = failReason.getCause();
                                if (t != null) {
                                    msg = t.getLocalizedMessage();
                                }
                            }
                        }
                        showToast(getString(R.string.main__detect_file_error) + msg + "," + url);
                        Log.e("wlf", "出错回调，探测文件出错：" + msg + "," + url);
                    }
                });
            }
        });
        builder.show();
    }

    // show new download(big file download) dialog
    private void showNewBigDownloadDialog() {

        final EditText etUrlCustom = new EditText(this);
        // big download file witch bigger than 2G to download
        etUrlCustom.setText("http://dx500.downyouxi.com/minglingyuzhengfu4.rar");
        etUrlCustom.setFocusable(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.main__please_input_download_file)).setView(etUrlCustom).setNegativeButton
                (getString(R.string.main__dialog_btn_cancel), null);
        builder.setPositiveButton(getString(R.string.main__dialog_btn_confirm), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // file url
                String url = etUrlCustom.getText().toString().trim();
                FileDownloader.detect(url, new OnDetectBigUrlFileListener() {
                    // ----------------------detect url file callback----------------------
                    @Override
                    public void onDetectNewDownloadFile(final String url, String fileName, final String saveDir, long
                            fileSize) {
                        final TextView tvFileDir = new TextView(MainActivity.this);
                        tvFileDir.setText(getString(R.string.main__save_path));

                        final EditText etFileDir = new EditText(MainActivity.this);
                        etFileDir.setText(saveDir);
                        etFileDir.setFocusable(true);

                        final TextView tvFileName = new TextView(MainActivity.this);
                        tvFileName.setText(getString(R.string.main__save_file_name));

                        final EditText etFileName = new EditText(MainActivity.this);
                        etFileName.setText(fileName);
                        etFileName.setFocusable(true);

                        final TextView tvFileSize = new TextView(MainActivity.this);
                        float size = fileSize / 1024f / 1024f;
                        tvFileSize.setText(getString(R.string.main__file_size) + ((float) (Math.round(size * 100)) / 
                                100) + "M");

                        LinearLayout linearLayout = new LinearLayout(MainActivity.this);
                        linearLayout.setOrientation(LinearLayout.VERTICAL);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams
                                .MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        linearLayout.addView(tvFileDir, params);
                        linearLayout.addView(etFileDir, params);
                        linearLayout.addView(tvFileName, params);
                        linearLayout.addView(etFileName, params);
                        linearLayout.addView(tvFileSize, params);

                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(getString(R.string.main__confirm_save_path_and_name)).setView(linearLayout)
                                .setNegativeButton(getString(R.string.main__dialog_btn_cancel), null);
                        builder.setPositiveButton(getString(R.string.main__dialog_btn_confirm), new DialogInterface
                                .OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // save file dir
                                String newFileDir = etFileDir.getText().toString().trim();
                                // save file name
                                String newFileName = etFileName.getText().toString().trim();
                                // create download
                                showToast(getString(R.string.main__new_download) + url);
                                Log.e("wlf", "探测文件，新建下载：" + url);
                                FileDownloader.createAndStart(url, newFileDir, newFileName);
                            }
                        });
                        builder.show();
                    }

                    @Override
                    public void onDetectUrlFileExist(String url) {
                        showToast(getString(R.string.main__continue_download) + url);
                        Log.e("wlf", "探测文件，继续下载：" + url);
                        // continue download
                        FileDownloader.start(url);
                    }

                    @Override
                    public void onDetectUrlFileFailed(String url, DetectUrlFileFailReason failReason) {
                        String msg = null;
                        if (failReason != null) {
                            msg = failReason.getMessage();
                            if (TextUtils.isEmpty(msg)) {
                                Throwable t = failReason.getCause();
                                if (t != null) {
                                    msg = t.getLocalizedMessage();
                                }
                            }
                        }
                        showToast(getString(R.string.main__detect_file_error) + msg + "," + url);
                        Log.e("wlf", "出错回调，探测文件出错：" + msg + "," + url);
                    }
                });
            }
        });
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // pause all downloads
        FileDownloader.pauseAll();
        // unregisterDownloadStatusListener
        FileDownloader.unregisterDownloadStatusListener(mDownloadFileListAdapter);
    }

    // show toast
    private void showToast(CharSequence text) {
        if (mToast == null) {
            mToast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        } else {
            mToast.cancel();
            mToast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    private void updateAdapter() {
        if (mDownloadFileListAdapter == null) {
            return;
        }
        mDownloadFileListAdapter.updateShow();
    }

    @Override
    public void onSelected(final List<DownloadFileInfo> selectDownloadFileInfos) {

        mLnlyOperation.setVisibility(View.VISIBLE);

        mBtnDelete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(getString(R.string.main__confirm_whether_delete_save_file));
                builder.setNegativeButton(getString(R.string.main__confirm_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteDownloadFiles(false, selectDownloadFileInfos);
                    }
                });
                builder.setPositiveButton(getString(R.string.main__confirm_yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteDownloadFiles(true, selectDownloadFileInfos);
                    }
                });
                builder.show();
            }

            private void deleteDownloadFiles(boolean deleteDownloadedFile, List<DownloadFileInfo> 
                    selectDownloadFileInfos) {

                List<String> urls = new ArrayList<String>();

                for (DownloadFileInfo downloadFileInfo : selectDownloadFileInfos) {
                    if (downloadFileInfo == null) {
                        continue;
                    }
                    urls.add(downloadFileInfo.getUrl());
                }

                // single delete
                if (urls.size() == 1) {
                    FileDownloader.delete(urls.get(0), deleteDownloadedFile, new OnDeleteDownloadFileListener() {
                        @Override
                        public void onDeleteDownloadFileSuccess(DownloadFileInfo downloadFileDeleted) {
                            showToast(getString(R.string.main__delete_succeed));
                            updateAdapter();
                        }

                        @Override
                        public void onDeleteDownloadFilePrepared(DownloadFileInfo downloadFileNeedDelete) {
                            showToast(getString(R.string.main__deleting) + downloadFileNeedDelete.getFileName());
                        }

                        @Override
                        public void onDeleteDownloadFileFailed(DownloadFileInfo downloadFileInfo, DeleteDownloadFileFailReason failReason) {
                            showToast(getString(R.string.main__delete) + downloadFileInfo.getFileName() + getString(R
                                    .string.main__failed));
                            Log.e("wlf", "出错回调，删除" + downloadFileInfo.getFileName() + "失败");
                        }
                    });
                }
                // multi delete
                else {
                    Log.e("wlf_deletes", "点击开始批量删除");
                    FileDownloader.delete(urls, deleteDownloadedFile, new OnDeleteDownloadFilesListener() {

                        @Override
                        public void onDeletingDownloadFiles(List<DownloadFileInfo> downloadFilesNeedDelete, 
                                                            List<DownloadFileInfo> downloadFilesDeleted, 
                                                            List<DownloadFileInfo> downloadFilesSkip, 
                                                            DownloadFileInfo downloadFileDeleting) {
                            showToast(getString(R.string.main__deleting) + downloadFileDeleting.getFileName() +
                                    getString(R.string.main__progress) + (downloadFilesDeleted.size() + 
                                    downloadFilesSkip.size()) + getString(R.string.main__failed2) + downloadFilesSkip
                                    .size() + getString(R.string.main__skip_and_total_delete_division) +
                                    downloadFilesNeedDelete.size());
                            updateAdapter();
                        }

                        @Override
                        public void onDeleteDownloadFilesPrepared(List<DownloadFileInfo> downloadFilesNeedDelete) {
                            showToast(getString(R.string.main__need_delete) + downloadFilesNeedDelete.size());
                        }

                        @Override
                        public void onDeleteDownloadFilesCompleted(List<DownloadFileInfo> downloadFilesNeedDelete, 
                                                                   List<DownloadFileInfo> downloadFilesDeleted) {
                            showToast(getString(R.string.main__delete_finish) + downloadFilesDeleted.size() +
                                    getString(R.string.main__failed3) + (downloadFilesNeedDelete.size() - 
                                    downloadFilesDeleted.size()));
                            updateAdapter();
                        }
                    });
                }
            }
        });

        mBtnMove.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                String oldDirPath = FileDownloader.getDownloadDir();

                final EditText etFileDir = new EditText(MainActivity.this);
                etFileDir.setText(oldDirPath);
                etFileDir.setFocusable(true);

                LinearLayout linearLayout = new LinearLayout(MainActivity.this);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams
                        .MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                linearLayout.addView(etFileDir, params);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(getString(R.string.main__confirm_the_dir_path_move_to)).setView(linearLayout)
                        .setNegativeButton(getString(R.string.main__dialog_btn_cancel), null);
                builder.setPositiveButton(getString(R.string.main__dialog_btn_confirm), new DialogInterface
                        .OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // move to file dir
                        String newDirPath = etFileDir.getText().toString().trim();

                        List<String> urls = new ArrayList<String>();

                        for (DownloadFileInfo downloadFileInfo : selectDownloadFileInfos) {
                            if (downloadFileInfo == null) {
                                continue;
                            }
                            urls.add(downloadFileInfo.getUrl());
                        }

                        // single move
                        if (urls.size() == 1) {
                            FileDownloader.move(urls.get(0), newDirPath, new OnMoveDownloadFileListener() {

                                @Override
                                public void onMoveDownloadFileSuccess(DownloadFileInfo downloadFileMoved) {
                                    showToast(getString(R.string.main__move_succeed) + downloadFileMoved.getFilePath());
                                    updateAdapter();
                                }

                                @Override
                                public void onMoveDownloadFilePrepared(DownloadFileInfo downloadFileNeedToMove) {
                                    showToast(getString(R.string.main__moving) + downloadFileNeedToMove.getFileName());
                                }

                                @Override
                                public void onMoveDownloadFileFailed(DownloadFileInfo downloadFileInfo, MoveDownloadFileFailReason failReason) {
                                    showToast(getString(R.string.main__move) + downloadFileInfo.getFileName() +
                                            getString(R.string.main__failed));
                                    Log.e("wlf", "出错回调，移动" + downloadFileInfo.getFileName() + "失败");
                                }
                            });
                        }
                        // multi move
                        else {
                            FileDownloader.move(urls, newDirPath, new OnMoveDownloadFilesListener() {

                                @Override
                                public void onMoveDownloadFilesPrepared(List<DownloadFileInfo> downloadFilesNeedMove) {
                                    showToast(getString(R.string.main__need_move) + downloadFilesNeedMove.size());
                                }

                                @Override
                                public void onMovingDownloadFiles(List<DownloadFileInfo> downloadFilesNeedMove, 
                                                                  List<DownloadFileInfo> downloadFilesMoved, 
                                                                  List<DownloadFileInfo> downloadFilesSkip, 
                                                                  DownloadFileInfo downloadFileMoving) {
                                    showToast(getString(R.string.main__moving) + downloadFileMoving.getFileName() +
                                            getString(R.string.main__progress) + (downloadFilesMoved.size() + 
                                            downloadFilesSkip.size()) + getString(R.string.main__failed2) +
                                            downloadFilesSkip.size() + getString(R.string
                                            .main__skip_and_total_delete_division) + downloadFilesNeedMove.size());
                                    updateAdapter();

                                }

                                @Override
                                public void onMoveDownloadFilesCompleted(List<DownloadFileInfo> 
                                                                                 downloadFilesNeedMove, 
                                                                         List<DownloadFileInfo> downloadFilesMoved) {
                                    showToast(getString(R.string.main__move_finish) + downloadFilesMoved.size() +
                                            getString(R.string.main__failed3) + (downloadFilesNeedMove.size() - 
                                            downloadFilesMoved.size()));
                                }
                            });
                        }

                    }
                });
                builder.show();
            }
        });

        mBtnRename.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                final List<String> urls = new ArrayList<String>();

                for (DownloadFileInfo downloadFileInfo : selectDownloadFileInfos) {
                    if (downloadFileInfo == null) {
                        continue;
                    }
                    if (TextUtils.isEmpty(downloadFileInfo.getUrl())) {
                        return;
                    }
                    urls.add(downloadFileInfo.getUrl());
                }

                if (urls.size() == 1) {

                    DownloadFileInfo downloadFileInfoNeedToRename = null;

                    for (DownloadFileInfo downloadFileInfo : selectDownloadFileInfos) {
                        if (downloadFileInfo == null) {
                            continue;
                        }
                        if (urls.get(0).equals(downloadFileInfo.getUrl())) {
                            downloadFileInfoNeedToRename = downloadFileInfo;
                            break;
                        }
                    }

                    if (downloadFileInfoNeedToRename == null) {
                        showToast(getString(R.string.main__can_not_rename));
                        return;
                    }

                    String oldName = downloadFileInfoNeedToRename.getFileName();

                    final EditText etFileName = new EditText(MainActivity.this);
                    etFileName.setText(oldName);
                    etFileName.setFocusable(true);

                    final CheckBox cbIncludedSuffix = new CheckBox(MainActivity.this);
                    cbIncludedSuffix.setChecked(true);
                    cbIncludedSuffix.setText(getString(R.string.main__rename_included_suffix));

                    LinearLayout linearLayout = new LinearLayout(MainActivity.this);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams
                            .MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    linearLayout.addView(etFileName, params);
                    params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams
                            .WRAP_CONTENT);
                    linearLayout.addView(cbIncludedSuffix, params);

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(getString(R.string.main__confirm_rename_info)).setView(linearLayout)
                            .setNegativeButton(getString(R.string.main__dialog_btn_cancel), null);
                    builder.setPositiveButton(getString(R.string.main__dialog_btn_confirm), new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            String newName = etFileName.getText().toString();

                            FileDownloader.rename(urls.get(0), newName, cbIncludedSuffix.isChecked(), new 
                                    OnRenameDownloadFileListener() {

                                @Override
                                public void onRenameDownloadFilePrepared(DownloadFileInfo downloadFileNeedRename) {

                                }

                                @Override
                                public void onRenameDownloadFileSuccess(DownloadFileInfo downloadFileRenamed) {
                                    showToast(getString(R.string.main__rename_succeed));
                                    updateAdapter();
                                }

                                @Override
                                public void onRenameDownloadFileFailed(DownloadFileInfo downloadFileInfo, RenameDownloadFileFailReason failReason) {
                                    showToast(getString(R.string.main__rename_failed));
                                    Log.e("wlf", "出错回调，重命名失败");
                                }
                            });
                        }
                    });
                    builder.show();
                } else {
                    showToast(getString(R.string.main__rename_failed_note));
                }
            }
        });

    }

    @Override
    public void onNoneSelect() {
        mLnlyOperation.setVisibility(View.GONE);
    }

}
