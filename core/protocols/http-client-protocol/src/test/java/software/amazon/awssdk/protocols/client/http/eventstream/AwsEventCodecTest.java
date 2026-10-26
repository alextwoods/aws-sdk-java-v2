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

package software.amazon.awssdk.protocols.client.http.eventstream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.BLOB_EVENT;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.BODY_AND_HEADER_EVENT;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.HEADERS_ONLY_EVENT;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.INPUT;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.OPERATION;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.OUTPUT;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.STRING_EVENT;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.STRUCTURE;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.STRUCTURE_EVENT;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.struct;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.NotEmpty;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.client.handler.AwsClientHandlerUtils;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.client.http.eventstream.AwsEventEncoderFactory.AwsEventEncoder;
import software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.MyError;
import software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.Struct;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.event.EventDecoder;
import software.amazon.awssdk.schema.serde.event.EventStreamingException;
import software.amazon.awssdk.schema.serde.event.EventStreamingProtocolException;
import software.amazon.awssdk.schema.serde.event.FrameDecoder;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;

class AwsEventCodecTest {
    private static final Codec CODEC = JsonCodec.builder().build();
    private static final String JSON = "application/json";

    private static AwsEventEncoder encoder(boolean emitEmptyPayload) {
        return AwsEventEncoderFactory.forInputStream(OPERATION, CODEC, JSON, emitEmptyPayload,
                                                     e -> new EventStreamingException("InternalError", e.getMessage()))
                                     .newEventEncoder();
    }

    private static AwsEventDecoderFactory decoderFactory() {
        return AwsEventDecoderFactory.forOutputStream(OPERATION, CODEC);
    }

    private static Struct bodyAndHeader() {
        return struct(BODY_AND_HEADER_EVENT, "stringHeader", "s", "intHeader", 7, "longHeader", 9L,
                      "booleanHeader", true, "timestampHeader", Instant.ofEpochMilli(1_700_000_000_123L),
                      "message", "hello", "count", 3);
    }

    @Test
    void encodeEvent_writesHeaderMembersAsTypedHeadersAndOtherMembersThroughTheCodec() {
        Message message = encoder(false).encodeEvent("secondBodyAndHeader", bodyAndHeader()).unwrap();

        Map<String, HeaderValue> headers = message.getHeaders();
        assertThat(headers.get(":message-type").getString()).isEqualTo("event");
        assertThat(headers.get(":event-type").getString()).isEqualTo("secondBodyAndHeader");
        assertThat(headers.get(":content-type").getString()).isEqualTo(JSON);
        assertThat(headers.get("stringHeader").getString()).isEqualTo("s");
        assertThat(headers.get("intHeader")).isEqualTo(HeaderValue.fromInteger(7));
        assertThat(headers.get("intHeader").getInteger()).isEqualTo(7);
        assertThat(headers.get("longHeader").getLong()).isEqualTo(9L);
        assertThat(headers.get("booleanHeader").getBoolean()).isTrue();
        assertThat(headers.get("timestampHeader").getTimestamp()).isEqualTo(Instant.ofEpochMilli(1_700_000_000_123L));
        assertThat(new String(message.getPayload(), StandardCharsets.UTF_8))
            .isEqualTo("{\"message\":\"hello\",\"count\":3}");
    }

    @Test
    void encode_eventWhoseShapeOneMemberTargets_usesThatMember() {
        Message message = encoder(false).encode(struct(HEADERS_ONLY_EVENT, "sequence", 5L)).unwrap();

        assertThat(message.getHeaders().get(":event-type").getString()).isEqualTo("headersOnly");
        assertThat(message.getHeaders()).doesNotContainKey(":content-type");
        assertThat(message.getPayload()).isEmpty();
    }

    @Test
    void encode_withEmitEmptyPayload_writesAnEmptyStructure() {
        Message message = encoder(true).encode(struct(HEADERS_ONLY_EVENT, "sequence", 5L)).unwrap();

        assertThat(message.getHeaders().get(":content-type").getString()).isEqualTo(JSON);
        assertThat(new String(message.getPayload(), StandardCharsets.UTF_8)).isEqualTo("{}");
    }

    /**
     * Port of smithy-java {@code testEncodeHeadersOnlyMemberWithEmitEmptyPayload} ({@code 8e892b96c}): the empty body
     * and content type are added, and the header members are still written.
     */
    @Test
    void encode_headersOnlyEventWithEmitEmptyPayload_keepsTheHeaders() {
        Message message = encoder(true).encode(struct(HEADERS_ONLY_EVENT, "sequence", 5L)).unwrap();

        assertThat(message.getHeaders()).isEqualTo(HeadersBuilder.forEvent().contentType(JSON).eventType("headersOnly")
                                                                 .put("sequence", 5L).build());
        assertThat(new String(message.getPayload(), StandardCharsets.UTF_8)).isEqualTo("{}");
    }

    /**
     * An event of another shape than the member's target: the member's target decides how the body is written, the
     * event's own members' traits which members are written, as before the member partition was precomputed.
     */
    @Test
    void encodeEvent_eventOfAnotherShape_isFilteredByItsOwnMembers() {
        Message message = encoder(false).encodeEvent("blob", struct(STRING_EVENT, "text", "plain")).unwrap();

        assertThat(message.getHeaders()).isEqualTo(HeadersBuilder.forEvent().contentType("text/plain")
                                                                 .eventType("blob").build());
        assertThat(message.getPayload()).isEqualTo("plain".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void encode_eventWhoseShapeSeveralMembersTarget_fails() {
        assertThatThrownBy(() -> encoder(false).encode(bodyAndHeader()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("encodeEvent");
        assertThatThrownBy(() -> encoder(false).encodeEvent("myError", bodyAndHeader()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void encode_eventPayloadMembers_writeRawBlobsAndStringsAndCodecStructures() {
        Message blob = encoder(false).encode(struct(BLOB_EVENT, "id", "i", "data", new byte[] {1, 2, 3})).unwrap();
        assertThat(blob.getHeaders().get(":content-type").getString()).isEqualTo("application/octet-stream");
        assertThat(blob.getPayload()).containsExactly(1, 2, 3);

        Message string = encoder(false).encode(struct(STRING_EVENT, "text", "plain")).unwrap();
        assertThat(string.getHeaders().get(":content-type").getString()).isEqualTo("text/plain");
        assertThat(string.getPayload()).isEqualTo("plain".getBytes(StandardCharsets.UTF_8));

        Message structure = encoder(false).encode(struct(STRUCTURE_EVENT, "nested", struct(STRUCTURE, "value", "v")))
                                          .unwrap();
        assertThat(structure.getHeaders().get(":content-type").getString()).isEqualTo(JSON);
        assertThat(new String(structure.getPayload(), StandardCharsets.UTF_8)).isEqualTo("{\"value\":\"v\"}");
    }

    @Test
    void encode_eventWithOnlyStringHeaders_isByteIdenticalToTheLegacyEncoding() {
        Struct event = struct(BLOB_EVENT, "id", "abc", "data", new byte[] {4, 5});
        ByteBuffer encoded = AwsFrameCodec.ENCODER.encode(encoder(false).encode(event));

        SdkHttpFullRequest legacyMarshalled = SdkHttpFullRequest.builder()
                                                                .method(SdkHttpMethod.POST)
                                                                .protocol("https")
                                                                .host("localhost")
                                                                .putHeader(":message-type", "event")
                                                                .putHeader(":event-type", "blob")
                                                                .putHeader(":content-type", "application/octet-stream")
                                                                .putHeader("id", "abc")
                                                                .contentStreamProvider(() -> new ByteArrayInputStream(new byte[] {4, 5}))
                                                                .build();
        assertThat(encoded).isEqualTo(AwsClientHandlerUtils.encodeEventStreamRequestToByteBuffer(legacyMarshalled));
    }

    @Test
    void decode_framesSplitAcrossBuffers_roundTripsEventsInOrder() {
        AwsEventEncoder encoder = encoder(false);
        List<Message> sent = new ArrayList<>();
        sent.add(encoder.encodeEvent("bodyAndHeader", bodyAndHeader()).unwrap());
        sent.add(encoder.encode(struct(HEADERS_ONLY_EVENT, "sequence", 11L)).unwrap());
        sent.add(encoder.encode(struct(STRING_EVENT, "text", "t")).unwrap());
        sent.add(encoder.encode(struct(STRUCTURE_EVENT, "nested", struct(STRUCTURE, "value", "v"))).unwrap());

        List<Object> events = decodeAll(sent, 7);

        assertThat(events).containsExactly(bodyAndHeader(), struct(HEADERS_ONLY_EVENT, "sequence", 11L),
                                           struct(STRING_EVENT, "text", "t"),
                                           struct(STRUCTURE_EVENT, "nested", struct(STRUCTURE, "value", "v")));
    }

    @Test
    void decode_blobPayloadAndStringHeaderFromALegacyEncodedFrame() {
        SdkHttpFullRequest legacyMarshalled = SdkHttpFullRequest.builder()
                                                                .method(SdkHttpMethod.POST)
                                                                .protocol("https")
                                                                .host("localhost")
                                                                .putHeader(":message-type", "event")
                                                                .putHeader(":event-type", "blob")
                                                                .putHeader("id", "abc")
                                                                .contentStreamProvider(() -> new ByteArrayInputStream(new byte[] {9}))
                                                                .build();
        ByteBuffer bytes = AwsClientHandlerUtils.encodeEventStreamRequestToByteBuffer(legacyMarshalled);

        Struct event = (Struct) decoderFactory().newEventDecoder()
                                                .decode(decoderFactory().newFrameDecoder().decode(bytes).get(0));

        assertThat(event.get("id")).isEqualTo("abc");
        assertThat((byte[]) event.get("data")).containsExactly(9);
    }

    @Test
    void decode_stringHeadersForNonStringMembers_areParsedAsLegacyHeaderUnmarshallingParsesThem() {
        SdkHttpFullRequest legacyMarshalled = SdkHttpFullRequest.builder()
                                                                .method(SdkHttpMethod.POST)
                                                                .protocol("https")
                                                                .host("localhost")
                                                                .putHeader(":message-type", "event")
                                                                .putHeader(":event-type", "bodyAndHeader")
                                                                .putHeader("stringHeader", "s")
                                                                .putHeader("intHeader", "7")
                                                                .putHeader("longHeader", "9")
                                                                .putHeader("booleanHeader", "true")
                                                                .putHeader("timestampHeader",
                                                                           "Tue, 14 Nov 2023 22:13:20 GMT")
                                                                .putHeader("blobHeader", "AQI=")
                                                                .build();
        ByteBuffer bytes = AwsClientHandlerUtils.encodeEventStreamRequestToByteBuffer(legacyMarshalled);

        Struct event = (Struct) decoderFactory().newEventDecoder()
                                                .decode(decoderFactory().newFrameDecoder().decode(bytes).get(0));

        assertThat(event.get("stringHeader")).isEqualTo("s");
        assertThat(event.get("intHeader")).isEqualTo(7);
        assertThat(event.get("longHeader")).isEqualTo(9L);
        assertThat(event.get("booleanHeader")).isEqualTo(true);
        assertThat(event.get("timestampHeader")).isEqualTo(Instant.ofEpochSecond(1_700_000_000L));
        assertThat(event.get("blobHeader")).isNotNull();
    }

    @Test
    void decode_unparsableStringHeader_failsWithoutTheValue() {
        Map<String, HeaderValue> headers = HeadersBuilder.forEvent().eventType("bodyAndHeader")
                                                         .put("intHeader", "secret-not-a-number").build();
        AwsEventFrame frame = new AwsEventFrame(new Message(headers, new byte[0]));

        assertThatThrownBy(() -> decoderFactory().newEventDecoder().decode(frame))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("Invalid integer in event header for member")
            .hasMessageNotContaining("secret");
    }

    @Test
    void decode_withEventBuilders_buildsThePerMemberEventClass() {
        Message message = encoder(false).encodeEvent("secondBodyAndHeader", bodyAndHeader()).unwrap();
        List<String> requested = new ArrayList<>();
        EventDecoder<AwsEventFrame> decoder = decoderFactory().withEventBuilders(name -> {
            requested.add(name);
            return null;
        }).newEventDecoder();

        assertThat(decoder.decode(new AwsEventFrame(message))).isEqualTo(bodyAndHeader());
        assertThat(requested).containsExactly("secondBodyAndHeader");
    }

    @Test
    void decode_unknownEventType_returnsTheUnknownEventOrFails() {
        Message unknown = new Message(HeadersBuilder.forEvent().eventType("newEvent").build(), new byte[0]);
        Object unknownEvent = new Object();

        assertThat(decoderFactory().withUnknownEvent(() -> unknownEvent).newEventDecoder()
                                   .decode(new AwsEventFrame(unknown)))
            .isSameAs(unknownEvent);
        assertThatThrownBy(() -> decoderFactory().newEventDecoder().decode(new AwsEventFrame(unknown)))
            .isInstanceOf(EventStreamingProtocolException.class)
            .hasMessage("Unsupported event type: newEvent");
    }

    @Test
    void decode_exceptionMessage_throwsTheModeledException() {
        AwsEventFrame frame = encoder(false).encodeFailure(new MyError("bad input"));
        assertThat(frame.unwrap().getHeaders().get(":message-type").getString()).isEqualTo("exception");
        assertThat(frame.unwrap().getHeaders().get(":exception-type").getString()).isEqualTo("myError");

        assertThatThrownBy(() -> decoderFactory().newEventDecoder().decode(frame))
            .isInstanceOf(MyError.class)
            .hasMessage("bad input");
    }

    @Test
    void decode_errorMessage_throwsEventStreamingException() {
        AwsEventFrame frame = encoder(false).encodeFailure(new IllegalStateException("boom"));
        assertThat(frame.unwrap().getHeaders().get(":message-type").getString()).isEqualTo("error");

        assertThatThrownBy(() -> decoderFactory().newEventDecoder().decode(frame))
            .isInstanceOfSatisfying(EventStreamingException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo("InternalError");
                assertThat(e.getMessage()).isEqualTo("boom");
            });
    }

    @Test
    void initialMessages_carryTheNonStreamMembers() {
        Message initialRequest = encoder(true).encode(struct(INPUT, "inputHeader", "x")).unwrap();
        assertThat(initialRequest.getHeaders().get(":event-type").getString()).isEqualTo("initial-request");
        assertThat(new String(initialRequest.getPayload(), StandardCharsets.UTF_8)).isEqualTo("{\"inputHeader\":\"x\"}");

        Message initialResponse = new Message(HeadersBuilder.forEvent().eventType("initial-response").build(),
                                              "{\"outputMember\":\"y\"}".getBytes(StandardCharsets.UTF_8));
        assertThat(decoderFactory().newEventDecoder().decodeInitialEvent(new AwsEventFrame(initialResponse)))
            .isEqualTo(struct(OUTPUT, "outputMember", "y"));
    }

    @Test
    void factories_rejectOperationsWithoutAnEventStream() {
        assertThatThrownBy(() -> AwsEventDecoderFactory.forOutputStream(EventStreamTestShapes.OPERATION_WITHOUT_STREAM,
                                                                        CODEC))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(AwsEventEncoderFactory.forInputStream(OPERATION, CODEC, JSON, e -> null).contentType())
            .isEqualTo("application/vnd.amazon.eventstream");
    }

    /**
     * Any event with header and payload members decodes to the event that was encoded. String header values are not
     * empty: the eventstream library rejects empty strings, for the legacy SDK too.
     */
    @Property(tries = 50)
    void encodeThenDecode_roundTripsBodyAndHeaderEvents(@ForAll @NotEmpty String stringHeader, @ForAll int intHeader,
                                                       @ForAll long longHeader, @ForAll boolean booleanHeader,
                                                       @ForAll String message, @ForAll int count) {
        Struct event = struct(BODY_AND_HEADER_EVENT, "stringHeader", stringHeader, "intHeader", intHeader,
                              "longHeader", longHeader, "booleanHeader", booleanHeader, "message", message,
                              "count", count);
        Message encoded = encoder(false).encodeEvent("bodyAndHeader", event).unwrap();

        assertThat(decodeAll(Collections.singletonList(encoded), 3)).containsExactly(event);
    }

    private static List<Object> decodeAll(List<Message> messages, int chunkSize) {
        ByteArrayOutputStream wire = new ByteArrayOutputStream();
        for (Message message : messages) {
            ByteBuffer buffer = AwsFrameCodec.ENCODER.encode(new AwsEventFrame(message));
            wire.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        }
        byte[] bytes = wire.toByteArray();
        FrameDecoder<AwsEventFrame> frameDecoder = decoderFactory().newFrameDecoder();
        EventDecoder<AwsEventFrame> eventDecoder = decoderFactory().newEventDecoder();
        List<Object> events = new ArrayList<>();
        for (int i = 0; i < bytes.length; i += chunkSize) {
            int length = Math.min(chunkSize, bytes.length - i);
            for (AwsEventFrame frame : frameDecoder.decode(ByteBuffer.wrap(bytes, i, length))) {
                events.add(eventDecoder.decode(frame));
            }
        }
        return events;
    }
}
