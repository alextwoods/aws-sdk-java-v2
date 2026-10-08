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

package software.amazon.awssdk.dynamic.client;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.DefaultTrait;

/**
 * Applies modeled {@code @default} values to schema-guided documents the way generated types do (Requirements 14.16,
 * 19.6).
 *
 * <p>A generated builder starts each {@code @default} member at its modeled default, so an unset member is serialized
 * with that value and a member absent from a response reads as it. A Smithy Java {@code StructDocument} has no such
 * initialization: an unset member is simply absent. {@link #fill} returns a document with every absent
 * {@code @default} member of every structure in it, recursively through structure and union members, list elements
 * and map values, set to its default; the dynamic client fills its request documents before serialization and its
 * output documents after deserialization. Streaming members and members explicitly present are left unchanged, and a
 * document that needs no default is returned as is.
 *
 * <p>Stateless and thread safe.
 */
final class DynamicDefaults {
    private DynamicDefaults() {
    }

    /** {@code document} with every absent {@code @default} member filled, or {@code document} if none is absent. */
    static StructDocument fill(StructDocument document, ShapeId service) {
        Document filled = fillValue(document.schema(), document, service);
        return filled == document ? document : (StructDocument) filled;
    }

    /** A builder that {@linkplain #fill fills} what {@code delegate} builds. */
    static ShapeBuilder<StructDocument> filling(ShapeBuilder<StructDocument> delegate, ShapeId service) {
        return new FillingBuilder(delegate, service);
    }

    /** {@code value} of {@code schema} filled, or {@code value} itself if nothing in it is absent. */
    private static Document fillValue(Schema schema, Document value, ShapeId service) {
        if (value == null || schema.hasTrait(TraitKey.STREAMING_TRAIT)) {
            return value;
        }
        return switch (schema.type()) {
            case STRUCTURE, UNION -> fillStruct(schema.isMember() ? schema.memberTarget() : schema, value, service);
            case LIST, SET -> fillList(schema.listMember(), value, service);
            case MAP -> fillMap(schema.mapValueMember(), value, service);
            default -> value;
        };
    }

    private static Document fillStruct(Schema schema, Document value, ShapeId service) {
        List<Schema> members = schema.members();
        Document[] values = null;
        for (int i = 0; i < members.size(); i++) {
            Schema member = members.get(i);
            Document current = value.getMember(member.memberName());
            Document next = current == null ? defaultValue(member) : fillValue(member, current, service);
            if (next != current) {
                if (values == null) {
                    values = new Document[members.size()];
                }
                values[i] = next;
            }
        }
        if (values == null) {
            return value;
        }
        ShapeBuilder<StructDocument> builder = SchemaConverter.createDocumentBuilder(schema, service);
        for (int i = 0; i < members.size(); i++) {
            Schema member = members.get(i);
            Document next = values[i] != null ? values[i] : value.getMember(member.memberName());
            if (next != null) {
                builder.setMemberValue(member, next);
            }
        }
        return builder.build();
    }

    private static Document fillList(Schema element, Document value, ShapeId service) {
        List<Document> elements = value.asList();
        List<Document> filled = null;
        for (int i = 0; i < elements.size(); i++) {
            Document current = elements.get(i);
            Document next = fillValue(element, current, service);
            if (next != current && filled == null) {
                filled = new ArrayList<>(elements);
            }
            if (filled != null) {
                filled.set(i, next);
            }
        }
        return filled == null ? value : Document.of(filled);
    }

    private static Document fillMap(Schema valueMember, Document value, ShapeId service) {
        Map<String, Document> entries = value.asStringMap();
        Map<String, Document> filled = null;
        for (Map.Entry<String, Document> entry : entries.entrySet()) {
            Document next = fillValue(valueMember, entry.getValue(), service);
            if (next != entry.getValue()) {
                if (filled == null) {
                    filled = new LinkedHashMap<>(entries);
                }
                filled.put(entry.getKey(), next);
            }
        }
        return filled == null ? value : Document.of(filled);
    }

    /** The modeled default of {@code member} as a document, or {@code null} without one (or with a null default). */
    private static Document defaultValue(Schema member) {
        DefaultTrait trait = member.getTrait(TraitKey.DEFAULT_TRAIT);
        if (trait == null || trait.toNode().isNullNode() || member.hasTrait(TraitKey.STREAMING_TRAIT)) {
            return null;
        }
        Node node = trait.toNode();
        return switch (member.type()) {
            case BOOLEAN -> Document.of(node.expectBooleanNode().getValue());
            case BYTE -> Document.of(number(node).byteValue());
            case SHORT -> Document.of(number(node).shortValue());
            case INTEGER, INT_ENUM -> Document.of(number(node).intValue());
            case LONG -> Document.of(number(node).longValue());
            case FLOAT -> Document.of((float) floating(node));
            case DOUBLE -> Document.of(floating(node));
            case BIG_INTEGER -> Document.of(new BigDecimal(number(node).toString()).toBigIntegerExact());
            case BIG_DECIMAL -> Document.of(new BigDecimal(number(node).toString()));
            case STRING, ENUM -> Document.of(node.expectStringNode().getValue());
            case BLOB -> Document.of(Base64.getDecoder().decode(node.expectStringNode().getValue()));
            case TIMESTAMP -> Document.of(timestamp(node));
            case LIST, SET -> Document.of(List.of());
            case MAP -> Document.of(Map.of());
            case DOCUMENT -> document(node);
            default -> null;
        };
    }

    private static Number number(Node node) {
        return node.expectNumberNode().getValue();
    }

    private static double floating(Node node) {
        if (node instanceof StringNode string) {
            return switch (string.getValue()) {
                case "NaN" -> Double.NaN;
                case "Infinity" -> Double.POSITIVE_INFINITY;
                case "-Infinity" -> Double.NEGATIVE_INFINITY;
                default -> throw new IllegalArgumentException("Invalid floating-point default " + string.getValue());
            };
        }
        return number(node).doubleValue();
    }

    private static Instant timestamp(Node node) {
        if (node instanceof StringNode string) {
            return Instant.parse(string.getValue());
        }
        BigDecimal seconds = new BigDecimal(number(node).toString());
        return Instant.ofEpochSecond(seconds.longValue(),
                                     seconds.remainder(BigDecimal.ONE).movePointRight(9).intValue());
    }

    private static Document document(Node node) {
        if (node.isNullNode()) {
            return null;
        }
        if (node.isBooleanNode()) {
            return Document.of(node.expectBooleanNode().getValue());
        }
        if (node.isNumberNode()) {
            Number value = number(node);
            return value instanceof BigInteger || value instanceof Long || value instanceof Integer
                   ? Document.of(value.longValue()) : Document.of(value.doubleValue());
        }
        if (node.isStringNode()) {
            return Document.of(node.expectStringNode().getValue());
        }
        if (node.isArrayNode()) {
            List<Document> elements = new ArrayList<>();
            node.expectArrayNode().getElements().forEach(element -> elements.add(document(element)));
            return Document.of(elements);
        }
        Map<String, Document> entries = new LinkedHashMap<>();
        node.expectObjectNode().getStringMap().forEach((key, value) -> entries.put(key, document(value)));
        return Document.of(entries);
    }

    /** Delegates every build step and fills the built document. */
    private static final class FillingBuilder implements ShapeBuilder<StructDocument> {
        private final ShapeBuilder<StructDocument> delegate;
        private final ShapeId service;

        private FillingBuilder(ShapeBuilder<StructDocument> delegate, ShapeId service) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.service = Objects.requireNonNull(service, "service");
        }

        @Override
        public StructDocument build() {
            return fill(delegate.build(), service);
        }

        @Override
        public ShapeBuilder<StructDocument> deserialize(ShapeDeserializer deserializer) {
            delegate.deserialize(deserializer);
            return this;
        }

        @Override
        public ShapeBuilder<StructDocument> deserializeMember(ShapeDeserializer deserializer, Schema member) {
            delegate.deserializeMember(deserializer, member);
            return this;
        }

        @Override
        public void setMemberValue(Schema member, Object value) {
            delegate.setMemberValue(member, value);
        }

        @Override
        public ShapeBuilder<StructDocument> errorCorrection() {
            delegate.errorCorrection();
            return this;
        }

        @Override
        public Schema schema() {
            return delegate.schema();
        }
    }
}
