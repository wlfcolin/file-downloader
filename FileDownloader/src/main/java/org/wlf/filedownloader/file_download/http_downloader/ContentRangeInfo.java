package org.wlf.filedownloader.file_download.http_downloader;

import android.text.TextUtils;

/**
 * use for http header Content-Range field info
 * <br/>
 * Http响应的头Content-Range字段所包含的信息
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class ContentRangeInfo {

    public final String contentType;// public final field
    public final long startPos;// public final field
    public final long endPos;// public final field
    public final long totalLength;// public final field

    private ContentRangeInfo(String contentType, long startPos, long endPos, long totalLength) {
        super();
        this.contentType = contentType;
        this.startPos = startPos;
        this.endPos = endPos;
        this.totalLength = totalLength;
    }

    /**
     * get ContentRangeInfo by contentRange field string
     *
     * @param contentRangeStr http header contentRange field string
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
                // get contentType
                contentType = contentRangeStrArrayTemp[0];
                String contentRangeAndLength = contentRangeStrArrayTemp[1];
                if (!TextUtils.isEmpty(contentRangeAndLength)) {
                    String[] contentRangeAndLengthArrayTemp = contentRangeAndLength.split("/");
                    if (contentRangeAndLengthArrayTemp != null && contentRangeAndLengthArrayTemp.length >= 2) {
                        String contentRanges = contentRangeAndLengthArrayTemp[0];
                        String[] contentRangesArrayTemp = contentRanges.split("-");
                        if (contentRangesArrayTemp != null && contentRangesArrayTemp.length >= 2) {
                            // get rangeStartPos
                            rangeStartPos = contentRangesArrayTemp[0];
                            // get rangeEndPos
                            rangeEndPos = contentRangesArrayTemp[1];
                        }
                        // get totalLength
                        totalLength = contentRangeAndLengthArrayTemp[1];
                    }
                }
            }
        }

        // create ContentRangeInfo
        if (!TextUtils.isEmpty(contentType) && !TextUtils.isEmpty(rangeStartPos) && !TextUtils.isEmpty(rangeEndPos) 
                && !TextUtils.isEmpty(totalLength)) {
            try {
                long startPos = Long.parseLong(rangeStartPos);
                long endPos = Long.parseLong(rangeEndPos);
                long totalLen = Long.parseLong(totalLength);

                contentRangeInfo = new ContentRangeInfo(contentType.trim(), startPos, endPos + 1, totalLen);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return contentRangeInfo;
    }
}
