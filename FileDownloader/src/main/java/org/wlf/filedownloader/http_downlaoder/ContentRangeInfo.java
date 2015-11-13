package org.wlf.filedownloader.http_downlaoder;

import android.text.TextUtils;

/**
 * Http响应的头Content-Range字段所包含的信息
 * 
 * @author wlf
 * 
 */
public class ContentRangeInfo {

	public final String contentType;// 公共字段
	public final int startPos;// 公共字段
	public final int endPos;// 公共字段
	public final int totalLength;// 公共字段

	public ContentRangeInfo(String contentType, int startPos, int endPos, int totalLength) {
		super();
		this.contentType = contentType;
		this.startPos = startPos;
		this.endPos = endPos;
		this.totalLength = totalLength;
	}

	/**
	 * 根据contentRange字符串获取ContentRangeInfo
	 * 
	 * @param contentRangeStr
	 *            contentRange字符串
	 * @return ContentRangeInfo
	 */
	public static ContentRangeInfo getContentRangeInfo(String contentRangeStr) {

		ContentRangeInfo contentRangeInfo = null;

		String contentType = null;
		String rangeStartPos = null;
		String rangeEndPos = null;
		String totalLength = null;

		if (!TextUtils.isEmpty(contentRangeStr)) {

			String[] contentRangeStrArrayTemp = contentRangeStr.split(" ");

			if (contentRangeStrArrayTemp != null && contentRangeStrArrayTemp.length >= 2) {

				// 拿到contentType
				contentType = contentRangeStrArrayTemp[0];

				String contentRangeAndLength = contentRangeStrArrayTemp[1];

				if (!TextUtils.isEmpty(contentRangeAndLength)) {

					String[] contentRangeAndLengthArrayTemp = contentRangeAndLength.split("/");

					if (contentRangeAndLengthArrayTemp != null && contentRangeAndLengthArrayTemp.length >= 2) {

						String contentRanges = contentRangeAndLengthArrayTemp[0];

						String[] contentRangesArrayTemp = contentRanges.split("-");

						if (contentRangesArrayTemp != null && contentRangesArrayTemp.length >= 2) {

							// 拿到rangeStartPos
							rangeStartPos = contentRangesArrayTemp[0];

							// 拿到rangeEndPos
							rangeEndPos = contentRangesArrayTemp[1];
						}
						// 拿到contentLength
						totalLength = contentRangeAndLengthArrayTemp[1];
					}
				}
			}
		}

		// 若合法则构建ContentRangeInfo
		if (!TextUtils.isEmpty(contentType) && !TextUtils.isEmpty(rangeStartPos) && !TextUtils.isEmpty(rangeEndPos)
				&& !TextUtils.isEmpty(totalLength)) {

			try {

				int startPos = Integer.parseInt(rangeStartPos);
				int endPos = Integer.parseInt(rangeEndPos);
				int totalLen = Integer.parseInt(totalLength);

				contentRangeInfo = new ContentRangeInfo(contentType.trim(), startPos, endPos + 1, totalLen);

			} catch (NumberFormatException e) {
				e.printStackTrace();
			}

		}

		return contentRangeInfo;
	}
}
