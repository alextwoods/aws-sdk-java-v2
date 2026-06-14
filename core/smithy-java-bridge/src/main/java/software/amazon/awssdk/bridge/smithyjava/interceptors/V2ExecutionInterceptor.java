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

package software.amazon.awssdk.bridge.smithyjava.interceptors;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A miniature of the AWS SDK for Java <b>v2</b> {@code ExecutionInterceptor} SPI
 * ({@code software.amazon.awssdk.core.interceptor.ExecutionInterceptor}).
 *
 * <p>The real interface has 13 hooks taking {@code Context.*} + {@code ExecutionAttributes}.
 * This models the representative subset, keeping the v2 names and the "read vs. modify"
 * split so the shape is recognizably v2. {@link V2InterceptorBridge} adapts implementations
 * of this onto smithy-java's {@code ClientInterceptor}.
 */
@SdkPublicApi
public interface V2ExecutionInterceptor {

    /** Mirrors v2 {@code beforeExecution(Context.BeforeExecution, ExecutionAttributes)}. */
    default void beforeExecution(String operationName) {
    }

    /** Mirrors v2 {@code beforeTransmission(...)} — observe the HTTP request before it's sent. */
    default void beforeTransmission(V2HttpRequest httpRequest) {
    }

    /**
     * Mirrors v2 {@code modifyHttpRequest(Context.ModifyHttpRequest, ExecutionAttributes)} —
     * return a (possibly mutated) HTTP request. The default is identity.
     */
    default V2HttpRequest modifyHttpRequest(V2HttpRequest httpRequest) {
        return httpRequest;
    }

    /** Mirrors v2 {@code afterExecution(...)} / {@code onExecutionFailure(...)}. */
    default void afterExecution(String operationName, Throwable error) {
    }

    /**
     * A tiny stand-in for v2's {@code SdkHttpRequest} as seen by an interceptor: just enough
     * surface (add a header) to prove a mutation made through the v2 API reaches the wire.
     * The bridge backs this by a live smithy-java {@code ModifiableHttpRequest}.
     */
    interface V2HttpRequest {
        String method();

        String uri();

        /** v2-style: returns a request with the header added (here: mutates the backing request). */
        V2HttpRequest addHeader(String name, String value);
    }
}
