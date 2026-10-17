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
import java.util.Locale;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpHeaders;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.SpecificShapeDeserializer;

/**
 * Reads an {@code @httpPrefixHeaders} map: one entry for every header whose name starts with the prefix, compared
 * case-insensitively. The key is the header name, as received, with the prefix removed; the value is the header's
 * first value.
 *
 * <p>Port of smithy-java {@code HttpPrefixHeadersDeserializer}.
 */
@SdkInternalApi
final class HttpPrefixHeadersDeserializer extends SpecificShapeDeserializer {
    private final SdkHttpHeaders headers;
    private final String canonicalPrefix;

    HttpPrefixHeadersDeserializer(SdkHttpHeaders headers, HttpBindingSchemaExtensions.MemberBinding binding) {
        this.headers = headers;
        this.canonicalPrefix = binding.canonicalName();
    }

    @Override
    protected RuntimeException throwForInvalidState(Schema schema) {
        return new SerializationException("httpPrefixHeaders must be bound to a string-valued map: " + schema.id());
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
        headers.forEachHeader((name, values) -> {
            String canonicalName = name.toLowerCase(Locale.ROOT);
            if (!canonicalName.startsWith(canonicalPrefix)
                || PrefixConstants.OMITTED_HEADER_NAMES.contains(canonicalName)) {
                return;
            }
            consumer.accept(state, name.substring(canonicalPrefix.length()), new HeaderValueDeserializer(values));
        });
    }

    private static final class HeaderValueDeserializer extends SpecificShapeDeserializer {
        private final List<String> values;

        HeaderValueDeserializer(List<String> values) {
            this.values = values;
        }

        @Override
        protected RuntimeException throwForInvalidState(Schema schema) {
            return new SerializationException("Header values must be strings: " + schema.id());
        }

        @Override
        public String readString(Schema schema) {
            return values.isEmpty() ? "" : values.get(0);
        }

        @Override
        public boolean isNull() {
            // An empty string is returned when the header has no value.
            return false;
        }
    }
}
