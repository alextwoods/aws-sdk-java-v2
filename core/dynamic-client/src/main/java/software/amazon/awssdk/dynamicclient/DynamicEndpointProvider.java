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

package software.amazon.awssdk.dynamicclient;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointProvider;

/**
 * Resolves the endpoint of each {@link DynamicClient} call.
 *
 * <p>Configure one with {@link DynamicClient.Builder#endpointProvider(DynamicEndpointProvider)} to replace the endpoint
 * rules of the service's model for every call. The dynamic counterpart of a generated client's
 * {@code <Service>EndpointProvider}, with the same method shape.
 *
 * <p>SDK addition (design C.2); smithy-java's counterpart is {@code EndpointResolver}.
 */
@SdkPublicApi
@ThreadSafe
@FunctionalInterface
public interface DynamicEndpointProvider extends EndpointProvider {
    /**
     * Resolves the endpoint of a call.
     *
     * @param endpointParams the operation, input and execution attributes of the call
     * @return a future that completes with the endpoint, or completes exceptionally when no endpoint can be resolved
     */
    CompletableFuture<Endpoint> resolveEndpoint(DynamicEndpointParams endpointParams);

    /**
     * Resolves the endpoint of a call, with parameters built by a consumer.
     *
     * @param endpointParamsConsumer configures a {@link DynamicEndpointParams.Builder}
     * @return a future that completes with the endpoint, or completes exceptionally when no endpoint can be resolved
     */
    default CompletableFuture<Endpoint> resolveEndpoint(Consumer<DynamicEndpointParams.Builder> endpointParamsConsumer) {
        DynamicEndpointParams.Builder builder = DynamicEndpointParams.builder();
        endpointParamsConsumer.accept(builder);
        return resolveEndpoint(builder.build());
    }
}
