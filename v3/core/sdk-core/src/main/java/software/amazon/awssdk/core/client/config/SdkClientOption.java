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

import java.net.URI;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The protocol-agnostic {@link ClientOption} keys of an {@link SdkClientConfiguration}.
 *
 * <p>Modules that own a setting's type declare its key next to the type (for example AWS region and credentials keys
 * in {@code aws-core}); this class holds only keys whose value types live in {@code sdk-core} or the Smithy runtime.
 */
public final class SdkClientOption {
    /** The endpoint every call starts from: the endpoint override, or the resolved default endpoint. */
    public static final ClientOption<URI> CLIENT_ENDPOINT = ClientOption.create("ClientEndpoint", URI.class);

    /** The client-level API-call and API-call-attempt timeouts. */
    public static final ClientOption<TimeoutConfiguration> TIMEOUT_CONFIGURATION =
        ClientOption.create("TimeoutConfiguration", TimeoutConfiguration.class);

    /** Shape ID of the protocol the client was built with, for example {@code aws.protocols#restXml}. */
    public static final ClientOption<ShapeId> PROTOCOL_ID = ClientOption.create("ProtocolId", ShapeId.class);

    private SdkClientOption() {
    }
}
