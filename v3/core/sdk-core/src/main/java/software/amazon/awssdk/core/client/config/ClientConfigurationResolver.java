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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.core.resource.ManagedResource;
import software.amazon.awssdk.smithy.runtime.version.RuntimeVersionHandshake;
import software.amazon.awssdk.smithy.runtime.version.RuntimeVersionMismatchException;
import software.amazon.awssdk.utils.ToString;

/**
 * Resolves the client-constant configuration of one client at construction (Requirements 9.3, 9.5, 9.6, 9.9, 15.11).
 *
 * <p>{@link #resolve(SdkClientConfiguration)} runs once per client build and, in order:
 * <ol>
 *     <li>rejects every supplied option registered as {@linkplain Builder#addUnsupportedOption unsupported} (an option
 *     with no V3 behavior never disappears silently);</li>
 *     <li>runs the {@linkplain RuntimeVersionHandshake runtime version handshake}, if configured, and fails on any
 *     incompatible V3 module;</li>
 *     <li>resolves each {@linkplain ClientSetting setting}, in registration order, from the highest-precedence source
 *     that provides it: explicit builder value, JVM property, environment variable, profile property, SDK default.
 *     Lower-precedence sources are not read once a value is found, and each source, parser and default is consulted at
 *     most once per setting;</li>
 *     <li>keeps every other supplied builder option as given, with its provenance;</li>
 *     <li>records ownership of every configured resource and of every resolved {@link AutoCloseable}
 *     value.</li>
 * </ol>
 * The result is immutable, so every call reads the values resolved once at construction (Requirement 9.5). Request
 * overrides outrank all client sources but apply to one call; {@link #select(Collection)} is the single precedence rule
 * for both.
 *
 * <p>Immutable and thread-safe when its sources, parsers and defaults are.
 */
public final class ClientConfigurationResolver {
    private static final List<ConfigurationSource> EXTERNAL_SOURCES =
        List.of(ConfigurationSource.JVM_PROPERTY, ConfigurationSource.ENVIRONMENT, ConfigurationSource.PROFILE);

    private final ConfigurationSources sources;
    private final List<ClientSetting<?>> settings;
    private final Map<ClientOption<?>, String> unsupportedOptions;
    private final RuntimeVersionHandshake handshake;
    private final String expectedVersion;
    private final List<String> participatingModules;

    private ClientConfigurationResolver(BuilderImpl builder) {
        this.sources = builder.sources;
        this.settings = List.copyOf(builder.settings.values());
        this.unsupportedOptions = new LinkedHashMap<>(builder.unsupportedOptions);
        this.handshake = builder.handshake;
        this.expectedVersion = builder.expectedVersion;
        this.participatingModules = builder.participatingModules;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * The client configuration resolved from {@code builderValues} (the options and resources the caller set on the
     * client builder) and this resolver's sources.
     *
     * @throws SdkClientConfigurationException if a supplied option has no V3 behavior, a runtime module version is
     *     incompatible, or external setting text cannot be parsed
     */
    public SdkClientConfiguration resolve(SdkClientConfiguration builderValues) {
        Objects.requireNonNull(builderValues, "builderValues");
        rejectUnsupported(builderValues);
        verifyRuntimeVersions();

        SdkClientConfiguration.Builder result = SdkClientConfiguration.builder();
        for (ClientSetting<?> setting : settings) {
            resolveSetting(setting, builderValues, result);
        }
        for (ClientOption<?> option : builderValues.options()) {
            if (result.resolved(option) == null) {
                builderValues.resolved(option).ifPresent(result::option);
            }
        }

        List<ManagedResource<?>> resources = new ArrayList<>(builderValues.resources());
        for (ClientOption<?> option : result.build().options()) {
            ResolvedOption<?> resolved = result.resolved(option);
            if (resolved.value() instanceof AutoCloseable) {
                ManagedResource<?> resource = ManagedResource.of(resolved.value(), resolved.ownership());
                if (!resources.contains(resource)) {
                    resources.add(resource);
                }
            }
        }
        return result.resources(resources).build();
    }

    /**
     * The candidate from the highest-precedence source (Requirement 9.3), or empty if there is none. {@code null}
     * elements are ignored.
     *
     * @throws IllegalArgumentException if candidates are for different options or two come from one source
     */
    public static <T> Optional<ResolvedOption<T>> select(Collection<? extends ResolvedOption<T>> candidates) {
        ResolvedOption<T> selected = null;
        for (ResolvedOption<T> candidate : Objects.requireNonNull(candidates, "candidates")) {
            if (candidate == null) {
                continue;
            }
            if (selected != null && selected.key() != candidate.key()) {
                throw new IllegalArgumentException("Candidates are for different options: " + selected.key().name()
                                                   + " and " + candidate.key().name() + ".");
            }
            if (selected != null && selected.source() == candidate.source()) {
                throw new IllegalArgumentException("Two candidates for " + candidate.key().name() + " come from "
                                                   + candidate.source() + ".");
            }
            if (selected == null || candidate.outranks(selected)) {
                selected = candidate;
            }
        }
        return Optional.ofNullable(selected);
    }

    private void rejectUnsupported(SdkClientConfiguration builderValues) {
        List<ClientOption<?>> rejected = new ArrayList<>();
        for (ClientOption<?> option : builderValues.options()) {
            if (unsupportedOptions.containsKey(option)) {
                rejected.add(option);
            }
        }
        if (rejected.isEmpty()) {
            return;
        }
        StringBuilder message = new StringBuilder("Client construction failed: ")
            .append(rejected.size() == 1 ? "configuration option " : "configuration options ");
        // Rejected options are listed in supply order, each with its disposition and provenance (the error taxonomy
        // requires option and source); the supplied value is never rendered.
        for (int i = 0; i < rejected.size(); i++) {
            ClientOption<?> option = rejected.get(i);
            ConfigurationSource source = builderValues.resolved(option).orElseThrow().source();
            message.append(i == 0 ? "" : ", ").append(option.name())
                   .append(" (disposition ").append(unsupportedOptions.get(option)).append(") from ")
                   .append(describe(source));
        }
        message.append(rejected.size() == 1 ? " has" : " have").append(" no V3 behavior. Remove ")
               .append(rejected.size() == 1 ? "it" : "them")
               .append(" or use the V3 replacement named by the migration guide.");
        throw SdkClientConfigurationException.builder()
                                             .option(rejected.get(0).name())
                                             .message(message.toString())
                                             .build();
    }

    private void verifyRuntimeVersions() {
        if (handshake == null) {
            return;
        }
        try {
            handshake.verify(expectedVersion, participatingModules);
        } catch (RuntimeVersionMismatchException e) {
            throw SdkClientConfigurationException.builder().message(e.getMessage()).cause(e).build();
        }
    }

    private <T> void resolveSetting(ClientSetting<T> setting, SdkClientConfiguration builderValues,
                                    SdkClientConfiguration.Builder result) {
        ClientOption<T> option = setting.option();
        Optional<ResolvedOption<T>> explicit = builderValues.resolved(option);
        if (explicit.isPresent()) {
            result.option(explicit.get());
            return;
        }
        for (ConfigurationSource source : EXTERNAL_SOURCES) {
            Optional<String> name = setting.name(source);
            if (name.isEmpty()) {
                continue;
            }
            Optional<String> text = sources.source(source).setting(name.get());
            if (text.isPresent()) {
                result.option(option, parse(setting, source, name.get(), text.get()), source);
                return;
            }
        }
        T defaultValue = setting.defaultValue(result.build());
        if (defaultValue != null) {
            result.option(option, defaultValue, ConfigurationSource.DEFAULT);
        }
    }

    // The setting text is never included in the diagnostic, nor is the parser's exception (its message may quote the
    // text): external settings can be secret (Requirement 21.10).
    private static <T> T parse(ClientSetting<T> setting, ConfigurationSource source, String name, String text) {
        T value;
        try {
            value = setting.parse(text);
        } catch (RuntimeException e) {
            throw invalid(setting, source, name, " (" + e.getClass().getName() + ")");
        }
        if (value == null || !setting.option().valueType().isInstance(value)) {
            throw invalid(setting, source, name, "");
        }
        return value;
    }

    private static SdkClientConfigurationException invalid(ClientSetting<?> setting, ConfigurationSource source,
                                                           String name, String detail) {
        return SdkClientConfigurationException.builder()
                                              .option(setting.option().name())
                                              .message("Invalid value for configuration option "
                                                       + setting.option().name() + " from " + describe(source)
                                                       + " '" + name + "'" + detail + ".")
                                              .build();
    }

    private static String describe(ConfigurationSource source) {
        return switch (source) {
            case REQUEST_OVERRIDE -> "request override";
            case BUILDER -> "client builder";
            case JVM_PROPERTY -> "JVM property";
            case ENVIRONMENT -> "environment variable";
            case PROFILE -> "profile property";
            case DEFAULT -> "SDK default";
        };
    }

    @Override
    public String toString() {
        List<String> names = new ArrayList<>(settings.size());
        settings.forEach(setting -> names.add(setting.option().name()));
        return ToString.builder("ClientConfigurationResolver")
                       .add("settings", names)
                       .add("runtimeVersion", expectedVersion)
                       .build();
    }

    /** Builder of a {@link ClientConfigurationResolver}. */
    public interface Builder {
        /** The JVM property, environment and profile sources; defaults to {@link ConfigurationSources#none()}. */
        Builder sources(ConfigurationSources sources);

        /**
         * Adds {@code setting}. Settings resolve in the order added, so a derived default sees every option added
         * before it.
         *
         * @throws IllegalArgumentException if a setting for the same option was already added
         */
        Builder addSetting(ClientSetting<?> setting);

        /**
         * Registers {@code option} as a supplied option without V3 behavior: construction fails if it is set, naming
         * the option, its Parity Manifest {@code disposition} and the {@link ConfigurationSource} it was supplied from
         * (Requirement 9.9). When several unsupported options are supplied, all are named, in supply order.
         * Registering an option again replaces its disposition.
         */
        Builder addUnsupportedOption(ClientOption<?> option, String disposition);

        /**
         * Verifies at every resolution that each of {@code participatingModules} is present at exactly
         * {@code expectedVersion}, through {@code handshake} (Requirement 15.11).
         */
        Builder runtimeVersions(RuntimeVersionHandshake handshake, String expectedVersion,
                                Collection<String> participatingModules);

        /** @throws IllegalArgumentException if an option is both a setting and unsupported */
        ClientConfigurationResolver build();
    }

    private static final class BuilderImpl implements Builder {
        private ConfigurationSources sources = ConfigurationSources.none();
        private final Map<ClientOption<?>, ClientSetting<?>> settings = new LinkedHashMap<>();
        private final Map<ClientOption<?>, String> unsupportedOptions = new LinkedHashMap<>();
        private RuntimeVersionHandshake handshake;
        private String expectedVersion;
        private List<String> participatingModules;

        @Override
        public Builder sources(ConfigurationSources sources) {
            this.sources = Objects.requireNonNull(sources, "sources");
            return this;
        }

        @Override
        public Builder addSetting(ClientSetting<?> setting) {
            Objects.requireNonNull(setting, "setting");
            if (settings.putIfAbsent(setting.option(), setting) != null) {
                throw new IllegalArgumentException("A setting for " + setting.option().name() + " was already added.");
            }
            return this;
        }

        @Override
        public Builder addUnsupportedOption(ClientOption<?> option, String disposition) {
            Objects.requireNonNull(option, "option");
            if (Objects.requireNonNull(disposition, "disposition").isBlank()) {
                throw new IllegalArgumentException("A disposition must not be blank.");
            }
            unsupportedOptions.put(option, disposition);
            return this;
        }

        @Override
        public Builder runtimeVersions(RuntimeVersionHandshake handshake, String expectedVersion,
                                       Collection<String> participatingModules) {
            this.handshake = Objects.requireNonNull(handshake, "handshake");
            if (Objects.requireNonNull(expectedVersion, "expectedVersion").isBlank()) {
                throw new IllegalArgumentException("An expected runtime version must not be blank.");
            }
            this.expectedVersion = expectedVersion;
            this.participatingModules = List.copyOf(participatingModules);
            return this;
        }

        @Override
        public ClientConfigurationResolver build() {
            for (ClientOption<?> option : unsupportedOptions.keySet()) {
                if (settings.containsKey(option)) {
                    throw new IllegalArgumentException(option.name() + " cannot be both a setting and unsupported.");
                }
            }
            return new ClientConfigurationResolver(this);
        }
    }
}
