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

package software.amazon.awssdk.core.client.protocol;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.Trait;

/**
 * Creates the {@link ClientProtocol} for one protocol definition trait.
 *
 * <p>Generated clients reference the factory of their default protocol directly. To make a factory discoverable by
 * its protocol ID through {@link ClientProtocolFactories#forId(ShapeId)}, list its class in
 * {@code META-INF/services/software.amazon.awssdk.core.client.protocol.ClientProtocolFactory}; a factory registered
 * that way needs a public no-argument constructor. At most one factory may be registered per protocol ID.
 *
 * <p>Implementations must be safe to use from multiple threads.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.client.core.ClientProtocolFactory}.
 *
 * @param <T> the type of the protocol definition trait
 */
@SdkPublicApi
public interface ClientProtocolFactory<T extends Trait> {
    /**
     * Returns the ID of the protocol the factory creates.
     *
     * @return the shape ID of the protocol definition trait, for example {@code aws.protocols#restJson1}, which equals
     *     the {@link ClientProtocol#id()} of every protocol the factory creates
     */
    ShapeId id();

    /**
     * Creates a protocol.
     *
     * @param settings the settings of the service the protocol is created for
     * @param protocolTrait the protocol definition trait, as applied to the service or built from defaults when the
     *     service does not declare the protocol
     * @return a new protocol whose {@link ClientProtocol#id()} equals {@link #id()}
     */
    ClientProtocol<?, ?> createProtocol(ProtocolSettings settings, T protocolTrait);
}
