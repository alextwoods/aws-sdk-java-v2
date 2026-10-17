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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;

/**
 * Reads and discards one value, so a codec whose struct reader expects the consumer to read every member value can
 * continue past a member the HTTP binding does not take from the body (for example a body key that names a header
 * member). SDK addition: the SDK {@link ShapeDeserializer} has no skip method.
 */
@SdkInternalApi
final class ValueSkipper {
    private static final ShapeDeserializer.StructMemberConsumer<Object> SKIP_MEMBER =
        new ShapeDeserializer.StructMemberConsumer<Object>() {
            @Override
            public void accept(Object state, Schema memberSchema, ShapeDeserializer memberDeserializer) {
                skip(memberSchema, memberDeserializer);
            }
        };

    private ValueSkipper() {
    }

    static void skip(Schema schema, ShapeDeserializer deserializer) {
        if (deserializer.isNull()) {
            deserializer.readNull();
            return;
        }
        switch (schema.type()) {
            case BOOLEAN:
                deserializer.readBoolean(schema);
                break;
            case BYTE:
                deserializer.readByte(schema);
                break;
            case SHORT:
                deserializer.readShort(schema);
                break;
            case INTEGER:
            case INT_ENUM:
                deserializer.readInteger(schema);
                break;
            case LONG:
                deserializer.readLong(schema);
                break;
            case FLOAT:
                deserializer.readFloat(schema);
                break;
            case DOUBLE:
                deserializer.readDouble(schema);
                break;
            case BIG_INTEGER:
                deserializer.readBigInteger(schema);
                break;
            case BIG_DECIMAL:
                deserializer.readBigDecimal(schema);
                break;
            case BLOB:
                deserializer.readBlob(schema);
                break;
            case TIMESTAMP:
                deserializer.readTimestamp(schema);
                break;
            case DOCUMENT:
                deserializer.readDocument();
                break;
            case LIST:
                deserializer.readList(schema, null, (state, element) -> skip(schema.listMember(), element));
                break;
            case MAP:
                deserializer.readStringMap(schema, null, (state, key, value) -> skip(schema.mapValueMember(), value));
                break;
            case STRUCTURE:
            case UNION:
                deserializer.readStruct(schema, null, SKIP_MEMBER);
                break;
            case STRING:
            case ENUM:
            default:
                deserializer.readString(schema);
                break;
        }
    }
}
