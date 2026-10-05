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
import java.util.Optional;
import java.util.function.Function;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * How one client-constant option is resolved from its sources (Requirement 9.3): the names it has in the JVM
 * properties, the environment and the profile, the parser of their text, and the SDK default.
 *
 * <p>For example, region is read from builder value, JVM property {@code aws.region}, environment variable
 * {@code AWS_REGION}, profile property {@code region}, in that order. A setting that has no name for a source is never
 * read from it. The default is computed from the options resolved before it, so derived constants (for example a
 * default endpoint that depends on the region) are computed once, at client construction, like every other value.
 *
 * <p>Immutable; the parser and default function must be thread-safe and side-effect free apart from creating the value.
 *
 * @param <T> the option's value type
 */
public final class ClientSetting<T> implements ToCopyableBuilder<ClientSetting.Builder<T>, ClientSetting<T>> {
    private final ClientOption<T> option;
    private final String jvmProperty;
    private final String environmentVariable;
    private final String profileProperty;
    private final Function<String, ? extends T> parser;
    private final Function<SdkClientConfiguration, ? extends T> defaultValue;

    private ClientSetting(BuilderImpl<T> builder) {
        this.option = builder.option;
        this.jvmProperty = builder.jvmProperty;
        this.environmentVariable = builder.environmentVariable;
        this.profileProperty = builder.profileProperty;
        this.parser = builder.parser;
        this.defaultValue = builder.defaultValue;
        if (parser == null && (jvmProperty != null || environmentVariable != null || profileProperty != null)) {
            throw new IllegalArgumentException("Setting " + option.name()
                                               + " names an external source but has no parser.");
        }
    }

    /** A builder of the setting that resolves {@code option}. */
    public static <T> Builder<T> builder(ClientOption<T> option) {
        return new BuilderImpl<>(Objects.requireNonNull(option, "option"));
    }

    /** The option this setting resolves. */
    public ClientOption<T> option() {
        return option;
    }

    /** Name of the JVM system property, if the option can be set by one. */
    public Optional<String> jvmProperty() {
        return Optional.ofNullable(jvmProperty);
    }

    /** Name of the environment variable, if the option can be set by one. */
    public Optional<String> environmentVariable() {
        return Optional.ofNullable(environmentVariable);
    }

    /** Name of the profile property, if the option can be set by one. */
    public Optional<String> profileProperty() {
        return Optional.ofNullable(profileProperty);
    }

    /** The source's name for this setting, if it has one. */
    Optional<String> name(ConfigurationSource source) {
        return switch (source) {
            case JVM_PROPERTY -> jvmProperty();
            case ENVIRONMENT -> environmentVariable();
            case PROFILE -> profileProperty();
            default -> Optional.empty();
        };
    }

    /** Parses external setting text; only called when the setting names an external source. */
    T parse(String text) {
        return parser.apply(text);
    }

    /** The SDK default given the options resolved so far, or {@code null} when there is none. */
    T defaultValue(SdkClientConfiguration resolvedSoFar) {
        return defaultValue == null ? null : defaultValue.apply(resolvedSoFar);
    }

    @Override
    public Builder<T> toBuilder() {
        return new BuilderImpl<>(this);
    }

    @Override
    public String toString() {
        return ToString.builder("ClientSetting")
                       .add("option", option.name())
                       .add("jvmProperty", jvmProperty)
                       .add("environmentVariable", environmentVariable)
                       .add("profileProperty", profileProperty)
                       .build();
    }

    /** Builder of a {@link ClientSetting}. */
    public interface Builder<T> extends CopyableBuilder<Builder<T>, ClientSetting<T>> {
        /** Name of the JVM system property that sets the option; {@code null} means none. */
        Builder<T> jvmProperty(String jvmProperty);

        /** Name of the environment variable that sets the option; {@code null} means none. */
        Builder<T> environmentVariable(String environmentVariable);

        /** Name of the active profile's property that sets the option; {@code null} means none. */
        Builder<T> profileProperty(String profileProperty);

        /**
         * Parser of external setting text. Required when any external source is named. It throws a runtime exception
         * for invalid text, which resolution reports without the text (the text may be secret).
         */
        Builder<T> parser(Function<String, ? extends T> parser);

        /** The SDK default. */
        Builder<T> defaultValue(T defaultValue);

        /**
         * The SDK default, computed from the options resolved before this setting; returning {@code null} means no
         * default. Called at most once per client construction.
         */
        Builder<T> derivedDefault(Function<SdkClientConfiguration, ? extends T> defaultValue);
    }

    private static final class BuilderImpl<T> implements Builder<T> {
        private final ClientOption<T> option;
        private String jvmProperty;
        private String environmentVariable;
        private String profileProperty;
        private Function<String, ? extends T> parser;
        private Function<SdkClientConfiguration, ? extends T> defaultValue;

        private BuilderImpl(ClientOption<T> option) {
            this.option = option;
        }

        private BuilderImpl(ClientSetting<T> setting) {
            this.option = setting.option;
            this.jvmProperty = setting.jvmProperty;
            this.environmentVariable = setting.environmentVariable;
            this.profileProperty = setting.profileProperty;
            this.parser = setting.parser;
            this.defaultValue = setting.defaultValue;
        }

        @Override
        public Builder<T> jvmProperty(String jvmProperty) {
            this.jvmProperty = jvmProperty;
            return this;
        }

        @Override
        public Builder<T> environmentVariable(String environmentVariable) {
            this.environmentVariable = environmentVariable;
            return this;
        }

        @Override
        public Builder<T> profileProperty(String profileProperty) {
            this.profileProperty = profileProperty;
            return this;
        }

        @Override
        public Builder<T> parser(Function<String, ? extends T> parser) {
            this.parser = parser;
            return this;
        }

        @Override
        public Builder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue == null ? null : resolved -> defaultValue;
            return this;
        }

        @Override
        public Builder<T> derivedDefault(Function<SdkClientConfiguration, ? extends T> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        @Override
        public ClientSetting<T> build() {
            return new ClientSetting<>(this);
        }
    }
}
