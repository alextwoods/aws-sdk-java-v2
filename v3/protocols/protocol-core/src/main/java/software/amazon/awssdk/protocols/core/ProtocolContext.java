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

package software.amazon.awssdk.protocols.core;

import java.util.Objects;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;

/**
 * What a protocol runtime is given, once per client, to create its {@link ClientProtocol}.
 *
 * @param selected the client's selected protocol, with the service's declared protocols and awsQuery error data
 * @param typeRegistry the service's type registry, through which modeled errors are resolved by shape ID and built
 * @param hostPrefixInjection true to apply {@code smithy.api#endpoint} host prefixes to request hosts (the client
 *        option that disables host-prefix injection sets it false)
 */
public record ProtocolContext(SelectedProtocol selected, SchemaTypeRegistry typeRegistry,
                              boolean hostPrefixInjection) {
    public ProtocolContext {
        Objects.requireNonNull(selected, "selected");
        Objects.requireNonNull(typeRegistry, "typeRegistry");
    }

    /** A context with host-prefix injection enabled. */
    public static ProtocolContext of(SelectedProtocol selected, SchemaTypeRegistry typeRegistry) {
        return new ProtocolContext(selected, typeRegistry, true);
    }

    /** The service's declared protocols. */
    public ServiceProtocols service() {
        return selected.service();
    }

    /** The selected protocol's declaration. */
    public ModeledProtocol declaration() {
        return selected.declaration();
    }
}
