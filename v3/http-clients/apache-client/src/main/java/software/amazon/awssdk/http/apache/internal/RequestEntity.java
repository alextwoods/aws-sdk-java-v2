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

package software.amazon.awssdk.http.apache.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import org.apache.http.entity.AbstractHttpEntity;
import software.amazon.awssdk.http.HttpBody;

/**
 * The request body as an Apache entity, written once. Buffered bytes are copied out of the body's read-only buffer
 * in {@link #COPY_BUFFER_SIZE} chunks; a streaming body is read from one {@link
 * software.amazon.awssdk.http.StreamingBody#openStream()}, which this entity closes when the write ends or fails.
 * Framing comes from the request's {@code Content-Length}/{@code Transfer-Encoding} headers; when a length is framed,
 * a source that ends early or runs long fails the write rather than being truncated silently.
 */
final class RequestEntity extends AbstractHttpEntity {
    static final int COPY_BUFFER_SIZE = 8192;

    private final HttpBody body;
    private final long framedLength;

    /** @param framedLength the {@code Content-Length} on the wire, or -1 for chunked framing */
    RequestEntity(HttpBody body, long framedLength) {
        this.body = body;
        this.framedLength = framedLength;
        setChunked(framedLength < 0);
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public long getContentLength() {
        return framedLength;
    }

    @Override
    public InputStream getContent() {
        throw new UnsupportedOperationException("The request entity is write-only");
    }

    @Override
    public boolean isStreaming() {
        return body.kind() == HttpBody.Kind.STREAMING;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        byte[] chunk = new byte[COPY_BUFFER_SIZE];
        long written = 0;
        if (body.kind() == HttpBody.Kind.BYTES) {
            ByteBuffer data = body.asByteBuffer();
            while (data.hasRemaining()) {
                int n = Math.min(chunk.length, data.remaining());
                data.get(chunk, 0, n);
                out.write(chunk, 0, n);
                written += n;
            }
        } else if (body.kind() == HttpBody.Kind.STREAMING) {
            try (InputStream in = open()) {
                int n;
                while ((n = read(in, chunk)) >= 0) {
                    if (framedLength >= 0 && written + n > framedLength) {
                        throw new BodySourceException("The request body is longer than its Content-Length "
                                                      + framedLength, null);
                    }
                    out.write(chunk, 0, n);
                    written += n;
                }
            }
        }
        if (framedLength >= 0 && written != framedLength) {
            throw new BodySourceException("The request body ended after " + written + " of " + framedLength
                                          + " framed bytes", null);
        }
    }

    private InputStream open() throws IOException {
        try {
            return body.streamingBody().openStream();
        } catch (RuntimeException e) {
            throw new BodySourceException("The request body could not be opened", e);
        }
    }

    private static int read(InputStream in, byte[] chunk) throws IOException {
        try {
            return in.read(chunk, 0, chunk.length);
        } catch (RuntimeException e) {
            throw new BodySourceException("The request body source failed", e);
        }
    }

    /** A failure of the request body source rather than the connection. */
    static final class BodySourceException extends IOException {
        private static final long serialVersionUID = 1L;

        BodySourceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
