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

package software.amazon.awssdk.protocols.query;

import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.ModeledProtocol;
import software.amazon.awssdk.protocols.core.ProtocolContext;
import software.amazon.awssdk.protocols.core.ProtocolIds;
import software.amazon.awssdk.protocols.core.ProtocolProvider;
import software.amazon.awssdk.protocols.core.ServiceProtocols;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The {@code aws.protocols#awsQuery} runtime: form-encoded requests ({@link QueryFormRules#AWS_QUERY}) and XML
 * responses wrapped in {@code <OpResponse><OpResult>} (see {@link QueryClientProtocol}).
 */
public final class AwsQueryProtocolProvider implements ProtocolProvider {
    @Override
    public ShapeId protocolId() {
        return ProtocolIds.AWS_QUERY;
    }

    /** False: awsQuery serializes the whole input into the form body and ignores HTTP binding traits. */
    @Override
    public boolean usesHttpBindingTraits() {
        return false;
    }

    /** Requires the service version, which every request sends as {@code Version}. */
    @Override
    public void validate(ServiceProtocols service, ModeledProtocol declaration) {
        QueryClientProtocol.requireServiceVersion(service, declaration);
    }

    @Override
    public ClientProtocol createClientProtocol(ProtocolContext context) {
        if (!context.selected().protocolId().equals(ProtocolIds.AWS_QUERY)) {
            throw new IllegalArgumentException("The selected protocol is " + context.selected().protocolId()
                                               + ", not " + ProtocolIds.AWS_QUERY);
        }
        return QueryClientProtocol.create(context, QueryFormRules.AWS_QUERY,
                                          QueryClientProtocol.ResultWrapping.OPERATION_RESULT);
    }

    @Override
    public String toString() {
        return "AwsQueryProtocolProvider[" + ProtocolIds.AWS_QUERY + "]";
    }
}
