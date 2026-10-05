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

package software.amazon.awssdk.endpoints.binding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.endpoints.EndpointBuiltIn;

/**
 * The client-scoped inputs of endpoint parameter binding: the values of endpoint rules built-ins (sourced from client
 * configuration, and for {@code AWS::Auth::AccountId} from the resolved identity), the values of the service's modeled
 * client context parameters, and whether host-prefix injection is disabled. Instances are immutable.
 */
public final class EndpointClientValues {
    private static final EndpointClientValues EMPTY = builder().build();

    private final Map<String, Object> builtIns;
    private final Map<String, Object> clientContextParams;
    private final boolean hostPrefixInjectionDisabled;

    private EndpointClientValues(Builder builder) {
        this.builtIns = Collections.unmodifiableMap(new LinkedHashMap<>(builder.builtIns));
        this.clientContextParams = Collections.unmodifiableMap(new LinkedHashMap<>(builder.clientContextParams));
        this.hostPrefixInjectionDisabled = builder.hostPrefixInjectionDisabled;
    }

    /** No built-in or client context parameter value, and host-prefix injection enabled. */
    public static EndpointClientValues empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.builtIns.putAll(builtIns);
        builder.clientContextParams.putAll(clientContextParams);
        builder.hostPrefixInjectionDisabled = hostPrefixInjectionDisabled;
        return builder;
    }

    /** The value of the built-in with ID {@code builtInId}, or {@code null} if it is unset. */
    public Object builtIn(String builtInId) {
        return builtIns.get(builtInId);
    }

    /** Every set built-in value by built-in ID. Immutable. */
    public Map<String, Object> builtIns() {
        return builtIns;
    }

    /** The value of client context parameter {@code name}, or {@code null} if it is unset. */
    public Object clientContextParam(String name) {
        return clientContextParams.get(name);
    }

    /** Every set client context parameter value by parameter name. Immutable. */
    public Map<String, Object> clientContextParams() {
        return clientContextParams;
    }

    /** Whether modeled host prefixes are not applied to resolved endpoints. */
    public boolean hostPrefixInjectionDisabled() {
        return hostPrefixInjectionDisabled;
    }

    /** Builder of {@link EndpointClientValues}. Not thread-safe. */
    public static final class Builder {
        private final Map<String, Object> builtIns = new LinkedHashMap<>();
        private final Map<String, Object> clientContextParams = new LinkedHashMap<>();
        private boolean hostPrefixInjectionDisabled;

        private Builder() {
        }

        /** Sets built-in {@code builtIn}; a {@code null} value unsets it. */
        public Builder builtIn(EndpointBuiltIn builtIn, Object value) {
            return builtIn(builtIn.id(), value);
        }

        /** Sets the built-in with ID {@code builtInId}; a {@code null} value unsets it. */
        public Builder builtIn(String builtInId, Object value) {
            put(builtIns, Objects.requireNonNull(builtInId, "builtInId"), value);
            return this;
        }

        /** Sets client context parameter {@code name}; a {@code null} value unsets it. */
        public Builder clientContextParam(String name, Object value) {
            put(clientContextParams, Objects.requireNonNull(name, "name"), value);
            return this;
        }

        public Builder hostPrefixInjectionDisabled(boolean disabled) {
            this.hostPrefixInjectionDisabled = disabled;
            return this;
        }

        public EndpointClientValues build() {
            return new EndpointClientValues(this);
        }

        private static void put(Map<String, Object> map, String key, Object value) {
            if (value == null) {
                map.remove(key);
            } else {
                map.put(key, value);
            }
        }
    }
}
