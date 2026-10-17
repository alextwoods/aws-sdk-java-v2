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

import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.SpecificShapeSerializer;

/**
 * Writes the entries of an {@code @httpPrefixHeaders} or {@code @httpQueryParams} map as (name, value) pairs. String
 * values give one pair; a list of strings (allowed for {@code @httpQueryParams}) gives one pair per element. Null
 * values are dropped.
 *
 * <p>Merges smithy-java {@code HttpPrefixHeadersSerializer} and {@code HttpQueryParamsSerializer}, whose map entry
 * serializers differ only in the consumer.
 */
@SdkInternalApi
final class StringMapEntrySerializer implements MapSerializer {
    private final BiConsumer<String, String> pairConsumer;

    StringMapEntrySerializer(BiConsumer<String, String> pairConsumer) {
        this.pairConsumer = pairConsumer;
    }

    @Override
    public <T> void writeEntry(Schema keySchema, String key, T state, BiConsumer<T, ShapeSerializer> valueSerializer) {
        valueSerializer.accept(state, new ValueSerializer(key, true));
    }

    private final class ValueSerializer extends SpecificShapeSerializer {
        private final String key;
        private final boolean allowList;

        ValueSerializer(String key, boolean allowList) {
            this.key = key;
            this.allowList = allowList;
        }

        @Override
        public void writeString(Schema schema, String value) {
            pairConsumer.accept(key, value);
        }

        @Override
        public <L> void writeList(Schema schema, L listState, int size, BiConsumer<L, ShapeSerializer> consumer) {
            if (!allowList) {
                super.writeList(schema, listState, size, consumer);
                return;
            }
            consumer.accept(listState, new ValueSerializer(key, false));
        }

        @Override
        public void writeNull(Schema schema) {
            // Null map values and sparse null elements are dropped.
        }
    }
}
