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

package software.amazon.awssdk.core.client.config;

import java.util.Objects;
import software.amazon.awssdk.core.resource.ResourceOwnership;

/**
 * One resolved configuration value and its provenance: the source it came from and who owns it (Requirements 9.3,
 * 9.6).
 *
 * <p>Immutable. {@link #toString()} renders the key, source and ownership but never the value, which may be secret
 * (Requirement 21.10).
 *
 * @param key the option
 * @param value the value, an instance of the option's value type
 * @param source the source the value was taken from
 * @param ownership who closes the value if it is a closeable resource
 * @param <T> the value type
 */
public record ResolvedOption<T>(ClientOption<T> key, T value, ConfigurationSource source, ResourceOwnership ownership) {

    /**
     * @throws NullPointerException if any component is null
     * @throws ClassCastException if {@code value} is not an instance of the option's value type
     */
    public ResolvedOption {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(ownership, "ownership");
        if (!key.valueType().isInstance(value)) {
            throw new ClassCastException(key + " requires a " + key.valueType().getName()
                                         + " value, but was given a " + value.getClass().getName() + ".");
        }
    }

    /** {@code value} from {@code source}, owned as {@link ConfigurationSource#defaultOwnership()} says. */
    public static <T> ResolvedOption<T> of(ClientOption<T> key, T value, ConfigurationSource source) {
        return new ResolvedOption<>(key, value, source, Objects.requireNonNull(source, "source").defaultOwnership());
    }

    /** Whether this value wins over {@code other} under the precedence of {@link ConfigurationSource}. */
    public boolean outranks(ResolvedOption<?> other) {
        return source.outranks(Objects.requireNonNull(other, "other").source);
    }

    /** Renders the option, source and ownership only: the value may be secret (Requirement 21.10). */
    @Override
    public String toString() {
        return "ResolvedOption(" + key.name() + " from " + source + ", " + ownership + ")";
    }
}
