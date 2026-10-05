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
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The one protocol a client uses: a protocol the service's model declares, with the installed runtime that
 * implements it, as chosen by {@link ProtocolSelector}. The constructor rejects a provider of another protocol and a
 * declaration that is not the service's own.
 *
 * @param service the service's declared protocols
 * @param declaration the selected declared protocol, with its modeled options
 * @param provider the runtime that implements it
 * @param source how the protocol was chosen
 */
public record SelectedProtocol(ServiceProtocols service, ModeledProtocol declaration, ProtocolProvider provider,
                               Source source) {

    /** How the protocol was chosen. */
    public enum Source {
        /** The caller supplied the protocol runtime. */
        EXPLICIT_PROVIDER,
        /** The caller named the protocol and it was taken from the installed runtimes. */
        EXPLICIT_ID,
        /** The caller named none; the highest-precedence declared, installed protocol was taken. */
        MODEL_DEFAULT
    }

    public SelectedProtocol {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(declaration, "declaration");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(source, "source");
        if (!declaration.id().equals(provider.protocolId())) {
            throw new IllegalArgumentException("Provider " + provider.getClass().getName() + " implements "
                                               + provider.protocolId() + ", not " + declaration.id());
        }
        if (service.protocol(declaration.id()).orElse(null) != declaration) {
            throw new IllegalArgumentException(declaration.id() + " is not a protocol declared by "
                                               + service.serviceId());
        }
    }

    /** Absolute shape ID of the selected protocol trait. */
    public ShapeId protocolId() {
        return declaration.id();
    }

    /** What the selected protocol requires of the transport. */
    public ProtocolTransportRequirements transportRequirements() {
        return provider.transportRequirements(declaration);
    }

    /**
     * Creates the client's protocol runtime from the selected provider (once per client).
     *
     * @param typeRegistry the service's type registry
     * @param hostPrefixInjection whether {@code smithy.api#endpoint} host prefixes are applied
     */
    public ClientProtocol createClientProtocol(SchemaTypeRegistry typeRegistry, boolean hostPrefixInjection) {
        return provider.createClientProtocol(new ProtocolContext(this, typeRegistry, hostPrefixInjection));
    }
}
