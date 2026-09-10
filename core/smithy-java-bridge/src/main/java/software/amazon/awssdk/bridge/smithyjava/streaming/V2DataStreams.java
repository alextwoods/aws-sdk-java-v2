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

package software.amazon.awssdk.bridge.smithyjava.streaming;

import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * Converts between v2's streaming body types and smithy-java's {@link DataStream}.
 *
 * <p>Neither direction copies. That is the whole point: an object body can be larger than the heap, so a
 * conversion that materializes it is not a conversion but a bug that only appears in production.
 */
@SdkProtectedApi
public final class V2DataStreams {

    private V2DataStreams() {
    }

    /**
     * Adapts a v2 {@link RequestBody} to a {@link DataStream}.
     *
     * <p>The result reports itself replayable, because every {@code RequestBody} factory v2 exposes is
     * backed by a {@link ContentStreamProvider}, whose contract is to return a fresh stream per call.
     * Replayability has to survive the conversion: it is what lets a retry re-send the body, and
     * {@link V2StreamingBridge} relies on it not being the signer's problem to discover.
     *
     * @param requestBody the v2 body; must not be null.
     * @return a non-buffering {@code DataStream} over the same bytes.
     */
    public static DataStream toDataStream(RequestBody requestBody) {
        return new RequestBodyDataStream(requestBody.contentStreamProvider(),
                                         requestBody.contentType(),
                                         requestBody.optionalContentLength().orElse(-1L));
    }

    /**
     * Exposes a {@link DataStream} to a v2 caller as an {@link AbortableInputStream}.
     *
     * <p>Used for streaming <em>responses</em>: v2's {@code ResponseTransformer} takes an
     * {@code AbortableInputStream} so a caller who stops reading early can release the connection instead
     * of draining the rest of the object. {@code DataStream} has no abort concept, so {@code abort()} maps
     * to {@code close()} — the strongest thing available. In the case that matters, the stream underneath
     * is v2's own abortable stream (the transport bridge hands it through untouched), so the close reaches
     * the real connection.
     *
     * @param body the response body.
     * @return the same bytes, as the type v2's response transformers accept.
     */
    public static AbortableInputStream toAbortableInputStream(DataStream body) {
        InputStream in = body.asInputStream();
        if (in instanceof AbortableInputStream abortable) {
            return abortable;
        }
        return AbortableInputStream.create(in, () -> {
            try {
                in.close();
            } catch (Exception e) {
                // An abort is a best-effort release of the connection. A caller who has stopped reading
                // has no use for a failure from the discard path, and throwing here would replace their
                // real exception with this one.
            }
        });
    }

    /**
     * A {@link DataStream} over a v2 {@link ContentStreamProvider}.
     *
     * <p>Written out rather than composed from {@code DataStream.ofInputStream}, which produces a one-shot
     * stream — correct for a response, wrong for a request that may be retried. The five abstract methods
     * below are the whole interface; {@code DataStream}'s defaults derive the rest from
     * {@link #asInputStream()}.
     */
    private static final class RequestBodyDataStream implements DataStream {

        private final ContentStreamProvider provider;
        private final String contentType;
        private final long contentLength;

        private RequestBodyDataStream(ContentStreamProvider provider, String contentType, long contentLength) {
            this.provider = provider;
            this.contentType = contentType;
            this.contentLength = contentLength;
        }

        @Override
        public long contentLength() {
            return contentLength;
        }

        @Override
        public String contentType() {
            return contentType;
        }

        @Override
        public boolean isReplayable() {
            return true;
        }

        @Override
        public boolean isAvailable() {
            // "Available" means the bytes are already in memory. They are not: this is a stream over a
            // file or socket, and claiming otherwise invites a caller to buffer it.
            return false;
        }

        @Override
        public InputStream asInputStream() {
            return provider.newStream();
        }
    }
}
