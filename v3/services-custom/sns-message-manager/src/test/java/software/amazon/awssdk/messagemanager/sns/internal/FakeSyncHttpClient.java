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

package software.amazon.awssdk.messagemanager.sns.internal;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.http.provider.HttpClientResolver;
import software.amazon.awssdk.http.provider.ResolvedHttpClient;

/**
 * An in-memory sync HTTP client: serves configured responses by request URL ({@code scheme://host/path}), records
 * every request and whether it was closed. Unknown URLs answer 404. No network access.
 */
final class FakeSyncHttpClient implements SdkSyncHttpClient {
    static final HttpClientCapabilities CAPABILITIES =
        HttpClientCapabilities.builder()
                              .mode(HttpTransportMode.SYNC)
                              .addProtocol(Protocol.HTTP1_1)
                              .addOption(HttpClientOption.CONNECTION_TIMEOUT, HttpClientOption.READ_TIMEOUT)
                              .maxBufferedBytesPerExchange(64 * 1024)
                              .cancellationTimeout(Duration.ofSeconds(1))
                              .build();

    private final Map<String, Supplier<SdkHttpResponse>> responses = new ConcurrentHashMap<>();
    private final List<SdkHttpRequest> requests = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    /** Serves {@code body} with status 200 for {@code url}. */
    FakeSyncHttpClient serve(String url, byte[] body) {
        return respond(url, () -> SdkHttpResponse.builder().statusCode(200).body(HttpBody.fromBytes(body)).build());
    }

    /** Serves {@code body} as a streaming response body for {@code url}. */
    FakeSyncHttpClient serveStreaming(String url, byte[] body) {
        return respond(url, () -> SdkHttpResponse.builder().statusCode(200)
                                                 .body(HttpBody.streaming(StreamingBodies.fromBytes(body, null)))
                                                 .build());
    }

    /** Answers {@code url} with what {@code response} returns or throws. */
    FakeSyncHttpClient respond(String url, Supplier<SdkHttpResponse> response) {
        responses.put(url, response);
        return this;
    }

    /** This client as a caller-owned resolved client. */
    ResolvedHttpClient<SdkSyncHttpClient> callerOwned() {
        return HttpClientResolver.sync().client(this).resolve();
    }

    List<SdkHttpRequest> requests() {
        return requests;
    }

    boolean isClosed() {
        return closed.get();
    }

    @Override
    public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
        if (closed.get()) {
            throw new IllegalStateException("closed");
        }
        return new SyncHttpExchange() {
            @Override
            public SdkHttpResponse execute() {
                requests.add(request);
                String url = request.scheme() + "://" + request.host() + request.encodedPath();
                return responses.getOrDefault(url, () -> SdkHttpResponse.builder().statusCode(404).build()).get();
            }

            @Override
            public void abort() {
            }

            @Override
            public void close() {
            }
        };
    }

    @Override
    public HttpClientCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public String clientName() {
        return "FakeSyncHttpClient";
    }

    @Override
    public void close() {
        closed.set(true);
    }
}
