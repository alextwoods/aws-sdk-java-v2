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

package software.amazon.awssdk.bridge.smithyjava.serde;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;

/**
 * Read-path counterpart to {@link SdkPojoSerializer}. The generated {@code BuilderImpl} (a
 * smithy-java {@link ShapeBuilder}) reads scalar members directly via {@code de.readX(member)}
 * into its typed fluent setters; this helper handles the two cases that can't be a one-liner at
 * the call site — list and map members — given the member's v2 {@link SdkField} (which carries
 * the element/value {@code SdkField}, including the {@code constructor()} needed to build nested
 * shapes).
 *
 * <p>List/map element <b>structs</b> (e.g. DynamoDB's {@code map<String,AttributeValue>}) recurse
 * through the element's generated {@code BuilderImpl} when it is itself a {@link ShapeBuilder}
 * (the fast, direct path); otherwise they fall back to the reflection-free bridge builder. Either
 * way there is no JVM reflection on the hot path.
 */
@SdkProtectedApi
public final class SdkPojoDeserializer {

    private SdkPojoDeserializer() {
    }

    /** Read a list member into a plain {@link ArrayList}. {@code field} is the member's v2 SdkField. */
    public static List<Object> readList(Schema member, SdkField<?> field, ShapeDeserializer de) {
        Schema element = member.listMember();
        SdkField<?> elementField = listElementField(field);
        List<Object> out = new ArrayList<>();
        de.readList(member, out, (state, elemDe) -> state.add(readElement(element, elementField, elemDe)));
        return out;
    }

    /** Read a string-keyed map member into a {@link LinkedHashMap}. */
    public static Map<String, Object> readStringMap(Schema member, SdkField<?> field, ShapeDeserializer de) {
        Schema value = member.mapValueMember();
        SdkField<?> valueField = mapValueField(field);
        Map<String, Object> out = new LinkedHashMap<>();
        de.readStringMap(member, out, (state, key, valDe) -> state.put(key, readElement(value, valueField, valDe)));
        return out;
    }

    /** Read a single element/value against its smithy {@link Schema} + v2 element {@link SdkField}. */
    public static Object readElement(Schema schema, SdkField<?> elementField, ShapeDeserializer de) {
        switch (schema.type()) {
            case STRING:
            case ENUM:
                return de.readString(schema);
            case INTEGER:
            case INT_ENUM:
                return de.readInteger(schema);
            case LONG:
                return de.readLong(schema);
            case SHORT:
                return de.readShort(schema);
            case BYTE:
                return de.readByte(schema);
            case FLOAT:
                return de.readFloat(schema);
            case DOUBLE:
                return de.readDouble(schema);
            case BIG_DECIMAL:
                return de.readBigDecimal(schema);
            case BOOLEAN:
                return de.readBoolean(schema);
            case TIMESTAMP:
                return de.readTimestamp(schema);
            case BLOB:
                return SdkBytes.fromByteBuffer(de.readBlob(schema));
            case LIST:
                return readList(schema, elementField, de);
            case MAP:
                return readStringMap(schema, elementField, de);
            case STRUCTURE:
            case UNION:
                return readStruct(schema, elementField, de);
            default:
                return de.readString(schema);
        }
    }

    private static Object readStruct(Schema schema, SdkField<?> elementField, ShapeDeserializer de) {
        SdkPojo builder = elementField != null ? nestedBuilder(elementField) : null;
        if (builder instanceof ShapeBuilder) {
            // Fast path: the nested shape's generated BuilderImpl is a smithy ShapeBuilder — let it
            // read itself (its own switch(memberIndex) + direct setters), then build to the pojo.
            @SuppressWarnings("unchecked")
            ShapeBuilder<?> sb = (ShapeBuilder<?>) builder;
            return sb.deserializeMember(de, schema).build();
        }
        if (builder != null) {
            // Fallback: drive the reflection-free bridge builder for a not-yet-regenerated shape.
            BridgeOutputBuilder.of(schema, builder).deserialize(de);
            return buildIfPossible(builder);
        }
        return null;
    }

    private static SdkField<?> listElementField(SdkField<?> listField) {
        if (listField == null) {
            return null;
        }
        ListTrait t = listField.getTrait(ListTrait.class);
        return t != null ? t.memberFieldInfo() : null;
    }

    private static SdkField<?> mapValueField(SdkField<?> mapField) {
        if (mapField == null) {
            return null;
        }
        MapTrait t = mapField.getTrait(MapTrait.class);
        return t != null ? t.valueFieldInfo() : null;
    }

    private static SdkPojo nestedBuilder(SdkField<?> field) {
        Supplier<SdkPojo> ctor = field.constructor();
        return ctor != null ? ctor.get() : null;
    }

    private static Object buildIfPossible(SdkPojo builder) {
        if (builder instanceof software.amazon.awssdk.utils.builder.Buildable buildable) {
            return buildable.build();
        }
        return builder;
    }

    /** Marker re-exports so generated code can null/auto-construct check without extra imports. */
    public static boolean isAutoConstruct(Object value) {
        return value instanceof SdkAutoConstructList || value instanceof SdkAutoConstructMap;
    }
}
