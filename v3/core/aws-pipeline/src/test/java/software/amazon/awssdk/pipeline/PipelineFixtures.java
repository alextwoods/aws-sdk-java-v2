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

package software.amazon.awssdk.pipeline;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.retries.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.AttemptFailure;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryDecision;
import software.amazon.awssdk.retries.RetryScheduler;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.RetryToken;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.StreamingTrait;

/** Schemas, operations and client components shared by the pipeline runtime tests. */
final class PipelineFixtures {
    static final ShapeId SERVICE = ShapeId.from("com.example#Things");
    static final URI ENDPOINT = URI.create("https://things.example.com");
    static final Schema STRING = Schema.createString(ShapeId.from("smithy.api#String"));

    static final Schema GET_INPUT = Schema.structureBuilder(ShapeId.from("com.example#GetThingInput"))
                                          .putMember("Name", STRING)
                                          .build();
    static final Schema GET_OUTPUT = Schema.structureBuilder(ShapeId.from("com.example#GetThingOutput"))
                                           .putMember("Value", STRING)
                                           .build();
    static final Schema PUT_INPUT = Schema.structureBuilder(ShapeId.from("com.example#PutThingInput"))
                                          .putMember("Name", STRING)
                                          .putMember("ClientToken", STRING, new IdempotencyTokenTrait())
                                          .build();

    /** A streaming-output operation: {@code Value} is bound to the {@code X-Value} header, {@code Body} streams. */
    static final Schema STREAM_OUTPUT = Schema.structureBuilder(ShapeId.from("com.example#StreamThingOutput"))
                                              .putMember("Value", STRING)
                                              .putMember("Body", Schema.createBlob(
                                                  ShapeId.from("com.example#Stream"), new StreamingTrait()))
                                              .build();

    static final AwsOperation<Struct, Struct> GET_THING =
        operation("com.example#GetThing", GET_INPUT, GET_OUTPUT);
    static final AwsOperation<Struct, Struct> STREAM_THING = streamingOutputOperation();
    static final AwsOperation<Struct, Struct> PUT_THING =
        operation("com.example#PutThing", PUT_INPUT, GET_OUTPUT);

    private PipelineFixtures() {
    }

    static AwsOperation<Struct, Struct> operation(String id, Schema input, Schema output) {
        return operation(id, input, output, null);
    }

    /** An operation with the modeled {@code auth} traits, or none when {@code auth} is null. */
    static AwsOperation<Struct, Struct> operation(String id, Schema input, Schema output, ModeledAuth auth) {
        return operationBuilder(Schema.createOperation(ShapeId.from(id)), input, output).auth(auth).build();
    }

    /** The builder of an operation of {@code operationSchema}, whose traits drive the plan's feature stages. */
    static AwsOperation.Builder<Struct, Struct> operationBuilder(Schema operationSchema, Schema input, Schema output) {
        ShapeId operationId = operationSchema.id();
        OperationSchemas schemas = OperationSchemas.builder(operationSchema)
                                                   .input(input)
                                                   .output(output)
                                                   .build();
        AwsOperationDescriptor descriptor = AwsOperationDescriptor.builder()
                                                                  .id(operationId)
                                                                  .service(SERVICE)
                                                                  .input(input.id())
                                                                  .output(output.id())
                                                                  .schemas(schemas)
                                                                  .build();
        return AwsOperation.builder(descriptor, () -> new Builder(input), () -> new Builder(output));
    }

    private static AwsOperation<Struct, Struct> streamingOutputOperation() {
        Schema operation = Schema.createOperation(ShapeId.from("com.example#StreamThing"));
        OperationSchemas schemas = OperationSchemas.builder(operation)
                                                   .input(GET_INPUT)
                                                   .output(STREAM_OUTPUT)
                                                   .outputStreamMember("Body")
                                                   .build();
        AwsOperationDescriptor descriptor = AwsOperationDescriptor.builder()
                                                                  .id(operation.id())
                                                                  .service(SERVICE)
                                                                  .input(GET_INPUT.id())
                                                                  .output(STREAM_OUTPUT.id())
                                                                  .schemas(schemas)
                                                                  .build();
        return AwsOperation.builder(descriptor, () -> new Builder(GET_INPUT), () -> new Builder(STREAM_OUTPUT))
                           .build();
    }

    static ClientRuntime.Builder runtimeBuilder(CountingProtocol protocol) {
        return ClientRuntime.builder()
                            .configuration(SdkClientConfiguration.empty())
                            .protocol(protocol)
                            .transportMode(HttpTransportMode.SYNC)
                            .retryStrategy(StandardRetryStrategy.create(), "things");
    }

    static ClientRuntime runtime() {
        return runtimeBuilder(new CountingProtocol()).build();
    }

    /** The token of attempt 1 from the runtime's real retry strategy. */
    static RetryToken firstToken(PipelinePlan<?, ?> plan) {
        return plan.runtime().retryStrategy().acquireInitialToken(plan.initialTokenRequest()).token();
    }

    /** The token of the next attempt after a retryable (HTTP 503) failure of the attempt {@code token} authorized. */
    static RetryToken retryToken(RetryStrategy strategy, RetryToken token) {
        RetryDecision decision = strategy.refreshRetryToken(
            token, AttemptFailure.of(SdkServiceException.builder().statusCode(503).build()));
        return ((RetryDecision.Retry) decision).token();
    }

    /** A standard retry strategy of {@code maxAttempts} without backoff. */
    static RetryStrategy retries(int maxAttempts) {
        return StandardRetryStrategy.builder()
                                    .maxAttempts(maxAttempts)
                                    .backoffStrategy(BackoffStrategy.none())
                                    .throttlingBackoffStrategy(BackoffStrategy.none())
                                    .build();
    }

    /** The runtime of an engine test: {@link #ENDPOINT}, a {@link HeaderProtocol}, and {@code retryStrategy}. */
    static ClientRuntime.Builder engineRuntime(RetryStrategy retryStrategy, HttpTransportMode mode) {
        return engineRuntime(retryStrategy, mode, new HeaderProtocol());
    }

    static ClientRuntime.Builder engineRuntime(RetryStrategy retryStrategy, HttpTransportMode mode,
                                               HeaderProtocol protocol) {
        return ClientRuntime.builder()
                            .configuration(SdkClientConfiguration.builder()
                                                                 .option(SdkClientOption.CLIENT_ENDPOINT, ENDPOINT)
                                                                 .build())
                            .protocol(protocol)
                            .transportMode(mode)
                            .retryStrategy(retryStrategy, "things");
    }

    static Struct request(Schema schema, String name) {
        return new Struct(schema, Map.of("Name", name));
    }

    /**
     * A real retry strategy that records its token ledger: {@code acquire#1}, then per consumed token
     * {@code retry#n} or {@code stop#n} (a refresh), {@code success#n} or {@code failure#n}, where {@code n} is the
     * attempt the token authorized.
     */
    static final class RecordingRetryStrategy implements RetryStrategy {
        final List<String> ledger = new CopyOnWriteArrayList<>();
        private final RetryStrategy delegate;

        RecordingRetryStrategy(RetryStrategy delegate) {
            this.delegate = delegate;
        }

        @Override
        public int maxAttempts() {
            return delegate.maxAttempts();
        }

        @Override
        public AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
            AcquireInitialTokenResponse response = delegate.acquireInitialToken(request);
            ledger.add("acquire#" + response.token().attempt());
            return response;
        }

        @Override
        public RetryDecision refreshRetryToken(RetryToken token, AttemptFailure failure) {
            RetryDecision decision = delegate.refreshRetryToken(token, failure);
            ledger.add((decision instanceof RetryDecision.Retry ? "retry#" : "stop#") + token.attempt());
            return decision;
        }

        @Override
        public RetryToken recordSuccess(RetryToken token) {
            RetryToken consumed = delegate.recordSuccess(token);
            ledger.add("success#" + token.attempt());
            return consumed;
        }

        @Override
        public RetryToken recordFailure(RetryToken token) {
            RetryToken consumed = delegate.recordFailure(token);
            ledger.add("failure#" + token.attempt());
            return consumed;
        }
    }

    /** An executor that queues tasks until the test runs them, on the test thread. */
    static final class ManualExecutor implements Executor {
        final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable task) {
            tasks.add(task);
        }

        void runAll() {
            Runnable task;
            while ((task = tasks.poll()) != null) {
                task.run();
            }
        }
    }

    /** Hands out timers that never elapse unless the test completes them; each counts its cancellations. */
    static final class ManualScheduler implements RetryScheduler {
        final List<CountingTimer> timers = new CopyOnWriteArrayList<>();

        @Override
        public CompletableFuture<Void> delay(Duration delay) {
            CountingTimer timer = new CountingTimer();
            timers.add(timer);
            return timer;
        }
    }

    static final class CountingTimer extends CompletableFuture<Void> {
        final AtomicInteger cancels = new AtomicInteger();

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancels.incrementAndGet();
            return super.cancel(mayInterruptIfRunning);
        }
    }

    /**
     * An interceptor that implements only the hook of {@code hook} (a {@link PipelineStage.Kind#HOOK} stage) and runs
     * {@code onCall} on every invocation, keeping every value. Each hook has its own class, so a plan activates
     * exactly the hook stages of the recorders it is given.
     *
     * @throws IllegalArgumentException if {@code hook} is not a hook stage
     */
    static ExecutionInterceptor hookRecorder(PipelineStage hook, Runnable onCall) {
        return switch (hook) {
            case BEFORE_EXECUTION -> new ExecutionInterceptor() {
                @Override
                public void beforeExecution(HookContext.BeforeExecution context) {
                    onCall.run();
                }
            };
            case MODIFY_REQUEST -> new ExecutionInterceptor() {
                @Override
                public SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
                    onCall.run();
                    return context.request();
                }
            };
            case BEFORE_ATTEMPT -> new ExecutionInterceptor() {
                @Override
                public void beforeAttempt(HookContext.BeforeAttempt context) {
                    onCall.run();
                }
            };
            case AFTER_SERIALIZATION -> new ExecutionInterceptor() {
                @Override
                public void afterSerialization(HookContext.AfterSerialization context) {
                    onCall.run();
                }
            };
            case MODIFY_BEFORE_SIGNING -> new ExecutionInterceptor() {
                @Override
                public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
                    onCall.run();
                    return context.httpRequest();
                }
            };
            case MODIFY_AFTER_SIGNING -> new ExecutionInterceptor() {
                @Override
                public SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
                    onCall.run();
                    return context.signedRequest();
                }
            };
            case BEFORE_TRANSMISSION -> new ExecutionInterceptor() {
                @Override
                public void beforeTransmission(HookContext.BeforeTransmission context) {
                    onCall.run();
                }
            };
            case AFTER_TRANSMISSION -> new ExecutionInterceptor() {
                @Override
                public void afterTransmission(HookContext.AfterTransmission context) {
                    onCall.run();
                }
            };
            case MODIFY_HTTP_RESPONSE -> new ExecutionInterceptor() {
                @Override
                public SdkHttpResponse modifyHttpResponse(HookContext.ModifyHttpResponse context) {
                    onCall.run();
                    return context.httpResponse();
                }
            };
            case MODIFY_RESPONSE_BODY -> new ExecutionInterceptor() {
                @Override
                public HttpBody modifyResponseBody(HookContext.ModifyResponseBody context) {
                    onCall.run();
                    return context.body();
                }
            };
            case BEFORE_DESERIALIZATION -> new ExecutionInterceptor() {
                @Override
                public void beforeDeserialization(HookContext.BeforeDeserialization context) {
                    onCall.run();
                }
            };
            case MODIFY_ATTEMPT_EXCEPTION -> new ExecutionInterceptor() {
                @Override
                public Throwable modifyAttemptException(HookContext.ModifyAttemptException context) {
                    onCall.run();
                    return context.failure();
                }
            };
            case AFTER_ATTEMPT -> new ExecutionInterceptor() {
                @Override
                public void afterAttempt(HookContext.AfterAttempt context) {
                    onCall.run();
                }
            };
            case MODIFY_RESPONSE -> new ExecutionInterceptor() {
                @Override
                public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
                    onCall.run();
                    return context.response();
                }
            };
            case MODIFY_EXCEPTION -> new ExecutionInterceptor() {
                @Override
                public Throwable modifyException(HookContext.ModifyException context) {
                    onCall.run();
                    return context.failure();
                }
            };
            case AFTER_EXECUTION -> new ExecutionInterceptor() {
                @Override
                public void afterExecution(HookContext.AfterExecution context) {
                    onCall.run();
                }
            };
            default -> throw new IllegalArgumentException(hook + " is not a hook stage");
        };
    }

    /** The two drivers of the engine. */
    enum Driver {
        SYNC(HttpTransportMode.SYNC),
        ASYNC(HttpTransportMode.ASYNC);

        final HttpTransportMode mode;

        Driver(HttpTransportMode mode) {
            this.mode = mode;
        }
    }

    /**
     * A scripted transport for both drivers, one outcome per transmission; records each request it is handed. The
     * async exchange delivers its outcome inline; an {@link Error} outcome is thrown by the transport call itself on
     * both drivers.
     */
    static final class ScriptedHttp implements SdkSyncHttpClient, SdkAsyncHttpClient {
        final List<SdkHttpRequest> requests = new CopyOnWriteArrayList<>();
        private final Deque<Object> outcomes = new ArrayDeque<>();
        int transmissions;

        ScriptedHttp respond(int status, String body) {
            outcomes.add(SdkHttpResponse.builder()
                                        .statusCode(status)
                                        .body(body == null ? HttpBody.empty() : HttpBody.fromUtf8(body))
                                        .build());
            return this;
        }

        ScriptedHttp fail(Throwable failure) {
            outcomes.add(failure);
            return this;
        }

        /** Responds with {@code response}, headers included; its body must be empty or buffered. */
        ScriptedHttp respond(SdkHttpResponse response) {
            outcomes.add(response);
            return this;
        }

        private Object next() {
            transmissions++;
            Object outcome = outcomes.remove();
            if (outcome instanceof Error error) {
                throw error;
            }
            return outcome;
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            requests.add(request);
            return new SyncHttpExchange() {
                @Override
                public SdkHttpResponse execute() {
                    Object outcome = next();
                    if (outcome instanceof RuntimeException failure) {
                        throw failure;
                    }
                    return (SdkHttpResponse) outcome;
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
            requests.add(request);
            Object outcome = next();
            AsyncExchangeCompletion exchange =
                new AsyncExchangeCompletion(clientName(), (reason, failure) -> handler.onError(failure));
            if (outcome instanceof RuntimeException failure) {
                exchange.fail(failure);
                handler.onError(failure);
                return exchange;
            }
            SdkHttpResponse response = (SdkHttpResponse) outcome;
            handler.onHeaders(response.toBuilder().body(HttpBody.empty()).build());
            handler.onStream(bodyOf(response.body().asUtf8String().getBytes(StandardCharsets.UTF_8)));
            exchange.complete();
            return exchange;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return "ScriptedHttp";
        }

        @Override
        public void close() {
        }
    }

    /** A response body publisher of {@code bytes}, delivered on the first request. */
    private static Publisher<ByteBuffer> bodyOf(byte[] bytes) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private boolean done;

            @Override
            public void request(long n) {
                if (done) {
                    return;
                }
                done = true;
                if (bytes.length > 0) {
                    subscriber.onNext(ByteBuffer.wrap(bytes).asReadOnlyBuffer());
                }
                subscriber.onComplete();
            }

            @Override
            public void cancel() {
                done = true;
            }
        });
    }

    /** A client protocol that counts the operation plans it is asked for, per operation. */
    static final class CountingProtocol implements ClientProtocol {
        final AtomicInteger total = new AtomicInteger();
        final Map<ShapeId, AtomicInteger> perOperation = new ConcurrentHashMap<>();

        @Override
        public ShapeId protocolId() {
            return ShapeId.from("aws.protocols#awsJson1_0");
        }

        @Override
        public OperationProtocol operation(OperationSchemas operation) {
            total.incrementAndGet();
            perOperation.computeIfAbsent(operation.id(), id -> new AtomicInteger()).incrementAndGet();
            return new PlannedOperation(operation);
        }

        int count(AwsOperation<?, ?> operation) {
            AtomicInteger count = perOperation.get(operation.id());
            return count == null ? 0 : count.get();
        }
    }

    /**
     * Binds {@code Name} to a header and the body, {@code ClientToken} to a header, and the body to {@code Value};
     * records the threads its response codec runs on.
     */
    static final class HeaderProtocol implements ClientProtocol {
        final List<Thread> codecThreads = new CopyOnWriteArrayList<>();

        @Override
        public ShapeId protocolId() {
            return ShapeId.from("aws.protocols#awsJson1_0");
        }

        @Override
        public OperationProtocol operation(OperationSchemas operation) {
            return new HeaderOperation(operation, codecThreads);
        }
    }

    private record HeaderOperation(OperationSchemas schemas, List<Thread> codecThreads) implements OperationProtocol {
        @Override
        public SdkHttpRequest serializeRequest(SerializableStruct input, URI endpoint) {
            String name = input.getMemberValue(schemas.input().member("Name"));
            SdkHttpRequest.Builder request = SdkHttpRequest.builder()
                                                           .method("POST")
                                                           .scheme(endpoint.getScheme())
                                                           .host(endpoint.getHost())
                                                           .putHeader("X-Name", name)
                                                           .body(HttpBody.fromUtf8(name));
            Schema token = schemas.input().member("ClientToken");
            if (token != null && input.getMemberValue(token) != null) {
                request.putHeader("X-Token", input.getMemberValue(token));
            }
            return request.build();
        }

        @Override
        public <T extends SerializableStruct> T deserializeResponse(SdkHttpResponse response, ShapeBuilder<T> output) {
            codecThreads.add(Thread.currentThread());
            output.setMemberValue(output.schema().member("Value"), response.body().asUtf8String());
            return output.build();
        }

        /** As the HTTP binding protocols do: {@code Value} from the {@code X-Value} header, the payload unread. */
        @Override
        public <T extends SerializableStruct> T deserializeStreamingOutput(SdkHttpResponse response,
                                                                           ShapeBuilder<T> output) {
            codecThreads.add(Thread.currentThread());
            output.setMemberValue(output.schema().member("Value"),
                                  response.headers().firstValue("X-Value").orElse(null));
            return output.build();
        }

        @Override
        public AwsServiceException deserializeError(SdkHttpResponse response) {
            codecThreads.add(Thread.currentThread());
            return AwsServiceException.builder()
                                      .statusCode(response.statusCode())
                                      .message("Service error " + response.statusCode())
                                      .awsErrorDetails(AwsErrorDetails.builder()
                                                                      .errorCode("Error" + response.statusCode())
                                                                      .build())
                                      .build();
        }
    }

    /** An operation plan whose SERDE is not exercised by these tests. */
    private record PlannedOperation(OperationSchemas schemas) implements OperationProtocol {
        @Override
        public SdkHttpRequest serializeRequest(SerializableStruct input, URI endpoint) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends SerializableStruct> T deserializeResponse(SdkHttpResponse response, ShapeBuilder<T> output) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AwsServiceException deserializeError(SdkHttpResponse response) {
            throw new UnsupportedOperationException();
        }
    }

    /** A map-backed modeled structure. */
    static final class Struct implements SerializableStruct {
        private final Schema schema;
        private final Map<String, Object> values;

        Struct(Schema schema, Map<String, Object> values) {
            this.schema = schema;
            this.values = Map.copyOf(values);
        }

        @Override
        public Schema schema() {
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            throw new UnsupportedOperationException();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) values.get(member.memberName());
        }
    }

    private static final class Builder implements ShapeBuilder<Struct> {
        private final Schema schema;
        private final Map<String, Object> values = new HashMap<>();

        Builder(Schema schema) {
            this.schema = schema;
        }

        @Override
        public Struct build() {
            return new Struct(schema, values);
        }

        @Override
        public ShapeBuilder<Struct> deserialize(ShapeDeserializer decoder) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMemberValue(Schema member, Object value) {
            values.put(member.memberName(), value);
        }

        @Override
        public Schema schema() {
            return schema;
        }
    }
}
