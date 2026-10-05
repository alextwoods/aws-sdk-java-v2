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

import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The Schema SERDE runtime of one protocol for one client (Requirements 5.1, 5.2): created once per client by
 * {@link ProtocolProvider#createClientProtocol(ProtocolContext)} from the {@link SelectedProtocol} and the service's
 * type registry, immutable and thread safe.
 *
 * <p>The pipeline (task 9.x) asks it for one {@link OperationProtocol} per operation when it builds the operation's
 * pipeline plan, and holds that plan for the client's lifetime; nothing here is looked up per call. No generated
 * per-operation marshaller or unmarshaller takes part (Requirements 5.3, 5.4): an operation is served entirely from
 * its {@link OperationSchemas}.
 */
public interface ClientProtocol {
    /** Absolute shape ID of the protocol trait. */
    ShapeId protocolId();

    /**
     * The precomputed SERDE plan of one operation of the client's service.
     *
     * <p>Operations with a streaming blob or event stream member are accepted here, so that constructing plans for a
     * whole client never fails because of them. Streaming blob payloads are served by the HTTP binding protocols and
     * event streams by every protocol except the query protocols; calling a plan with an event stream member under a
     * query protocol, or with a streaming blob member under a protocol without payload bindings, throws
     * {@link UnsupportedOperationException}.
     *
     * @throws IllegalArgumentException if the operation's schemas cannot be served by this protocol
     */
    OperationProtocol operation(OperationSchemas operation);
}
