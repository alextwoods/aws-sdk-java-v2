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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.protocols.smithyjava.SmithyJavaSerde;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

/**
 * The async client over a real HTTP connection (Netty, against WireMock): requests and responses on smithy-java
 * serde must match v2's, including a modeled error, which smithy-java serde leaves to v2's error unmarshalling.
 */
@WireMockTest
class SmithyJavaSerdeAsyncEquivalenceTest {

    private static final String ITEM = "{\"pk\":{\"S\":\"k\"},\"n\":{\"N\":\"1.5\"},\"nul\":{\"NULL\":true},"
                                       + "\"l\":{\"L\":[{\"M\":{\"b\":{\"B\":\"AAE=\"}}}]}}";

    @AfterEach
    void clearProperty() {
        System.clearProperty(SmithyJavaSerde.ENABLED_PROPERTY);
    }

    @Test
    void getItem(WireMockRuntimeInfo wm) {
        compare(wm, 200, "{\"Item\":" + ITEM + "}",
                c -> c.getItem(r -> r.tableName("t").key(Map.of("pk", AttributeValue.fromS("k")))).join());
    }

    @Test
    void putItem(WireMockRuntimeInfo wm) {
        compare(wm, 200, "{\"Attributes\":" + ITEM + "}",
                c -> c.putItem(r -> r.tableName("t").item(Map.of("pk", AttributeValue.fromS("k"),
                                                                 "nul", AttributeValue.fromNul(true)))).join());
    }

    @Test
    void batchGetItem(WireMockRuntimeInfo wm) {
        compare(wm, 200, "{\"Responses\":{\"t\":[" + ITEM + "," + ITEM + "]},\"UnprocessedKeys\":{}}",
                c -> c.batchGetItem(r -> r.requestItems(Map.of("t", KeysAndAttributes.builder().keys(
                    Map.of("pk", AttributeValue.fromS("a")), Map.of("pk", AttributeValue.fromS("b"))).build()))).join());
    }

    @Test
    void modeledError(WireMockRuntimeInfo wm) {
        compare(wm, 400, "{\"__type\":\"com.amazonaws.dynamodb.v20120810#ConditionalCheckFailedException\","
                         + "\"message\":\"The conditional request failed\",\"Item\":" + ITEM + "}",
                c -> c.putItem(r -> r.tableName("t").item(Map.of("pk", AttributeValue.fromS("k")))
                                     .conditionExpression("attribute_not_exists(pk)")).join());
    }

    private static void compare(WireMockRuntimeInfo wm, int status, String body,
                                Function<DynamoDbAsyncClient, Object> call) {
        wm.getWireMock().register(any(urlEqualTo("/")).willReturn(
            aResponse().withStatus(status).withHeader("x-amzn-RequestId", "rid").withBody(body)));
        Object v2 = outcome(wm, false, call);
        Object smithy = outcome(wm, true, call);
        assertThat(smithy).isEqualTo(v2);

        List<LoggedRequest> requests = wm.getWireMock().find(com.github.tomakehurst.wiremock.client.WireMock
                                                                 .anyRequestedFor(urlEqualTo("/")));
        assertThat(requests).hasSize(2);
        assertThat(requests.get(1).getBodyAsString()).isEqualTo(requests.get(0).getBodyAsString());
        assertThat(requests.get(1).getHeader("X-Amz-Target")).isEqualTo(requests.get(0).getHeader("X-Amz-Target"));
        assertThat(requests.get(1).getHeader("Content-Type")).isEqualTo(requests.get(0).getHeader("Content-Type"));
    }

    private static Object outcome(WireMockRuntimeInfo wm, boolean smithy, Function<DynamoDbAsyncClient, Object> call) {
        System.setProperty(SmithyJavaSerde.ENABLED_PROPERTY, Boolean.toString(smithy));
        try (DynamoDbAsyncClient client = DynamoDbAsyncClient.builder()
                                                             .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                                             .region(Region.US_EAST_1)
                                                             .credentialsProvider(StaticCredentialsProvider.create(
                                                                 AwsBasicCredentials.create("test", "test")))
                                                             .build()) {
            Object response = call.apply(client);
            return List.of(response, response.toString());
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AwsServiceException) {
                AwsServiceException ase = (AwsServiceException) cause;
                return List.of(ase.getClass().getName(), ase.getMessage(), ase.statusCode(),
                               ase.awsErrorDetails().errorCode(), String.valueOf(ase.requestId()));
            }
            return List.of(cause.getClass().getName(), String.valueOf(cause.getMessage()));
        }
    }
}
