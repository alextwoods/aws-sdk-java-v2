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

package software.amazon.awssdk.test.generated.support;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

/**
 * A fake sync and async HTTP transport: it records every request (with its buffered body) and answers each with the
 * response its {@code responder} builds. Nothing leaves the JVM.
 */
public final class FakeHttp implements SdkSyncHttpClient, SdkAsyncHttpClient {

    /** Static test credentials, so generated SigV4 clients never consult the default chain. */
    public static final IdentityProvider<AwsCredentialsIdentity> CREDENTIALS = new IdentityProvider<>() {
        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(
                AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY"));
        }
    };

    private final List<Recorded> requests = new CopyOnWriteArrayList<>();
    private final HttpTransportMode mode;
    private final Function<Recorded, SdkHttpResponse> responder;

    private FakeHttp(HttpTransportMode mode, Function<Recorded, SdkHttpResponse> responder) {
        this.mode = mode;
        this.responder = responder;
    }

    public static FakeHttp sync(Function<Recorded, SdkHttpResponse> responder) {
        return new FakeHttp(HttpTransportMode.SYNC, responder);
    }

    public static FakeHttp async(Function<Recorded, SdkHttpResponse> responder) {
        return new FakeHttp(HttpTransportMode.ASYNC, responder);
    }

    /** A {@code 200} response with a UTF-8 body. */
    public static SdkHttpResponse ok(String body) {
        return SdkHttpResponse.builder().statusCode(200).body(HttpBody.fromUtf8(body)).build();
    }

    /** Every request sent so far, in order. */
    public List<Recorded> requests() {
        return List.copyOf(requests);
    }

    private SdkHttpResponse respond(SdkHttpRequest request) {
        ByteBuffer buffer = request.body().buffered().asByteBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Recorded recorded = new Recorded(request, new String(bytes, StandardCharsets.UTF_8));
        requests.add(recorded);
        return responder.apply(recorded);
    }

    @Override
    public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
        return new SyncHttpExchange() {
            @Override
            public SdkHttpResponse execute() {
                return respond(request);
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
    public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                     HttpExecutionOptions options) {
        AsyncExchangeCompletion exchange =
            new AsyncExchangeCompletion(clientName(), (reason, failure) -> handler.onError(failure));
        SdkHttpResponse response;
        try {
            response = respond(request);
        } catch (RuntimeException failure) {
            // A responder that throws is a transport failure after the body was sent, reported as transports do.
            exchange.fail(failure);
            handler.onError(failure);
            return exchange;
        }
        ByteBuffer body = response.body().buffered().asByteBuffer();
        handler.onHeaders(response.toBuilder().body(HttpBody.empty()).build());
        handler.onStream(publisherOf(body));
        exchange.complete();
        return exchange;
    }

    @Override
    public HttpClientCapabilities capabilities() {
        return HttpClientCapabilities.builder().mode(mode).addProtocol(Protocol.HTTP1_1)
                                     .maxBufferedBytesPerExchange(1 << 20)
                                     .cancellationTimeout(Duration.ofSeconds(1)).build();
    }

    @Override
    public String clientName() {
        return "FakeHttp";
    }

    @Override
    public void close() {
    }

    private static Publisher<ByteBuffer> publisherOf(ByteBuffer bytes) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private boolean done;

            @Override
            public void request(long n) {
                if (done) {
                    return;
                }
                done = true;
                if (bytes.hasRemaining()) {
                    subscriber.onNext(bytes.asReadOnlyBuffer());
                }
                subscriber.onComplete();
            }

            @Override
            public void cancel() {
                done = true;
            }
        });
    }

    /** A request the transport received and its body as UTF-8. */
    public record Recorded(SdkHttpRequest request, String body) {
        /**
         * The payload the body carries: the body itself, or for an {@code aws-chunked} body its decoded data
         * frames, without the framing and trailer. Frame sizes count bytes, so this is for ASCII payloads.
         */
        public String decodedBody() {
            boolean chunked = request.headers().firstValue("Content-Encoding").map(v -> v.contains("aws-chunked"))
                                     .orElse(false);
            if (!chunked) {
                return body;
            }
            StringBuilder decoded = new StringBuilder();
            int position = 0;
            while (true) {
                int lineEnd = body.indexOf("\r\n", position);
                int size = Integer.parseInt(body.substring(position, lineEnd).split(";")[0].trim(), 16);
                position = lineEnd + 2;
                if (size == 0) {
                    return decoded.toString();
                }
                decoded.append(body, position, position + size);
                position += size + 2;
            }
        }

        /** The trailer of an {@code aws-chunked} body: the lines after its last (empty) frame. */
        public String trailer() {
            int last = body.lastIndexOf("0\r\n");
            return last < 0 ? "" : body.substring(last + 3).strip();
        }
    }
}
