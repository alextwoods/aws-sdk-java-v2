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

package software.amazon.awssdk.test.protocoltests.support;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.StreamingTrait;

/**
 * Converts the {@code params} of a protocol test case to a Smithy Java {@link Document} that a generated builder reads
 * (through {@link Document#asShape}), following the Smithy protocol test conventions for parameter values: blobs are
 * UTF-8 strings, timestamps epoch seconds, non-finite floats the strings {@code NaN}, {@code Infinity} and
 * {@code -Infinity}, and document members are untyped JSON.
 */
public final class NodeValues {
    private final Model model;

    public NodeValues(Model model) {
        this.model = model;
    }

    /**
     * The document of the members of {@code structure} given by {@code params}; streaming blob members are left out
     * (they are bound to the call, not the modeled value).
     */
    public Document structure(StructureShape structure, ObjectNode params) {
        Map<String, Document> members = new LinkedHashMap<>();
        for (Map.Entry<String, Node> entry : params.getStringMap().entrySet()) {
            MemberShape member = structure.getMember(entry.getKey()).orElseThrow(
                () -> new IllegalArgumentException(structure.getId() + " has no member " + entry.getKey()));
            if (isStreamingBlob(member) || entry.getValue().isNullNode()) {
                continue;
            }
            members.put(entry.getKey(), value(member, entry.getValue()));
        }
        return Document.of(members);
    }

    /**
     * A minimal valid value of {@code structure}: every required member (and every HTTP label, which a request
     * cannot be serialized without) with a fixed value of its type. Used as the input of response and error cases,
     * whose params describe only the output.
     */
    public ObjectNode minimalParams(StructureShape structure) {
        ObjectNode.Builder out = Node.objectNodeBuilder();
        for (MemberShape member : structure.getAllMembers().values()) {
            if ((member.hasTrait(RequiredTrait.class) || member.hasTrait(HttpLabelTrait.class))
                && !isStreamingBlob(member)) {
                out.withMember(member.getMemberName(), minimal(model.expectShape(member.getTarget()), 0));
            }
        }
        return out.build();
    }

    private Node minimal(Shape target, int depth) {
        return switch (target.getType()) {
            case STRING -> target.getTrait(EnumTrait.class)
                                 .map(e -> (Node) Node.from(e.getValues().get(0).getValue()))
                                 .orElse(Node.from("value"));
            case ENUM -> Node.from(target.asEnumShape().orElseThrow().getEnumValues().values().iterator().next());
            case INT_ENUM -> Node.from(target.asIntEnumShape().orElseThrow().getEnumValues().values()
                                             .iterator().next());
            case BLOB -> Node.from("value");
            case BOOLEAN -> Node.from(false);
            case BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BIG_INTEGER, BIG_DECIMAL, TIMESTAMP -> Node.from(0);
            case LIST, SET -> Node.arrayNode();
            case MAP -> Node.objectNode();
            case DOCUMENT -> Node.objectNode();
            case STRUCTURE -> depth > 8 ? Node.objectNode() : minimalParams(target.asStructureShape().orElseThrow());
            case UNION -> {
                MemberShape first = target.members().iterator().next();
                yield Node.objectNode().withMember(first.getMemberName(),
                                                   minimal(model.expectShape(first.getTarget()), depth + 1));
            }
            default -> throw new IllegalArgumentException("No minimal value for " + target.getType());
        };
    }

    /** Whether {@code member} is bound to an HTTP header, query string or label (where blobs are base64 text). */
    static boolean isHttpBound(MemberShape member) {
        return member.hasTrait(HttpHeaderTrait.class) || member.hasTrait(HttpQueryTrait.class)
               || member.hasTrait(HttpLabelTrait.class);
    }

    /** Whether {@code member} targets a streaming blob. */
    public boolean isStreamingBlob(MemberShape member) {
        Shape target = model.expectShape(member.getTarget());
        return target.isBlobShape() && target.hasTrait(StreamingTrait.class);
    }

    private Document value(MemberShape member, Node node) {
        return value(model.expectShape(member.getTarget()), node);
    }

    /** The document of {@code node} as a value of {@code shape}; a null node is {@code null}. */
    public Document value(Shape shape, Node node) {
        if (node.isNullNode()) {
            return null;
        }
        return switch (shape.getType()) {
            case STRUCTURE, UNION -> {
                Map<String, Document> members = new LinkedHashMap<>();
                for (Map.Entry<String, Node> entry : node.expectObjectNode().getStringMap().entrySet()) {
                    MemberShape member = shape.getMember(entry.getKey()).orElseThrow(
                        () -> new IllegalArgumentException(shape.getId() + " has no member " + entry.getKey()));
                    if (!entry.getValue().isNullNode()) {
                        members.put(entry.getKey(), value(member, entry.getValue()));
                    }
                }
                yield Document.of(members);
            }
            case LIST, SET -> {
                MemberShape item = ((CollectionShape) shape).getMember();
                List<Document> items = new ArrayList<>();
                node.expectArrayNode().getElements().forEach(e -> items.add(value(item, e)));
                yield Document.of(items);
            }
            case MAP -> {
                MemberShape valueMember = ((MapShape) shape).getValue();
                Map<String, Document> entries = new LinkedHashMap<>();
                node.expectObjectNode().getStringMap().forEach((k, v) -> entries.put(k, value(valueMember, v)));
                yield Document.of(entries);
            }
            case STRING, ENUM -> Document.of(node.expectStringNode().getValue());
            case BLOB -> Document.of(node.expectStringNode().getValue().getBytes(StandardCharsets.UTF_8));
            case BOOLEAN -> Document.of(node.expectBooleanNode().getValue());
            case BYTE -> Document.of(node.expectNumberNode().getValue().byteValue());
            case SHORT -> Document.of(node.expectNumberNode().getValue().shortValue());
            case INTEGER, INT_ENUM -> Document.of(node.expectNumberNode().getValue().intValue());
            case LONG -> Document.of(node.expectNumberNode().getValue().longValue());
            case FLOAT -> Document.of((float) floating(node));
            case DOUBLE -> Document.of(floating(node));
            case BIG_INTEGER -> Document.of(new BigInteger(decimal(node).toBigIntegerExact().toString()));
            case BIG_DECIMAL -> Document.of(decimal(node));
            case TIMESTAMP -> Document.of(timestamp(node));
            case DOCUMENT -> untyped(node);
            default -> throw new IllegalArgumentException("Unsupported params shape type " + shape.getType());
        };
    }

    private static double floating(Node node) {
        if (node.isStringNode()) {
            return switch (node.expectStringNode().getValue()) {
                case "NaN" -> Double.NaN;
                case "Infinity" -> Double.POSITIVE_INFINITY;
                case "-Infinity" -> Double.NEGATIVE_INFINITY;
                default -> throw new IllegalArgumentException("Not a float: " + node);
            };
        }
        return node.expectNumberNode().getValue().doubleValue();
    }

    private static BigDecimal decimal(Node node) {
        return new BigDecimal(node.expectNumberNode().getValue().toString());
    }

    private static Instant timestamp(Node node) {
        if (node.isStringNode()) {
            return Instant.parse(node.expectStringNode().getValue());
        }
        BigDecimal seconds = decimal(node);
        long whole = seconds.setScale(0, java.math.RoundingMode.FLOOR).longValueExact();
        int nanos = seconds.subtract(BigDecimal.valueOf(whole)).movePointRight(9).intValue();
        return Instant.ofEpochSecond(whole, nanos);
    }

    /** An untyped document value of a JSON node. */
    static Document untyped(Node node) {
        if (node.isNullNode()) {
            return null;
        }
        if (node.isObjectNode()) {
            Map<String, Document> out = new LinkedHashMap<>();
            node.expectObjectNode().getStringMap().forEach((k, v) -> out.put(k, untyped(v)));
            return Document.of(out);
        }
        if (node.isArrayNode()) {
            List<Document> out = new ArrayList<>();
            node.expectArrayNode().getElements().forEach(e -> out.add(untyped(e)));
            return Document.of(out);
        }
        if (node.isStringNode()) {
            return Document.of(node.expectStringNode().getValue());
        }
        if (node.isBooleanNode()) {
            return Document.of(node.expectBooleanNode().getValue());
        }
        Number number = node.expectNumberNode().getValue();
        if (number instanceof Double || number instanceof Float || number instanceof BigDecimal) {
            return Document.of(number.doubleValue());
        }
        return Document.of(number.longValue());
    }
}
