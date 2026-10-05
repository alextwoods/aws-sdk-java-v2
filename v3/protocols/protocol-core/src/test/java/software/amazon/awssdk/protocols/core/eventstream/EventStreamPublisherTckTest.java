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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.eventstream.HeaderValue;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.eventstream.MessageEncoder;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Reactive Streams TCK (TestNG) for {@link EventStreamPublisher}: {@code createPublisher(n)} decodes a body of
 * exactly {@code n} event frames. The publisher accepts one subscriber, so the optional multi-subscribe tests are skipped.
 */
public final class EventStreamPublisherTckTest extends PublisherVerification<SerializableStruct> {
    private static final EventStreamCodec CODEC = codec();

    public EventStreamPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<SerializableStruct> createPublisher(long elements) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (long i = 0; i < elements; i++) {
            body.writeBytes(frame(new byte[] {(byte) i}));
        }
        return EventStreams.publisher(StreamingBodies.fromBytes(body.toByteArray(), null), CODEC, null, 1024);
    }

    /** Decoding failures surface only under demand (the body is read on request), so eager-error tests do not apply. */
    @Override
    public Publisher<SerializableStruct> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }

    private static byte[] frame(byte[] payload) {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(EventStreamCodec.MESSAGE_TYPE, HeaderValue.ofString("event"));
        headers.put(EventStreamCodec.EVENT_TYPE, HeaderValue.ofString("data"));
        headers.put(EventStreamCodec.CONTENT_TYPE, HeaderValue.ofString("application/octet-stream"));
        ByteBuffer encoded = MessageEncoder.encode(new Message(headers, payload));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        return bytes;
    }

    private static EventStreamCodec codec() {
        Model model = Model.assembler().addUnparsedModel("events.smithy", String.join("\n",
            "$version: \"2\"",
            "namespace test.flow",
            "@streaming",
            "union Events { data: DataEvent }",
            "structure DataEvent {",
            "    @eventPayload",
            "    payload: Blob",
            "}")).assemble().unwrap();
        return EventStreamCodec.of(new WireContext(ShapeId.from("aws.protocols#restJson1"),
                                                   ShapeId.from("test.flow#SubscribeToEvents")),
                                   new SchemaConverter(model).getSchema(
                                       model.expectShape(ShapeId.from("test.flow#Events"))),
                                   EventPayloadCodec.of(JsonCodec.builder().build(), "application/json"), null);
    }
}
