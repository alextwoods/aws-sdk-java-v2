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

package software.amazon.awssdk.pipeline.client;

import java.util.Objects;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.pipeline.AwsCallEngine;
import software.amazon.awssdk.pipeline.ClientRuntime;

/**
 * What a generated default client is built from: its service definition, its {@link ClientRuntime} (resolved
 * configuration, protocol, plans and owned resources) and the {@link AwsCallEngine} its operations call directly.
 * Immutable and thread safe; {@link #close()} closes the runtime's SDK-owned resources exactly once.
 */
public final class AwsClientParts implements AutoCloseable {
    private final AwsServiceClientDefinition service;
    private final ClientRuntime runtime;
    private final AwsCallEngine engine;

    AwsClientParts(AwsServiceClientDefinition service, ClientRuntime runtime, AwsCallEngine engine) {
        this.service = Objects.requireNonNull(service, "service");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    public AwsServiceClientDefinition service() {
        return service;
    }

    public ClientRuntime runtime() {
        return runtime;
    }

    public AwsCallEngine engine() {
        return engine;
    }

    /** The exact resolved configuration every call of the client reads. */
    public SdkClientConfiguration configuration() {
        return runtime.configuration();
    }

    @Override
    public void close() {
        runtime.close();
    }
}
