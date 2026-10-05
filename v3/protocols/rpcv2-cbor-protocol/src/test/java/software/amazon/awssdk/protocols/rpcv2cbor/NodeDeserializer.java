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

package software.amazon.awssdk.protocols.rpcv2cbor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import software.amazon.awssdk.protocols.core.serde.TimestampFormats;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;

/**
 * Test support: reads Smithy protocol-test {@code params} (a Smithy {@link Node}) as a schema-guided
 * {@link ShapeDeserializer}, so that test values can be built with any {@code ShapeBuilder}. Blobs are UTF-8 strings,
 * timestamps epoch-seconds numbers, and non-finite floats the strings {@code NaN}, {@code Infinity},
 * {@code -Infinity}, as in the protocol test models.
 */
final class NodeDeserializer implements ShapeDeserializer {
    private static final JsonCodec DOCUMENTS = JsonCodec.builder().build();

    private final Node node;

    NodeDeserializer(Node node) {
        this.node = node;
    }

    @Override
    public boolean readBoolean(Schema schema) {
        return node.expectBooleanNode().getValue();
    }

    @Override
    public ByteBuffer readBlob(Schema schema) {
        return ByteBuffer.wrap(node.expectStringNode().getValue().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public byte readByte(Schema schema) {
        return node.expectNumberNode().getValue().byteValue();
    }

    @Override
    public short readShort(Schema schema) {
        return node.expectNumberNode().getValue().shortValue();
    }

    @Override
    public int readInteger(Schema schema) {
        return node.expectNumberNode().getValue().intValue();
    }

    @Override
    public long readLong(Schema schema) {
        return node.expectNumberNode().getValue().longValue();
    }

    @Override
    public float readFloat(Schema schema) {
        return (float) readDouble(schema);
    }

    @Override
    public double readDouble(Schema schema) {
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

    @Override
    public BigInteger readBigInteger(Schema schema) {
        return new BigInteger(numberText());
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        return new BigDecimal(numberText());
    }

    private String numberText() {
        return node.isStringNode() ? node.expectStringNode().getValue() : node.expectNumberNode().getValue().toString();
    }

    @Override
    public String readString(Schema schema) {
        return node.expectStringNode().getValue();
    }

    @Override
    public Document readDocument() {
        return DOCUMENTS.createDeserializer(Node.printJson(node).getBytes(StandardCharsets.UTF_8)).readDocument();
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        if (node.isStringNode()) {
            return Instant.parse(node.expectStringNode().getValue());
        }
        return TimestampFormats.fromEpochSeconds(new BigDecimal(node.expectNumberNode().getValue().toString()));
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
        for (Map.Entry<StringNode, Node> entry : node.expectObjectNode().getMembers().entrySet()) {
            String name = entry.getKey().getValue();
            Schema member = schema.member(name);
            if (member == null) {
                consumer.unknownMember(state, name);
            } else if (!entry.getValue().isNullNode()) {
                consumer.accept(state, member, new NodeDeserializer(entry.getValue()));
            }
        }
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
        for (Node element : node.expectArrayNode().getElements()) {
            consumer.accept(state, new NodeDeserializer(element));
        }
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
        for (Map.Entry<StringNode, Node> entry : node.expectObjectNode().getMembers().entrySet()) {
            consumer.accept(state, entry.getKey().getValue(), new NodeDeserializer(entry.getValue()));
        }
    }

    @Override
    public boolean isNull() {
        return node.isNullNode();
    }
}
