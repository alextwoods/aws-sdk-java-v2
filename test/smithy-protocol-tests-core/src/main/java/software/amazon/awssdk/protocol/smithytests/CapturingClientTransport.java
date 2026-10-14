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

package software.amazon.awssdk.protocol.smithytests;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import software.amazon.awssdk.core.client.protocol.AsyncClientTransport;
import software.amazon.awssdk.core.client.protocol.AsyncResponseHandoff;
import software.amazon.awssdk.core.client.protocol.ClientTransport;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * A sync and async HTTP transport for request protocol tests: it records each request it is given, body included, and
 * fails the send with {@link RequestCaptured}. It never opens a network connection and never returns a response.
 *
 * <p>Thread safe.
 */
public final class CapturingClientTransport
    implements ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse>,
               AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> {

    private final List<CapturedRequest> requests = new CopyOnWriteArrayList<>();
    private final CompletableFuture<CapturedRequest> firstRequest = new CompletableFuture<>();

    private CapturingClientTransport() {
    }

    /**
     * A transport that has captured nothing yet.
     */
    public static CapturingClientTransport create() {
        return new CapturingClientTransport();
    }

    /**
     * @return {@link HttpMessageExchange#INSTANCE}
     */
    @Override
    public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
        return HttpMessageExchange.INSTANCE;
    }

    /**
     * Records {@code request} and throws {@link RequestCaptured}.
     *
     * @throws RequestCaptured always, carrying the recorded request
     */
    @Override
    public SdkHttpFullResponse send(ExecutionAttributes context, SdkHttpFullRequest request) {
        throw new RequestCaptured(record(request));
    }

    /**
     * Records {@code request} and returns a future failed with {@link RequestCaptured}. The hand-off is never called.
     */
    @Override
    public CompletableFuture<SdkHttpFullResponse> send(ExecutionAttributes context,
                                                       SdkHttpFullRequest request,
                                                       AsyncResponseHandoff<SdkHttpFullResponse> streamingHandoff) {
        try {
            return CompletableFutureUtils.failedFuture(new RequestCaptured(record(request)));
        } catch (RuntimeException e) {
            return CompletableFutureUtils.failedFuture(e);
        }
    }

    private CapturedRequest record(SdkHttpFullRequest request) {
        CapturedRequest captured = CapturedRequest.create(Validate.paramNotNull(request, "request"));
        requests.add(captured);
        firstRequest.complete(captured);
        return captured;
    }

    /**
     * Every request sent so far, in order. A call that was not retried sent exactly one.
     */
    public List<CapturedRequest> requests() {
        return Collections.unmodifiableList(new ArrayList<>(requests));
    }

    /**
     * The first request sent, if any.
     */
    public Optional<CapturedRequest> firstRequest() {
        return Optional.ofNullable(firstRequest.getNow(null));
    }

    /**
     * Waits for the first request, for calls whose send happens on another thread (async clients).
     *
     * @param timeout how long to wait
     * @return the first request
     * @throws AssertionError if no request is sent within {@code timeout}
     */
    public CapturedRequest awaitFirstRequest(Duration timeout) {
        try {
            return firstRequest.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new AssertionError("The client sent no request within " + timeout, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for the client to send a request", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unexpected failure of the capture future", e.getCause());
        }
    }

    /**
     * Does nothing: the transport holds no resources.
     */
    @Override
    public void close() {
    }
}
