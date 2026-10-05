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

package software.amazon.awssdk.core.override;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.config.TimeoutConfiguration;
import software.amazon.awssdk.core.context.ContextKey;

/**
 * The configuration one call runs with: the client's configuration plus the call's sparse request-override deltas
 * (Requirements 9.4, 9.11, 9.12; design "Request overrides" and {@code CallState.options}).
 *
 * <p>A call options value holds the client's {@link SdkClientConfiguration} by reference and only the options the call
 * overrides, already merged by each {@link CallOption}'s rule. {@link #get(ClientOption)} returns the merged value
 * for an overridden option and reads through to the client configuration for every other option. Building one costs
 * time and memory proportional to the override, never to the client configuration, and nothing is serialized.
 *
 * <p>Header, raw query parameter and execution attribute overrides are request-level deltas that the pipeline applies
 * to the marshalled request and the execution context; they replace same-named (headers: case-insensitively) or
 * same-keyed values. Client-scoped options (every {@link ClientOption} without a {@link CallOption}) always read the
 * client's value.
 *
 * <p>Generated clients create {@link #of(SdkClientConfiguration)} once per client and call
 * {@link #withOverrides(RequestOverrideConfiguration, RequestOverrideConfiguration)} once per call, which returns the
 * client instance itself when the call overrides nothing. Immutable and safe to share; no value leaks between calls.
 */
public final class CallOptions {
    private static final ClientOption<?>[] NO_KEYS = new ClientOption<?>[0];
    private static final Object[] NO_VALUES = new Object[0];

    private final SdkClientConfiguration clientConfiguration;
    private final ClientOption<?>[] overriddenKeys;
    private final Object[] overriddenValues;
    private final Map<String, List<String>> headers;
    private final Map<String, List<String>> rawQueryParameters;
    private final Map<ContextKey<?>, Object> executionAttributes;

    private CallOptions(SdkClientConfiguration clientConfiguration, ClientOption<?>[] overriddenKeys,
                        Object[] overriddenValues, Map<String, List<String>> headers,
                        Map<String, List<String>> rawQueryParameters, Map<ContextKey<?>, Object> executionAttributes) {
        this.clientConfiguration = clientConfiguration;
        this.overriddenKeys = overriddenKeys;
        this.overriddenValues = overriddenValues;
        this.headers = headers;
        this.rawQueryParameters = rawQueryParameters;
        this.executionAttributes = executionAttributes;
    }

    /** The call options of a call without overrides: every read goes to {@code clientConfiguration}. */
    public static CallOptions of(SdkClientConfiguration clientConfiguration) {
        return new CallOptions(Objects.requireNonNull(clientConfiguration, "clientConfiguration"), NO_KEYS, NO_VALUES,
                               Map.of(), Map.of(), Map.of());
    }

    /** {@code clientConfiguration} with {@code override} layered over it; {@code null} means no override. */
    public static CallOptions of(SdkClientConfiguration clientConfiguration, RequestOverrideConfiguration override) {
        return of(clientConfiguration).withOverrides(null, override);
    }

    /**
     * These options with the request-embedded override and then the explicit operation-overload override layered over
     * them, so for each key the explicit override wins over the embedded one, which wins over the client's value
     * (appending options accumulate in that order). Either override may be {@code null}.
     *
     * <p>This is the helper generated operation overloads reach through the call engine:
     * {@code getItem(request)} passes the request's own override, and {@code getItem(request, override)} passes both.
     *
     * @return {@code this} when neither override sets anything
     */
    public CallOptions withOverrides(RequestOverrideConfiguration embedded, RequestOverrideConfiguration explicit) {
        boolean applyEmbedded = embedded != null && !embedded.isEmpty();
        boolean applyExplicit = explicit != null && !explicit.isEmpty();
        if (!applyEmbedded && !applyExplicit) {
            return this;
        }
        Layering layering = new Layering(this);
        if (applyEmbedded) {
            layering.apply(embedded);
        }
        if (applyExplicit) {
            layering.apply(explicit);
        }
        return layering.build();
    }

    /** The client configuration these options read through to; never modified by a call. */
    public SdkClientConfiguration clientConfiguration() {
        return clientConfiguration;
    }

    /** The value the call uses for {@code option}: the merged override if the call overrides it, else the client's. */
    public <T> Optional<T> get(ClientOption<T> option) {
        int index = indexOf(Objects.requireNonNull(option, "option"));
        return index >= 0 ? Optional.of(option.cast(overriddenValues[index])) : clientConfiguration.get(option);
    }

    /** Whether the call overrides {@code option}. */
    public boolean isOverridden(ClientOption<?> option) {
        return indexOf(Objects.requireNonNull(option, "option")) >= 0;
    }

    /** The merged value of every overridden option, in first-override order. Immutable; empty without overrides. */
    public Map<ClientOption<?>, Object> overriddenOptions() {
        if (overriddenKeys.length == 0) {
            return Map.of();
        }
        Map<ClientOption<?>, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < overriddenKeys.length; i++) {
            result.put(overriddenKeys[i], overriddenValues[i]);
        }
        return Collections.unmodifiableMap(result);
    }

    /** The timeouts the call uses: per-call timeouts merged field by field over the client's. */
    public TimeoutConfiguration timeouts() {
        return get(SdkClientOption.TIMEOUT_CONFIGURATION).orElse(TimeoutConfiguration.none());
    }

    /** Header deltas, case-insensitive by name. Immutable. */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /** Raw query parameter deltas. Immutable. */
    public Map<String, List<String>> rawQueryParameters() {
        return rawQueryParameters;
    }

    /** Execution attributes the call's execution context starts with. Immutable. */
    public Map<ContextKey<?>, Object> executionAttributes() {
        return executionAttributes;
    }

    /** The execution attribute {@code key}, if the call sets it. */
    public <T> Optional<T> executionAttribute(ContextKey<T> key) {
        return Optional.ofNullable(key.cast(executionAttributes.get(Objects.requireNonNull(key, "key"))));
    }

    /** Whether the call overrides anything. */
    public boolean hasOverrides() {
        return overriddenKeys.length > 0 || !headers.isEmpty() || !rawQueryParameters.isEmpty()
               || !executionAttributes.isEmpty();
    }

    private int indexOf(ClientOption<?> option) {
        for (int i = 0; i < overriddenKeys.length; i++) {
            if (overriddenKeys[i] == option) {
                return i;
            }
        }
        return -1;
    }

    /** Renders names and keys only: override values, header values and credentials may be secret. */
    @Override
    public String toString() {
        List<String> names = new ArrayList<>(overriddenKeys.length);
        for (ClientOption<?> key : overriddenKeys) {
            names.add(key.name());
        }
        return "CallOptions(overridden=" + names + ", headers=" + headers.keySet() + ", rawQueryParameters="
               + rawQueryParameters.keySet() + ", executionAttributes=" + executionAttributes.keySet() + ")";
    }

    /** Accumulates override layers; sized by the overrides, not the client configuration. */
    private static final class Layering {
        private final SdkClientConfiguration clientConfiguration;
        private ClientOption<?>[] keys;
        private Object[] values;
        private int size;
        private Map<String, List<String>> headers;
        private Map<String, List<String>> rawQueryParameters;
        private Map<ContextKey<?>, Object> executionAttributes;

        private Layering(CallOptions start) {
            this.clientConfiguration = start.clientConfiguration;
            this.keys = Arrays.copyOf(start.overriddenKeys, start.overriddenKeys.length + 4);
            this.values = Arrays.copyOf(start.overriddenValues, start.overriddenValues.length + 4);
            this.size = start.overriddenKeys.length;
            this.headers = start.headers;
            this.rawQueryParameters = start.rawQueryParameters;
            this.executionAttributes = start.executionAttributes;
        }

        private void apply(RequestOverrideConfiguration override) {
            override.callOptions().forEach(this::put);
            if (!override.headers().isEmpty()) {
                Map<String, List<String>> merged = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                merged.putAll(headers);
                override.headers().forEach((name, value) -> {
                    merged.remove(name);
                    merged.put(name, value);
                });
                headers = Collections.unmodifiableMap(merged);
            }
            if (!override.rawQueryParameters().isEmpty()) {
                Map<String, List<String>> merged = new LinkedHashMap<>(rawQueryParameters);
                merged.putAll(override.rawQueryParameters());
                rawQueryParameters = Collections.unmodifiableMap(merged);
            }
            if (!override.executionAttributes().isEmpty()) {
                Map<ContextKey<?>, Object> merged = new LinkedHashMap<>(executionAttributes);
                merged.putAll(override.executionAttributes());
                executionAttributes = Collections.unmodifiableMap(merged);
            }
        }

        private <T> void put(CallOption<T> option, Object delta) {
            ClientOption<T> key = option.clientOption();
            int index = -1;
            for (int i = 0; i < size; i++) {
                if (keys[i] == key) {
                    index = i;
                    break;
                }
            }
            T current = index >= 0 ? key.cast(values[index]) : clientConfiguration.get(key).orElse(null);
            T merged = option.merge(current, option.validate(delta));
            if (index >= 0) {
                values[index] = merged;
                return;
            }
            if (size == keys.length) {
                keys = Arrays.copyOf(keys, size * 2);
                values = Arrays.copyOf(values, size * 2);
            }
            keys[size] = key;
            values[size] = merged;
            size++;
        }

        private CallOptions build() {
            return new CallOptions(clientConfiguration, Arrays.copyOf(keys, size), Arrays.copyOf(values, size),
                                   headers, rawQueryParameters, executionAttributes);
        }
    }
}
