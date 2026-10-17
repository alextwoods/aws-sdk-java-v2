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

package software.amazon.awssdk.httpbinding.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.SmithyEnum;
import software.amazon.awssdk.schema.SmithyIntEnum;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Compiles an HTTP URI pattern into a compact tape for serializing the path of a single operation.
 *
 * <p>The tape is an {@code int[]} of {@code (opcode, operand)} pairs:
 * <ul>
 *   <li>{@code 1}: emit the literal at {@code literals[operand]}</li>
 *   <li>{@code 2}: emit a label, percent-encoding {@code /}; operand is the member index</li>
 *   <li>{@code 3}: emit a greedy label, keeping {@code /}; operand is the member index</li>
 * </ul>
 *
 * <p>Port of smithy-java {@code PathSerializer}.
 */
@SdkInternalApi
final class PathSerializer {
    private static final int LITERAL = 1;
    private static final int LABEL = 2;
    private static final int GREEDY_LABEL = 3;

    private final UriPattern uriPattern;
    private final Schema schema;
    private final int[] tape;
    private final String[] literals;
    // Non-null if the path has no labels.
    private final String fastPath;

    /**
     * @param uriPattern the operation's parsed {@code @http} URI pattern
     * @param inputSchema the input structure that provides the label members
     * @throws IllegalStateException if a label has no member of that name
     */
    PathSerializer(UriPattern uriPattern, Schema inputSchema) {
        this.uriPattern = uriPattern;
        this.schema = inputSchema;
        List<String> literalList = new ArrayList<>();
        List<Integer> tapeList = new ArrayList<>();

        // Literal text is accumulated until a label is reached, so adjacent literals collapse into one entry.
        StringBuilder current = new StringBuilder("/");
        boolean firstSegment = true;
        for (UriPattern.Segment seg : uriPattern.segments()) {
            if (!firstSegment) {
                current.append('/');
            }
            firstSegment = false;
            if (seg.isLabel() || seg.isGreedyLabel()) {
                if (current.length() > 0) {
                    tapeList.add(LITERAL);
                    tapeList.add(literalList.size());
                    literalList.add(current.toString());
                    current.setLength(0);
                }
                String name = seg.content();
                Schema labelMember = inputSchema.member(name);
                if (labelMember == null) {
                    throw new IllegalStateException("URI label `" + name + "` has no member in " + inputSchema.id());
                }
                tapeList.add(seg.isGreedyLabel() ? GREEDY_LABEL : LABEL);
                tapeList.add(labelMember.memberIndex());
            } else {
                current.append(seg.content());
            }
        }

        // Trailing literal: only emitted when non-empty, so a path ending in a label does no empty append.
        if (current.length() > 0) {
            tapeList.add(LITERAL);
            tapeList.add(literalList.size());
            literalList.add(current.toString());
        }

        this.literals = literalList.toArray(new String[0]);
        this.tape = new int[tapeList.size()];
        for (int i = 0; i < tape.length; i++) {
            tape[i] = tapeList.get(i);
        }
        this.fastPath = tape.length == 2 && tape[0] == LITERAL ? literals[0] : null;
    }

    UriPattern uriPattern() {
        return uriPattern;
    }

    /**
     * Writes the path for {@code struct}: literals as written in the pattern, labels percent-encoded (RFC 3986
     * unreserved set; {@code /} kept only for a greedy label).
     *
     * @param trimGreedyLabelLeadingSlash true to drop one leading {@code /} of a greedy label value before encoding it
     * @throws SerializationException if a label member is null or an empty string (Requirement 15.7)
     */
    String serialize(SerializableStruct struct, boolean trimGreedyLabelLeadingSlash) {
        if (fastPath != null) {
            return fastPath;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tape.length; i += 2) {
            int op = tape[i];
            int operand = tape[i + 1];
            if (op == LITERAL) {
                sb.append(literals[operand]);
            } else {
                Schema labelSchema = schema.member(operand);
                String value = formatLabelValue(struct, labelSchema);
                boolean greedy = op == GREEDY_LABEL;
                if (greedy && trimGreedyLabelLeadingSlash && value.startsWith("/")) {
                    value = value.substring(1);
                }
                UrlEncoding.encodeUnreserved(value, sb, greedy);
            }
        }
        return sb.toString();
    }

    // Labels support string, enum, intEnum, boolean, the numbers and timestamp. A value is a raw Java value
    // (generated shapes), a SmithyEnum/SmithyIntEnum, or a Document (document-backed structures such as the
    // Dynamic_Client).
    private static String formatLabelValue(SerializableStruct struct, Schema labelSchema) {
        Object value = struct.getMemberValue(labelSchema);
        if (value == null) {
            throw new SerializationException("Required httpLabel member `" + labelSchema.id() + "` must not be null");
        }
        Document doc = value instanceof Document ? (Document) value : null;
        switch (labelSchema.type()) {
            case STRING:
            case ENUM:
                String s = stringValue(labelSchema, value, doc);
                if (s.isEmpty()) {
                    throw new SerializationException("Required httpLabel member `" + labelSchema.id()
                                                     + "` must not be empty");
                }
                return s;
            case BOOLEAN:
                return Boolean.toString(doc != null ? doc.asBoolean() : (Boolean) value);
            case BYTE:
                return Byte.toString(doc != null ? doc.asByte() : (Byte) value);
            case SHORT:
                return Short.toString(doc != null ? doc.asShort() : (Short) value);
            case INTEGER:
                return Integer.toString(doc != null ? doc.asInteger() : (Integer) value);
            case INT_ENUM:
                return Integer.toString(intEnumValue(labelSchema, value, doc));
            case LONG:
                return Long.toString(doc != null ? doc.asLong() : (Long) value);
            case FLOAT:
                return Float.toString(doc != null ? doc.asFloat() : (Float) value);
            case DOUBLE:
                return Double.toString(doc != null ? doc.asDouble() : (Double) value);
            case BIG_INTEGER:
                return (doc != null ? doc.asBigInteger() : (BigInteger) value).toString();
            case BIG_DECIMAL:
                return (doc != null ? doc.asBigDecimal() : (BigDecimal) value).toString();
            case TIMESTAMP:
                return HttpBindingSchemaExtensions.memberBindingOf(labelSchema)
                                                  .timestampFormatter()
                                                  .writeString(doc != null ? doc.asTimestamp() : (Instant) value);
            default:
                throw new SerializationException("Unsupported HTTP label type " + labelSchema.type() + " for `"
                                                 + labelSchema.id() + "`");
        }
    }

    private static String stringValue(Schema labelSchema, Object value, Document doc) {
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof SmithyEnum) {
            return ((SmithyEnum) value).getValue();
        }
        if (doc != null) {
            return doc.asString();
        }
        throw unsupportedLabelValue(labelSchema, value);
    }

    private static int intEnumValue(Schema labelSchema, Object value, Document doc) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof SmithyIntEnum) {
            return ((SmithyIntEnum) value).getValue();
        }
        if (doc != null) {
            return doc.asInteger();
        }
        throw unsupportedLabelValue(labelSchema, value);
    }

    private static SerializationException unsupportedLabelValue(Schema labelSchema, Object value) {
        return new SerializationException("Unsupported HTTP label value " + value.getClass() + " for `"
                                          + labelSchema.id() + "`");
    }
}
