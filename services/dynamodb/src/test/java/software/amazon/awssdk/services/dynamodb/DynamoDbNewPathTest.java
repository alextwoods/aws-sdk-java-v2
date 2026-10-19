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

package software.amazon.awssdk.services.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.CRC32;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.Crc32MismatchException;
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
import software.amazon.awssdk.services.dynamodb.model.DynamoDbOperations;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

/**
 * The generated DynamoDB clients run on the schema-based pipeline path with the awsJson1_0 client protocol (task 9.2):
 * the {@code x-amz-crc32} checksum is checked against the gzip-compressed body (DynamoDB's
 * {@code calculateCrc32FromCompressedData}), a mismatch fails the call with the legacy {@link Crc32MismatchException}, and the
 * account ID of the credentials still selects the account-based endpoint.
 */
class DynamoDbNewPathTest {
    private static final String SYNC_PIPELINE = "software.amazon.awssdk.core.internal.http.SyncApiCallPipeline";
    private static final String ASYNC_PIPELINE = "software.amazon.awssdk.core.internal.http.AsyncApiCallPipeline";
    private static final String LEGACY_PIPELINE = "software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline";
    private static final byte[] ITEM = "{\"Item\":{\"id\":{\"S\":\"a\"}}}".getBytes(StandardCharsets.UTF_8);

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getItem_withGzipAndCompressedChecksum_runsOnTheNewPipelineWithTheAccountEndpoint(boolean async)
        throws IOException {
        Recorder recorder = new Recorder();
        byte[] gzipped = gzip(ITEM);

        GetItemResponse response = call(async, recorder, gzipResponse(gzipped, crc32(gzipped)));

        assertThat(response.item().get("id").s()).isEqualTo("a");
        assertThat(recorder.stacks).hasSize(1);
        assertThat(recorder.stacks.get(0)).contains(async ? ASYNC_PIPELINE : SYNC_PIPELINE)
                                          .doesNotContain(LEGACY_PIPELINE);
        assertThat(recorder.operation).isSameAs(DynamoDbOperations.GET_ITEM);
        assertThat(recorder.protocol).isInstanceOf(AwsJson1Protocol.class);
        SdkHttpRequest sent = recorder.sent.get(0);
        assertThat(sent.host()).isEqualTo("123456789012.ddb.us-west-2.amazonaws.com");
        assertThat(sent.firstMatchingHeader("X-Amz-Target")).hasValue("DynamoDB_20120810.GetItem");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getItem_withMismatchingChecksum_failsWithCrc32Mismatch(boolean async) throws IOException {
        Recorder recorder = new Recorder();
        byte[] gzipped = gzip(ITEM);
        // The checksum of the decompressed body does not match: DynamoDB checks the body as received.
        String wrong = crc32(ITEM);

        Throwable failure = catchThrowable(() -> call(async, recorder, gzipResponse(gzipped, wrong)));
        if (failure instanceof CompletionException) {
            failure = failure.getCause();
        }

        assertThat(failure).isInstanceOf(Crc32MismatchException.class)
                           .hasMessageStartingWith("Expected " + wrong + " as the Crc32 checksum but the actual "
                                                   + "calculated checksum was " + crc32(gzipped));
        assertThat(((Crc32MismatchException) failure).retryable()).isTrue();
        assertThat(recorder.sent).hasSize(1);
    }

    private static GetItemResponse call(boolean async, Recorder recorder, HttpExecuteResponse response) {
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
            AwsBasicCredentials.builder().accessKeyId("ak").secretAccessKey("sk").accountId("123456789012").build());
        ClientOverrideConfiguration override = ClientOverrideConfiguration.builder()
                                                                          .addExecutionInterceptor(recorder)
                                                                          .retryStrategy(r -> r.maxAttempts(1))
                                                                          .build();
        if (async) {
            MockAsyncHttpClient http = new MockAsyncHttpClient();
            http.stubResponses(response);
            try (DynamoDbAsyncClient client = DynamoDbAsyncClient.builder()
                                                                 .region(Region.US_WEST_2)
                                                                 .credentialsProvider(credentials)
                                                                 .httpClient(http)
                                                                 .overrideConfiguration(override)
                                                                 .build()) {
                return client.getItem(r -> r.tableName("t")).join();
            }
        }
        MockSyncHttpClient http = new MockSyncHttpClient();
        http.stubResponses(response);
        try (DynamoDbClient client = DynamoDbClient.builder()
                                                   .region(Region.US_WEST_2)
                                                   .credentialsProvider(credentials)
                                                   .httpClient(http)
                                                   .overrideConfiguration(override)
                                                   .build()) {
            return client.getItem(r -> r.tableName("t"));
        }
    }

    private static HttpExecuteResponse gzipResponse(byte[] body, String checksum) {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(200)
                                                           .putHeader("Content-Encoding", "gzip")
                                                           .putHeader("x-amz-crc32", checksum)
                                                           .build())
                                  .responseBody(AbortableInputStream.create(
                                      new ByteArrayInputStream(body)))
                                  .build();
    }

    private static String crc32(byte[] bytes) {
        CRC32 crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return String.valueOf(crc32.getValue());
    }

    private static byte[] gzip(byte[] bytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(bytes);
        }
        return out.toByteArray();
    }

    /**
     * Records the sent requests, and at {@code beforeTransmission} the pipeline on the calling thread's stack and the
     * call's operation and protocol.
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
    }
}
