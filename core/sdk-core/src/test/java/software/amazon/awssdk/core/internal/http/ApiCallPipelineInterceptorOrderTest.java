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

package software.amazon.awssdk.core.internal.http;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.CLIENT_ENDPOINT;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.marshalledRequest;
import static software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.OPERATION;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SdkAsyncClientHandler;
import software.amazon.awssdk.core.client.handler.SdkSyncClientHandler;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.HttpStubProtocol;
import software.amazon.awssdk.core.internal.http.HookRecordingInterceptor.HookRecord;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestRequest;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestResponse;
import software.amazon.awssdk.core.internal.http.transport.TransportExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Runs the same call through the legacy pipeline and the schema-based pipeline path (Requirements 37.5, 37.8, 30.2),
 * through the SDK client handlers, sync and async, and compares what a registered {@code ExecutionInterceptor} sees:
 * the hook sequence, the {@code Context} each hook receives (its type and the request, HTTP request, request body,
 * HTTP response, response body and response it exposes) and the {@code ExecutionAttributes} keys present at each hook,
 * plus the call's result and number of attempts.
 *
 * <p>The legacy call uses a marshaller and response handlers that produce the same request and outputs as the stub
 * protocol of the new path, and both answer from one {@link ScriptedExchange}. The new path runs both over the default
 * SDK HTTP client transport (the same stub HTTP client as the legacy call) and over a configured
 * {@code ClientTransport}.
 *
 * <p>The few differences the design accepts are applied to the legacy record by {@link AllowedDifferences}, each with
 * the reason; anything else fails. They are listed in {@code docs/design/schema-serde/porting-deviations.md} (task
 * 3.29).
 */
class ApiCallPipelineInterceptorOrderTest {
    private static final long TIMEOUT_SECONDS = 10;
    private static final String HOST_PREFIX = "data.";
    private static final String MARSHALLING_FAILURE = "Unable to marshall request: test failure";
    private static final String COMPLETION_THREAD = "future-completion-executor";
    private static final String HEADERS_ONLY = "headers-only";

    enum Mode {
        SYNC, ASYNC
    }

    enum Pipeline {
        LEGACY, NEW_PATH_SDK_TRANSPORT, NEW_PATH_STUB_TRANSPORT
    }

    /**
     * The calls compared. {@code failureReports} is the number of {@code onExecutionFailure} calls the Baseline makes.
     */
    enum Scenario {
        SUCCESS(e -> e.respond(200, "hello"), null, 0),
        SERVICE_ERROR(e -> e.respond(400, "bad"), null, 1),
        CLIENT_ERROR(e -> e.failWithIoException("connection reset")
                          .failWithIoException("connection reset")
                          .failWithIoException("connection reset"), null, 1),
        RETRY_THEN_SUCCESS(e -> e.respond(500, "busy").respond(200, "hello"), null, 0),
        CLIENT_ERROR_THEN_SUCCESS(e -> e.failWithIoException("connection reset").respond(200, "hello"), null, 0),
        RETRIES_EXHAUSTED(e -> e.respond(503, "busy").respond(503, "busy").respond(503, "busy"), null, 1),
        TRANSPORT_RUNTIME_EXCEPTION(e -> e.failWithRuntimeException("unexpected")
                                         .failWithRuntimeException("unexpected")
                                         .failWithRuntimeException("unexpected"), null, 1),
        MODIFY_REQUEST_FAILURE(e -> { }, "modifyRequest", 0),
        BEFORE_TRANSMISSION_FAILURE(e -> e.respond(200, "hello"), "beforeTransmission", 1),
        AFTER_TRANSMISSION_FAILURE(e -> e.respond(200, "hello"), "afterTransmission", 1),
        AFTER_UNMARSHALLING_FAILURE(e -> e.respond(200, "hello"), "afterUnmarshalling", 1),
        MODIFY_RESPONSE_FAILURE(e -> e.respond(200, "hello"), "modifyResponse", 1),
        AFTER_EXECUTION_FAILURE(e -> e.respond(200, "hello"), "afterExecution", 1);

        private final Consumer<ScriptedExchange> script;
        private final String failingHook;
        private final int failureReports;

        Scenario(Consumer<ScriptedExchange> script, String failingHook, int failureReports) {
            this.script = script;
            this.failingHook = failingHook;
            this.failureReports = failureReports;
        }
    }

    /**
     * Failures before the request is sent that the new path reports to {@code modifyException}/{@code onExecutionFailure}
     * and the Baseline does not (Requirement 37.8).
     */
    enum EarlyFailure {
        BEFORE_MARSHALLING("beforeMarshalling"), SERIALIZATION(null), AFTER_MARSHALLING("afterMarshalling"),
        MODIFY_HTTP_REQUEST("modifyHttpRequest");

        private final String failingHook;

        EarlyFailure(String failingHook) {
            this.failingHook = failingHook;
        }
    }

    static Stream<Arguments> calls() {
        List<Arguments> arguments = new ArrayList<>();
        for (Mode mode : Mode.values()) {
            for (Pipeline newPath : Arrays.asList(Pipeline.NEW_PATH_SDK_TRANSPORT, Pipeline.NEW_PATH_STUB_TRANSPORT)) {
                for (Scenario scenario : Scenario.values()) {
                    for (boolean collectMetrics : new boolean[] {false, true}) {
                        arguments.add(Arguments.of(mode, newPath, scenario, collectMetrics));
                    }
                }
            }
        }
        return arguments.stream();
    }

    static Stream<Arguments> earlyFailures() {
        List<Arguments> arguments = new ArrayList<>();
        for (Mode mode : Mode.values()) {
            for (EarlyFailure failure : EarlyFailure.values()) {
                arguments.add(Arguments.of(mode, failure));
            }
        }
        return arguments.stream();
    }

    @ParameterizedTest(name = "{0} {1} {2} collectMetrics={3}")
    @MethodSource("calls")
    void execute_onTheNewPath_invokesTheSameHooksWithTheSameContextsAndAttributesAsTheLegacyPipeline(
        Mode mode, Pipeline newPath, Scenario scenario, boolean collectMetrics) {
        CallOptions options = new CallOptions(mode, scenario.script, scenario.failingHook, collectMetrics);

        Outcome legacy = run(Pipeline.LEGACY, options);
        Outcome actual = run(newPath, options);

        assertThat(legacy.count("onExecutionFailure")).as("Baseline failure reports").isEqualTo(scenario.failureReports);
        assertThat(legacy.count("modifyException")).isEqualTo(scenario.failureReports);
        assertThat(actual.hooks()).isEqualTo(legacy.hooks());
        assertThat(actual.contexts()).isEqualTo(AllowedDifferences.contexts(options, legacy.records));
        assertThat(actual.attributeKeys()).isEqualTo(AllowedDifferences.attributeKeys(newPath, options, legacy.records));
        assertThat(actual.result).isEqualTo(AllowedDifferences.result(options, legacy.result));
        assertThat(actual.attempts).isEqualTo(legacy.attempts);
    }

    static Stream<Arguments> streamingCalls() {
        List<Arguments> arguments = new ArrayList<>();
        List<Scenario> scenarios = Arrays.asList(Scenario.SUCCESS, Scenario.SERVICE_ERROR, Scenario.RETRY_THEN_SUCCESS,
                                                 Scenario.CLIENT_ERROR_THEN_SUCCESS, Scenario.RETRIES_EXHAUSTED,
                                                 Scenario.AFTER_TRANSMISSION_FAILURE, Scenario.MODIFY_RESPONSE_FAILURE,
                                                 Scenario.AFTER_EXECUTION_FAILURE);
        for (Mode mode : Mode.values()) {
            for (Pipeline newPath : Arrays.asList(Pipeline.NEW_PATH_SDK_TRANSPORT, Pipeline.NEW_PATH_STUB_TRANSPORT)) {
                for (Scenario scenario : scenarios) {
                    arguments.add(Arguments.of(mode, newPath, scenario));
                }
            }
        }
        return arguments.stream();
    }

    /**
     * Streaming outputs (task 7.19): a {@code ResponseTransformer} or {@code AsyncResponseTransformer} call sees the same
     * hooks, contexts and attributes on both pipelines. For an async call that is the legacy
     * {@code AsyncStreamingResponseHandler} order: {@code afterUnmarshalling} and {@code modifyResponse} run on the
     * headers, before {@code modifyAsyncHttpResponseContent}; an error response is buffered and keeps the non-streaming
     * order. The new path's async attempts also carry {@link TransportExecutionAttribute#ABORTABLE_LISTENER} once sent.
     */
    @ParameterizedTest(name = "{0} {1} {2}")
    @MethodSource("streamingCalls")
    void execute_withAStreamingOutputOnTheNewPath_invokesTheSameHooksAsTheLegacyPipeline(
        Mode mode, Pipeline newPath, Scenario scenario) {
        CallOptions options = new CallOptions(mode, scenario.script, scenario.failingHook, false);
        options.streaming = true;

        Outcome legacy = run(Pipeline.LEGACY, options);
        Outcome actual = run(newPath, options);

        assertThat(actual.hooks()).isEqualTo(legacy.hooks());
        assertThat(actual.contexts()).isEqualTo(AllowedDifferences.contexts(options, legacy.records));
        assertThat(actual.attributeKeys()).isEqualTo(AllowedDifferences.attributeKeys(newPath, options, legacy.records));
        assertThat(actual.result).isEqualTo(AllowedDifferences.result(options, legacy.result));
        assertThat(actual.attempts).isEqualTo(legacy.attempts);
    }

    /**
     * Allowed difference 3: on the Baseline, a marshaller failure or an interceptor failure from
     * {@code beforeMarshalling} to {@code modifyHttpRequest} ends the call without {@code modifyException} and
     * {@code onExecutionFailure} (the reporting stage wraps only the HTTP client's stage graph); the new path reports it
     * once, as Requirement 37.8 requires for a serialization failure. The caller gets the same exception.
     */
    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("earlyFailures")
    void execute_withAnEarlyFailure_isReportedOnceOnTheNewPathOnly(Mode mode, EarlyFailure failure) {
        CallOptions options = new CallOptions(mode, e -> { }, failure.failingHook, false);
        options.failSerialization = failure == EarlyFailure.SERIALIZATION;

        Outcome legacy = run(Pipeline.LEGACY, options);
        Outcome actual = run(Pipeline.NEW_PATH_STUB_TRANSPORT, options);

        assertThat(legacy.hooks()).doesNotContain("modifyException", "onExecutionFailure");
        List<String> expectedHooks = new ArrayList<>(legacy.hooks());
        expectedHooks.addAll(Arrays.asList("modifyException", "onExecutionFailure"));
        assertThat(actual.hooks()).isEqualTo(expectedHooks);
        int common = legacy.records.size();
        assertThat(actual.contexts().subList(0, common)).isEqualTo(AllowedDifferences.contexts(options, legacy.records));
        assertThat(actual.attributeKeys().subList(0, common))
            .isEqualTo(AllowedDifferences.attributeKeys(Pipeline.NEW_PATH_STUB_TRANSPORT, options, legacy.records));
        assertThat(actual.count("onExecutionFailure")).isEqualTo(1);
        // The hooks get the failure as thrown; an async caller gets it as an SdkException, as on the Baseline.
        String reported = failure == EarlyFailure.SERIALIZATION
                          ? "SdkClientException: " + MARSHALLING_FAILURE
                          : "IllegalStateException: Interceptor failure in " + failure.failingHook;
        assertThat(actual.records.get(common).context()).startsWith("context=DefaultFailedExecutionContext, exception="
                                                                    + reported + ",");
        assertThat(actual.records.get(common + 1).context()).isEqualTo(actual.records.get(common).context());
        assertThat(actual.result).isEqualTo(legacy.result);
        assertThat(actual.attempts).isZero().isEqualTo(legacy.attempts);
    }

    /**
     * Allowed difference 2: the SDK protocols set {@code sdkHttpResponse} on the output builder (task 3.21), so every
     * hook that exposes the output ({@code afterUnmarshalling}, {@code modifyResponse}, {@code afterExecution} and the
     * failure hooks after them) sees it. Legacy unmarshallers leave it null, and the legacy handler attaches it to a copy
     * after {@code modifyResponse} that only the caller receives, so no legacy hook sees it. The caller gets the same
     * output on both.
     */
    @ParameterizedTest
    @EnumSource(Mode.class)
    void execute_onTheNewPath_exposesTheHttpResponseOnTheOutputToEveryHookThatSeesTheOutput(Mode mode) {
        CallOptions options = new CallOptions(mode, e -> e.respond(200, "hello"), null, false);

        Outcome legacy = run(Pipeline.LEGACY, options);
        Outcome actual = run(Pipeline.NEW_PATH_STUB_TRANSPORT, options);

        for (String hook : Arrays.asList("afterUnmarshalling", "modifyResponse", "afterExecution")) {
            assertThat(legacy.context(hook)).as(hook).endsWith("response=TestResponse(value=hello, sdkHttpResponse=none)");
            assertThat(actual.context(hook)).as(hook).endsWith("response=TestResponse(value=hello, sdkHttpResponse=200)");
        }
        assertThat(actual.result).isEqualTo(legacy.result).isEqualTo("output=hello, sdkHttpResponse=200");
    }

    /**
     * Difference 1 (resolved in task 3.25): {@code afterMarshalling} and {@code modifyHttpRequest} see the request
     * addressed to the client endpoint with the host prefix, as legacy marshallers address it, not
     * {@code ProtocolMarshalling.UNRESOLVED_URI}.
     */
    @ParameterizedTest
    @EnumSource(Mode.class)
    void execute_onTheNewPath_addressesTheRequestBeforeAfterMarshalling(Mode mode) {
        CallOptions options = new CallOptions(mode, e -> e.respond(200, "hello"), null, false);

        Outcome legacy = run(Pipeline.LEGACY, options);
        Outcome actual = run(Pipeline.NEW_PATH_STUB_TRANSPORT, options);

        String expectedUri = "https://" + HOST_PREFIX + CLIENT_ENDPOINT.getHost() + ApiCallStagesTestSupport.OPERATION_PATH;
        assertThat(legacy.context("afterMarshalling")).contains("httpRequest=POST " + expectedUri + " ");
        assertThat(actual.context("afterMarshalling")).isEqualTo(legacy.context("afterMarshalling"));
        assertThat(actual.context("modifyHttpRequest")).isEqualTo(legacy.context("modifyHttpRequest"));
    }

    /**
     * Difference 5: an async API call timeout ends the call with {@code ApiCallTimeoutException} and, as on the
     * Baseline, without {@code modifyException} or {@code onExecutionFailure}.
     */
    @Test
    void asyncExecute_whenTheApiCallTimesOut_runsNoFailureHooksAsTheLegacyPipeline() {
        CallOptions options = new CallOptions(Mode.ASYNC, ScriptedExchange::leavePending, null, false);
        options.apiCallTimeout = Duration.ofMillis(200);

        Outcome legacy = run(Pipeline.LEGACY, options);
        Outcome actual = run(Pipeline.NEW_PATH_SDK_TRANSPORT, options);
        Outcome stubTransport = run(Pipeline.NEW_PATH_STUB_TRANSPORT, options);

        assertThat(legacy.result).startsWith("failure=" + ApiCallTimeoutException.class.getSimpleName());
        assertThat(legacy.hooks()).doesNotContain("modifyException", "onExecutionFailure");
        assertSameHooksAsLegacy(legacy, actual, Pipeline.NEW_PATH_SDK_TRANSPORT, options);
        assertSameHooksAsLegacy(legacy, stubTransport, Pipeline.NEW_PATH_STUB_TRANSPORT, options);
    }

    /**
     * Difference 5: cancelling the returned future stops the call and, as on the Baseline, runs no failure hooks.
     */
    @Test
    void asyncExecute_whenTheCallerCancels_runsNoFailureHooksAsTheLegacyPipeline() {
        CallOptions options = new CallOptions(Mode.ASYNC, ScriptedExchange::leavePending, null, false);
        options.cancelWhenPending = true;

        Outcome legacy = run(Pipeline.LEGACY, options);
        Outcome actual = run(Pipeline.NEW_PATH_SDK_TRANSPORT, options);
        Outcome stubTransport = run(Pipeline.NEW_PATH_STUB_TRANSPORT, options);

        assertThat(legacy.result).isEqualTo("failure=" + CancellationException.class.getSimpleName());
        assertThat(legacy.hooks()).doesNotContain("modifyException", "onExecutionFailure");
        assertSameHooksAsLegacy(legacy, actual, Pipeline.NEW_PATH_SDK_TRANSPORT, options);
        assertSameHooksAsLegacy(legacy, stubTransport, Pipeline.NEW_PATH_STUB_TRANSPORT, options);
    }

    /**
     * Difference 4 (threads only; order and contexts are compared above): on the Baseline the response hooks
     * ({@code afterTransmission} … {@code modifyResponse}) run on the thread that delivers the HTTP response (here the
     * calling thread, where the stub HTTP client answers) and only {@code afterExecution} on
     * {@code FUTURE_COMPLETION_EXECUTOR}. The new path handles each response on {@code FUTURE_COMPLETION_EXECUTOR}, so
     * all of them run there. The request hooks run on the calling thread on both.
     */
    @ParameterizedTest
    @EnumSource(value = Pipeline.class, names = {"NEW_PATH_SDK_TRANSPORT", "NEW_PATH_STUB_TRANSPORT"})
    void asyncExecute_onTheNewPath_runsTheResponseHooksOnTheFutureCompletionExecutor(Pipeline newPath) {
        CallOptions options = new CallOptions(Mode.ASYNC, e -> e.respond(200, "hello"), null, false);
        String caller = Thread.currentThread().getName();

        Outcome legacy = run(Pipeline.LEGACY, options);
        Outcome actual = run(newPath, options);

        List<String> responseHooks = Arrays.asList("afterTransmission", "modifyHttpResponse", "modifyHttpResponseContent",
                                                   "beforeUnmarshalling", "modifyAsyncHttpResponseContent",
                                                   "afterUnmarshalling", "modifyResponse");
        assertThat(actual.hooks()).isEqualTo(legacy.hooks());
        assertThat(legacy.threads()).isEqualTo(legacy.hooks().stream()
                                                     .map(h -> h + ": " + (h.equals("afterExecution") ? COMPLETION_THREAD
                                                                                                       : caller))
                                                     .collect(Collectors.toList()));
        assertThat(actual.threads()).isEqualTo(actual.hooks().stream()
                                                     .map(h -> h + ": " + (h.equals("afterExecution")
                                                                           || responseHooks.contains(h) ? COMPLETION_THREAD
                                                                                                         : caller))
                                                     .collect(Collectors.toList()));
    }

    private static void assertSameHooksAsLegacy(Outcome legacy, Outcome actual, Pipeline newPath, CallOptions options) {
        assertThat(actual.hooks()).isEqualTo(legacy.hooks());
        assertThat(actual.contexts()).isEqualTo(AllowedDifferences.contexts(options, legacy.records));
        assertThat(actual.attributeKeys()).isEqualTo(AllowedDifferences.attributeKeys(newPath, options, legacy.records));
        assertThat(actual.result).isEqualTo(legacy.result);
        assertThat(actual.pendingRequestsDone).as("the in-flight request is cancelled").isTrue();
        assertThat(legacy.pendingRequestsDone).as("the in-flight request is cancelled").isTrue();
    }

    private Outcome run(Pipeline pipeline, CallOptions options) {
        ScriptedExchange exchange = new ScriptedExchange();
        options.script.accept(exchange);
        HookRecordingInterceptor interceptor = new HookRecordingInterceptor(options.failingHook);
        OrderTestProtocol protocol = new OrderTestProtocol(options.failSerialization, options.streaming);
        SdkClientConfiguration config = config(pipeline, options, exchange, interceptor, protocol);
        ClientExecutionParams<TestRequest, TestResponse> params =
            pipeline == Pipeline.LEGACY ? legacyParams(options) : newPathParams();
        params.withMetricCollector(options.collectMetrics ? MetricCollector.create("ApiCall") : NoOpMetricCollector.create());

        String result;
        boolean pendingDone = false;
        if (options.mode == Mode.SYNC) {
            try (SdkSyncClientHandler handler = new SdkSyncClientHandler(config) {
            }) {
                result = options.streaming
                         ? handler.execute(params, (response, body) -> streamed(response, IoUtils.toUtf8String(body)))
                         : success(handler.execute(params));
            } catch (RuntimeException e) {
                result = failure(e);
            }
        } else if (options.streaming) {
            try (SdkAsyncClientHandler handler = new SdkAsyncClientHandler(config)) {
                CompletableFuture<String> future =
                    handler.execute(params, AsyncResponseTransformer.<TestResponse>toBytes())
                           .thenApply(bytes -> streamed(bytes.response(), bytes.asUtf8String()));
                result = await(future, Function.identity());
            }
        } else {
            try (SdkAsyncClientHandler handler = new SdkAsyncClientHandler(config)) {
                CompletableFuture<TestResponse> future = handler.execute(params);
                if (options.cancelWhenPending) {
                    awaitPending(exchange);
                    future.cancel(true);
                }
                result = await(future);
                pendingDone = awaitPendingDone(exchange);
            }
        }
        return new Outcome(interceptor.records(), exchange.requests().size(), result, pendingDone);
    }

    private static SdkClientConfiguration config(Pipeline pipeline,
                                                 CallOptions options,
                                                 ScriptedExchange exchange,
                                                 HookRecordingInterceptor interceptor,
                                                 OrderTestProtocol protocol) {
        // Each handler closes its configuration, which shuts the executors down, so every call gets its own.
        ExecutorService completionExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, COMPLETION_THREAD);
            thread.setDaemon(true);
            return thread;
        });
        SdkClientConfiguration.Builder builder =
            ApiCallStagesTestSupport.baseConfig()
                                    .option(SdkClientOption.EXECUTION_INTERCEPTORS,
                                            new ArrayList<>(Collections.singletonList(interceptor)))
                                    .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false)
                                    .option(SdkAdvancedClientOption.USER_AGENT_PREFIX, "")
                                    .option(SdkAdvancedClientOption.USER_AGENT_SUFFIX, "")
                                    .option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE, Executors.newScheduledThreadPool(1))
                                    .option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, completionExecutor)
                                    .option(SdkClientOption.API_CALL_TIMEOUT, options.apiCallTimeout)
                                    .option(SdkClientOption.CLIENT_PROTOCOL, protocol);
        if (options.mode == Mode.SYNC) {
            builder.option(SdkClientOption.SYNC_HTTP_CLIENT, exchange.httpClient());
            if (pipeline == Pipeline.NEW_PATH_STUB_TRANSPORT) {
                builder.option(SdkClientOption.CLIENT_TRANSPORT, exchange.transport());
            }
        } else {
            builder.option(SdkClientOption.ASYNC_HTTP_CLIENT, exchange.asyncHttpClient());
            if (pipeline == Pipeline.NEW_PATH_STUB_TRANSPORT) {
                builder.option(SdkClientOption.ASYNC_CLIENT_TRANSPORT, exchange.asyncTransport());
            }
        }
        return builder.build();
    }

    private static ClientExecutionParams<TestRequest, TestResponse> newPathParams() {
        return new ClientExecutionParams<TestRequest, TestResponse>().withOperation(OPERATION)
                                                                     .withInput(new TestRequest("name"))
                                                                     .hostPrefixExpression(HOST_PREFIX);
    }

    /**
     * The same call as a generated client makes it on the legacy pipeline: a marshaller that addresses the request to
     * the client endpoint, and response handlers that create the outputs the stub protocol creates (legacy
     * unmarshallers do not set {@code sdkHttpResponse}).
     */
    private static ClientExecutionParams<TestRequest, TestResponse> legacyParams(CallOptions options) {
        return new ClientExecutionParams<TestRequest, TestResponse>()
            .withInput(new TestRequest("name"))
            .withOperationName(OPERATION.name())
            .hostPrefixExpression(HOST_PREFIX)
            .withMarshaller(request -> {
                if (options.failSerialization) {
                    throw SdkClientException.create(MARSHALLING_FAILURE);
                }
                return marshalledRequest(CLIENT_ENDPOINT, "{}");
            })
            .withResponseHandler((response, attributes) -> TestResponse.builder()
                                                                       .value(options.streaming ? HEADERS_ONLY : read(response))
                                                                       .build())
            .withErrorResponseHandler((response, attributes) -> serviceError(response));
    }

    private static SdkServiceException serviceError(SdkHttpFullResponse response) {
        return SdkServiceException.builder()
                                  .statusCode(response.statusCode())
                                  .message("Service error " + response.statusCode())
                                  .build();
    }

    private static String read(SdkHttpFullResponse response) {
        try {
            return IoUtils.toUtf8String(response.content().get());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String success(TestResponse output) {
        return "output=" + output.value() + ", sdkHttpResponse=" + output.sdkHttpResponse().statusCode();
    }

    private static String failure(Throwable failure) {
        StringBuilder result = new StringBuilder("failure=").append(HookRecordingInterceptor.describeException(failure));
        if (failure.getCause() != null) {
            result.append(", cause=").append(failure.getCause().getClass().getSimpleName());
        }
        return result.toString();
    }

    private static String await(CompletableFuture<TestResponse> future) {
        return await(future, ApiCallPipelineInterceptorOrderTest::success);
    }

    private static String streamed(TestResponse output, String body) {
        return "streamed=" + output.value() + ":" + body + ", sdkHttpResponse=" + output.sdkHttpResponse().statusCode();
    }

    private static <T> String await(CompletableFuture<T> future, Function<T, String> describe) {
        try {
            return describe.apply(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } catch (ExecutionException e) {
            return failure(e.getCause());
        } catch (CancellationException e) {
            return "failure=" + CancellationException.class.getSimpleName();
        } catch (Exception e) {
            throw new AssertionError("The call did not complete", e);
        }
    }

    private static void awaitPending(ScriptedExchange exchange) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (exchange.pending().isEmpty()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("The request was not sent");
            }
            Thread.yield();
        }
    }

    /**
     * Waits for every pending request's future to be failed by the pipeline, which cancels the HTTP request.
     */
    private static boolean awaitPendingDone(ScriptedExchange exchange) {
        for (CompletableFuture<?> pending : exchange.pending()) {
            try {
                pending.handle((r, t) -> null).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                return false;
            }
        }
        return !exchange.pending().isEmpty();
    }

    /**
     * The differences between the legacy pipeline and the new path that the design accepts, applied to the legacy
     * record to give the new path's expected record; anything else is a failure. See the task 3.29 section of
     * {@code docs/design/schema-serde/porting-deviations.md}.
     */
    private static final class AllowedDifferences {
        /**
         * Seeded before {@code beforeExecution}: the H10 template's client-level attributes (the aws-core context
         * factory writes the same ones; this test's sdk-core factory, {@code BaseClientHandler}'s, writes fewer) and the
         * call's {@code ApiOperation} and error registry, so every hook sees the operation the protocol serializes
         * (Requirement 30.3).
         */
        private static final List<String> SEEDED_KEYS = seededKeys();

        /**
         * Sync: {@link TransportExecutionAttribute#ABORTABLE_LISTENER}, written once per call after the context factory,
         * so present from {@code beforeMarshalling} on.
         */
        private static final String ABORTABLE_LISTENER = TransportExecutionAttribute.ABORTABLE_LISTENER.toString();

        /**
         * Written for each attempt before the transport is called, so present in every hook after the first request is
         * sent: {@link TransportExecutionAttribute#HTTP_METRIC_COLLECTOR}, async also
         * {@link TransportExecutionAttribute#ASYNC_REQUEST_BODY} (null without a streaming body) and, with the SDK async
         * transport, its owned response body ({@code OwnedResponseBody}, a private attribute cleared by each send).
         */
        private static final String HTTP_METRIC_COLLECTOR = TransportExecutionAttribute.HTTP_METRIC_COLLECTOR.toString();
        private static final String ASYNC_REQUEST_BODY = TransportExecutionAttribute.ASYNC_REQUEST_BODY.toString();
        private static final String OWNED_RESPONSE_BODY = "OwnedResponseBody";

        private static final Pattern HTTP_STATUS = Pattern.compile("(?<![A-Za-z])httpResponse=(\\d{3}) ");

        private AllowedDifferences() {
        }

        private static List<String> seededKeys() {
            List<String> keys = new ArrayList<>();
            ExecutionAttributesTemplate.builder(SdkClientConfiguration.builder().build()).build().attributes()
                                       .forEach(attribute -> keys.add(attribute.toString()));
            keys.add(SdkExecutionAttribute.API_OPERATION.toString());
            keys.add(SdkExecutionAttribute.ERROR_REGISTRY.toString());
            return Collections.unmodifiableList(keys);
        }

        static List<String> contexts(CallOptions options, List<HookRecord> legacy) {
            return legacy.stream()
                         .map(r -> r.hook() + ": " + exceptionMessage(options, outputWithHttpResponse(r.context())))
                         .collect(Collectors.toList());
        }

        static String result(CallOptions options, String legacyResult) {
            return exceptionMessage(options, legacyResult);
        }

        /**
         * Difference 2: every output a hook sees holds the HTTP response it was read from; the legacy handler attaches
         * it to a copy only the caller receives.
         */
        private static String outputWithHttpResponse(String context) {
            if (!context.contains("sdkHttpResponse=none")) {
                return context;
            }
            Matcher status = HTTP_STATUS.matcher(context);
            if (!status.find()) {
                return context;
            }
            return context.replace("sdkHttpResponse=none", "sdkHttpResponse=" + status.group(1));
        }

        /**
         * Difference 7: an interceptor failure in {@code afterUnmarshalling} or {@code modifyResponse} of an async call
         * is classified as on the sync legacy pipeline ({@code CombinedResponseHandler}: "Unable to unmarshall
         * response"), because both pipelines share {@code ProtocolResponseHandling} (Requirement 37.6). The legacy
         * async pipeline gives the raw exception to its retry stage, which wraps it as "Unable to execute HTTP
         * request". Same exception type, cause and attempts.
         */
        private static String exceptionMessage(CallOptions options, String text) {
            if (options.mode != Mode.ASYNC
                || !("afterUnmarshalling".equals(options.failingHook) || "modifyResponse".equals(options.failingHook))) {
                return text;
            }
            String failure = "Interceptor failure in " + options.failingHook;
            return text.replace("Unable to execute HTTP request: " + failure,
                                "Unable to unmarshall response (" + failure + "). Response Code: 200, Response Text: null");
        }

        static List<String> attributeKeys(Pipeline newPath, CallOptions options, List<HookRecord> legacy) {
            List<String> result = new ArrayList<>();
            boolean sent = false;
            for (HookRecord record : legacy) {
                TreeSet<String> keys = new TreeSet<>(record.attributeKeys());
                keys.addAll(SEEDED_KEYS);
                boolean afterContextFactory = !record.hook().equals("beforeExecution")
                                              && !record.hook().equals("modifyRequest");
                if (options.mode == Mode.SYNC && afterContextFactory) {
                    keys.add(ABORTABLE_LISTENER);
                }
                if (sent) {
                    keys.add(HTTP_METRIC_COLLECTOR);
                    if (options.mode == Mode.ASYNC) {
                        keys.add(ASYNC_REQUEST_BODY);
                        if (options.streaming) {
                            // Each streaming attempt registers the HTTP request's abort with the async transport.
                            keys.add(ABORTABLE_LISTENER);
                        }
                        if (newPath == Pipeline.NEW_PATH_SDK_TRANSPORT) {
                            keys.add(OWNED_RESPONSE_BODY);
                        }
                    }
                }
                result.add(record.hook() + ": " + new ArrayList<>(keys));
                if (record.hook().equals("beforeTransmission") && !record.hook().equals(options.failingHook)) {
                    sent = true;
                }
            }
            return result;
        }
    }

    /**
     * What a call does besides its scenario.
     */
    private static final class CallOptions {
        private final Mode mode;
        private final Consumer<ScriptedExchange> script;
        private final String failingHook;
        private final boolean collectMetrics;
        private boolean failSerialization;
        private boolean cancelWhenPending;
        private boolean streaming;
        private Duration apiCallTimeout;

        private CallOptions(Mode mode, Consumer<ScriptedExchange> script, String failingHook, boolean collectMetrics) {
            this.mode = mode;
            this.script = script;
            this.failingHook = failingHook;
            this.collectMetrics = collectMetrics;
        }
    }

    private static final class Outcome {
        private final List<HookRecord> records;
        private final int attempts;
        private final String result;
        private final boolean pendingRequestsDone;

        private Outcome(List<HookRecord> records, int attempts, String result, boolean pendingRequestsDone) {
            this.records = records;
            this.attempts = attempts;
            this.result = result;
            this.pendingRequestsDone = pendingRequestsDone;
        }

        List<String> hooks() {
            return records.stream().map(HookRecord::hook).collect(Collectors.toList());
        }

        List<String> contexts() {
            return records.stream().map(r -> r.hook() + ": " + r.context()).collect(Collectors.toList());
        }

        List<String> attributeKeys() {
            return records.stream().map(r -> r.hook() + ": " + r.attributeKeys()).collect(Collectors.toList());
        }

        List<String> threads() {
            return records.stream().map(r -> r.hook() + ": " + r.thread()).collect(Collectors.toList());
        }

        long count(String hook) {
            return records.stream().filter(r -> r.hook().equals(hook)).count();
        }

        String context(String hook) {
            return records.stream().filter(r -> r.hook().equals(hook)).findFirst()
                          .map(HookRecord::context)
                          .orElseThrow(() -> new AssertionError("No " + hook + " in " + hooks()));
        }
    }

    /**
     * An HTTP protocol that serializes the request a legacy marshaller produces and creates the outputs and errors the
     * legacy response handlers create, setting {@code sdkHttpResponse} on the output builder as the SDK protocols do.
     */
    private static final class OrderTestProtocol extends HttpStubProtocol {
        private final boolean streaming;

        OrderTestProtocol(boolean failSerialization, boolean streaming) {
            this.streaming = streaming;
            if (failSerialization) {
                serialize = endpoint -> {
                    throw SdkClientException.create(MARSHALLING_FAILURE);
                };
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I extends SerializableStruct, O extends SerializableStruct> O deserializeResponse(
            ApiOperation<I, O> operation, ExecutionAttributes context, TypeRegistry errorRegistry,
            SdkHttpFullRequest request, SdkHttpFullResponse response) {
            if (!response.isSuccessful()) {
                throw serviceError(response);
            }
            // A streaming output's body is left unread for the transformer.
            SdkResponse.Builder builder = TestResponse.builder()
                                                      .value(streaming ? HEADERS_ONLY : read(response))
                                                      .sdkHttpResponse(response);
            return (O) builder.build();
        }
    }
}
