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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.core.resource.ManagedResource;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The resolved configuration of one client: an immutable map from typed {@link ClientOption} keys to values and their
 * provenance, plus the resources the client was configured with and who owns each (Requirements 7.11, 9.1, 9.3, 9.6).
 *
 * <p>This is the storage behind every resolved client view (for example {@code AwsClientConfiguration}): a client
 * builds one instance at construction and every call and waiter reads that same instance. Settings whose types belong
 * to other modules (retry strategy, HTTP client, interceptors, telemetry, service-specific options) are stored under
 * {@link ClientOption} keys those modules declare and read through {@link #get(ClientOption)}.
 *
 * <p>Immutable and safe to publish. Values are stored as given: a value must itself be immutable or thread-safe.
 * {@link ClientConfigurationResolver} decides precedence between sources before this object is built and records, for
 * each value, the {@link ConfigurationSource} it came from; {@link #resolved(ClientOption)} exposes that provenance.
 * Closing resources is the client's job (see {@code core.resource.ResourceScope}), not this type's.
 *
 * <p>Equal when both resolved option maps (values and provenance) and resource lists are equal.
 */
public final class SdkClientConfiguration implements ToCopyableBuilder<SdkClientConfiguration.Builder,
                                                                       SdkClientConfiguration> {
    private static final SdkClientConfiguration EMPTY = new SdkClientConfiguration(new BuilderImpl());

    private final Map<ClientOption<?>, ResolvedOption<?>> options;
    private final List<ManagedResource<?>> resources;

    private SdkClientConfiguration(BuilderImpl builder) {
        this.options = builder.options.isEmpty() ? Map.of()
                                                 : Collections.unmodifiableMap(new LinkedHashMap<>(builder.options));
        this.resources = List.copyOf(builder.resources);
    }

    /** The configuration with no options and no resources. */
    public static SdkClientConfiguration empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    /** The value of {@code option}, or empty if it is not configured. */
    public <T> Optional<T> get(ClientOption<T> option) {
        return resolved(option).map(ResolvedOption::value);
    }

    /** The value of {@code option} with its source and ownership, or empty if it is not configured. */
    public <T> Optional<ResolvedOption<T>> resolved(ClientOption<T> option) {
        Objects.requireNonNull(option, "option");
        return Optional.ofNullable(typed(option, options.get(option)));
    }

    /** Whether {@code option} has a value. */
    public boolean contains(ClientOption<?> option) {
        return options.containsKey(Objects.requireNonNull(option, "option"));
    }

    /** Every configured option, in the order it was first set. Immutable. */
    public Set<ClientOption<?>> options() {
        return options.keySet();
    }

    /** Every configured resource and its ownership, in the order added. Immutable. */
    public List<ManagedResource<?>> resources() {
        return resources;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SdkClientConfiguration that
               && options.equals(that.options)
               && resources.equals(that.resources);
    }

    @Override
    public int hashCode() {
        return 31 * options.hashCode() + resources.hashCode();
    }

    /** Renders option names and resource types only: option values may be secret (Requirement 21.10). */
    @Override
    public String toString() {
        List<String> names = new ArrayList<>(options.size());
        options.keySet().forEach(option -> names.add(option.name()));
        return ToString.builder("SdkClientConfiguration")
                       .add("options", names.isEmpty() ? null : names)
                       .add("resources", resources.isEmpty() ? null : resources)
                       .build();
    }

    // Values are only stored by the typed builder methods, so a key always maps to a ResolvedOption of its own type.
    @SuppressWarnings("unchecked")
    private static <T> ResolvedOption<T> typed(ClientOption<T> option, ResolvedOption<?> resolved) {
        return (ResolvedOption<T>) resolved;
    }

    /** Builder of an {@link SdkClientConfiguration}. */
    public interface Builder extends CopyableBuilder<Builder, SdkClientConfiguration> {
        /**
         * Sets {@code option} to {@code value} as an explicit {@link ConfigurationSource#BUILDER} value; {@code null}
         * removes it. Equivalent to {@code option(option, value, ConfigurationSource.BUILDER)}.
         *
         * @throws ClassCastException if {@code value} is not an instance of the option's value type
         */
        <T> Builder option(ClientOption<T> option, T value);

        /**
         * Sets {@code option} to {@code value} taken from {@code source}, owned as the source's
         * {@link ConfigurationSource#defaultOwnership() default ownership}; {@code null} removes it.
         *
         * @throws ClassCastException if {@code value} is not an instance of the option's value type
         */
        <T> Builder option(ClientOption<T> option, T value, ConfigurationSource source);

        /** Sets {@link ResolvedOption#key()} to {@code resolved}, keeping its source and ownership. */
        <T> Builder option(ResolvedOption<T> resolved);

        /** The value set for {@code option}, or {@code null}. */
        <T> T option(ClientOption<T> option);

        /** The value set for {@code option} with its provenance, or {@code null}. */
        <T> ResolvedOption<T> resolved(ClientOption<T> option);

        /** Adds {@code resource} to the configured resources. */
        Builder addResource(ManagedResource<?> resource);

        /** Replaces every configured resource with {@code resources}. */
        Builder resources(List<? extends ManagedResource<?>> resources);

        List<ManagedResource<?>> resources();
    }

    private static final class BuilderImpl implements Builder {
        private final Map<ClientOption<?>, ResolvedOption<?>> options = new LinkedHashMap<>();
        private final List<ManagedResource<?>> resources = new ArrayList<>();

        private BuilderImpl() {
        }

        private BuilderImpl(SdkClientConfiguration configuration) {
            options.putAll(configuration.options);
            resources.addAll(configuration.resources);
        }

        @Override
        public <T> Builder option(ClientOption<T> option, T value) {
            return option(option, value, ConfigurationSource.BUILDER);
        }

        @Override
        public <T> Builder option(ClientOption<T> option, T value, ConfigurationSource source) {
            Objects.requireNonNull(option, "option");
            Objects.requireNonNull(source, "source");
            if (value == null) {
                options.remove(option);
                return this;
            }
            return option(ResolvedOption.of(option, value, source));
        }

        @Override
        public <T> Builder option(ResolvedOption<T> resolved) {
            Objects.requireNonNull(resolved, "resolved");
            options.put(resolved.key(), resolved);
            return this;
        }

        @Override
        public <T> T option(ClientOption<T> option) {
            ResolvedOption<T> resolved = resolved(option);
            return resolved == null ? null : resolved.value();
        }

        @Override
        public <T> ResolvedOption<T> resolved(ClientOption<T> option) {
            return typed(option, options.get(Objects.requireNonNull(option, "option")));
        }

        @Override
        public Builder addResource(ManagedResource<?> resource) {
            resources.add(Objects.requireNonNull(resource, "resource"));
            return this;
        }

        @Override
        public Builder resources(List<? extends ManagedResource<?>> resources) {
            this.resources.clear();
            resources.forEach(this::addResource);
            return this;
        }

        @Override
        public List<ManagedResource<?>> resources() {
            return Collections.unmodifiableList(resources);
        }

        @Override
        public SdkClientConfiguration build() {
            return options.isEmpty() && resources.isEmpty() ? EMPTY : new SdkClientConfiguration(this);
        }
    }
}
