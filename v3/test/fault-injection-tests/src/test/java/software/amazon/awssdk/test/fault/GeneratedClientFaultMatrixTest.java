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

package software.amazon.awssdk.test.fault;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.test.fault.TransportFault.connectFailure;
import static software.amazon.awssdk.test.fault.TransportFault.hang;
import static software.amazon.awssdk.test.fault.TransportFault.partialResponse;
import static software.amazon.awssdk.test.fault.TransportFault.timeout;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.ListFunctionsRequest;
import software.amazon.awssdk.services.route53.Route53AsyncClient;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.InvalidInputException;
import software.amazon.awssdk.services.route53.model.ListHostedZonesRequest;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.telemetry.spi.TelemetryOutcome;
import software.amazon.awssdk.test.fault.FaultOutcome.Terminal;

/**
 * The release fault matrix through generated clients (task 18.5; Requirements 19.7, 19.8, 19.14): DynamoDB
 * (awsJson1_0), Lambda (restJson1), SNS (awsQuery) and Route 53 (restXml), each built through its public generated
 * sync and async builder with the {@link FaultRig}'s transport, credentials provider, retry strategy, interceptor and
 * telemetry publisher. The generated client keeps its real SigV4 signer, endpoint rules (with an endpoint override)
 * and SDK-owned async retry scheduler, so retry delays are real (asserted from the retry decisions and as lower bounds
 * of the measured gaps between attempts). Endpoint, signer and request body faults, which a generated builder cannot
 * inject, run in {@link PipelineFaultMatrixTest} through the same pipeline stages.
 */
@Timeout(120)
class GeneratedClientFaultMatrixTest {
    private static final URI ENDPOINT = URI.create("https://fault-injection.example.com");
    private static final Duration T = RetrySettings.DEFAULT.backoff();
    private static final Duration TT = RetrySettings.DEFAULT.throttlingBackoff();
    private static final String REQUEST_ID = "generated-request-id";
    /** A backoff long enough that a sync call is still sleeping when the test interrupts it. */
    private static final RetrySettings SLOW = new RetrySettings(3, Duration.ofMinutes(1), Duration.ofMinutes(1));

    /** One generated service: its wire, modeled error and one call per driver. */
    record Service(String name, TransportFault.Respond success, TransportFault.Respond modeledError,
                   Class<? extends AwsServiceException> modeledType, String modeledCode,
                   TransportFault.Respond unmodeledError, TransportFault.Respond throttling,
                   TransportFault.Respond malformedSuccess, Function<FaultRig, Object> sync,
                   BiFunction<FaultRig, List<AutoCloseable>, CompletableFuture<?>> async) {
        @Override
        public String toString() {
            return name;
        }
    }

    /** One case: a trace built for a service, and its expectation. */
    record Case(String name, Set<FaultKind> kinds, Function<Service, FaultTrace> trace, Set<HttpTransportMode> modes,
                Terminal terminal, Class<? extends Throwable> failure, int attempts, int transmissions,
                List<Duration> delays) {
        @Override
        public String toString() {
            return name;
        }
    }

    static List<Service> services() {
        return List.of(dynamoDb(), lambda(), sns(), route53());
    }

    static List<Case> cases() {
        Set<HttpTransportMode> both = EnumSet.allOf(HttpTransportMode.class);
        List<Case> cases = new ArrayList<>();
        cases.add(new Case("success", Set.of(), s -> FaultTrace.of(s.success()), both, Terminal.SUCCEEDED, null, 1, 1,
                           List.of()));
        cases.add(new Case("modeled error", Set.of(FaultKind.MODELED_ERROR),
                           s -> FaultTrace.of(s.modeledError(), s.success()), both, Terminal.FAILED,
                           AwsServiceException.class, 1, 1, List.of()));
        cases.add(new Case("unmodeled error", Set.of(FaultKind.UNMODELED_ERROR),
                           s -> FaultTrace.of(s.unmodeledError(), s.success()), both, Terminal.FAILED,
                           AwsServiceException.class, 1, 1, List.of()));
        cases.add(new Case("throttling with a retry-after hint, retried after the hint",
                           Set.of(FaultKind.THROTTLING, FaultKind.RETRY_DELAY_HINT),
                           s -> FaultTrace.of(s.throttling().withHeader("x-amz-retry-after", "50"), s.success()), both,
                           Terminal.SUCCEEDED, null, 2, 2, List.of(Duration.ofMillis(50))));
        cases.add(new Case("throttling, retried with the throttling backoff", Set.of(FaultKind.THROTTLING),
                           s -> FaultTrace.of(s.throttling(), s.success()), both, Terminal.SUCCEEDED, null, 2, 2,
                           List.of(TT)));
        cases.add(new Case("malformed success, not retried", Set.of(FaultKind.MALFORMED_SUCCESS),
                           s -> FaultTrace.of(s.malformedSuccess(), s.success()), both, Terminal.FAILED,
                           SdkClientException.class, 1, 1, List.of()));
        cases.add(new Case("connection failure, retried", Set.of(FaultKind.CONNECTION_FAILURE),
                           s -> FaultTrace.of(connectFailure(), s.success()), both, Terminal.SUCCEEDED, null, 2, 2,
                           List.of(T)));
        cases.add(new Case("read timeout, attempts exhausted", Set.of(FaultKind.TIMEOUT),
                           s -> FaultTrace.of(timeout(HttpPhase.RESPONSE_CONSUMPTION),
                                              timeout(HttpPhase.RESPONSE_CONSUMPTION),
                                              timeout(HttpPhase.RESPONSE_CONSUMPTION)), both, Terminal.FAILED,
                           HttpTransportException.class, 3, 3, List.of(T, T)));
        cases.add(new Case("partial response body, retried", Set.of(FaultKind.PARTIAL_RESPONSE_BODY),
                           s -> FaultTrace.of(partialResponse(s.success(), 3), s.success()), both,
                           Terminal.SUCCEEDED, null, 2, 2, List.of(T)));
        cases.add(new Case("identity failure", Set.of(FaultKind.IDENTITY_FAILURE),
                           s -> FaultTrace.of(s.success()).withIdentities(IdentityStep.FAIL), both, Terminal.FAILED,
                           SdkClientException.class, 1, 0, List.of()));
        cases.add(new Case("cancelled while waiting for the identity", Set.of(FaultKind.CANCELLATION),
                           s -> FaultTrace.of(s.success()).withIdentities(IdentityStep.HANG)
                                          .withCancel(CancelPoint.IDENTITY), both, Terminal.CANCELLED, null, 1, 0,
                           List.of()));
        cases.add(new Case("cancelled during a retry delay", Set.of(FaultKind.CANCELLATION),
                           s -> FaultTrace.of(unavailable(s), s.success())
                                          .withRetry(SLOW).withCancel(CancelPoint.RETRY_DELAY), both,
                           Terminal.CANCELLED, null, 1, 1, List.of(SLOW.backoff())));
        cases.add(new Case("cancelled during transmission", Set.of(FaultKind.CANCELLATION),
                           s -> FaultTrace.of(hang(), s.success()).withCancel(CancelPoint.TRANSMISSION),
                           EnumSet.of(HttpTransportMode.ASYNC), Terminal.CANCELLED, null, 1, 1, List.of()));
        return cases;
    }

    static Stream<Arguments> matrix() {
        return services().stream().flatMap(service -> cases().stream().flatMap(
            c -> c.modes().stream().map(mode -> Arguments.of(service, c, mode))));
    }

    @ParameterizedTest(name = "[{0} {2}] {1}")
    @MethodSource("matrix")
    void generatedClientFaultOutcomeMatchesTheExpectation(Service service, Case c, HttpTransportMode mode) {
        FaultRig rig = new FaultRig(c.trace().apply(service), mode);
        List<AutoCloseable> clients = new ArrayList<>();
        FaultOutcome outcome;
        try {
            outcome = FaultCalls.run(rig, FaultCalls.Wiring.generatedClient(), () -> service.sync().apply(rig),
                                     () -> service.async().apply(rig, clients));
        } finally {
            // An async client is closed once its call ended: closing it with the call in flight would end the call.
            for (AutoCloseable client : clients) {
                try {
                    client.close();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        String description = service + ": " + outcome.describe();

        assertThat(outcome.terminal()).as(description).isEqualTo(c.terminal());
        if (c.terminal() == Terminal.CANCELLED) {
            if (mode == HttpTransportMode.ASYNC) {
                assertThat(outcome.failure()).as(description).isInstanceOf(CancellationException.class);
            } else {
                assertThat(outcome.failure()).as(description).isInstanceOf(SdkClientException.class)
                                             .hasCauseInstanceOf(InterruptedException.class);
            }
        } else if (c.failure() != null) {
            assertThat(outcome.failure()).as(description).isInstanceOf(c.failure());
        } else {
            assertThat(outcome.failure()).as(description).isNull();
        }
        assertThat(outcome.attempts()).as("attempts: " + description).isEqualTo(c.attempts());
        assertThat(outcome.transmissions()).as("transmissions: " + description).isEqualTo(c.transmissions());
        assertThat(outcome.retryDelays()).as("retry delays: " + description).isEqualTo(c.delays());
        List<Duration> gaps = outcome.attemptGaps();
        for (int i = 0; i < gaps.size() && i < c.delays().size(); i++) {
            assertThat(gaps.get(i)).as("gap " + i + ": " + description).isGreaterThanOrEqualTo(c.delays().get(i));
        }
        assertThat(outcome.leaks()).as("leaks: " + description).isEmpty();
        assertThat(outcome.violations()).as("violations: " + description).isEmpty();

        int afterExecution = c.terminal() == Terminal.CANCELLED && mode == HttpTransportMode.ASYNC ? 0 : 1;
        assertThat(outcome.afterExecutions()).as("afterExecution: " + description).isEqualTo(afterExecution);
        assertThat(outcome.telemetry()).as("telemetry: " + description).hasSize(1);
        assertThat(outcome.pipelineOutcome()).as(description).contains(switch (c.terminal()) {
            case SUCCEEDED -> TelemetryOutcome.SUCCEEDED;
            case CANCELLED -> TelemetryOutcome.CANCELLED;
            default -> TelemetryOutcome.FAILED;
        });
        assertThat(outcome.telemetry().get(0).attemptCount()).as(description).isEqualTo(c.attempts());

        if (outcome.result() instanceof SdkResponse response) {
            assertThat(response.sdkResponseMetadata().attemptCount()).as(description).hasValue(c.attempts());
            assertThat(response.sdkResponseMetadata().requestId()).as(description).contains(REQUEST_ID);
        }
        if (outcome.terminal() == Terminal.FAILED && outcome.failure() instanceof SdkException failure) {
            assertThat(failure.numAttempts()).as("exception attempts: " + description).isEqualTo(c.attempts());
        }
        if (c.kinds().contains(FaultKind.MODELED_ERROR)) {
            AwsServiceException error = (AwsServiceException) outcome.failure();
            assertThat(error).as(description).isInstanceOf(service.modeledType());
            assertThat(error.awsErrorDetails().errorCode()).as(description).isEqualTo(service.modeledCode());
            assertThat(error.requestId()).as(description).isEqualTo(REQUEST_ID);
        }
        if (c.kinds().contains(FaultKind.UNMODELED_ERROR)) {
            AwsServiceException error = (AwsServiceException) outcome.failure();
            assertThat(error).as(description).isNotInstanceOf(service.modeledType());
            assertThat(error.awsErrorDetails().errorCode()).as(description).isEqualTo("FaultInjectedMysteryError");
            assertThat(error.statusCode()).as(description).isEqualTo(400);
        }
        if (c.kinds().contains(FaultKind.MALFORMED_SUCCESS)) {
            assertThat(outcome.failure()).as(description).isNotInstanceOf(AwsServiceException.class);
        }
        if (c.kinds().contains(FaultKind.TIMEOUT)) {
            assertThat(((HttpTransportException) outcome.failure()).category()).as(description)
                                                                               .isEqualTo(HttpFailureCategory.TIMEOUT);
        }
        if (c.kinds().contains(FaultKind.IDENTITY_FAILURE)) {
            assertThat(outcome.failure()).as(description).hasCauseInstanceOf(FaultInjectedException.class);
        }
        // Every transmission was signed with the rig's identity by the real SigV4 signer.
        for (ExchangeRecord exchange : outcome.exchanges()) {
            assertThat(exchange.request().headers().firstValue("Authorization")).as(description)
                .hasValueSatisfying(v -> assertThat(v).contains("Credential=AKIDFAULTINJECTION/"));
        }
    }

    /** Every fault kind a generated builder can inject runs for every protocol on both drivers. */
    @Test
    void matrixCoversTheInjectableFaultKindsOnBothDrivers() {
        Set<FaultKind> injectable = EnumSet.range(FaultKind.CONNECTION_FAILURE, FaultKind.CANCELLATION);
        // Request body faults need a streaming-input operation and endpoint faults replaced rules: both run through
        // the same stages in PipelineFaultMatrixTest.
        injectable.removeAll(EnumSet.of(FaultKind.PARTIAL_REQUEST_BODY, FaultKind.ENDPOINT_FAILURE));
        for (HttpTransportMode mode : HttpTransportMode.values()) {
            Set<FaultKind> covered = EnumSet.noneOf(FaultKind.class);
            cases().stream().filter(c -> c.modes().contains(mode)).forEach(c -> covered.addAll(c.kinds()));
            assertThat(covered).as(mode.toString()).containsAll(injectable);
        }
        assertThat(services()).hasSize(4);
    }

    private static TransportFault.Respond unavailable(Service service) {
        TransportFault.Respond ok = service.success();
        return new TransportFault.Respond(503, ok.headers(), "<html>Service Unavailable</html>".getBytes(StandardCharsets.UTF_8));
    }

    // ---- Services ----

    private static Service dynamoDb() {
        String json = "application/x-amz-json-1.0";
        return new Service(
            "DynamoDB awsJson1_0",
            respond(200, json, "{\"TableNames\":[\"fault-table\"]}"),
            respond(400, json, "{\"__type\":\"com.amazonaws.dynamodb#ResourceNotFoundException\","
                               + "\"message\":\"Requested resource not found\"}"),
            software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException.class,
            "ResourceNotFoundException",
            respond(400, json, "{\"__type\":\"com.amazonaws.dynamodb#FaultInjectedMysteryError\","
                               + "\"message\":\"mystery\"}"),
            respond(400, json, "{\"__type\":\"com.amazonaws.dynamodb#ThrottlingException\",\"message\":\"slow down\"}"),
            respond(200, json, "{\"TableNames\":[\"fault-table\", "),
            rig -> {
                try (DynamoDbClient client = DynamoDbClient.builder()
                                                           .region(Region.US_EAST_1)
                                                           .endpointOverride(ENDPOINT)
                                                           .credentialsProvider(rig.identities())
                                                           .retryStrategy(rig.retryStrategy())
                                                           .httpClient(rig.http().sync())
                                                           .addExecutionInterceptor(rig.lifecycle())
                                                           .telemetry(rig.telemetry().configuration())
                                                           .build()) {
                    return client.listTables(ListTablesRequest.builder().build());
                }
            },
            (rig, clients) -> {
                DynamoDbAsyncClient client = DynamoDbAsyncClient.builder()
                                                                .region(Region.US_EAST_1)
                                                                .endpointOverride(ENDPOINT)
                                                                .credentialsProvider(rig.identities())
                                                                .retryStrategy(rig.retryStrategy())
                                                                .httpClient(rig.http().async())
                                                                .asyncCompletionExecutor(Runnable::run)
                                                                .addExecutionInterceptor(rig.lifecycle())
                                                                .telemetry(rig.telemetry().configuration())
                                                                .build();
                clients.add(client);
                return client.listTables(ListTablesRequest.builder().build());
            });
    }

    private static Service lambda() {
        String json = "application/json";
        return new Service(
            "Lambda restJson1",
            respond(200, json, "{\"Functions\":[]}"),
            respond(404, json, "{\"Type\":\"User\",\"Message\":\"Function not found\"}",
                    "X-Amzn-ErrorType", "ResourceNotFoundException"),
            software.amazon.awssdk.services.lambda.model.ResourceNotFoundException.class,
            "ResourceNotFoundException",
            respond(400, json, "{\"message\":\"mystery\"}", "X-Amzn-ErrorType", "FaultInjectedMysteryError"),
            respond(429, json, "{\"Type\":\"User\",\"message\":\"Rate exceeded\",\"Reason\":\"CallerRateLimitExceeded\"}",
                    "X-Amzn-ErrorType", "TooManyRequestsException"),
            respond(200, json, "{\"Functions\":["),
            rig -> {
                try (LambdaClient client = LambdaClient.builder()
                                                       .region(Region.US_EAST_1)
                                                       .endpointOverride(ENDPOINT)
                                                       .credentialsProvider(rig.identities())
                                                       .retryStrategy(rig.retryStrategy())
                                                       .httpClient(rig.http().sync())
                                                       .addExecutionInterceptor(rig.lifecycle())
                                                       .telemetry(rig.telemetry().configuration())
                                                       .build()) {
                    return client.listFunctions(ListFunctionsRequest.builder().build());
                }
            },
            (rig, clients) -> {
                LambdaAsyncClient client = LambdaAsyncClient.builder()
                                                            .region(Region.US_EAST_1)
                                                            .endpointOverride(ENDPOINT)
                                                            .credentialsProvider(rig.identities())
                                                            .retryStrategy(rig.retryStrategy())
                                                            .httpClient(rig.http().async())
                                                            .asyncCompletionExecutor(Runnable::run)
                                                            .addExecutionInterceptor(rig.lifecycle())
                                                            .telemetry(rig.telemetry().configuration())
                                                            .build();
                clients.add(client);
                return client.listFunctions(ListFunctionsRequest.builder().build());
            });
    }

    private static Service sns() {
        String xml = "text/xml";
        String ns = "http://sns.amazonaws.com/doc/2010-03-31/";
        return new Service(
            "SNS awsQuery",
            respond(200, xml, "<ListTopicsResponse xmlns=\"" + ns + "\"><ListTopicsResult><Topics/></ListTopicsResult>"
                              + "<ResponseMetadata><RequestId>" + REQUEST_ID + "</RequestId></ResponseMetadata>"
                              + "</ListTopicsResponse>"),
            respond(404, xml, queryError(ns, "NotFound")),
            NotFoundException.class,
            "NotFound",
            respond(400, xml, queryError(ns, "FaultInjectedMysteryError")),
            respond(429, xml, queryError(ns, "Throttled")),
            respond(200, xml, "<ListTopicsResponse xmlns=\"" + ns + "\"><ListTopicsResult><Topics>"),
            rig -> {
                try (SnsClient client = SnsClient.builder()
                                                 .region(Region.US_EAST_1)
                                                 .endpointOverride(ENDPOINT)
                                                 .credentialsProvider(rig.identities())
                                                 .retryStrategy(rig.retryStrategy())
                                                 .httpClient(rig.http().sync())
                                                 .addExecutionInterceptor(rig.lifecycle())
                                                 .telemetry(rig.telemetry().configuration())
                                                 .build()) {
                    return client.listTopics(ListTopicsRequest.builder().build());
                }
            },
            (rig, clients) -> {
                SnsAsyncClient client = SnsAsyncClient.builder()
                                                      .region(Region.US_EAST_1)
                                                      .endpointOverride(ENDPOINT)
                                                      .credentialsProvider(rig.identities())
                                                      .retryStrategy(rig.retryStrategy())
                                                      .httpClient(rig.http().async())
                                                      .asyncCompletionExecutor(Runnable::run)
                                                      .addExecutionInterceptor(rig.lifecycle())
                                                      .telemetry(rig.telemetry().configuration())
                                                      .build();
                clients.add(client);
                return client.listTopics(ListTopicsRequest.builder().build());
            });
    }

    private static Service route53() {
        String xml = "text/xml";
        String ns = "https://route53.amazonaws.com/doc/2013-04-01/";
        return new Service(
            "Route 53 restXml",
            respond(200, xml, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ListHostedZonesResponse xmlns=\"" + ns
                              + "\"><HostedZones/><Marker></Marker><IsTruncated>false</IsTruncated>"
                              + "<MaxItems>100</MaxItems></ListHostedZonesResponse>"),
            respond(400, xml, queryError(ns, "InvalidInput")),
            InvalidInputException.class,
            "InvalidInput",
            respond(400, xml, queryError(ns, "FaultInjectedMysteryError")),
            respond(400, xml, queryError(ns, "ThrottlingException")),
            respond(200, xml, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ListHostedZonesResponse xmlns=\"" + ns
                              + "\"><HostedZones><HostedZone>"),
            rig -> {
                try (Route53Client client = Route53Client.builder()
                                                         .region(Region.US_EAST_1)
                                                         .endpointOverride(ENDPOINT)
                                                         .credentialsProvider(rig.identities())
                                                         .retryStrategy(rig.retryStrategy())
                                                         .httpClient(rig.http().sync())
                                                         .addExecutionInterceptor(rig.lifecycle())
                                                         .telemetry(rig.telemetry().configuration())
                                                         .build()) {
                    return client.listHostedZones(ListHostedZonesRequest.builder().build());
                }
            },
            (rig, clients) -> {
                Route53AsyncClient client = Route53AsyncClient.builder()
                                                              .region(Region.US_EAST_1)
                                                              .endpointOverride(ENDPOINT)
                                                              .credentialsProvider(rig.identities())
                                                              .retryStrategy(rig.retryStrategy())
                                                              .httpClient(rig.http().async())
                                                              .asyncCompletionExecutor(Runnable::run)
                                                              .addExecutionInterceptor(rig.lifecycle())
                                                              .telemetry(rig.telemetry().configuration())
                                                              .build();
                clients.add(client);
                return client.listHostedZones(ListHostedZonesRequest.builder().build());
            });
    }

    private static String queryError(String namespace, String code) {
        return "<ErrorResponse xmlns=\"" + namespace + "\"><Error><Type>Sender</Type><Code>" + code + "</Code>"
               + "<Message>injected " + code + "</Message></Error><RequestId>" + REQUEST_ID + "</RequestId>"
               + "</ErrorResponse>";
    }

    private static TransportFault.Respond respond(int status, String contentType, String body, String... headers) {
        TransportFault.Respond response = TransportFault.respond(status, body, "Content-Type", contentType,
                                                                 "x-amzn-RequestId", REQUEST_ID);
        for (int i = 0; i < headers.length; i += 2) {
            response = response.withHeader(headers[i], headers[i + 1]);
        }
        return response;
    }
}
