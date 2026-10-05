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

import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.core.client.config.SettingSource;

/**
 * The JVM system property and environment variable sources that region, profile-location and identity settings are read
 * from, with the V2 {@code SdkSystemSetting} precedence: a JVM system property wins over its environment variable.
 *
 * <p>Injecting fixed sources keeps resolution deterministic in tests and tools. Immutable.
 *
 * @param jvmProperties the JVM system property source
 * @param environment the environment variable source
 */
public record SystemSettings(SettingSource jvmProperties, SettingSource environment) {
    private static final SystemSettings SYSTEM = new SystemSettings(SettingSource.jvmProperties(),
                                                                   SettingSource.environment());

    public SystemSettings {
        Objects.requireNonNull(jvmProperties, "jvmProperties");
        Objects.requireNonNull(environment, "environment");
    }

    /** The process's JVM system properties and environment variables. */
    public static SystemSettings system() {
        return SYSTEM;
    }

    /**
     * The value of the setting named {@code jvmProperty} / {@code environmentVariable}: the JVM property if set, else
     * the environment variable. Either name may be null when the setting has no such form. Blank values are absent.
     */
    public Optional<String> value(String jvmProperty, String environmentVariable) {
        if (jvmProperty != null) {
            Optional<String> value = jvmProperties.setting(jvmProperty).filter(v -> !v.isBlank());
            if (value.isPresent()) {
                return value;
            }
        }
        if (environmentVariable != null) {
            return environment.setting(environmentVariable).filter(v -> !v.isBlank());
        }
        return Optional.empty();
    }

    /**
     * The boolean value of a setting, parsed case-insensitively as {@code true}/{@code false}.
     *
     * @throws IllegalStateException naming the setting if it is set to anything else
     */
    public Optional<Boolean> booleanValue(String jvmProperty, String environmentVariable) {
        return value(jvmProperty, environmentVariable).map(v -> {
            String trimmed = v.trim();
            if (trimmed.equalsIgnoreCase("true")) {
                return true;
            }
            if (trimmed.equalsIgnoreCase("false")) {
                return false;
            }
            throw new IllegalStateException("Environment variable '" + environmentVariable + "' or system property '"
                                            + jvmProperty + "' must be 'true' or 'false', but was '" + v + "'.");
        });
    }
}
