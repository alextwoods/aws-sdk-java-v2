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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.ImmutableMap;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * The request body of {@code PutItem} contains only the remaining bytes of a binary attribute's buffer. Before task 9.2
 * this called the generated {@code PutItemRequestMarshaller}, which the schema path no longer generates; it now sends
 * the request through the client and reads the body at {@code beforeTransmission}.
 */
public class PutItemRequestMarshallerTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Regression test for TT0075355961
     */
    @Test
    public void onlyRemainingByteBufferDataIsMarshalled() throws IOException {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[] {0, 0, 0, 1, 1, 1});
        // Consume some of the byte buffer
        byteBuffer.position(3);
        AtomicReference<byte[]> body = new AtomicReference<>();
        MockSyncHttpClient http = new MockSyncHttpClient();
        http.stubNextResponse(HttpExecuteResponse.builder()
                                                 .response(SdkHttpResponse.builder().statusCode(200).build())
                                                 .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                                 .build());
        ExecutionInterceptor bodyRecorder = new ExecutionInterceptor() {
            @Override
            public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes attributes) {
                body.set(FunctionalUtils.invokeSafely(
                    () -> IoUtils.toByteArray(context.requestBody().get().contentStreamProvider().newStream())));
            }
        };

        try (DynamoDbClient client = DynamoDbClient.builder()
                                                   .credentialsProvider(StaticCredentialsProvider.create(
                                                       AwsBasicCredentials.create("test", "test")))
                                                   .region(Region.US_WEST_2)
                                                   .httpClient(http)
                                                   .overrideConfiguration(o -> o.addExecutionInterceptor(bodyRecorder))
                                                   .build()) {
            client.putItem(PutItemRequest.builder().tableName("t").item(
                ImmutableMap.of("binaryProp", AttributeValue.builder().b(SdkBytes.fromByteBuffer(byteBuffer)).build()))
                                         .build());
        }

        JsonNode marshalledContent = MAPPER.readTree(body.get());
        String base64Binary = marshalledContent.get("Item").get("binaryProp").get("B").asText();
        // Only the remaining data in the byte buffer should have been read and marshalled.
        assertEquals(BinaryUtils.toBase64(new byte[] {1, 1, 1}), base64Binary);
    }
}
