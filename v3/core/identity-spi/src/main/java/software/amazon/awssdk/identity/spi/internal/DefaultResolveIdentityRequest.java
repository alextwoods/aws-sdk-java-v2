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

package software.amazon.awssdk.identity.spi.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.identity.spi.IdentityProperty;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.utils.ToString;

/** The {@link ResolveIdentityRequest} implementation. Not a public API. */
public final class DefaultResolveIdentityRequest implements ResolveIdentityRequest {
    private final Map<IdentityProperty<?>, Object> properties;

    private DefaultResolveIdentityRequest(BuilderImpl builder) {
        this.properties = builder.properties.isEmpty()
                          ? Map.of()
                          : Collections.unmodifiableMap(new LinkedHashMap<>(builder.properties));
    }

    public static ResolveIdentityRequest.Builder builder() {
        return new BuilderImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T property(IdentityProperty<T> property) {
        return (T) properties.get(property);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DefaultResolveIdentityRequest that && properties.equals(that.properties);
    }

    @Override
    public int hashCode() {
        return properties.hashCode();
    }

    @Override
    public String toString() {
        // Property values may be secret (for example a token), so only the property keys are rendered.
        return ToString.builder("ResolveIdentityRequest")
                       .add("properties", properties.isEmpty() ? null : properties.keySet())
                       .build();
    }

    private static final class BuilderImpl implements ResolveIdentityRequest.Builder {
        private final Map<IdentityProperty<?>, Object> properties = new LinkedHashMap<>();

        private BuilderImpl() {
        }

        private BuilderImpl(DefaultResolveIdentityRequest request) {
            properties.putAll(request.properties);
        }

        @Override
        public <T> Builder putProperty(IdentityProperty<T> key, T value) {
            Objects.requireNonNull(key, "key");
            if (value == null) {
                properties.remove(key);
            } else {
                properties.put(key, value);
            }
            return this;
        }

        @Override
        public ResolveIdentityRequest build() {
            return new DefaultResolveIdentityRequest(this);
        }
    }
}
