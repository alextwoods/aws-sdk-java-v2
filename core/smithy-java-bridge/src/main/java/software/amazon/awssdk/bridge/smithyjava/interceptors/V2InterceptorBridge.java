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
import software.amazon.smithy.java.client.core.interceptors.ClientInterceptor;
import software.amazon.smithy.java.client.core.interceptors.InputHook;
import software.amazon.smithy.java.client.core.interceptors.OutputHook;
import software.amazon.smithy.java.client.core.interceptors.RequestHook;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.ModifiableHttpRequest;

/**
 * Bridges an AWS SDK v2-style {@link V2ExecutionInterceptor} into smithy-java's pipeline by
 * implementing smithy-java's {@link ClientInterceptor} and forwarding each hook to the v2
 * equivalent.
 *
 * <p>This is the crux of "the v2 SDK becomes a shell over smithy-java": customer-authored
 * v2 interceptors keep working, executed by the smithy-java engine. A full migration would
 * implement the complete 13-hook mapping (and {@code ExecutionAttributes <-> Context}); this
 * maps the representative subset, including the <b>modify</b> path that mutates the wire.
 *
 * <h2>Hook mapping</h2>
 * <pre>
 *   smithy-java ClientInterceptor        AWS SDK v2 ExecutionInterceptor
 *   ----------------------------         -------------------------------
 *   readBeforeExecution            --&gt;   beforeExecution
 *   modifyBeforeSigning (mutate)   --&gt;   modifyHttpRequest   (add header here)
 *   readBeforeTransmit             --&gt;   beforeTransmission
 *   readAfterExecution             --&gt;   afterExecution / onExecutionFailure
 * </pre>
 *
 * <p><b>Fidelity note:</b> v2's {@code modifyHttpRequest} runs <em>before</em> request
 * signing, so its mutations are part of the signed request. smithy-java's pipeline signs
 * between {@code modifyBeforeSigning} and {@code modifyBeforeTransmit}. We therefore map
 * {@code modifyHttpRequest} to {@code modifyBeforeSigning} (not {@code modifyBeforeTransmit})
 * to preserve v2 semantics — a header added by a v2 interceptor ends up signed, as it would
 * in the real SDK.
 */
@SdkPublicApi
public final class V2InterceptorBridge implements ClientInterceptor {

    private final V2ExecutionInterceptor v2;

    public V2InterceptorBridge(V2ExecutionInterceptor v2) {
        this.v2 = v2;
    }

    @Override
    public void readBeforeExecution(InputHook<?, ?> hook) {
        v2.beforeExecution(hook.operation().schema().id().getName());
    }

    /**
     * Maps smithy-java's {@code modifyBeforeSigning} to v2's {@code modifyHttpRequest}, so the
     * v2 interceptor's mutations are signed (see the fidelity note above). smithy-java's
     * request at this phase is an {@link HttpRequest}; we wrap it as a v2 {@code V2HttpRequest},
     * let the v2 interceptor mutate it, and hand the request back to the pipeline.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <RequestT> RequestT modifyBeforeSigning(RequestHook<?, ?, RequestT> hook) {
        if (hook.request() instanceof HttpRequest httpRequest) {
            ModifiableHttpRequest modifiable = httpRequest.toModifiable();
            v2.modifyHttpRequest(wrap(modifiable));
            return (RequestT) modifiable;
        }
        return hook.request();
    }

    @Override
    public void readBeforeTransmit(RequestHook<?, ?, ?> hook) {
        if (hook.request() instanceof HttpRequest httpRequest) {
            v2.beforeTransmission(wrap(httpRequest.toModifiable()));
        }
    }

    @Override
    public void readAfterExecution(OutputHook<?, ?, ?, ?> hook, RuntimeException error) {
        v2.afterExecution(hook.operation().schema().id().getName(), error);
    }

    /** Adapts a live smithy-java {@link ModifiableHttpRequest} to the v2 {@code V2HttpRequest} view. */
    private static V2ExecutionInterceptor.V2HttpRequest wrap(ModifiableHttpRequest req) {
        return new V2ExecutionInterceptor.V2HttpRequest() {
            @Override
            public String method() {
                return req.method();
            }

            @Override
            public String uri() {
                return req.uri().toString();
            }

            @Override
            public V2ExecutionInterceptor.V2HttpRequest addHeader(String name, String value) {
                req.headers().addHeader(name, value); // mutation lands on the real request -> the wire
                return this;
            }
        };
    }
}
