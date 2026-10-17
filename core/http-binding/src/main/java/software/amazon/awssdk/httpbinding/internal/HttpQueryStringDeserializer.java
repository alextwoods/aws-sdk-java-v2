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

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * Reads the values of one query parameter: the first value for a scalar member, every value for a list member.
 * Timestamps default to {@code date-time}. Port of smithy-java {@code HttpQueryStringDeserializer}.
 */
@SdkInternalApi
final class HttpQueryStringDeserializer extends BasicStringValueDeserializer {
    private final List<String> values;
    private final Schema owner;

    HttpQueryStringDeserializer(List<String> values, Schema owner) {
        super(values.get(0), "HTTP query-string bindings", owner);
        this.values = values;
        this.owner = owner;
    }

    @Override
    TimestampFormatTrait.Format defaultTimestampFormat() {
        return TimestampFormatTrait.Format.DATE_TIME;
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
        for (String value : values) {
            consumer.accept(state, new HttpQueryStringDeserializer(Collections.singletonList(value), owner));
        }
    }

    @Override
    public int containerSize() {
        return values.size();
    }
}
