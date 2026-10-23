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

package software.amazon.awssdk.httpbinding.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;

/**
 * Sends the remaining bytes of a {@link ByteBuffer} as a body without copying them.
 *
 * <ul>
 *   <li>The buffer's position and limit are captured when the provider is created; later changes to them by the caller
 *   do not change the body.</li>
 *   <li>The content is shared with the caller, as the earlier marshallers share an {@code SdkBytes} array, so the
 *   caller must not change it until the request is sent.</li>
 *   <li>{@link #newStream()} returns a new stream over the whole body on each call. The stream supports
 *   {@link InputStream#mark mark} and {@link InputStream#reset reset}, and {@code reset()} without an earlier
 *   {@code mark()} returns to the start, as {@link ByteArrayInputStream} does, so HTTP clients can repeat the body on a
 *   retry.</li>
 *   <li>{@link #contentAsByteBufferOrNull()} returns a new {@link ByteBuffer#duplicate() duplicate} on each call, so a
 *   read-only buffer stays read-only.</li>
 * </ul>
 *
 * <p>SDK counterpart of smithy-java {@code ByteBufferDataStream}.
 */
@SdkInternalApi
final class ByteBufferContentStreamProvider implements ContentStreamProvider {
    private final ByteBuffer content;

    private ByteBufferContentStreamProvider(ByteBuffer content) {
        this.content = content;
    }

    /**
     * Creates a provider of the remaining bytes of {@code buffer}, sharing its content. The buffer's position, limit and
     * mark are not changed.
     */
    static ByteBufferContentStreamProvider create(ByteBuffer buffer) {
        return new ByteBufferContentStreamProvider(buffer.duplicate());
    }

    /**
     * Returns the length of the body in bytes.
     */
    int length() {
        return content.remaining();
    }

    /**
     * Returns a new stream positioned at the start of the body: a {@link ByteArrayInputStream} over the backing array
     * when the buffer has an accessible one, otherwise a stream over a new duplicate of the buffer.
     */
    @Override
    public InputStream newStream() {
        if (content.hasArray()) {
            return new ByteArrayInputStream(content.array(), content.arrayOffset() + content.position(),
                                            content.remaining());
        }
        return new ByteBufferInputStream(content.duplicate());
    }

    /**
     * Returns a new duplicate of the body buffer, positioned at the start of the body.
     */
    @Override
    public ByteBuffer contentAsByteBufferOrNull() {
        return content.duplicate();
    }

    @Override
    public String name() {
        return ProviderType.BYTES.getName();
    }

    /**
     * Reads a buffer that has no accessible array (read-only or direct), with mark and reset. The mark starts at the
     * buffer's initial position.
     */
    private static final class ByteBufferInputStream extends InputStream {
        private final ByteBuffer buffer;
        private int mark;

        private ByteBufferInputStream(ByteBuffer buffer) {
            this.buffer = buffer;
            this.mark = buffer.position();
        }

        @Override
        public int read() {
            return buffer.hasRemaining() ? buffer.get() & 0xFF : -1;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException("off=" + off + ", len=" + len + ", length=" + b.length);
            }
            if (len == 0) {
                return 0;
            }
            if (!buffer.hasRemaining()) {
                return -1;
            }
            int n = Math.min(len, buffer.remaining());
            buffer.get(b, off, n);
            return n;
        }

        @Override
        public long skip(long n) {
            if (n <= 0) {
                return 0;
            }
            int skipped = (int) Math.min(n, buffer.remaining());
            buffer.position(buffer.position() + skipped);
            return skipped;
        }

        @Override
        public int available() {
            return buffer.remaining();
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public void mark(int readLimit) {
            mark = buffer.position();
        }

        @Override
        public void reset() {
            buffer.position(mark);
        }
    }
}
