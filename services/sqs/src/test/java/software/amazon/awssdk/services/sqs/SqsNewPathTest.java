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

package software.amazon.awssdk.services.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
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
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.client.awsjson.AwsJson1Protocol;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsOperations;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * The generated SQS clients run on the schema-based pipeline path with the awsJson1_0 client protocol (task 9.2), and
 * keep the legacy awsQueryCompatible behavior: every request carries {@code x-amzn-query-mode: true}, and the error
 * code of {@code x-amzn-query-error} is the reported error code while the modeled class comes from {@code __type}.
 */
class SqsNewPathTest {
    private static final String SYNC_PIPELINE = "software.amazon.awssdk.core.internal.http.SyncApiCallPipeline";
    private static final String ASYNC_PIPELINE = "software.amazon.awssdk.core.internal.http.AsyncApiCallPipeline";
    private static final String LEGACY_PIPELINE = "software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline";
    private static final String QUEUE_URL = "https://sqs.us-west-2.amazonaws.com/123456789012/q";

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sendMessage_runsOnTheNewPipelineWithQueryMode(boolean async) {
        Recorder recorder = new Recorder();
        // MessageMD5ChecksumInterceptor checks the MD5 of "hello" against the response on the new path too.
        SendMessageResponse response = call(async, recorder, response(
            200, "{\"MessageId\":\"id-1\",\"MD5OfMessageBody\":\"5d41402abc4b2a76b9719d911017c592\"}", null));

        assertThat(response.messageId()).isEqualTo("id-1");
        recorder.assertRanOnTheNewPipeline(async ? ASYNC_PIPELINE : SYNC_PIPELINE, SqsOperations.SEND_MESSAGE);
        SdkHttpRequest sent = recorder.sent.get(0);
        assertThat(sent.firstMatchingHeader("x-amzn-query-mode")).hasValue("true");
        assertThat(sent.firstMatchingHeader("X-Amz-Target")).hasValue("AmazonSQS.SendMessage");
        assertThat(sent.firstMatchingHeader("Content-Type")).hasValue("application/x-amz-json-1.0");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sendMessage_withQueryError_reportsTheQueryCodeOnTheModeledException(boolean async) {
        Recorder recorder = new Recorder();
        Throwable failure = catchThrowable(() -> call(async, recorder, response(
            400, "{\"__type\":\"com.amazonaws.sqs#QueueDoesNotExist\",\"message\":\"no queue\"}",
            "AWS.SimpleQueueService.NonExistentQueue;Sender")));
        if (failure instanceof CompletionException) {
            failure = failure.getCause();
        }

        assertThat(failure).isInstanceOf(QueueDoesNotExistException.class).hasMessageContaining("no queue");
        QueueDoesNotExistException error = (QueueDoesNotExistException) failure;
        assertThat(error.awsErrorDetails().errorCode()).isEqualTo("AWS.SimpleQueueService.NonExistentQueue");
        assertThat(error.statusCode()).isEqualTo(400);
        recorder.assertRanOnTheNewPipeline(async ? ASYNC_PIPELINE : SYNC_PIPELINE, SqsOperations.SEND_MESSAGE);
    }

    private static SendMessageResponse call(boolean async, Recorder recorder, HttpExecuteResponse response) {
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create("ak", "sk"));
        if (async) {
            MockAsyncHttpClient http = new MockAsyncHttpClient();
            http.stubResponses(response);
            try (SqsAsyncClient client = SqsAsyncClient.builder()
                                                       .region(Region.US_WEST_2)
                                                       .credentialsProvider(credentials)
                                                       .httpClient(http)
                                                       .overrideConfiguration(o -> o.addExecutionInterceptor(recorder)
                                                                                    .retryStrategy(r -> r.maxAttempts(1)))
                                                       .build()) {
                return client.sendMessage(r -> r.queueUrl(QUEUE_URL).messageBody("hello")).join();
            }
        }
        MockSyncHttpClient http = new MockSyncHttpClient();
        http.stubResponses(response);
        try (SqsClient client = SqsClient.builder()
                                         .region(Region.US_WEST_2)
                                         .credentialsProvider(credentials)
                                         .httpClient(http)
                                         .overrideConfiguration(o -> o.addExecutionInterceptor(recorder)
                                                                      .retryStrategy(r -> r.maxAttempts(1)))
                                         .build()) {
            return client.sendMessage(r -> r.queueUrl(QUEUE_URL).messageBody("hello"));
        }
    }

    private static HttpExecuteResponse response(int status, String body, String queryError) {
        SdkHttpResponse.Builder http = SdkHttpResponse.builder().statusCode(status);
        if (queryError != null) {
            http.putHeader("x-amzn-query-error", queryError);
        }
        return HttpExecuteResponse.builder()
                                  .response(http.build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(body)))
                                  .build();
    }

    /**
     * Records the sent request, and checks at {@code beforeTransmission} which pipeline is on the calling thread's stack
     * and the call's operation and protocol.
     */
    private static final class Recorder implements ExecutionInterceptor {
        private final List<SdkHttpRequest> sent = new CopyOnWriteArrayList<>();
        private final List<String> stacks = new CopyOnWriteArrayList<>();
        private volatile ApiOperation<?, ?> operation;
        private volatile Object protocol;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes attributes) {
            sent.add(context.httpRequest());
            StringBuilder stack = new StringBuilder();
            for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
                stack.append(frame.getClassName()).append('\n');
            }
            stacks.add(stack.toString());
            operation = attributes.getAttribute(SdkExecutionAttribute.API_OPERATION);
            protocol = attributes.getAttribute(SdkExecutionAttribute.CLIENT_PROTOCOL);
        }

        void assertRanOnTheNewPipeline(String pipeline, ApiOperation<?, ?> expected) {
            assertThat(stacks).hasSize(1);
            assertThat(stacks.get(0)).contains(pipeline).doesNotContain(LEGACY_PIPELINE);
            assertThat(operation).isSameAs(expected);
            assertThat(protocol).isInstanceOf(AwsJson1Protocol.class);
        }
    }
}
