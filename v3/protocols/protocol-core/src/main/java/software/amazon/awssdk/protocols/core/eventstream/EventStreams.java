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

package software.amazon.awssdk.protocols.core.eventstream;

import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.eventstream.FrameTransformer;
import software.amazon.awssdk.eventstream.MessageDecoder;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.StreamingBody;
import software.amazon.awssdk.protocols.core.serde.LocatingDeserializer;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.core.serde.event.ProtocolEventStreamReader;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * Event stream request and response bodies of the protocol runtimes (task 4.3), over {@link EventStreamCodec}.
 *
 * <ul>
 *   <li>Input: {@link #writeRequest} sets an {@link EventStreamRequestBody} (one-shot, demand-read frames, signing
 *       hook {@link EventStreamRequestBody#withFrameTransformer}) with {@code Content-Type:
 *       application/vnd.amazon.eventstream}.</li>
 *   <li>Output, sync: {@link #reader} (HTTP binding protocols) and {@link #readInitialResponse} (RPC protocols, whose
 *       first message is {@code initial-response}) return the Smithy Java {@code EventStream} reader that generated
 *       types hold; it pulls one message at a time from the body ({@link #DEFAULT_MAX_MESSAGE_SIZE} bound).</li>
 *   <li>Output, async: {@link #publisher} is the demand-driven Reactive Streams {@code Publisher} of typed events.</li>
 * </ul>
 */
public final class EventStreams {
    /** The HTTP content type of an event stream body. */
    public static final String CONTENT_TYPE = "application/vnd.amazon.eventstream";
    /** The default bound on one received message. */
    public static final int DEFAULT_MAX_MESSAGE_SIZE = MessageDecoder.DEFAULT_MAX_MESSAGE_SIZE;

    private static final String HTTP_CONTENT_TYPE = "Content-Type";

    private EventStreams() {
    }

    /** The event stream value of {@code member} in {@code struct}, or {@code null} if unset. */
    public static EventStream<? extends SerializableStruct> eventStream(SerializableStruct struct, Schema member) {
        return struct.getMemberValue(member);
    }

    /**
     * Sets the event stream of {@code input}'s {@code member} as the body of {@code request}, if the member is set.
     *
     * @param initialRequest the {@code initial-request} event (the input, whose event stream member is skipped) for
     *        RPC protocols, or {@code null} for HTTP binding protocols
     */
    public static void writeRequest(SerializableStruct input, Schema member, EventStreamCodec codec,
                                    SerializableStruct initialRequest, SdkHttpRequest.Builder request) {
        EventStream<? extends SerializableStruct> events = eventStream(input, member);
        if (events == null) {
            return;
        }
        request.putHeader(HTTP_CONTENT_TYPE, CONTENT_TYPE);
        EventStreamCodec encoding = input instanceof InputEventView view ? codec.withEventView(view::viewInputEvent)
                                                                         : codec;
        request.body(HttpBody.streaming(requestBody(events, encoding, initialRequest)));
    }

    /**
     * An input struct (typically a request view, such as the Dynamic Client's) that also views each event written to
     * its input event stream before the event is encoded, so the event serializes as the input itself does.
     */
    public interface InputEventView {
        /** {@code event}, a value of the input event stream union, as it should be encoded. */
        SerializableStruct viewInputEvent(SerializableStruct event);
    }

    /** The request body of {@code events}, with the identity frame transform. */
    public static EventStreamRequestBody requestBody(EventStream<? extends SerializableStruct> events,
                                                     EventStreamCodec codec, SerializableStruct initialRequest) {
        return new EventStreamRequestBody(events, codec, initialRequest, FrameTransformer.identity(),
                                          new AtomicBoolean());
    }

    /** The response body of {@code response} as a streaming body (an empty or buffered body is wrapped). */
    public static StreamingBody body(SdkHttpResponse response) {
        HttpBody body = response.body();
        return switch (body.kind()) {
            case STREAMING -> body.streamingBody();
            case EMPTY -> StreamingBodies.fromBytes(new byte[0], CONTENT_TYPE);
            case BYTES -> StreamingBodies.fromBytes(body.asByteArray(), CONTENT_TYPE);
        };
    }

    /** The sync reader of the event stream body of {@code response} (no initial-response message). */
    public static EventStream<? extends SerializableStruct> reader(SdkHttpResponse response, EventStreamCodec codec,
                                                                   int maxMessageSize) {
        MessageFrames.MessageBoundaryInputStream input = input(response, maxMessageSize);
        return ProtocolEventStreamReader.newReader(
            DataStream.ofInputStream(input, CONTENT_TYPE),
            MessageFrames.decoders(codec, response, maxMessageSize, input, e -> {
                throw new IllegalStateException("No initial event is expected");
            }), false);
    }

    /**
     * Reads the RPC {@code initial-response} message of {@code response} into {@code builder} with the rest of the
     * body as {@code streamMember}'s event stream, and builds the output. Blocks until the first message arrives.
     */
    public static <T extends SerializableStruct> T readInitialResponse(SdkHttpResponse response,
                                                                       EventStreamCodec codec,
                                                                       ShapeBuilder<T> builder, int maxMessageSize) {
        Objects.requireNonNull(builder, "builder");
        WireContext wire = codec.wire();
        MessageFrames.MessageBoundaryInputStream input = input(response, maxMessageSize);
        ProtocolEventStreamReader<SerializableStruct, SerializableStruct> reader = ProtocolEventStreamReader.newReader(
            DataStream.ofInputStream(input, CONTENT_TYPE),
            MessageFrames.decoders(codec, response, maxMessageSize, input, initial -> {
                codec.decodeInitialMessage(initial.message(), builder, initial.stream(), response);
                return LocatingDeserializer.build(wire, "event stream message", builder);
            }), true);
        @SuppressWarnings("unchecked")
        T output = (T) reader.readInitialEvent();
        return output;
    }

    /** The demand-driven async publisher of the typed events of {@code body}. */
    public static EventStreamPublisher publisher(StreamingBody body, EventStreamCodec codec,
                                                 SdkHttpResponse response, int maxMessageSize) {
        return new EventStreamPublisher(body, codec, response, maxMessageSize);
    }

    private static MessageFrames.MessageBoundaryInputStream input(SdkHttpResponse response, int maxMessageSize) {
        if (maxMessageSize < 16 || maxMessageSize > DEFAULT_MAX_MESSAGE_SIZE) {
            throw new IllegalArgumentException("maxMessageSize must be within 16.." + DEFAULT_MAX_MESSAGE_SIZE
                                               + ", not " + maxMessageSize);
        }
        InputStream source = body(response).openStream();
        return new MessageFrames.MessageBoundaryInputStream(source, maxMessageSize);
    }
}
