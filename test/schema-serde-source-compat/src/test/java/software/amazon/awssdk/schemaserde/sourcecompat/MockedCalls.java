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

package software.amazon.awssdk.schemaserde.sourcecompat;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schemaserde.sourcecompat.fixtures.ClientSettings;
import software.amazon.awssdk.testutils.mock.MockPlugin;
import software.amazon.awssdk.testutils.mock.MockQueue;
import software.amazon.awssdk.testutils.mock.MockedRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Test support: mocked results for the fixtures' schema-based clients, and HTTP clients that never reach the network.
 */
final class MockedCalls {
    private MockedCalls() {
    }

    /**
     * A {@link MockPlugin} that returns {@code results} in order. Each result is an output value or a modeled exception,
     * which the plugin turns into a response of the client's protocol.
     */
    static MockPlugin plugin(Object... results) {
        MockQueue queue = new MockQueue();
        for (Object result : results) {
            if (result instanceof AwsServiceException) {
                queue.enqueueError((SerializableStruct) result);
            } else {
                queue.enqueue((SerializableStruct) result);
            }
        }
        return MockPlugin.builder().addQueue(queue).build();
    }

    /**
     * Checks that every request the plugin received went to the override endpoint with the override header.
     */
    static void assertSentWithClientSettings(MockPlugin plugin, int expectedRequests) {
        List<MockedRequest> requests = plugin.getRequests();
        assertThat(requests).hasSize(expectedRequests);
        for (MockedRequest request : requests) {
            assertThat(request.request().host()).isEqualTo(ClientSettings.ENDPOINT.getHost());
            assertThat(request.request().port()).isEqualTo(ClientSettings.ENDPOINT.getPort());
            assertThat(request.request().firstMatchingHeader(ClientSettings.FIXTURE_HEADER)).hasValue("1");
        }
    }

    /**
     * A synchronous HTTP client that fails every call. The mock plugin replaces the transport of schema-based clients,
     * so this client is never called.
     */
    static final class NoNetworkHttpClient implements SdkHttpClient {
        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            throw new AssertionError("Unexpected HTTP request to " + request.httpRequest().getUri());
        }

        @Override
        public void close() {
        }
    }

    /**
     * An asynchronous HTTP client that fails every call.
     */
    static final class NoNetworkAsyncHttpClient implements SdkAsyncHttpClient {
        @Override
        public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
            return CompletableFutureUtils.failedFuture(
                new AssertionError("Unexpected HTTP request to " + request.request().getUri()));
        }

        @Override
        public void close() {
        }
    }

    /**
     * A synchronous HTTP client that returns one fixed 200 response, for the legacy-route DynamoDB Streams client, and
     * records the requests it receives.
     */
    static final class StubHttpClient implements SdkHttpClient {
        private final String body;
        private final List<SdkHttpRequest> requests = Collections.synchronizedList(new ArrayList<>());

        StubHttpClient(String body) {
            this.body = body;
        }

        List<SdkHttpRequest> requests() {
            return requests;
        }

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            requests.add(request.httpRequest());
            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() {
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    return HttpExecuteResponse.builder()
                                              .response(SdkHttpResponse.builder()
                                                                       .statusCode(200)
                                                                       .putHeader("Content-Length",
                                                                                  String.valueOf(bytes.length))
                                                                       .build())
                                              .responseBody(AbortableInputStream.create(new ByteArrayInputStream(bytes)))
                                              .build();
                }

                @Override
                public void abort() {
                }
            };
        }

        @Override
        public void close() {
        }
    }
}
