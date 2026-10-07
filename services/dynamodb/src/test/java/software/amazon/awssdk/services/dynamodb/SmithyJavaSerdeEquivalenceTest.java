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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.smithyjava.SmithyJavaSerde;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.Tag;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * Runs the same calls through a client on smithy-java serde and a client on v2's own serde, against the same
 * canned responses, and requires the two to agree: the same request bytes on the wire, and equal response
 * objects (including response metadata).
 *
 * <p>The request comparison is byte-for-byte on the body and exact on every header except the ones that
 * differ between any two calls (signature, date, invocation id). Where a difference is expected, the case
 * says why.
 */
class SmithyJavaSerdeEquivalenceTest {

    private static final Pattern SURROGATE_PAIR =
        Pattern.compile("\\\\u(D[89AB][0-9A-F]{2})\\\\u(D[C-F][0-9A-F]{2})", Pattern.CASE_INSENSITIVE);


    @AfterEach
    void clearProperty() {
        System.clearProperty(SmithyJavaSerde.ENABLED_PROPERTY);
    }

    static Stream<Arguments> requestCases() {
        Map<String, AttributeValue> key = Map.of("pk", AttributeValue.fromS("key-1"));
        Map<String, AttributeValue> item = allTypesItem();
        List<Arguments> cases = new ArrayList<>();
        cases.add(Arguments.of("getItem", (Function<DynamoDbClient, Object>) c -> c.getItem(r -> r
            .tableName("benchmark-table").key(key).consistentRead(true)
            .projectionExpression("#a, b").expressionAttributeNames(Map.of("#a", "a")))));
        cases.add(Arguments.of("putItem", (Function<DynamoDbClient, Object>) c -> c.putItem(r -> r
            .tableName("benchmark-table").item(item)
            .conditionExpression("attribute_not_exists(pk)")
            .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL))));
        cases.add(Arguments.of("batchGetItem", (Function<DynamoDbClient, Object>) c -> c.batchGetItem(r -> r
            .requestItems(Map.of("benchmark-table", KeysAndAttributes.builder()
                                                                       .keys(key, Map.of("pk", AttributeValue.fromS("k2")))
                                                                       .build())))));
        cases.add(Arguments.of("batchWriteItem", (Function<DynamoDbClient, Object>) c -> c.batchWriteItem(r -> r
            .requestItems(Map.of("benchmark-table", List.of(
                WriteRequest.builder().putRequest(PutRequest.builder().item(item).build()).build(),
                WriteRequest.builder().deleteRequest(d -> d.key(key)).build()))))));
        cases.add(Arguments.of("query", (Function<DynamoDbClient, Object>) c -> c.query(r -> r
            .tableName("t").keyConditionExpression("pk = :v").limit(25).scanIndexForward(false)
            .expressionAttributeValues(Map.of(":v", AttributeValue.fromN("-1.5e10"))))));
        cases.add(Arguments.of("emptyInput", (Function<DynamoDbClient, Object>) c -> c.listTables()));
        cases.add(Arguments.of("createTable", (Function<DynamoDbClient, Object>) c -> c.createTable(createTable())));
        cases.add(Arguments.of("timestamps", (Function<DynamoDbClient, Object>) c -> c.listBackups(r -> r
            .timeRangeLowerBound(Instant.ofEpochSecond(1_700_000_000L))
            .timeRangeUpperBound(Instant.ofEpochMilli(1_700_000_000_123L)))));
        cases.add(Arguments.of("emptyCollections", (Function<DynamoDbClient, Object>) c -> c.putItem(r -> r
            .tableName("t").item(Map.of("l", AttributeValue.fromL(List.of()), "m", AttributeValue.fromM(Map.of()),
                                         "ss", AttributeValue.builder().ss(new ArrayList<>()).build())))));
        cases.add(Arguments.of("escaping", (Function<DynamoDbClient, Object>) c -> c.putItem(r -> r
            .tableName("t").item(Map.of("s", AttributeValue.fromS("q\"b\\s/\b\f\n\r\t\u0001\u001f\u007f"
                                                                     + "\u00e9\u4e2d\ud83d\ude00\u2028"))))));
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requestCases")
    void requestsMatchV2(String name, Function<DynamoDbClient, Object> call) {
        Captured v2 = capture(false, call, "{}");
        Captured smithy = capture(true, call, "{}");

        assertThat(new String(smithy.body, StandardCharsets.UTF_8))
            .isEqualTo(unescapeSurrogatePairs(new String(v2.body, StandardCharsets.UTF_8)));
        assertThat(smithy.request.method()).isEqualTo(v2.request.method());
        assertThat(smithy.request.getUri()).isEqualTo(v2.request.getUri());
        if (!name.equals("escaping")) {
            assertThat(comparableHeaders(smithy.request)).isEqualTo(comparableHeaders(v2.request));
        }
    }

    static Stream<Arguments> invalidRequestCases() {
        Map<String, AttributeValue> key = Map.of("pk", AttributeValue.fromS("key-1"));
        List<Arguments> cases = new ArrayList<>();
        cases.add(Arguments.of("missingRequiredTableName", (Function<DynamoDbClient, Object>) c -> c.getItem(r -> r.key(key))));
        cases.add(Arguments.of("missingRequiredKey", (Function<DynamoDbClient, Object>) c -> c.getItem(r -> r.tableName("t"))));
        cases.add(Arguments.of("nullMapValue", (Function<DynamoDbClient, Object>) c -> c.putItem(r -> r
            .tableName("t").item(java.util.Collections.singletonMap("a", null)))));
        cases.add(Arguments.of("nullListElement", (Function<DynamoDbClient, Object>) c -> c.putItem(r -> r
            .tableName("t").item(Map.of("a", AttributeValue.fromL(java.util.Collections.singletonList(null)))))));
        return cases.stream();
    }

    /** Client-side validation: both serde paths must either send the same request or fail the same way. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequestCases")
    void invalidRequestsMatchV2(String name, Function<DynamoDbClient, Object> call) {
        Object v2 = outcome(false, call);
        Object smithy = outcome(true, call);
        assertThat(String.valueOf(smithy)).isEqualTo(String.valueOf(v2));
    }

    private static Object outcome(boolean smithy, Function<DynamoDbClient, Object> call) {
        try {
            Captured captured = capture(smithy, call, "{}");
            return "sent " + new String(captured.body, StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            return e.getClass().getName() + ": " + e.getMessage();
        }
    }

    static Stream<Arguments> responseCases() {
        List<Arguments> cases = new ArrayList<>();
        cases.add(Arguments.of("getItem", "{\"Item\":" + allTypesItemJson() + ","
                                          + "\"ConsumedCapacity\":{\"TableName\":\"t\",\"CapacityUnits\":0.5}}",
                               (Function<DynamoDbClient, Object>) c -> c.getItem(r -> r.tableName("t"))));
        cases.add(Arguments.of("getItemEmpty", "{}",
                               (Function<DynamoDbClient, Object>) c -> c.getItem(r -> r.tableName("t"))));
        cases.add(Arguments.of("putItem", "{\"Attributes\":" + allTypesItemJson() + "}",
                               (Function<DynamoDbClient, Object>) c -> c.putItem(r -> r.tableName("t"))));
        cases.add(Arguments.of("batchGetItem",
                               "{\"Responses\":{\"t\":[" + allTypesItemJson() + "," + allTypesItemJson() + "]},"
                               + "\"UnprocessedKeys\":{\"t\":{\"Keys\":[{\"pk\":{\"S\":\"k\"}}],"
                               + "\"ConsistentRead\":true}}}",
                               (Function<DynamoDbClient, Object>) c -> c.batchGetItem(r -> r.requestItems(Map.of()))));
        cases.add(Arguments.of("batchWriteItem",
                               "{\"UnprocessedItems\":{\"t\":[{\"PutRequest\":{\"Item\":{\"pk\":{\"S\":\"k\"}}}}]},"
                               + "\"ConsumedCapacity\":[{\"TableName\":\"t\",\"CapacityUnits\":2.0}]}",
                               (Function<DynamoDbClient, Object>) c -> c.batchWriteItem(r -> r.requestItems(Map.of()))));
        cases.add(Arguments.of("describeTableTimestamps",
                               "{\"Table\":{\"TableName\":\"t\",\"CreationDateTime\":1.700000000123E9,"
                               + "\"TableStatus\":\"ACTIVE\",\"ItemCount\":9007199254740993,"
                               + "\"ProvisionedThroughput\":{\"LastIncreaseDateTime\":1700000000,"
                               + "\"ReadCapacityUnits\":5,\"WriteCapacityUnits\":5},"
                               + "\"KeySchema\":[{\"AttributeName\":\"pk\",\"KeyType\":\"HASH\"}]}}",
                               (Function<DynamoDbClient, Object>) c -> c.describeTable(r -> r.tableName("t"))));
        cases.add(Arguments.of("unknownMembersAndNulls",
                               "{\"Item\":{\"a\":{\"S\":\"x\"}},\"SomethingNew\":{\"x\":[1,2,{\"y\":null}]},"
                               + "\"ConsumedCapacity\":null}",
                               (Function<DynamoDbClient, Object>) c -> c.getItem(r -> r.tableName("t"))));
        cases.add(Arguments.of("unknownEnumValue",
                               "{\"Table\":{\"TableName\":\"t\",\"TableStatus\":\"NOT_A_STATUS\"}}",
                               (Function<DynamoDbClient, Object>) c -> c.describeTable(r -> r.tableName("t"))));
        cases.add(Arguments.of("unknownUnionMember",
                               "{\"Item\":{\"a\":{\"NEWTYPE\":\"x\"}}}",
                               (Function<DynamoDbClient, Object>) c -> c.getItem(r -> r.tableName("t"))));
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("responseCases")
    void responsesMatchV2(String name, String responseJson, Function<DynamoDbClient, Object> call) {
        Captured v2 = capture(false, call, responseJson);
        Captured smithy = capture(true, call, responseJson);

        assertThat(smithy.response).isEqualTo(v2.response);
        assertThat(smithy.response.toString()).isEqualTo(v2.response.toString());
        AwsResponse v2Response = (AwsResponse) v2.response;
        AwsResponse smithyResponse = (AwsResponse) smithy.response;
        assertThat(smithyResponse.responseMetadata().requestId()).isEqualTo(v2Response.responseMetadata().requestId());
        assertThat(smithyResponse.sdkHttpResponse().statusCode()).isEqualTo(v2Response.sdkHttpResponse().statusCode());
        assertThat(smithyResponse.sdkHttpResponse().headers()).isEqualTo(v2Response.sdkHttpResponse().headers());
    }

    /**
     * The one body difference that is expected: v2's Jackson 2 writes a character outside the Basic Multilingual
     * Plane as an escaped surrogate pair ({@code \\uD83D\\uDE00}), smithy-java's Jackson 3 writes its UTF-8
     * bytes. Both decode to the same string. Content-Length and the payload hash differ with it, so the
     * header comparison is skipped for that case only.
     */
    private static String unescapeSurrogatePairs(String json) {
        return SURROGATE_PAIR.matcher(json).replaceAll(m -> new String(new char[] {
            (char) Integer.parseInt(m.group(1), 16), (char) Integer.parseInt(m.group(2), 16)}));
    }

    private static Map<String, List<String>> comparableHeaders(SdkHttpRequest request) {
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.putAll(request.headers());
        for (String h : Arrays.asList("Authorization", "X-Amz-Date", "amz-sdk-invocation-id")) {
            headers.remove(h);
        }
        return headers;
    }

    private static Captured capture(boolean smithy, Function<DynamoDbClient, Object> call, String responseJson) {
        System.setProperty(SmithyJavaSerde.ENABLED_PROPERTY, Boolean.toString(smithy));
        CapturingHttpClient http = new CapturingHttpClient(responseJson);
        try (DynamoDbClient client = DynamoDbClient.builder()
                                                   .credentialsProvider(StaticCredentialsProvider.create(
                                                       AwsBasicCredentials.create("akid", "skid")))
                                                   .region(Region.US_EAST_1)
                                                   .endpointOverride(URI.create("http://localhost:8000"))
                                                   .httpClient(http)
                                                   .build()) {
            Object response = call.apply(client);
            return new Captured(http.request, http.body, response);
        }
    }

    private static Map<String, AttributeValue> allTypesItem() {
        Map<String, AttributeValue> item = new LinkedHashMap<>();
        item.put("pk", AttributeValue.fromS("key-1"));
        item.put("n", AttributeValue.fromN("12345678901234567890.123456789"));
        item.put("b", AttributeValue.fromB(SdkBytes.fromByteArray(new byte[] {0, 1, 2, (byte) 0xff})));
        item.put("ss", AttributeValue.fromSs(List.of("a", "b")));
        item.put("ns", AttributeValue.fromNs(List.of("1", "2.5")));
        item.put("bs", AttributeValue.fromBs(List.of(SdkBytes.fromUtf8String("x"), SdkBytes.fromUtf8String("y"))));
        item.put("m", AttributeValue.fromM(Map.of("inner", AttributeValue.fromL(List.of(AttributeValue.fromBool(true),
                                                                                        AttributeValue.fromNul(true))))));
        item.put("bool", AttributeValue.fromBool(false));
        item.put("nul", AttributeValue.fromNul(true));
        return item;
    }

    private static String allTypesItemJson() {
        return "{\"pk\":{\"S\":\"key-1\"},\"n\":{\"N\":\"12345678901234567890.123456789\"},\"b\":{\"B\":\"AAEC/w==\"},"
               + "\"ss\":{\"SS\":[\"a\",\"b\"]},\"ns\":{\"NS\":[\"1\",\"2.5\"]},\"bs\":{\"BS\":[\"eA==\",\"eQ==\"]},"
               + "\"m\":{\"M\":{\"inner\":{\"L\":[{\"BOOL\":true},{\"NULL\":true}]}}},\"bool\":{\"BOOL\":false},"
               + "\"nul\":{\"NULL\":true},\"uni\":{\"S\":\"\\u00e9\\ud83d\\ude00\\\"\\n\"}}";
    }

    private static CreateTableRequest createTable() {
        return CreateTableRequest.builder()
                                 .tableName("t")
                                 .attributeDefinitions(AttributeDefinition.builder().attributeName("pk")
                                                                          .attributeType(ScalarAttributeType.S).build())
                                 .keySchema(KeySchemaElement.builder().attributeName("pk").keyType(KeyType.HASH).build())
                                 .billingMode(BillingMode.PROVISIONED)
                                 .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(5L)
                                                                             .writeCapacityUnits(Long.MAX_VALUE).build())
                                 .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                                                                             .indexName("gsi")
                                                                             .keySchema(KeySchemaElement.builder()
                                                                                                        .attributeName("pk")
                                                                                                        .keyType(KeyType.HASH)
                                                                                                        .build())
                                                                             .projection(Projection.builder()
                                                                                                   .projectionType(
                                                                                                       ProjectionType.INCLUDE)
                                                                                                   .nonKeyAttributes("a", "b")
                                                                                                   .build())
                                                                             .build())
                                 .tags(Tag.builder().key("k").value("v").build())
                                 .build();
    }

    private static final class Captured {
        private final SdkHttpRequest request;
        private final byte[] body;
        private final Object response;

        private Captured(SdkHttpRequest request, byte[] body, Object response) {
            this.request = request;
            this.body = body;
            this.response = response;
        }
    }

    private static final class CapturingHttpClient implements SdkHttpClient {
        private final byte[] response;
        private SdkHttpRequest request;
        private byte[] body = new byte[0];

        private CapturingHttpClient(String responseJson) {
            this.response = responseJson.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest executeRequest) {
            this.request = executeRequest.httpRequest();
            executeRequest.contentStreamProvider().ifPresent(p -> {
                try {
                    this.body = p.newStream().readAllBytes();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() {
                    return HttpExecuteResponse.builder()
                                              .response(SdkHttpResponse.builder()
                                                                       .statusCode(200)
                                                                       .putHeader("x-amzn-RequestId", "req-123")
                                                                       .putHeader("Content-Type", "application/x-amz-json-1.0")
                                                                       .putHeader("Content-Length",
                                                                                  String.valueOf(response.length))
                                                                       .build())
                                              .responseBody(AbortableInputStream.create(new ByteArrayInputStream(response)))
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
    }
}
