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

package software.amazon.awssdk.protocols.core.serde;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Flow;
import org.reactivestreams.FlowAdapters;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.StreamingBody;
import software.amazon.awssdk.protocols.core.binding.HttpMemberBinding;
import software.amazon.awssdk.protocols.core.binding.HttpMessageBindings;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.SpecificShapeSerializer;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.RequiresLengthTrait;

/**
 * Streaming blob {@code httpPayload} members of the HTTP binding protocols ({@code restJson1}, {@code restXml}),
 * adapted between Smithy Java {@link DataStream}, the value generated types hold, and the HTTP SPI
 * {@link StreamingBody}. Nothing here reads or buffers stream data: the request body is the member's stream, and the
 * response member is a stream over the response body, each keeping its content length, content type and
 * replayability, and each view (blocking stream, demand-driven publisher) mapped to the same view on the other side.
 *
 * <p>Smithy Java boundary: {@link DataStream} is a {@code java.util.concurrent.Flow.Publisher}, while the V3 streaming
 * surface is Reactive Streams. The two async views are bridged here, and only here, with {@link FlowAdapters}, which
 * forwards every signal (demand, cancellation, terminal signals) without buffering.
 */
public final class StreamingPayloads {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String OCTET_STREAM = "application/octet-stream";
    private static final TraitKey<RequiresLengthTrait> REQUIRES_LENGTH = TraitKey.get(RequiresLengthTrait.class);

    private StreamingPayloads() {
    }

    /** True if {@code bindings} bind a streaming blob payload. */
    public static boolean isStreamingBlob(HttpMessageBindings bindings) {
        return bindings.payloadKind() == HttpMessageBindings.PayloadKind.STREAMING_BLOB;
    }

    /**
     * Sets the streaming blob payload of {@code input}, if present, as the body of {@code request}, with a
     * {@code Content-Type} unless one is already bound: the stream's content type, else the member's
     * {@code mediaType}, else {@code application/octet-stream}. An absent member leaves the request without a body.
     *
     * @param bindings the operation's request bindings, whose payload kind is
     *        {@link HttpMessageBindings.PayloadKind#STREAMING_BLOB}
     * @param writer the operation's request binding writer
     * @throws IllegalArgumentException if the member targets a {@code requiresLength} blob and the stream's length
     *         is unknown
     */
    public static void writeRequest(HttpMessageBindings bindings, HttpBindingWriter writer, SerializableStruct input,
                                    SdkHttpRequest.Builder request) {
        if (!isStreamingBlob(bindings)) {
            throw new IllegalArgumentException("The request of " + bindings.structure().id()
                                               + " has no streaming blob payload");
        }
        HttpMemberBinding payload = bindings.payload().orElseThrow();
        Capture capture = new Capture();
        writer.payloadMember(input).serializeMembers(capture);
        DataStream stream = capture.stream;
        if (stream == null) {
            return;
        }
        Schema member = payload.member();
        if (!stream.hasKnownLength() && member.hasTrait(REQUIRES_LENGTH)) {
            throw new IllegalArgumentException("Member " + payload.memberName() + " of "
                                               + bindings.structure().id()
                                               + " requires a stream of known length (requiresLength)");
        }
        if (!request.hasHeader(CONTENT_TYPE)) {
            request.addHeader(CONTENT_TYPE, stream.contentType() != null ? stream.contentType()
                                                                          : modeledContentType(member));
        }
        request.body(HttpBody.streaming(toBody(stream)));
    }

    /**
     * The streaming blob payload of {@code response}: a {@link DataStream} over the response body (an empty stream
     * for an empty body), with the body's content type or else the {@code Content-Type} header.
     */
    public static DataStream readResponse(SdkHttpResponse response) {
        HttpBody body = response.body();
        String contentType = response.headers().firstValue(CONTENT_TYPE).orElse(null);
        return switch (body.kind()) {
            case EMPTY -> DataStream.ofEmpty();
            case BYTES -> DataStream.ofByteBuffer(body.asByteBuffer(), contentType);
            case STREAMING -> toDataStream(body.streamingBody(), contentType);
        };
    }

    /** {@code stream} as an HTTP SPI body, each view delegating to the same view of {@code stream}. */
    public static StreamingBody toBody(DataStream stream) {
        Objects.requireNonNull(stream, "stream");
        return StreamingBodies.of(stream.hasKnownLength() ? stream.contentLength() : -1, stream.contentType(),
                                  stream.isReplayable(), stream::asInputStream, FlowAdapters.toPublisher(stream));
    }

    /**
     * {@code body} as a {@link DataStream}, each view delegating to the same view of {@code body}.
     *
     * @param contentType the content type to report if {@code body} has none; may be {@code null}
     */
    public static DataStream toDataStream(StreamingBody body, String contentType) {
        return new BodyDataStream(Objects.requireNonNull(body, "body"),
                                  body.contentType().orElse(contentType));
    }

    /**
     * Why {@code protocol} cannot serve the streaming members of {@code operation}, or {@code null} if it can, for a
     * protocol without event streams (the query protocols). See {@link #streamingRejection(OperationSchemas, ShapeId,
     * boolean, boolean)}.
     */
    public static String streamingRejection(OperationSchemas operation, ShapeId protocol, boolean streamingBlobs) {
        return streamingRejection(operation, protocol, streamingBlobs, false);
    }

    /**
     * Why {@code protocol} cannot serve the streaming members of {@code operation}, or {@code null} if it can: a
     * streaming blob member is served only by protocols with an HTTP payload binding, an event stream member only by
     * protocols with event stream support ({@code restJson1}, {@code restXml}, {@code awsJson1_0/1_1},
     * {@code rpcv2Cbor}; not the query protocols).
     *
     * @param streamingBlobs true if the protocol binds streaming blob payloads ({@code restJson1}, {@code restXml})
     * @param eventStreams true if the protocol supports event streams
     */
    public static String streamingRejection(OperationSchemas operation, ShapeId protocol, boolean streamingBlobs,
                                            boolean eventStreams) {
        for (Optional<Schema> member : List.of(operation.inputStreamMember(), operation.outputStreamMember())) {
            if (member.isEmpty()) {
                continue;
            }
            Schema stream = member.get();
            if (stream.type() == ShapeType.BLOB) {
                if (!streamingBlobs) {
                    return "Operation " + operation.id() + " has the streaming blob member " + stream.memberName()
                           + ", but " + protocol + " has no HTTP payload binding and does not support streaming "
                           + "blobs";
                }
            } else if (!eventStreams) {
                return "Operation " + operation.id() + " has the event stream member " + stream.memberName()
                       + ", but " + protocol + " does not support event streams";
            }
        }
        return null;
    }

    private static String modeledContentType(Schema member) {
        MediaTypeTrait mediaType = member.getTrait(TraitKey.MEDIA_TYPE_TRAIT);
        return mediaType != null ? mediaType.getValue() : OCTET_STREAM;
    }

    /** Captures the payload member's stream. */
    private static final class Capture extends SpecificShapeSerializer {
        private DataStream stream;

        @Override
        public void writeDataStream(Schema schema, DataStream value) {
            stream = value;
        }

        @Override
        public void writeNull(Schema schema) {
        }
    }

    /** A {@link DataStream} view of a {@link StreamingBody}. */
    private static final class BodyDataStream implements DataStream {
        private final StreamingBody body;
        private final String contentType;

        BodyDataStream(StreamingBody body, String contentType) {
            this.body = body;
            this.contentType = contentType;
        }

        @Override
        public long contentLength() {
            return body.contentLength().orElse(-1);
        }

        @Override
        public String contentType() {
            return contentType;
        }

        @Override
        public boolean isReplayable() {
            return body.isReplayable();
        }

        @Override
        public boolean isAvailable() {
            return body.isAvailable();
        }

        @Override
        public InputStream asInputStream() {
            return body.openStream();
        }

        /** Smithy Java boundary: DataStream's Flow view over the body's Reactive Streams view. */
        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            body.subscribe(FlowAdapters.toSubscriber(Objects.requireNonNull(subscriber, "subscriber")));
        }

        @Override
        public String toString() {
            return "DataStream[" + body + "]";
        }
    }
}
