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

package software.amazon.awssdk.services.cloudwatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.codecs.cbor.Rpcv2CborCodec;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.protocols.client.rpcv2.RpcV2CborProtocol;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchOperations;
import software.amazon.awssdk.services.cloudwatch.model.DashboardNotFoundErrorException;
import software.amazon.awssdk.services.cloudwatch.model.GetDashboardRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetDashboardResponse;
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsRequest;
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.ResourceNotFoundException;

/**
 * The generated sync and async CloudWatch clients run their calls on the schema-based pipeline path (task 3.32,
 * Requirements 23.6, 30.3, 33.2, 37.2): every interceptor hook is reached from {@code SyncApiCallPipeline} or
 * {@code AsyncApiCallPipeline} and never from the legacy {@code RequestPipelineBuilder} stage graph or the legacy
 * {@code AmazonSyncHttpClient}/{@code AmazonAsyncHttpClient}, the {@code ApiOperation} the hooks see is the
 * {@link CloudWatchOperations} constant of the operation, and the request is rpcv2Cbor.
 */
class CloudWatchNewPathTest {
    private static final long TIMEOUT_SECONDS = 10;
    private static final Rpcv2CborCodec CBOR = Rpcv2CborCodec.builder().build();
    private static final String SERVICE_PATH = "/service/GraniteServiceVersion20100801/operation/";

    private static final String SYNC_PIPELINE = "software.amazon.awssdk.core.internal.http.SyncApiCallPipeline";
    private static final String ASYNC_PIPELINE = "software.amazon.awssdk.core.internal.http.AsyncApiCallPipeline";
    /**
     * The legacy stage graph: {@code RequestPipelineBuilder} and its composed pipelines, every legacy stage, and the
     * legacy HTTP clients that build and run it. The stateless helpers in {@code pipeline.stages.utils} (such as
     * {@code ExceptionReportingUtils}, which both paths use to report a failure to the interceptors) are not part of
     * the graph.
     */
    private static final List<String> LEGACY_PIPELINE_PREFIXES = Arrays.asList(
        "software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline",
        "software.amazon.awssdk.core.internal.http.pipeline.stages.",
        "software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient",
        "software.amazon.awssdk.core.internal.http.AmazonAsyncHttpClient");
    private static final String SHARED_STAGE_UTILITIES = "software.amazon.awssdk.core.internal.http.pipeline.stages.utils.";

    enum Mode {
        SYNC, ASYNC
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void getDashboard_whenTheServiceAnswers_runsOnTheNewPipeline(Mode mode) {
        ScriptedHttp http = new ScriptedHttp().respond(200, cbor(members("DashboardName", "my-dashboard",
                                                                         "DashboardBody", "{}")));
        PipelineRecorder recorder = new PipelineRecorder();

        GetDashboardResponse response = call(mode, http, recorder,
                                             c -> c.getDashboard(r -> r.dashboardName("my-dashboard")),
                                             c -> c.getDashboard(r -> r.dashboardName("my-dashboard")));

        assertThat(response.dashboardName()).isEqualTo("my-dashboard");
        assertThat(response.dashboardBody()).isEqualTo("{}");
        assertThat(response.sdkHttpResponse().statusCode()).isEqualTo(200);
        recorder.assertRanOnTheNewPipeline(mode, CloudWatchOperations.GET_DASHBOARD);
        assertThat(recorder.hooks()).containsExactly("beforeExecution", "beforeMarshalling", "afterMarshalling",
                                                     "beforeTransmission", "afterTransmission", "beforeUnmarshalling",
                                                     "afterUnmarshalling", "afterExecution");
        assertThat(recorder.sentPaths()).containsExactly(SERVICE_PATH + "GetDashboard");
        assertThat(recorder.sentProtocolHeaders()).containsExactly("rpc-v2-cbor");
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void getDashboard_whenTheServiceReturnsAModeledError_throwsItFromTheNewPipeline(Mode mode) {
        ScriptedHttp http = new ScriptedHttp().respond(404, cbor(members("__type", "DashboardNotFoundError",
                                                                         "message", "no such dashboard")));
        PipelineRecorder recorder = new PipelineRecorder();

        Throwable failure = failure(mode, http, recorder, c -> c.getDashboard(r -> r.dashboardName("missing")),
                                    c -> c.getDashboard(r -> r.dashboardName("missing")));

        assertThat(failure).isInstanceOf(DashboardNotFoundErrorException.class)
                           .hasMessageContaining("no such dashboard");
        AwsServiceException error = (AwsServiceException) failure;
        assertThat(error.statusCode()).isEqualTo(404);
        assertThat(error.requestId()).isEqualTo("request-404");
        assertThat(error.awsErrorDetails().errorCode()).isEqualTo("DashboardNotFoundError");
        recorder.assertRanOnTheNewPipeline(mode, CloudWatchOperations.GET_DASHBOARD);
        assertThat(recorder.hooks()).endsWith("beforeTransmission", "afterTransmission", "beforeUnmarshalling",
                                              "onExecutionFailure");
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void getDashboard_whenTheErrorIsOnlyInTheServiceRegistry_throwsItsModeledException(Mode mode) {
        // GetDashboard does not list ResourceNotFoundException; the service-wide Error_Registry resolves it (23.7).
        ScriptedHttp http = new ScriptedHttp().respond(404, cbor(members("__type", "ResourceNotFoundException",
                                                                         "message", "gone")));
        PipelineRecorder recorder = new PipelineRecorder();

        Throwable failure = failure(mode, http, recorder, c -> c.getDashboard(r -> r.dashboardName("x")),
                                    c -> c.getDashboard(r -> r.dashboardName("x")));

        assertThat(failure).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("gone");
        recorder.assertRanOnTheNewPipeline(mode, CloudWatchOperations.GET_DASHBOARD);
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void getDashboard_whenTheErrorTypeIsUnknown_throwsTheServiceBaseException(Mode mode) {
        ScriptedHttp http = new ScriptedHttp().respond(400, cbor(members("__type", "NoSuchErrorShape",
                                                                         "message", "unknown")));
        PipelineRecorder recorder = new PipelineRecorder();

        Throwable failure = failure(mode, http, recorder, c -> c.getDashboard(r -> r.dashboardName("x")),
                                    c -> c.getDashboard(r -> r.dashboardName("x")));

        assertThat(failure.getClass()).isEqualTo(CloudWatchException.class);
        assertThat(((CloudWatchException) failure).statusCode()).isEqualTo(400);
        assertThat(((CloudWatchException) failure).awsErrorDetails().errorCode()).isEqualTo("NoSuchErrorShape");
        recorder.assertRanOnTheNewPipeline(mode, CloudWatchOperations.GET_DASHBOARD);
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void listMetricsPaginator_withTwoPages_fetchesEachPageOnTheNewPipeline(Mode mode) {
        ScriptedHttp http = new ScriptedHttp().respond(200, cbor(page("CPUUtilization", "page-2")))
                                              .respond(200, cbor(page("NetworkIn", null)));
        PipelineRecorder recorder = new PipelineRecorder();
        ListMetricsRequest request = ListMetricsRequest.builder().namespace("AWS/EC2").build();
        List<ListMetricsResponse> pages = new CopyOnWriteArrayList<>();

        call(mode, http, recorder,
             c -> {
                 c.listMetricsPaginator(request).forEach(pages::add);
                 return null;
             },
             c -> c.listMetricsPaginator(request).subscribe(pages::add));

        assertThat(pages.stream().flatMap(p -> p.metrics().stream()).map(Metric::metricName))
            .containsExactly("CPUUtilization", "NetworkIn");
        assertThat(pages.get(0).nextToken()).isEqualTo("page-2");
        assertThat(http.requestCount()).isEqualTo(2);
        recorder.assertRanOnTheNewPipeline(mode, CloudWatchOperations.LIST_METRICS);
        assertThat(recorder.sentPaths()).containsExactly(SERVICE_PATH + "ListMetrics", SERVICE_PATH + "ListMetrics");
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void putMetricData_withRequestCompression_runsOnTheNewPipelineWithTheCompressionAttribute(Mode mode) {
        ScriptedHttp http = new ScriptedHttp().respond(200, new byte[0]);
        PipelineRecorder recorder = new PipelineRecorder();
        PutMetricDataRequest request = PutMetricDataRequest.builder()
                                                           .namespace("Demo")
                                                           .metricData(d -> d.metricName("Requests").value(1.0))
                                                           .build();

        PutMetricDataResponse response = call(mode, http, recorder, c -> c.putMetricData(request),
                                              c -> c.putMetricData(request));

        assertThat(response.sdkHttpResponse().statusCode()).isEqualTo(200);
        recorder.assertRanOnTheNewPipeline(mode, CloudWatchOperations.PUT_METRIC_DATA);
        assertThat(recorder.requestCompressionSet()).isTrue();
        assertThat(recorder.sentPaths()).containsExactly(SERVICE_PATH + "PutMetricData");
    }

    private static <T> T call(Mode mode, ScriptedHttp http, PipelineRecorder recorder,
                              Function<CloudWatchClient, T> sync,
                              Function<CloudWatchAsyncClient, CompletableFuture<?>> async) {
        try {
            return callOrThrow(mode, http, recorder, sync, async);
        } catch (Throwable t) {
            throw new AssertionError("The call failed", t);
        }
    }

    private static Throwable failure(Mode mode, ScriptedHttp http, PipelineRecorder recorder,
                                     Function<CloudWatchClient, ?> sync,
                                     Function<CloudWatchAsyncClient, CompletableFuture<?>> async) {
        Throwable failure = catchThrowable(() -> callOrThrow(mode, http, recorder, sync, async));
        assertThat(failure).as("the call's failure").isNotNull();
        return failure;
    }

    /**
     * Makes a call through the sync or async client, returning its result or throwing its failure (for async, the
     * cause of the future's {@code ExecutionException}).
     */
    @SuppressWarnings("unchecked")
    private static <T> T callOrThrow(Mode mode, ScriptedHttp http, PipelineRecorder recorder,
                                     Function<CloudWatchClient, T> sync,
                                     Function<CloudWatchAsyncClient, CompletableFuture<?>> async) throws Throwable {
        if (mode == Mode.SYNC) {
            try (CloudWatchClient client = configure(CloudWatchClient.builder(), recorder).httpClient(http.sync()).build()) {
                return sync.apply(client);
            }
        }
        try (CloudWatchAsyncClient client = configure(CloudWatchAsyncClient.builder(), recorder).httpClient(http.async())
                                                                                                .build()) {
            return (T) async.apply(client).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    private static <B extends AwsClientBuilder<B, ?>> B configure(B builder, PipelineRecorder recorder) {
        return builder.region(Region.US_WEST_2)
                      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                      .overrideConfiguration(o -> o.addExecutionInterceptor(recorder));
    }

    private static Map<String, Document> page(String metricName, String nextToken) {
        Map<String, Document> metric = members("Namespace", "AWS/EC2", "MetricName", metricName);
        Map<String, Document> page = new LinkedHashMap<>();
        page.put("Metrics", Document.of(Collections.singletonList(Document.of(metric))));
        if (nextToken != null) {
            page.put("NextToken", Document.of(nextToken));
        }
        return page;
    }

    private static Map<String, Document> members(String... keysAndValues) {
        Map<String, Document> members = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            members.put(keysAndValues[i], Document.of(keysAndValues[i + 1]));
        }
        return members;
    }

    /**
     * A CBOR map, encoded with the codec rather than by the code under test.
     */
    private static byte[] cbor(Map<String, Document> members) {
        ByteBuffer buffer = CBOR.serialize(Document.of(members));
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    /**
     * Records, at each read-only hook, which pipeline the hook was reached from (by the calling thread's stack), any
     * legacy pipeline frames on that stack, and the schema attributes.
     */
    private static final class PipelineRecorder implements ExecutionInterceptor {
        private final List<HookRecord> records = new CopyOnWriteArrayList<>();
        private final List<SdkHttpRequest> sent = new CopyOnWriteArrayList<>();

        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes attributes) {
            record("beforeExecution", attributes);
        }

        @Override
        public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes attributes) {
            record("beforeMarshalling", attributes);
        }

        @Override
        public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes attributes) {
            record("afterMarshalling", attributes);
        }

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes attributes) {
            record("beforeTransmission", attributes);
            sent.add(context.httpRequest());
        }

        @Override
        public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes attributes) {
            record("afterTransmission", attributes);
        }

        @Override
        public void beforeUnmarshalling(Context.BeforeUnmarshalling context, ExecutionAttributes attributes) {
            record("beforeUnmarshalling", attributes);
        }

        @Override
        public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes attributes) {
            record("afterUnmarshalling", attributes);
        }

        @Override
        public void afterExecution(Context.AfterExecution context, ExecutionAttributes attributes) {
            record("afterExecution", attributes);
        }

        @Override
        public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes attributes) {
            record("onExecutionFailure", attributes);
        }

        private void record(String hook, ExecutionAttributes attributes) {
            List<String> pipelines = new ArrayList<>();
            List<String> legacyFrames = new ArrayList<>();
            for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
                String className = frame.getClassName();
                if (className.startsWith(SYNC_PIPELINE) && !pipelines.contains(SYNC_PIPELINE)) {
                    pipelines.add(SYNC_PIPELINE);
                } else if (className.startsWith(ASYNC_PIPELINE) && !pipelines.contains(ASYNC_PIPELINE)) {
                    pipelines.add(ASYNC_PIPELINE);
                }
                if (LEGACY_PIPELINE_PREFIXES.stream().anyMatch(className::startsWith)
                    && !className.startsWith(SHARED_STAGE_UTILITIES)) {
                    legacyFrames.add(frame.toString());
                }
            }
            records.add(new HookRecord(hook, pipelines, legacyFrames,
                                       attributes.getAttribute(SdkExecutionAttribute.API_OPERATION),
                                       attributes.getAttribute(SdkExecutionAttribute.CLIENT_PROTOCOL) != null
                                       && attributes.getAttribute(SdkExecutionAttribute.CLIENT_PROTOCOL)
                                           instanceof RpcV2CborProtocol,
                                       attributes.getAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION)
                                       != null));
        }

        void assertRanOnTheNewPipeline(Mode mode, ApiOperation<?, ?> operation) {
            String pipeline = mode == Mode.SYNC ? SYNC_PIPELINE : ASYNC_PIPELINE;
            assertThat(records).isNotEmpty();
            for (HookRecord record : records) {
                assertThat(record.pipelines).as("pipelines on the stack at %s", record.hook)
                                            .containsExactly(pipeline);
                assertThat(record.legacyFrames).as("legacy pipeline frames on the stack at %s", record.hook)
                                               .isEmpty();
                assertThat(record.operation).as("API_OPERATION at %s", record.hook).isSameAs(operation);
                assertThat(record.rpcv2Cbor).as("CLIENT_PROTOCOL is rpcv2Cbor at %s", record.hook).isTrue();
            }
        }

        List<String> hooks() {
            return records.stream().map(r -> r.hook).collect(Collectors.toList());
        }

        boolean requestCompressionSet() {
            return records.stream().allMatch(r -> r.requestCompression);
        }

        List<String> sentPaths() {
            return sent.stream().map(SdkHttpRequest::encodedPath).collect(Collectors.toList());
        }

        List<String> sentProtocolHeaders() {
            return sent.stream()
                       .map(r -> r.firstMatchingHeader("smithy-protocol").orElse(null))
                       .collect(Collectors.toList());
        }
    }

    private static final class HookRecord {
        private final String hook;
        private final List<String> pipelines;
        private final List<String> legacyFrames;
        private final ApiOperation<?, ?> operation;
        private final boolean rpcv2Cbor;
        private final boolean requestCompression;

        private HookRecord(String hook, List<String> pipelines, List<String> legacyFrames, ApiOperation<?, ?> operation,
                           boolean rpcv2Cbor, boolean requestCompression) {
            this.hook = hook;
            this.pipelines = pipelines;
            this.legacyFrames = legacyFrames;
            this.operation = operation;
            this.rpcv2Cbor = rpcv2Cbor;
            this.requestCompression = requestCompression;
        }
    }

    /**
     * Answers each request with the next scripted rpcv2Cbor response, through a sync or async SDK HTTP client.
     */
    private static final class ScriptedHttp {
        private final Queue<Integer> statuses = new ConcurrentLinkedQueue<>();
        private final Queue<byte[]> bodies = new ConcurrentLinkedQueue<>();
        private final List<String> requests = new CopyOnWriteArrayList<>();

        ScriptedHttp respond(int status, byte[] body) {
            statuses.add(status);
            bodies.add(body);
            return this;
        }

        int requestCount() {
            return requests.size();
        }

        SdkHttpClient sync() {
            return new SdkHttpClient() {
                @Override
                public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
                    return new ExecutableHttpRequest() {
                        @Override
                        public HttpExecuteResponse call() throws IOException {
                            requests.add(request.httpRequest().encodedPath());
                            int status = next();
                            byte[] body = bodies.remove();
                            return HttpExecuteResponse.builder()
                                                      .response(headers(status, body))
                                                      .responseBody(AbortableInputStream.create(
                                                          new ByteArrayInputStream(body)))
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
            };
        }

        SdkAsyncHttpClient async() {
            return new SdkAsyncHttpClient() {
                @Override
                public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
                    requests.add(request.request().encodedPath());
                    int status = next();
                    byte[] body = bodies.remove();
                    request.responseHandler().onHeaders(headers(status, body));
                    request.responseHandler().onStream(
                        SdkPublisher.fromIterable(Collections.singletonList(ByteBuffer.wrap(body))));
                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public void close() {
                }
            };
        }

        private int next() {
            Integer status = statuses.poll();
            if (status == null) {
                throw new IllegalStateException("No scripted response for request " + requests.size());
            }
            return status;
        }

        private static SdkHttpFullResponse headers(int status, byte[] body) {
            return SdkHttpFullResponse.builder()
                                      .statusCode(status)
                                      .putHeader("Content-Length", String.valueOf(body.length))
                                      .putHeader("Content-Type", "application/cbor")
                                      .putHeader("smithy-protocol", "rpc-v2-cbor")
                                      .putHeader("x-amzn-RequestId", "request-" + status)
                                      .build();
        }
    }
}
