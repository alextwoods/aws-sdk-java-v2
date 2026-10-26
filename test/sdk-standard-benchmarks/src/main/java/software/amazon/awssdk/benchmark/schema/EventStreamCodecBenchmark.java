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

package software.amazon.awssdk.benchmark.schema;

import static software.amazon.awssdk.benchmark.schema.EventStreamBenchmarkShapes.BLOB_EVENT;
import static software.amazon.awssdk.benchmark.schema.EventStreamBenchmarkShapes.BODY_AND_HEADER_EVENT;
import static software.amazon.awssdk.benchmark.schema.EventStreamBenchmarkShapes.HEADERS_ONLY_EVENT;
import static software.amazon.awssdk.benchmark.schema.EventStreamBenchmarkShapes.OPERATION;
import static software.amazon.awssdk.benchmark.schema.EventStreamBenchmarkShapes.STRING_EVENT;
import static software.amazon.awssdk.benchmark.schema.EventStreamBenchmarkShapes.STRUCTURE_EVENT;
import static software.amazon.awssdk.benchmark.schema.EventStreamBenchmarkShapes.TEST_OPERATION_INPUT;
import static software.amazon.awssdk.benchmark.schema.EventStreamBenchmarkShapes.struct;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.protocols.client.http.eventstream.AwsEventDecoderFactory;
import software.amazon.awssdk.protocols.client.http.eventstream.AwsEventEncoderFactory;
import software.amazon.awssdk.protocols.client.http.eventstream.AwsEventEncoderFactory.AwsEventEncoder;
import software.amazon.awssdk.protocols.client.http.eventstream.AwsEventFrame;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.event.EventDecoder;
import software.amazon.awssdk.schema.serde.event.EventStreamingException;

/**
 * Encodes and decodes one AWS event stream message per operation through the schema-based event codec
 * ({@code AwsEventShapeEncoder} and {@code AwsEventShapeDecoder}, behind {@link AwsEventEncoderFactory} and
 * {@link AwsEventDecoderFactory}), with the JSON codec for message bodies. Neither framing nor transport is measured.
 *
 * <p>The archetypes are those of smithy-java's {@code EventEncodeBenchmark} (smithy-java {@code 8e892b96c}), which
 * exercise the per-message header, payload and event-payload member partition:
 * <ul>
 *     <li>{@code headersOnly}: one {@code eventHeader} member, no body;</li>
 *     <li>{@code structure}: one body member;</li>
 *     <li>{@code bodyAndHeader}: one header and one body member;</li>
 *     <li>{@code stringPayload} and {@code blobPayload}: an {@code eventPayload} member;</li>
 *     <li>{@code initialRequest}: the operation input without its event stream member, one header and one body
 *     member.</li>
 * </ul>
 * Events are encoded with {@code encodeEvent}, as generated clients do; {@code decode} decodes the frame that
 * {@code encode} produces. Run with {@code -prof gc} for bytes per op.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(2)
public class EventStreamCodecBenchmark {
    @Param({"headersOnly", "structure", "bodyAndHeader", "stringPayload", "blobPayload", "initialRequest"})
    private String archetype;

    private AwsEventEncoder encoder;
    private EventDecoder<AwsEventFrame> decoder;
    private String eventType;
    private SerializableStruct event;
    private AwsEventFrame frame;

    @Setup
    public void setup() {
        Codec codec = JsonCodec.builder().build();
        // One encoder and decoder per trial, as one serves a whole stream.
        encoder = AwsEventEncoderFactory.forInputStream(OPERATION, codec, "application/json",
                                                        e -> new EventStreamingException("InternalError",
                                                                                         e.getMessage()))
                                        .newEventEncoder();
        decoder = AwsEventDecoderFactory.forInputStream(OPERATION, codec).newEventDecoder();
        switch (archetype) {
            case "headersOnly":
                eventType = "headersOnlyMember";
                event = struct(HEADERS_ONLY_EVENT, "sequenceNum", 123);
                break;
            case "structure":
                eventType = "structureMember";
                event = struct(STRUCTURE_EVENT, "foo", "memberFooValue");
                break;
            case "bodyAndHeader":
                eventType = "bodyAndHeaderMember";
                event = struct(BODY_AND_HEADER_EVENT, "intMember", 123, "stringMember", "Hello world!");
                break;
            case "stringPayload":
                eventType = "stringMember";
                event = struct(STRING_EVENT, "payload", "hello world!");
                break;
            case "blobPayload":
                eventType = "blobMember";
                event = struct(BLOB_EVENT, "payload", "hello world!".getBytes(StandardCharsets.UTF_8));
                break;
            case "initialRequest":
                eventType = null;
                event = struct(TEST_OPERATION_INPUT, "headerString", "headerValue",
                               "inputStringMember", "inputStringValue");
                break;
            default:
                throw new IllegalArgumentException("Unknown archetype: " + archetype);
        }
        frame = encode();
    }

    @Benchmark
    public AwsEventFrame encode() {
        return eventType == null ? encoder.encode(event) : encoder.encodeEvent(eventType, event);
    }

    @Benchmark
    public Object decode() {
        return eventType == null ? decoder.decodeInitialEvent(frame) : decoder.decode(frame);
    }
}
