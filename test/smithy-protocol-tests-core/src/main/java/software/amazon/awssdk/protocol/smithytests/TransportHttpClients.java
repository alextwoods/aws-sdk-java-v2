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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.protocol.AsyncClientTransport;
import software.amazon.awssdk.core.client.protocol.ClientTransport;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * {@code SdkHttpClient} and {@code SdkAsyncHttpClient} views of a protocol test transport, for clients whose calls still
 * run on the legacy request pipeline, which sends through the configured HTTP client instead of the
 * {@code ClientTransport}. They hand each request to the transport unchanged, so a request reaches the same
 * {@link CapturingClientTransport} or {@link FixedResponseTransport} whichever pipeline a call runs on.
 */
final class TransportHttpClients {
    private static final byte[] EMPTY = new byte[0];

    private TransportHttpClients() {
    }

    static SdkHttpClient sync(ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> transport) {
        return new SyncClient(transport);
    }

    static SdkAsyncHttpClient async(AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> transport) {
        return new AsyncClient(transport);
    }

    private static SdkHttpFullRequest toFullRequest(SdkHttpRequest request, ContentStreamProvider content) {
        return SdkHttpFullRequest.builder()
                                 .method(request.method())
                                 .protocol(request.protocol())
                                 .host(request.host())
                                 .port(request.port())
                                 .encodedPath(request.encodedPath())
                                 .rawQueryParameters(request.rawQueryParameters())
                                 .headers(request.headers())
                                 .contentStreamProvider(content)
                                 .build();
    }

    private static byte[] readBody(SdkHttpFullResponse response) {
        if (!response.content().isPresent()) {
            return EMPTY;
        }
        try (InputStream in = response.content().get()) {
            return IoUtils.toByteArray(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final class SyncClient implements SdkHttpClient {
        private final ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> transport;

        private SyncClient(ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> transport) {
            this.transport = Validate.paramNotNull(transport, "transport");
        }

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() {
                    SdkHttpFullRequest full = toFullRequest(request.httpRequest(),
                                                            request.contentStreamProvider().orElse(null));
                    SdkHttpFullResponse response = transport.send(new ExecutionAttributes(), full);
                    return HttpExecuteResponse.builder()
                                              .response(response.toBuilder().content(null).build())
                                              .responseBody(response.content().orElse(null))
                                              .build();
                }

                @Override
                public void abort() {
                }
            };
        }

        @Override
        public String clientName() {
            return "ProtocolTestTransport";
        }

        @Override
        public void close() {
        }
    }

    private static final class AsyncClient implements SdkAsyncHttpClient {
        private final AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> transport;

        private AsyncClient(AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> transport) {
            this.transport = Validate.paramNotNull(transport, "transport");
        }

        @Override
        public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            SdkAsyncHttpResponseHandler handler = request.responseHandler();
            collect(request.requestContentPublisher())
                .thenCompose(body -> transport.send(new ExecutionAttributes(),
                                                    toFullRequest(request.request(), ContentStreamProvider.fromByteArray(body)),
                                                    null))
                .whenComplete((response, error) -> {
                    if (error != null) {
                        Throwable cause = error instanceof CompletionException && error.getCause() != null
                                          ? error.getCause() : error;
                        handler.onError(cause);
                        result.completeExceptionally(cause);
                        return;
                    }
                    try {
                        byte[] body = readBody(response);
                        handler.onHeaders(response.toBuilder().content(null).build());
                        handler.onStream(AsyncRequestBody.fromBytes(body));
                        result.complete(null);
                    } catch (RuntimeException e) {
                        handler.onError(e);
                        result.completeExceptionally(e);
                    }
                });
            return result;
        }

        @Override
        public String clientName() {
            return "ProtocolTestTransport";
        }

        @Override
        public void close() {
        }
    }

    /**
     * Reads a request body publisher to the end. A null publisher is an empty body.
     */
    private static CompletableFuture<byte[]> collect(Publisher<ByteBuffer> publisher) {
        CompletableFuture<byte[]> bytes = new CompletableFuture<>();
        if (publisher == null) {
            bytes.complete(EMPTY);
            return bytes;
        }
        publisher.subscribe(new Subscriber<ByteBuffer>() {
            private final ByteArrayOutputStream out = new ByteArrayOutputStream();

            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer buffer) {
                ByteBuffer copy = buffer.duplicate();
                byte[] chunk = new byte[copy.remaining()];
                copy.get(chunk);
                out.write(chunk, 0, chunk.length);
            }

            @Override
            public void onError(Throwable t) {
                bytes.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                bytes.complete(out.toByteArray());
            }
        });
        return bytes;
    }
}
