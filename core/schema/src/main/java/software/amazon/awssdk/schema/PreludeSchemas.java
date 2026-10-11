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

package software.amazon.awssdk.schema;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.DefaultTrait;
import software.amazon.awssdk.schema.traits.UnitTypeTrait;

// CHECKSTYLE:OFF - Keeps the smithy-java (and design A.4) name: the class holds the prelude schemas.
/**
 * Schemas for the shapes of the Smithy prelude ({@code smithy.api}). Generated schemas reference these instead of
 * defining their own copies.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.PreludeSchemas}.
 */
@SdkPublicApi
public final class PreludeSchemas {
    // CHECKSTYLE:ON
    private static final String PRELUDE = "smithy.api";

    /** {@code smithy.api#Blob}. */
    public static final Schema BLOB = Schema.createBlob(ShapeId.fromParts(PRELUDE, "Blob"));
    /** {@code smithy.api#Boolean}. */
    public static final Schema BOOLEAN = Schema.createBoolean(ShapeId.fromParts(PRELUDE, "Boolean"));
    /** {@code smithy.api#String}. */
    public static final Schema STRING = Schema.createString(ShapeId.fromParts(PRELUDE, "String"));
    /** {@code smithy.api#Timestamp}. */
    public static final Schema TIMESTAMP = Schema.createTimestamp(ShapeId.fromParts(PRELUDE, "Timestamp"));
    /** {@code smithy.api#Byte}. */
    public static final Schema BYTE = Schema.createByte(ShapeId.fromParts(PRELUDE, "Byte"));
    /** {@code smithy.api#Short}. */
    public static final Schema SHORT = Schema.createShort(ShapeId.fromParts(PRELUDE, "Short"));
    /** {@code smithy.api#Integer}. */
    public static final Schema INTEGER = Schema.createInteger(ShapeId.fromParts(PRELUDE, "Integer"));
    /** {@code smithy.api#Long}. */
    public static final Schema LONG = Schema.createLong(ShapeId.fromParts(PRELUDE, "Long"));
    /** {@code smithy.api#Float}. */
    public static final Schema FLOAT = Schema.createFloat(ShapeId.fromParts(PRELUDE, "Float"));
    /** {@code smithy.api#Double}. */
    public static final Schema DOUBLE = Schema.createDouble(ShapeId.fromParts(PRELUDE, "Double"));
    /** {@code smithy.api#BigInteger}. */
    public static final Schema BIG_INTEGER = Schema.createBigInteger(ShapeId.fromParts(PRELUDE, "BigInteger"));
    /** {@code smithy.api#BigDecimal}. */
    public static final Schema BIG_DECIMAL = Schema.createBigDecimal(ShapeId.fromParts(PRELUDE, "BigDecimal"));
    /** {@code smithy.api#Document}. */
    public static final Schema DOCUMENT = Schema.createDocument(ShapeId.fromParts(PRELUDE, "Document"));

    /** {@code smithy.api#PrimitiveBoolean}, with a default of {@code false}. */
    public static final Schema PRIMITIVE_BOOLEAN = Schema.createBoolean(
        ShapeId.fromParts(PRELUDE, "PrimitiveBoolean"), new DefaultTrait(Document.of(false)));
    /** {@code smithy.api#PrimitiveByte}, with a default of 0. */
    public static final Schema PRIMITIVE_BYTE = Schema.createByte(
        ShapeId.fromParts(PRELUDE, "PrimitiveByte"), new DefaultTrait(Document.of((byte) 0)));
    /** {@code smithy.api#PrimitiveShort}, with a default of 0. */
    public static final Schema PRIMITIVE_SHORT = Schema.createShort(
        ShapeId.fromParts(PRELUDE, "PrimitiveShort"), new DefaultTrait(Document.of((short) 0)));
    /** {@code smithy.api#PrimitiveInteger}, with a default of 0. */
    public static final Schema PRIMITIVE_INTEGER = Schema.createInteger(
        ShapeId.fromParts(PRELUDE, "PrimitiveInteger"), new DefaultTrait(Document.of(0)));
    /** {@code smithy.api#PrimitiveLong}, with a default of 0. */
    public static final Schema PRIMITIVE_LONG = Schema.createLong(
        ShapeId.fromParts(PRELUDE, "PrimitiveLong"), new DefaultTrait(Document.of(0L)));
    /** {@code smithy.api#PrimitiveFloat}, with a default of 0. */
    public static final Schema PRIMITIVE_FLOAT = Schema.createFloat(
        ShapeId.fromParts(PRELUDE, "PrimitiveFloat"), new DefaultTrait(Document.of(0f)));
    /** {@code smithy.api#PrimitiveDouble}, with a default of 0. */
    public static final Schema PRIMITIVE_DOUBLE = Schema.createDouble(
        ShapeId.fromParts(PRELUDE, "PrimitiveDouble"), new DefaultTrait(Document.of(0.0)));

    /** {@code smithy.api#Unit}: a structure with no members and the {@code unitType} trait. */
    public static final Schema UNIT = Schema.structureBuilder(ShapeId.fromParts(PRELUDE, "Unit"), UnitTypeTrait.INSTANCE)
                                            .build();

    private PreludeSchemas() {
    }

    /**
     * Returns the prelude schema that best matches a shape type. Numeric and boolean types return the nullable schema
     * (for example {@link #INTEGER}, not {@link #PRIMITIVE_INTEGER}); enums return {@link #STRING} and int enums
     * {@link #INTEGER}. Types with no prelude schema (aggregates, members, services) return {@link #DOCUMENT}.
     *
     * @param type the shape type
     * @return the prelude schema
     */
    public static Schema getSchemaForType(ShapeType type) {
        switch (type) {
            case BOOLEAN:
                return BOOLEAN;
            case BYTE:
                return BYTE;
            case SHORT:
                return SHORT;
            case INTEGER:
            case INT_ENUM:
                return INTEGER;
            case LONG:
                return LONG;
            case FLOAT:
                return FLOAT;
            case DOUBLE:
                return DOUBLE;
            case BIG_INTEGER:
                return BIG_INTEGER;
            case BIG_DECIMAL:
                return BIG_DECIMAL;
            case STRING:
            case ENUM:
                return STRING;
            case BLOB:
                return BLOB;
            case TIMESTAMP:
                return TIMESTAMP;
            default:
                return DOCUMENT;
        }
    }
}
