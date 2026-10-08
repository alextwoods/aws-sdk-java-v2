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

package software.amazon.awssdk.test.differential;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * Canonical text of modeled values and failures, and the encoders of scripted response bodies (JSON, CBOR, XML).
 *
 * <p>Documents are rendered by shape type and value with structure members in name order (the Smithy data model
 * leaves structure members unordered; their wire order is compared on the transmitted bytes instead) and map entries in
 * order. Failures are rendered by their AWS error data and modeled error document, never their Java class, so a
 * generated modeled exception and a {@code DynamicServiceException} of the same error shape render identically.
 */
final class Values {
    private Values() {
    }

    /** The canonical text of a modeled value: a document or a generated model object. */
    /**
     * The canonical text of a modeled value. A modeled structure, generated or dynamic (a Dynamic Client output
     * document is a {@code SerializableStruct} too), is viewed through its schema serialization
     * ({@link Document#of(SerializableStruct)}), so both are compared by what they serialize, not by their in-memory
     * representation (a generated enum member is a string field, a dynamic one an enum document).
     */
    static String render(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof SerializableStruct struct) {
            return renderDocument(Document.of(struct));
        }
        return renderDocument((Document) value);
    }

    private static String renderDocument(Document document) {
        if (document == null) {
            return "null";
        }
        return switch (document.type()) {
            case STRUCTURE, UNION, MAP -> {
                StringBuilder out = new StringBuilder(document.type().toString()).append('{');
                String separator = "";
                Map<String, Document> members = document.type() == ShapeType.MAP
                                                ? document.asStringMap()
                                                : new TreeMap<>(document.asStringMap());
                for (Map.Entry<String, Document> member : members.entrySet()) {
                    out.append(separator).append(member.getKey()).append('=')
                       .append(renderDocument(member.getValue()));
                    separator = ", ";
                }
                yield out.append('}').toString();
            }
            case LIST, SET -> {
                List<String> items = new ArrayList<>();
                for (Document item : document.asList()) {
                    items.add(renderDocument(item));
                }
                yield document.type() + items.toString();
            }
            case BLOB -> "BLOB:" + Base64.getEncoder().encodeToString(bytes(document.asBlob()));
            case TIMESTAMP -> "TIMESTAMP:" + document.asTimestamp();
            case STRING, ENUM -> document.type() + ":" + document.asString();
            case BOOLEAN -> "BOOLEAN:" + document.asBoolean();
            case BYTE, SHORT, INTEGER, INT_ENUM, LONG, FLOAT, DOUBLE, BIG_INTEGER, BIG_DECIMAL ->
                document.type() + ":" + document.asNumber();
            default -> document.type() + ":" + document;
        };
    }

    /**
     * The canonical text of a failure and its causes: service exceptions by status, error code, message, request ID,
     * attempts, retryability and their modeled error document; async wrappers are skipped.
     */
    static String failure(Throwable failure) {
        StringBuilder out = new StringBuilder();
        Throwable current = failure;
        for (int depth = 0; current != null && depth < 8; depth++) {
            if ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
                current = current.getCause();
                continue;
            }
            if (out.length() > 0) {
                out.append(" <- ");
            }
            if (current instanceof AwsServiceException service) {
                AwsErrorDetails details = service.awsErrorDetails();
                out.append("AwsServiceException{status=").append(service.statusCode())
                   .append(", code=").append(details == null ? null : details.errorCode())
                   .append(", errorMessage=").append(details == null ? null : details.errorMessage())
                   .append(", service=").append(details == null ? null : details.serviceName())
                   .append(", requestId=").append(service.requestId())
                   .append(", attempts=").append(service.numAttempts())
                   .append(", retryable=").append(service.retryable())
                   .append(", throttling=").append(service.isThrottlingException());
                if (current instanceof SerializableStruct modeled) {
                    out.append(", modeled=").append(modeled.schema().id()).append(' ')
                       .append(render(modeled));
                } else {
                    out.append(", unmodeled");
                }
                out.append(", message=").append(service.getMessage()).append('}');
            } else {
                out.append(current.getClass().getName()).append(": ").append(current.getMessage());
                if (current instanceof SdkException sdk) {
                    out.append(" {attempts=").append(sdk.numAttempts()).append(", retryable=")
                       .append(sdk.retryable()).append('}');
                }
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return out.toString();
    }

    /** {@code bytes} as printable ASCII, every other byte as {@code \xNN}. */
    static StringBuilder escape(StringBuilder out, byte[] bytes) {
        for (byte b : bytes) {
            int c = b & 0xff;
            if (c >= 0x20 && c < 0x7f && c != '\\') {
                out.append((char) c);
            } else {
                out.append(String.format("\\x%02x", c));
            }
        }
        return out;
    }

    static byte[] bytes(ByteBuffer buffer) {
        ByteBuffer view = buffer.duplicate();
        byte[] out = new byte[view.remaining()];
        view.get(out);
        return out;
    }

    static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------------------------------------------------------
    // Modeled input values

    /** The document of a plain value: strings, numbers, booleans, instants, byte arrays, lists and maps. */
    @SuppressWarnings("unchecked")
    static Document toDocument(Object value) {
        if (value instanceof String s) {
            return Document.of(s);
        } else if (value instanceof Integer i) {
            return Document.of(i.intValue());
        } else if (value instanceof Long l) {
            return Document.of(l.longValue());
        } else if (value instanceof Double d) {
            return Document.of(d.doubleValue());
        } else if (value instanceof BigInteger b) {
            return Document.of(b);
        } else if (value instanceof BigDecimal b) {
            return Document.of(b);
        } else if (value instanceof Boolean b) {
            return Document.of(b.booleanValue());
        } else if (value instanceof Instant t) {
            return Document.of(t);
        } else if (value instanceof byte[] b) {
            return Document.of(b);
        } else if (value instanceof List<?> list) {
            List<Document> out = new ArrayList<>();
            list.forEach(item -> out.add(toDocument(item)));
            return Document.of(out);
        } else if (value instanceof Map<?, ?> map) {
            Map<String, Document> out = new LinkedHashMap<>();
            ((Map<String, Object>) map).forEach((k, v) -> out.put(k, toDocument(v)));
            return Document.of(out);
        }
        throw new IllegalArgumentException(String.valueOf(value));
    }

    /** An insertion-ordered map of alternating keys and values. */
    static Map<String, Object> map(Object... entries) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            out.put((String) entries[i], entries[i + 1]);
        }
        return out;
    }

    // -------------------------------------------------------------------------------------------------------------
    // Response body encoders

    /** JSON text; instants are epoch seconds with a fraction, byte arrays base64. */
    static String json(Object value) {
        if (value instanceof String s) {
            StringBuilder out = new StringBuilder("\"");
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    default -> {
                        if (c < 0x20) {
                            out.append(String.format("\\u%04x", (int) c));
                        } else {
                            out.append(c);
                        }
                    }
                }
            }
            return out.append('"').toString();
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Instant t) {
            return BigDecimal.valueOf(t.toEpochMilli(), 3).toPlainString();
        } else if (value instanceof byte[] b) {
            return json(Base64.getEncoder().encodeToString(b));
        } else if (value instanceof List<?> list) {
            List<String> items = new ArrayList<>();
            list.forEach(item -> items.add(json(item)));
            return "[" + String.join(",", items) + "]";
        } else if (value instanceof Map<?, ?> map) {
            List<String> entries = new ArrayList<>();
            map.forEach((k, v) -> entries.add(json(k) + ":" + json(v)));
            return "{" + String.join(",", entries) + "}";
        }
        throw new IllegalArgumentException(String.valueOf(value));
    }

    /**
     * XML elements of {@code members}: nested maps as nested elements, lists as {@code member} elements (or flattened
     * repeated elements when {@code flattenedLists}), instants as ISO-8601 date-times.
     */
    static String xml(Map<String, Object> members) {
        StringBuilder out = new StringBuilder();
        members.forEach((name, value) -> {
            out.append('<').append(name).append('>');
            if (value instanceof Map<?, ?> map) {
                out.append(xml(castMap(map)));
            } else if (value instanceof List<?> list) {
                for (Object item : list) {
                    out.append("<member>");
                    out.append(item instanceof Map<?, ?> m ? xml(castMap(m)) : xmlEscape(item.toString()));
                    out.append("</member>");
                }
            } else if (value instanceof byte[] b) {
                out.append(Base64.getEncoder().encodeToString(b));
            } else {
                out.append(xmlEscape(value.toString()));
            }
            out.append("</").append(name).append('>');
        });
        return out.toString();
    }

    static String xmlEscape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    /** CBOR of maps, lists, text, byte strings, integers, booleans and instants (tag 1, epoch seconds). */
    static byte[] cbor(Object value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeCbor(out, value);
        return out.toByteArray();
    }

    private static void writeCbor(ByteArrayOutputStream out, Object value) {
        if (value instanceof String s) {
            byte[] bytes = utf8(s);
            cborHead(out, 3, bytes.length);
            out.writeBytes(bytes);
        } else if (value instanceof byte[] bytes) {
            cborHead(out, 2, bytes.length);
            out.writeBytes(bytes);
        } else if (value instanceof Boolean b) {
            out.write(b ? 0xf5 : 0xf4);
        } else if (value instanceof Integer || value instanceof Long) {
            long n = ((Number) value).longValue();
            if (n >= 0) {
                cborHead(out, 0, n);
            } else {
                cborHead(out, 1, -1 - n);
            }
        } else if (value instanceof Instant t) {
            out.write(0xc1);
            writeCbor(out, t.getEpochSecond());
        } else if (value instanceof List<?> list) {
            cborHead(out, 4, list.size());
            list.forEach(item -> writeCbor(out, item));
        } else if (value instanceof Map<?, ?> map) {
            cborHead(out, 5, map.size());
            map.forEach((k, v) -> {
                writeCbor(out, k);
                writeCbor(out, v);
            });
        } else {
            throw new IllegalArgumentException(String.valueOf(value));
        }
    }

    private static void cborHead(ByteArrayOutputStream out, int major, long n) {
        int type = major << 5;
        if (n < 24) {
            out.write(type | (int) n);
        } else if (n < 0x100) {
            out.write(type | 24);
            out.write((int) n);
        } else if (n < 0x10000) {
            out.write(type | 25);
            out.write((int) (n >> 8));
            out.write((int) n);
        } else if (n < 0x100000000L) {
            out.write(type | 26);
            for (int shift = 24; shift >= 0; shift -= 8) {
                out.write((int) (n >> shift));
            }
        } else {
            out.write(type | 27);
            for (int shift = 56; shift >= 0; shift -= 8) {
                out.write((int) (n >> shift));
            }
        }
    }
}
