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
import static org.assertj.core.api.Assertions.entry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.codecs.cbor.Rpcv2CborCodec;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.ClientProtocolFactories;
import software.amazon.awssdk.core.client.protocol.ClientProtocolFactory;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.protocols.client.awsjson.AwsJson1Protocol;
import software.amazon.awssdk.protocols.client.awsquery.AwsQueryClientProtocol;
import software.amazon.awssdk.protocols.client.rpcv2.RpcV2CborProtocol;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.services.cloudwatch.internal.CloudWatchServiceSchemas;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchResponse;
import software.amazon.awssdk.services.cloudwatch.model.DashboardNotFoundErrorException;
import software.amazon.awssdk.services.cloudwatch.model.GetDashboardResponse;
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Metric;

/**
 * CloudWatch {@code protocol(...)} overrides to {@code awsJson1_0} and {@code awsQuery}, with no regeneration, compared
 * with the default {@code rpcv2Cbor} (task 9.5, Requirements 12.2, 12.6, 12.7, 33.2). Each case runs through the sync
 * and the async client over a stub SDK HTTP client and checks the request on the wire, the parsed output or modeled
 * error, and the protocol the client and the call use.
 */
class CloudWatchProtocolOverrideTest {
    private static final long TIMEOUT_SECONDS = 10;
    private static final String SERVICE_NAME = "GraniteServiceVersion20100801";
    private static final ShapeId SERVICE_ID = ShapeId.from("com.amazonaws.cloudwatch#" + SERVICE_NAME);
    private static final ShapeId AWS_QUERY_ID = ShapeId.from("aws.protocols#awsQuery");
    private static final ShapeId AWS_JSON_1_0_ID = ShapeId.from("aws.protocols#awsJson1_0");
    private static final String XML_NAMESPACE = "http://monitoring.amazonaws.com/doc/2010-08-01/";
    private static final Rpcv2CborCodec CBOR = Rpcv2CborCodec.builder().build();

    /**
     * The settings the generated builder creates its default protocol with ({@code protocolSettings()}): the service
     * schema carries {@code awsQueryCompatible}, and the version is the {@code Version} of awsQuery requests.
     */
    private static final ProtocolSettings SETTINGS = ProtocolSettings.builder()
                                                                     .serviceSchema(CloudWatchServiceSchemas.SERVICE)
                                                                     .serviceVersion("2010-08-01")
                                                                     .baseExceptionBuilder(CloudWatchException::builder)
                                                                     .build();

    enum Mode {
        SYNC, ASYNC
    }

    enum Protocol {
        /** The default: no override is configured. */
        RPCV2_CBOR {
            @Override
            ClientProtocol<?, ?> override() {
                return null;
            }

            @Override
            Class<?> protocolClass() {
                return RpcV2CborProtocol.class;
            }
        },
        /** Created with the factory class. */
        AWS_JSON_1_0 {
            @Override
            ClientProtocol<?, ?> override() {
                return AwsJson1Protocol.Factory.INSTANCE.createProtocol(SETTINGS, null);
            }

            @Override
            Class<?> protocolClass() {
                return AwsJson1Protocol.class;
            }
        },
        /** Created with the factory found by protocol ID, without a reference to the protocol module. */
        AWS_QUERY {
            @Override
            ClientProtocol<?, ?> override() {
                return ClientProtocolFactories.forId(AWS_QUERY_ID).createProtocol(SETTINGS, null);
            }

            @Override
            Class<?> protocolClass() {
                return AwsQueryClientProtocol.class;
            }
        };

        abstract ClientProtocol<?, ?> override();

        abstract Class<?> protocolClass();
    }

    static Stream<Arguments> cases() {
        List<Arguments> cases = new ArrayList<>();
        for (Protocol protocol : Protocol.values()) {
            for (Mode mode : Mode.values()) {
                cases.add(Arguments.of(protocol, mode));
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("cases")
    void getDashboard_withEachProtocol_sendsItsWireShapeAndParsesTheOutput(Protocol protocol, Mode mode) {
        ScriptedHttp http = new ScriptedHttp().respond(protocol, 200, getDashboardBody(protocol));
        Recorder recorder = new Recorder();
        ClientProtocol<?, ?> override = protocol.override();

        GetDashboardResponse response = call(mode, http, recorder, override,
                                             c -> c.getDashboard(r -> r.dashboardName("my dashboard")),
                                             c -> c.getDashboard(r -> r.dashboardName("my dashboard")));

        assertThat(response).usingComparator(CloudWatchProtocolOverrideTest::compareMembers)
                            .isEqualTo(GetDashboardResponse.builder()
                                                                       .dashboardName("my dashboard")
                                                                       .dashboardBody("{}")
                                                                       .build());
        assertThat(response.sdkHttpResponse().statusCode()).isEqualTo(200);
        assertThat(response.responseMetadata().requestId()).isEqualTo("request-200");
        recorder.assertCallProtocol(protocol, override);
        SentRequest sent = http.onlyRequest();
        assertThat(sent.request.method().name()).isEqualTo("POST");
        switch (protocol) {
            case RPCV2_CBOR:
                assertThat(sent.request.encodedPath())
                    .isEqualTo("/service/" + SERVICE_NAME + "/operation/GetDashboard");
                assertThat(sent.header("smithy-protocol")).isEqualTo("rpc-v2-cbor");
                assertThat(sent.header("Content-Type")).isEqualTo("application/cbor");
                assertThat(sent.header("x-amzn-query-mode")).isEqualTo("true");
                assertThat(sent.header("X-Amz-Target")).isNull();
                assertThat(CBOR.createDeserializer(sent.body).readDocument().asMap())
                    .containsExactly(entry("DashboardName", Document.of("my dashboard")));
                break;
            case AWS_JSON_1_0:
                assertThat(sent.request.encodedPath()).isEqualTo("/");
                assertThat(sent.header("X-Amz-Target")).isEqualTo(SERVICE_NAME + ".GetDashboard");
                assertThat(sent.header("Content-Type")).isEqualTo("application/x-amz-json-1.0");
                assertThat(sent.header("x-amzn-query-mode")).isEqualTo("true");
                assertThat(sent.header("smithy-protocol")).isNull();
                assertThat(sent.bodyText()).isEqualTo("{\"DashboardName\":\"my dashboard\"}");
                break;
            case AWS_QUERY:
                assertThat(sent.request.encodedPath()).isEqualTo("/");
                assertThat(sent.header("Content-Type")).isEqualTo("application/x-www-form-urlencoded");
                assertThat(sent.header("X-Amz-Target")).isNull();
                assertThat(sent.header("x-amzn-query-mode")).isNull();
                // Phase 5 policy row 1: a space is sent as %20.
                assertThat(sent.bodyText())
                    .isEqualTo("Action=GetDashboard&Version=2010-08-01&DashboardName=my%20dashboard");
                break;
            default:
                throw new AssertionError(protocol);
        }
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("cases")
    void listMetrics_withEachProtocol_parsesTheListAndTheToken(Protocol protocol, Mode mode) {
        ScriptedHttp http = new ScriptedHttp().respond(protocol, 200, listMetricsBody(protocol));
        Recorder recorder = new Recorder();
        ClientProtocol<?, ?> override = protocol.override();

        ListMetricsResponse response = call(mode, http, recorder, override,
                                            c -> c.listMetrics(r -> r.namespace("AWS/EC2")),
                                            c -> c.listMetrics(r -> r.namespace("AWS/EC2")));

        assertThat(response).usingComparator(CloudWatchProtocolOverrideTest::compareMembers)
                            .isEqualTo(ListMetricsResponse.builder()
                                                          .metrics(metric("CPUUtilization"), metric("NetworkIn"))
                                                          .nextToken("page-2")
                                                          .build());
        recorder.assertCallProtocol(protocol, override);
        SentRequest sent = http.onlyRequest();
        if (protocol == Protocol.AWS_QUERY) {
            assertThat(sent.bodyText()).isEqualTo("Action=ListMetrics&Version=2010-08-01&Namespace=AWS%2FEC2");
        } else if (protocol == Protocol.AWS_JSON_1_0) {
            assertThat(sent.header("X-Amz-Target")).isEqualTo(SERVICE_NAME + ".ListMetrics");
            assertThat(sent.bodyText()).isEqualTo("{\"Namespace\":\"AWS/EC2\"}");
        }
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("cases")
    void getDashboard_whenTheDashboardIsMissing_throwsTheSameModeledErrorAndCodeForEachProtocol(Protocol protocol,
                                                                                               Mode mode) {
        ScriptedHttp http = new ScriptedHttp().respond(protocol, 404, dashboardNotFoundBody(protocol));
        Recorder recorder = new Recorder();
        ClientProtocol<?, ?> override = protocol.override();

        Throwable failure = failure(mode, http, recorder, override,
                                    c -> c.getDashboard(r -> r.dashboardName("missing")),
                                    c -> c.getDashboard(r -> r.dashboardName("missing")));

        // rpcv2Cbor and awsJson1_0 report the x-amzn-query-error code (awsQueryCompatible); their __type alone would
        // report DashboardNotFoundError. awsQuery reads the <Code> of the body.
        assertThat(failure).isInstanceOf(DashboardNotFoundErrorException.class)
                           .hasMessageContaining("no such dashboard");
        AwsServiceException error = (AwsServiceException) failure;
        assertThat(error.statusCode()).isEqualTo(404);
        assertThat(error.awsErrorDetails().errorCode()).isEqualTo("ResourceNotFound");
        assertThat(error.requestId()).isEqualTo("request-404");
        recorder.assertCallProtocol(protocol, override);
    }

    @Test
    void protocolIds_resolveToTheFactoriesOfTheRuntimeDependencies() {
        ClientProtocolFactory<?> awsJson = ClientProtocolFactories.forId(AWS_JSON_1_0_ID);
        ClientProtocolFactory<?> awsQuery = ClientProtocolFactories.forId(AWS_QUERY_ID);

        assertThat(awsJson).isInstanceOf(AwsJson1Protocol.Factory.class);
        assertThat(awsQuery).isInstanceOf(AwsQueryClientProtocol.Factory.class);
        assertThat(awsJson.createProtocol(SETTINGS, null)).hasToString("AwsJson1Protocol(" + SERVICE_ID + ")");
        assertThat(awsQuery.createProtocol(SETTINGS, null).id()).isEqualTo(AWS_QUERY_ID);
    }

    /**
     * Without the internal service schema, settings built from public API carry no {@code awsQueryCompatible} trait:
     * the awsJson1_0 override then sends no {@code x-amzn-query-mode} header and ignores {@code x-amzn-query-error}.
     * Calls still work. This records the gap reported in porting-deviations.md (task 9.5).
     */
    @Test
    void awsJsonOverride_withSettingsFromPublicApiOnly_worksWithoutTheQueryCompatibleHeaders() {
        ProtocolSettings publicSettings = ProtocolSettings.builder()
                                                          .service(SERVICE_ID)
                                                          .serviceVersion("2010-08-01")
                                                          .baseExceptionBuilder(CloudWatchException::builder)
                                                          .build();
        ClientProtocol<?, ?> override = AwsJson1Protocol.Factory.INSTANCE.createProtocol(publicSettings, null);
        ScriptedHttp http = new ScriptedHttp().respond(Protocol.AWS_JSON_1_0, 200,
                                                       getDashboardBody(Protocol.AWS_JSON_1_0))
                                              .respond(Protocol.AWS_JSON_1_0, 404,
                                                       dashboardNotFoundBody(Protocol.AWS_JSON_1_0));

        try (CloudWatchClient client = configure(CloudWatchClient.builder(), new Recorder()).httpClient(http.sync())
                                                                                           .protocol(override)
                                                                                           .build()) {
            assertThat(client.getDashboard(r -> r.dashboardName("my dashboard")).dashboardName())
                .isEqualTo("my dashboard");
            Throwable failure = catchThrowable(() -> client.getDashboard(r -> r.dashboardName("missing")));

            assertThat(failure).isInstanceOf(DashboardNotFoundErrorException.class);
            assertThat(((AwsServiceException) failure).awsErrorDetails().errorCode()).isEqualTo("DashboardNotFoundError");
        }
        assertThat(http.requests).allSatisfy(r -> {
            assertThat(r.header("X-Amz-Target")).startsWith(SERVICE_NAME + ".");
            assertThat(r.header("x-amzn-query-mode")).isNull();
        });
    }

    /**
     * Equal (0) when the members are equal; {@code equals()} also compares the HTTP response and response metadata.
     */
    private static int compareMembers(CloudWatchResponse actual, CloudWatchResponse expected) {
        return actual.equalsBySdkFields(expected) ? 0 : 1;
    }

    private static Metric metric(String name) {
        return Metric.builder().namespace("AWS/EC2").metricName(name).build();
    }

    private static byte[] getDashboardBody(Protocol protocol) {
        switch (protocol) {
            case RPCV2_CBOR:
                return cbor(members("DashboardName", Document.of("my dashboard"), "DashboardBody", Document.of("{}")));
            case AWS_JSON_1_0:
                return utf8("{\"DashboardName\":\"my dashboard\",\"DashboardBody\":\"{}\"}");
            case AWS_QUERY:
                return utf8("<GetDashboardResponse xmlns=\"" + XML_NAMESPACE + "\"><GetDashboardResult>"
                            + "<DashboardName>my dashboard</DashboardName><DashboardBody>{}</DashboardBody>"
                            + "</GetDashboardResult><ResponseMetadata><RequestId>request-200</RequestId>"
                            + "</ResponseMetadata></GetDashboardResponse>");
            default:
                throw new AssertionError(protocol);
        }
    }

    private static byte[] listMetricsBody(Protocol protocol) {
        switch (protocol) {
            case RPCV2_CBOR:
                return cbor(members("Metrics", Document.of(Arrays.asList(
                                        Document.of(members("Namespace", Document.of("AWS/EC2"),
                                                            "MetricName", Document.of("CPUUtilization"))),
                                        Document.of(members("Namespace", Document.of("AWS/EC2"),
                                                            "MetricName", Document.of("NetworkIn"))))),
                                    "NextToken", Document.of("page-2")));
            case AWS_JSON_1_0:
                return utf8("{\"Metrics\":[{\"Namespace\":\"AWS/EC2\",\"MetricName\":\"CPUUtilization\"},"
                            + "{\"Namespace\":\"AWS/EC2\",\"MetricName\":\"NetworkIn\"}],\"NextToken\":\"page-2\"}");
            case AWS_QUERY:
                return utf8("<ListMetricsResponse xmlns=\"" + XML_NAMESPACE + "\"><ListMetricsResult><Metrics>"
                            + "<member><Namespace>AWS/EC2</Namespace><MetricName>CPUUtilization</MetricName></member>"
                            + "<member><Namespace>AWS/EC2</Namespace><MetricName>NetworkIn</MetricName></member>"
                            + "</Metrics><NextToken>page-2</NextToken></ListMetricsResult>"
                            + "<ResponseMetadata><RequestId>request-200</RequestId></ResponseMetadata>"
                            + "</ListMetricsResponse>");
            default:
                throw new AssertionError(protocol);
        }
    }

    private static byte[] dashboardNotFoundBody(Protocol protocol) {
        switch (protocol) {
            case RPCV2_CBOR:
                return cbor(members("__type", Document.of("com.amazonaws.cloudwatch#DashboardNotFoundError"),
                                    "message", Document.of("no such dashboard")));
            case AWS_JSON_1_0:
                return utf8("{\"__type\":\"com.amazonaws.cloudwatch#DashboardNotFoundError\","
                            + "\"message\":\"no such dashboard\"}");
            case AWS_QUERY:
                return utf8("<ErrorResponse xmlns=\"" + XML_NAMESPACE + "\"><Error><Type>Sender</Type>"
                            + "<Code>ResourceNotFound</Code><Message>no such dashboard</Message></Error>"
                            + "<RequestId>request-404</RequestId></ErrorResponse>");
            default:
                throw new AssertionError(protocol);
        }
    }

    private static <T> T call(Mode mode, ScriptedHttp http, Recorder recorder, ClientProtocol<?, ?> override,
                              Function<CloudWatchClient, T> sync,
                              Function<CloudWatchAsyncClient, CompletableFuture<?>> async) {
        try {
            return callOrThrow(mode, http, recorder, override, sync, async);
        } catch (Throwable t) {
            throw new AssertionError("The call failed", t);
        }
    }

    private static Throwable failure(Mode mode, ScriptedHttp http, Recorder recorder, ClientProtocol<?, ?> override,
                                     Function<CloudWatchClient, ?> sync,
                                     Function<CloudWatchAsyncClient, CompletableFuture<?>> async) {
        Throwable failure = catchThrowable(() -> callOrThrow(mode, http, recorder, override, sync, async));
        assertThat(failure).as("the call's failure").isNotNull();
        return failure;
    }

    /**
     * Makes a call through a client built with the override (null: the default), after checking that
     * {@code CLIENT_PROTOCOL} of the built client is the override, or the default rpcv2Cbor protocol.
     */
    @SuppressWarnings("unchecked")
    private static <T> T callOrThrow(Mode mode, ScriptedHttp http, Recorder recorder, ClientProtocol<?, ?> override,
                                     Function<CloudWatchClient, T> sync,
                                     Function<CloudWatchAsyncClient, CompletableFuture<?>> async) throws Throwable {
        if (mode == Mode.SYNC) {
            try (CloudWatchClient client = configure(CloudWatchClient.builder(), recorder).httpClient(http.sync())
                                                                                         .protocol(override)
                                                                                         .build()) {
                recorder.clientProtocol = clientProtocol(client);
                return sync.apply(client);
            }
        }
        try (CloudWatchAsyncClient client = configure(CloudWatchAsyncClient.builder(), recorder)
            .httpClient(http.async())
            .protocol(override)
            .build()) {
            recorder.clientProtocol = clientProtocol(client);
            return (T) async.apply(client).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    private static <B extends AwsClientBuilder<B, ?>> B configure(B builder, Recorder recorder) {
        return builder.region(Region.US_WEST_2)
                      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                      .overrideConfiguration(o -> o.addExecutionInterceptor(recorder));
    }

    private static ClientProtocol<?, ?> clientProtocol(SdkClient client) {
        try {
            Field field = client.getClass().getDeclaredField("clientConfiguration");
            field.setAccessible(true);
            return ((SdkClientConfiguration) field.get(client)).option(SdkClientOption.CLIENT_PROTOCOL);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static Map<String, Document> members(Object... keysAndValues) {
        Map<String, Document> members = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            members.put((String) keysAndValues[i], (Document) keysAndValues[i + 1]);
        }
        return members;
    }

    private static byte[] cbor(Map<String, Document> members) {
        return bytes(CBOR.serialize(Document.of(members)));
    }

    private static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    /**
     * Records the protocol of the built client and the {@code CLIENT_PROTOCOL} execution attribute of the call.
     */
    private static final class Recorder implements ExecutionInterceptor {
        private final List<ClientProtocol<?, ?>> callProtocols = new CopyOnWriteArrayList<>();
        private volatile ClientProtocol<?, ?> clientProtocol;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes attributes) {
            callProtocols.add(attributes.getAttribute(SdkExecutionAttribute.CLIENT_PROTOCOL));
        }

        void assertCallProtocol(Protocol protocol, ClientProtocol<?, ?> override) {
            assertThat(clientProtocol).as("CLIENT_PROTOCOL of the built client").isInstanceOf(protocol.protocolClass());
            if (override != null) {
                assertThat(clientProtocol).as("CLIENT_PROTOCOL of the built client").isSameAs(override);
            }
            assertThat(callProtocols).as("CLIENT_PROTOCOL of the call").isNotEmpty()
                                     .allSatisfy(p -> assertThat(p).isSameAs(clientProtocol));
        }
    }

    private static final class SentRequest {
        private final SdkHttpRequest request;
        private final byte[] body;

        private SentRequest(SdkHttpRequest request, byte[] body) {
            this.request = request;
            this.body = body;
        }

        String header(String name) {
            return request.firstMatchingHeader(name).orElse(null);
        }

        String bodyText() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    /**
     * Answers each request with the next scripted response, through a sync or async SDK HTTP client, and records each
     * request with its body.
     */
    private static final class ScriptedHttp {
        private final Queue<SdkHttpFullResponse> responses = new ConcurrentLinkedQueue<>();
        private final Queue<byte[]> bodies = new ConcurrentLinkedQueue<>();
        private final List<SentRequest> requests = new CopyOnWriteArrayList<>();

        ScriptedHttp respond(Protocol protocol, int status, byte[] body) {
            SdkHttpFullResponse.Builder response = SdkHttpFullResponse.builder()
                                                                      .statusCode(status)
                                                                      .putHeader("Content-Length",
                                                                                 String.valueOf(body.length))
                                                                      .putHeader("x-amzn-RequestId",
                                                                                 "request-" + status);
            switch (protocol) {
                case RPCV2_CBOR:
                    response.putHeader("Content-Type", "application/cbor").putHeader("smithy-protocol", "rpc-v2-cbor");
                    break;
                case AWS_JSON_1_0:
                    response.putHeader("Content-Type", "application/x-amz-json-1.0");
                    break;
                case AWS_QUERY:
                    response.putHeader("Content-Type", "text/xml");
                    break;
                default:
                    throw new AssertionError(protocol);
            }
            if (status == 404 && protocol != Protocol.AWS_QUERY) {
                // What an awsQueryCompatible service sends with a JSON or CBOR error.
                response.putHeader("x-amzn-query-error", "ResourceNotFound;Sender");
            }
            responses.add(response.build());
            bodies.add(body);
            return this;
        }

        SentRequest onlyRequest() {
            assertThat(requests).hasSize(1);
            return requests.get(0);
        }

        SdkHttpClient sync() {
            return new SdkHttpClient() {
                @Override
                public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
                    return new ExecutableHttpRequest() {
                        @Override
                        public HttpExecuteResponse call() {
                            byte[] sentBody = request.contentStreamProvider().map(p -> read(p.newStream()))
                                                     .orElse(new byte[0]);
                            requests.add(new SentRequest(request.httpRequest(), sentBody));
                            SdkHttpFullResponse response = next();
                            return HttpExecuteResponse.builder()
                                                      .response(response)
                                                      .responseBody(AbortableInputStream.create(
                                                          new ByteArrayInputStream(bodies.remove())))
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
                    CompletableFuture<Void> done = new CompletableFuture<>();
                    request.requestContentPublisher().subscribe(new Subscriber<ByteBuffer>() {
                        private final ByteArrayOutputStream sentBody = new ByteArrayOutputStream();

                        @Override
                        public void onSubscribe(Subscription subscription) {
                            subscription.request(Long.MAX_VALUE);
                        }

                        @Override
                        public void onNext(ByteBuffer buffer) {
                            byte[] chunk = bytes(buffer);
                            sentBody.write(chunk, 0, chunk.length);
                        }

                        @Override
                        public void onError(Throwable t) {
                            done.completeExceptionally(t);
                        }

                        @Override
                        public void onComplete() {
                            requests.add(new SentRequest(request.request(), sentBody.toByteArray()));
                            request.responseHandler().onHeaders(next());
                            request.responseHandler().onStream(SdkPublisher.fromIterable(
                                Collections.singletonList(ByteBuffer.wrap(bodies.remove()))));
                            done.complete(null);
                        }
                    });
                    return done;
                }

                @Override
                public void close() {
                }
            };
        }

        private SdkHttpFullResponse next() {
            SdkHttpFullResponse response = responses.poll();
            if (response == null) {
                throw new IllegalStateException("No scripted response for request " + requests.size());
            }
            return response;
        }

        private static byte[] read(InputStream in) {
            try (InputStream stream = in) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int n;
                while ((n = stream.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                }
                return out.toByteArray();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
