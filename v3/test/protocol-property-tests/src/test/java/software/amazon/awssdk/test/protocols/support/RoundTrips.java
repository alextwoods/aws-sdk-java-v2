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

package software.amazon.awssdk.test.protocols.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.eventstream.HeaderValue;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.eventstream.MessageDecoder;
import software.amazon.awssdk.eventstream.MessageEncoder;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.eventstream.EventStreamCodec;
import software.amazon.awssdk.protocols.core.xml.XmlShapeSerializer;
import software.amazon.awssdk.test.protocols.support.Canonical.Struct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/**
 * The round trip of one {@link RandomModel} value through its protocol runtime, by protocol:
 * <ul>
 *   <li><b>awsJson1_0, awsJson1_1, rpcv2Cbor</b>: {@link OperationProtocol#serializeRequest} of the input, then
 *       {@link OperationProtocol#deserializeResponse} of a 200 response carrying the request's headers and body into
 *       the output (same members). An event stream request body is read to its end; its {@code initial-request}
 *       message becomes the {@code initial-response} message and every event frame is passed through
 *       byte-for-byte.</li>
 *   <li><b>restJson1, restXml</b>: the same, so that headers, prefix headers, payloads and body members are written
 *       by the request binding writer and read by the response binding reader. Labels and query parameters have no
 *       response location: they are decoded from the request URI by an RFC 3986 decoder of this test (not the
 *       runtime's) and compared with the input too.</li>
 *   <li><b>awsQuery, ec2Query</b>: the query protocols write form-encoded requests and read XML responses, so the
 *       round trip is response-direction: the value is written with the V3 {@link XmlShapeSerializer} (the XML
 *       binding shared with restXml), wrapped as the protocol's response ({@code <OpResponse><OpResult>} for
 *       awsQuery, {@code <OpResponse>} for ec2Query), and read with {@code deserializeResponse}. The form writer
 *       has no V3 reader; its encoding is covered by the protocol conformance tests.</li>
 * </ul>
 * The result is read back into a {@link Canonical} tree, reading event streams to their end.
 */
public final class RoundTrips {
    private static final URI ENDPOINT = URI.create("https://roundtrip.example.com");
    private static final ShapeId SERVICE = ShapeId.from(RandomModel.NAMESPACE + "#RoundTrip");
    private static final ShapeId OPERATION = ShapeId.from(RandomModel.NAMESPACE + "#Op");
    private static final ShapeId INPUT = ShapeId.from(RandomModel.NAMESPACE + "#OpInput");
    private static final ShapeId OUTPUT = ShapeId.from(RandomModel.NAMESPACE + "#OpOutput");

    /** The prelude and trait definitions, assembled once. */
    private static final Model BASE = Model.assembler(RoundTrips.class.getClassLoader())
                                           .discoverModels(RoundTrips.class.getClassLoader())
                                           .assemble()
                                           .unwrap();

    private RoundTrips() {
    }

    /** The result of one round trip. */
    public record Result(Struct output, Struct uriValues, String wire) {
    }

    /** Assembles and validates {@code model}'s IDL; a generator bug fails here. */
    public static Model assemble(RandomModel model) {
        return Model.assembler().addModel(BASE).addUnparsedModel("roundtrip.smithy", model.idl()).assemble()
                    .unwrap();
    }

    /**
     * The encoding half of a round trip: the response that carries {@code value} (a value of the model's input, or,
     * for the query protocols, of its output) before the runtime reads it.
     *
     * @param service the model's service
     * @param protocol the operation's protocol runtime
     * @param request the serialized request, or {@code null} for the query protocols (response direction)
     * @param response the response to read with {@link OperationProtocol#deserializeResponse}
     * @param wire a description of the wire message, for failure reports
     */
    public record Encoded(DynamicService service, OperationProtocol protocol, SdkHttpRequest request,
                          SdkHttpResponse response, String wire) {
        /** A fresh builder of the operation's output structure. */
        public ShapeBuilder<StructDocument> outputBuilder() {
            return service.builder(OUTPUT);
        }
    }

    public static Result run(RandomModel model) {
        Encoded encoded = encode(model, model.input());
        StructDocument output = encoded.protocol().deserializeResponse(encoded.response(), encoded.outputBuilder());
        Struct uriValues = encoded.request() == null ? new Struct(Map.of()) : uriValues(model, encoded.request());
        return new Result(Canonical.tree(output), uriValues, encoded.wire());
    }

    /** Encodes {@code value} as the response {@link #run} would read, without reading it. */
    public static Encoded encode(RandomModel model, Struct value) {
        DynamicService service = new DynamicService(assemble(model), SERVICE);
        OperationProtocol protocol = service.client(model.protocol().provider())
                                            .operation(service.schemas(service.operation(OPERATION)));
        return model.protocol().query() ? queryResponse(model, value, service, protocol)
                                        : requestToResponse(model, value, service, protocol);
    }

    private static Encoded requestToResponse(RandomModel model, Struct value, DynamicService service,
                                             OperationProtocol protocol) {
        Canonical.EventWriters writers = new Canonical.EventWriters();
        StructDocument input = Canonical.build(service.builder(INPUT), value, writers);
        SdkHttpRequest request = protocol.serializeRequest(input, ENDPOINT);
        byte[] body = readBody(request.body(), writers);
        String contentType = request.headers().firstValue("Content-Type").orElse(null);
        HttpBody responseBody;
        String wire;
        if (model.hasEventStream()) {
            byte[] frames = initialRequestAsResponse(body);
            responseBody = HttpBody.streaming(StreamingBodies.fromBytes(frames, contentType));
            wire = describeFrames(frames);
        } else {
            responseBody = HttpBody.fromBytes(body);
            wire = describe(body, model.protocol());
        }
        HttpHeaders.Builder headers = HttpHeaders.builder();
        for (int i = 0; i < request.headers().size(); i++) {
            headers.add(request.headers().name(i), request.headers().value(i));
        }
        SdkHttpResponse response = SdkHttpResponse.builder().statusCode(200).headers(headers.build())
                                                  .body(responseBody).build();
        return new Encoded(service, protocol, request, response,
                           request.method() + " " + request.encodedPath()
                           + (request.encodedQuery() == null ? "" : "?" + request.encodedQuery()) + "\n"
                           + request.headers() + "\n" + wire);
    }

    private static Encoded queryResponse(RandomModel model, Struct tree, DynamicService service,
                                         OperationProtocol protocol) {
        StructDocument value = Canonical.build(service.builder(OUTPUT), tree, new Canonical.EventWriters());
        String xml = new String(XmlShapeSerializer.serialize(value, XmlNamespaceTrait.builder()
                                                                                        .uri(RandomModel.XML_NAMESPACE)
                                                                                        .build()),
                                StandardCharsets.UTF_8);
        // Rename the root element (the output structure's name) to the protocol's result element.
        String wrapper = model.protocol() == WireProtocol.AWS_QUERY ? "OpResult" : "OpResponse";
        if (!xml.startsWith("<OpOutput")) {
            throw new AssertionError("Unexpected root element: " + xml);
        }
        String body = "<" + wrapper + xml.substring("<OpOutput".length());
        if (body.endsWith("</OpOutput>")) {
            body = body.substring(0, body.length() - "</OpOutput>".length()) + "</" + wrapper + ">";
        }
        if (model.protocol() == WireProtocol.AWS_QUERY) {
            body = "<OpResponse xmlns=\"" + RandomModel.XML_NAMESPACE + "\">" + body
                   + "<ResponseMetadata><RequestId>r</RequestId></ResponseMetadata></OpResponse>";
        }
        SdkHttpResponse response = SdkHttpResponse.builder().statusCode(200)
                                                  .addHeader("Content-Type", "text/xml")
                                                  .body(HttpBody.fromUtf8(body)).build();
        return new Encoded(service, protocol, null, response, body);
    }

    /** The request body; an event stream body is read while its events are written. */
    private static byte[] readBody(HttpBody body, Canonical.EventWriters writers) {
        if (body.kind() != HttpBody.Kind.STREAMING) {
            return writers.isEmpty() ? body.asByteArray() : withEvents(body, writers);
        }
        return withEvents(body, writers);
    }

    /**
     * Reads an event stream request body while a producer writes the events. The producer starts only after
     * {@code openStream} has activated the writer: Smithy Java 1.6.1's {@code DefaultEventStreamWriter} fails
     * activation ("Stream is already completed or closed") if the writer was closed before it, which happens to a
     * stream with no events that is closed before the request is sent.
     */
    private static byte[] withEvents(HttpBody body, Canonical.EventWriters writers) {
        try (InputStream in = body.streamingBody().openStream()) {
            CompletableFuture<Void> producer = CompletableFuture.runAsync(writers::writeAll);
            byte[] bytes = in.readAllBytes();
            producer.get(30, TimeUnit.SECONDS);
            return bytes;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (Exception e) {
            throw new IllegalStateException("The event producer failed", e);
        }
    }

    /** The request frames with {@code initial-request} renamed {@code initial-response}. */
    private static byte[] initialRequestAsResponse(byte[] frames) {
        MessageDecoder decoder = new MessageDecoder();
        List<Message> messages = decoder.decode(ByteBuffer.wrap(frames));
        decoder.endOfStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Message message : messages) {
            HeaderValue type = message.header(EventStreamCodec.EVENT_TYPE);
            if (type != null && EventStreamCodec.INITIAL_REQUEST.equals(type.asString())) {
                Map<String, HeaderValue> headers = new LinkedHashMap<>(message.headers());
                headers.put(EventStreamCodec.EVENT_TYPE, HeaderValue.ofString(EventStreamCodec.INITIAL_RESPONSE));
                message = new Message(headers, message.payloadBytes());
            }
            ByteBuffer encoded = MessageEncoder.encode(message);
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            out.writeBytes(bytes);
        }
        return out.toByteArray();
    }

    private static String describeFrames(byte[] frames) {
        StringBuilder text = new StringBuilder();
        for (Message message : new MessageDecoder().decode(ByteBuffer.wrap(frames))) {
            text.append(message.headers()).append(' ')
                .append(new String(message.payloadBytes(), StandardCharsets.UTF_8)).append('\n');
        }
        return text.toString();
    }

    private static String describe(byte[] body, WireProtocol protocol) {
        return protocol.cbor() ? java.util.HexFormat.of().formatHex(body) : new String(body, StandardCharsets.UTF_8);
    }

    // Independent URI decoding of labels and query parameters.

    private static Struct uriValues(RandomModel model, SdkHttpRequest request) {
        Map<String, Object> values = new TreeMap<>();
        String[] segments = request.encodedPath().split("/", -1);
        int label = 2;
        Map<String, List<String>> query = new LinkedHashMap<>();
        String encodedQuery = request.encodedQuery();
        if (encodedQuery != null && !encodedQuery.isEmpty()) {
            for (String pair : encodedQuery.split("&")) {
                int eq = pair.indexOf('=');
                String name = decode(eq < 0 ? pair : pair.substring(0, eq));
                String value = eq < 0 ? "" : decode(pair.substring(eq + 1));
                query.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
            }
        }
        for (RandomModel.UriMember member : model.uriMembers()) {
            if (member.binding() == RandomModel.Binding.LABEL) {
                values.put(member.name(), parse(member.type(), decode(segments[label++]), member.timestampFormat()));
            } else {
                List<String> params = query.get(member.wireName());
                if (params == null) {
                    continue;
                }
                if (member.type() instanceof RandomModel.ListType) {
                    values.put(member.name(), List.copyOf(params));
                } else {
                    values.put(member.name(), parse(member.type(), params.get(0), member.timestampFormat()));
                }
            }
        }
        return new Struct(values);
    }

    private static Object parse(RandomModel.Type type, String text, String timestampFormat) {
        RandomModel.Kind kind = ((RandomModel.Simple) type).kind();
        return switch (kind) {
            case STRING -> text;
            case INTEGER -> Integer.parseInt(text);
            case LONG -> Long.parseLong(text);
            case BOOLEAN -> Boolean.parseBoolean(text);
            case DOUBLE -> switch (text) {
                case "NaN" -> Double.NaN;
                case "Infinity" -> Double.POSITIVE_INFINITY;
                case "-Infinity" -> Double.NEGATIVE_INFINITY;
                default -> Double.parseDouble(text);
            };
            case TIMESTAMP -> switch (timestampFormat) {
                case "epoch-seconds" -> {
                    BigDecimal seconds = new BigDecimal(text);
                    yield Instant.ofEpochSecond(seconds.longValue(),
                                                seconds.subtract(new BigDecimal(seconds.toBigInteger()))
                                                       .movePointRight(9).intValue());
                }
                case "http-date" -> java.time.ZonedDateTime.parse(
                    text, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                default -> Instant.parse(text);
            };
            default -> throw new IllegalArgumentException("Not a URI type: " + kind);
        };
    }

    /** RFC 3986 percent-decoding of UTF-8 ({@code +} is a literal plus). */
    private static String decode(String encoded) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c == '%') {
                bytes.write(Integer.parseInt(encoded.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                bytes.write(c);
            }
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
