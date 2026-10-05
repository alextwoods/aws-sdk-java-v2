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

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * The data of a streaming HTTP body ({@link HttpBody.Kind#STREAMING}), readable through one of two native views:
 * a blocking {@link #openStream() InputStream} for sync transports, or a demand-driven {@link Publisher} of
 * read-only {@link ByteBuffer} chunks for async transports. A sync consumer never routes through the publisher and an
 * async consumer never blocks on the stream (design, "HTTP messages and bodies").
 *
 * <p>Publisher contract (Requirements 5.11, 8.4, 8.5, 12.5), which every implementation must honor:
 * <ul>
 *   <li>no {@code onNext} without outstanding demand, and never more chunks than the cumulative demand;</li>
 *   <li>every chunk is a read-only buffer with at most {@link #maxChunkSize()} remaining bytes and at least one;
 *       empty upstream chunks are never delivered;</li>
 *   <li>at most one source chunk is buffered per subscription, so buffered data stays within
 *       {@link #maxChunkSize()} (or the size of one upstream chunk for adapted publishers);</li>
 *   <li>exactly one terminal signal ({@code onComplete} or {@code onError}), after which nothing is signalled;</li>
 *   <li>{@code cancel} stops delivery and releases the source; a non-positive {@code request} is an
 *       {@link IllegalArgumentException} error (Reactive Streams rule 3.9);</li>
 *   <li>a known {@link #contentLength()} is enforced: a source that ends early or runs long fails with
 *       {@link ContentLengthMismatchException} (via {@code onError}, or thrown from the stream's {@code read}).</li>
 * </ul>
 *
 * <p>Replay metadata: a {@linkplain #isReplayable() replayable} body can be consumed any number of times, each
 * {@link #openStream()} or subscription starting again from the first byte, so a retry can retransmit it. A one-shot
 * body can be consumed once, through either view; a second {@link #openStream()} throws
 * {@link IllegalStateException} and a second subscriber receives {@code onError(IllegalStateException)}, so a consumed
 * non-replayable body is never partially retransmitted (design state invariant 8).
 *
 * <p>Transports consume it through {@link SdkSyncHttpClient} and {@link SdkAsyncHttpClient}, whose ownership rules
 * say who closes each stream and subscription. The {@link StreamingBodies} publishers read their source on the thread
 * that calls {@code request}; the pipeline hands blocking sources to its executor before an async transport requests
 * data. The caller-facing
 * {@code RequestBody} and Reactive Streams {@code AsyncRequestBody} of sdk-core adapt to this type (itself a
 * Reactive Streams {@link Publisher}) through {@link RequestBodyAdapter}; sources that expose a
 * {@code java.util.concurrent.Flow.Publisher} (Smithy Java {@code DataStream}) are bridged with
 * {@code org.reactivestreams.FlowAdapters} where they are adapted.
 *
 * <p>Create bodies with {@link StreamingBodies}; transports may implement this interface directly.
 */
public interface StreamingBody extends Publisher<ByteBuffer> {
    /** The length in bytes, if known. */
    OptionalLong contentLength();

    /** The MIME type of the data, if known. */
    Optional<String> contentType();

    /** True if the body can be consumed again from the first byte (for example by a retry). */
    boolean isReplayable();

    /** True if the body can still be consumed: always for a replayable body, else until its first consumption. */
    boolean isAvailable();

    /** The largest chunk, in bytes, that {@link #subscribe} delivers. */
    int maxChunkSize();

    /**
     * A blocking stream of the data, from the first byte. The caller owns and must close it.
     *
     * @throws IllegalStateException if the body is one-shot and already consumed
     * @throws java.io.UncheckedIOException if the source cannot be opened
     */
    InputStream openStream();

    /**
     * Subscribes to the data, from the first byte, under the demand contract of this interface.
     */
    @Override
    void subscribe(Subscriber<? super ByteBuffer> subscriber);
}
