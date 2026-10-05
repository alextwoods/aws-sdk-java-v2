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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Raw text settings of one external configuration source: JVM system properties, environment variables, or the
 * properties of the active profile (Requirement 9.3).
 *
 * <p>Resolution reads every external source through this interface, never through {@link System} directly, so a client
 * can be built against a fixed, injected snapshot. The profile source is the seam that the shared configuration file
 * support fills in: it returns the properties of the already-selected profile, keyed by property name (for example
 * {@code region}).
 *
 * <p>Implementations must be thread-safe. A setting is present when the returned value is present, even if it is
 * empty; interpreting the text is the setting parser's job.
 */
@FunctionalInterface
public interface SettingSource {

    /** The raw value of setting {@code name}, or empty if this source does not define it. */
    Optional<String> setting(String name);

    /** The JVM system properties, read when a setting is looked up. */
    static SettingSource jvmProperties() {
        return name -> Optional.ofNullable(System.getProperty(Objects.requireNonNull(name, "name")));
    }

    /** The process environment variables, read when a setting is looked up. */
    static SettingSource environment() {
        return name -> Optional.ofNullable(System.getenv(Objects.requireNonNull(name, "name")));
    }

    /** A fixed snapshot of {@code settings}; later changes to the map are not observed. */
    static SettingSource fromMap(Map<String, String> settings) {
        Map<String, String> copy = Map.copyOf(Objects.requireNonNull(settings, "settings"));
        return name -> Optional.ofNullable(copy.get(Objects.requireNonNull(name, "name")));
    }

    /** A source that defines no setting. */
    static SettingSource empty() {
        return name -> {
            Objects.requireNonNull(name, "name");
            return Optional.empty();
        };
    }
}
