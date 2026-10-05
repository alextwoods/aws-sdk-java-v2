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

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.core.serde.LocatingDeserializer;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;

/**
 * How an async caller reads one operation's output event stream from a live response body: the output codec, the
 * message bound, and whether the output's other members arrive as the RPC {@code initial-response} event (the RPC
 * protocols) or are bound to the response headers (the HTTP binding protocols, whose
 * {@link software.amazon.awssdk.protocols.core.OperationProtocol#deserializeStreamingOutput} reads them without the
 * body).
 *
 * <p>Immutable and thread safe.
 */
public final class OutputEventStream {
    private final EventStreamCodec codec;
    private final int maxMessageSize;
    private final boolean initialResponse;

    OutputEventStream(EventStreamCodec codec, int maxMessageSize, boolean initialResponse) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.maxMessageSize = maxMessageSize;
        this.initialResponse = initialResponse;
    }

    /** The output event stream codec. */
    public EventStreamCodec codec() {
        return codec;
    }

    /**
     * True if the first message of the body is the {@code initial-response} event carrying the output's other
     * members; false if they are bound to the response headers.
     */
    public boolean initialResponse() {
        return initialResponse;
    }

    /**
     * The demand-driven publisher of the events of {@code body}. With an {@code initialResponse} consumer (required
     * if and only if {@link #initialResponse()}), the first message is read without downstream demand and handed to
     * it, not published; a body that ends before it fails the publisher.
     *
     * @param response the HTTP response, for error metadata
     */
    public EventStreamPublisher publisher(Publisher<ByteBuffer> body, SdkHttpResponse response,
                                          Consumer<InitialResponse> initialResponse) {
        if (this.initialResponse != (initialResponse != null)) {
            throw new IllegalArgumentException(this.initialResponse ? "An initial-response consumer is required"
                                                                    : "The output has no initial-response event");
        }
        Consumer<Message> initial = initialResponse == null
                                    ? null : message -> initialResponse.accept(new InitialResponse(message, response));
        return new EventStreamPublisher(body, codec, response, maxMessageSize, initial);
    }

    /** The RPC {@code initial-response} message of one response, as read from its event stream body. */
    public final class InitialResponse {
        private final Message message;
        private final SdkHttpResponse response;

        private InitialResponse(Message message, SdkHttpResponse response) {
            this.message = message;
            this.response = response;
        }

        /**
         * Reads the message into {@code builder} without the event stream member and builds the output.
         *
         * @throws software.amazon.awssdk.awscore.exception.AwsServiceException if the message is an exception or
         *         error event
         * @throws software.amazon.awssdk.protocols.core.SdkProtocolException if it is not the initial-response event
         */
        public <T extends SerializableStruct> T read(ShapeBuilder<T> builder) {
            codec.decodeInitialMessage(message, builder, null, response);
            return LocatingDeserializer.build(codec.wire(), "event stream message", builder);
        }
    }
}
