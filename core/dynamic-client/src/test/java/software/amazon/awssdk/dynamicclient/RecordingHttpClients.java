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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.IoUtils;

/**
 * HTTP clients that record each request with its body and answer with one stubbed response. The service-test-utils
 * mocks record only the request line and headers of synchronous calls, so the wire form of the body could not be
 * checked with them.
 */
final class RecordingHttpClients {
    private RecordingHttpClients() {
    }

    /**
     * A recorded request: the HTTP request and its body as bytes and as text.
     */
    static final class Recorded {
        final SdkHttpRequest request;
        final byte[] bytes;
        final String body;

        private Recorded(SdkHttpRequest request, byte[] body) {
            this.request = request;
            this.bytes = body;
            this.body = new String(body, StandardCharsets.UTF_8);
        }
    }

    /**
     * The stubs of the next responses, in order; the last one answers every later request.
     */
    private static final class Stubs {
        private final Deque<Stub> queued = new ArrayDeque<>();
        private Stub last = Stub.of(200, "{}");

        synchronized void set(Stub... stubs) {
            queued.clear();
            queued.addAll(Arrays.asList(stubs));
            last = stubs[stubs.length - 1];
        }

        synchronized Stub next() {
            Stub next = queued.poll();
            return next != null ? next : last;
        }
    }

    /**
     * The stubbed response: status, headers and body.
     */
    static final class Stub {
        private final SdkHttpResponse response;
        private final byte[] body;

        private Stub(SdkHttpResponse response, byte[] body) {
            this.response = response;
            this.body = body;
        }

        static Stub of(int status, String body, String... headerNamesAndValues) {
            SdkHttpResponse.Builder response = SdkHttpResponse.builder().statusCode(status);
            for (int i = 0; i < headerNamesAndValues.length; i += 2) {
                response.putHeader(headerNamesAndValues[i], headerNamesAndValues[i + 1]);
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            response.putHeader("Content-Length", String.valueOf(bytes.length));
            return new Stub(response.build(), bytes);
        }
    }

    static final class Sync implements SdkHttpClient {
        private final List<Recorded> requests = Collections.synchronizedList(new ArrayList<>());
        private final Stubs stubs = new Stubs();

        /**
         * Answers the next requests with these responses, in order, and every later request with the last one.
         */
        void stub(Stub... responses) {
            stubs.set(responses);
        }

        List<Recorded> requests() {
            return requests;
        }

        Recorded lastRequest() {
            return requests.get(requests.size() - 1);
        }

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            byte[] body = request.contentStreamProvider().map(p -> read(p.newStream())).orElse(new byte[0]);
            requests.add(new Recorded(request.httpRequest(), body));
            Stub response = stubs.next();
            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() {
                    return HttpExecuteResponse.builder()
                                              .response(response.response)
                                              .responseBody(AbortableInputStream.create(
                                                  new ByteArrayInputStream(response.body)))
                                              .build();
                }

                @Override
                public void abort() {
                }
            };
        }

        @Override
        public String clientName() {
            return "RecordingSync";
        }

        @Override
        public void close() {
        }

        private static byte[] read(InputStream stream) {
            try (InputStream in = stream) {
                return IoUtils.toByteArray(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Reads the request body and answers on the calling thread.
     */
    static final class Async implements SdkAsyncHttpClient {
        private final List<Recorded> requests = Collections.synchronizedList(new ArrayList<>());
        private final Stubs stubs = new Stubs();

        /**
         * Answers the next requests with these responses, in order, and every later request with the last one.
         */
        void stub(Stub... responses) {
            stubs.set(responses);
        }

        List<Recorded> requests() {
            return requests;
        }

        Recorded lastRequest() {
            return requests.get(requests.size() - 1);
        }

        @Override
        public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
            CompletableFuture<byte[]> body = new CompletableFuture<>();
            request.requestContentPublisher().subscribe(new CollectingSubscriber(body));
            return body.thenAccept(bytes -> {
                requests.add(new Recorded(request.request(), bytes));
                Stub response = stubs.next();
                request.responseHandler().onHeaders(response.response);
                request.responseHandler().onStream(new OneBufferPublisher(response.body));
            });
        }

        @Override
        public String clientName() {
            return "RecordingAsync";
        }

        @Override
        public void close() {
        }
    }

    private static final class CollectingSubscriber implements Subscriber<ByteBuffer> {
        private final CompletableFuture<byte[]> result;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        private CollectingSubscriber(CompletableFuture<byte[]> result) {
            this.result = result;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer buffer) {
            byte[] chunk = new byte[buffer.remaining()];
            buffer.get(chunk);
            bytes.write(chunk, 0, chunk.length);
        }

        @Override
        public void onError(Throwable t) {
            result.completeExceptionally(t);
        }

        @Override
        public void onComplete() {
            result.complete(bytes.toByteArray());
        }
    }

    private static final class OneBufferPublisher implements SdkHttpContentPublisher {
        private final byte[] content;

        private OneBufferPublisher(byte[] content) {
            this.content = content;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.of((long) content.length);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            AtomicBoolean done = new AtomicBoolean();
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    if (n > 0 && done.compareAndSet(false, true)) {
                        subscriber.onNext(ByteBuffer.wrap(content));
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    done.set(true);
                }
            });
        }
    }
}
