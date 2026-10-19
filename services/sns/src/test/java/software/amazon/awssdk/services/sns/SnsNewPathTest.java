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

package software.amazon.awssdk.services.sns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.net.URI;
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
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.client.awsquery.AwsQueryClientProtocol;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsOperations;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * The generated SNS clients run on the schema-based pipeline path with the awsQuery client protocol (task 9.4). The
 * request form keys are the legacy ones: list members as {@code <Name>.member.N}, map entries as
 * {@code <Name>.entry.N.key}/{@code value} or the map's own key and value names ({@code MessageAttributes}), and the
 * {@code emitPropertyName} members of {@code AddPermission} under their wire names. The form encoding, the
 * {@code Content-Type} without a charset and the endpoint path with a trailing slash are Phase 5 policy rows 1–3.
 */
class SnsNewPathTest {
    private static final String SYNC_PIPELINE = "software.amazon.awssdk.core.internal.http.SyncApiCallPipeline";
    private static final String ASYNC_PIPELINE = "software.amazon.awssdk.core.internal.http.AsyncApiCallPipeline";
    private static final String LEGACY_PIPELINE = "software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline";
    private static final String TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:t";
    private static final String ENCODED_TOPIC_ARN = "arn%3Aaws%3Asns%3Aus-east-1%3A123456789012%3At";

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void createTopic_withAttributesAndTags_runsOnTheNewPipeline(boolean async) {
        Recorder recorder = new Recorder();
        String body = "<CreateTopicResponse xmlns=\"http://sns.amazonaws.com/doc/2010-03-31/\">"
                      + "<CreateTopicResult><TopicArn>" + TOPIC_ARN + "</TopicArn></CreateTopicResult>"
                      + "<ResponseMetadata><RequestId>req-1</RequestId></ResponseMetadata>"
                      + "</CreateTopicResponse>";

        CreateTopicResponse response = call(
            async, recorder, null, response(200, body),
            c -> c.createTopic(r -> r.name("t").attributes(Collections.singletonMap("DisplayName", "d"))
                                     .tags(t -> t.key("k").value("v"))),
            c -> c.createTopic(r -> r.name("t").attributes(Collections.singletonMap("DisplayName", "d"))
                                     .tags(t -> t.key("k").value("v"))));

        assertThat(response.topicArn()).isEqualTo(TOPIC_ARN);
        assertThat(response.responseMetadata().requestId()).isEqualTo("req-1");
        recorder.assertRanOnTheNewPipeline(async, SnsOperations.CREATE_TOPIC);
        SdkHttpRequest sent = recorder.sent.get(0);
        assertThat(sent.method()).isEqualTo(SdkHttpMethod.POST);
        assertThat(sent.encodedPath()).isEqualTo("/");
        // Policy row 2: no charset parameter.
        assertThat(sent.firstMatchingHeader("Content-Type")).hasValue("application/x-www-form-urlencoded");
        assertThat(recorder.body()).isEqualTo("Action=CreateTopic&Version=2010-03-31&Name=t"
                                              + "&Attributes.entry.1.key=DisplayName&Attributes.entry.1.value=d"
                                              + "&Tags.member.1.Key=k&Tags.member.1.Value=v");
    }

    /**
     * {@code AWSAccountId} and {@code ActionName} are renamed to {@code awsAccountIds} and {@code actionNames} by the
     * {@code emitPropertyName} shape modifiers; the wire names stay.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void addPermission_withEmitPropertyNameMembers_sendsTheWireNames(boolean async) {
        Recorder recorder = new Recorder();
        String body = "<AddPermissionResponse xmlns=\"http://sns.amazonaws.com/doc/2010-03-31/\">"
                      + "<ResponseMetadata><RequestId>req-2</RequestId></ResponseMetadata></AddPermissionResponse>";

        call(async, recorder, null, response(200, body),
             c -> c.addPermission(r -> r.topicArn(TOPIC_ARN).label("l").awsAccountIds("111", "222")
                                        .actionNames("Publish")),
             c -> c.addPermission(r -> r.topicArn(TOPIC_ARN).label("l").awsAccountIds("111", "222")
                                        .actionNames("Publish")));

        assertThat(recorder.body()).isEqualTo("Action=AddPermission&Version=2010-03-31&TopicArn=" + ENCODED_TOPIC_ARN
                                              + "&Label=l&AWSAccountId.member.1=111&AWSAccountId.member.2=222"
                                              + "&ActionName.member.1=Publish");
    }

    /**
     * {@code MessageAttributes} uses the map's own key and value names ({@code Name}, {@code Value}). The space in the
     * message is sent as {@code %20} (policy row 1; legacy sent {@code +}).
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void publish_withMessageAttributes_usesTheMapKeyAndValueNames(boolean async) {
        Recorder recorder = new Recorder();
        String body = "<PublishResponse xmlns=\"http://sns.amazonaws.com/doc/2010-03-31/\">"
                      + "<PublishResult><MessageId>m-1</MessageId></PublishResult>"
                      + "<ResponseMetadata><RequestId>req-3</RequestId></ResponseMetadata></PublishResponse>";
        MessageAttributeValue value = MessageAttributeValue.builder().dataType("String").stringValue("v").build();

        PublishResponse response = call(
            async, recorder, null, response(200, body),
            c -> c.publish(r -> r.topicArn(TOPIC_ARN).message("hello world")
                                 .messageAttributes(Collections.singletonMap("a", value))),
            c -> c.publish(r -> r.topicArn(TOPIC_ARN).message("hello world")
                                 .messageAttributes(Collections.singletonMap("a", value))));

        assertThat(response.messageId()).isEqualTo("m-1");
        assertThat(recorder.body()).isEqualTo("Action=Publish&Version=2010-03-31&TopicArn=" + ENCODED_TOPIC_ARN
                                              + "&Message=hello%20world"
                                              + "&MessageAttributes.entry.1.Name=a"
                                              + "&MessageAttributes.entry.1.Value.DataType=String"
                                              + "&MessageAttributes.entry.1.Value.StringValue=v");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getTopicAttributes_readsTheEntryMap(boolean async) {
        Recorder recorder = new Recorder();
        String body = "<GetTopicAttributesResponse xmlns=\"http://sns.amazonaws.com/doc/2010-03-31/\">"
                      + "<GetTopicAttributesResult><Attributes>"
                      + "<entry><key>DisplayName</key><value>d</value></entry>"
                      + "<entry><key>Owner</key><value>123456789012</value></entry>"
                      + "</Attributes></GetTopicAttributesResult>"
                      + "<ResponseMetadata><RequestId>req-4</RequestId></ResponseMetadata>"
                      + "</GetTopicAttributesResponse>";

        GetTopicAttributesResponse response = call(async, recorder, null, response(200, body),
                                                   c -> c.getTopicAttributes(r -> r.topicArn(TOPIC_ARN)),
                                                   c -> c.getTopicAttributes(r -> r.topicArn(TOPIC_ARN)));

        assertThat(response.attributes()).containsEntry("DisplayName", "d").containsEntry("Owner", "123456789012")
                                         .hasSize(2);
        recorder.assertRanOnTheNewPipeline(async, SnsOperations.GET_TOPIC_ATTRIBUTES);
    }

    /**
     * An endpoint with a path gets a trailing slash (policy row 3; legacy sent {@code /custom}).
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void listTopics_withEndpointPath_sendsThePathWithATrailingSlash(boolean async) {
        Recorder recorder = new Recorder();
        String body = "<ListTopicsResponse xmlns=\"http://sns.amazonaws.com/doc/2010-03-31/\">"
                      + "<ListTopicsResult><Topics><member><TopicArn>" + TOPIC_ARN + "</TopicArn></member></Topics>"
                      + "</ListTopicsResult></ListTopicsResponse>";

        call(async, recorder, URI.create("https://example.com/custom"), response(200, body),
             c -> c.listTopics(r -> { }), c -> c.listTopics(r -> { }));

        assertThat(recorder.sent.get(0).host()).isEqualTo("example.com");
        assertThat(recorder.sent.get(0).encodedPath()).isEqualTo("/custom/");
        assertThat(recorder.body()).isEqualTo("Action=ListTopics&Version=2010-03-31");
    }

    /**
     * The error code is read from {@code <ErrorResponse><Error><Code>} and selects the modeled exception by its
     * {@code awsQueryError} code ({@code NotFound}).
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getTopicAttributes_withModeledError_throwsTheModeledException(boolean async) {
        Recorder recorder = new Recorder();
        String body = "<ErrorResponse xmlns=\"http://sns.amazonaws.com/doc/2010-03-31/\">"
                      + "<Error><Type>Sender</Type><Code>NotFound</Code><Message>Topic does not exist</Message></Error>"
                      + "<RequestId>req-5</RequestId></ErrorResponse>";

        Throwable failure = catchThrowable(() -> call(async, recorder, null, response(404, body),
                                                      c -> c.getTopicAttributes(r -> r.topicArn(TOPIC_ARN)),
                                                      c -> c.getTopicAttributes(r -> r.topicArn(TOPIC_ARN))));
        if (failure instanceof CompletionException) {
            failure = failure.getCause();
        }

        assertThat(failure).isInstanceOf(NotFoundException.class).hasMessageContaining("Topic does not exist");
        NotFoundException error = (NotFoundException) failure;
        assertThat(error.awsErrorDetails().errorCode()).isEqualTo("NotFound");
        assertThat(error.requestId()).isEqualTo("req-5");
        assertThat(error.statusCode()).isEqualTo(404);
        recorder.assertRanOnTheNewPipeline(async, SnsOperations.GET_TOPIC_ATTRIBUTES);
    }

    private static <R> R call(boolean async, Recorder recorder, URI endpoint, HttpExecuteResponse response,
                              Function<SnsClient, R> sync, Function<SnsAsyncClient, CompletableFuture<R>> asyncCall) {
        StaticCredentialsProvider credentials =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("ak", "sk"));
        if (async) {
            MockAsyncHttpClient http = new MockAsyncHttpClient();
            http.stubResponses(response);
            SnsAsyncClientBuilder builder =
                SnsAsyncClient.builder()
                              .region(Region.US_EAST_1)
                              .credentialsProvider(credentials)
                              .httpClient(http)
                              .overrideConfiguration(o -> o.addExecutionInterceptor(recorder)
                                                           .retryStrategy(r -> r.maxAttempts(1)));
            if (endpoint != null) {
                builder.endpointOverride(endpoint);
            }
            try (SnsAsyncClient client = builder.build()) {
                try {
                    return asyncCall.apply(client).join();
                } finally {
                    http.getStreamingPayload().ifPresent(recorder::asyncBody);
                }
            }
        }
        MockSyncHttpClient http = new MockSyncHttpClient();
        http.stubResponses(response);
        SnsClientBuilder builder =
            SnsClient.builder()
                     .region(Region.US_EAST_1)
                     .credentialsProvider(credentials)
                     .httpClient(http)
                     .overrideConfiguration(o -> o.addExecutionInterceptor(recorder)
                                                  .retryStrategy(r -> r.maxAttempts(1)));
        if (endpoint != null) {
            builder.endpointOverride(endpoint);
        }
        try (SnsClient client = builder.build()) {
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
            if (body == null) {
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
            assertThat(protocol).isInstanceOf(AwsQueryClientProtocol.class);
        }
    }
}
