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

package software.amazon.awssdk.smithy.runtime.document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableShape;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * Schema-driven conversion between modeled values and Smithy Java {@link Document}s (Requirement 6).
 *
 * <ul>
 *     <li>{@link #of(SerializableStruct)} converts a generated value to a {@link ModeledDocument} that keeps every
 *         value's exact kind and shape ID, member presence, blob bytes, timestamps and collection order
 *         (Requirements 6.1, 6.2 and 6.4).</li>
 *     <li>{@link #toShape} and {@link #toShapeStrict} construct a generated value from any document through its
 *         builder, and {@link #coerce} converts any document into a {@link ModeledDocument} of a schema. Both read the
 *         document through its own deserializer, so protocol-decoded documents keep their protocol context (field
 *         names, blob and timestamp representations, discriminator namespace; Requirement 6.3), and both check the
 *         document against the schema: incompatible kinds, numeric range and integrality, invalid representations
 *         and conflicting discriminators fail with a deterministic {@link DocumentCoercionException} (Requirements
 *         6.10 and 6.14).</li>
 *     <li>{@link #equivalent} compares two documents of any origin through a schema.</li>
 * </ul>
 *
 * <p>Nothing here uses reflection, class-name lookup or a Smithy model.
 */
public final class SchemaDocuments {

    private static final String ROOT = "$";

    private SchemaDocuments() {
    }

    /**
     * Converts a structure or union value to a modeled document.
     *
     * <p>A {@link ModeledDocument} is returned unchanged; another document that is also a {@code SerializableStruct}
     * (such as Smithy Java's typed structure documents) is {@linkplain #coerce coerced} to its schema.
     *
     * @throws software.amazon.smithy.java.core.serde.SerializationException if the value has a streaming member,
     *         which is not a finite value
     */
    public static ModeledDocument of(SerializableStruct value) {
        Objects.requireNonNull(value, "value");
        if (value instanceof ModeledDocument modeled) {
            return modeled;
        }
        if (value instanceof Document document) {
            return (ModeledDocument) coerce(document, value.schema());
        }
        ModeledDocumentParser parser = new ModeledDocumentParser();
        parser.writeStruct(value.schema(), value);
        return (ModeledDocument) parser.result();
    }

    /**
     * Converts {@code document} to a {@link ModeledDocument} of {@code schema}, checking every value against the
     * schema. The result is a {@code ModeledDocument}, except that a schema of a {@code smithy.api#Document} shape
     * returns {@code document} itself.
     *
     * @throws DocumentCoercionException if the document cannot represent a value of {@code schema}
     */
    public static Document coerce(Document document, Schema schema) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(schema, "schema");
        return read(new StrictDocumentDeserializer(document, ROOT), schema);
    }

    /**
     * Constructs the value of {@code builder}'s schema from {@code document} and builds it with error correction,
     * like Smithy Java's {@code Document.asShape} (for non-authoritative consumers such as clients).
     *
     * @throws DocumentCoercionException if the document cannot represent a value of the builder's schema, including
     *         when its discriminator names another shape
     */
    public static <T extends SerializableShape> T toShape(Document document, ShapeBuilder<T> builder) {
        deserializeInto(document, builder);
        return builder.errorCorrection().build();
    }

    /**
     * Like {@link #toShape}, but builds without error correction, so a generated builder rejects missing required
     * members.
     */
    public static <T extends SerializableShape> T toShapeStrict(Document document, ShapeBuilder<T> builder) {
        deserializeInto(document, builder);
        return builder.build();
    }

    /** Populates {@code builder} from {@code document}, checking it against the builder's schema. */
    public static <T extends SerializableShape> ShapeBuilder<T> deserializeInto(Document document,
                                                                               ShapeBuilder<T> builder) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(builder, "builder");
        builder.deserialize(deserializer(document));
        return builder;
    }

    /**
     * A schema-checked deserializer of {@code document} that keeps the document's protocol context; see
     * {@link #coerce} for the checks.
     */
    public static ShapeDeserializer deserializer(Document document) {
        return new StrictDocumentDeserializer(Objects.requireNonNull(document, "document"), ROOT);
    }

    /**
     * Whether {@code left} and {@code right} represent the same value of {@code schema}, whatever their document
     * implementations and protocol representations. Both documents are {@linkplain #coerce coerced} to the schema
     * and compared with {@link ModeledDocument#equals}.
     *
     * @throws DocumentCoercionException if either document cannot represent a value of {@code schema}
     */
    public static boolean equivalent(Schema schema, Document left, Document right) {
        Objects.requireNonNull(schema, "schema");
        if (left == null || right == null) {
            return left == right;
        }
        return coerce(left, schema).equals(coerce(right, schema));
    }

    /** Reads one value of {@code schema} (a member schema for members, elements and entries). */
    private static Document read(ShapeDeserializer deserializer, Schema schema) {
        return switch (schema.type()) {
            case BOOLEAN -> new ModeledDocument.Scalar(schema, deserializer.readBoolean(schema));
            case BYTE -> new ModeledDocument.Scalar(schema, deserializer.readByte(schema));
            case SHORT -> new ModeledDocument.Scalar(schema, deserializer.readShort(schema));
            case INTEGER, INT_ENUM -> new ModeledDocument.Scalar(schema, deserializer.readInteger(schema));
            case LONG -> new ModeledDocument.Scalar(schema, deserializer.readLong(schema));
            case FLOAT -> new ModeledDocument.Scalar(schema, deserializer.readFloat(schema));
            case DOUBLE -> new ModeledDocument.Scalar(schema, deserializer.readDouble(schema));
            case BIG_INTEGER -> new ModeledDocument.Scalar(schema, deserializer.readBigInteger(schema));
            case BIG_DECIMAL -> new ModeledDocument.Scalar(schema, deserializer.readBigDecimal(schema));
            case STRING, ENUM -> new ModeledDocument.Scalar(schema, deserializer.readString(schema));
            case BLOB -> ModeledDocument.Scalar.blob(schema, deserializer.readBlob(schema));
            case TIMESTAMP -> new ModeledDocument.Scalar(schema, deserializer.readTimestamp(schema));
            case DOCUMENT -> deserializer.readDocument();
            case LIST, SET -> readList(deserializer, schema);
            case MAP -> readMap(deserializer, schema);
            case STRUCTURE, UNION -> readAggregate(deserializer, schema);
            default -> throw new IllegalArgumentException("A " + schema.type() + " schema has no document values: "
                                                          + schema.id());
        };
    }

    private static Document readNullable(ShapeDeserializer deserializer, Schema schema) {
        if (deserializer.isNull()) {
            return deserializer.readNull();
        }
        return read(deserializer, schema);
    }

    private static Document readList(ShapeDeserializer deserializer, Schema schema) {
        Schema member = schema.listMember();
        List<Document> elements = new ArrayList<>();
        deserializer.readList(schema, elements, (list, element) -> list.add(readNullable(element, member)));
        return new ModeledDocument.Sequence(schema, elements);
    }

    private static Document readMap(ShapeDeserializer deserializer, Schema schema) {
        Schema value = schema.mapValueMember();
        Map<String, Document> entries = new LinkedHashMap<>();
        deserializer.readStringMap(schema, entries, (map, key, entry) -> map.put(key, readNullable(entry, value)));
        return new ModeledDocument.StringMap(schema, entries);
    }

    private static Document readAggregate(ShapeDeserializer deserializer, Schema schema) {
        Map<String, Document> read = new HashMap<>();
        deserializer.readStruct(schema, read, (members, member, value) -> {
            if (!value.isNull()) {
                members.put(member.memberName(), read(value, member));
            }
        });
        // Present members in schema member order, whatever order the document iterates them in.
        Map<String, Document> ordered = new LinkedHashMap<>();
        for (Schema member : schema.members()) {
            Document value = read.get(member.memberName());
            if (value != null) {
                ordered.put(member.memberName(), value);
            }
        }
        return new ModeledDocument.Aggregate(schema, ordered);
    }
}
