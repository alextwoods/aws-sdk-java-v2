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

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.SpecificShapeDeserializer;

/**
 * Reads every query parameter into an {@code @httpQueryParams} map. Port of smithy-java
 * {@code HttpQueryParamsDeserializer}.
 */
@SdkInternalApi
final class HttpQueryParamsDeserializer extends SpecificShapeDeserializer {
    private final Map<String, List<String>> queryParams;

    HttpQueryParamsDeserializer(Map<String, List<String>> queryParams) {
        this.queryParams = queryParams;
    }

    @Override
    protected RuntimeException throwForInvalidState(Schema schema) {
        return new SerializationException(schema.id() + " is not a valid @httpQueryParams target");
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
        for (Map.Entry<String, List<String>> e : queryParams.entrySet()) {
            if (!e.getValue().isEmpty()) {
                consumer.accept(state, e.getKey(), new HttpQueryStringDeserializer(e.getValue(), schema));
            }
        }
    }

    @Override
    public int containerSize() {
        return queryParams.size();
    }
}
