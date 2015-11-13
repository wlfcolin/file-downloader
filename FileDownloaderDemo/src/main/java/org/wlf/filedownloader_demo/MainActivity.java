package org.wlf.filedownloader_demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloadManager;
import org.wlf.filedownloader.base.FailReason;
import org.wlf.filedownloader.lisener.OnDeleteDownloadFileListener;
import org.wlf.filedownloader.lisener.OnDeleteDownloadFilesListener;
import org.wlf.filedownloader.lisener.OnDetectUrlFileListener;
import org.wlf.filedownloader.lisener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.lisener.OnMoveDownloadFileListener;
import org.wlf.filedownloader.lisener.OnMoveDownloadFilesListener;
import org.wlf.filedownloader.lisener.OnRenameDownloadFileListener;
import org.wlf.filedownloader_demo.DownloadFileListAdapter.OnItemSelectLisener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试主界面
 * 
 * @author wlf
 * 
 */
public class MainActivity extends Activity implements OnDetectUrlFileListener, OnItemSelectLisener {

	// 适配器
	private DownloadFileListAdapter mDownloadFileListAdapter;

	// 文件下载状态改变监听器
	private OnFileDownloadStatusListener mOnFileDownloadStatusListener;
	// 探测网络文件监听器
	private OnDetectUrlFileListener mOnDetectUrlFileListener;

	// 文件下载管理器
	private FileDownloadManager mFileDownloadManager;

	// toast
	private Toast mToast;

	private LinearLayout lnlyOperation;
	private Button btnDelete;
	private Button btnMove;
	private Button btnRename;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		lnlyOperation = (LinearLayout) findViewById(R.id.lnlyOperation);
		btnDelete = (Button) findViewById(R.id.btnDelete);
		btnMove = (Button) findViewById(R.id.btnMove);
		btnRename = (Button) findViewById(R.id.btnRename);

		// ListView
		ListView lvDownloadFileList = (ListView) findViewById(R.id.lvDownloadFileList);
		// 将当前下载任务显示到listview的adapter中，只要是未完成的任务都支持：开始、暂停等操作，完成的任务支持删除、重命名、移动等操作
		mDownloadFileListAdapter = new DownloadFileListAdapter(this);
		lvDownloadFileList.setAdapter(mDownloadFileListAdapter);

		// 指定list的adapter实现文件下载状态改变监听器
		mOnFileDownloadStatusListener = mDownloadFileListAdapter;
		// 指定当前activity实现文件下载状态改变监听器
		mOnDetectUrlFileListener = this;

		// 初始化文件下载管理器
		mFileDownloadManager = FileDownloadManager.getInstance(this);
		
		mDownloadFileListAdapter.setOnItemSelectLisener(this);
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
		getMenuInflater().inflate(R.menu.options, menu);// 初始化OptionsMenu
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {// 处理OptionsMenu

		// Item选择事件
		switch (item.getItemId()) {
		case R.id.optionsNew:// 新建下载
			// 弹出对话框输入下载地址
			showNewDownloadDialog();
			return true;
		case R.id.optionsNews:// 新建下载（批量）
			// 弹出对话框输入下载地址
			showMultiNewDownloadDialog();
			return true;
		case R.id.optionsNewWithDetect:// 新建下载（自定义保存路径和名称）
			// 弹出对话框输入下载地址
			showCustomNewDownloadDialog();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// 显示新建下载对话框
	private void showNewDownloadDialog() {

		final EditText etUrl = new EditText(this);
		etUrl.setText("http://182.254.149.157/ftp/image/shop/product/儿童英语升华&￥.apk");
		etUrl.setFocusable(true);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("请输入要下载文件的地址").setView(etUrl).setNegativeButton("取消", null);
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// 下载url
				String url = etUrl.getText().toString().trim();
				mFileDownloadManager.start(url, mOnFileDownloadStatusListener);
			}
		});
		// 显示对话框
		builder.show();
	}

	// 显示批量新建下载对话框
	private void showMultiNewDownloadDialog() {

		final EditText etUrl1 = new EditText(this);
		etUrl1.setText("http://img13.360buyimg.com/n1/g14/M01/1B/1F/rBEhVlM03iwIAAAAAAFJnWsj5UAAAK8_gKFgkMAAUm1950.jpg");
		etUrl1.setFocusable(true);

		final EditText etUrl2 = new EditText(this);
		etUrl2.setText("http://img10.360buyimg.com/n1/jfs/t853/355/1172323504/52399/1e48e004/557e4325N54137a0d.jpg");
		etUrl2.setFocusable(true);

		final EditText etUrl3 = new EditText(this);
		etUrl3.setText("http://img13.360buyimg.com/n1/jfs/t1144/281/125705764/55544/2c37837b/55000151Ne909045f.jpg");
		etUrl3.setFocusable(true);

		LinearLayout linearLayout = new LinearLayout(this);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		linearLayout.addView(etUrl1, params);
		linearLayout.addView(etUrl2, params);
		linearLayout.addView(etUrl3, params);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("请输入要下载文件的所有地址").setView(linearLayout).setNegativeButton("取消", null);
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// 下载urls
				String url1 = etUrl1.getText().toString().trim();
				String url2 = etUrl2.getText().toString().trim();
				String url3 = etUrl3.getText().toString().trim();

				List<String> urls = new ArrayList<String>();
				urls.add(url1);
				urls.add(url2);
				urls.add(url3);

				mFileDownloadManager.start(urls, mOnFileDownloadStatusListener);
			}
		});
		// 显示对话框
		builder.show();
	}

	// 显示自定义保存路径和名称的新建下载对话框
	private void showCustomNewDownloadDialog() {

		final EditText etUrlCustom = new EditText(this);
		etUrlCustom
				.setText("http://182.254.149.157/ftp/image/shop/product/儿童英语拓展篇HD_air.com.congcongbb.yingyue.mi_1000000.apk");
		etUrlCustom.setFocusable(true);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("请输入要下载文件的地址").setView(etUrlCustom).setNegativeButton("取消", null);
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// 下载url
				String url = etUrlCustom.getText().toString().trim();
				mFileDownloadManager.detect(url, mOnDetectUrlFileListener);
			}
		});
		// 显示对话框
		builder.show();
	}

	@Override
	protected void onDestroy() {
		// 暂停所有下载
		mFileDownloadManager.pauseAll();
		super.onDestroy();
	}

	// 显示toast
	private void showToast(CharSequence text) {
		if (mToast == null) {
			mToast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
		} else {
			mToast.cancel();
			mToast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
		}
		mToast.show();
	}

	// ///////////////////////////////////////////////////////////

	// ----------------------探测文件回调----------------------

	// 探测到需要创建文件
	@Override
	public void onDetectNewDownloadFile(final String url, String fileName, final String saveDir, int fileSize) {
		// 弹出下载文件信息框
		final TextView tvFileDir = new TextView(MainActivity.this);
		tvFileDir.setText("保存位置");

		final EditText etFileDir = new EditText(MainActivity.this);
		etFileDir.setText(saveDir);
		etFileDir.setFocusable(true);

		final TextView tvFileName = new TextView(MainActivity.this);
		tvFileName.setText("保存名称");

		final EditText etFileName = new EditText(MainActivity.this);
		etFileName.setText(fileName);
		etFileName.setFocusable(true);

		final TextView tvFileSize = new TextView(MainActivity.this);
		tvFileSize.setText("文件大小：" + (fileSize / 1024f / 1024f) + " M");

		LinearLayout linearLayout = new LinearLayout(MainActivity.this);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		linearLayout.addView(tvFileDir, params);
		linearLayout.addView(etFileDir, params);
		linearLayout.addView(tvFileName, params);
		linearLayout.addView(etFileName, params);
		linearLayout.addView(tvFileSize, params);

		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("请确认/修改保存路径和名称").setView(linearLayout).setNegativeButton("取消", null);
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// 下载文件保存
				String newFileDir = etFileDir.getText().toString().trim();
				// 下载文件保存名称
				String newFileName = etFileName.getText().toString().trim();
				// 新建下载
				showToast("探测文件，新建下载：" + url);
				Log.e("wlf", "探测文件，新建下载：" + url);
				mFileDownloadManager.createAndStart(url, newFileDir, newFileName, mOnFileDownloadStatusListener);
			}
		});
		// 显示对话框
		builder.show();
	}

	// 探测文件已经存在下载
	@Override
	public void onDetectUrlFileExist(String url) {// FIXME 改进成返回封装的model对象？
		showToast("探测文件，继续下载：" + url);
		Log.e("wlf", "探测文件，继续下载：" + url);
		// 继续下载（如果没有下载完的话）
		mFileDownloadManager.start(url, mOnFileDownloadStatusListener);
	}

	// 探测文件出错
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
		showToast("探测文件出错：" + msg + "," + url);
		Log.e("wlf", "出错回调，探测文件出错：" + msg + "," + url);
	}
	
	private void updateAdapter(){
		if(mDownloadFileListAdapter == null){
			return;
		}
		mDownloadFileListAdapter.updateShow();
	}

	@Override
	public void onSelected(final List<DownloadFileInfo> selectDownloadFileInfos) {

		lnlyOperation.setVisibility(View.VISIBLE);

		btnDelete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				List<String> urls = new ArrayList<String>();

				for (DownloadFileInfo downloadFileInfo : selectDownloadFileInfos) {
					if (downloadFileInfo == null) {
						continue;
					}
					urls.add(downloadFileInfo.getUrl());
				}

				// 单个删除
				if (urls.size() == 1) {
					mFileDownloadManager.delete(urls.get(0), true, new OnDeleteDownloadFileListener() {
						@Override
						public void onDeleteDownloadFileSuccess(DownloadFileInfo downloadFileDeleted) {
							showToast("删除成功");
							updateAdapter();
						}

						@Override
						public void onDeleteDownloadFilePrepared(DownloadFileInfo downloadFileNeedDelete) {
							showToast("正在删除" + downloadFileNeedDelete.getFileName() + "....");
						}

						@Override
						public void onDeleteDownloadFileFailed(DownloadFileInfo downloadFileInfo, FailReason failReason) {
							showToast("删除" + downloadFileInfo.getFileName() + "失败");
							Log.e("wlf", "出错回调，删除" + downloadFileInfo.getFileName() + "失败");
						}
					});
				}
				// 批量删除
				else {
					mFileDownloadManager.delete(urls, true, new OnDeleteDownloadFilesListener() {

						@Override
						public void onDeletingDownloadFiles(List<DownloadFileInfo> downloadFilesNeedDelete,
								List<DownloadFileInfo> downloadFilesDeleted, List<DownloadFileInfo> downloadFilesSkip,
								DownloadFileInfo downloadFileDeleting) {
							showToast("正在删除" + downloadFileDeleting.getFileName() + "，进度："
									+ (downloadFilesDeleted.size() + downloadFilesSkip.size()) + "(失败："
									+ downloadFilesSkip.size() + ")/" + downloadFilesNeedDelete.size());
							updateAdapter();
						}

						@Override
						public void onDeleteDownloadFilesPrepared(List<DownloadFileInfo> downloadFilesNeedDelete) {
							showToast("需要删除" + downloadFilesNeedDelete.size());
						}

						@Override
						public void onDeleteDownloadFilesCompleted(List<DownloadFileInfo> downloadFilesNeedDelete,
								List<DownloadFileInfo> downloadFilesDeleted) {
							showToast("删除完成，成功：" + downloadFilesDeleted.size() + "，失败："
									+ (downloadFilesNeedDelete.size() - downloadFilesDeleted.size()));
							updateAdapter();
						}
					});
				}
			}
		});

		btnMove.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				final String newDirPath = Environment.getDataDirectory().getAbsolutePath() + File.separator + "测试移动文件夹";

				List<String> urls = new ArrayList<String>();

				for (DownloadFileInfo downloadFileInfo : selectDownloadFileInfos) {
					if (downloadFileInfo == null) {
						continue;
					}
					urls.add(downloadFileInfo.getUrl());
				}

				// 单个移动
				if (urls.size() == 1) {
					mFileDownloadManager.move(urls.get(0), newDirPath, new OnMoveDownloadFileListener() {

						@Override
						public void onMoveDownloadFileSuccess(DownloadFileInfo downloadFileMoved) {
							showToast("移动成功，新路径：" + downloadFileMoved.getFilePath());
							updateAdapter();

						}

						@Override
						public void onMoveDownloadFilePrepared(DownloadFileInfo downloadFileNeedToMove) {
							showToast("正在移动" + downloadFileNeedToMove.getFileName() + "....");
						}

						@Override
						public void onMoveDownloadFileFailed(DownloadFileInfo downloadFileInfo,
								OnMoveDownloadFileFailReason failReason) {
							showToast("移动" + downloadFileInfo.getFileName() + "失败");
							Log.e("wlf", "出错回调，移动" + downloadFileInfo.getFileName() + "失败");
						}
					});
				}
				// 批量移动
				else {
					mFileDownloadManager.move(urls, newDirPath, new OnMoveDownloadFilesListener() {

						@Override
						public void onMoveDownloadFilesPrepared(List<DownloadFileInfo> downloadFilesNeedMove) {
							showToast("需要移动" + downloadFilesNeedMove.size());
						}

						@Override
						public void onMovingDownloadFiles(List<DownloadFileInfo> downloadFilesNeedMove,
								List<DownloadFileInfo> downloadFilesMoved, List<DownloadFileInfo> downloadFilesSkip,
								DownloadFileInfo downloadFileMoving) {
							showToast("正在移动" + downloadFileMoving.getFileName() + "，进度："
									+ (downloadFilesMoved.size() + downloadFilesSkip.size()) + "(失败："
									+ downloadFilesSkip.size() + ")/" + downloadFilesNeedMove.size());
							updateAdapter();

						}

						@Override
						public void onMoveDownloadFilesCompleted(List<DownloadFileInfo> downloadFilesNeedMove,
								List<DownloadFileInfo> downloadFilesMoved) {
							showToast("移动完成，成功：" + downloadFilesMoved.size() + "，失败："
									+ (downloadFilesNeedMove.size() - downloadFilesMoved.size()));
						}

					});
				}

			}
		});

		btnRename.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				List<String> urls = new ArrayList<String>();

				for (DownloadFileInfo downloadFileInfo : selectDownloadFileInfos) {
					if (downloadFileInfo == null) {
						continue;
					}
					urls.add(downloadFileInfo.getUrl());
				}

				if (urls.size() == 1) {
					mFileDownloadManager.rename(urls.get(0), "ABC", new OnRenameDownloadFileListener() {

						@Override
						public void onRenameDownloadFileSuccess(DownloadFileInfo downloadFileRenamed) {
							showToast("重命名成功");
							updateAdapter();
						}

						@Override
						public void onRenameDownloadFileFailed(DownloadFileInfo downloadFileInfo, FailReason failReason) {
							showToast("重命名失败");
							Log.e("wlf", "出错回调，重命名失败");
						}
					});
				} else {
					showToast("只支持单个文件重命名");
				}
			}
		});

	}

	@Override
	public void onNoSelect() {
		lnlyOperation.setVisibility(View.GONE);
	}

}
