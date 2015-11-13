package org.wlf.filedownloader.http_downlaoder;

/**
 * 表示数据范围
 * 
 * @author wlf
 * 
 */
public class Range {

	public final int startPos;// 公共字段
	public final int endPos;// 公共字段

	public Range(int startPos, int endPos) {
		super();
		this.startPos = startPos;
		this.endPos = endPos;
	}

	public int getLength() {
		return endPos - startPos;
	}

	/**
	 * 检查一个Range是否合法
	 * 
	 * @param range
	 * @return true表示合法
	 */
	public static boolean isLegal(Range range) {
		if (range != null && range.startPos > 0 && range.endPos > 0 && range.endPos > range.startPos) {
			return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Range) {
			Range other = (Range) o;
			if (other.startPos == startPos && other.endPos == endPos) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "[" + startPos + "," + endPos + "]";
	}
}
