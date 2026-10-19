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

package software.amazon.awssdk.services.route53;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.client.restxml.RestXmlClientProtocol;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.services.route53.model.ChangeAction;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.ChangeStatus;
import software.amazon.awssdk.services.route53.model.CreateHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.GetChangeResponse;
import software.amazon.awssdk.services.route53.model.GetHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.route53.model.NoSuchHostedZoneException;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.route53.model.Route53Exception;
import software.amazon.awssdk.services.route53.model.Route53Operations;
import software.amazon.awssdk.services.route53.model.TagResourceType;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * The generated Route 53 clients run on the schema-based pipeline path with the restXml client protocol (task 9.4):
 * URI labels (the converter's member-named labels, such as {@code {HostedZoneId}}, send the same path as the C2J
 * {@code {Id}}), query parameters, the request root element and namespace, response parsing and the
 * {@code Location} response header, errors from {@code <ErrorResponse><Error>} (including the deprecated
 * {@code HealthCheckInUse}), the waiter, and {@code Route53IdInterceptor}, which removes the resource path prefix
 * from the returned IDs.
 */
class Route53NewPathTest {
    private static final String SYNC_PIPELINE = "software.amazon.awssdk.core.internal.http.SyncApiCallPipeline";
    private static final String ASYNC_PIPELINE = "software.amazon.awssdk.core.internal.http.AsyncApiCallPipeline";
    private static final String LEGACY_PIPELINE = "software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline";
    private static final String NAMESPACE = "https://route53.amazonaws.com/doc/2013-04-01/";
    private static final String CHANGE_INFO = "<ChangeInfo><Id>/change/C1</Id><Status>%s</Status>"
                                              + "<SubmittedAt>2024-01-01T00:00:00Z</SubmittedAt></ChangeInfo>";

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void changeResourceRecordSets_sendsTheRootElement_andTheInterceptorStripsTheChangeId(boolean async) {
        Recorder recorder = new Recorder();
        String body = "<ChangeResourceRecordSetsResponse xmlns=\"" + NAMESPACE + "\">"
                      + String.format(CHANGE_INFO, "PENDING") + "</ChangeResourceRecordSetsResponse>";

        ChangeResourceRecordSetsResponse response = call(
            async, recorder, response(200, body),
            c -> c.changeResourceRecordSets(r -> r.hostedZoneId("Z1").changeBatch(b -> b.changes(
                ch -> ch.action(ChangeAction.CREATE).resourceRecordSet(
                    s -> s.name("a.example.com").type(RRType.A).ttl(60L).resourceRecords(v -> v.value("192.0.2.1")))))),
            c -> c.changeResourceRecordSets(r -> r.hostedZoneId("Z1").changeBatch(b -> b.changes(
                ch -> ch.action(ChangeAction.CREATE).resourceRecordSet(
                    s -> s.name("a.example.com").type(RRType.A).ttl(60L).resourceRecords(v -> v.value("192.0.2.1")))))));

        assertThat(response.changeInfo().id()).isEqualTo("C1");
        assertThat(response.changeInfo().status()).isEqualTo(ChangeStatus.PENDING);
        recorder.assertRanOnTheNewPipeline(async, Route53Operations.CHANGE_RESOURCE_RECORD_SETS);
        SdkHttpRequest sent = recorder.sent.get(0);
        assertThat(sent.method()).isEqualTo(SdkHttpMethod.POST);
        assertThat(sent.encodedPath()).isEqualTo("/2013-04-01/hostedzone/Z1/rrset/");
        assertThat(recorder.body()).isEqualTo(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<ChangeResourceRecordSetsRequest xmlns=\"" + NAMESPACE + "\"><ChangeBatch><Changes><Change>"
            + "<Action>CREATE</Action><ResourceRecordSet><Name>a.example.com</Name><Type>A</Type><TTL>60</TTL>"
            + "<ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords>"
            + "</ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>");
    }

    /**
     * A 201 response with the {@code Location} header bound to an output member, and the interceptor removing the
     * prefixes of all three returned IDs.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void createHostedZone_readsTheLocationHeader_andTheInterceptorStripsEveryId(boolean async) {
        Recorder recorder = new Recorder();
        String location = "https://route53.amazonaws.com/2013-04-01/hostedzone/Z1";
        String body = "<CreateHostedZoneResponse xmlns=\"" + NAMESPACE + "\"><HostedZone><Id>/hostedzone/Z1</Id>"
                      + "<Name>example.com.</Name><CallerReference>ref</CallerReference></HostedZone>"
                      + String.format(CHANGE_INFO, "PENDING")
                      + "<DelegationSet><Id>/delegationset/D1</Id><NameServers><NameServer>ns-1.example.net"
                      + "</NameServer></NameServers></DelegationSet></CreateHostedZoneResponse>";
        HttpExecuteResponse created =
            HttpExecuteResponse.builder()
                               .response(SdkHttpResponse.builder().statusCode(201).putHeader("Location", location)
                                                        .build())
                               .responseBody(AbortableInputStream.create(new StringInputStream(body)))
                               .build();

        CreateHostedZoneResponse response = call(
            async, recorder, created,
            c -> c.createHostedZone(r -> r.name("example.com").callerReference("ref")),
            c -> c.createHostedZone(r -> r.name("example.com").callerReference("ref")));

        assertThat(response.location()).isEqualTo(location);
        assertThat(response.hostedZone().id()).isEqualTo("Z1");
        assertThat(response.changeInfo().id()).isEqualTo("C1");
        assertThat(response.delegationSet().id()).isEqualTo("D1");
        recorder.assertRanOnTheNewPipeline(async, Route53Operations.CREATE_HOSTED_ZONE);
        assertThat(recorder.sent.get(0).encodedPath()).isEqualTo("/2013-04-01/hostedzone");
        assertThat(recorder.body()).isEqualTo(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<CreateHostedZoneRequest xmlns=\"" + NAMESPACE + "\"><Name>example.com</Name>"
            + "<CallerReference>ref</CallerReference></CreateHostedZoneRequest>");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void listResourceRecordSets_sendsTheLabelAndQuery_andReadsTheRecordSets(boolean async) {
        Recorder recorder = new Recorder();
        String body = "<ListResourceRecordSetsResponse xmlns=\"" + NAMESPACE + "\"><ResourceRecordSets>"
                      + "<ResourceRecordSet><Name>a.example.com.</Name><Type>A</Type><AliasTarget>"
                      + "<HostedZoneId>/hostedzone/Z2</HostedZoneId><DNSName>b.example.com.</DNSName>"
                      + "<EvaluateTargetHealth>false</EvaluateTargetHealth></AliasTarget></ResourceRecordSet>"
                      + "</ResourceRecordSets><IsTruncated>false</IsTruncated><MaxItems>10</MaxItems>"
                      + "</ListResourceRecordSetsResponse>";

        ListResourceRecordSetsResponse response = call(
            async, recorder, response(200, body),
            c -> c.listResourceRecordSets(r -> r.hostedZoneId("Z1").startRecordName("a.example.com")
                                                .startRecordType(RRType.A).maxItems("10")),
            c -> c.listResourceRecordSets(r -> r.hostedZoneId("Z1").startRecordName("a.example.com")
                                                .startRecordType(RRType.A).maxItems("10")));

        assertThat(response.isTruncated()).isFalse();
        assertThat(response.maxItems()).isEqualTo("10");
        assertThat(response.resourceRecordSets()).hasSize(1);
        assertThat(response.resourceRecordSets().get(0).name()).isEqualTo("a.example.com.");
        assertThat(response.resourceRecordSets().get(0).aliasTarget().hostedZoneId()).isEqualTo("Z2");
        recorder.assertRanOnTheNewPipeline(async, Route53Operations.LIST_RESOURCE_RECORD_SETS);
        SdkHttpRequest sent = recorder.sent.get(0);
        assertThat(sent.method()).isEqualTo(SdkHttpMethod.GET);
        assertThat(sent.encodedPath()).isEqualTo("/2013-04-01/hostedzone/Z1/rrset");
        assertThat(sent.rawQueryParameters()).containsOnlyKeys("name", "type", "maxitems")
                                             .containsEntry("name", Collections.singletonList("a.example.com"))
                                             .containsEntry("type", Collections.singletonList("A"))
                                             .containsEntry("maxitems", Collections.singletonList("10"));
        assertThat(recorder.body).isNull();
    }

    /**
     * A label value is percent-encoded, {@code /} included (no Route 53 label is greedy), and the returned hosted zone
     * ID loses its {@code /hostedzone/} prefix.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getHostedZone_encodesTheLabel_andTheInterceptorStripsTheZoneId(boolean async) {
        Recorder recorder = new Recorder();
        String body = "<GetHostedZoneResponse xmlns=\"" + NAMESPACE + "\"><HostedZone><Id>/hostedzone/Z1</Id>"
                      + "<Name>example.com.</Name><CallerReference>r</CallerReference></HostedZone>"
                      + "<DelegationSet><NameServers><NameServer>ns-1.example.net</NameServer>"
                      + "<NameServer>ns-2.example.net</NameServer></NameServers></DelegationSet>"
                      + "</GetHostedZoneResponse>";

        GetHostedZoneResponse response = call(async, recorder, response(200, body),
                                              c -> c.getHostedZone(r -> r.id("a/b c")),
                                              c -> c.getHostedZone(r -> r.id("a/b c")));

        assertThat(response.hostedZone().id()).isEqualTo("Z1");
        assertThat(response.delegationSet().nameServers()).containsExactly("ns-1.example.net", "ns-2.example.net");
        assertThat(recorder.sent.get(0).encodedPath()).isEqualTo("/2013-04-01/hostedzone/a%2Fb%20c");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void listTagsForResource_sendsBothLabels(boolean async) {
        Recorder recorder = new Recorder();
        String body = "<ListTagsForResourceResponse xmlns=\"" + NAMESPACE + "\"><ResourceTagSet>"
                      + "<ResourceType>hostedzone</ResourceType><ResourceId>Z1</ResourceId>"
                      + "<Tags><Tag><Key>k</Key><Value>v</Value></Tag></Tags></ResourceTagSet>"
                      + "</ListTagsForResourceResponse>";

        ListTagsForResourceResponse response = call(
            async, recorder, response(200, body),
            c -> c.listTagsForResource(r -> r.resourceType(TagResourceType.HOSTEDZONE).resourceId("Z1")),
            c -> c.listTagsForResource(r -> r.resourceType(TagResourceType.HOSTEDZONE).resourceId("Z1")));

        assertThat(response.resourceTagSet().resourceType()).isEqualTo(TagResourceType.HOSTEDZONE);
        assertThat(response.resourceTagSet().tags()).hasSize(1);
        assertThat(response.resourceTagSet().tags().get(0).key()).isEqualTo("k");
        assertThat(recorder.sent.get(0).encodedPath()).isEqualTo("/2013-04-01/tags/hostedzone/Z1");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getHostedZone_withModeledError_throwsTheModeledException(boolean async) {
        Recorder recorder = new Recorder();
        Throwable failure = callForError(async, recorder, 404, "NoSuchHostedZone", "No hosted zone found",
                                         c -> c.getHostedZone(r -> r.id("Z1")),
                                         c -> c.getHostedZone(r -> r.id("Z1")));

        assertThat(failure).isInstanceOf(NoSuchHostedZoneException.class).hasMessageContaining("No hosted zone found");
        NoSuchHostedZoneException error = (NoSuchHostedZoneException) failure;
        assertThat(error.awsErrorDetails().errorCode()).isEqualTo("NoSuchHostedZone");
        assertThat(error.requestId()).isEqualTo("req-1");
        assertThat(error.statusCode()).isEqualTo(404);
        recorder.assertRanOnTheNewPipeline(async, Route53Operations.GET_HOSTED_ZONE);
    }

    /**
     * {@code HealthCheckInUse} is listed in {@code deprecatedShapes}, so it is not one of {@code DeleteHealthCheck}'s
     * errors and has no exception class (as on the legacy route): the error is the service exception with its code.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deleteHealthCheck_withDeprecatedError_throwsTheServiceException(boolean async) {
        Recorder recorder = new Recorder();
        Throwable failure = callForError(async, recorder, 400, "HealthCheckInUse", "in use",
                                         c -> c.deleteHealthCheck(r -> r.healthCheckId("h")),
                                         c -> c.deleteHealthCheck(r -> r.healthCheckId("h")));

        assertThat(failure).isExactlyInstanceOf(Route53Exception.class).hasMessageContaining("in use");
        assertThat(((Route53Exception) failure).awsErrorDetails().errorCode()).isEqualTo("HealthCheckInUse");
        assertThat(((Route53Exception) failure).statusCode()).isEqualTo(400);
        assertThat(recorder.sent.get(0).method()).isEqualTo(SdkHttpMethod.DELETE);
        assertThat(recorder.sent.get(0).encodedPath()).isEqualTo("/2013-04-01/healthcheck/h");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void waitUntilResourceRecordSetsChanged_readsTheChangeStatus(boolean async) {
        Recorder recorder = new Recorder();
        String body = "<GetChangeResponse xmlns=\"" + NAMESPACE + "\">" + String.format(CHANGE_INFO, "INSYNC")
                      + "</GetChangeResponse>";

        WaiterResponse<GetChangeResponse> response = call(
            async, recorder, response(200, body),
            c -> c.waiter().waitUntilResourceRecordSetsChanged(r -> r.id("C1")),
            c -> c.waiter().waitUntilResourceRecordSetsChanged(r -> r.id("C1")));

        assertThat(response.attemptsExecuted()).isEqualTo(1);
        GetChangeResponse change = response.matched().response().orElseThrow(AssertionError::new);
        assertThat(change.changeInfo().status()).isEqualTo(ChangeStatus.INSYNC);
        assertThat(change.changeInfo().id()).isEqualTo("C1");
        recorder.assertRanOnTheNewPipeline(async, Route53Operations.GET_CHANGE);
        assertThat(recorder.sent.get(0).encodedPath()).isEqualTo("/2013-04-01/change/C1");
    }

    private static <R> Throwable callForError(boolean async, Recorder recorder, int status, String code,
                                              String message, Function<Route53Client, R> sync,
                                              Function<Route53AsyncClient, CompletableFuture<R>> asyncCall) {
        String body = "<ErrorResponse xmlns=\"" + NAMESPACE + "\"><Error><Type>Sender</Type><Code>" + code
                      + "</Code><Message>" + message + "</Message></Error><RequestId>req-1</RequestId></ErrorResponse>";
        Throwable failure = catchThrowable(() -> call(async, recorder, response(status, body), sync, asyncCall));
        return failure instanceof CompletionException ? failure.getCause() : failure;
    }

    private static <R> R call(boolean async, Recorder recorder, HttpExecuteResponse response,
                              Function<Route53Client, R> sync,
                              Function<Route53AsyncClient, CompletableFuture<R>> asyncCall) {
        StaticCredentialsProvider credentials =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("ak", "sk"));
        if (async) {
            MockAsyncHttpClient http = new MockAsyncHttpClient();
            http.stubResponses(response);
            try (Route53AsyncClient client =
                     Route53AsyncClient.builder()
                                       .region(Region.AWS_GLOBAL)
                                       .credentialsProvider(credentials)
                                       .httpClient(http)
                                       .overrideConfiguration(o -> o.addExecutionInterceptor(recorder)
                                                                    .retryStrategy(r -> r.maxAttempts(1)))
                                       .build()) {
                try {
                    return asyncCall.apply(client).join();
                } finally {
                    http.getStreamingPayload().ifPresent(recorder::asyncBody);
                }
            }
        }
        MockSyncHttpClient http = new MockSyncHttpClient();
        http.stubResponses(response);
        try (Route53Client client =
                 Route53Client.builder()
                              .region(Region.AWS_GLOBAL)
                              .credentialsProvider(credentials)
                              .httpClient(http)
                              .overrideConfiguration(o -> o.addExecutionInterceptor(recorder)
                                                           .retryStrategy(r -> r.maxAttempts(1)))
                              .build()) {
            return sync.apply(client);
        }
    }

    private static HttpExecuteResponse response(int status, String body) {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(status).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(body)))
                                  .build();
    }

    /**
     * Records the sent request and its body, and checks at {@code beforeTransmission} which pipeline is on the calling
     * thread's stack and the call's operation and protocol.
     */
    private static final class Recorder implements ExecutionInterceptor {
        private final List<SdkHttpRequest> sent = new CopyOnWriteArrayList<>();
        private final List<String> stacks = new CopyOnWriteArrayList<>();
        private volatile byte[] body;
        private volatile ApiOperation<?, ?> operation;
        private volatile Object protocol;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes attributes) {
            sent.add(context.httpRequest());
            if (context.requestBody().isPresent()) {
                body = FunctionalUtils.invokeSafely(
                    () -> IoUtils.toByteArray(context.requestBody().get().contentStreamProvider().newStream()));
            }
            StringBuilder stack = new StringBuilder();
            for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
                stack.append(frame.getClassName()).append('\n');
            }
            stacks.add(stack.toString());
            operation = attributes.getAttribute(SdkExecutionAttribute.API_OPERATION);
            protocol = attributes.getAttribute(SdkExecutionAttribute.CLIENT_PROTOCOL);
        }

        void asyncBody(byte[] bytes) {
            if (body == null && bytes.length > 0) {
                body = bytes;
            }
        }

        String body() {
            assertThat(body).as("request body").isNotNull();
            return new String(body, StandardCharsets.UTF_8);
        }

        void assertRanOnTheNewPipeline(boolean async, ApiOperation<?, ?> expected) {
            assertThat(stacks).hasSize(1);
            assertThat(stacks.get(0)).contains(async ? ASYNC_PIPELINE : SYNC_PIPELINE).doesNotContain(LEGACY_PIPELINE);
            assertThat(operation).isSameAs(expected);
            assertThat(protocol).isInstanceOf(RestXmlClientProtocol.class);
        }
    }
}
