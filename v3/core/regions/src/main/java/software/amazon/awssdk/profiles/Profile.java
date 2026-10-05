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

package software.amazon.awssdk.profiles;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.core.client.config.SettingSource;
import software.amazon.awssdk.utils.ToString;

/**
 * A named section of the shared configuration files (a profile, or an {@code sso-session}), with the V2 name and
 * members {@code name()}, {@code property(String)}, {@code booleanProperty(String)} and {@code properties()}.
 *
 * <p>Immutable. {@code toString} names the properties but never renders values, which may be secrets.
 */
public final class Profile {
    private final String name;
    private final Map<String, String> properties;

    private Profile(String name, Map<String, String> properties) {
        this.name = Objects.requireNonNull(name, "name");
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

    /** A profile named {@code name} with {@code properties}. */
    public static Profile create(String name, Map<String, String> properties) {
        return new Profile(name, Objects.requireNonNull(properties, "properties"));
    }

    /** The profile name, without any {@code profile } prefix. */
    public String name() {
        return name;
    }

    /** The value of property {@code propertyName}, if the profile defines it. */
    public Optional<String> property(String propertyName) {
        return Optional.ofNullable(properties.get(Objects.requireNonNull(propertyName, "propertyName")));
    }

    /**
     * The boolean value of property {@code propertyName}, parsed case-insensitively.
     *
     * @throws IllegalStateException if the property is neither {@code true} nor {@code false}
     */
    public Optional<Boolean> booleanProperty(String propertyName) {
        return property(propertyName).map(value -> {
            if (value.equalsIgnoreCase("true")) {
                return true;
            }
            if (value.equalsIgnoreCase("false")) {
                return false;
            }
            throw new IllegalStateException("Profile property '" + propertyName + "' of profile '" + name
                                            + "' must be 'true' or 'false', but was '" + value + "'.");
        });
    }

    /** Every property, in file order. Immutable. */
    public Map<String, String> properties() {
        return properties;
    }

    /** This profile as the profile source of client configuration resolution (Requirement 9.3). */
    public SettingSource asSettingSource() {
        return this::property;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Profile that && name.equals(that.name) && properties.equals(that.properties);
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + properties.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("Profile").add("name", name).add("properties", properties.keySet()).build();
    }
}
