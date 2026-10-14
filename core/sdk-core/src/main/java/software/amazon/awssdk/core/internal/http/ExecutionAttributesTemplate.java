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

package software.amazon.awssdk.core.internal.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.Validate;

/**
 * The execution attributes whose values depend only on the client configuration, read from the configuration once per
 * client and written into each call's {@link ExecutionAttributes} (racecar H10, re-implemented on the existing
 * {@code ExecutionAttributes} type: a call still gets an ordinary, mutable {@code ExecutionAttributes}).
 *
 * <p>The template holds the values the legacy client handlers ({@code AwsExecutionContextBuilder} and
 * {@code BaseClientHandler}) read from the client configuration on every call, plus the two schema-based values:
 * {@link SdkExecutionAttribute#CLIENT_PROTOCOL}, from {@link SdkClientOption#CLIENT_PROTOCOL} (the protocol the client
 * builder resolved), and {@link SdkExecutionAttribute#ERROR_REGISTRY}, the service's error registry when the client
 * has one. Values that change per call (the operation, the metric collector, the profile file read from its supplier,
 * the per-request auth scheme provider and identity providers) are not in the template. Modules above {@code sdk-core}
 * add their own client-level values through {@link Builder#put}.
 *
 * <p>{@link #applyTo} overwrites, which is the precedence the legacy handlers give these values over execution
 * attributes from the request and client override configuration.
 */
@SdkInternalApi
@ThreadSafe
public final class ExecutionAttributesTemplate {
    private final List<Seed<?>> seeds;

    private ExecutionAttributesTemplate(Builder builder) {
        this.seeds = Collections.unmodifiableList(new ArrayList<>(builder.seeds));
    }

    /**
     * Returns a builder seeded with the client-level values of the given configuration.
     *
     * @param clientConfiguration the client configuration; the template reads it only while building
     */
    public static Builder builder(SdkClientConfiguration clientConfiguration) {
        return new Builder(Validate.paramNotNull(clientConfiguration, "clientConfiguration"));
    }

    /**
     * Creates a new, mutable execution attributes object holding the template's values.
     */
    public ExecutionAttributes newExecutionAttributes() {
        ExecutionAttributes attributes = new ExecutionAttributes();
        applyTo(attributes);
        return attributes;
    }

    /**
     * Writes every value of the template into the given attributes, replacing a value already set for the same
     * attribute.
     *
     * @param attributes the call's execution attributes
     * @return the same attributes
     */
    public ExecutionAttributes applyTo(ExecutionAttributes attributes) {
        for (Seed<?> seed : seeds) {
            seed.applyTo(attributes);
        }
        return attributes;
    }

    /**
     * Returns the attributes the template writes, in the order it writes them.
     */
    public List<ExecutionAttribute<?>> attributes() {
        List<ExecutionAttribute<?>> attributes = new ArrayList<>(seeds.size());
        for (Seed<?> seed : seeds) {
            attributes.add(seed.attribute);
        }
        return attributes;
    }

    /**
     * Builds an {@link ExecutionAttributesTemplate}.
     */
    public static final class Builder {
        private final List<Seed<?>> seeds = new ArrayList<>();

        private Builder(SdkClientConfiguration config) {
            put(SdkExecutionAttribute.SERVICE_CONFIG, config.option(SdkClientOption.SERVICE_CONFIGURATION));
            put(SdkExecutionAttribute.SERVICE_NAME, config.option(SdkClientOption.SERVICE_NAME));
            put(SdkExecutionAttribute.CLIENT_TYPE, config.option(SdkClientOption.CLIENT_TYPE));
            put(SdkExecutionAttribute.PROFILE_FILE_SUPPLIER, config.option(SdkClientOption.PROFILE_FILE_SUPPLIER));
            put(SdkExecutionAttribute.PROFILE_NAME, config.option(SdkClientOption.PROFILE_NAME));
            put(SdkInternalExecutionAttribute.NEW_RETRIES_2026_ENABLED, config.option(SdkClientOption.NEW_RETRIES_2026_ENABLED));
            put(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER, config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER));
            put(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS, config.option(SdkClientOption.CLIENT_CONTEXT_PARAMS));
            put(SdkInternalExecutionAttribute.DISABLE_HOST_PREFIX_INJECTION,
                config.option(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION));
            put(SdkInternalExecutionAttribute.SDK_CLIENT, config.option(SdkClientOption.SDK_CLIENT));
            put(SdkExecutionAttribute.SIGNER_OVERRIDDEN, config.option(SdkClientOption.SIGNER_OVERRIDDEN));
            put(SdkInternalExecutionAttribute.REQUEST_CHECKSUM_CALCULATION,
                config.option(SdkClientOption.REQUEST_CHECKSUM_CALCULATION));
            put(SdkInternalExecutionAttribute.RESPONSE_CHECKSUM_VALIDATION,
                config.option(SdkClientOption.RESPONSE_CHECKSUM_VALIDATION));
            put(SdkInternalExecutionAttribute.AUTH_SCHEMES, config.option(SdkClientOption.AUTH_SCHEMES));
            put(SdkExecutionAttribute.CLIENT_PROTOCOL, config.option(SdkClientOption.CLIENT_PROTOCOL));
        }

        /**
         * Adds the service's error registry as {@link SdkExecutionAttribute#ERROR_REGISTRY}. A generated client's
         * operations all use the service's registry, so it is a client-level value.
         *
         * @param errorRegistry the registry, or null to leave the attribute to the call
         * @return this builder
         */
        public Builder errorRegistry(TypeRegistry errorRegistry) {
            if (errorRegistry != null) {
                put(SdkExecutionAttribute.ERROR_REGISTRY, errorRegistry);
            }
            return this;
        }

        /**
         * Adds a client-level value. A later value for the same attribute replaces an earlier one.
         *
         * @param attribute the attribute
         * @param value the value, may be null (the attribute is then written as null, as the legacy handlers do for an
         *     unset option)
         * @param <T> the value type
         * @return this builder
         */
        public <T> Builder put(ExecutionAttribute<T> attribute, T value) {
            Validate.paramNotNull(attribute, "attribute");
            seeds.removeIf(seed -> seed.attribute == attribute);
            seeds.add(new Seed<>(attribute, value));
            return this;
        }

        public ExecutionAttributesTemplate build() {
            return new ExecutionAttributesTemplate(this);
        }
    }

    private static final class Seed<T> {
        private final ExecutionAttribute<T> attribute;
        private final T value;

        private Seed(ExecutionAttribute<T> attribute, T value) {
            this.attribute = attribute;
            this.value = value;
        }

        private void applyTo(ExecutionAttributes attributes) {
            attributes.putAttribute(attribute, value);
        }
    }
}
