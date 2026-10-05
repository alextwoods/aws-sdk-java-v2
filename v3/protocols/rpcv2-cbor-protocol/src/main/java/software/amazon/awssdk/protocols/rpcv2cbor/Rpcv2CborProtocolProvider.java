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

package software.amazon.awssdk.protocols.rpcv2cbor;

import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.ProtocolContext;
import software.amazon.awssdk.protocols.core.ProtocolIds;
import software.amazon.awssdk.protocols.core.ProtocolProvider;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The provider of the Smithy RPC v2 CBOR protocol, {@code smithy.protocols#rpcv2Cbor}, registered as a
 * {@code ProtocolProvider} service entry. See {@link Rpcv2CborClientProtocol} for the wire rules.
 */
public final class Rpcv2CborProtocolProvider implements ProtocolProvider {
    @Override
    public ShapeId protocolId() {
        return ProtocolIds.RPC_V2_CBOR;
    }

    /** False: rpcv2Cbor serializes the whole input into the body and ignores HTTP binding traits. */
    @Override
    public boolean usesHttpBindingTraits() {
        return false;
    }

    @Override
    public ClientProtocol createClientProtocol(ProtocolContext context) {
        if (!context.selected().protocolId().equals(ProtocolIds.RPC_V2_CBOR)) {
            throw new IllegalArgumentException("The selected protocol is " + context.selected().protocolId()
                                               + ", not " + ProtocolIds.RPC_V2_CBOR);
        }
        return new Rpcv2CborClientProtocol(context);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + ProtocolIds.RPC_V2_CBOR + "]";
    }
}
