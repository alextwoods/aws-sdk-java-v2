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
import software.amazon.awssdk.core.resource.ResourceOwnership;

/**
 * Where a resolved configuration value came from (Requirement 9.3).
 *
 * <p>Constants are declared in precedence order, highest first: when several sources provide one setting, the value of
 * the earliest-declared present source wins:
 *
 * <pre>
 * request override &gt; explicit builder &gt; JVM property &gt; environment &gt; profile &gt; SDK default
 * </pre>
 */
public enum ConfigurationSource {
    /** A per-call request override; it applies to one call only. */
    REQUEST_OVERRIDE(ResourceOwnership.CALLER_OWNED),

    /** A value set explicitly on the client builder. */
    BUILDER(ResourceOwnership.CALLER_OWNED),

    /** A JVM system property, for example {@code aws.region}. */
    JVM_PROPERTY(ResourceOwnership.SDK_OWNED),

    /** An environment variable, for example {@code AWS_REGION}. */
    ENVIRONMENT(ResourceOwnership.SDK_OWNED),

    /** A property of the active profile in the shared configuration files. */
    PROFILE(ResourceOwnership.SDK_OWNED),

    /** The SDK default. */
    DEFAULT(ResourceOwnership.SDK_OWNED);

    private final ResourceOwnership defaultOwnership;

    ConfigurationSource(ResourceOwnership defaultOwnership) {
        this.defaultOwnership = defaultOwnership;
    }

    /** Whether a value from this source wins over a value from {@code other}. A source never outranks itself. */
    public boolean outranks(ConfigurationSource other) {
        return ordinal() < Objects.requireNonNull(other, "other").ordinal();
    }

    /**
     * Who owns a value from this source unless the setter says otherwise: values the caller passes (request overrides
     * and builder values) are caller-owned; values the SDK creates from text or defaults are SDK-owned. A builder
     * setter that takes a resource builder or factory records {@link ResourceOwnership#SDK_OWNED} explicitly.
     */
    public ResourceOwnership defaultOwnership() {
        return defaultOwnership;
    }
}
