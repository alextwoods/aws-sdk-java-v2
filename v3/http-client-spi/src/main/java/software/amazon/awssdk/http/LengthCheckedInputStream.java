/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.http;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream that fails with {@link ContentLengthMismatchException} when its source ends before, or runs past,
 * a declared content length.
 */
final class LengthCheckedInputStream extends FilterInputStream {
    private final long expected;
    private long count;

    private LengthCheckedInputStream(InputStream in, long expected) {
        super(in);
        this.expected = expected;
    }

    /** {@code in}, checked against {@code expected} if it is known (non-negative). */
    static InputStream of(InputStream in, long expected) {
        return expected < 0 ? in : new LengthCheckedInputStream(in, expected);
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b < 0) {
            requireComplete();
        } else {
            advance(1);
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n < 0) {
            requireComplete();
        } else {
            advance(n);
        }
        return n;
    }

    /** Skips by reading, so that skipped bytes are counted. */
    @Override
    public long skip(long n) throws IOException {
        byte[] scratch = new byte[(int) Math.min(Math.max(n, 0), 8192)];
        long skipped = 0;
        while (skipped < n) {
            int r = read(scratch, 0, (int) Math.min(scratch.length, n - skipped));
            if (r < 0) {
                break;
            }
            skipped += r;
        }
        return skipped;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void mark(int readlimit) {
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset is not supported");
    }

    private void advance(int n) throws ContentLengthMismatchException {
        count += n;
        if (count > expected) {
            throw new ContentLengthMismatchException(expected, count);
        }
    }

    private void requireComplete() throws ContentLengthMismatchException {
        if (count != expected) {
            throw new ContentLengthMismatchException(expected, count);
        }
    }
}
