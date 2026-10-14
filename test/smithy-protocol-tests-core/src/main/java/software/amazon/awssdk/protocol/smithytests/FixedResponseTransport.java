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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.protocol.AsyncClientTransport;
import software.amazon.awssdk.core.client.protocol.AsyncResponseHandoff;
import software.amazon.awssdk.core.client.protocol.ClientTransport;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * A sync and async HTTP transport for response protocol tests: it answers every request with the same
 * {@link StubbedResponse} (the test case {@code code}, {@code headers} and {@code body}) and never opens a network
 * connection. Build the response with {@link StubbedResponse#fromTestCase}, which base64-decodes CBOR bodies.
 *
 * <p>Each header of the stubbed response becomes one header value, as written in the test case. Every send returns a new
 * stream over the body. The requests are recorded, so a test can check how many attempts were made.
 *
 * <p>Thread safe.
 */
public final class FixedResponseTransport
    implements ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse>,
               AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> {

    private final StubbedResponse response;
    private final SdkHttpFullResponse headers;
    private final List<CapturedRequest> requests = new CopyOnWriteArrayList<>();

    private FixedResponseTransport(StubbedResponse response) {
        this.response = Validate.paramNotNull(response, "response");
        SdkHttpFullResponse.Builder builder = SdkHttpFullResponse.builder().statusCode(response.statusCode());
        response.headers().forEach(builder::appendHeader);
        this.headers = builder.build();
    }

    /**
     * A transport that answers every request with {@code response}.
     */
    public static FixedResponseTransport create(StubbedResponse response) {
        return new FixedResponseTransport(response);
    }

    /**
     * @return {@link HttpMessageExchange#INSTANCE}
     */
    @Override
    public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
        return HttpMessageExchange.INSTANCE;
    }

    /**
     * Records {@code request} and returns the stubbed status, headers and body.
     */
    @Override
    public SdkHttpFullResponse send(ExecutionAttributes context, SdkHttpFullRequest request) {
        record(request);
        return fullResponse();
    }

    /**
     * Records {@code request} and completes with the stubbed response. When {@code streamingHandoff} accepts the headers,
     * the body is handed to it as a publisher and the future completes with the response without a body, as the SDK's
     * asynchronous HTTP transport does.
     */
    @Override
    public CompletableFuture<SdkHttpFullResponse> send(ExecutionAttributes context,
                                                       SdkHttpFullRequest request,
                                                       AsyncResponseHandoff<SdkHttpFullResponse> streamingHandoff) {
        try {
            record(request);
            if (streamingHandoff != null && streamingHandoff.onHeaders(headers)) {
                streamingHandoff.onStream(AsyncRequestBody.fromBytes(response.body()));
                return CompletableFuture.completedFuture(headers);
            }
            return CompletableFuture.completedFuture(fullResponse());
        } catch (RuntimeException e) {
            return CompletableFutureUtils.failedFuture(e);
        }
    }

    private void record(SdkHttpFullRequest request) {
        requests.add(CapturedRequest.create(Validate.paramNotNull(request, "request")));
    }

    private SdkHttpFullResponse fullResponse() {
        return headers.toBuilder()
                      .content(AbortableInputStream.create(new ByteArrayInputStream(response.body())))
                      .build();
    }

    /**
     * Every request answered so far, in order.
     */
    public List<CapturedRequest> requests() {
        return Collections.unmodifiableList(new ArrayList<>(requests));
    }

    /**
     * Does nothing: the transport holds no resources.
     */
    @Override
    public void close() {
    }
}
