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

package software.amazon.awssdk.protocols.restjson;

import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.ProtocolContext;
import software.amazon.awssdk.protocols.core.ProtocolIds;
import software.amazon.awssdk.protocols.core.ProtocolProvider;
import software.amazon.smithy.model.shapes.ShapeId;

/** The {@code aws.protocols#restJson1} runtime (see {@link RestJsonClientProtocol} for the wire rules). */
public final class RestJsonProtocolProvider implements ProtocolProvider {
    @Override
    public ShapeId protocolId() {
        return ProtocolIds.REST_JSON_1;
    }

    /** True: restJson1 binds members to HTTP message locations with the Smithy HTTP binding traits. */
    @Override
    public boolean usesHttpBindingTraits() {
        return true;
    }

    @Override
    public ClientProtocol createClientProtocol(ProtocolContext context) {
        if (!context.selected().protocolId().equals(ProtocolIds.REST_JSON_1)) {
            throw new IllegalArgumentException("The selected protocol is " + context.selected().protocolId()
                                               + ", not " + ProtocolIds.REST_JSON_1);
        }
        return new RestJsonClientProtocol(context);
    }

    @Override
    public String toString() {
        return "RestJsonProtocolProvider[" + ProtocolIds.REST_JSON_1 + "]";
    }
}
