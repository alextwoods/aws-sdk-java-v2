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

package software.amazon.awssdk.benchmark.dynamodb;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.protocols.awsjson.AwsJson10ProtocolProvider;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.ProtocolSelector;
import software.amazon.awssdk.services.dynamodb.DynamoDbServiceMetadata;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * One V3 arm's call path: marshal ({@link OperationProtocol#serializeRequest}) → HTTP exchange on the arm's transport →
 * unmarshal ({@link OperationProtocol#deserializeResponse}). This is the whole of a V3 "call" until the client engine
 * exists; see {@link Arm#SCOPE_DETAIL}.
 *
 * <p>Everything that is per-client rather than per-call is built once here: the protocol, one
 * {@link OperationProtocol} per scenario, and the prebuilt requests. Service errors are deserialized and thrown, so a
 * non-2xx response fails the operation (and the repetition) instead of being timed as a success.
 *
 * <p>The async path collects the response body from the transport's publisher into one array and unmarshals on the
 * thread that completes the body. That collection is harness glue standing in for the missing async engine, and it
 * is charged to the async arms' CPU and allocation.
 */
final class V3Workload implements AutoCloseable {

    private static final HttpExecutionOptions OPTIONS = HttpExecutionOptions.defaults();

    final Arm arm;
    final URI endpoint;
    final AutoCloseable transport;
    private final SdkSyncHttpClient sync;
    private final SdkAsyncHttpClient async;
    private final OperationProtocol[] operations = new OperationProtocol[Scenario.values().length];
    private final SerializableStruct[] requests = new SerializableStruct[Scenario.values().length];

    V3Workload(Arm arm, URI endpoint, int concurrency) {
        this(arm, endpoint, arm.newTransport(concurrency));
    }

    /** Visible for the negative verification cases: any transport, whatever the arm claims. */
    V3Workload(Arm arm, URI endpoint, AutoCloseable transport) {
        this.arm = arm;
        this.endpoint = endpoint;
        this.transport = transport;
        this.sync = transport instanceof SdkSyncHttpClient s ? s : null;
        this.async = transport instanceof SdkAsyncHttpClient a ? a : null;
        ClientProtocol protocol =
            ProtocolSelector.select(DynamoDbServiceMetadata.serviceProtocols(), new AwsJson10ProtocolProvider())
                            .createClientProtocol(DynamoDbServiceMetadata.typeRegistry(), true);
        for (Scenario s : Scenario.values()) {
            AwsOperationDescriptor descriptor = DynamoDbServiceMetadata
                .operation(ShapeId.from("com.amazonaws.dynamodb#" + s.operation)).orElseThrow();
            operations[s.ordinal()] = protocol.operation(descriptor.schemas().orElseThrow());
            requests[s.ordinal()] = s.request();
        }
    }

    boolean supportsAsync() {
        return arm.mode == HttpTransportMode.ASYNC;
    }

    /** The constructed transport's self-reported name. */
    String clientName() {
        return sync != null ? sync.clientName() : async.clientName();
    }

    HttpTransportMode capabilityMode() {
        return sync != null ? sync.capabilities().mode() : async.capabilities().mode();
    }

    /** One blocking call; for async arms, the async call joined (used only by verification and join mode). */
    Object call(Scenario scenario) {
        if (sync == null) {
            return callAsync(scenario).join();
        }
        OperationProtocol op = operations[scenario.ordinal()];
        SdkHttpRequest request = op.serializeRequest(requests[scenario.ordinal()], endpoint);
        try (SyncHttpExchange exchange = sync.prepare(request, OPTIONS)) {
            return unmarshal(op, scenario, exchange.execute());
        }
    }

    CompletableFuture<Object> callAsync(Scenario scenario) {
        OperationProtocol op = operations[scenario.ordinal()];
        SdkHttpRequest request = op.serializeRequest(requests[scenario.ordinal()], endpoint);
        CompletableFuture<Object> result = new CompletableFuture<>();
        BodyCollector collector = new BodyCollector(op, scenario, result);
        AsyncHttpExchange exchange = async.execute(request, collector, OPTIONS);
        exchange.completion().whenComplete((v, t) -> {
            if (t != null) {
                result.completeExceptionally(t);
            } else if (!collector.streamed.get()) {
                // No body publisher was handed over: complete from the headers alone.
                collector.finish(new byte[0], 0);
            }
        });
        return result;
    }

    /**
     * Verification only: one call whose request body is a stream that captures the call stack of its first read, i.e.
     * the frames of whichever transport code consumed the body. Returns those frames ({@code class.method}).
     */
    List<String> probeFrames(Scenario scenario) {
        OperationProtocol op = operations[scenario.ordinal()];
        SdkHttpRequest serialized = op.serializeRequest(requests[scenario.ordinal()], endpoint);
        byte[] body = serialized.body().asByteArray();
        List<String> frames = new CopyOnWriteArrayList<>();
        InputStream capturing = new ByteArrayInputStream(body) {
            @Override
            public synchronized int read(byte[] b, int off, int len) {
                capture();
                return super.read(b, off, len);
            }

            @Override
            public synchronized int read() {
                capture();
                return super.read();
            }

            // ByteArrayInputStream overrides these without going through read(), so each must capture too.
            @Override
            public synchronized byte[] readAllBytes() {
                capture();
                return super.readAllBytes();
            }

            @Override
            public int readNBytes(byte[] b, int off, int len) {
                capture();
                return super.readNBytes(b, off, len);
            }

            @Override
            public synchronized long transferTo(java.io.OutputStream out) throws java.io.IOException {
                capture();
                return super.transferTo(out);
            }

            private void capture() {
                if (frames.isEmpty()) {
                    for (StackTraceElement e : new Throwable().getStackTrace()) {
                        frames.add(e.getClassName() + "." + e.getMethodName());
                    }
                }
            }
        };
        SdkHttpRequest request = serialized.toBuilder()
                                           .body(HttpBody.streaming(StreamingBodies.fromInputStream(
                                               capturing, body.length, MockDdbServer.CONTENT_TYPE)))
                                           .build();
        if (sync != null) {
            try (SyncHttpExchange exchange = sync.prepare(request, OPTIONS)) {
                unmarshal(op, scenario, exchange.execute());
            }
        } else {
            CompletableFuture<Object> result = new CompletableFuture<>();
            BodyCollector collector = new BodyCollector(op, scenario, result);
            async.execute(request, collector, OPTIONS).completion().whenComplete((v, t) -> {
                if (t != null) {
                    result.completeExceptionally(t);
                }
            });
            result.join();
        }
        return frames;
    }

    private static Object unmarshal(OperationProtocol op, Scenario scenario, SdkHttpResponse response) {
        if (op.isError(response)) {
            throw op.deserializeError(response);
        }
        return op.deserializeResponse(response, scenario.newOutputBuilder());
    }

    @Override
    public void close() throws Exception {
        transport.close();
    }

    /** Collects the async response body into one array, then unmarshals. */
    private static final class BodyCollector implements AsyncHttpResponseHandler, Subscriber<ByteBuffer> {
        private final OperationProtocol op;
        private final Scenario scenario;
        private final CompletableFuture<Object> result;
        private final AtomicBoolean streamed = new AtomicBoolean();
        private volatile SdkHttpResponse headers;
        private byte[] buffer = new byte[0];
        private int size;

        BodyCollector(OperationProtocol op, Scenario scenario, CompletableFuture<Object> result) {
            this.op = op;
            this.scenario = scenario;
            this.result = result;
        }

        @Override
        public void onHeaders(SdkHttpResponse response) {
            this.headers = response;
            long length = response.headers().firstValue("Content-Length").map(Long::parseLong).orElse(-1L);
            if (length > 0 && length < Integer.MAX_VALUE) {
                buffer = new byte[(int) length];
            }
        }

        @Override
        public void onStream(Publisher<ByteBuffer> body) {
            streamed.set(true);
            body.subscribe(this);
        }

        @Override
        public void onError(Throwable error) {
            result.completeExceptionally(error);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer chunk) {
            int n = chunk.remaining();
            if (size + n > buffer.length) {
                buffer = Arrays.copyOf(buffer, Math.max(size + n, buffer.length * 2));
            }
            chunk.get(buffer, size, n);
            size += n;
        }

        @Override
        public void onComplete() {
            finish(buffer, size);
        }

        void finish(byte[] bytes, int length) {
            if (result.isDone()) {
                return;
            }
            try {
                byte[] exact = length == bytes.length ? bytes : Arrays.copyOf(bytes, length);
                SdkHttpResponse response = headers.toBuilder().body(HttpBody.fromBytes(exact)).build();
                result.complete(unmarshal(op, scenario, response));
            } catch (RuntimeException | Error e) {
                result.completeExceptionally(e);
            }
        }
    }
}
