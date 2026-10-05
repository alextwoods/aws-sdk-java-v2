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

package software.amazon.awssdk.auth.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.ToString;

/**
 * The input of {@link HttpSigner#sign}: the unsigned HTTP request (whose body is the payload), the identity to sign
 * with and the signer properties of the selected {@link AuthSchemeOption}, with the V2 name.
 *
 * <p>Immutable. {@code toString} never renders the identity or property values.
 *
 * @param <IdentityT> the identity type
 */
public final class SignRequest<IdentityT extends Identity> {
    private final SdkHttpRequest request;
    private final IdentityT identity;
    private final Map<SignerProperty<?>, Object> properties;

    private SignRequest(Builder<IdentityT> builder) {
        this.request = Objects.requireNonNull(builder.request, "request");
        this.identity = Objects.requireNonNull(builder.identity, "identity");
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(builder.properties));
    }

    /** A builder of a request signed with {@code identity}. */
    public static <IdentityT extends Identity> Builder<IdentityT> builder(IdentityT identity) {
        return new Builder<IdentityT>().identity(identity);
    }

    /** The unsigned request. */
    public SdkHttpRequest request() {
        return request;
    }

    /** The identity to sign with. */
    public IdentityT identity() {
        return identity;
    }

    /** The value of {@code property}, or null. */
    @SuppressWarnings("unchecked")
    public <T> T property(SignerProperty<T> property) {
        return (T) properties.get(property);
    }

    /**
     * The value of {@code property}.
     *
     * @throws IllegalArgumentException naming the property if it is not set
     */
    public <T> T requireProperty(SignerProperty<T> property) {
        T value = property(property);
        if (value == null) {
            throw new IllegalArgumentException(property + " must not be null!");
        }
        return value;
    }

    /** The value of {@code property}, or {@code defaultValue}. */
    public <T> T requireProperty(SignerProperty<T> property, T defaultValue) {
        T value = property(property);
        return value == null ? defaultValue : value;
    }

    /** Every property. Immutable. */
    public Map<SignerProperty<?>, Object> properties() {
        return properties;
    }

    /** A builder copying this request. */
    public Builder<IdentityT> toBuilder() {
        Builder<IdentityT> builder = new Builder<>();
        builder.request = request;
        builder.identity = identity;
        builder.properties.putAll(properties);
        return builder;
    }

    @Override
    public String toString() {
        return ToString.builder("SignRequest").add("request", request).add("properties", properties.keySet()).build();
    }

    /** Builder of a {@link SignRequest}. */
    public static final class Builder<IdentityT extends Identity> {
        private SdkHttpRequest request;
        private IdentityT identity;
        private final Map<SignerProperty<?>, Object> properties = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder<IdentityT> request(SdkHttpRequest request) {
            this.request = request;
            return this;
        }

        public Builder<IdentityT> identity(IdentityT identity) {
            this.identity = identity;
            return this;
        }

        /** Sets {@code property}; {@code null} removes it. */
        public <T> Builder<IdentityT> putProperty(SignerProperty<T> property, T value) {
            Objects.requireNonNull(property, "property");
            if (value == null) {
                properties.remove(property);
            } else {
                properties.put(property, value);
            }
            return this;
        }

        /** Sets every property of {@code values} (for example an option's signer properties). */
        public Builder<IdentityT> putProperties(Map<SignerProperty<?>, Object> values) {
            values.forEach((k, v) -> properties.put(Objects.requireNonNull(k, "property"), Objects.requireNonNull(v)));
            return this;
        }

        public SignRequest<IdentityT> build() {
            return new SignRequest<>(this);
        }
    }
}
