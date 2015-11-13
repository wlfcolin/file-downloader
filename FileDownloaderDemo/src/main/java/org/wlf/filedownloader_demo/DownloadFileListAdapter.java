package org.wlf.filedownloader_demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloadManager;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.lisener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader_demo.util.ApkUtil;
import org.wlf.filedownloader_demo.util.TimeUtil;

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

/**
 * 测试下载列表适配器
 * 
 * @author wlf
 * 
 */
public class DownloadFileListAdapter extends BaseAdapter implements OnFileDownloadStatusListener {

	/** LOG TAG */
	private static final String TAG = DownloadFileListAdapter.class.getSimpleName();

	// 所有的下载信息
	private List<DownloadFileInfo> mDownloadFileInfos = Collections.synchronizedList(new ArrayList<DownloadFileInfo>());
	// 下载缓存的views
	private Map<String, View> mConvertViews = new LinkedHashMap<String, View>();
	// 选中的文件
	private List<DownloadFileInfo> mSelectedDownloadFileInfos = new ArrayList<DownloadFileInfo>();

	private Activity mActivity;

	private Toast mToast;

	private OnItemSelectLisener mOnItemSelectLisener;

	public DownloadFileListAdapter(Activity activity) {
		super();
		this.mActivity = activity;
		initDownloadFileInfos();
	}

	// 初始化DownloadFileInfos
	private void initDownloadFileInfos() {
		this.mDownloadFileInfos = FileDownloadManager.getInstance(mActivity).getDownloadFiles();
		mConvertViews.clear();
	}

	/** 更新数据 */
	public void updateShow() {
		initDownloadFileInfos();
		notifyDataSetChanged();
	}

	public void setOnItemSelectLisener(OnItemSelectLisener onItemSelectLisener) {
		this.mOnItemSelectLisener = onItemSelectLisener;
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

		// 获取出最新的下载信息
		DownloadFileInfo downloadFileInfo = getItem(position);

		if (downloadFileInfo == null) {
			return null;
		}

		if (TextUtils.isEmpty(downloadFileInfo.getUrl())) {
			// 把缓存的View删除
			mConvertViews.remove(downloadFileInfo.getUrl());
			return null;
		}

		final String url = downloadFileInfo.getUrl();

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

		// 下载名
		tvFileName.setText(downloadFileInfo.getFileName());

		// 下载进度
		pbProgress.setMax(downloadFileInfo.getFileSize());
		pbProgress.setProgress(downloadFileInfo.getDownloadedSize());

		// 下载大小
		float downloadSize = downloadFileInfo.getDownloadedSize() / 1024f / 1024;
		float fileSize = downloadFileInfo.getFileSize() / 1024f / 1024;

		tvDownloadSize.setText(((float) (Math.round(downloadSize * 100)) / 100) + "M/");
		tvTotalSize.setText(((float) (Math.round(fileSize * 100)) / 100) + "M");

		// 下载百分比
		float percent = downloadSize / fileSize * 100;
		tvPercent.setText(((float) (Math.round(percent * 100)) / 100) + "%");

		switch (downloadFileInfo.getStatus()) {
		// 未知状态，点击则开始下载
		case Status.DOWNLOAD_STATUS_UNKNOWN:
			tvText.setText("无法下载，请删除后重新下载");
			break;
		// 正在获取资源，点击则开始下载
		case Status.DOWNLOAD_STATUS_PREPARING:
			tvText.setText("正在获取资源");
			break;
		// 已连接资源，点击则开始下载
		case Status.DOWNLOAD_STATUS_PREPARED:
			tvText.setText("已连接资源");
			break;
		case Status.DOWNLOAD_STATUS_PAUSED:// 暂停状态
			tvText.setText("已暂停");
			break;
		case Status.DOWNLOAD_STATUS_DOWNLOADING:// 正在下载
			tvText.setText("正在下载");
			break;
		case Status.DOWNLOAD_STATUS_ERROR:// 出错
			tvText.setText("下载出错");
			break;
		case Status.DOWNLOAD_STATUS_WAITING:// 等待下载，准备下载
			tvText.setText("等待下载");
			break;
		case Status.DOWNLOAD_STATUS_COMPLETED:// 下载完成
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
		case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:// 已经开始下载过，但是文件不存在了
			tvDownloadSize.setText("");
			tvText.setText("文件不存在");
			break;
		}

		cbSelect.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

				// 文件下载管理器
				final FileDownloadManager fileDownloadManager = FileDownloadManager.getInstance(buttonView.getContext());

				if (isChecked) {
					mSelectedDownloadFileInfos.add(fileDownloadManager.getDownloadFileByUrl(url));
					if (mOnItemSelectLisener != null) {
						mOnItemSelectLisener.onSelected(mSelectedDownloadFileInfos);
					}
				} else {
					mSelectedDownloadFileInfos.remove(fileDownloadManager.getDownloadFileByUrl(url));
					if (mSelectedDownloadFileInfos.isEmpty()) {
						if (mOnItemSelectLisener != null) {
							mOnItemSelectLisener.onNoSelect();
						}
					} else {
						if (mOnItemSelectLisener != null) {
							mOnItemSelectLisener.onSelected(mSelectedDownloadFileInfos);
						}
					}
				}
			}
		});

		// 设置背景item点击事件
		setBackgroundOnclickLisener(lnlyDownloadItem, downloadFileInfo);

		return cacheConvertView;
	}

	// 设置点击事件
	private void setBackgroundOnclickLisener(final View lnlyDownloadItem, final DownloadFileInfo curDownloadFileInfo) {

		// 设置监听事件
		lnlyDownloadItem.setOnClickListener(new OnClickListener() {

			// 文件下载管理器
			final FileDownloadManager fileDownloadManager = FileDownloadManager.getInstance(lnlyDownloadItem
					.getContext());
			// 下载监听器
			final OnFileDownloadStatusListener mOnFileDownloadStatusListener = DownloadFileListAdapter.this;

			@Override
			public void onClick(View v) {

				final Context context = v.getContext();

				if (curDownloadFileInfo != null) {

					switch (curDownloadFileInfo.getStatus()) {
					// 未知状态
					case Status.DOWNLOAD_STATUS_UNKNOWN:
						showToast("无法下载，请删除：" + curDownloadFileInfo.getFilePath() + " 后重新下载");
						break;
					// 等待下载
					case Status.DOWNLOAD_STATUS_WAITING:
						showToast(curDownloadFileInfo.getFileName() + "的下载任务正在排队，请稍后");
						break;
					// 暂停状态，点击则开始下载
					// 出错，点击则开始下载
					case Status.DOWNLOAD_STATUS_ERROR:
					case Status.DOWNLOAD_STATUS_PAUSED:
						fileDownloadManager.start(curDownloadFileInfo.getUrl(), mOnFileDownloadStatusListener);
						showToast("开始/继续下载：" + curDownloadFileInfo.getFileName());
						break;
					case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:// 已经开始下载过，但是文件不存在了
						// 弹出对话框
						AlertDialog.Builder builder = new AlertDialog.Builder(context);
						builder.setTitle("是否重新下载").setNegativeButton("取消", null);
						builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								// 重新下载
								fileDownloadManager.reStart(curDownloadFileInfo.getUrl(), mOnFileDownloadStatusListener);
								showToast("重新下载下载：" + curDownloadFileInfo.getFileName());
							}
						});
						// 显示对话框
						builder.show();
						break;
					// 正在下载，点击则暂停下载
					case Status.DOWNLOAD_STATUS_PREPARING:
					case Status.DOWNLOAD_STATUS_PREPARED:
					case Status.DOWNLOAD_STATUS_DOWNLOADING:
						// 暂停下载
						fileDownloadManager.pause(curDownloadFileInfo.getUrl());

						showToast("暂停下载：" + curDownloadFileInfo.getFileName());

						TextView tvText = (TextView) lnlyDownloadItem.findViewById(R.id.tvText);
						if (tvText != null) {
							tvText.setText("已暂停");
						}
						break;
					case Status.DOWNLOAD_STATUS_COMPLETED:// 下载完成

						TextView tvDownloadSize = (TextView) lnlyDownloadItem.findViewById(R.id.tvDownloadSize);
						if (tvDownloadSize != null) {
							tvDownloadSize.setText("");
						}

						final TextView tvText2 = (TextView) lnlyDownloadItem.findViewById(R.id.tvText);

						if ("apk".equalsIgnoreCase(FileUtil.getFileSuffix(curDownloadFileInfo.getFileName()))) {// apk

							final String packageName = ApkUtil.getUnInstallApkPckageName(context,
									curDownloadFileInfo.getFilePath());
							boolean isInstall = ApkUtil.checkAppInstalled(context, packageName);

							if (isInstall) {
								if (tvText2 != null) {
									tvText2.setText("打开");
									try {
										Intent intent2 = mActivity.getPackageManager().getLaunchIntentForPackage(
												packageName);
										mActivity.startActivity(intent2);
									} catch (Exception e) {
										e.printStackTrace();
										// 弹出安装框
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

	// 显示toast
	private void showToast(CharSequence text) {
		if (mToast == null) {
			mToast = Toast.makeText(mActivity, text, Toast.LENGTH_SHORT);
		} else {
			mToast.cancel();
			mToast = Toast.makeText(mActivity, text, Toast.LENGTH_SHORT);
		}
		mToast.show();
	}

	/** 添加一个新的下载信息 */
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
				mDownloadFileInfos.add(downloadFileInfo);// 添加一个下载信息
				notifyDataSetChanged();
				return true;
			}
		}
		return false;
	}

	// ///////////////////////////////////////////////////////////

	// ----------------------下载状态回调----------------------
	@Override
	public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {

		if (downloadFileInfo == null) {
			return;
		}

		// 添加到新列表，如果没有添加的话
		if (addNewDownloadFileInfo(downloadFileInfo)) {
			// 添加成功
		} else {
			String url = downloadFileInfo.getUrl();
			View cacheConvertView = mConvertViews.get(url);
			if (cacheConvertView != null) {
				TextView tvText = (TextView) cacheConvertView.findViewById(R.id.tvText);
				tvText.setText("等待下载");

				Log.w(TAG, "onFileDownloadStatusWaiting，url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_WAITING
						+ ")：" + downloadFileInfo.getStatus());
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

			Log.w(TAG, "onFileDownloadStatusPreparing，url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_PREPARING
					+ ")：" + downloadFileInfo.getStatus());
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

			Log.w(TAG, "onFileDownloadStatusPrepared，url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_PREPARED
					+ ")：" + downloadFileInfo.getStatus());
		} else {
			updateShow();
		}
	}

	@Override
	public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed,
			long remainingTime) {

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

			// 下载进度
			// pbProgress.setMax(downloadFileInfo.getFileSize());
			pbProgress.setProgress(downloadFileInfo.getDownloadedSize());

			// 下载大小
			float downloadSize = downloadFileInfo.getDownloadedSize() / 1024f / 1024f;
			float fileSize = downloadFileInfo.getFileSize() / 1024f / 1024f;

			tvDownloadSize.setText(((float) (Math.round(downloadSize * 100)) / 100) + "M/");
			tvTotalSize.setText(((float) (Math.round(fileSize * 100)) / 100) + "M");

			// 下载百分比
			float percent = downloadSize / fileSize * 100;
			tvPercent.setText(((float) (Math.round(percent * 100)) / 100) + "%");

			// 下载速度和剩余时间
			tvText.setText(((float) (Math.round(downloadSpeed * 100)) / 100) + "KB/s" + "  "
					+ TimeUtil.seconds2HH_mm_ss(remainingTime));

			// 重新设置背景item点击事件？
			setBackgroundOnclickLisener(lnlyDownloadItem, downloadFileInfo);
		} else {
			updateShow();
		}

		Log.w(TAG, "onFileDownloadStatusDownloading，url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_DOWNLOADING
				+ ")：" + downloadFileInfo.getStatus());

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

			// 设置背景item点击事件
			setBackgroundOnclickLisener(lnlyDownloadItem, downloadFileInfo);

			Log.w(TAG, "onFileDownloadStatusPaused，url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_PAUSED + ")："
					+ downloadFileInfo.getStatus());
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

			// 下载百分比
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

			// 设置背景item点击事件
			setBackgroundOnclickLisener(lnlyDownloadItem, downloadFileInfo);
		} else {
			updateShow();
		}

		Log.w(TAG, "onFileDownloadStatusCompleted，url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_COMPLETED
				+ ")：" + downloadFileInfo.getStatus());

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
				}
				else if(OnFileDownloadStatusFailReason.TYPE_FILE_IS_DOWNLOADING.equals(failReason.getType())){
					msg = downloadFileInfo.getFileName()+" 正在下载";
					showToast(msg);
				}
				else if(OnFileDownloadStatusFailReason.TYPE_URL_ILLEGAL.equals(failReason.getType())){
					msg = "Url不合法";
					showToast(msg);
				}
			}

			// 设置背景item点击事件
			setBackgroundOnclickLisener(lnlyDownloadItem, downloadFileInfo);

			Log.w(TAG, "出错回调，onFileDownloadStatusFailed，url：" + url + "，status(正常应该是" + Status.DOWNLOAD_STATUS_ERROR + ")："
					+ downloadFileInfo.getStatus());
		} else {
			updateShow();
		}

	}

	// ----------------------其它回调----------------------

	public interface OnItemSelectLisener {

		void onSelected(List<DownloadFileInfo> selectDownloadFileInfos);

		void onNoSelect();
	}

}
