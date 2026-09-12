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

package software.amazon.awssdk.core.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;

/**
 * Client options that are an implementation detail of the request pipeline: derived once from the public configuration
 * when the client is built, read on every call, and not part of any public or protected configuration surface.
 */
@SdkInternalApi
public final class SdkInternalClientOption<T> extends ClientOption<T> {
    /**
     * The {@link ExecutionInterceptorChain} for {@link SdkClientOption#EXECUTION_INTERCEPTORS}. The chain is immutable and
     * safe to share, but was being rebuilt (list copy plus per-hook override analysis) on every call. This is a lazy option
     * derived from {@code EXECUTION_INTERCEPTORS}, so a request-level plugin that changes the interceptor list gets a matching
     * chain.
     */
    public static final SdkInternalClientOption<ExecutionInterceptorChain> EXECUTION_INTERCEPTOR_CHAIN =
        new SdkInternalClientOption<>(ExecutionInterceptorChain.class);

    private SdkInternalClientOption(Class<T> valueClass) {
        super(valueClass);
    }

    /**
     * The interceptor chain for this configuration: the cached one when the client builder derived it, otherwise a fresh one
     * (configurations assembled by hand, as in tests, do not go through the builder).
     */
    public static ExecutionInterceptorChain interceptorChain(SdkClientConfiguration configuration) {
        ExecutionInterceptorChain chain = configuration.option(EXECUTION_INTERCEPTOR_CHAIN);
        return chain != null ? chain
                             : new ExecutionInterceptorChain(configuration.option(SdkClientOption.EXECUTION_INTERCEPTORS));
    }
}
