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

package software.amazon.awssdk.schema.serde;

import java.util.function.BiConsumer;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;

/**
 * Port of smithy-java {@code software.amazon.smithy.java.core.TestHelper}: a structure whose members are written by
 * a callback.
 */
final class TestHelper {

    private TestHelper() {
    }

    static SerializableStruct create(Schema schema, BiConsumer<Schema, ShapeSerializer> memberWriter) {
        return new SerializableStruct() {
            @Override
            public Schema sdkSchema() {
                return schema;
            }

            @Override
            public void serializeMembers(ShapeSerializer serializer) {
                memberWriter.accept(schema, serializer);
            }

            @Override
            public <T> T getMemberValue(Schema member) {
                throw new UnsupportedOperationException("Getting member not supported: " + member);
            }
        };
    }
}
