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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;

/**
 * Factories of {@link StreamingBody} values and the bounded-chunk adapter for foreign publishers.
 *
 * <p>Every body honors the {@link StreamingBody} contract: its publisher delivers chunks of at most
 * {@link #DEFAULT_MAX_CHUNK_SIZE} bytes (or the given bound) on demand and buffers at most one chunk; its stream and
 * publisher both enforce a known content length. A content length of {@code -1} means unknown; a content type of
 * {@code null} means unknown.
 */
public final class StreamingBodies {
    /**
     * The default, test-verified bound on one delivered chunk and on the data a body publisher buffers per
     * subscription (Requirement 12.6): 64 KiB.
     */
    public static final int DEFAULT_MAX_CHUNK_SIZE = 64 * 1024;

    private StreamingBodies() {
    }

    /** A replayable body of a copy of {@code bytes}. */
    public static StreamingBody fromBytes(byte[] bytes, String contentType) {
        byte[] copy = bytes.clone();
        return new DefaultStreamingBody(copy.length, contentType, true, DEFAULT_MAX_CHUNK_SIZE,
                                        () -> new ByteArrayInputStream(copy),
                                        new InputStreamPublisher(() -> new ByteArrayInputStream(copy),
                                                                 DEFAULT_MAX_CHUNK_SIZE, copy.length));
    }

    /**
     * A replayable body of the file at {@code file}, whose length is read now; each consumption reopens the file.
     *
     * @throws UncheckedIOException if the file's size cannot be read
     */
    public static StreamingBody fromFile(Path file, String contentType) {
        Objects.requireNonNull(file, "file");
        long length;
        try {
            length = Files.size(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        InputStreamPublisher.Source source = () -> Files.newInputStream(file);
        return new DefaultStreamingBody(length, contentType, true, DEFAULT_MAX_CHUNK_SIZE, source,
                                        new InputStreamPublisher(source, DEFAULT_MAX_CHUNK_SIZE, length));
    }

    /** A one-shot body of {@code stream}, which the body's consumer closes. */
    public static StreamingBody fromInputStream(InputStream stream, long contentLength, String contentType) {
        return fromInputStream(stream, contentLength, contentType, DEFAULT_MAX_CHUNK_SIZE);
    }

    /** A one-shot body of {@code stream} whose publisher delivers chunks of at most {@code maxChunkSize} bytes. */
    public static StreamingBody fromInputStream(InputStream stream, long contentLength, String contentType,
                                                int maxChunkSize) {
        Objects.requireNonNull(stream, "stream");
        requireLength(contentLength);
        requireChunkSize(maxChunkSize);
        InputStreamPublisher.Source source = () -> stream;
        return new DefaultStreamingBody(contentLength, contentType, false, maxChunkSize, source,
                                        new InputStreamPublisher(source, maxChunkSize, contentLength));
    }

    /**
     * A replayable body whose every consumption reads a fresh stream from {@code streams}; each stream must supply
     * the same bytes.
     */
    public static StreamingBody fromInputStreamSupplier(Supplier<? extends InputStream> streams, long contentLength,
                                                        String contentType) {
        Objects.requireNonNull(streams, "streams");
        requireLength(contentLength);
        InputStreamPublisher.Source source = streams::get;
        return new DefaultStreamingBody(contentLength, contentType, true, DEFAULT_MAX_CHUNK_SIZE, source,
                                        new InputStreamPublisher(source, DEFAULT_MAX_CHUNK_SIZE, contentLength));
    }

    /** {@link #of(long, String, boolean, Supplier, Publisher, int)} with the default chunk bound. */
    public static StreamingBody of(long contentLength, String contentType, boolean replayable,
                                   Supplier<? extends InputStream> streams,
                                   Publisher<? extends ByteBuffer> publisher) {
        return of(contentLength, contentType, replayable, streams, publisher, DEFAULT_MAX_CHUNK_SIZE);
    }

    /**
     * A body over a source that already has both native views, such as a Smithy Java {@code DataStream}: the sync
     * view reads {@code streams}, the async view re-chunks {@code publisher} through
     * {@link #boundedChunks(Publisher, int, long)}. The body adds content-length enforcement and, when not
     * {@code replayable}, single-consumption guarding across both views.
     */
    public static StreamingBody of(long contentLength, String contentType, boolean replayable,
                                   Supplier<? extends InputStream> streams,
                                   Publisher<? extends ByteBuffer> publisher, int maxChunkSize) {
        Objects.requireNonNull(streams, "streams");
        Objects.requireNonNull(publisher, "publisher");
        requireLength(contentLength);
        requireChunkSize(maxChunkSize);
        return new DefaultStreamingBody(contentLength, contentType, replayable, maxChunkSize, streams::get,
                                        new BoundedChunkPublisher(publisher, maxChunkSize, contentLength));
    }

    /**
     * Adapts {@code upstream} to the {@link StreamingBody} publisher contract: one upstream buffer requested at a
     * time and only under positive downstream demand, zero-copy read-only slices of at most {@code maxChunkSize}
     * bytes, empty buffers dropped, a known {@code contentLength} (else {@code -1}) enforced, and exactly one terminal
     * signal.
     */
    public static Publisher<ByteBuffer> boundedChunks(Publisher<? extends ByteBuffer> upstream,
                                                           int maxChunkSize, long contentLength) {
        Objects.requireNonNull(upstream, "upstream");
        requireLength(contentLength);
        requireChunkSize(maxChunkSize);
        return new BoundedChunkPublisher(upstream, maxChunkSize, contentLength);
    }

    /**
     * Reads a streaming body completely, for finite messages only (error responses and non-streaming outputs). The
     * body is consumed.
     *
     * @throws UncheckedIOException if reading fails, including on a content-length mismatch
     */
    static byte[] readAll(StreamingBody body) {
        try (InputStream in = body.openStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void requireLength(long contentLength) {
        if (contentLength < -1) {
            throw new IllegalArgumentException("A content length is -1 (unknown) or non-negative: " + contentLength);
        }
    }

    private static void requireChunkSize(int maxChunkSize) {
        if (maxChunkSize <= 0) {
            throw new IllegalArgumentException("A chunk size is positive: " + maxChunkSize);
        }
    }
}
