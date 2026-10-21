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

import java.util.List;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;

/**
 * Resolves the auth scheme options of each {@link DynamicClient} call, in priority order.
 *
 * <p>Configure one with {@link DynamicClient.Builder#authSchemeResolver(DynamicAuthSchemeResolver)} to replace the
 * auth schemes of the service's model for every call. The dynamic counterpart of a generated client's
 * {@code <Service>AuthSchemeProvider}, with the same method shape.
 *
 * <p>SDK addition (design C.2); smithy-java's counterpart is {@code AuthSchemeResolver}.
 */
@SdkPublicApi
@ThreadSafe
@FunctionalInterface
public interface DynamicAuthSchemeResolver extends AuthSchemeProvider {
    /**
     * Resolves the auth scheme options of a call.
     *
     * @param authSchemeParams the protocol, operation and execution attributes of the call
     * @return the auth scheme options, in priority order
     */
    List<AuthSchemeOption> resolveAuthScheme(DynamicAuthSchemeParams authSchemeParams);

    /**
     * Resolves the auth scheme options of a call, with parameters built by a consumer.
     *
     * @param authSchemeParamsConsumer configures a {@link DynamicAuthSchemeParams.Builder}
     * @return the auth scheme options, in priority order
     */
    default List<AuthSchemeOption> resolveAuthScheme(Consumer<DynamicAuthSchemeParams.Builder> authSchemeParamsConsumer) {
        DynamicAuthSchemeParams.Builder builder = DynamicAuthSchemeParams.builder();
        authSchemeParamsConsumer.accept(builder);
        return resolveAuthScheme(builder.build());
    }
}
