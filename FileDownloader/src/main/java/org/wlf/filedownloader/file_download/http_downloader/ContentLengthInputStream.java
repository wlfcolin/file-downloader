package org.wlf.filedownloader.file_download.http_downloader;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream wrapper
 * <br/>
 * 对InputStream的包装类
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class ContentLengthInputStream extends InputStream {

    private final InputStream mStream;
    private final long mLength;

    public ContentLengthInputStream(InputStream stream, long length) {
        this.mStream = stream;
        this.mLength = length;
    }

    public long getLength() {
        return mLength;
    }

    @Override
    public int available() {
        return (int) mLength;
    }

    @Override
    public void close() throws IOException {
        mStream.close();
    }

    @Override
    public void mark(int readLimit) {
        mStream.mark(readLimit);
    }

    @Override
    public int read() throws IOException {
        return mStream.read();
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return mStream.read(buffer);
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        return mStream.read(buffer, byteOffset, byteCount);
    }

    @Override
    public void reset() throws IOException {
        mStream.reset();
    }

    @Override
    public long skip(long byteCount) throws IOException {
        return mStream.skip(byteCount);
    }

    @Override
    public boolean markSupported() {
        return mStream.markSupported();
    }
}