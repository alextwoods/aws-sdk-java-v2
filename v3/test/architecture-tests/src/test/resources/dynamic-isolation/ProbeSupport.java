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

package probe;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.api.SignedRequest;
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
import software.amazon.awssdk.pipeline.ExecutionInterceptor;
import software.amazon.awssdk.pipeline.HookContext;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * Shared parts of the DynamicIsolationRuntimeTest probes, which the test compiles against and loads from an isolated
 * class path: static credentials, an in-memory awsJson1_0 transport and an interceptor that records every hook with
 * the aws-pipeline frames that dispatched it. Uses only types that generated service clients use, so it loads without
 * the dynamic layer.
 */
public final class ProbeSupport {
    public static final URI ENDPOINT = URI.create("https://fixture.test");
    public static final String PIPELINE_PACKAGE = "software.amazon.awssdk.pipeline.";

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

    private ProbeSupport() {
    }

    /** Answers every request with the awsJson1_0 Ping output {"message":"pong"} and records the X-Amz-Target. */
    public static final class Transport implements SdkSyncHttpClient, SdkAsyncHttpClient {
        public final List<String> targets = new CopyOnWriteArrayList<>();
        private final HttpTransportMode mode;

        public Transport(HttpTransportMode mode) {
            this.mode = mode;
        }

        private SdkHttpResponse respond(SdkHttpRequest request) {
            targets.add(request.method() + " " + request.headers().firstValue("X-Amz-Target").orElse("-"));
            return SdkHttpResponse.builder().statusCode(200)
                                  .addHeader("Content-Type", "application/x-amz-json-1.0")
                                  .body(HttpBody.fromUtf8("{\"message\":\"pong\"}"))
                                  .build();
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
            SdkHttpResponse response = respond(request);
            AsyncExchangeCompletion exchange =
                new AsyncExchangeCompletion(clientName(), (reason, failure) -> handler.onError(failure));
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
            return "ProbeTransport";
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
    }

    /**
     * Records every hook as {@code hook [stage outputs] <- frames}. The stage outputs of an attempt-scoped hook are
     * what the attempt's endpoint, auth-scheme and identity stages produced so far; the frames are the aws-pipeline
     * methods on the stack (innermost first), i.e. the engine driver and stage that reached the hook.
     */
    public static final class Recorder implements ExecutionInterceptor {
        public final List<String> events = new CopyOnWriteArrayList<>();

        private void record(String hook, HookContext context) {
            String outputs = "";
            if (context instanceof HookContext.Attempt attempt) {
                outputs = " [attempt=" + attempt.attemptNumber()
                          + " endpoint=" + attempt.endpoint().map(e -> e.url().toString()).orElse("-")
                          + " auth=" + attempt.authScheme().map(s -> s.scheme().schemeId()).orElse("-")
                          + " identity=" + attempt.identity().map(i -> i.getClass().getSimpleName()).orElse("-")
                          + "]";
            }
            List<String> frames = StackWalker.getInstance().walk(stack -> stack
                .filter(f -> f.getClassName().startsWith(PIPELINE_PACKAGE))
                .map(f -> f.getClassName().substring(PIPELINE_PACKAGE.length()) + "#" + f.getMethodName())
                .toList());
            events.add(hook + outputs + " <- " + String.join(" < ", frames));
        }

        @Override
        public void beforeExecution(HookContext.BeforeExecution context) {
            record("beforeExecution", context);
        }

        @Override
        public SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
            record("modifyRequest", context);
            return context.request();
        }

        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            record("beforeAttempt", context);
        }

        @Override
        public void afterSerialization(HookContext.AfterSerialization context) {
            record("afterSerialization", context);
        }

        @Override
        public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
            record("modifyBeforeSigning", context);
            return context.httpRequest();
        }

        @Override
        public SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
            record("modifyAfterSigning", context);
            return context.signedRequest();
        }

        @Override
        public void beforeTransmission(HookContext.BeforeTransmission context) {
            record("beforeTransmission", context);
        }

        @Override
        public void afterTransmission(HookContext.AfterTransmission context) {
            record("afterTransmission", context);
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(HookContext.ModifyHttpResponse context) {
            record("modifyHttpResponse", context);
            return context.httpResponse();
        }

        @Override
        public HttpBody modifyResponseBody(HookContext.ModifyResponseBody context) {
            record("modifyResponseBody", context);
            return context.body();
        }

        @Override
        public void beforeDeserialization(HookContext.BeforeDeserialization context) {
            record("beforeDeserialization", context);
        }

        @Override
        public Throwable modifyAttemptException(HookContext.ModifyAttemptException context) {
            record("modifyAttemptException", context);
            return context.failure();
        }

        @Override
        public void afterAttempt(HookContext.AfterAttempt context) {
            record("afterAttempt", context);
        }

        @Override
        public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
            record("modifyResponse", context);
            return context.response();
        }

        @Override
        public Throwable modifyException(HookContext.ModifyException context) {
            record("modifyException", context);
            return context.failure();
        }

        @Override
        public void afterExecution(HookContext.AfterExecution context) {
            record("afterExecution", context);
        }
    }
}
