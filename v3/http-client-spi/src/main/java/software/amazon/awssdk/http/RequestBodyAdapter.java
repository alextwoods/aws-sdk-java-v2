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

import java.util.Objects;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.ContentStreamProvider;
import software.amazon.awssdk.core.sync.RequestBody;

/**
 * Adapts the caller-facing request bodies of {@code sdk-core} ({@link RequestBody}, {@link AsyncRequestBody}) to the
 * transport-facing {@link StreamingBody} and {@link HttpBody}. It lives here because {@code http-spi} depends on
 * {@code sdk-core}, so only this side can see both types.
 *
 * <p>Each adapted body keeps the source's length, media type and replayability, and gains the full
 * {@link StreamingBody} contract: chunks of at most {@link StreamingBodies#DEFAULT_MAX_CHUNK_SIZE} bytes, exact
 * demand, a known length enforced on both views, and single-consumption guarding for one-shot sources.
 * <ul>
 *   <li>A {@link RequestBody} keeps its {@linkplain RequestBody#replayable() replayability}; its sync view reads a new
 *       stream from its {@link ContentStreamProvider}, and its async view reads that stream on demand.</li>
 *   <li>An {@link AsyncRequestBody} keeps its {@linkplain AsyncRequestBody#isReplayable() replayability}: its async
 *       view is the body's publisher re-chunked, subscribed to anew by every consumption, and its sync view blocks
 *       on a new subscription one chunk at a time. A replayable body can therefore be read before transmission (for a
 *       payload hash or header checksum) and sent again on a retry; a one-shot body is consumed at most once.</li>
 * </ul>
 */
public final class RequestBodyAdapter {
    private RequestBodyAdapter() {
    }

    /** {@code body} as a {@link StreamingBody}. */
    public static StreamingBody toStreamingBody(RequestBody body) {
        Objects.requireNonNull(body, "body");
        long length = body.optionalContentLength().orElse(-1L);
        ContentStreamProvider provider = body.contentStreamProvider();
        InputStreamPublisher.Source source = provider::newStream;
        return new DefaultStreamingBody(length, body.contentType(), body.replayable(),
                                        StreamingBodies.DEFAULT_MAX_CHUNK_SIZE, source,
                                        new InputStreamPublisher(source, StreamingBodies.DEFAULT_MAX_CHUNK_SIZE,
                                                                 length));
    }

    /** {@code body} as a {@link StreamingBody}, replayable if {@code body} is. */
    public static StreamingBody toStreamingBody(AsyncRequestBody body) {
        Objects.requireNonNull(body, "body");
        long length = body.contentLength().orElse(-1L);
        if (length < -1) {
            throw new IllegalArgumentException("A content length is non-negative: " + length);
        }
        return new DefaultStreamingBody(length, body.contentType(), body.isReplayable(),
                                        StreamingBodies.DEFAULT_MAX_CHUNK_SIZE,
                                        () -> new PublisherInputStream(body),
                                        new BoundedChunkPublisher(body, StreamingBodies.DEFAULT_MAX_CHUNK_SIZE,
                                                                  length));
    }

    /** {@code body} as a {@link HttpBody.Kind#STREAMING} {@link HttpBody}. */
    public static HttpBody toHttpBody(RequestBody body) {
        return HttpBody.streaming(toStreamingBody(body));
    }

    /** {@code body} as a {@link HttpBody.Kind#STREAMING} {@link HttpBody}, replayable if {@code body} is. */
    public static HttpBody toHttpBody(AsyncRequestBody body) {
        return HttpBody.streaming(toStreamingBody(body));
    }
}
