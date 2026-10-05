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

import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The entry point of one installed protocol runtime, for one protocol trait.
 *
 * <p>Each protocol module registers one provider per protocol it implements as a
 * {@code META-INF/services/software.amazon.awssdk.protocols.core.ProtocolProvider} entry, which
 * {@link InstalledProtocols#discover(ClassLoader)} finds at client construction; a caller may also pass a provider
 * explicitly. Implementations must be immutable, thread safe and have a public no-argument constructor.
 *
 * <p>This contract covers selection, construction-time validation and, through
 * {@link #createClientProtocol(ProtocolContext)}, the per-client Schema SERDE runtime ({@link ClientProtocol}) whose
 * per-operation {@link OperationProtocol} plans serialize requests and deserialize responses and errors.
 */
public interface ProtocolProvider {

    /** Absolute shape ID of the protocol trait this provider implements, for example {@code aws.protocols#restXml}. */
    ShapeId protocolId();

    /**
     * True if the protocol binds members to HTTP message locations with the Smithy HTTP binding traits
     * ({@code restJson1}, {@code restXml}); false if it ignores them and serializes the whole input into the body
     * ({@code awsJson1_*}, {@code awsQuery}, {@code ec2Query}, {@code rpcv2Cbor}).
     */
    boolean usesHttpBindingTraits();

    /**
     * What the protocol requires of the transport when a service declares it as {@code declaration}. The default
     * derives HTTP requirements from the declaration's {@code http}/{@code eventStreamHttp} options.
     */
    default ProtocolTransportRequirements transportRequirements(ModeledProtocol declaration) {
        return ProtocolTransportRequirements.http(declaration);
    }

    /**
     * Validates, at client construction, that this runtime can serve {@code service} with {@code declaration} (for
     * example that required service data such as an XML namespace is present). The default accepts every service.
     *
     * @throws ProtocolSelectionException if it cannot
     */
    default void validate(ServiceProtocols service, ModeledProtocol declaration) {
    }

    /**
     * Creates the Schema SERDE runtime of this protocol for one client (task 4.2). Called once per client, after
     * {@link #validate}, with the client's {@link SelectedProtocol}; the result is held by the client.
     *
     * <p>The default throws: a provider that only participates in selection (for example a test double) need not
     * implement it. Every launch protocol module implements it.
     *
     * @throws UnsupportedOperationException if this provider has no SERDE runtime
     */
    default ClientProtocol createClientProtocol(ProtocolContext context) {
        throw new UnsupportedOperationException("Protocol provider " + getClass().getName() + " for " + protocolId()
                                                + " has no request/response runtime");
    }
}
