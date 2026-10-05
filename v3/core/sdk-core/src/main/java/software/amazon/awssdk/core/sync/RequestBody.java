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

package software.amazon.awssdk.core.sync;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The body of a streaming-input operation of a sync client, source compatible with the V2 {@code RequestBody}
 * factories ({@code fromBytes}, {@code fromString}, {@code fromByteBuffer}, {@code fromFile}, {@code fromInputStream},
 * {@code fromContentProvider}, {@code empty}).
 *
 * <p>A body is {@link #replayable()} when its {@link #contentStreamProvider()} can supply the bytes again from the
 * start, which is what makes a retry after transmission safe. Bytes, strings, buffers and files are replayable and
 * copied or referenced at creation; {@link #fromInputStream} is one-shot. The SDK closes each stream it opens, but a
 * caller-supplied {@link InputStream} stays caller-owned: closing the SDK's view does not close it.
 */
public final class RequestBody {
    private static final String OCTET_STREAM = "application/octet-stream";
    private static final String TEXT_PLAIN = "text/plain";

    private final ContentStreamProvider provider;
    private final Long contentLength;
    private final String contentType;
    private final boolean replayable;

    private RequestBody(ContentStreamProvider provider, Long contentLength, String contentType, boolean replayable) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.contentType = Objects.requireNonNull(contentType, "contentType");
        if (contentLength != null && contentLength < 0) {
            throw new IllegalArgumentException("contentLength must not be negative, but was " + contentLength + ".");
        }
        this.contentLength = contentLength;
        this.replayable = replayable;
    }

    /** An empty, replayable body. */
    public static RequestBody empty() {
        return fromBytesUnsafe(new byte[0], OCTET_STREAM);
    }

    /** A replayable body of a copy of {@code bytes}. */
    public static RequestBody fromBytes(byte[] bytes) {
        return fromBytesUnsafe(bytes.clone(), OCTET_STREAM);
    }

    /** A replayable body of a copy of the remaining bytes of {@code buffer}; the buffer's position is unchanged. */
    public static RequestBody fromByteBuffer(ByteBuffer buffer) {
        ByteBuffer view = buffer.duplicate();
        byte[] bytes = new byte[view.remaining()];
        view.get(bytes);
        return fromBytesUnsafe(bytes, OCTET_STREAM);
    }

    /** A replayable body of {@code text} encoded as UTF-8. */
    public static RequestBody fromString(String text) {
        return fromString(text, StandardCharsets.UTF_8);
    }

    /** A replayable body of {@code text} encoded with {@code charset}. */
    public static RequestBody fromString(String text, Charset charset) {
        return fromBytesUnsafe(text.getBytes(charset), TEXT_PLAIN + "; charset=" + charset.name());
    }

    /**
     * A replayable body of the file at {@code path}; its length is read now, and each stream reopens the file.
     *
     * @throws UncheckedIOException if the file's size cannot be read
     */
    public static RequestBody fromFile(Path path) {
        Objects.requireNonNull(path, "path");
        long length;
        try {
            length = Files.size(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the size of " + path, e);
        }
        ContentStreamProvider provider = new ContentStreamProvider() {
            @Override
            public InputStream newStream() {
                try {
                    return Files.newInputStream(path);
                } catch (IOException e) {
                    throw new UncheckedIOException("Cannot open " + path, e);
                }
            }

            @Override
            public String name() {
                return "File";
            }
        };
        return new RequestBody(provider, length, OCTET_STREAM, true);
    }

    /**
     * A one-shot body of the next {@code contentLength} bytes of the caller-owned {@code inputStream}. The SDK never
     * closes {@code inputStream}, and a second {@link ContentStreamProvider#newStream()} throws
     * {@link IllegalStateException}.
     */
    public static RequestBody fromInputStream(InputStream inputStream, long contentLength) {
        Objects.requireNonNull(inputStream, "inputStream");
        AtomicBoolean opened = new AtomicBoolean();
        ContentStreamProvider provider = new ContentStreamProvider() {
            @Override
            public InputStream newStream() {
                if (!opened.compareAndSet(false, true)) {
                    throw new IllegalStateException("A body from an InputStream can be read only once.");
                }
                return new FilterInputStream(inputStream) {
                    @Override
                    public void close() {
                        // Caller-owned: the SDK does not close the caller's stream.
                    }
                };
            }

            @Override
            public String name() {
                return "InputStream";
            }
        };
        return new RequestBody(provider, contentLength, OCTET_STREAM, false);
    }

    /** A replayable body of {@code contentLength} bytes of type {@code mimeType} supplied by {@code provider}. */
    public static RequestBody fromContentProvider(ContentStreamProvider provider, long contentLength,
                                                  String mimeType) {
        return new RequestBody(provider, contentLength, mimeType, true);
    }

    /** A replayable body of unknown length of type {@code mimeType} supplied by {@code provider}. */
    public static RequestBody fromContentProvider(ContentStreamProvider provider, String mimeType) {
        return new RequestBody(provider, null, mimeType, true);
    }

    private static RequestBody fromBytesUnsafe(byte[] bytes, String contentType) {
        ContentStreamProvider provider = new ContentStreamProvider() {
            @Override
            public InputStream newStream() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public String name() {
                return "Bytes";
            }
        };
        return new RequestBody(provider, (long) bytes.length, contentType, true);
    }

    /** The supplier of the body's bytes. */
    public ContentStreamProvider contentStreamProvider() {
        return provider;
    }

    /** The body's length in bytes, if known. */
    public Optional<Long> optionalContentLength() {
        return Optional.ofNullable(contentLength);
    }

    /** The body's media type. */
    public String contentType() {
        return contentType;
    }

    /** Whether {@link #contentStreamProvider()} can supply the bytes again from the start. */
    public boolean replayable() {
        return replayable;
    }

    @Override
    public String toString() {
        return "RequestBody(" + provider.name() + ", contentLength=" + contentLength + ", contentType=" + contentType
               + ", replayable=" + replayable + ")";
    }
}
