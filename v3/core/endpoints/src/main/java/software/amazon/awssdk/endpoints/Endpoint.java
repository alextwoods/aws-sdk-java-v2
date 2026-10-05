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

package software.amazon.awssdk.endpoints;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.endpoints.internal.EndpointValues;

/**
 * A resolved endpoint: the URL a request is sent to, the headers the rules require on it, and the endpoint
 * properties (including the {@code authSchemes} that override the modeled signing properties). Instances are
 * immutable and compare by value.
 *
 * <p>Property values are the rule-set value types: {@link String}, {@link Boolean}, {@link Integer}, immutable
 * {@code List<Object>} and immutable {@code Map<String, Object>}.
 */
public final class Endpoint {
    /** The endpoint property that lists the auth schemes of the endpoint, in preference order. */
    public static final String AUTH_SCHEMES_PROPERTY = "authSchemes";

    private final URI url;
    private final Map<String, List<String>> headers;
    private final Map<String, Object> properties;
    private final List<EndpointAuthScheme> authSchemes;

    private Endpoint(Builder builder) {
        this.url = Objects.requireNonNull(builder.url, "url");
        Map<String, List<String>> copiedHeaders = new LinkedHashMap<>();
        builder.headers.forEach((name, values) -> copiedHeaders.put(name, List.copyOf(values)));
        this.headers = Collections.unmodifiableMap(copiedHeaders);
        Map<String, Object> copiedProperties = new LinkedHashMap<>();
        builder.properties.forEach((name, value) -> copiedProperties.put(name, EndpointValues.immutable(value)));
        this.properties = Collections.unmodifiableMap(copiedProperties);
        this.authSchemes = EndpointAuthScheme.fromProperty(properties.get(AUTH_SCHEMES_PROPERTY));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        Builder builder = new Builder().url(url);
        headers.forEach(builder::putHeader);
        properties.forEach(builder::putProperty);
        return builder;
    }

    /** The endpoint URL. */
    public URI url() {
        return url;
    }

    /** The headers the endpoint requires, by name, in rule order. Immutable. */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /** Every endpoint property, by name. Immutable. */
    public Map<String, Object> properties() {
        return properties;
    }

    /** The property {@code name}, if the endpoint has it. */
    public Optional<Object> property(String name) {
        return Optional.ofNullable(properties.get(name));
    }

    /**
     * The auth schemes of the {@value #AUTH_SCHEMES_PROPERTY} property, in the rules' preference order; empty if the
     * endpoint does not constrain auth. Immutable.
     */
    public List<EndpointAuthScheme> authSchemes() {
        return authSchemes;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Endpoint that && url.equals(that.url) && headers.equals(that.headers)
               && properties.equals(that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, headers, properties);
    }

    @Override
    public String toString() {
        return "Endpoint(url=" + url + ", headers=" + headers + ", properties=" + properties + ")";
    }

    /** Builder of an {@link Endpoint}. Not thread-safe. */
    public static final class Builder {
        private URI url;
        private final Map<String, List<String>> headers = new LinkedHashMap<>();
        private final Map<String, Object> properties = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder url(URI url) {
            this.url = url;
            return this;
        }

        /** Sets the values of header {@code name}, replacing any earlier values. */
        public Builder putHeader(String name, List<String> values) {
            headers.put(Objects.requireNonNull(name, "name"), new ArrayList<>(values));
            return this;
        }

        /** Sets property {@code name}; {@code value} must be a rule-set value type. */
        public Builder putProperty(String name, Object value) {
            properties.put(Objects.requireNonNull(name, "name"), EndpointValues.requireValue(value));
            return this;
        }

        public Endpoint build() {
            return new Endpoint(this);
        }
    }
}
