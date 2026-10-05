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

package software.amazon.awssdk.protocols.awsjson;

import java.util.Objects;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.ProtocolContext;
import software.amazon.awssdk.protocols.core.ProtocolProvider;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The provider of one AWS JSON protocol version; {@link AwsJson10ProtocolProvider} and
 * {@link AwsJson11ProtocolProvider} are the registered service entries.
 */
public abstract class AwsJsonProtocolProvider implements ProtocolProvider {
    private final ShapeId protocolId;
    private final String contentType;

    AwsJsonProtocolProvider(ShapeId protocolId, String version) {
        this.protocolId = Objects.requireNonNull(protocolId, "protocolId");
        this.contentType = "application/x-amz-json-" + version;
    }

    @Override
    public final ShapeId protocolId() {
        return protocolId;
    }

    /** False: AWS JSON serializes the whole input into the body and ignores HTTP binding traits. */
    @Override
    public final boolean usesHttpBindingTraits() {
        return false;
    }

    @Override
    public final ClientProtocol createClientProtocol(ProtocolContext context) {
        if (!context.selected().protocolId().equals(protocolId)) {
            throw new IllegalArgumentException("The selected protocol is " + context.selected().protocolId()
                                               + ", not " + protocolId);
        }
        return new AwsJsonClientProtocol(context, contentType);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + protocolId + "]";
    }
}
