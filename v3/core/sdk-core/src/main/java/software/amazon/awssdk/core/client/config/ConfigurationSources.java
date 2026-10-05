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
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The external configuration sources one client resolution reads: JVM system properties, environment variables and
 * the active profile (Requirement 9.3).
 *
 * <p>Injecting these sources keeps resolution deterministic: tests and tools supply fixed snapshots instead of the
 * process's real properties and environment. Every source that is not set defines no setting.
 *
 * <p>Immutable and thread-safe when the supplied sources are.
 */
public final class ConfigurationSources
    implements ToCopyableBuilder<ConfigurationSources.Builder, ConfigurationSources> {
    private final SettingSource jvmProperties;
    private final SettingSource environment;
    private final SettingSource profile;

    private ConfigurationSources(BuilderImpl builder) {
        this.jvmProperties = builder.jvmProperties;
        this.environment = builder.environment;
        this.profile = builder.profile;
    }

    /**
     * The process's JVM system properties and environment variables. No profile source is included: the shared
     * configuration file reader supplies one through {@link Builder#profile(SettingSource)}.
     */
    public static ConfigurationSources system() {
        return builder().jvmProperties(SettingSource.jvmProperties())
                        .environment(SettingSource.environment())
                        .build();
    }

    /** Sources that define no setting, so only builder values and defaults apply. */
    public static ConfigurationSources none() {
        return builder().build();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    /** The JVM system property source. */
    public SettingSource jvmProperties() {
        return jvmProperties;
    }

    /** The environment variable source. */
    public SettingSource environment() {
        return environment;
    }

    /** The property source of the active profile. */
    public SettingSource profile() {
        return profile;
    }

    /** The source of {@code source} kind; builder values, request overrides and defaults are not external sources. */
    SettingSource source(ConfigurationSource source) {
        return switch (source) {
            case JVM_PROPERTY -> jvmProperties;
            case ENVIRONMENT -> environment;
            case PROFILE -> profile;
            default -> throw new IllegalArgumentException(source + " is not an external setting source.");
        };
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    @Override
    public String toString() {
        return ToString.create("ConfigurationSources");
    }

    /** Builder of {@link ConfigurationSources}. */
    public interface Builder extends CopyableBuilder<Builder, ConfigurationSources> {
        /** The JVM system property source; {@code null} means none. */
        Builder jvmProperties(SettingSource jvmProperties);

        /** The environment variable source; {@code null} means none. */
        Builder environment(SettingSource environment);

        /** The property source of the active profile; {@code null} means none. */
        Builder profile(SettingSource profile);
    }

    private static final class BuilderImpl implements Builder {
        private SettingSource jvmProperties = SettingSource.empty();
        private SettingSource environment = SettingSource.empty();
        private SettingSource profile = SettingSource.empty();

        private BuilderImpl() {
        }

        private BuilderImpl(ConfigurationSources sources) {
            this.jvmProperties = sources.jvmProperties;
            this.environment = sources.environment;
            this.profile = sources.profile;
        }

        @Override
        public Builder jvmProperties(SettingSource jvmProperties) {
            this.jvmProperties = orEmpty(jvmProperties);
            return this;
        }

        @Override
        public Builder environment(SettingSource environment) {
            this.environment = orEmpty(environment);
            return this;
        }

        @Override
        public Builder profile(SettingSource profile) {
            this.profile = orEmpty(profile);
            return this;
        }

        private static SettingSource orEmpty(SettingSource source) {
            return Objects.requireNonNullElseGet(source, SettingSource::empty);
        }

        @Override
        public ConfigurationSources build() {
            return new ConfigurationSources(this);
        }
    }
}
