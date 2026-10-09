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

package software.amazon.awssdk.benchmark.harness;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import software.amazon.awssdk.dynamic.client.DynamicAsyncClient;
import software.amazon.awssdk.dynamic.client.DynamicClient;
import software.amazon.awssdk.dynamic.client.DynamicServiceException;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The shared engine of the finite workloads of every protocol other than awsJson1.0 (which {@link DynamoDbWorkloads}
 * covers): one modeled operation per protocol whose request carries a padded string or blob member and whose success
 * response echoes the pad, so the request plus the response hit the payload class's target size and the decoded
 * response proves the round trip. Each provider supplies the wire bytes ({@link PaddedFixture}), the generated sync
 * and async invocations, and the Dynamic Client input over the service's vendored Smithy model; this class supplies
 * the four client paths, the outcome checks and the shared client configuration of {@link DynamoDbWorkloads}
 * (static credentials, fixed region, endpoint override, the cell's transport, the benchmark retry strategy).
 */
final class FiniteWorkloads {

    private FiniteWorkloads() {
    }

    static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    /** How the mock service recognizes the operation. */
    record Route(String method, String path, String headerName, String headerValue, byte[] bodyPrefix) {
        static Route path(String method, String path) {
            return new Route(method, path, null, null, null);
        }
    }

    /**
     * The wire bytes of one cell, deterministic in the payload class. The pad is a run of {@code x} whose length is a
     * multiple of {@code granularity} (3 for base64-encoded blobs, so the encoding has no padding) and is solved
     * from the encoded size of the request and success response, which are linear in it.
     */
    static final class PaddedFixture implements ServiceFixture {
        private final Route route;
        private final String pad;
        private final byte[] request;
        private final MockResponse success;
        private final MockResponse error;
        private final MockResponse retryable;

        PaddedFixture(PayloadClass payload, Route route, int granularity, Function<String, byte[]> request,
                      Function<String, MockResponse> success, MockResponse error, MockResponse retryable) {
            Function<Integer, Long> size = n -> {
                String p = "x".repeat(n);
                return (long) request.apply(p).length + success.apply(p).body().length;
            };
            int base = granularity;
            int probe = granularity * 1001;
            long s1 = size.apply(base);
            double slope = (size.apply(probe) - s1) / (double) (probe - base);
            long units = Math.max(0, Math.round((payload.targetBytes() - s1) / slope / granularity));
            this.route = route;
            this.pad = "x".repeat(base + (int) units * granularity);
            this.request = request.apply(pad);
            this.success = success.apply(pad);
            this.error = error;
            this.retryable = retryable;
            if (!payload.contains(encodedBytes())) {
                throw new IllegalStateException("fixture of " + encodedBytes() + " encoded bytes is not " + payload.id());
            }
            if (error != null && !payload.contains(this.request.length + (long) error.body().length)) {
                throw new IllegalStateException("error fixture is not " + payload.id());
            }
        }

        String pad() {
            return pad;
        }

        byte[] request() {
            return request.clone();
        }

        @Override
        public boolean matches(String method, String path, String routingHeader) {
            return route.method().equals(method) && route.path().equals(path)
                   && (route.headerName() == null || route.headerValue().equals(routingHeader));
        }

        @Override
        public boolean matchesBody(byte[] body) {
            byte[] prefix = route.bodyPrefix();
            return prefix == null
                   || body.length >= prefix.length && Arrays.equals(body, 0, prefix.length, prefix, 0, prefix.length);
        }

        @Override
        public String routingHeaderName() {
            return route.headerName();
        }

        @Override
        public MockResponse success() {
            return success;
        }

        @Override
        public MockResponse modeledError() {
            if (error == null) {
                throw new IllegalStateException("the operation has no modeled error");
            }
            return error;
        }

        @Override
        public MockResponse retryableError() {
            return retryable;
        }

        @Override
        public long encodedBytes() {
            return request.length + (long) success.body().length;
        }
    }

    /**
     * One client path's invocation: the built client, the sync or async call, which throwables are the modeled
     * error of error cells, and how to read the echoed pad from a decoded response.
     */
    record Invoker(AutoCloseable client, Supplier<Object> sync, Supplier<CompletableFuture<?>> async,
                   Predicate<Throwable> modeledError, Function<Object, String> echo) {
        static Invoker sync(AutoCloseable client, Supplier<Object> call, Predicate<Throwable> modeledError,
                            Function<Object, String> echo) {
            return new Invoker(client, call, null, modeledError, echo);
        }

        static Invoker async(AutoCloseable client, Supplier<CompletableFuture<?>> call,
                             Predicate<Throwable> modeledError, Function<Object, String> echo) {
            return new Invoker(client, null, call, modeledError, echo);
        }
    }

    /** A protocol's finite provider: one operation, its fixture, and its four client paths. */
    abstract static class Provider implements WorkloadProvider {
        private final String name;
        private final BenchProtocol protocol;
        private final String modelKey;
        private final ShapeId service;
        private final String operation;
        private final ShapeId errorShape;

        /**
         * @param modelKey the {@code --model} key of the Dynamic Client paths, also the service module under
         *     {@code v3/services} whose vendored model is the default
         * @param errorShape the modeled error of error cells, or {@code null} if the operation models none (then the
         *     provider does not support error cells)
         */
        Provider(String name, BenchProtocol protocol, String modelKey, String service, String operation,
                 String errorShape) {
            this.name = name;
            this.protocol = protocol;
            this.modelKey = modelKey;
            this.service = ShapeId.from(service);
            this.operation = operation;
            this.errorShape = errorShape == null ? null : ShapeId.from(errorShape);
        }

        @Override
        public final String name() {
            return name;
        }

        @Override
        public boolean supports(BenchmarkCell cell) {
            return cell.protocol() == protocol && cell.kind() == WorkloadKind.FINITE
                   && (errorShape != null || cell.outcome() != Outcome.MODELED_ERROR);
        }

        @Override
        public abstract PaddedFixture fixture(BenchmarkCell cell);

        /** The generated client invocation on {@code http} (sync or async per {@code async}). */
        abstract Invoker generated(boolean async, PaddedFixture fixture, URI endpoint, AutoCloseable http);

        abstract Document dynamicInput(PaddedFixture fixture);

        abstract String dynamicEcho(Document output);

        /** The default vendored model of the Dynamic Client paths, relative to the module directory. */
        Path defaultModel() {
            return Path.of("../../services/" + modelKey + "/src/main/smithy/model.json");
        }

        @Override
        public CellWorkload create(BenchmarkCell cell, URI endpoint, Options options) {
            PaddedFixture fixture = fixture(cell);
            AutoCloseable transport = cell.transport().create(cell.concurrency());
            try {
                Invoker invoker = cell.path().isDynamic() ? dynamic(cell.path(), fixture, endpoint, transport, options)
                                  : generated(cell.path().mode() == software.amazon.awssdk.http.HttpTransportMode.ASYNC,
                                              fixture, endpoint, transport);
                return new PathWorkload(cell, fixture.pad(), transport, invoker);
            } catch (RuntimeException e) {
                try {
                    transport.close();
                } catch (Exception ignored) {
                    // best effort while failing construction
                }
                throw e;
            }
        }

        private Invoker dynamic(ClientPath path, PaddedFixture fixture, URI endpoint, AutoCloseable http,
                                Options options) {
            Path model = options.models().getOrDefault(modelKey, defaultModel());
            Document input = dynamicInput(fixture);
            Predicate<Throwable> error = t -> t instanceof DynamicServiceException d && errorShape != null
                                              && errorShape.equals(d.errorShapeId());
            Function<Object, String> echo = o -> dynamicEcho((Document) o);
            if (path == ClientPath.DYNAMIC_ASYNC) {
                DynamicAsyncClient client = DynamicAsyncClient.builder().model(model).serviceId(service)
                    .region(Region.US_EAST_1).credentialsProvider(DynamoDbWorkloads.credentials())
                    .endpointOverride(endpoint).retryStrategy(DynamoDbWorkloads.retryStrategy())
                    .httpClient((SdkAsyncHttpClient) http).build();
                return Invoker.async(client, () -> client.callAsync(operation, input), error, echo);
            }
            DynamicClient client = DynamicClient.builder().model(model).serviceId(service).region(Region.US_EAST_1)
                .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
                .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkSyncHttpClient) http).build();
            return Invoker.sync(client, () -> client.call(operation, input), error, echo);
        }
    }

    /** A client path driven through an {@link Invoker}, with the outcome checks of {@link DynamoDbWorkloads}. */
    static final class PathWorkload implements CellWorkload {
        private final BenchmarkCell cell;
        private final String pad;
        private final AutoCloseable transport;
        private final Invoker invoker;
        private final boolean expectError;

        PathWorkload(BenchmarkCell cell, String pad, AutoCloseable transport, Invoker invoker) {
            this.cell = cell;
            this.pad = pad;
            this.transport = transport;
            this.invoker = invoker;
            this.expectError = cell.outcome() == Outcome.MODELED_ERROR;
        }

        @Override
        public boolean isAsync() {
            return invoker.async() != null;
        }

        @Override
        public String transportClass() {
            return transport.getClass().getName();
        }

        private void checkError(Throwable t) {
            Throwable cause = t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
            if (expectError && invoker.modeledError().test(cause)) {
                return;
            }
            throw new UnexpectedOutcomeException(cell, cause);
        }

        private Object invoke() {
            Object response;
            try {
                response = isAsync() ? invoker.async().get().join() : invoker.sync().get();
            } catch (RuntimeException e) {
                checkError(e);
                return null;
            }
            if (expectError) {
                throw new UnexpectedOutcomeException(cell, null);
            }
            return response;
        }

        @Override
        public void call() {
            invoke();
        }

        @Override
        public CompletableFuture<Void> callAsync() {
            if (!isAsync()) {
                throw new UnsupportedOperationException("sync path");
            }
            return invoker.async().get().handle((r, t) -> {
                if (t != null) {
                    checkError(t);
                } else if (expectError) {
                    throw new UnexpectedOutcomeException(cell, null);
                }
                return null;
            });
        }

        @Override
        public void verify() {
            Object response = invoke();
            if (response != null && !pad.equals(invoker.echo().apply(response))) {
                throw new IllegalStateException(cell + ": decoded response does not match the fixture");
            }
        }

        @Override
        public void close() throws Exception {
            try {
                invoker.client().close();
            } finally {
                transport.close();
            }
        }
    }

    /** A {@code Content-Type}-only response. */
    static ServiceFixture.MockResponse response(int status, String contentType, String body) {
        return new ServiceFixture.MockResponse(status, contentType, utf8(body));
    }

    static ServiceFixture.MockResponse response(int status, String contentType, byte[] body,
                                                Map<String, String> headers) {
        return new ServiceFixture.MockResponse(status, contentType, body, headers);
    }
}
