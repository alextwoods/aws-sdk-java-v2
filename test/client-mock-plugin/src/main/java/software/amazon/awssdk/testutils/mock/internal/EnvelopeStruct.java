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

package software.amazon.awssdk.testutils.mock.internal;

import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * A structure value for a protocol envelope schema, such as awsQuery's {@code <OpResponse><OpResult>}, whose members
 * are structures or strings given in member order. A null value is not written.
 */
@SdkInternalApi
final class EnvelopeStruct implements SerializableStruct {
    private final Schema schema;
    private final Object[] values;

    EnvelopeStruct(Schema schema, Object... values) {
        this.schema = schema;
        this.values = values.clone();
    }

    @Override
    public Schema sdkSchema() {
        return schema;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        List<Schema> members = schema.members();
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value instanceof SerializableStruct) {
                serializer.writeStruct(members.get(i), (SerializableStruct) value);
            } else if (value != null) {
                serializer.writeString(members.get(i), (String) value);
            }
        }
    }

    @Override
    public <T> T getMemberValue(Schema member) {
        List<Schema> members = schema.members();
        for (int i = 0; i < values.length; i++) {
            if (members.get(i).memberName().equals(member.memberName())) {
                @SuppressWarnings("unchecked")
                T value = (T) values[i];
                return value;
            }
        }
        return null;
    }
}
