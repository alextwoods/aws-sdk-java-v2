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
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.codecs.cbor.Rpcv2CborCodec;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.http.transport.TransportExecutionAttribute;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.services.cloudwatch.model.GetDashboardRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetDashboardResponse;

/**
 * The interceptor hooks of the generated sync and async CloudWatch clients (Requirements 37.5, 37.8, 30.2): for a
 * {@code GetDashboard} call over a stub SDK HTTP client answering in rpcv2Cbor, each scenario's hook sequence, what each
 * hook's {@code Context} exposes and the {@code ExecutionAttributes} keys present at each hook equal the
 * Baseline_Build's, recorded here, as do the caller's result and the number of attempts.
 *
 * <p>The test runs on whichever pipeline the generated client methods use. Until task 3.32 they call the legacy
 * pipeline; after it they call {@code withOperation(CloudWatchOperations.…)} and run on the schema-based path. The
 * only differences allowed on the new path, which the test detects by the {@link SdkExecutionAttribute#API_OPERATION}
 * key at {@code beforeExecution}, are the ones {@code ApiCallPipelineInterceptorOrderTest} (sdk-core) pins for the
 * client handlers (porting-deviations.md, task 3.29): the schema attributes and the transport's attributes are
 * additional keys, and the outputs the hooks see already hold the HTTP response.
 */
class CloudWatchInterceptorOrderTest {
    private static final long TIMEOUT_SECONDS = 10;
    private static final Rpcv2CborCodec CBOR = Rpcv2CborCodec.builder().build();

    private static final String INPUT = "GetDashboardRequest(DashboardName=my-dashboard)";
    private static final String OUTPUT = "GetDashboardResponse(DashboardArn=arn:aws:cloudwatch::123456789012:dashboard/my, "
                                         + "DashboardBody={\"widgets\":[]}, DashboardName=my-dashboard)";
    private static final String URI = "https://monitoring.us-west-2.amazonaws.com/service/GraniteServiceVersion20100801"
                                      + "/operation/GetDashboard";
    private static final String MARSHALLED_REQUEST =
        "POST " + URI + " headers=[accept, content-length, content-type, smithy-protocol, x-amzn-query-mode]";
    private static final String SENT_REQUEST =
        "POST " + URI + " headers=[accept, amz-sdk-invocation-id, amz-sdk-request, authorization, content-length, "
        + "content-type, host, smithy-protocol, user-agent, x-amz-content-sha256, x-amz-date, x-amzn-query-mode]";
    private static final String REQUEST_BODY =
        "requestBody=RequestBody(length=29, type=application/cbor), asyncRequestBody=none";
    private static final String RESPONSE_HEADERS =
        " headers=[content-length, content-type, smithy-protocol, x-amzn-requestid]";

    private static final List<String> REQUEST_HOOKS = Arrays.asList(
        "beforeExecution", "modifyRequest", "beforeMarshalling", "afterMarshalling", "modifyAsyncHttpContent",
        "modifyHttpContent", "modifyHttpRequest");
    private static final List<String> SYNC_RESPONSE_HOOKS = Arrays.asList(
        "afterTransmission", "modifyHttpResponse", "modifyHttpResponseContent", "beforeUnmarshalling");
    private static final List<String> ASYNC_RESPONSE_HOOKS = Arrays.asList(
        "afterTransmission", "modifyHttpResponse", "modifyHttpResponseContent", "beforeUnmarshalling",
        "modifyAsyncHttpResponseContent");
    private static final List<String> OUTPUT_HOOKS = Arrays.asList("afterUnmarshalling", "modifyResponse", "afterExecution");
    private static final List<String> FAILURE_HOOKS = Arrays.asList("modifyException", "onExecutionFailure");

    /**
     * The Baseline's attribute keys at {@code beforeExecution} (written by the aws-core context builder), and the keys
     * it adds later, by the first hook that sees them.
     */
    private static final List<String> BASELINE_KEYS_AT_BEFORE_EXECUTION = Arrays.asList(
        "ApiCallMetricCollector", "AuthSchemeOptionsResolver", "AuthSchemeProvider", "AuthSchemeSnapshotPreInterceptors",
        "AuthSchemes", "AwsAuthAccountIdEndpointMode", "AwsEndpointPrefix", "AwsRegion", "AwsSigv4aSigningRegionSet",
        "BusinessMetricsCollection", "ClientContextParams", "ClientEndpointProvider", "ClientType",
        "DisableHostPrefixInjection", "DualstackEndpointsEnabled", "EndpointProvider", "EndpointResolver",
        "FipsEndpointsEnabled", "HasInitialRequestEvent", "IdentityProviders", "InternalResolvedChecksumSpecs",
        "IsFullDuplex", "IsLongPolling", "NewRetries2026Enabled", "OperationName", "ProfileFile", "ProfileFileSupplier",
        "ProfileName", "ProtocolMetadata", "RequestChecksumCalculation", "RequestIdentityProviderResolver",
        "ResponseChecksumValidation", "SdkClient", "SdkInternalExecutionAttempt", "SelectedAuthScheme", "ServiceConfig",
        "ServiceName", "SignerOverridden", "UseGlobalEndpoint");
    private static final List<String> BASELINE_KEYS_FROM_BEFORE_MARSHALLING = Arrays.asList(
        "SigningMethod", "SigningMethodUpdater");
    private static final List<String> BASELINE_KEYS_FROM_MODIFY_HTTP_REQUEST = Collections.singletonList(
        "HttpRequestUriBeforeModify");
    private static final List<String> BASELINE_KEYS_FROM_BEFORE_TRANSMISSION = Arrays.asList(
        "ApiCallAttemptMetricCollector", "AuthSchemeSnapshotPostInterceptors", "ChecksumStore", "LastBackoffDuration",
        "RequestBodyMetrics", "ResolvedEndpoint", "ResponseBytesRead", "SdkInternalRetryToken", "TimeOffset");
    private static final String BASELINE_KEY_AFTER_SEND = "ApiCallAttemptStartNanoTime";
    private static final String BASELINE_KEY_FROM_AFTER_TRANSMISSION = "HeadersReadEndNanoTime";

    /**
     * New path only: the schema attributes, seeded before {@code beforeExecution}; the sync transport's abort listener,
     * written after the context builder; and, once the first request is sent, the transport's per-attempt inputs (async:
     * also the request body slot and the SDK async transport's owned response body).
     */
    private static final List<String> NEW_PATH_SCHEMA_KEYS = Arrays.asList(
        SdkExecutionAttribute.API_OPERATION.toString(), SdkExecutionAttribute.CLIENT_PROTOCOL.toString(),
        SdkExecutionAttribute.ERROR_REGISTRY.toString());
    private static final String NEW_PATH_SYNC_KEY_FROM_BEFORE_MARSHALLING =
        TransportExecutionAttribute.ABORTABLE_LISTENER.toString();
    private static final List<String> NEW_PATH_SYNC_KEYS_AFTER_SEND = Collections.singletonList(
        TransportExecutionAttribute.HTTP_METRIC_COLLECTOR.toString());
    private static final List<String> NEW_PATH_ASYNC_KEYS_AFTER_SEND = Arrays.asList(
        TransportExecutionAttribute.HTTP_METRIC_COLLECTOR.toString(),
        TransportExecutionAttribute.ASYNC_REQUEST_BODY.toString(), "OwnedResponseBody");

    enum Mode {
        SYNC, ASYNC
    }

    /**
     * A call: the status of each attempt's response ({@code null} for an I/O failure) and the caller's result.
     */
    enum Scenario {
        SUCCESS(h -> h.respond(200, success()), Collections.singletonList(200),
                "output=" + OUTPUT + ", sdkHttpResponse=200"),
        SERVICE_ERROR(h -> h.respond(404, error("DashboardNotFoundError", "no such dashboard")),
                      Collections.singletonList(404),
                      "failure=DashboardNotFoundErrorException: no such dashboard (Service: CloudWatch, Status Code: 404, "
                      + "Request ID: request-404) (SDK Attempt Count: 1)"),
        RETRY_THEN_SUCCESS(h -> h.respond(500, error("InternalServiceFault", "busy")).respond(200, success()),
                           Arrays.asList(500, 200), "output=" + OUTPUT + ", sdkHttpResponse=200"),
        CLIENT_ERROR(h -> h.failWithIoException().failWithIoException().failWithIoException(),
                     Arrays.asList(null, null, null),
                     "failure=SdkClientException: Unable to execute HTTP request: connection reset (SDK Attempt Count: 3), "
                     + "cause=IOException");

        private final Consumer<ScriptedHttp> script;
        private final List<Integer> attempts;
        private final String result;

        Scenario(Consumer<ScriptedHttp> script, List<Integer> attempts, String result) {
            this.script = script;
            this.attempts = attempts;
            this.result = result;
        }

        boolean succeeds() {
            return result.startsWith("output=");
        }

        String exception() {
            return result.substring("failure=".length(), result.indexOf(':'));
        }
    }

    static Stream<Arguments> calls() {
        List<Arguments> arguments = new ArrayList<>();
        for (Mode mode : Mode.values()) {
            for (Scenario scenario : Scenario.values()) {
                arguments.add(Arguments.of(mode, scenario));
            }
        }
        return arguments.stream();
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("calls")
    void generatedClient_invokesTheBaselineHooksWithTheBaselineContextsAndAttributes(Mode mode, Scenario scenario) {
        Outcome outcome = call(mode, scenario);

        assertMatchesBaseline(mode, scenario, outcome);
    }

    /**
     * Asserts that a call's record is the Baseline_Build's for the scenario, allowing only the new path's documented
     * differences when the call ran on it.
     */
    static void assertMatchesBaseline(Mode mode, Scenario scenario, Outcome outcome) {
        List<Expected> expected = expected(mode, scenario);
        assertThat(outcome.hooks()).isEqualTo(expected.stream().map(e -> e.hook).collect(Collectors.toList()));
        assertThat(outcome.hooks().stream().filter("onExecutionFailure"::equals).count())
            .isEqualTo(scenario.succeeds() ? 0 : 1);
        assertThat(outcome.contexts()).isEqualTo(expected.stream()
                                                         .map(e -> e.hook + ": " + e.context)
                                                         .collect(Collectors.toList()));
        boolean newPath = outcome.records.get(0).attributeKeys().contains(SdkExecutionAttribute.API_OPERATION.toString());
        assertThat(outcome.attributeKeys()).isEqualTo(expectedAttributeKeys(mode, newPath, expected));
        assertThat(outcome.httpResponsesOnOutputs()).isEqualTo(expectedHttpResponsesOnOutputs(newPath, expected));
        assertThat(outcome.result).isEqualTo(scenario.result);
        assertThat(outcome.attempts).isEqualTo(scenario.attempts.size());
    }

    static Outcome call(Mode mode, Scenario scenario) {
        ScriptedHttp http = script(scenario);
        HookRecorder recorder = new HookRecorder();
        GetDashboardRequest request = GetDashboardRequest.builder().dashboardName("my-dashboard").build();
        String result;
        if (mode == Mode.SYNC) {
            try (CloudWatchClient client = configure(CloudWatchClient.builder(), recorder).httpClient(http.sync()).build()) {
                result = success(client.getDashboard(request));
            } catch (RuntimeException e) {
                result = failure(e);
            }
        } else {
            try (CloudWatchAsyncClient client = configure(CloudWatchAsyncClient.builder(), recorder)
                .httpClient(http.async()).build()) {
                result = success(client.getDashboard(request).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            } catch (ExecutionException e) {
                result = failure(e.getCause());
            } catch (Exception e) {
                throw new AssertionError("The call did not complete", e);
            }
        }
        return new Outcome(recorder.records(), http.attempts(), result);
    }
    /**
     * A stub HTTP client pair answering the scenario's attempts in order.
     */
    static ScriptedHttp script(Scenario scenario) {
        ScriptedHttp http = new ScriptedHttp();
        scenario.script.accept(http);
        return http;
    }

    /**
     * The client builder settings of every call: static credentials, the recorder, and the standard retry strategy
     * (three attempts) without backoff delays.
     */
    static <B extends AwsClientBuilder<B, ?>> B configure(B builder, HookRecorder recorder) {
        return builder.region(Region.US_WEST_2)
                      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                      .overrideConfiguration(o -> o.addExecutionInterceptor(recorder)
                                                   .retryStrategy(AwsRetryStrategy.forRetryMode(RetryMode.STANDARD)
                                                                                  .toBuilder()
                                                                                  .maxAttempts(3)
                                                                                  .backoffStrategy(
                                                                                      BackoffStrategy.retryImmediately())
                                                                                  .throttlingBackoffStrategy(
                                                                                      BackoffStrategy.retryImmediately())
                                                                                  .build()));
    }

    static String success(GetDashboardResponse output) {
        return "output=" + output + ", sdkHttpResponse=" + output.sdkHttpResponse().statusCode();
    }

    static String failure(Throwable failure) {
        StringBuilder result = new StringBuilder("failure=").append(failure.getClass().getSimpleName())
                                                            .append(": ").append(failure.getMessage());
        if (failure.getCause() != null) {
            result.append(", cause=").append(failure.getCause().getClass().getSimpleName());
        }
        return result.toString();
    }

    /**
     * The Baseline_Build's record of a scenario: each hook with what its context exposes, and how far the call had got
     * (which decides the attribute keys present).
     */
    private static List<Expected> expected(Mode mode, Scenario scenario) {
        List<Expected> result = new ArrayList<>();
        String request = "context=InterceptorContext, request=" + INPUT;
        String marshalled = request + ", httpRequest=" + MARSHALLED_REQUEST + ", " + REQUEST_BODY;
        String sent = request + ", httpRequest=" + SENT_REQUEST + ", " + REQUEST_BODY;
        Progress progress = new Progress();
        for (String hook : REQUEST_HOOKS) {
            progress.marshalling |= hook.equals("beforeMarshalling");
            progress.modifying |= hook.equals("modifyAsyncHttpContent");
            boolean hasHttpRequest = !hook.equals("beforeExecution") && !hook.equals("modifyRequest")
                                     && !hook.equals("beforeMarshalling");
            result.add(new Expected(hook, hasHttpRequest ? marshalled : request, progress, false));
        }

        Integer lastStatus = null;
        for (Integer status : scenario.attempts) {
            progress.attempting = true;
            result.add(new Expected("beforeTransmission", sent, progress, false));
            progress.sent = true;
            if (status == null) {
                continue;
            }
            progress.headersRead = true;
            lastStatus = status;
            for (String hook : mode == Mode.SYNC ? SYNC_RESPONSE_HOOKS : ASYNC_RESPONSE_HOOKS) {
                result.add(new Expected(hook, sent + httpResponse(mode, hook, status), progress, false));
            }
        }

        if (scenario.succeeds()) {
            for (String hook : OUTPUT_HOOKS) {
                result.add(new Expected(hook, sent + httpResponse(mode, hook, 200) + ", response=" + OUTPUT, progress, true));
            }
        } else {
            String failure = "context=DefaultFailedExecutionContext, exception=" + scenario.exception()
                             + ", request=" + INPUT
                             + ", httpRequest=" + SENT_REQUEST
                             + ", httpResponse=" + (lastStatus == null ? "none" : lastStatus + RESPONSE_HEADERS)
                             + ", response=none";
            for (String hook : FAILURE_HOOKS) {
                result.add(new Expected(hook, failure, progress, false));
            }
        }
        return result;
    }

    /**
     * The HTTP response part of a context. Sync hooks see the response stream; async hooks see the headers only, and
     * only {@code modifyAsyncHttpResponseContent} sees the response publisher (the hooks after it get the context
     * stored before it).
     */
    private static String httpResponse(Mode mode, String hook, int status) {
        boolean publisher = mode == Mode.ASYNC && hook.equals("modifyAsyncHttpResponseContent");
        return ", httpResponse=" + status + RESPONSE_HEADERS
               + ", responseBody=" + (mode == Mode.SYNC ? "present" : "none")
               + ", responsePublisher=" + (publisher ? "present" : "none");
    }

    private static List<String> expectedAttributeKeys(Mode mode, boolean newPath, List<Expected> expected) {
        return expected.stream()
                       .map(e -> e.hook + ": " + new ArrayList<>(e.attributeKeys(mode, newPath)))
                       .collect(Collectors.toList());
    }

    /**
     * Baseline: no hook sees an output holding the HTTP response (the legacy handler attaches it to the copy only the
     * caller receives). New path: the protocol sets it on the output builder, so every hook that sees the output does.
     */
    private static List<String> expectedHttpResponsesOnOutputs(boolean newPath, List<Expected> expected) {
        return expected.stream()
                       .map(e -> e.hook + ": " + (newPath && e.seesOutput ? Integer.valueOf(200) : null))
                       .collect(Collectors.toList());
    }

    private static byte[] success() {
        return cbor("DashboardArn", "arn:aws:cloudwatch::123456789012:dashboard/my",
                    "DashboardBody", "{\"widgets\":[]}",
                    "DashboardName", "my-dashboard");
    }

    private static byte[] error(String type, String message) {
        return cbor("__type", type, "message", message);
    }

    /**
     * A CBOR map of string members, encoded with the codec rather than by the code under test.
     */
    private static byte[] cbor(String... keysAndValues) {
        Map<String, Document> members = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            members.put(keysAndValues[i], Document.of(keysAndValues[i + 1]));
        }
        ByteBuffer buffer = CBOR.serialize(Document.of(members));
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    /**
     * How far a call had got at a hook.
     */
    private static final class Progress {
        private boolean marshalling;
        private boolean modifying;
        private boolean attempting;
        private boolean sent;
        private boolean headersRead;
    }

    private static final class Expected {
        private final String hook;
        private final String context;
        private final boolean seesOutput;
        private final boolean marshalling;
        private final boolean modifying;
        private final boolean attempting;
        private final boolean sent;
        private final boolean headersRead;

        private Expected(String hook, String context, Progress progress, boolean seesOutput) {
            this.hook = hook;
            this.context = context;
            this.seesOutput = seesOutput;
            this.marshalling = progress.marshalling;
            this.modifying = progress.modifying;
            this.attempting = progress.attempting;
            this.sent = progress.sent;
            this.headersRead = progress.headersRead;
        }

        TreeSet<String> attributeKeys(Mode mode, boolean newPath) {
            TreeSet<String> keys = new TreeSet<>(BASELINE_KEYS_AT_BEFORE_EXECUTION);
            if (marshalling) {
                keys.addAll(BASELINE_KEYS_FROM_BEFORE_MARSHALLING);
            }
            if (modifying) {
                keys.addAll(BASELINE_KEYS_FROM_MODIFY_HTTP_REQUEST);
            }
            if (attempting) {
                keys.addAll(BASELINE_KEYS_FROM_BEFORE_TRANSMISSION);
            }
            if (sent) {
                keys.add(BASELINE_KEY_AFTER_SEND);
            }
            if (headersRead) {
                keys.add(BASELINE_KEY_FROM_AFTER_TRANSMISSION);
            }
            if (!newPath) {
                return keys;
            }
            keys.addAll(NEW_PATH_SCHEMA_KEYS);
            if (mode == Mode.SYNC && marshalling) {
                keys.add(NEW_PATH_SYNC_KEY_FROM_BEFORE_MARSHALLING);
            }
            if (sent) {
                keys.addAll(mode == Mode.SYNC ? NEW_PATH_SYNC_KEYS_AFTER_SEND : NEW_PATH_ASYNC_KEYS_AFTER_SEND);
            }
            return keys;
        }
    }

    static final class Outcome {
        private final List<HookRecorder.Record> records;
        private final int attempts;
        private final String result;

        Outcome(List<HookRecorder.Record> records, int attempts, String result) {
            this.records = records;
            this.attempts = attempts;
            this.result = result;
        }

        List<String> hooks() {
            return records.stream().map(HookRecorder.Record::hook).collect(Collectors.toList());
        }

        List<String> contexts() {
            return records.stream().map(r -> r.hook() + ": " + r.context()).collect(Collectors.toList());
        }

        List<String> attributeKeys() {
            return records.stream().map(r -> r.hook() + ": " + r.attributeKeys()).collect(Collectors.toList());
        }

        List<String> httpResponsesOnOutputs() {
            return records.stream().map(r -> r.hook() + ": " + r.httpResponseOnOutput()).collect(Collectors.toList());
        }
    }

    /**
     * Answers each request with the next scripted rpcv2Cbor response or I/O failure, through a sync or async SDK HTTP
     * client.
     */
    static final class ScriptedHttp {
        private final Queue<byte[]> bodies = new ConcurrentLinkedQueue<>();
        private final Queue<Integer> statuses = new ConcurrentLinkedQueue<>();
        private final AtomicInteger attempts = new AtomicInteger();

        ScriptedHttp respond(int status, byte[] body) {
            statuses.add(status);
            bodies.add(body);
            return this;
        }

        ScriptedHttp failWithIoException() {
            statuses.add(-1);
            bodies.add(new byte[0]);
            return this;
        }

        int attempts() {
            return attempts.get();
        }

        SdkHttpClient sync() {
            return new SdkHttpClient() {
                @Override
                public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
                    return new ExecutableHttpRequest() {
                        @Override
                        public HttpExecuteResponse call() throws IOException {
                            int status = next();
                            byte[] body = bodies.remove();
                            if (status < 0) {
                                throw new IOException("connection reset");
                            }
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
                    int status = next();
                    byte[] body = bodies.remove();
                    CompletableFuture<Void> result = new CompletableFuture<>();
                    if (status < 0) {
                        IOException failure = new IOException("connection reset");
                        request.responseHandler().onError(failure);
                        result.completeExceptionally(failure);
                        return result;
                    }
                    request.responseHandler().onHeaders(headers(status, body));
                    request.responseHandler().onStream(
                        SdkPublisher.fromIterable(Collections.singletonList(ByteBuffer.wrap(body))));
                    result.complete(null);
                    return result;
                }

                @Override
                public void close() {
                }
            };
        }

        private int next() {
            attempts.incrementAndGet();
            Integer status = statuses.poll();
            if (status == null) {
                throw new IllegalStateException("No scripted response for request " + attempts.get());
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
