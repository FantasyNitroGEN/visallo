package org.visallo.core.util;

import com.google.common.base.Preconditions;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class LimitInputStream extends FilterInputStream {
    private long left;
    private long mark = -1L;

    public LimitInputStream(InputStream in, long limit) {
        super(in);
        checkNotNull(in);
        checkArgument(limit >= 0L, "limit must be non-negative");
        this.left = limit;
    }

    public int available() throws IOException {
        return (int) Math.min((long) this.in.available(), this.left);
    }

    public synchronized void mark(int readLimit) {
        this.in.mark(readLimit);
        this.mark = this.left;
    }

    public int read() throws IOException {
        if (this.left == 0L) {
            return -1;
        } else {
            int result = this.in.read();
            if (result != -1) {
                --this.left;
            }

            return result;
        }
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (this.left == 0L) {
            return -1;
        } else {
            len = (int) Math.min((long) len, this.left);
            int result = this.in.read(b, off, len);
            if (result != -1) {
                this.left -= (long) result;
            }

            return result;
        }
    }

    public synchronized void reset() throws IOException {
        if (!this.in.markSupported()) {
            throw new IOException("Mark not supported");
        } else if (this.mark == -1L) {
            throw new IOException("Mark not set");
        } else {
            this.in.reset();
            this.left = this.mark;
        }
    }

    public long skip(long n) throws IOException {
        n = Math.min(n, this.left);
        long skipped = this.in.skip(n);
        this.left -= skipped;
        return skipped;
    }
}

