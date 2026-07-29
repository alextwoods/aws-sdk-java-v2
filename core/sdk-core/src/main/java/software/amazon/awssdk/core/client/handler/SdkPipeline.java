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

package software.amazon.awssdk.core.client.handler;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Abstraction over the SDK's internal request/response execution pipeline. An {@code SdkPipeline}
 * handles the full lifecycle of a service call: marshalling, signing, transport, retry, and
 * unmarshalling.
 *
 * <p>The SDK ships a default implementation backed by the v2 runtime ({@link SyncClientHandler} /
 * {@link AsyncClientHandler}). Alternative implementations — such as one backed by smithy-java —
 * can be discovered at runtime via {@link SdkPipelineProvider} and Java's {@link java.util.ServiceLoader}.
 *
 * <p>When a higher-priority pipeline provider is on the classpath, the generated client will
 * delegate to it transparently, giving users performance or correctness improvements without
 * requiring source changes.
 *
 * <p>Implementations must be thread-safe and reusable across concurrent calls.
 */
@ThreadSafe
@SdkProtectedApi
public interface SdkPipeline extends SdkAutoCloseable {

    /**
     * Execute a non-streaming synchronous API call.
     *
     * @param executionParams  Operation metadata, marshaller, response handlers, and the input request.
     * @param clientConfiguration  The resolved client configuration for this call (may include per-request overrides).
     * @param <InputT>  The SDK request type.
     * @param <OutputT> The SDK response type.
     * @return The unmarshalled response.
     */
    <InputT extends SdkRequest, OutputT extends SdkResponse> OutputT execute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        SdkClientConfiguration clientConfiguration);

    /**
     * Returns {@code true} if this pipeline supports the given operation. Allows a pipeline
     * implementation to opt out of handling specific operations (e.g., streaming or event-stream
     * operations that it cannot yet handle), falling back to the default v2 pipeline.
     *
     * @param executionParams The execution parameters for the operation about to be invoked.
     * @return {@code true} if this pipeline can handle the operation; {@code false} to fall back.
     */
    default <InputT extends SdkRequest, OutputT extends SdkResponse> boolean supportsOperation(
        ClientExecutionParams<InputT, OutputT> executionParams) {
        return true;
    }
}
