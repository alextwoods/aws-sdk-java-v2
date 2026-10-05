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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One entry of a resolved endpoint's {@code authSchemes} property: the endpoint-auth dimension of endpoint rules. It
 * names an auth scheme (for example {@code sigv4}, {@code sigv4a} or {@code sigv4-s3express}) and the signing
 * properties the endpoint requires, which take precedence over the modeled signing properties of that scheme.
 *
 * <p>Well-known properties have typed accessors; {@link #properties()} holds every property of the entry, including
 * ones this SDK version does not know. Instances are immutable and compare by value.
 */
public final class EndpointAuthScheme {
    /** The {@code name} property. */
    public static final String NAME = "name";
    /** The {@code signingName} property. */
    public static final String SIGNING_NAME = "signingName";
    /** The {@code signingRegion} property (SigV4). */
    public static final String SIGNING_REGION = "signingRegion";
    /** The {@code signingRegionSet} property (SigV4a). */
    public static final String SIGNING_REGION_SET = "signingRegionSet";
    /** The {@code disableDoubleEncoding} property. */
    public static final String DISABLE_DOUBLE_ENCODING = "disableDoubleEncoding";
    /** The {@code disableNormalizePath} property. */
    public static final String DISABLE_NORMALIZE_PATH = "disableNormalizePath";

    private final String name;
    private final Map<String, Object> properties;

    private EndpointAuthScheme(String name, Map<String, Object> properties) {
        this.name = name;
        this.properties = properties;
    }

    /**
     * The auth schemes of an endpoint's {@code authSchemes} property value, in order; empty if the value is absent.
     *
     * @throws IllegalArgumentException if the value is not a list of records that each have a string {@code name}
     */
    static List<EndpointAuthScheme> fromProperty(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("The endpoint authSchemes property must be a list: " + value);
        }
        List<EndpointAuthScheme> result = new ArrayList<>(list.size());
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> map) || !(map.get(NAME) instanceof String name)) {
                throw new IllegalArgumentException("Every endpoint auth scheme must be a record with a string name: "
                                                   + entry);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) map;
            result.add(new EndpointAuthScheme(name, properties));
        }
        return List.copyOf(result);
    }

    /** The auth scheme name, for example {@code sigv4}. */
    public String name() {
        return name;
    }

    /** The signing name the endpoint requires. */
    public Optional<String> signingName() {
        return string(SIGNING_NAME);
    }

    /** The SigV4 signing region the endpoint requires. */
    public Optional<String> signingRegion() {
        return string(SIGNING_REGION);
    }

    /** The SigV4a signing region set the endpoint requires; empty if it does not set one. */
    public List<String> signingRegionSet() {
        Object value = properties.get(SIGNING_REGION_SET);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> regions = new ArrayList<>(list.size());
        for (Object region : list) {
            regions.add(String.valueOf(region));
        }
        return List.copyOf(regions);
    }

    /** Whether the endpoint disables double URI-encoding of the canonical request path, if it says. */
    public Optional<Boolean> disableDoubleEncoding() {
        return bool(DISABLE_DOUBLE_ENCODING);
    }

    /** Whether the endpoint disables path normalization of the canonical request, if it says. */
    public Optional<Boolean> disableNormalizePath() {
        return bool(DISABLE_NORMALIZE_PATH);
    }

    /** Every property of the entry, including {@code name}. Immutable. */
    public Map<String, Object> properties() {
        return properties;
    }

    private Optional<String> string(String key) {
        return properties.get(key) instanceof String s ? Optional.of(s) : Optional.empty();
    }

    private Optional<Boolean> bool(String key) {
        return properties.get(key) instanceof Boolean b ? Optional.of(b) : Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EndpointAuthScheme that && properties.equals(that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(properties);
    }

    @Override
    public String toString() {
        return "EndpointAuthScheme" + properties;
    }
}
