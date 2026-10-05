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
import software.amazon.awssdk.identity.spi.IdentityProperty;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * One candidate way to authenticate a call, with the V2 name and members: the {@linkplain #schemeId() auth scheme} and
 * the identity and signer properties to use with it. An operation's candidates come, in order, from its modeled auth
 * traits ({@link ModeledAuthSchemes}), possibly refined by endpoint auth properties; the first one the client can use
 * is selected ({@link AuthSchemeSelector}).
 *
 * <p>Immutable. {@code toString} names the properties but never renders values.
 */
public final class AuthSchemeOption implements ToCopyableBuilder<AuthSchemeOption.Builder, AuthSchemeOption> {
    private final String schemeId;
    private final Map<IdentityProperty<?>, Object> identityProperties;
    private final Map<SignerProperty<?>, Object> signerProperties;

    private AuthSchemeOption(Builder builder) {
        this.schemeId = Objects.requireNonNull(builder.schemeId, "schemeId");
        this.identityProperties = Collections.unmodifiableMap(new LinkedHashMap<>(builder.identityProperties));
        this.signerProperties = Collections.unmodifiableMap(new LinkedHashMap<>(builder.signerProperties));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The absolute Smithy shape ID of the auth scheme, for example {@code aws.auth#sigv4}. */
    public String schemeId() {
        return schemeId;
    }

    /** The value of identity property {@code property}, or null. */
    @SuppressWarnings("unchecked")
    public <T> T identityProperty(IdentityProperty<T> property) {
        return (T) identityProperties.get(property);
    }

    /** The value of signer property {@code property}, or null. */
    @SuppressWarnings("unchecked")
    public <T> T signerProperty(SignerProperty<T> property) {
        return (T) signerProperties.get(property);
    }

    /** Every signer property. Immutable. */
    public Map<SignerProperty<?>, Object> signerProperties() {
        return signerProperties;
    }

    /** The identity request carrying this option's identity properties. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ResolveIdentityRequest identityRequest() {
        ResolveIdentityRequest.Builder builder = ResolveIdentityRequest.builder();
        identityProperties.forEach((k, v) -> builder.putProperty((IdentityProperty) k, v));
        return builder.build();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.schemeId = schemeId;
        builder.identityProperties.putAll(identityProperties);
        builder.signerProperties.putAll(signerProperties);
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AuthSchemeOption that && schemeId.equals(that.schemeId)
               && identityProperties.equals(that.identityProperties) && signerProperties.equals(that.signerProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemeId, identityProperties, signerProperties);
    }

    @Override
    public String toString() {
        return ToString.builder("AuthSchemeOption")
                       .add("schemeId", schemeId)
                       .add("identityProperties", identityProperties.isEmpty() ? null : identityProperties.keySet())
                       .add("signerProperties", signerProperties.isEmpty() ? null : signerProperties.keySet())
                       .build();
    }

    /** Builder of an {@link AuthSchemeOption}. */
    public static final class Builder implements CopyableBuilder<Builder, AuthSchemeOption> {
        private String schemeId;
        private final Map<IdentityProperty<?>, Object> identityProperties = new LinkedHashMap<>();
        private final Map<SignerProperty<?>, Object> signerProperties = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder schemeId(String schemeId) {
            this.schemeId = schemeId;
            return this;
        }

        /** Sets {@code property}; {@code null} removes it. */
        public <T> Builder putIdentityProperty(IdentityProperty<T> property, T value) {
            Objects.requireNonNull(property, "property");
            if (value == null) {
                identityProperties.remove(property);
            } else {
                identityProperties.put(property, value);
            }
            return this;
        }

        /** Sets {@code property}; {@code null} removes it. */
        public <T> Builder putSignerProperty(SignerProperty<T> property, T value) {
            Objects.requireNonNull(property, "property");
            if (value == null) {
                signerProperties.remove(property);
            } else {
                signerProperties.put(property, value);
            }
            return this;
        }

        /** Sets {@code property} unless it is already set. */
        public <T> Builder putSignerPropertyIfAbsent(SignerProperty<T> property, T value) {
            if (!signerProperties.containsKey(property)) {
                putSignerProperty(property, value);
            }
            return this;
        }

        @Override
        public AuthSchemeOption build() {
            return new AuthSchemeOption(this);
        }
    }
}
