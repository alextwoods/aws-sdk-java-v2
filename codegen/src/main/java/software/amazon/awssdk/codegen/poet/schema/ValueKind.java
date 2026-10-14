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

package software.amazon.awssdk.codegen.poet.schema;

import com.squareup.javapoet.CodeBlock;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.schema.SchemaRuntimeType;

/**
 * How generated schema serde code writes and reads a value of a model class member, list element or map value, by the
 * member's Java type: {@code ShapeSerializer.write<Name>} and {@code ShapeDeserializer.read<Name>}.
 */
@SdkInternalApi
public enum ValueKind {
    STRING("String"),
    BOOLEAN("Boolean"),
    BYTE("Byte"),
    SHORT("Short"),
    INTEGER("Integer"),
    LONG("Long"),
    FLOAT("Float"),
    DOUBLE("Double"),
    BIG_DECIMAL("BigDecimal"),
    BLOB("Blob"),
    TIMESTAMP("Timestamp"),
    STRUCTURE("Struct"),
    LIST("List"),
    MAP("Map");

    private final String method;

    ValueKind(String method) {
        this.method = method;
    }

    /**
     * @param member a member, list element or map value
     * @return its kind, or null if schema serde does not support its type (documents, {@code BigInteger}, streams, maps
     *     with non-string keys and collections of those)
     */
    public static ValueKind of(MemberModel member) {
        if (member.isList()) {
            return of(member.getListModel().getListMemberModel()) == null ? null : LIST;
        }
        if (member.isMap()) {
            MemberModel key = member.getMapModel().getKeyModel();
            return of(key) != STRING || of(member.getMapModel().getValueModel()) == null ? null : MAP;
        }
        if (!member.isSimple()) {
            return STRUCTURE;
        }
        String type = member.getVariable().getVariableType();
        switch (type.substring(type.lastIndexOf('.') + 1)) {
            case "String":
                return STRING;
            case "Boolean":
                return BOOLEAN;
            case "Byte":
                return BYTE;
            case "Short":
                return SHORT;
            case "Integer":
                return INTEGER;
            case "Long":
                return LONG;
            case "Float":
                return FLOAT;
            case "Double":
                return DOUBLE;
            case "BigDecimal":
                return BIG_DECIMAL;
            case "SdkBytes":
                return BLOB;
            case "Instant":
                return TIMESTAMP;
            default:
                return null;
        }
    }

    /**
     * Renders {@code serializer.write<Kind>(schema, value)} for a value that is not null. Lists and maps need their
     * {@code <Svc>SharedSerde} writer ({@code writer}); other kinds ignore it.
     */
    public CodeBlock write(String serializer, CodeBlock schema, CodeBlock value, CodeBlock writer) {
        switch (this) {
            case BLOB:
                return CodeBlock.of("$N.writeBlob($L, $L.asByteBuffer())", serializer, schema, value);
            case LIST:
            case MAP:
                return CodeBlock.of("$N.write$L($L, $L, $L.size(), $L)", serializer, method, schema, value, value, writer);
            default:
                return CodeBlock.of("$N.write$L($L, $L)", serializer, method, schema, value);
        }
    }

    /**
     * Renders the expression that reads a value. Structures need their class ({@code type}); lists and maps their
     * {@code <Svc>SharedSerde} class ({@code reader}); other kinds ignore both.
     */
    public CodeBlock read(String deserializer, CodeBlock schema, Object type, CodeBlock reader) {
        switch (this) {
            case BLOB:
                return CodeBlock.of("$T.toSdkBytes($N.readBlob($L))", SchemaRuntimeType.SCHEMA_SERDE_SUPPORT,
                                    deserializer, schema);
            case STRUCTURE:
                return CodeBlock.of("$T.newShapeBuilder().deserializeMember($N, $L).build()", type, deserializer, schema);
            case LIST:
            case MAP:
                return CodeBlock.of("$L.read($L, $N)", reader, schema, deserializer);
            default:
                return CodeBlock.of("$N.read$L($L)", deserializer, method, schema);
        }
    }
}
