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

package software.amazon.awssdk.test.fleet;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The wire observations the differential compares, normalized the same way for V3 and V2, and the small codecs it
 * needs (form bodies and CBOR) so the comparison does not depend on either SDK's own parsers.
 *
 * <p><b>Normalization rule</b> (documented in {@code wire-differential/README.md}): a request is its method, host,
 * raw path, decoded query parameters (sorted multiset), the protocol-semantic headers ({@link #SEMANTIC_HEADERS})
 * plus every header the operation input binds ({@code @httpHeader}, {@code @httpPrefixHeaders}), lower-cased names,
 * and its body normalized by media type (JSON and CBOR as data, form bodies as a sorted parameter multiset, XML with
 * inter-element whitespace and the declaration removed, anything else as Base64). Transport, signing, user-agent,
 * invocation-ID, retry, length and checksum headers are runtime-owned and covered by their own suites (signing,
 * telemetry, checksum and retry conformance), so they are not wire-semantic deltas of the model.
 */
final class Wire {
    static final List<String> SEMANTIC_HEADERS = List.of("content-type", "x-amz-target", "smithy-protocol", "accept");

    private Wire() {
    }

    /** One normalized request. */
    record Request(String method, String host, String path, List<String> query, SortedMap<String, String> headers,
                   String body) {

        /** The aspect-by-aspect view the differential compares (each a separately classifiable aspect). */
        SortedMap<String, String> aspects() {
            SortedMap<String, String> result = new TreeMap<>();
            result.put("request.method", method);
            result.put("request.host", host);
            result.put("request.path", path);
            result.put("request.query", String.valueOf(query));
            headers.forEach((name, value) -> result.put("request.header:" + name, value));
            result.put("request.body", body);
            return result;
        }
    }

    /** The decoded query parameters of a raw (encoded) query string, as sorted {@code name=value} entries. */
    static List<String> decodeQuery(String encodedQuery) {
        List<String> result = new ArrayList<>();
        if (encodedQuery == null || encodedQuery.isEmpty()) {
            return result;
        }
        for (String pair : encodedQuery.split("&")) {
            int eq = pair.indexOf('=');
            String name = eq < 0 ? pair : pair.substring(0, eq);
            String value = eq < 0 ? "" : pair.substring(eq + 1);
            result.add(decode(name) + "=" + decode(value));
        }
        result.sort(null);
        return result;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    /** The normalized body by content type (see the class documentation). */
    static String normalizeBody(String contentType, byte[] body) {
        if (body.length == 0) {
            return "";
        }
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        try {
            if (type.contains("json")) {
                return Node.printJson(sorted(Node.parse(new String(body, StandardCharsets.UTF_8))));
            }
            if (type.contains("cbor")) {
                return Node.printJson(sorted(new CborReader(body).read()));
            }
            if (type.contains("x-www-form-urlencoded")) {
                return String.valueOf(decodeQuery(new String(body, StandardCharsets.UTF_8)));
            }
            if (type.contains("xml")) {
                return new String(body, StandardCharsets.UTF_8).replaceAll("<\\?xml[^>]*\\?>", "")
                                                               .replaceAll(">\\s+<", "><").strip();
            }
        } catch (RuntimeException e) {
            return "unparseable " + type + " body: " + Base64.getEncoder().encodeToString(body);
        }
        return "bytes:" + Base64.getEncoder().encodeToString(body);
    }

    private static Node sorted(Node node) {
        if (node.isObjectNode()) {
            ObjectNode.Builder builder = ObjectNode.builder();
            new TreeMap<>(node.expectObjectNode().getStringMap()).forEach((k, v) -> builder.withMember(k, sorted(v)));
            return builder.build();
        }
        if (node.isArrayNode()) {
            ArrayNode.Builder builder = ArrayNode.builder();
            node.expectArrayNode().getElements().forEach(e -> builder.withValue(sorted(e)));
            return builder.build();
        }
        return node;
    }

    /** A CBOR map of text keys to text, integer, boolean or nested-map values (definite lengths). */
    static byte[] cbor(Map<String, Object> map) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeCbor(out, map);
        return out.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static void writeCbor(ByteArrayOutputStream out, Object value) {
        if (value instanceof String s) {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            head(out, 3, bytes.length);
            out.writeBytes(bytes);
        } else if (value instanceof Boolean b) {
            out.write(b ? 0xF5 : 0xF4);
        } else if (value instanceof Number n) {
            long v = n.longValue();
            if (v >= 0) {
                head(out, 0, v);
            } else {
                head(out, 1, -1 - v);
            }
        } else if (value instanceof Map<?, ?> m) {
            head(out, 5, m.size());
            ((Map<String, Object>) m).forEach((k, v) -> {
                writeCbor(out, k);
                writeCbor(out, v);
            });
        } else {
            throw new IllegalArgumentException("Unsupported CBOR value " + value);
        }
    }

    private static void head(ByteArrayOutputStream out, int major, long length) {
        int type = major << 5;
        if (length < 24) {
            out.write(type | (int) length);
        } else if (length < 256) {
            out.write(type | 24);
            out.write((int) length);
        } else if (length < 65536) {
            out.write(type | 25);
            out.write((int) (length >> 8));
            out.write((int) length);
        } else {
            out.write(type | 26);
            for (int shift = 24; shift >= 0; shift -= 8) {
                out.write((int) (length >> shift));
            }
        }
    }

    /** A minimal CBOR decoder to Smithy nodes (definite and indefinite lengths; tags kept as {@code {"tag:N": v}}). */
    static final class CborReader {
        private final byte[] bytes;
        private int position;

        CborReader(byte[] bytes) {
            this.bytes = bytes;
        }

        Node read() {
            Node node = item();
            if (position != bytes.length) {
                throw new IllegalArgumentException("Trailing CBOR bytes at " + position);
            }
            return node;
        }

        private Node item() {
            int initial = bytes[position++] & 0xFF;
            int major = initial >>> 5;
            int info = initial & 0x1F;
            if (major == 7) {
                return switch (info) {
                    case 20 -> Node.from(false);
                    case 21 -> Node.from(true);
                    case 22, 23 -> Node.nullNode();
                    case 25 -> Node.from(Float.float16ToFloat((short) uint(2)));
                    case 26 -> Node.from(Float.intBitsToFloat((int) uint(4)));
                    case 27 -> Node.from(Double.longBitsToDouble(uint(8)));
                    default -> throw new IllegalArgumentException("Unsupported CBOR simple value " + info);
                };
            }
            boolean indefinite = info == 31;
            long length = indefinite ? -1 : argument(info);
            switch (major) {
                case 0:
                    return Node.from(length);
                case 1:
                    return Node.from(-1 - length);
                case 2:
                case 3: {
                    ByteArrayOutputStream data = new ByteArrayOutputStream();
                    if (indefinite) {
                        while ((bytes[position] & 0xFF) != 0xFF) {
                            int chunkInfo = bytes[position++] & 0x1F;
                            int chunk = (int) argument(chunkInfo);
                            data.write(bytes, position, chunk);
                            position += chunk;
                        }
                        position++;
                    } else {
                        data.write(bytes, position, (int) length);
                        position += (int) length;
                    }
                    return major == 3 ? Node.from(data.toString(StandardCharsets.UTF_8))
                                      : Node.from("bytes:" + Base64.getEncoder().encodeToString(data.toByteArray()));
                }
                case 4: {
                    ArrayNode.Builder array = ArrayNode.builder();
                    for (long i = 0; indefinite ? !atBreak() : i < length; i++) {
                        array.withValue(item());
                    }
                    return array.build();
                }
                case 5: {
                    ObjectNode.Builder object = ObjectNode.builder();
                    for (long i = 0; indefinite ? !atBreak() : i < length; i++) {
                        String key = item().expectStringNode().getValue();
                        object.withMember(key, item());
                    }
                    return object.build();
                }
                case 6:
                    return Node.objectNode().withMember("tag:" + length, item());
                default:
                    throw new IllegalArgumentException("Unsupported CBOR major type " + major);
            }
        }

        private boolean atBreak() {
            if ((bytes[position] & 0xFF) == 0xFF) {
                position++;
                return true;
            }
            return false;
        }

        private long argument(int info) {
            return switch (info) {
                case 24 -> uint(1);
                case 25 -> uint(2);
                case 26 -> uint(4);
                case 27 -> uint(8);
                default -> {
                    if (info > 27) {
                        throw new IllegalArgumentException("Unsupported CBOR length " + info);
                    }
                    yield info;
                }
            };
        }

        private long uint(int size) {
            long value = 0;
            for (int i = 0; i < size; i++) {
                value = (value << 8) | (bytes[position++] & 0xFF);
            }
            return value;
        }
    }
}
